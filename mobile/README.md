# Victual Mobile

The Victual app for iOS and Android, built with Expo and React Native. This is the user-facing product: preferences, weekly meal plans, recipes, and grocery lists.

> **Status:** scaffolded. Theme, fonts, logo, icons, app icon and splash are set up; the home screen is a temporary style guide. The splash and auth screens come next from the Figma designs.

## Responsibilities

The app talks only to the backend API (see [../backend/README.md](../backend/README.md)). It never connects to the database or any third-party data source directly. All recipe parsing, nutrition, pricing, and meal plan generation happen on the backend.

## Planned Features

| Feature                                                                                                            | Backend routes                                                       |
| ------------------------------------------------------------------------------------------------------------------ | -------------------------------------------------------------------- |
| Sign up, log in, email verification, password reset                                                                | `/api/auth/*`                                                        |
| Onboarding: weekly budget, household size, dietary restrictions, calorie and macro targets                         | `/api/users/me/preferences`                                          |
| Browse and search recipes (with fit flags), scale servings                                                         | `/api/recipes`, `/api/recipes/{id}?servings=`                        |
| Recipe cost: per-serving estimate on cards, `sort=cost`, `maxCost` filter, total for the chosen servings in detail | `/api/recipes`, `/api/recipes/{id}?servings=`                        |
| Favorite recipes                                                                                                   | `PUT`/`DELETE /api/recipes/{id}/favorite`, `/api/users/me/favorites` |
| Generate and view weekly meal plans (calendar view)                                                                | `/api/meal-plans`, `/api/meal-plans/{id}/calendar`                   |
| Grocery list with purchased checkboxes                                                                             | `/api/meal-plans/{id}/grocery-list`, `/api/grocery-list-items/{id}`  |

## Auth Notes

The backend issues short-lived JWT access tokens plus refresh tokens. Tokens should be kept in secure device storage (`expo-secure-store`), not plain async storage.

## Cost Notes

`costPerServing` is an estimate in USD from one representative store, refreshed weekly. When `costComplete` is false, some lines couldn't be priced and the real cost is higher; show it as "from $X" or with an "estimate" label rather than as an exact price. `estimatedCost` in recipe detail is what the chosen servings use, not what a shopping trip costs (the grocery list buys whole packages).

## Tech Stack

| Area         | Technology                                             |
| ------------ | ------------------------------------------------------ |
| Framework    | React Native via Expo (SDK 58)                         |
| Navigation   | Expo Router (file-based, routes in `src/app/`)         |
| Language     | TypeScript                                             |
| Styling      | React Native styles + a theme from the shared tokens   |
| Icons        | Lucide (`lucide-react-native`)                         |
| Fonts        | Poppins (UI), DM Sans (brand) via `@expo-google-fonts` |
| Distribution | App Store and Google Play, built with EAS              |

## Setup

Requires Node 20.19+ (22 recommended).

```bash
cd mobile
npm install
npx expo start         # scan the QR code with Expo Go (iPhone camera / Android Expo Go app)
npm run typecheck
```

Your phone and computer need to be on the same Wi-Fi. If they can't see each other, use `npx expo start --tunnel`.

Use `npx expo install <package>` instead of `npm install` for anything with native code, so you get the version that matches the Expo SDK.

**iOS builds without a Mac:** Expo Go covers day-to-day testing on a real iPhone. For App Store builds (and any native module Expo Go doesn't include), use EAS to build in the cloud: `npx eas-cli@latest build --platform ios`.

## How Styling Works

- **Design tokens** live in `../design/tokens.json`. `src/theme/tokens.ts` and `src/theme/icons.ts` are generated from it, so don't edit them by hand. After changing the JSON, run `node design/build-tokens.mjs` from the repo root.
- **`useTheme()`** (`src/theme/themeContext.ts`) returns `colors` for the current mode plus `spacing`, `radius` and `palette`, and `setPreference('light' | 'dark' | 'system')`.
- **`<Text variant="h1">`** (`src/components/Text.tsx`) applies the Figma type styles with the right font file and theme color. Use it instead of React Native's `Text`.
- **Fonts**: React Native needs one font per weight. `src/theme/fonts.ts` loads them and maps `(family, weight)` to the loaded name. To use a new weight, add it there.
- **Splash**: the app holds the native splash until fonts load, so nothing flashes in a fallback font.

```tsx
const { colors, spacing } = useTheme();
<View style={{ backgroundColor: colors.background, padding: spacing.lg }}>
  <Logo height={40} />
  <Text variant="h2">This week</Text>
  <Icon.groceries size={24} color={colors.primary} />
</View>
```

## App Icon and Splash

Generated from the logo in `../design/brand/` and stored in `assets/images/`: `icon.png` (iOS and store), the Android adaptive icon layers (forest background, mark foreground, monochrome for themed icons), `splash-icon.png` (shown on a forest background), and `favicon.png`.
