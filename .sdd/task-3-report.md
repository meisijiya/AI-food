# Task 3 Report: 迁移 entity 和 mapper 到 common

**Status:** ✅ DONE

**Branch:** `feature/admin-backend`
**Commit:** `c8e4aa1` — `refactor(common): move entity and mapper to ai-food-common`

## 摘要

将 `com.ai.food.model.*` (16 个 entity) 和 `com.ai.food.mapper.*` (16 个 mapper) 从
`backend/src/main/java/com/ai/food/{model,mapper}/` 整体迁移到
`backend/ai-food-common/src/main/java/com/ai/food/common/{model,mapper}/`,
并同步更新所有引用方的 `import` 语句（src/main + src/test 共 18 个文件）。

包名重命名规则:
- `com.ai.food.model` → `com.ai.food.common.model`
- `com.ai.food.mapper` → `com.ai.food.common.mapper`
- `com.ai.food.common.util.ApiResponse*` 文件 **未触碰**（已在正确包名）

## 执行步骤

1. **mkdir + mv**：创建 `ai-food-common/src/main/java/com/ai/food/common/{model,mapper}`，
   `mv` 16 个 entity + 16 个 mapper 文件。
2. **sed 替换包名**（从 `backend/` 目录执行，`find` 范围限定当前目录）：
   - `package com.ai.food.model;` → `package com.ai.food.common.model;`
   - `package com.ai.food.mapper;` → `package com.ai.food.common.mapper;`
   - 所有 `import com.ai.food.model.` → `import com.ai.food.common.model.`
   - 所有 `import com.ai.food.mapper.` → `import com.ai.food.common.mapper.`
3. **rm -rf** 删除 `src/main/java/com/ai/food/{model,mapper}` 旧目录。
4. **验证零残留**：`grep -rln "com\.ai\.food\.\(model\|mapper\)\." backend/ --include="*.java"
   | grep -v "common\.\(model\|mapper\)"` → **空输出**。
5. **编译 common 模块**：`JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -f
   backend/ai-food-common/pom.xml clean compile` → **BUILD SUCCESS**
   （34 source files compiled in 2.975 s）。
6. **git add 精确范围**：
   - `git add backend/ai-food-common/src/main/java/com/ai/food/common/{model,mapper}/`
   - `git add -u backend/src/`（src/main + src/test 修改和删除）
   - **不**提交 `backend/.settings/org.eclipse.jdt.core.prefs`（pre-existing modification，
     非本任务范围）。
7. **git commit**：`refactor(common): move entity and mapper to ai-food-common`

## 变更文件清单（共 50 个）

### 重命名（32 个，git 检测为 `R`）

**16 entities** → `backend/ai-food-common/src/main/java/com/ai/food/common/model/`
- BloomSyncLog, ChatConversation, ChatFile, ChatMessage, ChatPhoto,
  CollectedParam, ConversationSession, FeedComment, FeedPost, Photo,
  QaRecord, RecommendationResult, ShareRecord, SysUser, UserBloomFilter, UserFollow

**16 mappers** → `backend/ai-food-common/src/main/java/com/ai/food/common/mapper/`
- BloomSyncLogMapper, ChatConversationMapper, ChatFileMapper, ChatMessageMapper,
  ChatPhotoMapper, CollectedParamMapper, ConversationSessionMapper, FeedCommentMapper,
  FeedPostMapper, PhotoMapper, QaRecordMapper, RecommendationResultMapper,
  ShareRecordMapper, UserBloomFilterMapper, UserFollowMapper, UserMapper

> 注意：mapper 比 model 多一个 `UserMapper.java`（model 中 `SysUser` 对应此 mapper），
> model/mapper 各 16 个。

### 仅修改 import（18 个）

- **controller** (3): ChatController, ConversationController, UserController
- **service** (12): AuthService, BloomFilterServiceImpl, BloomPersistenceServiceImpl,
  ChatService, ConversationService, FeedService, FollowService, LikeStreamConsumer,
  ParamNormalizationService, NotificationService, RecordService, ShareService,
  FileUploadService, UserService
- **job** (1): CleanupSoftDeletedJob
- **websocket** (1): ChatWebSocketHandler
- **test** (4): BloomFilterServiceImplTest, ChatServiceTest, ConversationServiceTest,
  RecordServiceTest

## 验证

| 检查 | 结果 |
|---|---|
| `grep -rn "com\.ai\.food\.model\.\|com\.ai\.food\.mapper\." backend/ --include="*.java"` | ✅ 0 匹配 |
| `api-food-common/.../util/ApiResponse*.java` 包名 | ✅ 仍是 `com.ai.food.common.util`（未触碰） |
| `mvn -f backend/ai-food-common/pom.xml clean compile` | ✅ BUILD SUCCESS (34 files, 2.975 s) |
| 完整 backend/ 编译 | ⏭️ 跳过（ai-food-app 和 admin-server 模块尚未创建，预期失败） |
| 提交范围（不包含 target/、.bak） | ✅ `git status` 仅剩 pre-existing untracked |

## 已知遗留（不阻塞）

- `backend.bak.20260629/`：Task 1 之前的备份目录，未被本任务 commit 触碰（仍在 untracked），
  后续清理任务处理。
- `backend/.settings/org.eclipse.jdt.core.prefs`：pre-existing modification，非本任务范围，
  故未纳入本 commit（保持 commit 干净）。

## 注意事项

- sed 命令必须从 `backend/` 目录执行，`find .` 才不会触碰 worktree 根目录的
  `backend.bak.20260629/`（该目录含同名 model/mapper 副本）。
- 本任务**没有**运行完整 backend 编译（`mvn -f backend/pom.xml compile` 会因
  `ai-food-app` / `admin-server` 模块不存在而失败，这是预期行为，不影响本任务成功标准）。
