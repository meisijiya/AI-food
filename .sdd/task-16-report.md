# Task 16: 实现 Token 用量统计 — 实施报告

**Date:** 2026-06-30
**Branch:** `feature/admin-backend`
**Commit:** `bdd8bc1`

## 用户需求

admin-server 增加 Token 用量统计端点,从 `qa_record` 表聚合
prompt/completion/total token 用量,支持按 day / model / session 三种维度分组,
按 totalTokens 降序返回。

## 实施总结

按 brief 创建 2 个文件,沿用 DashboardService 风格(差异:QueryWrapper 而非
LambdaQueryWrapper,因为 select 子句需要聚合函数别名)。

### 新增文件 (2)

| 文件 | 行数 | 作用 |
|---|---|---|
| `service/TokenUsageService.java` | 88 | `stats(groupBy, startDate, endDate)` 用 QueryWrapper.select(...) 选 5 列(SUM×3 + COUNT + 分组键),按 groupBy 切 3 种 group 列,按 totalTokens 降序,filter isNotNull(total_tokens) + 日期范围 |
| `controller/TokenUsageController.java` | 50 | GET `/admin/api/token-usage/stats?groupBy=&startDate=&endDate=`,类级 `@RequireAdmin`,沿用 ApiResponse 包装 |

### 端到端验证结果

```
✅ mvn -f admin-server/pom.xml clean compile -q          exit 0
✅ spring-boot:run                                       Started in 4.362s (端口 8081)
✅ POST /admin/api/auth/login (smoke@aifood.local)      → 200, token 156 chars
✅ GET  /admin/api/token-usage/stats?groupBy=day         → 200, data: []  (空库)
✅ GET  /admin/api/token-usage/stats?groupBy=model       → 200, data: []
✅ GET  /admin/api/token-usage/stats?groupBy=user        → 200, data: []
✅ GET  .../stats?groupBy=day&startDate=2026-01-01&endDate=2026-12-31 → 200, data: []
✅ 无 token → 401 拦截
✅ INSERT 3 条种子 (qwen-turbo×2 + gpt-4o-mini×1,跨 2 session)
✅ GET  ?groupBy=day   → [{key:"2026-06-30", count:3, promptTokens:420, completionTokens:190, totalTokens:610}]
✅ GET  ?groupBy=model → 2 条,qwen-turbo 排前 (totalTokens:330 vs 280) — 降序生效
✅ GET  ?groupBy=user  → 2 条,按 session_id,totalTokens 降序 (330, 280)
✅ DELETE 种子 (qa_record WHERE session_id LIKE 'seed-test-session-%')
✅ 清理无残留,spring-boot 进程已 kill -9
```

## 关键决策

### 1. **`user` 维度实际是 `session_id`** (与 brief 偏差)

**问题**:Brief Step 1 用 `user_id` 作为分组键,但 `QaRecord` 实体没有
`user_id` 列,只有 `session_id`(见 `QaRecord.java` 字段列表)。

**决策**:按 brief 的 "When You're in Over Your Head" 指引,改用 `session_id`
作为分组键,在注释里说明这是 ponytail 简化方案 + TODO 提示后续如需
"按用户视角"统计,需 JOIN `conversation_session.user_id`。

**影响**:brief 假设是 "用户维度",实际是 "会话维度"。前端展示时需要自己
JOIN 用户名,或者后续改造 service 把 session_id → user_id 翻译一遍。

### 2. **驼峰别名必须加 backtick 包 key**

`QueryWrapper.select("DATE(created_at) as \`key\`")` — `key` 是 MySQL
保留字附近(实际是 user/system variable prefix),保险起见反引号包裹,避免
某些 SQL mode 下报错。`promptTokens` 等普通别名不需要引号。

### 3. **编译错误 1:包名拼写**

第一次写 controller 错把 `com.aifood` 写成 `com.ai.food`(漏了
`com.ai.food` 是 common 模块包名,`com.aifood` 是 admin 模块包名)。
`mvn clean compile -q` 立刻报错,编辑修正后通过。

### 4. **`rtk jq` 工具截断输出导致首轮 smoke test 全 401**

**现象**:`rtk jq -r .data.token` 输出被截到 120 字符,token 签名被
截成 4 字符 + `...` 尾巴,服务端 JWT 校验失败报"Token 已过期"。

**解决**:换成 `/usr/bin/jq -j`(系统原生 jq),输出完整 156 字符,smoke
test 全绿。这是个工具坑,与本次代码无关,但值得记录:**admin-server smoke
test 务必用 `/usr/bin/jq`,不要用 `rtk jq`**。

## Concerns

1. **聚合大表性能未评估**:`qa_record` 当前数据量小,但 query 走全表扫描 +
   GROUP BY,记录数到百万级会变慢。可加索引 `(created_at, total_tokens)`
   或拆分成每日预聚合表。当前规模(估计 < 10k 行)够用。

2. **`groupBy=user` 含义与前端预期不符**(最严重偏差):API 返回的 `key` 是
   `session_id`(UUID/雪花ID),前端若直接展示给管理员看不懂。需要在 controller
   层再做一次 session → user 的翻译,或前端自己 join。本任务按 brief
   指引保留 session_id 粒度,留 TODO 给后续。

3. **日期范围仅支持 ISO 格式**:`yyyy-MM-dd`,不含时间。如要支持 `yyyy-MM-dd HH:mm:ss`
   或相对日期(`last_7_days`),需扩展解析器。当前按 brief 简化为 ISO_DATE。

4. **`orderByDesc("totalTokens")` 引用别名**:MyBatis-Plus 把这个当作字段
   名拼到 ORDER BY,但因为 SELECT 里也用了 `as totalTokens`,生成 SQL 是
   `ORDER BY totalTokens` —— 数据库接受别名排序。已验证聚合测试里 day
   模式输出顺序正确(虽然只有 1 条看不出降序效果)。

5. **JwtAuthFilter / JwtService 同款依赖注入方式**:`TokenUsageService`
   用 `@RequiredArgsConstructor` 注入 `QaRecordMapper`,与
   `DashboardService` 注入 `UserMapper` 风格一致,无新增配置。

## 验证

- 编译:`mvn -f admin-server/pom.xml clean compile -q` → exit 0
- 启动:`spring-boot:run` → 4.362s 启动 (prod profile, 端口 8081)
- 3 种 groupBy × 日期过滤 × 401 拦截:全部预期返回
- 聚合功能:插入 3 条种子 → day/model/user 三种模式均返回正确 SUM 与 COUNT,
  按 totalTokens 降序
- 种子已清理,spring-boot 进程已 kill,无副作用