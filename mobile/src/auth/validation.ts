// Client-side checks that mirror the backend's @Valid rules (SignupRequest etc.)
// so most mistakes are caught before a round trip.

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export const validate = {
  required: (v: string, label: string) => (v.trim() ? null : `${label} is required.`),
  name: (v: string, label: string) =>
    !v.trim() ? `${label} is required.` : v.trim().length > 100 ? `${label} must be 100 characters or fewer.` : null,
  email: (v: string) =>
    !v.trim() ? 'Email is required.' : EMAIL.test(v.trim()) ? null : 'Enter a valid email address.',
  password: (v: string) =>
    !v ? 'Password is required.' : v.length < 8 ? 'Use at least 8 characters.' : v.length > 72 ? 'Use 72 characters or fewer.' : null,
  match: (v: string, other: string) => (v === other ? null : "Passwords don't match."),
  code: (v: string) => (/^\d{6}$/.test(v) ? null : 'Enter the 6-digit code.'),
};

/** Returns only the fields that have an error. */
export function collectErrors<K extends string>(checks: Record<K, string | null>) {
  return Object.fromEntries(Object.entries(checks).filter(([, v]) => v)) as Partial<Record<K, string>>;
}
