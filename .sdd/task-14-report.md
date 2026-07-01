# Task 14 — Dashboard Controller 实现报告

## 1. 需求概述

为管理后台 admin-server 实现 Dashboard 聚合接口:
- `GET /admin/api/dashboard/summary` — 汇总指标(用户/会话/Token/在线/健康度)
- `GET /admin/api/dashboard/trends?days=N` — 最近 N 天趋势,默认 7

继承 Task 13 引入的 `@RequireAdmin` 鉴权模式,使用现有 `UserMapper` + `LambdaQueryWrapper` 按 created_at 范围查 sys_user。会话/Token 指标 Phase 2 暂占位 0,Phase 3 接入 ChatConversationMapper / QaRecordMapper 后填充。

## 2. 实施内容

### 2.1 文件创建(4 个,共 205 行)

| 文件 | 行数 | 说明 |
|---|---|---|
| `dto/DashboardSummaryVO.java` | 30 | 8 字段聚合 VO |
| `dto/TrendVO.java` | 15 | (date, count) 数据点 |
| `service/DashboardService.java` | 111 | summary() + trends(int) |
| `controller/DashboardController.java` | 49 | 两个端点 + `@RequireAdmin` |

### 2.2 关键修正点

**Brief 笔误修正**:Task brief 中写的 `SysUserMapper` 不存在,实际 mapper 名为 `UserMapper`(见 `ai-food-common/.../mapper/UserMapper.java`)。已替换为正确导入。

**Ponytail 注释**:
- `KEYS` 适合低基数场景(全局并发 < 万级),Phase 3 改造为 SCAN cursor 迭代
- Phase 2 占位标记(0L)集中在 `conversationCount / todayConversations / tokenToday / tokenMonthTotal / conversationTrend`,Phase 3 替换为 mapper 查询

### 2.3 函数级注释

四个文件均含中文 Javadoc,涵盖:
- 类职责(谁调用、为什么存在)
- 方法语义、参数边界、返回值结构
- 占位字段的 Phase 2/Phase 3 演进路径

## 3. 验证结果

### 3.1 编译

```bash
mvn -f admin-server/pom.xml clean compile -q
# exit 0,无 error
```

### 3.2 Smoke Test

启动 admin-server (port 8081, profile=prod),登录 `smoke@aifood.local`,调用两个端点:

| 端点 | 结果 |
|---|---|
| `/admin/api/dashboard/summary` | ✅ userCount=1, onlineCount=1, systemHealth={jvm/db/redis:UP} |
| `/admin/api/dashboard/trends?days=7` | ✅ userTrend[7] + conversationTrend[7],userTrend 在 2026-06-29 显示 count=1(smokeuser 创建日) |

预期值与 brief 一致:`userCount=1` ✓,`trends` 7 条 ✓。

### 3.3 提交

```
a53d9f2 feat(admin): add Dashboard summary and trends
4 files changed, 205 insertions(+)
```

## 4. 风险与遗留

| 项 | 说明 | 何时处理 |
|---|---|---|
| `KEYS token:*` 阻塞 | 现有数据量极小,不会触发 | 在线 token > 1 万时改 SCAN |
| 会话/Token 占位 0 | Phase 2 验收范围外 | Task 15+ 接入 ChatConversationMapper / QaRecordMapper |
| `systemHealth` 硬编码 UP | 没有真实 ping 检查 | 加 actuator health indicator 或定时心跳 |
| todayNew=0 | smokeuser 创建于昨天,符合预期 | N/A |