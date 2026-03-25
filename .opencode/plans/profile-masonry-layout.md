# Plan: Profile Masonry Layout + Recommendation Reminder Verification

## Context
User wants the desktop Profile page to use a CSS Multi-Column (masonry/waterfall) layout instead of the current 2-column CSS Grid. User cards should span full width, then remaining sections (sign-in, stats, logout) flow into columns like a masonry layout. Sign-in calendar should be more compact on desktop.

The recommendation reminder (Redis primary key → MySQL) flow is already correctly implemented. User confirmed keeping the double Redis deletion safety.

## Changes

### 1. Profile.vue — Masonry Layout (desktop ≥1024px)

**Current**: `bento-grid` uses `display: grid; grid-template-columns: 2fr 1fr;`
**Target**: `bento-grid` uses `display: columns; column-count: 2;` with user card spanning full width

**Template changes**:
- Remove `bento-full` class from user-card div
- User card needs `column-span: all` (CSS property) to stay full-width above the masonry columns

**CSS changes**:
```scss
.bento-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

@media (min-width: 1024px) {
  .bento-grid {
    display: block;           // CSS columns don't need flexbox/grid
    column-count: 2;
    column-gap: 16px;
  }

  .user-card {
    column-span: all;          // full width above masonry
    margin-bottom: 16px;       // spacing after spanning card
  }

  .signin-card,
  .stats-card,
  .logout-btn {
    break-inside: avoid;       // prevent card splitting across columns
    margin-bottom: 16px;
  }
}
```

**Sign-in calendar further compact** (reduce height to balance columns):
- `signed-circle`: desktop 14px (from 16px)
- `calendar-grid gap`: 3px (from 4px)
- `calendar-day aspect-ratio`: remove, use fixed height instead? No, keep aspect-ratio but add `min-height: 0`
- `signin-count`: 14px (from 16px)
- `padding`: 12px (from 16px)

### 2. No backend changes needed
The Redis primary key flow is already correctly implemented:
- Redis stores only `sessionId` string ✓
- Feed.vue reads from Redis, shows reminder if non-null ✓
- Click navigates to `/result?sessionId=xxx`, fetches full data from MySQL ✓
- `updatePhoto`/`deletePhoto` compare sessionId with Redis and delete if match ✓
- Double deletion (RecordController + RecordService) kept for safety ✓

## Files to modify
- `/frontend/src/views/Profile.vue` — CSS only, replace grid with columns for desktop

## Verification
1. `cd /frontend && npm run typecheck` — should pass
2. Desktop (≥1024px): user card full-width, sign-in/stats/logout in 2 columns
3. Sign-in calendar compact, not too tall
4. Recommendation reminder flow unchanged, works correctly
