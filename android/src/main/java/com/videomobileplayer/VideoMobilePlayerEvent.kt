package com.videomobileplayer

import com.facebook.react.bridge.WritableMap
import com.facebook.react.uimanager.events.Event

internal class VideoMobilePlayerEvent(
  surfaceId: Int,
  viewId: Int,
  private val name: String,
  private val payload: WritableMap,
) : Event<VideoMobilePlayerEvent>(surfaceId, viewId) {
  override fun getEventName(): String = name

  override fun getEventData(): WritableMap = payload
}
