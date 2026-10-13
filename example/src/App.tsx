import { useRef, useState } from 'react';
import { Button, SafeAreaView, StyleSheet, Text, View } from 'react-native';
import {
  VideoMobilePlayerView,
  type VideoMobilePlayerRef,
  type VideoPlayerState,
} from 'react-native-video-mobile-player';

export default function App() {
  const player = useRef<VideoMobilePlayerRef>(null);
  const [state, setState] = useState<VideoPlayerState>();

  return (
    <SafeAreaView style={styles.screen}>
      <VideoMobilePlayerView
        ref={player}
        source={{
          uri: 'https://www.w3schools.com/html/mov_bbb.mp4',
          title: 'Sample video',
          contentCachePolicy: 'disabled',
        }}
        style={styles.player}
        onPlaybackStateChange={setState}
      />
      <Text style={styles.status}>
        {state
          ? `${state.status}  ${Math.floor(state.positionMs / 1000)}s  ${Math.floor(
              state.networkSpeedBytesPerSecond / 1024
            )} KB/s`
          : 'Waiting for player'}
      </Text>
      <View style={styles.controls}>
        <Button title="Play" onPress={() => player.current?.play()} />
        <Button title="Pause" onPress={() => player.current?.pause()} />
        <Button title="+10s" onPress={() => player.current?.seekBy(10_000)} />
        <Button
          title="Fullscreen"
          onPress={() => player.current?.requestMode('fullscreen')}
        />
        <Button
          title="PiP"
          onPress={() => player.current?.requestMode('floating')}
        />
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: '#101214' },
  player: { width: '100%', aspectRatio: 16 / 9, backgroundColor: 'black' },
  status: { color: 'white', padding: 16 },
  controls: { gap: 8, paddingHorizontal: 16 },
});
