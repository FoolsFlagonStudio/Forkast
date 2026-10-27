import { useEffect, useMemo, useRef, useState } from 'react';
import { AccessibilityInfo, Animated, Easing, StyleSheet, View } from 'react-native';
import Svg, { Path } from 'react-native-svg';
import { brandGeometry as g } from './geometry';
import { tokens } from '@/theme/tokens';

// The bowl mark with its steam animated: each wisp rises, sways and fades on its
// own loop, offset from its neighbors so the steam reads as continuous.
// Honors the system "Reduce Motion" setting by holding still.

const { mark } = g;
const viewBox = `${mark.crop.x} ${mark.crop.y} ${mark.crop.width} ${mark.crop.height}`;
const wisps = mark.steam.split(/(?=M)/).map((d) => d.trim());
const CYCLE_MS = 1800;

export function SteamingMark({ height = 80 }: { height?: number }) {
  const width = (height * mark.crop.width) / mark.crop.height;
  const unit = height / mark.crop.height; // points per mark unit
  const progress = useRef(wisps.map(() => new Animated.Value(0))).current;
  const [reduceMotion, setReduceMotion] = useState(false);

  useEffect(() => {
    AccessibilityInfo.isReduceMotionEnabled().then(setReduceMotion).catch(() => {});
    const sub = AccessibilityInfo.addEventListener('reduceMotionChanged', setReduceMotion);
    return () => sub.remove();
  }, []);

  useEffect(() => {
    if (reduceMotion) return;
    const loops = progress.map((v, i) =>
      Animated.loop(
        Animated.sequence([
          Animated.delay(i * (CYCLE_MS / 3)),
          Animated.timing(v, { toValue: 1, duration: CYCLE_MS, easing: Easing.inOut(Easing.sin), useNativeDriver: true }),
          Animated.timing(v, { toValue: 0, duration: 0, useNativeDriver: true }),
        ]),
      ),
    );
    loops.forEach((l) => l.start());
    return () => {
      loops.forEach((l) => l.stop());
      progress.forEach((v) => v.setValue(0));
    };
  }, [progress, reduceMotion]);

  const styles = useMemo(
    () =>
      progress.map((v) => ({
        opacity: v.interpolate({ inputRange: [0, 0.15, 0.7, 1], outputRange: [0, 1, 0.9, 0] }),
        transform: [
          { translateY: v.interpolate({ inputRange: [0, 1], outputRange: [2.5 * unit, -3 * unit] }) },
          { translateX: v.interpolate({ inputRange: [0, 0.33, 0.66, 1], outputRange: [0, 0.8 * unit, -0.8 * unit, 0] }) },
        ],
      })),
    [progress, unit],
  );

  return (
    <View style={{ width, height }} accessibilityRole="image" accessibilityLabel="Victual">
      <Svg width={width} height={height} viewBox={viewBox} style={StyleSheet.absoluteFill}>
        <Path
          d={mark.bowl}
          fill="none"
          stroke={tokens.palette.sage}
          strokeWidth={mark.bowlWidth}
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </Svg>
      {wisps.map((d, i) => (
        <Animated.View key={i} style={[StyleSheet.absoluteFill, reduceMotion ? null : styles[i]]} pointerEvents="none">
          <Svg width={width} height={height} viewBox={viewBox}>
            <Path d={d} fill="none" stroke={tokens.palette.mauve} strokeWidth={mark.steamWidth} strokeLinecap="round" />
          </Svg>
        </Animated.View>
      ))}
    </View>
  );
}
