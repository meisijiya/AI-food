# Plan: 完善用户信息页 + 聊天优化

## Context

Profile.vue is currently read-only — no editing. The "私聊" sub-tab in Friends.vue shows a card but doesn't auto-navigate. Chat messages lack avatars. User info is cached in localStorage but without expiration logic. Avatar thumbnails aren't consistently used across the app.

---

## 1. Backend: Avatar Upload Endpoint

**File:** `backend/src/main/java/com/ai/food/controller/UserController.java`

- Add `POST /api/user/avatar` endpoint
- Accept `MultipartFile`, delegate to a new `uploadAvatar()` method in `FileUploadService`
- Save avatar URL to `SysUser.avatar`, return updated user info

**File:** `backend/src/main/java/com/ai/food/service/upload/FileUploadService.java`

- Add `uploadAvatar(MultipartFile file)` method:
  - Reuse existing validation + Thumbnailator pattern
  - Save to `uploads/avatars/{uuid}_thumb.jpg` (simpler than photos, no date subfolder)
  - Generate thumbnail at **120px** (smaller than photo thumbnails, optimized for circular avatars)
  - Return `{ originalUrl, thumbnailUrl }` without creating a Photo record

**File:** `backend/src/main/java/com/ai/food/controller/UserController.java`

- Add `PUT /api/user/nickname` endpoint — validate non-empty, max 50 chars, update `SysUser.nickname`

- Add `PUT /api/user/password` endpoint — accept `{ oldPassword, newPassword }`, verify old password with BCrypt, update

**File:** `backend/src/main/java/com/ai/food/service/user/UserService.java`

- Add `updateNickname(Long userId, String nickname)`
- Add `updatePassword(Long userId, String oldPassword, String newPassword)` — BCrypt check + update

---

## 2. Frontend: Auth Store Cache with Expiration

**File:** `frontend/src/stores/auth.ts`

- Add `userCacheExpiry` field to localStorage alongside `userInfo`
- On `setUserInfo()`: set expiry = now + 30min + random(0~5min) jitter
- Add `isCacheValid()` computed — checks if `Date.now() < userCacheExpiry`
- On app start / `clearStale()`: if cache expired, clear `userInfo` so Profile.vue refetches

---

## 3. Frontend: Profile.vue Edit Mode

**File:** `frontend/src/views/Profile.vue`

Add edit functionality to the existing user card:

### 3a. Avatar Upload
- Add a small camera icon overlay on the avatar circle (visible in edit mode)
- Click triggers file input → client-side compress (max 512px, quality 0.8, under 500KB)
- Call `userApi.uploadAvatar(compressedFile)`
- On success: update auth store, refresh cached avatar URL

### 3b. Nickname Edit
- Add edit pencil icon next to nickname
- Click toggles to inline `<input>` with save/cancel buttons
- On save: call `userApi.updateNickname(newName)` → update auth store

### 3c. Password Change
- Add a "修改密码" row below the user card (before sign-in card)
- Click opens a simple modal/dialog with: old password, new password, confirm new password
- Validate: non-empty, new passwords match, min 6 chars
- Call `userApi.updatePassword({ oldPassword, newPassword })`

### 3d. General Info Display
- Show user registration date (from `SysUser.createdAt` if available)

---

## 4. Frontend: Avatar Display Everywhere

Ensure avatars use `thumbnailUrl` with click-to-full-image pattern.

### 4a. Image Viewer Utility
**File:** `frontend/src/utils/imageViewer.ts` (new)
- Export a simple function `showOriginalImage(thumbnailUrl: string, originalUrl?: string)`
- Use Vant's `showImagePreview()` or a custom overlay to display the original image
- If no `originalUrl`, use `thumbnailUrl` as fallback

