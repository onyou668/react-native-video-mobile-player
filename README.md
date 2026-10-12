# react-native-video-mobile-player

React Native Android Fabric 视频播放器，底层复用 `video-mobile-player` 的 Media3、IjkPlayer、Jetpack Compose UI 和展示模式控制。

当前只支持 Android，要求 React Native 新架构。播放器源码通过 Git submodule 固定在 `android/video-mobile-player`。

## 获取源码

```shell
git clone --recurse-submodules https://github.com/onyou668/react-native-video-mobile-player.git
cd react-native-video-mobile-player
yarn install
```

已有仓库初始化 submodule：

```shell
git submodule update --init --recursive
```

## 安装

```shell
yarn add react-native-video-mobile-player
```

使用 Git dependency 时，包管理器不保证初始化 submodule。正式发布 npm 包前必须确保 `android/video-mobile-player` 已检出，npm 包会包含对应源码快照。

宿主 Android 工程需要启用新架构：

```properties
newArchEnabled=true
```

## 使用

```tsx
import React, { useRef } from 'react';
import {
  VideoMobilePlayerView,
  type VideoMobilePlayerRef,
} from 'react-native-video-mobile-player';

export function Player() {
  const player = useRef<VideoMobilePlayerRef>(null);

  return (
    <VideoMobilePlayerView
      ref={player}
      source={{
        uri: 'https://example.com/video.mp4',
        headers: { Authorization: 'Bearer token' },
        title: 'Video',
      }}
      autoPlay
      initialEngine="media3"
      startupMode="inline"
      style={{ width: '100%', aspectRatio: 16 / 9 }}
      onPlaybackStateChange={(state) => {
        console.log(state.positionMs);
        console.log(state.networkSpeedBytesPerSecond);
      }}
    />
  );
}
```

本地文件使用 Android `content://` URI：

```tsx
<VideoMobilePlayerView
  ref={player}
  source={{ uri: contentUri, type: 'local', title: 'Local video' }}
  style={{ width: '100%', aspectRatio: 16 / 9 }}
/>
```

## 命令

```tsx
player.current?.play();
player.current?.pause();
player.current?.seekTo(30_000);
player.current?.seekBy(10_000);
player.current?.setVolume(0.5);
player.current?.setPlaybackSpeed(1.5);
player.current?.switchEngine('ijk');
player.current?.requestMode('fullscreen');
player.current?.requestMode('floating');
player.current?.returnFromFloating();
player.current?.close();
```

## 事件

- `onPlaybackStateChange`：进度、缓冲、播放状态、内核、音量、倍速、视频尺寸、错误和实时网速。
- `onPresentationStateChange`：页面内、全屏、悬浮、悬浮返回目标和失败信息。
- `onCloseRequest`：原生控制栏请求宿主关闭播放页面或会话。

## 扩展

弹幕、字幕、手势层等业务 UI 建议作为播放器上方的 React Native 兄弟视图实现，不需要修改播放器内核：

```tsx
<View style={{ aspectRatio: 16 / 9 }}>
  <VideoMobilePlayerView
    ref={player}
    source={source}
    style={StyleSheet.absoluteFill}
    onPlaybackStateChange={setPlaybackState}
  />
  <DanmakuOverlay
    style={StyleSheet.absoluteFill}
    positionMs={playbackState?.positionMs ?? 0}
    isPlaying={playbackState?.isPlaying ?? false}
  />
</View>
```

需要扩展原生 Compose 控制栏、播放内核或展示模式时，在 `android/video-mobile-player` 主库中实现并更新 submodule 指针；TypeScript 层只维护稳定的命令和状态协议，避免业务 UI 与 Media3/IjkPlayer 直接耦合。

## PiP

宿主 Activity 需要声明：

```xml
<activity
    android:name=".MainActivity"
    android:configChanges="screenSize|smallestScreenSize|screenLayout|orientation"
    android:resizeableActivity="true"
    android:supportsPictureInPicture="true" />
```

PiP 点击展开后，宿主可根据最后一次 `onPresentationStateChange` 返回的 `floatingReturnMode` 同步状态：

```tsx
player.current?.syncPresentationMode(lastPresentation.floatingReturnMode);
```

## 更新底层播放器

```shell
git submodule update --remote android/video-mobile-player
git add android/video-mobile-player
git commit -m "chore: update native player"
```

## 验证

```shell
yarn typecheck
yarn lint
yarn prepare
cd example/android
./gradlew assembleDebug
```
