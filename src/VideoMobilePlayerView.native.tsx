import React, { forwardRef, useImperativeHandle, useMemo, useRef } from 'react';
import type { NativeSyntheticEvent } from 'react-native';
import NativeView, {
  Commands,
  type NativePlaybackState,
  type NativePresentationState,
  type VideoMobilePlayerNativeComponent,
} from './VideoMobilePlayerViewNativeComponent';
import type { VideoMobilePlayerProps, VideoMobilePlayerRef } from './types';

export const VideoMobilePlayerView = forwardRef<
  VideoMobilePlayerRef,
  VideoMobilePlayerProps
>(function VideoMobilePlayerViewComponent(
  {
    source,
    startPositionMs,
    autoPlay = true,
    initialEngine = 'media3',
    startupMode = 'inline',
    showEngineSwitcher = true,
    showPresentationControls = true,
    showNetworkSpeedWhileLoading = true,
    onPlaybackStateChange,
    onPresentationStateChange,
    onCloseRequest,
    ...viewProps
  },
  forwardedRef
) {
  const nativeRef =
    useRef<React.ElementRef<VideoMobilePlayerNativeComponent>>(null);
  const configJson = useMemo(
    () =>
      JSON.stringify({
        source,
        startPositionMs,
        autoPlay,
        initialEngine,
        startupMode,
        showEngineSwitcher,
        showPresentationControls,
        showNetworkSpeedWhileLoading,
      }),
    [
      source,
      startPositionMs,
      autoPlay,
      initialEngine,
      startupMode,
      showEngineSwitcher,
      showPresentationControls,
      showNetworkSpeedWhileLoading,
    ]
  );

  useImperativeHandle(forwardedRef, () => ({
    play: () => nativeRef.current && Commands.play(nativeRef.current),
    pause: () => nativeRef.current && Commands.pause(nativeRef.current),
    togglePlayPause: () =>
      nativeRef.current && Commands.togglePlayPause(nativeRef.current),
    seekTo: (positionMs) =>
      nativeRef.current && Commands.seekTo(nativeRef.current, positionMs),
    seekBy: (offsetMs) =>
      nativeRef.current && Commands.seekBy(nativeRef.current, offsetMs),
    setVolume: (volume) =>
      nativeRef.current && Commands.setVolume(nativeRef.current, volume),
    setPlaybackSpeed: (speed) =>
      nativeRef.current && Commands.setPlaybackSpeed(nativeRef.current, speed),
    switchEngine: (engine) =>
      nativeRef.current && Commands.switchEngine(nativeRef.current, engine),
    retry: () => nativeRef.current && Commands.retry(nativeRef.current),
    requestMode: (mode) =>
      nativeRef.current && Commands.requestMode(nativeRef.current, mode),
    returnFromFloating: () =>
      nativeRef.current && Commands.returnFromFloating(nativeRef.current),
    syncPresentationMode: (mode) =>
      nativeRef.current &&
      Commands.syncPresentationMode(nativeRef.current, mode),
    close: () => nativeRef.current && Commands.close(nativeRef.current),
  }));

  return (
    <NativeView
      {...viewProps}
      ref={nativeRef}
      configJson={configJson}
      onPlaybackStateChange={(
        event: NativeSyntheticEvent<NativePlaybackState>
      ) => onPlaybackStateChange?.(event.nativeEvent)}
      onPresentationStateChange={(
        event: NativeSyntheticEvent<NativePresentationState>
      ) => onPresentationStateChange?.(event.nativeEvent)}
      onCloseRequest={() => onCloseRequest?.()}
    />
  );
});
