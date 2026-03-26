# Fix: Deleted posts remain in hot rank and friend feed

## Context

When a user deletes a recommendation record via `RecordService.deleteRecord()` or `batchDeleteRecords()`, the associated FeedPost is soft-deleted in MySQL but Redis entries (hot rank sorted set, friend feed lists) are NOT cleaned up. This causes deleted posts to remain visible in the hot rank (up to 10 min) and friend feed (permanently until pushed out).

The correct cleanup path already exists in `FeedService.unpublish()`, but `RecordService` deletes posts through a different code path without Redis cleanup.

## Changes

### 1. Extract Redis cleanup method from `FeedService.unpublish()`

**File:** `backend/src/main/java/com/ai/food/service/feed/FeedService.java`

Extract lines 504-533 (Redis cleanup logic) into a new public method:

```java
public void cleanRedisForDeletedPost(Long postId, Long userId) {
    // Clean like data
    stringRedisTemplate.delete(LIKE_SET_KEY + postId);
    stringRedisTemplate.delete(LIKE_COUNT_KEY + postId);
    // Clean hot rank
    stringRedisTemplate.opsForZSet().remove(HOT_RANK_KEY, postId.toString());
    // Invalidate hot rank cache (fix: use delete instead of opsForHash().delete)
    stringRedisTemplate.delete(HOT_DETAILS_KEY);
    // Clean friend feed entries for followers
    List<Long> followerIds = followService.getFollowerIds(userId);
    for (Long followerId : followerIds) {
        String friendKey = FRIEND_FEED_KEY + followerId;
        List<String> entries = stringRedisTemplate.opsForList().range(friendKey, 0, -1);
        if (entries != null) {
            for (String entry : entries) {
                try {
                    Map<?, ?> map = objectMapper.readValue(entry, Map.class);
                    if (postId.toString().equals(String.valueOf(map.get("postId")))) {
                        stringRedisTemplate.opsForList().remove(friendKey, 1, entry);
                    }
                } catch (Exception ignored) {}
            }
        }
    }
}
```

Also fix the `opsForHash().delete` → `delete` bug on the HOT_DETAILS_KEY.

Refactor `unpublish()` to call `cleanRedisForDeletedPost()` after soft-delete + photo cleanup.

### 2. Call cleanup in `RecordService.deleteRecord()`

**File:** `backend/src/main/java/com/ai/food/service/record/RecordService.java`

- Add `FeedService` as a dependency
- After soft-deleting the FeedPost, call `feedService.cleanRedisForDeletedPost(post.getId(), post.getUserId())`

### 3. Call cleanup in `RecordService.batchDeleteRecords()`

Same pattern as above, inside the loop.

### 4. Frontend: friendlier toast on deleted post click

**File:** `frontend/src/views/FeedDetail.vue`

Current code (line 277-279):
```ts
catch (e: any) {
    showError(e?.message || '动态不存在或已被删除')
    router.back()
}
```

Change to show a more friendly message:
```ts
catch {
    showError('该内容已被删除')
    router.back()
}
```

## Verification

- `mvn compile` to verify backend changes compile
- Manual test: delete a record, check hot rank and friend feed no longer contain it
- Manual test: click a deleted post from the list, verify friendly error message appears
