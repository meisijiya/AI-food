## Background

The project already implements soft-delete behavior for feed comments, chat photos, and chat files, with a Quartz job that performs physical cleanup later. However, chat clearing still contains an online hard-delete path in `ChatService.clearConversation()`, which violates the agreed rule: all online deletion paths should only affect visibility and soft-delete state, while all hard deletion and physical file cleanup should happen during the scheduled cleanup job.

This design focuses on bringing deletion behavior back to one consistent rule without introducing a broad lifecycle refactor.

## Goal

Unify deletion behavior across chat and feed flows so that:

1. Online requests only mark records as deleted or hidden.
2. Daytime user-visible behavior does not depend on immediate hard deletion.
3. `CleanupSoftDeletedJob` becomes the only place that performs hard deletion and physical file cleanup.

## Scope

In scope:

- Remove the immediate hard-delete path triggered when both sides clear a conversation.
- Keep chat photo and chat file deletion aligned with the existing soft-delete model.
- Ensure chat history visibility continues to rely on `clearedAt` and per-user delete flags.
- Make scheduled cleanup the single hard-delete entry point for chat media and cleared conversation data.
- Audit existing request-time deletion flows in the touched area and reject any request-path hard-delete or physical file deletion behavior.

Out of scope:

- Reworking the full deletion model across all entities.
- Introducing a shared deletion framework or new state machine.
- Large repository/API redesigns.

## Current Problem

### Confirmed inconsistency

`backend/src/main/java/com/ai/food/service/chat/ChatService.java` still hard-deletes cleared conversation data during the online request path:

- `clearConversation()` sets `clearedAt`/`hiddenAt`
- then checks whether both users have cleared
- then immediately calls `hardDeleteClearedMessages(conversationId)`

This breaks the agreed behavior because users can trigger hard deletion before the scheduled cleanup window.

### Why this matters

- The system rule becomes harder to reason about because some data is cleaned immediately and some is deferred.
- Cleanup responsibility is split between request handling and Quartz jobs.
- Future query bugs become more likely because visibility and storage cleanup are mixed.

## Recommended Approach

Use a single operational rule:

- **Request-time deletion** changes visibility and writes soft-delete state.
- **Scheduled cleanup** performs physical deletion and hard deletion.

This keeps user experience immediate while preserving a predictable backend lifecycle.

## Design

### 1. Online deletion semantics

#### Feed comments

- Keep the existing behavior: comment deletion marks the record as soft-deleted.
- Keep synchronous counter and notification consistency fixes.
- Do not hard-delete comment rows or their attached files during the request.

#### Chat photos and chat files

- Keep the current per-user delete markers:
  - sender delete -> `isSenderDelete`
  - receiver delete -> `isReceiverDelete`
- When both sides have deleted the same media, only mark `isDeleted=true`.
- Do not remove physical files or hard-delete DB rows in request handlers.

#### Clear conversation

- `clearConversation()` continues to set user-specific `clearedAt` and `hiddenAt` values.
- `clearConversation()` continues to soft-delete conversation-scoped rows before the clear timestamp, matching the current storage model for cleared chat history.
- It continues to clear unread state and update last-message preview as needed.
- It must no longer immediately call any hard-delete method when both users have cleared.
- The result is:
  - the conversation becomes invisible or filtered for the current user immediately
  - storage cleanup is deferred entirely to the scheduled job

### 2. Query semantics

Chat history visibility remains query-driven rather than cleanup-driven.

- `getChatHistory()` keeps using `clearedAt` as the boundary for what a user can still see.
- `shouldShowMessage()` keeps filtering image/file messages using the current user's delete flags.
- This guarantees that:
  - after a delete action, the data disappears immediately for the deleting user
  - refresh behavior stays correct even before the nightly cleanup runs

No user-facing flow should depend on rows already being physically deleted.

### 3. Cleanup ownership

`CleanupSoftDeletedJob` becomes the only hard-delete owner.

It is responsible for:

- deleting soft-deleted feed comments and related files
- deleting soft-deleted feed photos and related files
- deleting soft-deleted chat photos and related files
- deleting soft-deleted chat files and related files
- deleting old soft-deleted chat messages
- deleting already-soft-deleted conversation-scoped chat rows for conversations that have become fully cleared and are eligible for cleanup

If helper methods like `hardDeleteClearedMessages()` remain in `ChatService`, they are treated as job-internal cleanup helpers rather than request-path behavior.

### 3.1 Fully cleared conversation eligibility

For this change, a conversation is considered **fully cleared** when:

- `clearedAtUser1` is not null, and
- `clearedAtUser2` is not null

Cleanup eligibility for this spec is immediate at the next scheduled cleanup run; no additional age threshold is introduced in this iteration.

For an eligible fully cleared conversation, the cleanup path may hard-delete only data that is already part of the conversation-scoped soft-delete lifecycle:

