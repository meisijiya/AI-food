# Fix: Avatar upload bug + Login with Email + Username as auto-generated ID

## Context

Two issues:
1. **Avatar upload doesn't display after update**: `ProfileEdit.vue:173` treats the upload response as a URL string, but the backend returns a full `SysUser` object. After API interceptor unwrapping, `url` is a SysUser object, not a string.
2. **Login change**: Switch from username/password login to email/password login. Username becomes a system-generated unique identifier that can't be modified. Nickname becomes the user-editable display name (can repeat).

---

## Part 1: Avatar Upload Bug

**Root cause**: `ProfileEdit.vue:173-178`
```typescript
const url = await userApi.uploadAvatar(compressed)
currentAvatar.value = url      // url is SysUser object, not a string!
authStore.setUserInfo({ ...userInfo.value!, avatar: url })
```

Backend `UserController.uploadAvatar()` returns `ApiResponse<SysUser>`. After interceptor unwrapping, this is the full SysUser object. Frontend should use `.avatar` to extract the URL.

### Changes

**File:** `frontend/src/views/ProfileEdit.vue` (lines 171-178)

```typescript
// Before
const url = await userApi.uploadAvatar(compressed)
currentAvatar.value = url
authStore.setUserInfo({ ...userInfo.value!, avatar: url })

// After
const updatedUser = await userApi.uploadAvatar(compressed)
const avatarUrl = updatedUser.avatar
currentAvatar.value = avatarUrl
authStore.setUserInfo({
  userId: updatedUser.id,
  username: updatedUser.username,
  nickname: updatedUser.nickname || updatedUser.username,
  email: updatedUser.email,
  avatar: avatarUrl
})
```

---

## Part 2: Email Login + Auto-generated Username

### Backend Changes

#### 1. `LoginRequest.java` — 改为 email
- Remove `username` field
- Add `email` field with `@NotBlank` + `@Email` validation

#### 2. `AuthService.login()` — findByEmail
- `userRepository.findByUsername(req.getUsername())` → `userRepository.findByEmail(req.getEmail())`
- Error message: "邮箱或密码错误"

#### 3. `AuthService.register()` — auto-generate username
- Remove username from request handling
- Auto-generate unique username (e.g., `"user" + 8-digit random number`, check uniqueness)
- Nickname defaults to auto-generated username if not provided
- Remove `existsByUsername(req.getUsername())` check (auto-generated, always unique)

#### 4. `RegisterRequest.java` — remove username
- Remove `username` field
- Keep `password`, `email`, `code`, `nickname`

#### 5. `UserController.java` — remove updateUsername endpoint
- Remove `PUT /api/user/username` endpoint (if it exists)
- Username cannot be modified

#### 6. `UserService.java` — remove updateUsername method
- Remove `updateUsername()` method (if it exists)

#### 7. `UserRepository.java` — search by nickname only
- `searchUsers` query: remove `u.username LIKE` part, only search by `u.nickname LIKE`

#### 8. `schema.sql` — no schema changes needed
- `username` remains `VARCHAR(50) UNIQUE NOT NULL` (auto-generated)
- `nickname` remains `VARCHAR(50)` (user-editable, no unique constraint)

### Frontend Changes

#### 9. `Login.vue` — login form: email input
- Login form: change `v-model="loginForm.username"` to `v-model="loginForm.email"`, type `email`, placeholder "邮箱"
- Login form icon: change from user icon to email icon
- `handleLogin()`: send `{ email: loginForm.email, password }` instead of `{ username, password }`
- Register form: remove username input (system auto-generates)
- `handleRegister()`: send `{ email, password, code }` (no username, no nickname)
- `sendCode`: change from `registerForm.username` to use email for rate limiting

#### 10. `Login.vue` — register form: simplify
- Remove username input field from register form
- Keep: email, verification code, password
- Remove `nickname: registerForm.username` from register API call

#### 11. `ProfileEdit.vue` — show username (read-only)
- Add a new section showing username (用户ID) as read-only, non-editable
- Nickname section stays editable

#### 12. `Profile.vue` — show username
- Under nickname, show username as secondary text (用户ID: xxx)

#### 13. `UserSearch.vue` — search by nickname
- Change placeholder from "搜索昵称或用户名..." to "搜索昵称..."

#### 14. `auth.ts` store — no changes needed
- UserInfo interface already has `username`, `nickname`, `email` fields

---

## File Change Summary

| File | Change |
|------|--------|
| `frontend/src/views/ProfileEdit.vue` | Fix avatar response extraction + add read-only username section |
| `frontend/src/views/Profile.vue` | Show username below nickname |
| `frontend/src/views/Login.vue` | Email login + remove username from register |
| `frontend/src/views/UserSearch.vue` | Placeholder text update |
| `backend/.../dto/LoginRequest.java` | username → email |
| `backend/.../dto/RegisterRequest.java` | Remove username field |
| `backend/.../service/auth/AuthService.java` | findByEmail + auto-generate username |
| `backend/.../repository/UserRepository.java` | Search by nickname only |
| `backend/.../controller/UserController.java` | Remove username update endpoint (if exists) |
| `backend/.../service/user/UserService.java` | Remove updateUsername (if exists) |

## Verification
- `mvn compile` to verify backend changes
- Manual test: register with email → system generates username
- Manual test: login with email + password
- Manual test: upload avatar → verify display
- Manual test: profile shows read-only username + editable nickname
