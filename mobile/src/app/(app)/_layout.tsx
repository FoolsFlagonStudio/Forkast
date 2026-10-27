import { Redirect, Stack } from 'expo-router';
import { useAuth } from '@/auth/AuthProvider';

// Everything in (app) requires a session. The tab bar goes here once those screens are designed.
export default function AppLayout() {
  const { status, user } = useAuth();
  if (status === 'restoring') return null;
  if (status !== 'signedIn') return <Redirect href="/login" />;
  if (!user?.onboardingCompleted) return <Redirect href="/onboarding" />;
  return <Stack screenOptions={{ headerShown: false }} />;
}
