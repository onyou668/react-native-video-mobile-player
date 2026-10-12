# react-native-video-mobile-player

React Native Android Fabric 视频播放器，底层复用 `video-mobile-player` 的 Media3、IjkPlayer、Jetpack Compose UI 和展示模式控制。

当前只支持 Android，要求 React Native 新架构。播放器源码通过 Git submodule 固定在 `android/video-mobile-player`。

插件 `v0.1.0` 固定使用 Android 播放器主库 `v0.1.0`。调用方只需要依赖本插件 tag，不需要直接管理底层主库版本。

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
yarn add https://github.com/onyou668/react-native-video-mobile-player.git#v0.1.0
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
        mediaId: 'movie-42',
        headers: { Authorization: 'Bearer token' },
        title: 'Video',
      }}
      // 非空时优先于本地保存进度；传 0 强制从头播放。
      startPositionMs={10 * 60 * 1000}
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

`mediaId` 是播放进度和视频内容缓存的稳定标识；不传时使用 URI。本地文件只保存进度，不复制视频内容。网络 MP4 等支持 HTTP Range 的渐进式文件会自动边播边缓存，拖动到新位置时只下载对应分段，Media3/Ijk 切换时共用同一份状态和缓存。App 数据清除或卸载后这些数据会被系统删除。

HLS/DASH 不缓存媒体内容。标准 `.m3u8`/`.mpd` URL 会自动绕过；无标准扩展名时显式关闭：

```tsx
<VideoMobilePlayerView
  ref={player}
  source={{
    uri: 'https://example.com/opaque-live-url',
    mediaId: 'live-channel-1',
    contentCachePolicy: 'disabled',
  }}
/>
```

目标 SDK 禁止明文流量时，宿主需要在 Android Network Security Config 中仅允许 `127.0.0.1`/`localhost`，供本地 Range 代理使用；不要全局开放 HTTP。完整配置见底层播放器 [README](android/video-mobile-player/README.md)。

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
git -C android/video-mobile-player fetch --tags
git -C android/video-mobile-player checkout <android-player-tag>
git add android/video-mobile-player
git commit -m "chore: update native player to <android-player-tag>"
```

底层主库变更后，插件必须更新 submodule、通过 CI 并发布新的插件 tag。业务项目始终只依赖插件自己的 tag。

## 验证

```shell
yarn typecheck
yarn lint
yarn prepare
cd example/android
./gradlew assembleDebug
```
