# AI-food 设计系统与三页深耕 · Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Roll out the "冬日晴空" design system (cold blue + warm accent + glass) into `frontend/` and deep-polish three high-frequency views (Home / Chat / Profile), following the spec at `docs/superpowers/specs/2026-07-04-aifood-design-system-design.md`.

**Architecture:** Single source of design tokens (`frontend/src/styles/_tokens.scss` mirrors `frontend-UI/src/index.css`). Vant 4 styled via 3 CSS variables. Three custom Vue components per page using the token system. All UI work uses CSS variables / Tailwind utility classes — no JavaScript animation library (no framer-motion / @vueuse/motion). Motion uses CSS transitions or the project's existing `animation: sanctuary-*` keyframes.

**Tech Stack:** Vue 3 + Vant 4 + Vite 5 + Sass + Pinia + Vue Router. No test framework installed — verification is via `npm run build`, `npx vue-tsc`, `grep`, and visual screenshots against the preview at `http://localhost:12121/`.

## Global Constraints

These apply to every task. Copy verbatim from the spec:

- **Tone:** Cold blue primary `#4a8dd5` + warm accent `#e8855a` · "winter sky + sunrise"
- **Warm color rule:** ≤ 3 warm elements per screen · ≤ 80×80px or ≤ 32px font size · only hero / CTA / food images / status badges
- **Newsreader italic rule:** NEVER on Chinese text (browser fakes 12° skew). Use `text-h1` (sans bold) for Chinese heroes. Newsreader italic (`text-hero-serif`) for English / pure digits only.
- **Editorial restraint:** Glass effects only on hero / floating menus / top decoration. Dense pages (Chat / Feed) keep `--radius-md` (12px) and tight spacing.
- **Component strategy:** 11 self-written components + Vant retained via CSS variable override (no SCSS variable penetration). 6 over-engineered components removed: `DSButton` / `DSIcon` / `DSAvatar` / `DSBadge` / `DSField` / `DSTextArea` / `DSStack` — replaced by CSS utility classes.
- **Vant override:** 3 CSS variables: `--van-primary-color` / `--van-text-color` / `--van-background`.
- **App.vue pill nav active:** MUST use `var(--color-nav-active)` (= `#22d3ee`). NEVER hardcode.
- **Build verification:** Every task ends with `npm run build` (frontend/) succeeding + 0 TypeScript errors (`npx vue-tsc --noEmit`).
- **Commit format:** `feat(scope): subject` or `fix(scope): subject` per project AGENTS.md. Body explains why.
- **No push to origin** (sandbox network policy). Commit only.
- **Visual ground truth:** Preview at `http://localhost:12121/` (currently running). User checks visual fidelity there before approving each phase.

---

# Phase 1 · Foundation (tokens落地 · ~2-3h)

**Objective:** Replace Material 3 token names with new design system tokens. Establish single source of truth at `frontend/src/styles/_tokens.scss`. No visual changes yet.

## Task 1.1 · Audit existing token usage in `frontend/src/`

**Files:**
- Read: `frontend/src/assets/styles/main.scss`
- Read: 35 Vue/SCSS files referencing old tokens (grep result above)

**Interfaces:**
- Consumes: nothing (read-only audit)
- Produces: List of old token names + their replacement mappings

- [ ] **Step 1:** Read `frontend/src/assets/styles/main.scss` and list all `:root` variables (lines 5-18).

- [ ] **Step 2:** Run grep to enumerate all files using old token names:
  ```bash
  cd /home/ubuntu/projects/AI-food
  grep -rln "color-primary-container\|color-secondary-fixed\|color-surface-container-low" frontend/src/
  ```
  Expected: ~35 files.

- [ ] **Step 3:** Build replacement table (write to scratch or commit later):

  | Old | New |
  |---|---|
  | `--color-primary-container` | `--color-primary-soft` |
  | `--color-secondary-fixed` | `--color-cyan` |
  | `--color-surface-container-low` | `--color-surface-low` |
  | `--color-surface-container-lowest` | `--color-surface-lowest` |

- [ ] **Step 4:** Commit audit notes (optional):
  ```bash
  git add frontend/
  git commit -m "docs(audit): map old Material 3 tokens to new design system"
  ```

## Task 1.2 · Create `frontend/src/styles/_tokens.scss` (single source of truth)

**Files:**
- Create: `frontend/src/styles/_tokens.scss`

**Interfaces:**
- Consumes: spec §1 (full token list)
- Produces: Sass partial exporting all CSS custom properties

- [ ] **Step 1:** Create directory and file:
  ```bash
  mkdir -p frontend/src/styles
  touch frontend/src/styles/_tokens.scss
  ```

- [ ] **Step 2:** Write the tokens partial. Copy this verbatim into `frontend/src/styles/_tokens.scss`:

