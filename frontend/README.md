# Victual Website

The marketing website: general information about the app and links to download it from the app stores.

> **Status:** scaffolded. Theme, fonts, logo and icons are set up; the marketing pages come next from the Figma designs.

## Scope

This is a static site. It does not call the backend or require login. Planned pages:

| Page           | Purpose                                                                     |
| -------------- | --------------------------------------------------------------------------- |
| Home           | What Victual does, key features, screenshots, App Store / Google Play links |
| Privacy Policy | Required by the App Store and Google Play                                   |
| Support        | Contact information and FAQ; the App Store requires a support URL           |

## Tech Stack

| Area       | Technology                                       |
| ---------- | ------------------------------------------------ |
| Build tool | Vite                                             |
| Language   | TypeScript                                       |
| Framework  | React, React Router                              |
| UI         | Ant Design, themed from the shared design tokens |
| Icons      | Lucide (`lucide-react`)                          |
| Fonts      | Poppins (UI), DM Sans (brand) via Fontsource     |
| Hosting    | To be decided                                    |

## Setup

Requires Node 20.19+ (22 recommended).

```bash
cd frontend
npm install
npm run dev      # http://localhost:5173
npm run build    # typecheck + production build to dist/
npm run lint
```

Open `/styleguide` to check colors, type, logo and icons against the Figma. It's a dev reference and should be removed before launch.

## How Styling Works

- **Design tokens** (colors, type, spacing, radii) live in `../design/tokens.json`. `src/theme/tokens.ts` and `src/theme/tokens.css` are generated from it, so don't edit them by hand. After changing the JSON, run `node design/build-tokens.mjs` from the repo root.
- **Ant Design** gets its theme from `src/theme/antdTheme.ts`, which maps the tokens onto AntD's token names. Buttons, inputs and other AntD components pick up the brand colors and Poppins automatically.
- **CSS variables** are available everywhere, e.g. `var(--color-primary)`, `var(--space-lg)`, `var(--radius-md)`. Type styles are classes: `type-h1`, `type-h2`, `type-h3`, `type-body`, `type-small`, `type-caption`, `type-brand`.
- **Light/dark**: follows the system by default. `useTheme()` from `src/theme/themeContext.ts` exposes `mode` and `setPreference('light' | 'dark' | 'system')`.

## Logo and Icons

```tsx
import { Logo } from './brand/Logo';
import { Icon, icons } from './theme/icons';

<Logo />                                  // lockup, colors follow the theme
<Logo variant="mark" height={32} />       // also 'wordmark'; tone="mono"; surface="dark"
<Icon.groceries size={24} />              // names from the Figma icon sheet
<icons.tabBar.groceries size={24} />      // same icon, grouped
```

Icon names come from `../design/icons.json`. To add or swap an icon, edit that file and rerun the token build.
