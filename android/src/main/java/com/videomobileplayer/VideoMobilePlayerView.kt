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
import io.github.mobileplayer.compose.LandscapeVideoPlayer
import io.github.mobileplayer.compose.VideoPlayerUiConfig
import io.github.mobileplayer.compose.createDefaultVideoPlayerController
import io.github.mobileplayer.core.PlaybackState
import io.github.mobileplayer.core.PlayerEngineType
import io.github.mobileplayer.core.VideoPlayerController
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
  private val player: VideoPlayerController = createDefaultVideoPlayerController(
    context = reactContext,
    scope = scope,
    cache = VideoPlayerCacheProvider.get(reactContext),
  )
  private val fullscreen = mutableStateOf(false)
  private var currentMode = "inline"
  private var floatingReturnMode = "inline"
  private var lastConfigJson: String? = null
  private var released = false
  private var startupApplied = false
  private val uiConfig = mutableStateOf(VideoPlayerUiConfig())

  private val composeView = ComposeView(reactContext).apply {
    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
    setContent {
      LandscapeVideoPlayer(
        controller = player,
        isFullscreen = fullscreen.value,
        onFullscreenChange = { setPresentationMode(if (it) "fullscreen" else "inline") },
        uiConfig = uiConfig.value,
      )
    }
  }

  init {
    addView(composeView, LayoutParams(MATCH_PARENT, MATCH_PARENT))
    observeState()
  }

  fun setConfigJson(value: String?) {
    if (value.isNullOrBlank() || value == lastConfigJson || released) return
    lastConfigJson = value
    val config = JSONObject(value)
    val engine = config.optString("initialEngine").toEngineType()
    if (player.state.value.engineType != engine) player.switchEngine(engine)
    uiConfig.value = config.uiConfig()
    val source = config.getJSONObject("source").toVideoSource()
    player.setMediaIfChanged(
      source,
      playWhenReady = config.optBoolean("autoPlay", true),
      startPositionMs = config.optLong("startPositionMs", 0L),
    )
    if (!startupApplied) {
      startupApplied = true
      setPresentationMode(config.optString("startupMode").normalizedMode())
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
  fun requestMode(mode: String?) = setPresentationMode(mode.normalizedMode())
  fun returnFromFloating() {
    if (currentMode == "floating") setPresentationMode(floatingReturnMode)
  }
  fun syncPresentationMode(mode: String?) = setPresentationMode(mode.normalizedMode())
  fun closePlayer() {
    emit("topCloseRequest", Arguments.createMap().apply { putBoolean("isClosed", true) })
  }

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
    emitPresentationState()
  }

  private fun observeState() {
    scope.launch { player.state.collectLatest(::emitPlaybackState) }
  }

  private fun setPresentationMode(requested: String): Boolean {
    val mode = requested.normalizedMode()
    if (mode == "floating") {
      val activity = reactContext.currentActivity
      val entered = activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        activity.enterPictureInPictureMode(PictureInPictureParams.Builder().build())
      if (!entered) return false
      floatingReturnMode = if (currentMode == "fullscreen") "fullscreen" else "inline"
      fullscreen.value = false
      currentMode = "floating"
    } else {
      fullscreen.value = mode == "fullscreen"
      currentMode = mode
      reactContext.currentActivity?.requestedOrientation = if (mode == "fullscreen") {
        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
      } else {
        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
      }
    }
    emitPresentationState()
    return true
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

  private fun emitPresentationState() {
    emit("topPresentationStateChange", Arguments.createMap().apply {
      putString("currentMode", currentMode)
      putString("displayedMode", currentMode)
      putString("floatingReturnMode", floatingReturnMode)
      putBoolean("hasPendingRequest", false)
      putBoolean("isClosed", false)
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
      ),
    )
  }
}

private fun JSONObject.toVideoSource(): VideoSource {
  val uri = getString("uri")
  val title = optString("title").takeIf(String::isNotBlank)
  if (optString("type") == "local") return VideoSource.local(uri = uri, title = title)
  val headersObject = optJSONObject("headers")
  val headers = buildMap {
    headersObject?.keys()?.forEach { key -> put(key, headersObject.getString(key)) }
  }
  return VideoSource.network(
    url = uri,
    headers = headers,
    title = title,
    contentCachePolicy = if (optString("contentCachePolicy") == "disabled") {
      VideoSource.ContentCachePolicy.DISABLED
    } else VideoSource.ContentCachePolicy.AUTO,
  )
}

private fun JSONObject.uiConfig(): VideoPlayerUiConfig = VideoPlayerUiConfig(
  showEngineSwitcher = optBoolean("showEngineSwitcher", true),
  showNetworkSpeedWhileLoading = optBoolean("showNetworkSpeedWhileLoading", true),
)

private fun String?.toEngineType(): PlayerEngineType =
  if (this?.lowercase() == "ijk") PlayerEngineType.IJK else PlayerEngineType.MEDIA3

private fun String?.normalizedMode(): String = when (this?.lowercase()) {
  "fullscreen" -> "fullscreen"
  "floating" -> "floating"
  else -> "inline"
}
