---
name: sanctuary-ui
description: >
  Create React pages and components using the Sanctuary UI design system — a calm, editorial wellness aesthetic
  with glass morphism, large rounded cards, serif italic headings, and blue primary accents.
  Use this skill whenever building health/wellness dashboards, profile pages, activity trackers,
  chat interfaces, or any UI that needs a premium, tranquil feel. Also use for UI design questions
  about this style, converting existing components to Sanctuary style, or generating design tokens.
---

# Sanctuary UI Design System

A React + Tailwind CSS v4 design system for creating calm, editorial wellness interfaces.

## Tech Stack Requirements

- **React 18+** with TypeScript
- **Tailwind CSS v4** with `@theme` configuration
- **motion** (formerly framer-motion) for animations
- **lucide-react** for icons

## Setup

When creating a new project or adding Sanctuary style to an existing one, ensure this Tailwind config exists in `index.css`:

```css
@import "tailwindcss";
@import url('https://fonts.googleapis.com/css2?family=Newsreader:ital,opsz,wght@0,6..72,200..800;1,6..72,200..800&family=Plus+Jakarta+Sans:wght@200..800&display=swap');

@theme {
  --font-sans: "Plus Jakarta Sans", ui-sans-serif, system-ui, sans-serif;
  --font-serif: "Newsreader", serif;

  --color-primary: #0059b6;
  --color-primary-container: #68a0ff;
  --color-secondary-fixed: #8ce1f3;
  --color-surface: #f4f7f9;
  --color-surface-container-low: #edf1f3;
  --color-surface-container-lowest: #ffffff;
  --color-on-surface: #2b2f31;
  --color-on-surface-variant: #585c5e;
  --color-inverse-surface: #0b0f10;

  --radius-xl: 1.5rem;
  --radius-2xl: 2rem;
  --radius-3xl: 3rem;
}

@layer base {
  body {
    @apply bg-surface text-on-surface font-sans antialiased;
  }
}

.glass-card {
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(20px);
}

.vibrant-shadow {
  box-shadow: 0 20px 40px -15px rgba(0, 89, 182, 0.15);
}

.no-scrollbar::-webkit-scrollbar {
  display: none;
}

.no-scrollbar {
  -ms-overflow-style: none;
  scrollbar-width: none;
}
```

## Design Principles

### Typography

Sanctuary uses a dual-font system that creates editorial contrast:

- **Headings**: `font-serif italic` — Newsreader serif in italic gives the calm, magazine-like quality
  - Hero text: `text-4xl sm:text-5xl lg:text-7xl font-serif italic`
  - Section titles: `text-3xl font-serif italic`
  - Card titles: `font-serif text-2xl`
- **Body/Labels**: `font-sans` — Plus Jakarta Sans for readability
  - Small labels: `text-[10px] font-bold uppercase tracking-widest`
  - Status badges: `text-[10px] font-bold uppercase tracking-[0.25em]`
  - Body text: `text-sm leading-relaxed`

The `tracking-widest` (0.1em) or `tracking-[0.2em]` on uppercase labels is a signature Sanctuary pattern.

### Color Palette

| Token | Hex | Usage |
|---|---|---|
| `primary` | #0059b6 | Buttons, accents, active states |
| `primary-container` | #68a0ff | Gradient endpoints |
| `secondary-fixed` | #8ce1f3 | Decorative glows, gradients |
| `surface` | #f4f7f9 | Page background |
| `surface-container-low` | #edf1f3 | Secondary cards |
| `surface-container-lowest` | #ffffff | Primary cards, modals |
| `on-surface` | #2b2f31 | Main text |
| `on-surface-variant` | #585c5e | Secondary text |
| `inverse-surface` | #0b0f10 | Dark cards, bottom nav |

Accent colors (not in theme, use Tailwind directly):
- **Positive/trends**: `text-lime-400`, `bg-lime-100`
- **Warning/energy**: `text-orange-500`, `bg-orange-50`
- **Info/rest**: `text-cyan-600`, `bg-cyan-100`
- **Danger**: `text-red-500`, `bg-red-50`

### Card Patterns

Sanctuary has three card variants:

**1. Light Card** (most common):
```tsx
<div className="bg-surface-container-lowest rounded-[2rem] sm:rounded-[3rem] p-8 border border-white shadow-sm">
```

**2. Dark Card** (for contrast/stats):
```tsx
<div className="bg-inverse-surface text-white rounded-[2.5rem] sm:rounded-[3rem] p-10">
```

**3. Glass Card** (overlays/sidebars):
```tsx
<div className="bg-white/60 backdrop-blur-xl rounded-3xl border border-white shadow-sm">
```

Key card rules:
- Border radius: always `rounded-[2rem]` to `rounded-[3rem]` — never use small radii
- Light cards get `border border-white` for a subtle edge
- Dark cards don't need borders but get decorative blurred glows: `absolute top-0 right-0 w-32 h-32 bg-lime-400/10 rounded-full blur-3xl -mr-10 -mt-10`
- Hover lift: `whileHover={{ y: -8 }}` or `hover:-translate-y-0.5`
- Add `vibrant-shadow` class to hero cards for the blue-tinted shadow

### Decorative Glows

Every card should have a soft blurred circle for depth. Position it absolutely in a corner:

```tsx
<div className="absolute top-0 right-0 w-64 h-64 bg-secondary-fixed/10 rounded-full blur-3xl -mr-20 -mt-20 group-hover:bg-secondary-fixed/20 transition-colors duration-700" />
```

