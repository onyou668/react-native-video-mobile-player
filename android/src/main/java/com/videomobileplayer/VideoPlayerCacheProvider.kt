package com.videomobileplayer

import android.content.Context
import io.github.mobileplayer.core.AppPrivateVideoPlayerCache

/** One persistent cache seam shared by every React Native player view. */
internal object VideoPlayerCacheProvider {
  @Volatile private var cache: AppPrivateVideoPlayerCache? = null

  fun get(context: Context): AppPrivateVideoPlayerCache =
    cache ?: synchronized(this) {
      cache ?: AppPrivateVideoPlayerCache(context.applicationContext).also { cache = it }
    }
}
