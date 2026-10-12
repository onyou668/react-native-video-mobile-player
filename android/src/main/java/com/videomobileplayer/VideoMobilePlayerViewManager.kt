package com.videomobileplayer

import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.ViewManagerDelegate
import com.facebook.react.uimanager.annotations.ReactProp
import com.facebook.react.viewmanagers.VideoMobilePlayerViewManagerDelegate
import com.facebook.react.viewmanagers.VideoMobilePlayerViewManagerInterface

@ReactModule(name = VideoMobilePlayerViewManager.NAME)
class VideoMobilePlayerViewManager : SimpleViewManager<VideoMobilePlayerView>(),
  VideoMobilePlayerViewManagerInterface<VideoMobilePlayerView> {
  private val delegate: ViewManagerDelegate<VideoMobilePlayerView> =
    VideoMobilePlayerViewManagerDelegate(this)

  override fun getDelegate(): ViewManagerDelegate<VideoMobilePlayerView> = delegate
  override fun getName(): String = NAME

  public override fun createViewInstance(context: ThemedReactContext): VideoMobilePlayerView =
    VideoMobilePlayerView(context)

  override fun onDropViewInstance(view: VideoMobilePlayerView) {
    view.release()
    super.onDropViewInstance(view)
  }

  @ReactProp(name = "configJson")
  override fun setConfigJson(view: VideoMobilePlayerView?, value: String?) {
    view?.setConfigJson(value)
  }

  override fun play(view: VideoMobilePlayerView?) { view?.play() }
  override fun pause(view: VideoMobilePlayerView?) { view?.pause() }
  override fun togglePlayPause(view: VideoMobilePlayerView?) { view?.togglePlayPause() }
  override fun seekTo(view: VideoMobilePlayerView?, positionMs: Double) { view?.seekTo(positionMs) }
  override fun seekBy(view: VideoMobilePlayerView?, offsetMs: Double) { view?.seekBy(offsetMs) }
  override fun setVolume(view: VideoMobilePlayerView?, volume: Double) { view?.setVolume(volume) }
  override fun setPlaybackSpeed(view: VideoMobilePlayerView?, speed: Double) {
    view?.setPlaybackSpeed(speed)
  }
  override fun switchEngine(view: VideoMobilePlayerView?, engine: String?) {
    view?.switchEngine(engine)
  }
  override fun retry(view: VideoMobilePlayerView?) { view?.retry() }
  override fun requestMode(view: VideoMobilePlayerView?, mode: String?) { view?.requestMode(mode) }
  override fun returnFromFloating(view: VideoMobilePlayerView?) { view?.returnFromFloating() }
  override fun syncPresentationMode(view: VideoMobilePlayerView?, mode: String?) {
    view?.syncPresentationMode(mode)
  }
  override fun close(view: VideoMobilePlayerView?) { view?.closePlayer() }

  companion object {
    const val NAME = "VideoMobilePlayerView"
  }
}
