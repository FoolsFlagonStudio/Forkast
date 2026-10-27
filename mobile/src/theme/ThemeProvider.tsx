import { useMemo, useState, type ReactNode } from 'react';
import { useColorScheme } from 'react-native';
import { DarkTheme, DefaultTheme, ThemeProvider as NavThemeProvider } from 'expo-router';
import { tokens } from './tokens';
import { fontFamily } from './fonts';
import { ThemeContext, type Theme, type ThemePreference } from './themeContext';

// TODO: persist `preference` once the Settings screen exists (Light / Dark / System).
export function ThemeProvider({ children }: { children: ReactNode }) {
  const system = useColorScheme();
  const [preference, setPreference] = useState<ThemePreference>('system');
  const mode = preference === 'system' ? (system === 'dark' ? 'dark' : 'light') : preference;

  const theme = useMemo<Theme>(
    () => ({
      mode,
      colors: tokens.colors[mode],
      palette: tokens.palette,
      spacing: tokens.spacing,
      radius: tokens.radius,
      preference,
      setPreference,
    }),
    [mode, preference],
  );

  // Keeps React Navigation's headers, tab bar and screen backgrounds on-palette.
  const navTheme = useMemo(() => {
    const base = mode === 'dark' ? DarkTheme : DefaultTheme;
    const c = tokens.colors[mode];
    return {
      ...base,
      colors: {
        ...base.colors,
        primary: c.primary,
        background: c.background,
        card: c.background,
        text: c.textStrong,
        border: c.border,
        notification: c.secondary,
      },
      fonts: {
        regular: { fontFamily: fontFamily('ui', 400), fontWeight: '400' as const },
        medium: { fontFamily: fontFamily('ui', 500), fontWeight: '500' as const },
        bold: { fontFamily: fontFamily('ui', 600), fontWeight: '600' as const },
        heavy: { fontFamily: fontFamily('ui', 700), fontWeight: '700' as const },
      },
    };
  }, [mode]);

  return (
    <ThemeContext.Provider value={theme}>
      <NavThemeProvider value={navTheme}>{children}</NavThemeProvider>
    </ThemeContext.Provider>
  );
}