Match the glow color to the card's accent:
- Blue cards: `bg-secondary-fixed/10` or `bg-primary/10`
- Green cards: `bg-lime-400/10`
- Dark cards: `bg-lime-500/10`
- Red cards: `bg-red-500/5`

### Motion & Animation

Use `motion/react` for all transitions. Key patterns:

**Page transitions:**
```tsx
<motion.div
  initial={{ opacity: 0, y: 20 }}
  animate={{ opacity: 1, y: 0 }}
  exit={{ opacity: 0, y: -20 }}
  transition={{ duration: 0.4 }}
>
```

**Card entrance stagger:**
```tsx
<motion.div
  initial={{ opacity: 0, scale: 0.98 }}
  animate={{ opacity: 1, scale: 1 }}
  transition={{ delay: 0.1 }}
>
```

**Hover effects:**
```tsx
<motion.div whileHover={{ y: -8 }}>
```

**Progress bars:**
```tsx
<motion.div
  initial={{ width: 0 }}
  animate={{ width: "84%" }}
  className="h-full bg-primary rounded-full"
/>
```

**Bar charts (animated height):**
```tsx
{data.map((value, i) => (
  <motion.div
    key={i}
    initial={{ height: 0 }}
    animate={{ height: `${value}%` }}
    transition={{ delay: i * 0.05, type: "spring", stiffness: 100 }}
  />
))}
```

### Bottom Navigation

Sanctuary uses a floating pill-shaped bottom nav:

```tsx
<motion.nav className="fixed bottom-8 left-1/2 -translate-x-1/2 w-[90%] max-w-md bg-inverse-surface/90 backdrop-blur-2xl rounded-full px-8 py-4 z-50 flex justify-between items-center shadow-2xl shadow-primary/10 border border-white/10">
```

Active state: `text-cyan-400 scale-110` with an `absolute -bottom-2 w-1 h-1 bg-cyan-400 rounded-full` indicator.

Inactive state: `text-white/40 hover:text-white`.

### Buttons

**Primary CTA:**
```tsx
<button className="px-10 py-4 rounded-full bg-gradient-to-r from-primary-container to-primary text-white font-bold text-sm tracking-widest shadow-lg shadow-primary/20 hover:shadow-primary/40 hover:-translate-y-0.5 active:translate-y-0 transition-all uppercase">
```

**Ghost button:**
```tsx
<button className="px-6 py-2 rounded-full border border-black/10 text-[10px] font-bold uppercase tracking-widest hover:bg-black/5 transition-colors">
```

**Icon button (in header):**
```tsx
<button className="p-2 hover:bg-black/5 rounded-full transition-colors text-on-surface-variant">
```

### Stat Display Pattern

Large numbers use `font-serif italic`:

```tsx
<span className="text-4xl sm:text-5xl font-bold tracking-tight">8,432</span>
<span className="text-on-surface-variant text-sm mb-2 font-medium">/ 10k steps</span>
```

For hero stats in dark cards:
```tsx
<div className="text-5xl sm:text-6xl font-serif italic text-lime-400">1,240</div>
<div className="text-[10px] text-white/40 uppercase tracking-[0.2em] mt-3 font-bold">Calories remaining</div>
```

### List Items

Activity/ritual list items use horizontal cards with hover slide:

```tsx
<motion.div
  whileHover={{ x: 10 }}
  className="bg-white/60 backdrop-blur-sm rounded-3xl p-6 flex items-center justify-between border border-white shadow-sm hover:shadow-md transition-all cursor-pointer group"
>
```

## Page Structure Templates

### Dashboard Page
1. Hero greeting section (serif italic, large)
2. Main bento grid (8-col + 4-col layout)
3. Activity cards grid (3 columns)
4. Featured content section (recipe, article, etc.)

### Profile Page
1. Centered avatar with award badge
2. Stats row (3 columns)
3. Settings list (rounded container with icon rows)
4. Logout button at bottom

### Activity Page
1. Header with description
2. Metric cards (2-column grid)
3. Dark progress card (full width)
4. Recent items list

### Chat Page
1. Collapsible sidebar with chat groups
2. Message area with avatars
3. AI messages styled differently (italic, serif, gradient bg)
4. Pill-shaped input at bottom

## Responsive Breakpoints

- Mobile-first approach
- `sm:` — 640px (add padding, adjust grid cols)
- `lg:` — 1024px (full bento grid layouts)
- Use `sm:rounded-[3rem]` to increase radius on larger screens

## What NOT to Do

- Never use small border radius (`rounded-lg`, `rounded-md`) on cards
- Never use non-serif fonts for headings
- Never skip the decorative glow blobs on cards
- Never use harsh drop shadows — always use soft `shadow-sm` or `shadow-md`
- Never use bright/saturated background colors — keep everything muted and calm
- Never forget `motion` for page transitions — static page changes feel wrong

## Creating Components

When asked to create a Sanctuary-style component:

1. Determine which card variant fits (light, dark, glass)
2. Use proper typography hierarchy (serif italic for titles)
3. Add decorative glow blob
4. Wrap in `motion.div` with appropriate animation
5. Use the theme color tokens, not hardcoded hex values
6. Include hover states with lift or scale effects
7. Keep spacing generous (`p-8`, `p-10`, `gap-6`, `gap-8`)
