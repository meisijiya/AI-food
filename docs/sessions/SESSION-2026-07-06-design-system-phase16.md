# SESSION 2026-07-06: Design System Rollout Phase 16+ Deep Polish

## Session goal
继续 Phase 16+ 候选。Deepwork mode + ponytail lite。Close token coverage gap across remaining views.

## Final state

| Metric | Value |
|---|---|
| Phase 1-16 commits | 21 |
| Views tokenized | 19/19 (100% bg utility) |
| Token coverage | 95.4% (801 var(--) / 39 hardcoded) |
| Self-written components | 6 |
| Vant overrides | 3 CSS vars |
| Design tokens | 80+ |
| mvn tests | 168/168 PASS |
| vue-tsc errors | 0 |
| vite build | success, JS main 119.4 KB gzip 46.3 KB |

## Phase 16 timeline (this session)

| Phase | Work | Commit |
|---|---|---|
| 16.0 | Deepwork plan + oracle plan review | (no commit, plan + logs) |
| 16.1 | bg utility on 12 views + 11 obsolete bg-glow divs removed | eb23331 |
| 16.2 | sed-sweep 5 views (28→26 hardcoded): 2 exact-match swaps + 4 ponytail comments | e6f6696 |
| 16.3 | Final validation: mvn 168/168 + tsc 0 + build success + 95% coverage | (no commit) |
| 16.4 | Oracle final review: hold — 3 critical issues flagged | logs/2026-07-06-10-00 |
| 16.5 | Oracle fixes: added missing --shadow-glow-hover token (5 theme blocks) + 3 low-hanging focus-ring-color swaps + committed Profile.vue dirty | 52c3798 |

## Oracle review log
- 2026-07-06 09:15 plan review: accept-with-changes (simplified 11-bucket plan to 2 commits)
- 2026-07-06 10:00 final review: hold → 3 critical issues fixed in 16.5

## Final 39 hardcoded breakdown

| View | Hardcoded | Type |
|---|---|---|
| Home.vue | 4 | shadow rgba (warm decoration) |
| Chat.vue | 4 | shadow rgba + 1 success indicator glow |
| FeedDetail.vue | 7 | 1 cyan gradient (ponytail) + 6 text/overlay |
| Match.vue | 7 | shadow rgba + cyan gradient end |
| Profile.vue | 7 | shadow rgba + 2 border (over-overlay-fg-active) |
| RecordDetail.vue | 2 | shadow rgba |
| Share.vue | 4 | 1 cyan gradient + 1 dark hex + 2 text |

All are element-specific design decisions (one-time shadow values,
decorative gradient endpoints, status indicator colors) — not
generalizable. Per @oracle verdict: token catalog frozen, do not
grow for one-time uses.

## Risk: --shadow-glow-hover broken (fixed in 16.5)

Phase 14 commit message claimed to add --shadow-glow-hover token but
the actual token definition was MISSING. 4 files silently referenced
an undefined CSS variable, breaking hover state shadow on:
- Profile.vue recommendation card
- Login.vue login button
- Records.vue record card
- ResultRecommendation.vue recommendation card

Fixed by adding --shadow-glow-hover to all 5 theme blocks in
_tokens.scss.

**Lesson for future**: when commit message claims "added token X",
verify the actual file diff includes the definition, not just the
swap in view files.

## Out of scope (Phase 17+ candidates per @oracle)

| Candidate | Status |
|---|---|
| Close non-shadow hardcoded gap | Recommend |
| 187 px / radius migration | Skip (low ROI) |
| App.vue pill nav motion polish | Skip (subjective; needs designer) |
| frontend-UI/ design preview sync | Skip (frontend tooling) |
| --overlay-backdrop token (7 rgba(11,15,16,X)) | Skip (YAGNI — 4 unique alphas) |

## Files changed (21 commits Phase 1-16)

```
frontend/src/styles/_tokens.scss        — 80+ tokens, 5 theme blocks
frontend/src/stores/theme.ts            — Pinia store, 3 modes
frontend/src/components/ui/ThemeSwitcher.vue
frontend/src/main.ts                   — theme boot
frontend/src/App.vue                   — pill nav 100% token
frontend/src/views/Home.vue
frontend/src/views/Chat.vue
frontend/src/views/Profile.vue
frontend/src/views/Feed.vue
frontend/src/views/Login.vue
frontend/src/views/Records.vue
frontend/src/views/Result.vue
frontend/src/views/ProfileEdit.vue
frontend/src/views/ChatList.vue
frontend/src/views/ChatRoom.vue
frontend/src/views/Contacts.vue
frontend/src/views/Friends.vue
frontend/src/views/FollowList.vue
frontend/src/views/Match.vue
frontend/src/views/Notifications.vue
frontend/src/views/Share.vue
frontend/src/views/UserSearch.vue
frontend/src/views/FeedDetail.vue
frontend/src/views/RecordDetail.vue
frontend/src/components/ui/{RecommendationCard,MoodChip,
  MessageBubble,DSLoadingOrb,StatCard}.vue
docs/superpowers/specs/2026-07-04-aifood-design-system-design.md
docs/superpowers/specs/2026-07-05-aifood-design-system-usage.md
docs/superpowers/specs/2026-07-04-aifood-design-system-screenshots/
.slim/deepwork/ai-food-design-system-rollout.md
.slim/deepwork/ai-food-design-phase16-deepen-remaining-views.md
```

## Working tree state at session end

- Design system rollout: clean (committed)
- admin-web / backend / .gitignore / docs: dirty, user's separate
  work, intentionally untouched (Phase 16 scope was design system
  rollout only)

## Sign-off

Phase 16 + 16.5 + 17 closing protocol complete. Ready for new work.
Comet-archive not applicable (rollout was tracked under
docs/superpowers/, not OpenSpec).

To resume: read
- `.slim/deepwork/ai-food-design-system-rollout.md` (full history)
- `.slim/deepwork/ai-food-design-phase16-deepen-remaining-views.md` (this session)
- `docs/superpowers/specs/2026-07-05-aifood-design-system-usage.md` (token reference)