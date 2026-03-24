# Plan: User Search & Friends Page

## Context

The Feed.vue page currently has three sub-tabs (大厅/热榜/友榜) and a "大厅" title. The user wants:
1. A new user search feature (fuzzy query, sorted by continuous sign-in days)
2. A follow button added to FeedDetail.vue (post detail page)
3. Feed.vue header restructured: "大厅" title clickable to return to hall, "好友" entry added
4. A new Friends page with three sub-tabs: 聊天列表, 私聊, 好友列表
5. All new UI styled with Sanctuary UI design system

## Task 1: Backend - User Search API

**File:** `backend/src/main/java/com/ai/food/controller/UserController.java`
- Add `GET /api/user/search` endpoint
  - Params: `keyword` (fuzzy match on nickname/username), `page` (default 0), `size` (default 10)

**File:** `backend/src/main/java/com/ai/food/service/user/UserService.java`
- Add `searchUsers(Long currentUserId, String keyword, int page, int size)` method
  - Query `sys_user` table with `LIKE %keyword%` on nickname/username
  - Exclude current user from results
  - For each user, compute `continuousDays` by calling existing `calculateContinuousDays()` (reuse private method - make it package-private or duplicate the logic)
  - Sort results by `continuousDays` DESC
  - Return paginated list with: userId, nickname, avatar, continuousDays

**File:** `backend/src/main/java/com/ai/food/repository/UserRepository.java`
- Add `@Query("SELECT u FROM SysUser u WHERE u.nickname LIKE %:keyword% OR u.username LIKE %:keyword%")` with `Pageable`

## Task 2: Frontend - User Search API

**File:** `frontend/src/api/index.ts`
- Add to `userApi`: `searchUsers(keyword, page, size)` → `GET /api/user/search`

## Task 3: Frontend - Search Page

**New file:** `frontend/src/views/UserSearch.vue`
- Search input with debounce (300ms) at top
- User list cards with: avatar, nickname, continuous sign-in days badge, follow button
- Pagination: load 10 items, pull-down refresh to load next page
- Empty state when no results
- Sanctuary UI styling: glass cards, serif italic headings, soft animations, decorative glows

**File:** `frontend/src/router/index.ts`
- Add route: `/user-search` → `UserSearch.vue`

## Task 4: FeedDetail.vue - Add Follow Button

**File:** `frontend/src/views/FeedDetail.vue`
- In the `user-like-row` section, next to user info, add a follow button
- Only show if `post.userId !== currentUserId`
- Button calls `followApi.toggleFollow(post.userId)` then `followApi.checkFollow(post.userId)` on mount
- Import `followApi` from `@/api` and `useAuthStore` from `@/stores/auth`
- Style: pill-shaped button, Sanctuary UI style (border-radius: 100px, gradient background when following)

## Task 5: Feed.vue - Restructure Header

**File:** `frontend/src/views/Feed.vue`
- Remove "大厅" from sub-tab list (keep only 热榜/友榜 in sub-nav)
- Make the "大厅" `<h1>` title clickable: clicking it resets to the hall feed view (scrolls to top, shows hall content)
- Add a "好友" button/link next to the "大厅" title (inline, styled as a pill or icon button)
- Clicking "好友" navigates to `/friends` route

## Task 6: Friends Page with Sub-Tabs

**New file:** `frontend/src/views/Friends.vue`
- Header: "好友" title (serif italic, Sanctuary style)
- Sub-navigation bar with 3 tabs: 聊天列表 / 私聊 / 好友列表
  - **聊天列表 (Chat List):** WeChat-style conversation list - each item shows: avatar, nickname, last message preview, timestamp, unread badge. Clicking a conversation saves the partner info to localStorage (`lastChatPartner`) and navigates to `/chat-room`. Reuse logic from existing `ChatList.vue`.
  - **私聊 (Direct Chat):** A shortcut tab. On click, check localStorage for `lastChatPartner`. If found, auto-navigate to `/chat-room` with that partner. If no previous chat, show empty state: "还没有聊过天，去聊天列表开始吧". This allows quick access to resume the last conversation without re-browsing the list.
  - **好友列表 (Friend List):** Shows mutual friends (互关好友). Each item: avatar, nickname, unfollow button (取消关注), and chat button. Unfollow calls `followApi.toggleFollow()`. Chat button navigates to `/chat-room`. Reuse logic from existing `Contacts.vue`.

**File:** `frontend/src/router/index.ts`
- Add route: `/friends` → `Friends.vue`

## Task 7: Sanctuary UI Styling

Apply Sanctuary UI design system to new pages (UserSearch.vue, Friends.vue):

- **Typography:** `font-serif italic` for headings (e.g. "搜索用户", "好友")
- **Cards:** `rounded-[2rem]`, `border border-white`, `bg-white/60 backdrop-blur-xl`, decorative glow blobs
- **Color tokens:** Use CSS variables from existing `--color-*` tokens
- **Animations:** `fade-up` entrance animations on load
- **Glass cards:** For user list items and conversation items
- **Buttons:** Pill-shaped (`rounded-full`), gradient backgrounds for primary actions

## File Change Summary

| File | Action |
|------|--------|
| `backend/.../controller/UserController.java` | Modify - add search endpoint |
| `backend/.../service/user/UserService.java` | Modify - add searchUsers method |
| `backend/.../repository/UserRepository.java` | Modify - add search query |
| `frontend/src/api/index.ts` | Modify - add searchUsers API |
| `frontend/src/views/UserSearch.vue` | **Create** - search page |
| `frontend/src/views/FeedDetail.vue` | Modify - add follow button |
| `frontend/src/views/Feed.vue` | Modify - restructure header |
| `frontend/src/views/Friends.vue` | **Create** - friends page with 3 sub-tabs |
| `frontend/src/router/index.ts` | Modify - add 2 new routes |

## Verification

1. `cd /home/meisijiya/opencode-projects/AI-food/backend && ./mvnw compile` - backend compiles
2. `cd /home/meisijiya/opencode-projects/AI-food/frontend && npx vue-tsc --noEmit` - frontend type-checks
3. Visual check: Feed.vue header shows "大厅" clickable + "好友" button, sub-nav only has 热榜/友榜
4. Visual check: Friends.vue has 3 sub-tabs with proper content
5. Visual check: FeedDetail.vue shows follow button next to user info
6. API test: `GET /api/user/search?keyword=xxx&page=0&size=10` returns users sorted by continuousDays
