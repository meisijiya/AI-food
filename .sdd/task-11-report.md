# Task 11 Report: 实现 AuditAspect 操作审计

## Status: DONE

## Summary
Implemented an AOP-based operation audit layer for the admin-server module. Methods
annotated with `@AuditLog` are intercepted by `AuditAspect` (@Around), which records
actor / action / target into the `admin_audit_log` table via MyBatis-Plus.

## Files Created
1. `common/annotation/AuditLog.java` — method-level annotation (`value`, `action`)
2. `common/audit/AuditLogEntity.java` — MyBatis-Plus entity, `@TableName("admin_audit_log")`
3. `common/audit/AuditLogMapper.java` — `BaseMapper<AuditLogEntity>`
4. `common/audit/AuditAspect.java` — `@Around("@annotation(auditLog)")` aspect

## Files Modified
- `AdminApplication.java` — added `com.aifood.admin.common.audit` to `@MapperScan`

## Design Notes
- Actor identity (`adminId` / `adminUsername`) is read from request attributes set by
  `AdminInterceptor` (Task 10); falls back to `0L` / "anonymous" when absent.
- `@EnableAspectJAutoProxy` already present on `AdminApplication` (Task 7) — aspect fires.
- Audit save errors are swallowed (logged, never re-thrown) so auditing cannot block
  business logic. The wrapped method's own exception is still re-thrown after a FAIL log.
- Client IP resolved via X-Forwarded-For → X-Real-IP → remoteAddr.
- Convention: 2nd method arg treated as target id (e.g. `@PathVariable Long id`).

## Verification
`mvn -f admin-server/pom.xml clean compile` → BUILD SUCCESS (Java 21).

## Commits
- `63d9fd0` feat(admin): add @AuditLog + AuditAspect for operation audit

## Concerns
- No controller currently uses `@AuditLog` yet; aspect is dormant until annotated.
- Assumes `admin_audit_log` table exists (schema/migration owned by another task).
