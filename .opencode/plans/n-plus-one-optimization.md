# N+1 Query Optimization Plan

## Goal
Eliminate all N+1 query patterns and add batch processing where multiple DB/Redis calls per item can be consolidated into single round-trips.

## Changes

### Step 1: Add Batch Repository Methods (no service changes yet)

**Repositories to modify:**

1. `RecommendationResultRepository.java` — add `findBySessionIdIn(List<String>)` and `softDeleteBySessionIdIn(List<String>)`
2. `QaRecordRepository.java` — add `softDeleteBySessionIdIn(List<String>)`
3. `CollectedParamRepository.java` — add `softDeleteBySessionIdIn(List<String>)`
4. `ConversationSessionRepository.java` — add `softDeleteBySessionIdIn(List<String>)`
5. `FeedPostRepository.java` — add `softDeleteBySessionIdIn(List<String>)` (replaces `softDeleteBySessionId` in batch context)
6. `FeedCommentRepository.java` — add `softDeleteByPostIdIn(List<Long>)`
7. `UserFollowRepository.java` — add `isMutualFollow(Long, Long)` single query
8. `PhotoRepository.java` — add `findByRelatedSessionIdInOrderByCreatedAtDesc(List<String>)`

### Step 2: FeedService — Batch `enrichUserInfo`

**File:** `FeedService.java`

- Add new helper method `batchEnrichUserInfo(List<Map<String, Object>> items, Function<Map<String, Object>, Long> userIdExtractor)`:
  - Collect all unique userIds from items
  - Single `userRepository.findByIdIn(userIds)` call
  - Build `Map<Long, SysUser>` lookup
  - Enrich each item from the map (nickname + avatar)

- **Fix 1:** `getPublicFeedList()` (lines 128-133) — replace inline `enrichUserInfo` loop with `batchEnrichUserInfo`
- **Fix 2:** `getHotRank()` (lines 211-226) — replace inline `enrichUserInfo` loop with `batchEnrichUserInfo`
- **Fix 3:** `getComments()` (lines 500-511) — replace inline `enrichUserInfo` loop with `batchEnrichUserInfo`
- **Fix 4:** `pushToFollowersFeeds()` (lines 348-354) — use `executePipelined()` to batch LPUSH+LTRIM
- **Fix 5:** `cleanRedisForDeletedPost()` (lines 573-587) — use `executePipelined()` to batch LRANGE+LREM
- **Fix 6:** `toggleLike()` — remove duplicate `findById` at line 432 (reuse from line 443)

### Step 3: FollowService — Batch User Fetches

**File:** `FollowService.java`

- **Fix 7:** `getFollowingList()` (lines 56-61) — `findByIdIn(pageIds)` then iterate
- **Fix 8:** `getFollowersList()` (lines 82-88) — `findByIdIn(pageIds)` then iterate
- **Fix 9:** `getMutualFriendsList()` (lines 143-148) — `findByIdIn(pageIds)` then iterate
- **Fix 10:** `isMutualFollow()` (lines 130-133) — use single `userFollowRepository.isMutualFollow()` query

### Step 4: ChatService — Batch User Fetches

**File:** `ChatService.java`

- **Fix 11:** `getConversationList()` (lines 195-208) — collect `otherUserIds`, single `findByIdIn`, build map, enrich loop
- **Fix 12:** `getContacts()` (lines 293-301) — `findByIdIn` for users + pipeline Redis `HASKEY` for online status

### Step 5: RecordService — Batch Fetches + Batch Deletes

**File:** `RecordService.java`

- **Fix 13:** `getRecordList()` (lines 59-90) — collect sessionIds, batch fetch `recommendationResult` and latest `photo` per session, build maps, then map from maps
- **Fix 14:** `batchDeleteRecords()` (lines 117-137) — replace loop with batch operations:
  1. `feedPostRepository.findBySessionIdIn(sessionIds)` → get all posts
  2. Collect postIds, batch `feedCommentRepository.softDeleteByPostIdIn(postIds)`
  3. Batch `feedPostRepository.softDeleteBySessionIdIn(sessionIds)`
  4. Clean Redis for each post (loop, but each is just a few Redis calls)
  5. Batch photo soft-delete (or keep as loop since it also deletes files)
  6. Batch `qaRecordRepository.softDeleteBySessionIdIn(sessionIds)`
  7. Batch `collectedParamRepository.softDeleteBySessionIdIn(sessionIds)`
  8. Batch `recommendationResultRepository.softDeleteBySessionIdIn(sessionIds)`
  9. Batch `conversationSessionRepository.softDeleteBySessionIdIn(sessionIds)`

### Step 6: NotificationService — Batch User Fetches

**File:** `NotificationService.java`

- **Fix 15:** `getNotifications()` (lines 134-148) — collect commenterIds, single `findByIdIn`, build avatar map, enrich from map

## Verification
- Run `mvn compile` after all changes to verify compilation
- Verify no regressions in LSP errors (only expected Lombok false positives)