- soft-deleted `chat_message` rows for that conversation
- soft-deleted `chat_photo` rows for that conversation, plus physical photo files
- soft-deleted `chat_file` rows for that conversation, plus physical file contents

This spec does not expand cleanup to non-soft-deleted rows. The invariant is:

- online clear marks visibility boundaries and soft-delete state only; it never hard-deletes
- scheduled cleanup removes conversation-scoped rows that are already soft-deleted and now belong to a fully cleared conversation

Under the current chat-clear flow, ordinary cleared conversation messages become cleanup-eligible because the clear action already soft-deletes conversation-scoped rows before the clear timestamp; this iteration keeps that behavior and only removes the online hard-delete step.

### 3.2 Request-path audit rule

The implementation must explicitly review request-time deletion entry points in the touched chat/feed area and ensure they do not perform:

- hard-delete repository operations
- physical file deletion
- direct invocation of cleanup helpers intended for scheduled cleanup

At minimum, this audit covers:

- `clearConversation()`
- `deleteChatPhoto()`
- `deleteChatFile()`
- feed comment deletion path

If any additional request-path hard-delete behavior is discovered during implementation in these touched areas, it must be removed or moved behind the scheduled cleanup path.

### 4. Cleanup order

To reduce partial-state risk, cleanup should maintain a stable order:

1. Mark expired chat media as soft-deleted where applicable.
2. Clean up conversation-scoped cleared data through the scheduled path only.
3. Delete physical chat media files for rows already marked as soft-deleted.
4. Hard-delete the associated chat media rows.
5. Hard-delete remaining soft-deleted chat messages.

The important invariant is not the exact sequence but that request handlers never perform hard deletion, and cleanup ownership stays centralized.

## File-Level Changes

### `backend/src/main/java/com/ai/food/service/chat/ChatService.java`

- Remove the immediate `hardDeleteClearedMessages(conversationId)` call from `clearConversation()`.
- Keep the method focused on visibility state, unread cleanup, and conversation metadata updates.
- Keep `deleteChatPhoto()` and `deleteChatFile()` as soft-delete-only flows.
- If `hardDeleteClearedMessages()` is retained, document and use it only from the cleanup path.

### `backend/src/main/java/com/ai/food/job/CleanupSoftDeletedJob.java`

- Keep this job as the only hard-delete entry point.
- Ensure the job handles fully cleared conversation cleanup instead of relying on request-time deletion.
- Preserve physical file cleanup here rather than in controllers or service request paths.

## Risks and Mitigations

### Risk: conversation metadata becomes stale

`clearConversation()` currently updates last-message preview after clearing. Removing the immediate hard delete must not regress the preview or list visibility.

Mitigation:

- keep existing preview recalculation logic in place
- rely on `clearedAt`/`hiddenAt` for visibility, not hard deletion

### Risk: cleanup order leaves orphaned rows briefly

If cleanup removes files and rows in the wrong sequence, there can be temporary mismatches between media rows and message rows.

Mitigation:

- keep a single scheduled cleanup owner
- preserve a stable, explicit cleanup sequence

### Risk: visible behavior changes after refresh

If chat history endpoints accidentally depend on hard deletion, users may see deleted content return before the nightly job runs.

Mitigation:

- preserve query-time filtering with `clearedAt` and per-user delete flags
- verify refresh behavior explicitly

## Verification Plan

### Build verification

- Backend: `mvn compile -q`
- Frontend: `npm run build`

### Manual scenarios

1. Delete a chat photo as one side only:
   - deleting user no longer sees it
   - other user still sees it
2. Delete the same chat photo/file from both sides:
   - both users stop seeing it immediately
   - row/file cleanup is deferred to the scheduled job
3. Clear a conversation from one side:
   - old messages are hidden only for that user
   - the other side is unaffected
4. Clear a conversation from both sides:
   - no immediate hard deletion occurs
   - scheduled cleanup performs later storage cleanup

### State verification checks

The implementation verification should also confirm storage-level behavior, not only UI behavior:

1. Immediately after an online delete/clear action:
   - content is no longer returned to the acting user where expected
   - corresponding DB rows still exist
   - corresponding physical media files still exist
   - after both users clear the same conversation, the same conversation/message rows still exist immediately after the request
2. After `CleanupSoftDeletedJob` runs:
   - eligible soft-deleted DB rows are removed
   - eligible physical media files are removed
3. Request-path protection:
   - no touched controller/service request path directly calls hard-delete repository methods or physical file deletion helpers

## Success Criteria

- No request-time path performs hard deletion for chat clear, chat photo deletion, or chat file deletion.
- No touched request-time delete path performs hard deletion or physical file deletion.
- User-visible deletion behavior remains immediate.
- Refresh behavior stays consistent before the cleanup job runs.
- `CleanupSoftDeletedJob` is the single owner of hard deletion and physical cleanup.
