package com.videomobileplayer

import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.os.Build
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.UIManagerHelper
import io.github.mobileplayer.compose.VideoPlayer
import io.github.mobileplayer.compose.VideoPlayerUiConfig
import io.github.mobileplayer.compose.createDefaultVideoPlayerController
import io.github.mobileplayer.core.PlaybackState
import io.github.mobileplayer.core.PlayerEngineType
import io.github.mobileplayer.core.PlayerPresentationController
import io.github.mobileplayer.core.PlayerPresentationMode
import io.github.mobileplayer.core.PlayerPresentationRequest
import io.github.mobileplayer.core.PlayerPresentationState
import io.github.mobileplayer.core.VideoSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONObject

class VideoMobilePlayerView(
  private val reactContext: ThemedReactContext,
) : FrameLayout(reactContext) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private val player = createDefaultVideoPlayerController(reactContext, scope)
  private val presentation = PlayerPresentationController()
  private val uiConfig = mutableStateOf(VideoPlayerUiConfig())
  private var lastConfigJson: String? = null
  private var released = false

  private val composeView = ComposeView(reactContext).apply {
    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
    setContent {
      VideoPlayer(
        controller = player,
        presentationController = presentation,
        uiConfig = uiConfig.value,
      )
    }
  }

  init {
    addView(composeView, LayoutParams(MATCH_PARENT, MATCH_PARENT))
    observeState()
    handlePresentationRequests()
  }

  fun setConfigJson(value: String?) {
    if (value.isNullOrBlank() || value == lastConfigJson || released) return
    lastConfigJson = value
    val config = JSONObject(value)
    val engine = config.optString("initialEngine").toEngineType()
    if (player.state.value.engineType != engine) player.switchEngine(engine)

    uiConfig.value = VideoPlayerUiConfig(
      showEngineSwitcher = config.optBoolean("showEngineSwitcher", true),
      showPresentationControls = config.optBoolean("showPresentationControls", true),
      showNetworkSpeedWhileLoading = config.optBoolean("showNetworkSpeedWhileLoading", true),
    )

    val source = config.getJSONObject("source").toVideoSource()
    player.setMediaIfChanged(source, playWhenReady = config.optBoolean("autoPlay", true))

    val startupMode = config.optString("startupMode").toPresentationMode()
    if (presentation.state.value.displayedMode != startupMode) {
      presentation.requestMode(startupMode)
    }
  }

  fun play() = player.play()
  fun pause() = player.pause()
  fun togglePlayPause() = player.togglePlayPause()
  fun seekTo(positionMs: Double) = player.seekTo(positionMs.toLong())
  fun seekBy(offsetMs: Double) = player.seekBy(offsetMs.toLong())
  fun setVolume(volume: Double) = player.setVolume(volume.toFloat())
  fun setPlaybackSpeed(speed: Double) = player.setPlaybackSpeed(speed.toFloat())
  fun switchEngine(engine: String?) = player.switchEngine(engine.toEngineType())
  fun retry() = player.retry()
  fun requestMode(mode: String?) = presentation.requestMode(mode.toPresentationMode())
  fun returnFromFloating() = presentation.requestReturnFromFloating()
  fun syncPresentationMode(mode: String?) = presentation.confirmMode(mode.toPresentationMode())
  fun closePlayer() = presentation.requestClose()

  fun release() {
    if (released) return
    released = true
    composeView.disposeComposition()
    player.close()
    scope.cancel()
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    emitPlaybackState(player.state.value)
    emitPresentationState(presentation.state.value)
  }

  private fun observeState() {
    scope.launch {
      player.state.collectLatest(::emitPlaybackState)
    }
    scope.launch {
      presentation.state.collectLatest(::emitPresentationState)
    }
  }

  private fun handlePresentationRequests() {
    scope.launch {
      presentation.requests.collectLatest { request ->
        when (request) {
          is PlayerPresentationRequest.ChangeMode -> changeMode(request.to)
          PlayerPresentationRequest.Close -> {
            presentation.confirmClosed()
            emit("topCloseRequest", Arguments.createMap().apply {
              putBoolean("isClosed", true)
            })
          }
        }
      }
    }
  }

  private fun changeMode(mode: PlayerPresentationMode) {
    val activity = reactContext.currentActivity
    if (activity == null) {
      presentation.rejectPendingRequest("No Activity is attached")
      return
    }
    when (mode) {
      PlayerPresentationMode.INLINE -> {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        presentation.confirmMode(mode)
      }
      PlayerPresentationMode.FULLSCREEN -> {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        presentation.confirmMode(mode)
      }
      PlayerPresentationMode.FLOATING -> {
        val entered = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
          activity.enterPictureInPictureMode(PictureInPictureParams.Builder().build())
        if (entered) {
          presentation.confirmMode(mode)
        } else {
          presentation.rejectPendingRequest("Picture-in-picture is unavailable")
        }
      }
    }
  }

  private fun emitPlaybackState(state: PlaybackState) {
    emit("topPlaybackStateChange", Arguments.createMap().apply {
      putString("engine", state.engineType.name.lowercase())
      putString("status", state.status.name.lowercase())
      putBoolean("isPlaying", state.isPlaying)
      putDouble("positionMs", state.positionMs.toDouble())
      putDouble("durationMs", state.durationMs.toDouble())
      putDouble("bufferedPositionMs", state.bufferedPositionMs.toDouble())
      putDouble("networkSpeedBytesPerSecond", state.networkSpeedBytesPerSecond.toDouble())
      putDouble("videoWidth", state.videoWidth.toDouble())
      putDouble("videoHeight", state.videoHeight.toDouble())
      putDouble("volume", state.volume.toDouble())
      putDouble("playbackSpeed", state.playbackSpeed.toDouble())
      state.error?.message?.let { putString("error", it) }
    })
  }

  private fun emitPresentationState(state: PlayerPresentationState) {
    emit("topPresentationStateChange", Arguments.createMap().apply {
      putString("currentMode", state.currentMode.name.lowercase())
      putString("displayedMode", state.displayedMode.name.lowercase())
      putString("floatingReturnMode", state.floatingReturnMode.name.lowercase())
      putBoolean("hasPendingRequest", state.hasPendingRequest)
      putBoolean("isClosed", state.isClosed)
      state.lastFailure?.let { putString("lastFailure", it) }
    })
  }

  private fun emit(eventName: String, payload: WritableMap) {
    if (!isAttachedToWindow || id == NO_ID || released) return
    UIManagerHelper.getEventDispatcher(reactContext)?.dispatchEvent(
      VideoMobilePlayerEvent(
        surfaceId = UIManagerHelper.getSurfaceId(this),
        viewId = id,
        name = eventName,
        payload = payload,
      )
    )
  }
}

private fun JSONObject.toVideoSource(): VideoSource {
  val uri = getString("uri")
  val title = optString("title").takeIf(String::isNotBlank)
  if (optString("type") == "local") return VideoSource.local(uri, title)
  val headersObject = optJSONObject("headers")
  val headers = buildMap {
    headersObject?.keys()?.forEach { key -> put(key, headersObject.getString(key)) }
  }
  return VideoSource.network(uri, headers, title)
}

private fun String?.toEngineType(): PlayerEngineType =
  if (this?.lowercase() == "ijk") PlayerEngineType.IJK else PlayerEngineType.MEDIA3

private fun String?.toPresentationMode(): PlayerPresentationMode = when (this?.lowercase()) {
  "fullscreen" -> PlayerPresentationMode.FULLSCREEN
  "floating" -> PlayerPresentationMode.FLOATING
  else -> PlayerPresentationMode.INLINE
}
