import type React from 'react';
import type { CodegenTypes, HostComponent, ViewProps } from 'react-native';
import { codegenNativeCommands, codegenNativeComponent } from 'react-native';

export type NativePlaybackState = Readonly<{
  engine: string;
  status: string;
  isPlaying: boolean;
  positionMs: CodegenTypes.Double;
  durationMs: CodegenTypes.Double;
  bufferedPositionMs: CodegenTypes.Double;
  networkSpeedBytesPerSecond: CodegenTypes.Double;
  videoWidth: CodegenTypes.Double;
  videoHeight: CodegenTypes.Double;
  volume: CodegenTypes.Double;
  playbackSpeed: CodegenTypes.Double;
  error?: string;
}>;

export type NativePresentationState = Readonly<{
  currentMode: string;
  displayedMode: string;
  floatingReturnMode: string;
  hasPendingRequest: boolean;
  isClosed: boolean;
  lastFailure?: string;
}>;

export interface NativeProps extends ViewProps {
  configJson: string;
  onPlaybackStateChange?: CodegenTypes.DirectEventHandler<NativePlaybackState>;
  onPresentationStateChange?: CodegenTypes.DirectEventHandler<NativePresentationState>;
  onCloseRequest?: CodegenTypes.DirectEventHandler<
    Readonly<{ isClosed: boolean }>
  >;
}

export type VideoMobilePlayerNativeComponent = HostComponent<NativeProps>;

export default codegenNativeComponent<NativeProps>(
  'VideoMobilePlayerView'
) as VideoMobilePlayerNativeComponent;

interface NativeCommands {
  play(viewRef: React.ElementRef<VideoMobilePlayerNativeComponent>): void;
  pause(viewRef: React.ElementRef<VideoMobilePlayerNativeComponent>): void;
  togglePlayPause(
    viewRef: React.ElementRef<VideoMobilePlayerNativeComponent>
  ): void;
  seekTo(
    viewRef: React.ElementRef<VideoMobilePlayerNativeComponent>,
    positionMs: CodegenTypes.Double
  ): void;
  seekBy(
    viewRef: React.ElementRef<VideoMobilePlayerNativeComponent>,
    offsetMs: CodegenTypes.Double
  ): void;
  setVolume(
    viewRef: React.ElementRef<VideoMobilePlayerNativeComponent>,
    volume: CodegenTypes.Double
  ): void;
  setPlaybackSpeed(
    viewRef: React.ElementRef<VideoMobilePlayerNativeComponent>,
    speed: CodegenTypes.Double
  ): void;
  switchEngine(
    viewRef: React.ElementRef<VideoMobilePlayerNativeComponent>,
    engine: string
  ): void;
  retry(viewRef: React.ElementRef<VideoMobilePlayerNativeComponent>): void;
  requestMode(
    viewRef: React.ElementRef<VideoMobilePlayerNativeComponent>,
    mode: string
  ): void;
  returnFromFloating(
    viewRef: React.ElementRef<VideoMobilePlayerNativeComponent>
  ): void;
  syncPresentationMode(
    viewRef: React.ElementRef<VideoMobilePlayerNativeComponent>,
    mode: string
  ): void;
  close(viewRef: React.ElementRef<VideoMobilePlayerNativeComponent>): void;
}

export const Commands = codegenNativeCommands<NativeCommands>({
  supportedCommands: [
    'play',
    'pause',
    'togglePlayPause',
    'seekTo',
    'seekBy',
    'setVolume',
    'setPlaybackSpeed',
    'switchEngine',
    'retry',
    'requestMode',
    'returnFromFloating',
    'syncPresentationMode',
    'close',
  ],
});
