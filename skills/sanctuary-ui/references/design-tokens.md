# Sanctuary UI Design Tokens Reference

## Color Tokens (Tailwind @theme)

| CSS Variable | Tailwind Token | Hex | Description |
|---|---|---|---|
| `--color-primary` | `primary` | #0059b6 | Main brand blue |
| `--color-primary-container` | `primary-container` | #68a0ff | Lighter blue for gradients |
| `--color-secondary-fixed` | `secondary-fixed` | #8ce1f3 | Cyan accent for glows |
| `--color-surface` | `surface` | #f4f7f9 | Page background |
| `--color-surface-container-low` | `surface-container-low` | #edf1f3 | Secondary card bg |
| `--color-surface-container-lowest` | `surface-container-lowest` | #ffffff | Primary card bg |
| `--color-on-surface` | `on-surface` | #2b2f31 | Primary text |
| `--color-on-surface-variant` | `on-surface-variant` | #585c5e | Secondary text |
| `--color-inverse-surface` | `inverse-surface` | #0b0f10 | Dark elements |

## Accent Colors (Direct Tailwind)

| Purpose | Text | Background |
|---|---|---|
| Positive/Success | `text-lime-400` | `bg-lime-100` |
| Energy/Warning | `text-orange-500` | `bg-orange-50` |
| Info/Rest | `text-cyan-600` | `bg-cyan-100` |
| Danger/Error | `text-red-500` | `bg-red-50` |
| Heart Rate | `text-red-500` | `bg-red-50` |

## Typography Scale

| Element | Classes |
|---|---|
| Hero heading | `text-4xl sm:text-5xl lg:text-7xl font-serif italic` |
| Page heading | `text-4xl sm:text-5xl font-serif italic` |
| Section title | `text-3xl font-serif italic` |
| Card title | `font-serif text-xl` or `font-serif text-2xl` |
| Body text | `text-sm leading-relaxed` or `text-base` |
| Small label | `text-[10px] font-bold uppercase tracking-widest` |
| Wide label | `text-[10px] font-bold uppercase tracking-[0.2em]` |
| Extra wide | `text-[10px] font-bold uppercase tracking-[0.25em]` |
| Timestamp | `text-[9px] text-on-surface-variant/60 font-medium` |

## Border Radius

| Context | Classes |
|---|---|
| Large card | `rounded-[2rem] sm:rounded-[3rem]` |
| Medium card | `rounded-[2.5rem]` |
| Small card | `rounded-3xl` (1.5rem) |
| Button | `rounded-full` |
| Icon container | `rounded-2xl` |
| Badge | `rounded-full` |
| Avatar | `rounded-full` or `rounded-[3rem]` |

## Spacing

| Context | Value |
|---|---|
| Card padding | `p-8` or `p-10` or `sm:p-10` |
| Section gap | `space-y-8` or `space-y-12` |
| Grid gap | `gap-6` or `gap-8` |
| Icon to text | `gap-4` or `gap-6` |
| Card internal | `space-y-6` or `space-y-8` |

## Shadow

| Type | Class |
|---|---|
| Subtle | `shadow-sm` |
| Standard | `shadow-md` |
| Blue glow | `vibrant-shadow` |
| Dark nav | `shadow-2xl shadow-primary/10` |

## Glass Effects

```css
/* Light glass */
bg-white/60 backdrop-blur-xl

/* Dark glass */
bg-white/40 backdrop-blur-md

/* Input glass */
bg-white/50 border border-black/5

/* Nav glass */
bg-inverse-surface/90 backdrop-blur-2xl
```

## Animation Timing

| Effect | Duration |
|---|---|
| Page transition | `duration: 0.4` |
| Card entrance | `duration: 0.3`, delay by index |
| Hover lift | `transition-all` (~300ms) |
| Glow color shift | `duration-700` |
| Progress bar | `duration: 1.5, ease: "easeOut"` |
| Bar chart | `delay: i * 0.05, type: "spring", stiffness: 100` |
