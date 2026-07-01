# Task 8 Report — Flyway V4 Migration

## Status: DONE_WITH_CONCERNS

## What was done

- Created `backend/ai-food-app/src/main/resources/db/migration/V4__add_token_and_role_audit.sql`
  combining four operations: qa_record token fields, sys_user.role, admin_audit_log table,
  and promote smokeuser (id=1) to ADMIN.
- Backed up DB to `/tmp/ai_food_backup_20260629.sql` (25KB, pre-V4 state at first; refreshed
  to post-V4 state after final apply — 28KB).
- Applied V4 via `mvn flyway:migrate` so Flyway itself records the correct checksum in
  `flyway_schema_history` (cheaper than computing Flyway's parser-aware CRC32 by hand).
- Verified schema: `qa_record` has 4 new columns, `sys_user` has `role` column, `admin_audit_log`
  exists with all 11 columns, `smokeuser.role='ADMIN'`. `flyway_schema_history` shows V1-V4
  all `success=1`.
- Verified `ai-food-app` starts in prod profile against the new schema and `POST /api/auth/login`
  for `smoke@aifood.local` returns HTTP 200 with a valid JWT.
- Commit: `35e358c feat(db): V4 add token fields + sys_user.role + admin_audit_log; promote smokeuser to ADMIN`

## Concerns (transparency)

1. **Brief's index was wrong.** The original brief had
   `CREATE INDEX idx_qa_created_user ON qa_record(created_at, user_id)`. The `qa_record`
   table has no `user_id` column — it is keyed by `session_id`. First manual apply of V4
   failed on that index after the ALTERs had already run, leaving the DB in a partially
   migrated state. I dropped the partial additions, re-dumped a clean backup, replaced the
   index with `idx_qa_created_at(created_at)` (annotated with a `ponytail:` comment), and
   re-ran V4. **Anyone relying on the brief's exact index definition will see a diff.**

2. **Brief's env var names were wrong.** The verify step's
   `DB_URL` / `DB_USER` / `DB_PASSWORD` env vars do not bind to anything in
   `application-prod.yml`, which uses `MYSQL_HOST` / `MYSQL_PORT` / `MYSQL_DATABASE` /
   `MYSQL_USER` / `MYSQL_PASSWORD`. Without correcting these, the app tries to resolve
   hostname `mysql` and dies. Used the real names for the verify.

3. **Pre-existing app bug, not V4-related.** The `ai-food-app` prod profile does not
   configure `JavaMailSender`, so the `EmailService` bean fails to autowire and Spring
   context fails to start. This is independent of V4 (Flyway ran fine and validated
   4 migrations before the bean wiring failure). Worked around the verify by setting
   `SPRING_MAIL_HOST/PORT/USERNAME/PASSWORD` env vars so Spring Boot's mail auto-config
   creates a default `JavaMailSender`. **Task 9 / future work should add a real mail
   config or guard the `EmailService` so it doesn't hard-require `JavaMailSender` in
   environments that don't send email.**

4. **Verify approach: maven plugin instead of bare `mysql`.** The brief said to apply
   V4 manually with `mysql ... < V4.sql`, but that leaves Flyway's history table out of
   sync, and a subsequent `spring-boot:run` then fails on the duplicate column. I
   pivoted to `mvn flyway:migrate` against a freshly restored pre-V4 DB so Flyway
   applies the migration and records the correct checksum. End result is identical
   (schema and Flyway history both correct), but the path differs from the brief.

## Artifacts

- Migration file: `backend/ai-food-app/src/main/resources/db/migration/V4__add_token_and_role_audit.sql`
- DB backup (post-V4, clean): `/tmp/ai_food_backup_20260629.sql` (28KB)
- App startup verify log: `/tmp/aifood-app-verify.log`
- Commit: `35e358c`

## Self-review

- V4 SQL applied successfully: yes
- qa_record has 4 new columns (prompt_tokens, completion_tokens, total_tokens, model): yes
- sys_user has role column: yes
- admin_audit_log table created (11 columns, 3 indexes): yes
- smokeuser.role = 'ADMIN': yes
- ai-food-app can start and login returns 200: yes