### 4b. Components to Update
- **Profile.vue** — avatar already shown, add click-to-preview
- **Feed.vue** — feed post author avatars (check if already present)
- **FeedDetail.vue** — post author avatar, comment avatars
- **FollowList.vue** — user avatars in follow/follower lists
- **ChatRoom.vue** — add avatars next to messages (both sender and receiver)
- **Friends.vue** — conversation list avatars, friend list avatars (already present)
- **UserSearch.vue** — user avatars (already present)

### 4c. Avatar URL Helper
**File:** `frontend/src/utils/avatar.ts` (new)
- `getAvatarUrl(avatarPath: string | null, size?: 'thumb' | 'original'): string`
- If `avatarPath` is null → return empty string (template shows initial letter)
- If `avatarPath` contains `_thumb` → it's already a thumbnail
- Otherwise, try to derive thumbnail URL from original (if backend convention allows)
- For simplicity: store both `avatar` (thumbnail) and `avatarOriginal` in UserInfo, or just use the single avatar URL (backend serves thumbnail by default)

**Decision:** Backend returns the thumbnail URL as `avatar`. The `originalUrl` is derivable by replacing `_thumb` suffix. But for simplicity, we'll just use the single avatar URL everywhere. Clicking the avatar will show the original via `imageViewer`.

---

## 5. Frontend: Friends.vue "私聊" Tab Behavior

**File:** `frontend/src/views/Friends.vue`

Current behavior: clicking "私聊" tab shows a card, user clicks again to open chat.

New behavior:
- When user clicks "私聊" tab (`switchTab('direct')`):
  - If `lastPartner` exists in localStorage → **immediately navigate** to `/chat-room` with partner info
  - If no `lastPartner` → show a friendly empty state: "还没有聊过天呢～ 去好友列表找人聊天吧！" with a decorative icon

Update `switchTab('direct')`:
```ts
function switchTab(tab: string) {
  activeTab.value = tab
  if (tab === 'chat') {
    fetchConversations()
  } else if (tab === 'direct') {
    loadLastPartner()
    if (lastPartner.value) {
      openDirectChat()
      return
    }
  } else if (tab === 'friends') {
    fetchFriends()
  }
}
```

The empty state template for "私聊" when no last partner stays the same (friendly message).

---

## 6. Frontend: ChatRoom.vue Avatars

**File:** `frontend/src/views/ChatRoom.vue`

Add avatars to messages:
- Pass avatar info via route query or fetch from conversation list
- Template change: add `<img>` or avatar-initial next to each message bubble
- Self messages: avatar on right side
- Other messages: avatar on left side

Need to pass partner's avatar in route query from Friends.vue, ChatList.vue, etc.

Update all navigation to `/chat-room` to include `avatar` query param.

---

## Files to Modify

| File | Action |
|------|--------|
| `backend/.../UserController.java` | Add avatar upload, nickname update, password update endpoints |
| `backend/.../UserService.java` | Add updateNickname, updatePassword methods |
| `backend/.../FileUploadService.java` | Add uploadAvatar method |
| `frontend/src/stores/auth.ts` | Add cache expiration logic |
| `frontend/src/views/Profile.vue` | Add edit mode (avatar, nickname, password) |
| `frontend/src/utils/imageViewer.ts` | New — click-to-full-image utility |
| `frontend/src/views/ChatRoom.vue` | Add avatars to messages |
| `frontend/src/views/Friends.vue` | "私聊" tab auto-navigates |
| `frontend/src/views/FeedDetail.vue` | Ensure post author avatar is clickable |
| `frontend/src/views/FollowList.vue` | Ensure avatars use thumbnails + click to full |

## Verification

1. `mvn compile` — backend compiles
2. `npx vue-tsc --noEmit` — frontend compiles
3. Manual test: Profile → edit nickname → save → verify updated
4. Manual test: Profile → upload avatar → verify thumbnail shows
5. Manual test: Friends → 私聊 → auto-navigates to last chat partner
6. Manual test: ChatRoom → messages show avatars
7. Manual test: Click avatar → shows full image
