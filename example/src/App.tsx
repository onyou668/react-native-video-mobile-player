import { useRef } from 'react';
import { SafeAreaView, StyleSheet } from 'react-native';
import {
  VideoMobilePlayerView,
  type VideoMobilePlayerRef,
} from 'react-native-video-mobile-player';

export default function App() {
  const player = useRef<VideoMobilePlayerRef>(null);

  return (
    <SafeAreaView style={styles.screen}>
      <VideoMobilePlayerView
        ref={player}
        source={{
          uri: 'https://media.w3.org/2010/05/sintel/trailer.mp4',
          title: 'Sintel trailer',
          contentCachePolicy: 'disabled',
        }}
        style={styles.player}
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: '#101214' },
  player: { width: '100%', aspectRatio: 16 / 9, backgroundColor: 'black' },
});