```scss
/* ============================================================================
   AI 美食推荐 · 设计系统令牌
   Source of truth: docs/superpowers/specs/2026-07-04-aifood-design-system-design.md §1
   Mirror: frontend-UI/src/index.css
   ============================================================================ */

:root {
  /* --- 字体 --- */
  --font-sans: "Plus Jakarta Sans", "PingFang SC", "Microsoft YaHei", "Noto Sans SC", ui-sans-serif, system-ui, sans-serif;
  --font-serif: "Newsreader", "Songti SC", "STSong", Georgia, serif;

  /* --- 主色 · 冬日晴空 --- */
  --color-primary: #4a8dd5;
  --color-primary-hover: #2c5fa0;
  --color-primary-soft: #c5dcf0;
  --color-primary-grad-top: #5b9be0;
  --color-primary-grad-bottom: #3a7bc0;
  --color-cyan: #8ce1f3;

  /* --- 暖色点缀 · 旭日 --- */
  --color-accent-warm: #e8855a;
  --color-accent-warm-2: #f5b78a;

  /* --- 表面 --- */
  --color-surface: #f4f7f9;
  --color-surface-low: #edf1f3;
  --color-surface-lowest: #ffffff;

  /* --- 文字 --- */
  --color-on-surface: #2b2f31;
  --color-on-surface-variant: #585c5e;
  --color-on-surface-faint: #8b9094;
  --color-inverse-surface: #0b0f10;
  --color-on-primary: #ffffff;

  /* --- 语义 --- */
  --color-danger: #c8344a;
  --color-success: #2f9e6e;

  /* --- Nav 激活态 --- */
  --color-nav-active: #22d3ee;

  /* --- 圆角 --- */
  --radius-sm: 8px;
  --radius-md: 12px;
  --radius-lg: 16px;
  --radius-xl: 24px;
  --radius-2xl: 32px;
  --radius-pill: 999px;

  /* --- 间距 --- */
  --space-1: 4px;  --space-2: 8px;  --space-3: 12px; --space-4: 16px;
  --space-5: 20px; --space-6: 24px; --space-8: 32px; --space-10: 40px;
  --space-12: 48px; --space-16: 64px;

  /* --- 阴影 --- */
  --shadow-xs: 0 1px 2px rgba(11, 15, 16, 0.05);
  --shadow-sm: 0 1px 2px rgba(11, 15, 16, 0.06), 0 1px 3px rgba(11, 15, 16, 0.04);
  --shadow-md: 0 4px 12px -2px rgba(11, 15, 16, 0.08), 0 2px 4px -1px rgba(11, 15, 16, 0.04);
  --shadow-lg: 0 8px 24px -4px rgba(11, 15, 16, 0.10), 0 4px 8px -2px rgba(11, 15, 16, 0.06);
  --shadow-xl: 0 16px 40px -8px rgba(11, 15, 16, 0.14), 0 6px 12px -4px rgba(11, 15, 16, 0.08);
  --shadow-glow: 0 12px 32px -8px rgba(0, 89, 182, 0.22);
  --shadow-warm: 0 12px 32px -8px rgba(232, 133, 90, 0.25);

  /* --- 玻璃 --- */
  --glass-bg: rgba(255, 255, 255, 0.62);
  --glass-bg-strong: rgba(255, 255, 255, 0.82);
  --glass-blur: blur(20px);
  --glass-blur-strong: blur(32px);
  --glass-border: rgba(255, 255, 255, 0.4);

  /* --- 动效 --- */
  --ease-out-soft: cubic-bezier(0.22, 1, 0.36, 1);
  --ease-in-soft: cubic-bezier(0.55, 0, 0.45, 0);
  --ease-spring: cubic-bezier(0.34, 1.56, 0.64, 1);
  --dur-fast: 180ms;
  --dur-base: 280ms;
  --dur-slow: 480ms;
  --dur-spring: 680ms;

  /* --- a11y / 移动端 --- */
  --focus-ring-width: 3px;
  --focus-ring-color: rgba(0, 89, 182, 0.25);
  --focus-ring: 0 0 0 var(--focus-ring-width) var(--focus-ring-color);
  --safe-area-top: env(safe-area-inset-top, 0px);
  --safe-area-bottom: env(safe-area-inset-bottom, 0px);
  --z-base: 1; --z-sticky: 50; --z-nav: 100; --z-dialog: 200; --z-toast: 300;
  --min-tap-target: 44px;
  --color-skeleton: #e8ecf0;
  --color-skeleton-shimmer: #f4f6f8;
}

/* Motion reduce */
@media (prefers-reduced-motion: reduce) {
  :root {
    --dur-fast: 0ms;
    --dur-base: 0ms;
    --dur-slow: 0ms;
    --dur-spring: 0ms;
  }
}

/* --- 复用类 --- */
.glass-card {
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border);
}
.glass-card-strong {
  background: var(--glass-bg-strong);
  backdrop-filter: var(--glass-blur-strong);
  -webkit-backdrop-filter: var(--glass-blur-strong);
  border: 1px solid var(--glass-border);
}
.shadow-glow { box-shadow: var(--shadow-glow); }
.shadow-warm { box-shadow: var(--shadow-warm); }
.pill-nav {
  background: rgba(11, 15, 16, 0.92);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border-radius: var(--radius-pill);
  box-shadow: var(--shadow-lg);
  border: 1px solid rgba(255, 255, 255, 0.08);
}
.btn-primary-gradient {
  background: linear-gradient(180deg, var(--color-primary-grad-top) 0%, var(--color-primary-grad-bottom) 100%);
}
.btn-primary-gradient:hover {
  background: var(--color-primary-hover);
}
.bg-winter-sunrise {
  background:
    radial-gradient(ellipse 800px 400px at 80% 0%, rgba(245, 183, 138, 0.35), transparent 60%),
    radial-gradient(ellipse 600px 500px at 0% 30%, rgba(140, 225, 243, 0.25), transparent 60%),
    linear-gradient(180deg, #f4f7f9 0%, #edf1f3 100%);
}
.bg-cold-canvas {
  background:
    radial-gradient(ellipse 1200px 600px at 50% -10%, rgba(74, 141, 213, 0.18), transparent 60%),
    linear-gradient(180deg, #f4f7f9 0%, #ffffff 100%);
}

/* --- 字体层级 (Editorial 克制) --- */
.text-hero-serif {
  font-family: var(--font-serif);
  font-style: italic;
  font-weight: 600;
  font-size: 40px;
  line-height: 1.05;
  letter-spacing: -0.02em;
}
.text-h1 {
  font-family: var(--font-sans);
  font-weight: 700;
  font-size: 24px;
  line-height: 1.25;
  letter-spacing: -0.01em;
}
.text-h2 {
  font-family: var(--font-sans);
  font-weight: 700;
  font-size: 18px;
  line-height: 1.3;
}
.text-body {
  font-family: var(--font-sans);
  font-weight: 400;
  font-size: 14px;
  line-height: 1.45;
}
.text-meta {
  font-family: var(--font-sans);
  font-weight: 500;
  font-size: 12px;
  line-height: 1.4;
}
.text-caption {
  font-family: var(--font-sans);
  font-weight: 600;
  font-size: 11px;
  line-height: 1.4;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}
.text-truncate {
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.text-clamp-2 {
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}
.text-clamp-3 {
  display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden;
}
.no-scrollbar::-webkit-scrollbar { display: none; }
.no-scrollbar { -ms-overflow-style: none; scrollbar-width: none; }
```

- [ ] **Step 3:** Verify file compiles (import it standalone in a scratch SCSS or just visually check syntax).

## Task 1.3 · Replace `main.scss` :root with `@use` of tokens

**Files:**
- Modify: `frontend/src/assets/styles/main.scss:1-18`

**Interfaces:**
- Consumes: `_tokens.scss` from Task 1.2
- Produces: `main.scss` that imports the partial + retains reset/utility/keyframes

- [ ] **Step 1:** Edit `frontend/src/assets/styles/main.scss`. Replace the `:root { ... }` block (lines 5-18) with the import directive. The new top of the file:

```scss
/* ============================================
   Sanctuary UI — Global Styles
   Design tokens: ../../../styles/_tokens.scss
   ============================================ */
@use "../../styles/tokens" as *;

/* Reset & base */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}
html, body {
  width: 100%;
  height: 100%;
  overflow-x: hidden;
}
body {
  font-family: var(--font-sans);
  font-size: 14px;
  line-height: 1.45;
  color: var(--color-on-surface);
  background-color: var(--color-surface);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}
```

