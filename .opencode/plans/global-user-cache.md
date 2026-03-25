# Plan: Global User Cache for Optimistic Comments

## Problem
When posting a comment on FeedDetail, the optimistic update reads `authStore.userInfo` for avatar and nickname. But the cache has a 30-minute expiry — after that, `userInfo` becomes `null`, and comments show empty avatar/nickname until the user visits Profile.vue (which re-fetches user info from API).

## Root Cause
`auth/stores/auth.ts:35-39` — `isCacheExpired()` checks `userInfoExpiry` in localStorage. If expired, the constructor sets `userInfo = null` (line 49-51). The only place that re-loads user info is Profile.vue's `fetchData()`. No global init exists.

## Solution: Remove Cache Expiry

User info (nickname, avatar) rarely changes. The cache should persist in localStorage for the lifetime of the login session, matching the user's request that "用户的信息要全局缓存".

### Changes

#### 1. `frontend/src/stores/auth.ts` — Remove expiry mechanism

- Remove `EXPIRY_KEY`, `BASE_EXPIRE_MS`, `JITTER_MAX_MS`, `isCacheExpired()`, `setCacheExpiry()` 
- In store init: always load `userInfo` from localStorage if token exists (no expiry check)
- In `setUserInfo()`: just save to localStorage (no expiry set)
- In `logout()`: keep clearing `userInfo` from localStorage
- Keep `clearStale()` but simplify — only check token, not expiry

#### 2. `frontend/src/views/ProfileEdit.vue` — Ensure cache updates after edit

Verify that after nickname/avatar update, `authStore.setUserInfo()` is called to refresh the global cache. (This ensures the cached data stays in sync when user edits profile.)

## Files to modify
- `frontend/src/stores/auth.ts` — remove expiry, simplify cache
- `frontend/src/views/ProfileEdit.vue` — verify `setUserInfo` is called after edits (no change if already done)

## Verification
1. `npm run typecheck` passes
2. Login → user info in localStorage → comment shows avatar/nickname immediately
3. After 30+ minutes → user info still in cache → comment still works
4. ProfileEdit → change nickname → next comment shows new nickname
