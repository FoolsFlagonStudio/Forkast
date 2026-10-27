// AUTO-GENERATED from design/tokens.json - do not edit by hand.
// Run `node design/build-tokens.mjs` from the repo root to regenerate.

export const tokens = {
  "palette": {
    "backgroundLight": "#F7F2EE",
    "backgroundDark": "#3C3C3C",
    "mauve": "#C59DB0",
    "plum": "#3D2433",
    "sage": "#9BC59D",
    "forest": "#1F3326",
    "black": "#000000",
    "white": "#FFFFFF"
  },
  "colors": {
    "light": {
      "background": "#F7F2EE",
      "surface": "#FFFFFF",
      "text": "#3C3C3C",
      "textStrong": "#1F3326",
      "primary": "#1F3326",
      "onPrimary": "#F7F2EE",
      "accent": "#9BC59D",
      "onAccent": "#1F3326",
      "secondary": "#C59DB0",
      "onSecondary": "#3D2433",
      "highlight": "#3D2433",
      "border": "#D9D2CC",
      "muted": "#7A7470"
    },
    "dark": {
      "background": "#3C3C3C",
      "surface": "#474747",
      "text": "#F7F2EE",
      "textStrong": "#FFFFFF",
      "primary": "#9BC59D",
      "onPrimary": "#1F3326",
      "accent": "#C59DB0",
      "onAccent": "#3D2433",
      "secondary": "#C59DB0",
      "onSecondary": "#3D2433",
      "highlight": "#C59DB0",
      "border": "#5A5A5A",
      "muted": "#B3ADA8"
    }
  },
  "fonts": {
    "brand": "DM Sans",
    "ui": "Poppins"
  },
  "typography": {
    "brand": {
      "font": "brand",
      "weight": 600,
      "size": 32,
      "lineHeight": 40
    },
    "h1": {
      "font": "ui",
      "weight": 700,
      "size": 32,
      "lineHeight": 40
    },
    "h2": {
      "font": "ui",
      "weight": 600,
      "size": 28,
      "lineHeight": 36
    },
    "h3": {
      "font": "ui",
      "weight": 500,
      "size": 20,
      "lineHeight": 28
    },
    "body": {
      "font": "ui",
      "weight": 400,
      "size": 16,
      "lineHeight": 24
    },
    "small": {
      "font": "ui",
      "weight": 300,
      "size": 12,
      "lineHeight": 18
    },
    "caption": {
      "font": "ui",
      "weight": 700,
      "size": 12,
      "lineHeight": 16
    }
  },
  "spacing": {
    "xxs": 2,
    "xs": 4,
    "sm": 8,
    "md": 12,
    "lg": 16,
    "xl": 24,
    "xxl": 32,
    "xxxl": 48
  },
  "radius": {
    "sm": 6,
    "md": 10,
    "lg": 16,
    "xl": 24,
    "pill": 999
  }
} as const;

export type ColorMode = keyof typeof tokens.colors;
export type ColorRole = keyof typeof tokens.colors.light;
export type TypeVariant = keyof typeof tokens.typography;
export type Spacing = keyof typeof tokens.spacing;
export type Radius = keyof typeof tokens.radius;