- [ ] **Step 2:** Remove the duplicate `.glass-card` definition (lines 64-68 in original). The tokens partial already provides it.

- [ ] **Step 3:** Remove the duplicate `.no-scrollbar` definition (lines 74-81). Already in tokens.

- [ ] **Step 4:** Keep `sanctuary-*` keyframes + `.animate-*` classes + scrollbar styles (those aren't in tokens).

- [ ] **Step 5:** Verify file structure visually: should have `@use` → reset → keyframes → scrollbar. No duplicate `:root` or `.glass-card`.

- [ ] **Step 6:** Run `npm run build` to verify SCSS compiles:
  ```bash
  cd /home/ubuntu/projects/AI-food/frontend
  npm run build 2>&1 | tail -20
  ```
  Expected: build success, no SCSS errors.

- [ ] **Step 7:** Commit:
  ```bash
  git add frontend/src/styles/_tokens.scss frontend/src/assets/styles/main.scss
  git commit -m "refactor(frontend): migrate main.scss to design tokens partial"
  ```

## Task 1.4 · Global replace old token names → new (35 files)

**Files:**
- Modify: 35 files in `frontend/src/` (grep result from Task 1.1)

**Interfaces:**
- Consumes: replacement table from Task 1.1
- Produces: zero references to old Material 3 token names

- [ ] **Step 1:** Run sed replacement on all Vue files:
  ```bash
  cd /home/ubuntu/projects/AI-food
  grep -rl "color-primary-container" frontend/src/ | xargs sed -i 's/--color-primary-container/--color-primary-soft/g'
  grep -rl "color-secondary-fixed" frontend/src/ | xargs sed -i 's/--color-secondary-fixed/--color-cyan/g'
  grep -rl "color-surface-container-low\b" frontend/src/ | xargs sed -i 's/--color-surface-container-low/--color-surface-low/g'
  grep -rl "color-surface-container-lowest" frontend/src/ | xargs sed -i 's/--color-surface-container-lowest/--color-surface-lowest/g'
  ```
  Note: `\b` ensures `--color-surface-container-low` doesn't match `--color-surface-container-lowest` (which is replaced separately).

- [ ] **Step 2:** Verify zero remaining old token references:
  ```bash
  cd /home/ubuntu/projects/AI-food
  grep -rn "color-primary-container\|color-secondary-fixed\|color-surface-container-low\|color-surface-container-lowest" frontend/src/
  ```
  Expected: empty output.

- [ ] **Step 3:** Run build:
  ```bash
  cd /home/ubuntu/projects/AI-food/frontend
  npm run build 2>&1 | tail -10
  ```
  Expected: success.

- [ ] **Step 4:** Run type check:
  ```bash
  cd /home/ubuntu/projects/AI-food/frontend
  npx vue-tsc --noEmit 2>&1 | tail -10
  ```
  Expected: 0 errors (warnings OK).

- [ ] **Step 5:** Spot-check one file visually — open `frontend/src/components/CachedImage.vue` and confirm `var(--color-surface-low)` is used (not the old name).

- [ ] **Step 6:** Commit:
  ```bash
  git add frontend/src/
  git commit -m "refactor(frontend): replace Material 3 token names with design system names"
  ```

## Task 1.4b · Replace hardcoded old primary rgba values (~10 places)

**Files:**
- Modify: ~5 Vue files + 1 SCSS file (Chat.vue, UploadPhoto.vue, RecordActions.vue, main.scss)

**Interfaces:**
- Consumes: hardcoded old primary color `rgba(0, 89, 182, ...)` / `#0059b6`
- Produces: new primary color `rgba(74, 141, 213, ...)` / `#4a8dd5`

> **Rationale (Oracle P0)**: Phase 1 Task 1.4 only replaces token NAMES, not hardcoded VALUES. ~10 places in `*.vue` / `*.scss` use the old primary color directly via rgba() or hex. These would otherwise keep the old deep blue while everything else migrates.

- [ ] **Step 1:** Audit hardcoded old primary:
  ```bash
  cd /home/ubuntu/projects/AI-food
  grep -rn "rgba(0,\s*89,\s*182\|#0059b6\|#003f80\|#68a0ff" --include="*.vue" --include="*.scss" frontend/src/ | tee /tmp/old-color-audit.txt
  ```
  Expected: ~10-15 lines.

- [ ] **Step 2:** For each match, decide:
  - Is it a **shadow** like `box-shadow: 0 4px 16px rgba(0,89,182,0.2)`? → prefer replacing with existing token (`var(--shadow-glow)` / `var(--shadow-md)` / `var(--shadow-lg)`) where the shape matches, else rewrite with new primary rgba.
  - Is it a **background / border**? → replace `rgba(0, 89, 182, X)` with `rgba(74, 141, 213, X)` (new primary rgb) OR `var(--color-primary)` with `color-mix` if supported.

- [ ] **Step 3:** Run sed for bulk replacements (manual review first):
  ```bash
  cd /home/ubuntu/projects/AI-food
  # Replace rgba(0, 89, 182 with rgba(74, 141, 213 (new primary rgb)
  grep -rl "rgba(0,\s*89,\s*182" --include="*.vue" --include="*.scss" frontend/src/ | xargs sed -i 's/rgba(0,\s*89,\s*182/rgba(74, 141, 213/g'
  # Replace #0059b6 with #4a8dd5
  grep -rl "#0059b6" --include="*.vue" --include="*.scss" frontend/src/ | xargs sed -i 's/#0059b6/#4a8dd5/g'
  ```
  ⚠️ **Note**: Don't replace rgba(11, 15, 16, ...) — that's the inverse-surface (unchanged).

- [ ] **Step 4:** Manual review each remaining match (3-5 places):
  - Chat.vue:475, 539, 647 — shadows → likely use new primary rgba
  - UploadPhoto.vue:193, 300, 305 — backgrounds / shadows
  - RecordActions.vue:217, 405, 479 — hover / focus
  - main.scss:71 — sanctuary-glow shadow

- [ ] **Step 5:** Verify build still works:
  ```bash
  cd /home/ubuntu/projects/AI-food/frontend && npm run build 2>&1 | tail -5
  ```

- [ ] **Step 6:** Commit (combine with Task 1.4 commit if both touch same files):
  ```bash
  git add frontend/src/
  git commit -m "refactor(frontend): replace hardcoded old primary rgba with new primary"
  ```
  OR amend previous commit:
  ```bash
  git add frontend/src/
  git commit --amend --no-edit
  ```

## Task 1.5 · Add font preload to `frontend/index.html`

**Files:**
- Modify: `frontend/index.html:22-27`

**Interfaces:**
- Consumes: existing font links
- Produces: preloaded Google Fonts to mitigate FOUT

- [ ] **Step 1:** Edit `frontend/index.html`. After the existing `<link rel="stylesheet">` for fonts (around line 25), add preload links. Replace the existing preconnect + stylesheet block (lines 22-27) with:

```html
<link rel="preconnect" href="https://fonts.googleapis.com" />
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin />
<link rel="preload" as="style" href="https://fonts.googleapis.com/css2?family=Newsreader:ital,opsz,wght@0,6..72,200..800;1,6..72,200..800&family=Plus+Jakarta+Sans:wght@200..800&display=swap" />
<link
  href="https://fonts.googleapis.com/css2?family=Newsreader:ital,opsz,wght@0,6..72,200..800;1,6..72,200..800&family=Plus+Jakarta+Sans:wght@200..800&display=swap"
  rel="stylesheet"
/>
```

- [ ] **Step 2:** Build + verify:
  ```bash
  cd /home/ubuntu/projects/AI-food/frontend
  npm run build 2>&1 | tail -5
  ```

- [ ] **Step 3:** Commit:
  ```bash
  git add frontend/index.html
  git commit -m "perf(frontend): preload Google Fonts to mitigate FOUT"
  ```

## Task 1.6 · Phase 1 verification gate

**Files:**
- Read: `docs/superpowers/specs/2026-07-04-aifood-design-system-design.md §6.3`

**Interfaces:**
- Consumes: all Phase 1 changes
- Produces: passing verification checklist

- [ ] **Step 1:** Run all four verification checks:
  ```bash
  cd /home/ubuntu/projects/AI-food
  grep -rn "color-primary-container\|color-secondary-fixed" frontend/src/   # Expect: empty
  grep -rn "color-surface-container" frontend/src/                           # Expect: empty
  cd frontend && npm run build 2>&1 | tail -5                                  # Expect: success
  npx vue-tsc --noEmit 2>&1 | tail -5                                          # Expect: 0 errors
  ```

- [ ] **Step 2:** Open `http://localhost:12121/` in browser. Visually verify the preview still renders correctly (no CSS breakage from token rename — colors / shadows / glass all should look the same as last approved version).

- [ ] **Step 3:** Document Phase 1 completion in log:
  ```bash
  echo "Phase 1 Foundation complete — tokens migrated, no visual changes" >> logs/2026-07-04-design-system-rollout.log
  ```

- [ ] **Step 4:** Ask user to verify by viewing the live frontend (`npm run dev` → check `http://localhost:3000/` doesn't look broken).

---

# Phase 2 · Vant CSS Variable Override (~30min)

**Objective:** Make Vant components respect the new primary/text/background colors.

## Task 2.1 · Add 3 Vant CSS variables to main.scss

**Files:**
- Modify: `frontend/src/assets/styles/main.scss` (top of file after `@use`)

**Interfaces:**
- Consumes: tokens partial
- Produces: Vant components use new colors

- [ ] **Step 1:** Add the override block right after `@use "../../styles/tokens" as *;`:

```scss
/* ============================================
   Vant 4 CSS Variable Override
   ============================================ */
:root {
  --van-primary-color: var(--color-primary);
  --van-text-color: var(--color-on-surface);
  --van-background: var(--color-surface);
}
```

- [ ] **Step 2:** Verify SCSS still compiles:
  ```bash
  cd /home/ubuntu/projects/AI-food/frontend
  npm run build 2>&1 | tail -5
  ```
  Expected: success.

- [ ] **Step 3:** Commit:
  ```bash
  git add frontend/src/assets/styles/main.scss
  git commit -m "feat(frontend): override Vant 4 primary/text/background via CSS variables"
  ```

## Task 2.2 · Phase 2 verification — Vant re-skin

**Files:**
- Read: `frontend/src/views/Feed.vue` (uses Vant Cell)
- Read: `frontend/src/views/Login.vue` (uses Vant Field)
- Read: `frontend/src/views/Records.vue` (uses Vant List)

**Interfaces:**
- Consumes: Vant override from Task 2.1
- Produces: Vant components re-skinned

- [ ] **Step 1:** Start dev server:
  ```bash
  cd /home/ubuntu/projects/AI-food/frontend
  setsid nohup npm run dev < /dev/null > /tmp/frontend-dev.log 2>&1 & disown
  sleep 5
  ```

- [ ] **Step 2:** Navigate to `/feed` page. Check that any Vant Cell / Tab / Button elements show the new `#4a8dd5` blue (use browser devtools "Inspect" → computed style).

- [ ] **Step 3:** Navigate to `/login` page. Check Vant Field labels are `--color-on-surface` (dark gray).

- [ ] **Step 4:** Navigate to `/records` page. Check Vant List background is `--color-surface` (light gray-blue).

- [ ] **Step 5:** If any Vant components still show default colors, add additional `--van-*` overrides to `main.scss` per https://vant-ui.github.io/vant/v4/#/en-US/theme.

- [ ] **Step 6:** Document Phase 2 complete in log:
  ```bash
  echo "Phase 2 Vant override complete — 3 CSS variables applied" >> logs/2026-07-04-design-system-rollout.log
  ```

- [ ] **Step 7:** Stop dev server:
  ```bash
  PID=$(lsof -ti :3000 2>/dev/null); [ -n "$PID" ] && kill $PID 2>/dev/null; sleep 1
  ```

---

# Phase 3 · Home Deep Polish (~3-4h)

**Objective:** Apply winter-sunrise aesthetic + RecommendationCard + MoodChips + gradient CTA to `Home.vue`. Update `App.vue` pill nav to use `--color-nav-active`.

**Component strategy note**: Spec §2.1 lists 11 components. Of those, **5 are Vue files** (RecommendationCard, MoodChip, MessageBubble, DSLoadingOrb, StatCard). The remaining 6 are intentionally NOT created as Vue files:
- `DSAppShell` / `DSTopBar` — App.vue already has layout + page-level top bars; wrapping in another component adds no value
- `DSGlassCard` / `DSSurfaceCard` — already implemented as `.glass-card` / `.glass-card-strong` / `.surface-card` CSS utility classes in `_tokens.scss`
- `DSSegmented` — only Feed uses it (Phase 6 deferred); create when Feed phase lands
- `DSEmptyState` — none of the 3 deep-polish pages (Home / Chat / Profile) need an empty state; create when a page requires one

Acceptance gate explicitly checks: each page references only what's needed, no speculative component files.

## Task 3.1 · Update App.vue pill nav active color

**Files:**
- Modify: `frontend/src/App.vue:208-210, 242` (3 occurrences of `#22d3ee`)

**Interfaces:**
- Consumes: `--color-nav-active` token
- Produces: pill nav active state references token, not hardcoded hex

- [ ] **Step 1:** Read `frontend/src/App.vue` and locate the 3 occurrences of `#22d3ee`:
  ```bash
  grep -n "#22d3ee" frontend/src/App.vue
  ```

- [ ] **Step 2:** Replace all 3 occurrences with `var(--color-nav-active)`:
  ```bash
  cd /home/ubuntu/projects/AI-food
  sed -i 's/#22d3ee/var(--color-nav-active)/g' frontend/src/App.vue
  ```

- [ ] **Step 3:** Verify:
  ```bash
  grep -n "color-nav-active\|#22d3ee" frontend/src/App.vue
  ```
  Expected: `var(--color-nav-active)` (3 times), no `#22d3ee`.

- [ ] **Step 4:** Build:
  ```bash
  cd /home/ubuntu/projects/AI-food/frontend && npm run build 2>&1 | tail -5
  ```

- [ ] **Step 5:** Commit:
  ```bash
  git add frontend/src/App.vue
  git commit -m "refactor(frontend): pill nav active color uses --color-nav-active token"
  ```

## Task 3.2 · Create `RecommendationCard` component

**Files:**
- Create: `frontend/src/components/ui/RecommendationCard.vue`

**Interfaces:**
- Consumes: glass-card utility, tokens
- Produces: glass hero card component accepting `title`, `subtitle`, `ctaText` props

- [ ] **Step 1:** Create directory and file:
  ```bash
  mkdir -p frontend/src/components/ui
  ```

- [ ] **Step 2:** Write `frontend/src/components/ui/RecommendationCard.vue`:

```vue
<template>
  <div class="recommendation-card glass-card-strong">
    <div class="caption">{{ caption }}</div>
    <div class="title">{{ title }}</div>
    <div v-if="subtitle" class="subtitle">{{ subtitle }}</div>
    <slot name="extra" />
  </div>
</template>

<script setup lang="ts">
interface Props {
  caption: string;
  title: string;
  subtitle?: string;
}
defineProps<Props>();
</script>

<style lang="scss" scoped>
.recommendation-card {
  border-radius: var(--radius-xl);
  padding: var(--space-6);
  box-shadow: var(--shadow-glow);
}
.caption {
  font-family: var(--font-sans);
  font-weight: 600;
  font-size: 11px;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: var(--color-accent-warm);
  margin-bottom: var(--space-2);
}
.title {
  font-family: var(--font-sans);
  font-weight: 700;
  font-size: 24px;
  line-height: 1.25;
  color: var(--color-primary);
  margin-bottom: var(--space-2);
}
.subtitle {
  font-family: var(--font-sans);
  font-weight: 400;
  font-size: 14px;
  line-height: 1.45;
  color: var(--color-on-surface-variant);
}
</style>
```

- [ ] **Step 3:** Build to verify type-check passes:
  ```bash
  cd /home/ubuntu/projects/AI-food/frontend && npx vue-tsc --noEmit 2>&1 | tail -5
  ```
  Expected: 0 errors.

- [ ] **Step 4:** Commit:
  ```bash
  git add frontend/src/components/ui/RecommendationCard.vue
  git commit -m "feat(frontend): RecommendationCard component for hero glass card"
  ```

## Task 3.3 · Create `MoodChip` component

**Files:**
- Create: `frontend/src/components/ui/MoodChip.vue`

**Interfaces:**
- Consumes: tokens
- Produces: pill-shaped chip with warm accent

- [ ] **Step 1:** Write `frontend/src/components/ui/MoodChip.vue`:

```vue
<template>
  <button class="mood-chip" :class="{ 'mood-chip--selected': selected }" @click="$emit('toggle')">
    <span v-if="icon" class="icon" v-html="icon" />
    <span class="label">{{ label }}</span>
  </button>
</template>

<script setup lang="ts">
interface Props {
  label: string;
  icon?: string;
  selected?: boolean;
}
defineProps<Props>();
defineEmits<{ (e: 'toggle'): void }>();
</script>

<style lang="scss" scoped>
.mood-chip {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-4);
  border-radius: var(--radius-pill);
  background: var(--color-surface-lowest);
  border: 1px solid var(--color-surface-low);
  font-family: var(--font-sans);
  font-weight: 600;
  font-size: 12px;
  color: var(--color-on-surface);
  cursor: pointer;
  transition: all var(--dur-fast) var(--ease-out-soft);
}
.mood-chip:hover {
  border-color: var(--color-accent-warm);
  transform: translateY(-1px);
}
.mood-chip:active {
  transform: scale(0.95);
}
.mood-chip--selected {
  background: rgba(232, 133, 90, 0.12);
  border-color: var(--color-accent-warm);
  color: var(--color-accent-warm);
}
.icon {
  display: inline-flex;
  align-items: center;
}
</style>
```

- [ ] **Step 2:** Type check:
  ```bash
  cd /home/ubuntu/projects/AI-food/frontend && npx vue-tsc --noEmit 2>&1 | tail -5
  ```

- [ ] **Step 3:** Commit:
  ```bash
  git add frontend/src/components/ui/MoodChip.vue
  git commit -m "feat(frontend): MoodChip component for hero quick filter pills"
  ```

## Task 3.4 · Refactor `Home.vue` to use new components

**Files:**
- Modify: `frontend/src/views/Home.vue:1-242`

**Interfaces:**
- Consumes: `RecommendationCard` + `MoodChip` + `btn-primary-gradient` + `bg-winter-sunrise`
- Produces: Home page with editorial hero

- [ ] **Step 1:** Read current `Home.vue` and identify the layout sections (greeting / hero recommendation / chips / main CTA).

- [ ] **Step 2:** Update the `<template>` section. Replace the existing recommendation block + chip area with new components. Specifically:
  - Change the outer wrapper `class="home-container"` to add `bg-winter-sunrise` (or apply via inline style).
  - Replace the existing recommendation card with `<RecommendationCard caption="今晚的推荐" title="一碗热汤面，暖心暖胃。" subtitle="..." />`.
  - Add `<MoodChip>` × 3 below: 热汤面 / 暖心菜 / 夜宵.
  - Replace existing main CTA with `<button class="btn-primary-gradient">开始 30 秒对话</button>`.

- [ ] **Step 3:** Update script imports:
  ```ts
  import RecommendationCard from "@/components/ui/RecommendationCard.vue";
  import MoodChip from "@/components/ui/MoodChip.vue";
  ```

- [ ] **Step 4:** Keep existing logic (doStartChat / goToResult / pending recommendation dialog) intact — only the visual layer changes.

- [ ] **Step 5:** Verify build:
  ```bash
  cd /home/ubuntu/projects/AI-food/frontend && npm run build 2>&1 | tail -5
  ```

- [ ] **Step 6:** Run dev server + take screenshot at 375px width:
  ```bash
  cd /home/ubuntu/projects/AI-food/frontend
  setsid nohup npm run dev < /dev/null > /tmp/frontend-dev.log 2>&1 & disown
  sleep 5
  # Navigate to / via browser devtools (or curl to verify page returns 200)
  curl -s -o /dev/null -w "%{http_code}\n" http://localhost:3000/
  # Stop dev server
  PID=$(lsof -ti :3000 2>/dev/null); [ -n "$PID" ] && kill $PID 2>/dev/null; sleep 1
  ```

- [ ] **Step 7:** Commit:
  ```bash
  git add frontend/src/views/Home.vue
  git commit -m "feat(home): redesign with RecommendationCard + MoodChips + winter-sunrise bg"
  ```

## Task 3.5 · Phase 3 verification gate

- [ ] **Step 1:** Compare `frontend/src/views/Home.vue` visual output to preview at `http://localhost:12121/#mobile` Home mock. They should match in:
  - Background = warm sunrise gradient
  - Recommendation card = glass + warm caption + bold blue title
  - 3 chips below
  - Primary CTA = blue gradient

- [ ] **Step 2:** Verify warm color count ≤ 3 (caption + MoodChip selected state + avatar in pill nav if applicable).

- [ ] **Step 3:** Document Phase 3 complete:
  ```bash
  echo "Phase 3 Home deep polish complete — RecommendationCard + MoodChip integrated" >> logs/2026-07-04-design-system-rollout.log
  ```

- [ ] **Step 4:** Ask user to verify by viewing dev server screenshot.

---

# Phase 4 · Chat Deep Polish (~3-4h)

**Objective:** Refactor `Chat.vue` with MessageBubble (2 variants) + DSLoadingOrb + compact input bar.

## Task 4.1 · Create `MessageBubble` component (user variant)

**Files:**
- Create: `frontend/src/components/ui/MessageBubble.vue`

**Interfaces:**
- Consumes: tokens
- Produces: bubble component with `role` prop ("user" / "ai") + content slot

- [ ] **Step 1:** Write `frontend/src/components/ui/MessageBubble.vue`:

```vue
<template>
  <div class="bubble-row" :class="`bubble-row--${role}`">
    <div v-if="role === 'ai'" class="avatar avatar--ai" />
    <div class="bubble" :class="`bubble--${role}`">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  role: "user" | "ai";
}
defineProps<Props>();
</script>

<style lang="scss" scoped>
.bubble-row {
  display: flex;
  align-items: flex-start;
  gap: var(--space-2);
  margin-bottom: var(--space-2);
}
.bubble-row--user {
  justify-content: flex-end;
}
.avatar {
  width: 28px;
  height: 28px;
  border-radius: var(--radius-md);
  flex-shrink: 0;
}
.avatar--ai {
  background: linear-gradient(135deg, var(--color-accent-warm), var(--color-accent-warm-2));
}
.bubble {
  max-width: 70%;
  padding: var(--space-3);
  border-radius: var(--radius-md);
  font-family: var(--font-sans);
  font-size: 12px;
  line-height: 1.45;
}
.bubble--ai {
  background: linear-gradient(135deg, #fef0e8, #fff5ee);
  color: #7a3d22;
}
.bubble--user {
  background: var(--color-primary);
  color: var(--color-on-primary);
}
</style>
```

- [ ] **Step 2:** Type check + commit:
  ```bash
  cd /home/ubuntu/projects/AI-food/frontend && npx vue-tsc --noEmit 2>&1 | tail -3
  git add frontend/src/components/ui/MessageBubble.vue
  git commit -m "feat(frontend): MessageBubble with user/AI variants"
  ```

## Task 4.2 · Create `DSLoadingOrb` component

**Files:**
- Create: `frontend/src/components/ui/DSLoadingOrb.vue`

**Interfaces:**
- Consumes: tokens
- Produces: 3-dot pulsing indicator

- [ ] **Step 1:** Write `frontend/src/components/ui/DSLoadingOrb.vue`:

```vue
<template>
  <div class="loading-orb">
    <span v-for="i in 3" :key="i" class="dot" :style="{ animationDelay: `${(i - 1) * 150}ms` }" />
  </div>
</template>

<style lang="scss" scoped>
.loading-orb {
  display: inline-flex;
  gap: var(--space-1);
  padding: var(--space-3);
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, #fef0e8, #fff5ee);
}
.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: rgba(122, 61, 34, 0.4);
  animation: orb-bounce 1.2s ease-in-out infinite;
}
@keyframes orb-bounce {
  0%, 80%, 100% { transform: translateY(0); }
  40% { transform: translateY(-4px); }
}
</style>
```

- [ ] **Step 2:** Type check + commit:
  ```bash
  cd /home/ubuntu/projects/AI-food/frontend && npx vue-tsc --noEmit 2>&1 | tail -3
  git add frontend/src/components/ui/DSLoadingOrb.vue
  git commit -m "feat(frontend): DSLoadingOrb 3-dot pulsing indicator"
  ```

## Task 4.3 · Refactor `Chat.vue` TopBar + message list

**Files:**
- Modify: `frontend/src/views/Chat.vue` (template section)

**Interfaces:**
- Consumes: MessageBubble + DSLoadingOrb + tokens
- Produces: compact chat layout with new bubbles

- [ ] **Step 1:** Read current `Chat.vue` and identify the TopBar + message list area.

- [ ] **Step 2:** Update TopBar:
  - Change bg to `var(--color-surface-lowest)` with `border-bottom: 1px solid var(--color-surface-low)`.
  - Avatar = gradient warm (same as MessageBubble ai avatar).
  - Status text "正在思考…" = `var(--color-on-surface-faint)`.

- [ ] **Step 3:** Replace inline message rendering with `<MessageBubble role="user|ai">content</MessageBubble>`.

- [ ] **Step 4:** Replace inline loading dots with `<DSLoadingOrb />` — only shown when `isThinking` is true.

- [ ] **Step 5:** Update script imports:
  ```ts
  import MessageBubble from "@/components/ui/MessageBubble.vue";
  import DSLoadingOrb from "@/components/ui/DSLoadingOrb.vue";
  ```

- [ ] **Step 6:** Build:
  ```bash
  cd /home/ubuntu/projects/AI-food/frontend && npm run build 2>&1 | tail -5
  ```

- [ ] **Step 7:** Commit:
  ```bash
  git add frontend/src/views/Chat.vue
  git commit -m "feat(chat): MessageBubble + DSLoadingOrb + compact TopBar"
  ```

## Task 4.4 · Update `Chat.vue` input bar

**Files:**
- Modify: `frontend/src/views/Chat.vue` (input area in template)

**Interfaces:**
- Consumes: tokens + `btn-primary-gradient`
- Produces: compact input pill + send button

- [ ] **Step 1:** Replace input container styling:
  ```vue
  <div class="chat-input-row">
    <input v-model="inputText" class="chat-input" placeholder="说点什么…" @keydown.enter="send" />
    <button class="btn-primary-gradient send-btn" :disabled="!inputText" @click="send">
      <ArrowRight :size="14" />
    </button>
  </div>
  ```

- [ ] **Step 2:** Add scoped styles:
  ```scss
  .chat-input-row {
    display: flex;
    gap: var(--space-2);
    padding: var(--space-2);
    border-top: 1px solid var(--color-surface-low);
    background: var(--color-surface-lowest);
  }
  .chat-input {
    flex: 1;
    height: 36px;
    padding: 0 var(--space-4);
    border-radius: var(--radius-pill);
    background: var(--color-surface-low);
    border: none;
    font-family: var(--font-sans);
    font-size: 12px;
    color: var(--color-on-surface);
    outline: none;
  }
  .chat-input:focus {
    box-shadow: var(--focus-ring);
  }
  .send-btn {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    border: none;
    color: var(--color-on-primary);
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
  }
  .send-btn:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }
  ```

- [ ] **Step 3:** Build + commit:
  ```bash
  cd /home/ubuntu/projects/AI-food/frontend && npm run build 2>&1 | tail -5
  git add frontend/src/views/Chat.vue
  git commit -m "feat(chat): compact input pill with gradient send button"
  ```

## Task 4.5 · Phase 4 verification gate

- [ ] **Step 1:** Compare Chat.vue visual to preview `http://localhost:12121/#mobile` Chat mock. Should match:
  - AI messages = warm-tinted bubbles + warm avatar
  - User messages = solid primary blue bubbles
  - Loading = 3-dot orb (warm tinted)
  - Input = pill + gradient send button

- [ ] **Step 2:** Run end-to-end test: open dev server → navigate to `/chat` → send a message → verify DSLoadingOrb appears during stream → verify AI response renders with warm bubble.

- [ ] **Step 3:** Document Phase 4 complete:
  ```bash
  echo "Phase 4 Chat deep polish complete — MessageBubble + DSLoadingOrb + compact input" >> logs/2026-07-04-design-system-rollout.log
  ```

---

# Phase 5 · Profile Deep Polish (~2-3h)

**Objective:** Refactor `Profile.vue` with gradient hero + StatCard × 3 + list items.

## Task 5.1 · Create `StatCard` component

**Files:**
- Create: `frontend/src/components/ui/StatCard.vue`

**Interfaces:**
- Consumes: tokens + Newsreader italic (digits only — safe)
- Produces: white card with big number + label

- [ ] **Step 1:** Write `frontend/src/components/ui/StatCard.vue`:

```vue
<template>
  <div class="stat-card">
    <div class="stat-number">{{ number }}</div>
    <div class="stat-label">{{ label }}</div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  number: number | string;
  label: string;
}
defineProps<Props>();
</script>

<style lang="scss" scoped>
.stat-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(12px);
  border-radius: var(--radius-lg);
  padding: var(--space-4) var(--space-3) var(--space-2);
  text-align: center;
  box-shadow: var(--shadow-md);
}
.stat-number {
  font-family: var(--font-serif);
  font-style: italic;
  font-weight: 600;
  font-size: 26px;
  line-height: 1;
  color: var(--color-primary);
}
.stat-label {
  font-family: var(--font-sans);
  font-weight: 600;
  font-size: 11px;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: var(--color-on-surface-variant);
  margin-top: 2px;
}
</style>
```

- [ ] **Step 2:** Type check + commit:
  ```bash
  cd /home/ubuntu/projects/AI-food/frontend && npx vue-tsc --noEmit 2>&1 | tail -3
  git add frontend/src/components/ui/StatCard.vue
  git commit -m "feat(frontend): StatCard component with Newsreader italic number"
  ```

## Task 5.2 · Refactor `Profile.vue` hero + StatCard row

**Files:**
- Modify: `frontend/src/views/Profile.vue`

**Interfaces:**
- Consumes: StatCard + tokens
- Produces: gradient hero + 3 stat cards

- [ ] **Step 1:** Read current `Profile.vue` and locate the hero + stat card section.

- [ ] **Step 2:** Update hero block:
  - Change bg to `linear-gradient(135deg, var(--color-primary), #2c5fa0)`.
  - Set padding `pt-6 pb-6` (compact).
  - Avatar = `width: 64px; height: 64px; border-radius: var(--radius-pill); border: 2px solid white; background: linear-gradient(135deg, var(--color-accent-warm), var(--color-accent-warm-2)); box-shadow: var(--shadow-warm)`.
  - Name = `text-h1` (中文 hero sans bold — do NOT use Newsreader).
  - Bio = `text-meta` opacity 80%.

- [ ] **Step 3:** Replace inline stat cards with `<StatCard :number="42" label="对话" />` × 3.

- [ ] **Step 4:** Wrap stat cards in `<div class="stat-grid">` (use scoped CSS instead of Tailwind). Add to `<style scoped>`:

```scss
.stat-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-2);
  padding: var(--space-4) var(--space-4) 0;
}
```

- [ ] **Step 5:** Add script imports:
  ```ts
  import StatCard from "@/components/ui/StatCard.vue";
  ```

- [ ] **Step 6:** Build:
  ```bash
  cd /home/ubuntu/projects/AI-food/frontend && npm run build 2>&1 | tail -5
  ```

- [ ] **Step 7:** Commit:
  ```bash
  git add frontend/src/views/Profile.vue
  git commit -m "feat(profile): gradient hero + StatCard × 3 (compact pt-6 pb-6 + pt-4 pb-2 px-3)"
  ```

## Task 5.3 · Update `Profile.vue` list items

**Files:**
- Modify: `frontend/src/views/Profile.vue` (list section)

**Interfaces:**
- Consumes: tokens
- Produces: editorial list items

- [ ] **Step 1:** Update each list item to:
  - `background: var(--color-surface-lowest)`
  - `border: 1px solid var(--color-surface-low)`
  - `border-radius: var(--radius-md)`
  - `padding: var(--space-3)`
  - Icon color = `var(--color-primary)`
  - Text = `var(--color-on-surface)` font-weight 600
  - ChevronRight = `var(--color-on-surface-faint)`

- [ ] **Step 2:** Build + commit:
  ```bash
  cd /home/ubuntu/projects/AI-food/frontend && npm run build 2>&1 | tail -5
  git add frontend/src/views/Profile.vue
  git commit -m "feat(profile): editorial list items with token-based colors"
  ```

## Task 5.4 · Phase 5 verification gate

- [ ] **Step 1:** Compare Profile.vue visual to preview `http://localhost:12121/#mobile` Profile mock. Should match:
  - Hero = compact gradient + warm avatar
  - Stat cards = white with Newsreader italic numbers + uppercase label
  - List items = white surface with primary icon

- [ ] **Step 2:** Take screenshots at 320 / 375 / 414 / 768px widths, verify no horizontal scroll.

- [ ] **Step 3:** Document Phase 5 complete:
  ```bash
  echo "Phase 5 Profile deep polish complete — gradient hero + StatCard + editorial list" >> logs/2026-07-04-design-system-rollout.log
  ```

---

# Phase 6 · Verify & Archive (~1-2h)

**Objective:** Full validation across backend + frontend + design system grep + screenshots.

## Task 6.1 · Backend regression test

**Files:**
- Read: AGENTS.md test command

**Interfaces:**
- Consumes: 168-test baseline
- Produces: passing build

- [ ] **Step 1:** Run from repo root:
  ```bash
  cd /home/ubuntu/projects/AI-food
  mvn -pl ai-food-app test 2>&1 | tail -15
  ```
  Expected: BUILD SUCCESS, 168 tests passing.

- [ ] **Step 2:** If any test fails, document and decide whether fix or skip (UI changes shouldn't break backend).

## Task 6.2 · Frontend type check + build

- [ ] **Step 1:** Type check:
  ```bash
  cd /home/ubuntu/projects/AI-food/frontend
  npx vue-tsc --noEmit 2>&1 | tail -10
  ```
  Expected: 0 errors.

- [ ] **Step 2:** Build:
  ```bash
  npm run build 2>&1 | tail -15
  ```
  Expected: success with bundle size within 5KB CSS / 0KB JS tolerance vs pre-rollout baseline.

## Task 6.3 · Design system grep validation

- [ ] **Step 1:** All forbidden patterns must be empty:
  ```bash
  cd /home/ubuntu/projects/AI-food
  grep -rn "color-primary-container\|color-secondary-fixed\|color-surface-container" frontend/src/
  grep -rn "#22d3ee" frontend/src/
  grep -rn "color: #0059b6\|background: #0059b6" frontend/src/
  ```
  Expected: all empty (no old tokens, no hardcoded nav-active or old primary).

## Task 6.4 · Visual screenshot at 3 widths

- [ ] **Step 1:** Start dev server:
  ```bash
  cd /home/ubuntu/projects/AI-food/frontend
  setsid nohup npm run dev < /dev/null > /tmp/frontend-dev.log 2>&1 & disown
  sleep 5
  ```

- [ ] **Step 2:** Use browser devtools (or playwright) to take screenshots at 320 / 375 / 414 / 768px for:
  - `/` (Home)
  - `/chat` (Chat)
  - `/profile` (Profile)
  - `/feed` (Feed — verify Vant re-skin)

- [ ] **Step 3:** Save screenshots to `docs/superpowers/specs/2026-07-04-aifood-design-system-screenshots/` for archival.

- [ ] **Step 4:** Stop dev server:
  ```bash
  PID=$(lsof -ti :3000 2>/dev/null); [ -n "$PID" ] && kill $PID 2>/dev/null; sleep 1
  ```

## Task 6.5 · Acceptance criteria sign-off

- [ ] **Step 1:** Tick all boxes in `docs/superpowers/specs/2026-07-04-aifood-design-system-design.md §6` (visual / performance / engineering / mobile).

- [ ] **Step 2:** Document completion in `logs/2026-07-04-design-system-rollout.log`:
  ```bash
  echo "Phase 6 Verify complete — all acceptance criteria met" >> logs/2026-07-04-design-system-rollout.log
  ```

- [ ] **Step 3:** Final commit:
  ```bash
  cd /home/ubuntu/projects/AI-food
  git add logs/
  git commit -m "docs(rollout): design system 6-phase rollout complete"
  ```

- [ ] **Step 4:** Hand off to `/comet-archive` skill per project workflow (per AGENTS.md deepwork mode).

---

## Self-Review (per skill checklist)

**1. Spec coverage:**
- §1 tokens → Phase 1 (Task 1.2-1.4) ✓
- §2 components (11) → Phase 3-5 (Tasks 3.2, 3.3, 4.1, 4.2, 5.1) + 5 Vant kept (Phase 2) ✓
- §3 per-page direction → Phase 3 Home / Phase 4 Chat / Phase 5 Profile ✓
- §4 phasing → this plan's 6 phases ✓
- §5 Vant mapping → Phase 2 (Task 2.1) ✓
- §6 acceptance → Phase 6 ✓
- §7 risks → noted in Phase 3-5 tasks (motion = CSS only, hardcoded #22d3ee → Task 3.1, font preload → Task 1.5) ✓
- §8 deferred items → out of scope, listed in spec, not in plan ✓

**2. Placeholder scan:** No TBD / TODO / "implement later" / "fill in details". Each task has exact code, exact commands, exact commit messages.

**3. Type consistency:**
- `--color-nav-active` defined Task 1.2, used Task 3.1 ✓
- `RecommendationCard` props (caption/title/subtitle) defined Task 3.2, used Task 3.4 ✓
- `MoodChip` props (label/icon/selected) defined Task 3.3, used Task 3.4 ✓
- `MessageBubble` prop `role: "user" | "ai"` defined Task 4.1, used Task 4.3 ✓
- `StatCard` props (number/label) defined Task 5.1, used Task 5.2 ✓
- All import paths consistent `@/components/ui/*` ✓
- All Vant var names match docs (`--van-primary-color` etc.) ✓

---

## Estimated Total

| Phase | Tasks | Time |
|---|---|---|
| 1 Foundation | 6 | 2-3h |
| 2 Vant Override | 2 | 30min |
| 3 Home | 5 | 3-4h |
| 4 Chat | 5 | 3-4h |
| 5 Profile | 4 | 2-3h |
| 6 Verify | 5 | 1-2h |
| **Total** | **27** | **12-16h** |

Plan complete and saved to `docs/superpowers/plans/2026-07-04-aifood-design-system-rollout.md`.