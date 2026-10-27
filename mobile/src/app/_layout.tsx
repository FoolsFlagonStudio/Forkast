import { useEffect } from 'react';
import { Stack } from 'expo-router';
import * as SplashScreen from 'expo-splash-screen';
import { StatusBar } from 'expo-status-bar';
import { useFonts } from 'expo-font';
import { fontFiles } from '@/theme/fonts';
import { ThemeProvider } from '@/theme/ThemeProvider';
import { useTheme } from '@/theme/themeContext';

SplashScreen.preventAutoHideAsync();

function RootStack() {
  const { mode } = useTheme();
  return (
    <>
      <StatusBar style={mode === 'dark' ? 'light' : 'dark'} />
      <Stack screenOptions={{ headerShown: false }} />
    </>
  );
}

export default function RootLayout() {
  const [fontsLoaded, fontError] = useFonts(fontFiles);

  useEffect(() => {
    if (fontsLoaded || fontError) SplashScreen.hideAsync();
  }, [fontsLoaded, fontError]);

  // Keep the native splash up until fonts are ready so nothing renders in a fallback font.
  if (!fontsLoaded && !fontError) return null;

  return (
    <ThemeProvider>
      <RootStack />
    </ThemeProvider>
  );
}
