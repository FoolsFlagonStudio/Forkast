import { View } from 'react-native';
import { Link } from 'expo-router';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useAuth } from '@/auth/AuthProvider';
import { Logo } from '@/brand/Logo';
import { Button } from '@/components/Button';
import { Text } from '@/components/Text';
import { useTheme } from '@/theme/themeContext';

// Placeholder landing after sign-in until onboarding and the main tabs are designed.
export default function Home() {
  const { user, signOut } = useAuth();
  const { colors } = useTheme();

  return (
    <SafeAreaView style={{ flex: 1, backgroundColor: colors.background }}>
      <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center', gap: 16, padding: 24 }}>
        <Logo variant="mark" tone="accent" height={64} />
        <Text variant="h2" style={{ textAlign: 'center' }}>
          Hi, {user?.firstName}
        </Text>
        <Text variant="bodySmall" style={{ textAlign: 'center' }}>
          {user?.onboardingCompleted ? 'Your meal plan will live here.' : 'Onboarding comes next.'}
        </Text>
        <Button label="Log out" onPress={signOut} />
        <Link href="/style-guide" style={{ marginTop: 8 }}>
          <Text variant="fine" style={{ textDecorationLine: 'underline' }}>
            Style guide
          </Text>
        </Link>
      </View>
    </SafeAreaView>
  );
}
