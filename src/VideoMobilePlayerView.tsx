import type React from 'react';
import type { VideoMobilePlayerProps, VideoMobilePlayerRef } from './types';

export function VideoMobilePlayerView(
  _props: VideoMobilePlayerProps & { ref?: React.Ref<VideoMobilePlayerRef> }
): never {
  throw new Error(
    "'react-native-video-mobile-player' currently supports Android only."
  );
}
