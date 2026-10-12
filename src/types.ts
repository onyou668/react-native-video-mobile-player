import type { ViewProps } from 'react-native';

export type VideoPlayerEngine = 'media3' | 'ijk';
export type VideoPresentationMode = 'inline' | 'fullscreen' | 'floating';
export type VideoSource = Readonly<{
  uri: string;
  type?: 'network' | 'local';
  headers?: Readonly<Record<string, string>>;
  title?: string;
}>;

export type VideoPlayerState = Readonly<{
  engine: string;
  status: string;
  isPlaying: boolean;
  positionMs: number;
  durationMs: number;
  bufferedPositionMs: number;
  networkSpeedBytesPerSecond: number;
  videoWidth: number;
  videoHeight: number;
  volume: number;
  playbackSpeed: number;
  error?: string;
}>;

export type VideoPresentationState = Readonly<{
  currentMode: string;
  displayedMode: string;
  floatingReturnMode: string;
  hasPendingRequest: boolean;
  isClosed: boolean;
  lastFailure?: string;
}>;

export interface VideoMobilePlayerProps extends ViewProps {
  source: VideoSource;
  autoPlay?: boolean;
  initialEngine?: VideoPlayerEngine;
  startupMode?: VideoPresentationMode;
  showEngineSwitcher?: boolean;
  showPresentationControls?: boolean;
  showNetworkSpeedWhileLoading?: boolean;
  onPlaybackStateChange?: (state: VideoPlayerState) => void;
  onPresentationStateChange?: (state: VideoPresentationState) => void;
  onCloseRequest?: () => void;
}

export interface VideoMobilePlayerRef {
  play(): void;
  pause(): void;
  togglePlayPause(): void;
  seekTo(positionMs: number): void;
  seekBy(offsetMs: number): void;
  setVolume(volume: number): void;
  setPlaybackSpeed(speed: number): void;
  switchEngine(engine: VideoPlayerEngine): void;
  retry(): void;
  requestMode(mode: VideoPresentationMode): void;
  returnFromFloating(): void;
  syncPresentationMode(mode: VideoPresentationMode): void;
  close(): void;
}
