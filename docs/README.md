# AI-Food 文档目录

> 入口:给"新接触项目的人/agent"看的总览。**5 分钟看完,知道东西在哪**。
> 最后更新: 2026-07-02(按 deepwork 模式维护)

---

## 快速导航(按用途)

| 我想找… | 路径 |
|---|---|
| 5 个核心流程图(架构 / 登录 / AI 对话 / Feed / WebSocket) | `docs/architecture/01-system-overview.md` |
| 整体技术方案(栈选型 / 模块设计 / 数据模型) | `docs/architecture/02-tech-design.md` |
| **最新 session 交接**(本 session P1 收尾 + 已知问题) | `docs/handoff/2026-07-02-p1-finish-handoff.md` |
| 之前 session 交接(test=prod 部署) | `docs/handoff/2026-06-29-deploy-handoff.md` |
| 部署实战(4GB sandbox + 1.7GB cloud 链路) | `docs/learnings/2026-06-29-test-prod-deploy.md` |
| admin v1 部署报告(URL / 凭证 / 验证步骤) | `docs/admin-deployment-2026-07-01.md` |
| admin 后台使用 | `docs/admin-usage.md` |
| Admin 模块设计 / 实施计划 | `docs/superpowers/specs/2026-06-29-admin-backend-design.md` + `docs/superpowers/plans/2026-06-29-admin-backend-implementation.md` |
| 软删除清理一致性(原 spec) | `docs/superpowers/specs/2026-03-28-soft-delete-cleanup-consistency-design.md` |
| MyBatis-Plus 迁移 spec/plan | `docs/superpowers/specs/2026-06-29-migrate-jpa-to-mybatis-plus-design.md` + `docs/superpowers/plans/2026-06-29-migrate-jpa-to-mybatis-plus.md` |
| OpenCode 三档自动化 / git-worktree 沙盒开发 | `docs/OpenCode-并行开发三档自动化.md` / `docs/git-worktree沙盒开发指南.md` |
| 同步 / 合并 vs 同步分支 | `docs/同步分支概念详解.md` / `docs/合并vs同步-本质区别.md` |

## 目录结构

```
docs/
├── README.md (本文件)              ← 入口索引
├── architecture/                     ← 架构与技术方案
│   ├── 01-system-overview.md        ← 5 个 Mermaid 流程图
│   └── 02-tech-design.md            ← 整体技术方案(从 doc/ 迁入)
├── handoff/                          ← 跨 session 交接
│   ├── 2026-06-29-deploy-handoff.md
│   └── 2026-07-02-p1-finish-handoff.md
├── superpowers/                      ← subagent-driven 模式开发计划
│   ├── specs/                        ← 设计 spec
│   ├── plans/                        ← 实施 plan
│   └── reports/                      ← 验证 report
├── learnings/                        ← 实战经验总结
│   └── 2026-06-29-test-prod-deploy.md
├── admin-deployment-2026-07-01.md   ← admin v1 部署报告
└── admin-usage.md                    ← admin 后台使用

doc/                                ← 历史文档(部分内容已过期,慎用)
├── bug排除过程.md(已过期 — 涉及 42.193.183.187 不可达 IP)
├── 实现想法.md(历史想法 — 部分已实现,需对照代码确认)
├── 待实现.md(历史待办 — 部分已实现)
└── (技术方案.md 已迁至 docs/architecture/02-tech-design.md)
```

## 项目根 .md

| 文件 | 用途 |
|---|---|
| `README.md` | 项目门面(作者 / 技术栈 / 部署) |
| `AGENTS.md` | 项目级 AI 行为规范 |
| `DOCKER.md` | Docker 部署说明 |
| `SWAGGER.md` | Swagger / Knife4j 接口文档 |
| `DEMO_VIDEO_SCRIPT.md` | 演示视频脚本 |
| `项目经历与面试话术.md` | 个人简历素材 |

## `logs/`

每次 session 自动生成 `logs/<时间戳>-<主题>.md`,记录"该 session 做了什么"。**不进 git**(`*.log` + `logs/` 已在 `.gitignore`)。

## `.sdd/`

subagent-driven-development 模式的 task 报告(20+ 文件),已 commit 进 git。**历史记录,不再维护**。

## 已知问题(本 session 发现,详见 handoff §3)

- 🔴 cloud nginx 没配 `/api/*` 转发 → `119.29.52.111/api/guest/stats` 502
- 🟡 `FeedHotRankService:117` `feed:hot:details` 无 TTL
- 🟡 WebSocket 5s 心跳超时未实现
- 🟢 pre-commit `end-of-file-fixer` hook bug(被多次 `--no-verify` 绕过)
