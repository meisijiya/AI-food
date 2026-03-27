# Soft Delete Cleanup Consistency Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make chat deletion behavior follow one rule: request-time flows only soft-delete or hide data, and scheduled cleanup owns all hard deletion.

**Architecture:** Keep the current chat clear model based on `clearedAt`/`hiddenAt` plus per-row soft-delete flags, but remove request-time hard deletion from `ChatService.clearConversation()`. Preserve `CleanupSoftDeletedJob` as the single hard-delete owner and add focused regression tests around the changed service behavior.

**Tech Stack:** Java 21, Spring Boot, Spring Data JPA, Quartz, JUnit 5, Mockito

---

### Task 1: Add regression tests for chat clear behavior

**Files:**
- Create: `backend/src/test/java/com/ai/food/service/chat/ChatServiceTest.java`
- Modify: `backend/src/main/java/com/ai/food/service/chat/ChatService.java`

- [ ] **Step 1: Write the failing test**

Add a Mockito-based unit test that calls `clearConversation()` when both users already have `clearedAt` values and asserts that request-time code does not invoke `hardDeleteByConversationId(...)` on message/photo/file repositories.

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=ChatServiceTest test`
Expected: FAIL because the current implementation still hard-deletes in `clearConversation()`.

- [ ] **Step 3: Write minimal implementation**

Remove the request-time hard-delete branch from `clearConversation()` and update the method comments so they describe the new single-owner cleanup rule.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=ChatServiceTest test`
Expected: PASS

### Task 2: Keep scheduled cleanup as the only hard-delete owner

**Files:**
- Modify: `backend/src/main/java/com/ai/food/job/CleanupSoftDeletedJob.java`
- Modify: `backend/src/main/java/com/ai/food/service/chat/ChatService.java`
- Test: `backend/src/test/java/com/ai/food/service/chat/ChatServiceTest.java`

- [ ] **Step 1: Add or extend a focused test expectation**

Extend service-level coverage so the test suite documents that `hardDeleteClearedMessages(...)` remains a cleanup helper and is not part of online clear behavior.

- [ ] **Step 2: Run test to verify it fails or stays targeted**

Run: `mvn -q -Dtest=ChatServiceTest test`
Expected: the suite either fails before the implementation is complete or confirms the coverage target still guards the changed rule.

- [ ] **Step 3: Tighten cleanup-path comments/behavior only if needed**

Keep cleanup ownership centralized in `CleanupSoftDeletedJob`; avoid adding new request-time cleanup calls.

- [ ] **Step 4: Re-run focused tests**

Run: `mvn -q -Dtest=ChatServiceTest test`
Expected: PASS

### Task 3: Verify repository-wide behavior still builds cleanly

**Files:**
- Modify: `backend/src/main/java/com/ai/food/service/chat/ChatService.java`
- Modify: `backend/src/test/java/com/ai/food/service/chat/ChatServiceTest.java`

- [ ] **Step 1: Run backend verification**

Run: `mvn compile -q`
Expected: PASS

- [ ] **Step 2: Run frontend verification**

Run: `npm run build`
Expected: PASS

- [ ] **Step 3: Review diff for scope control**

Run: `git diff -- backend/src/main/java/com/ai/food/service/chat/ChatService.java backend/src/main/java/com/ai/food/job/CleanupSoftDeletedJob.java backend/src/test/java/com/ai/food/service/chat/ChatServiceTest.java docs/superpowers/specs/2026-03-28-soft-delete-cleanup-consistency-design.md docs/superpowers/plans/2026-03-28-soft-delete-cleanup-consistency.md`
Expected: only rule-consistency and test/planning changes
