# AI-food 设计系统 · 使用指南

> **Date**: 2026-07-05
> **Status**: Active · v0.1
> **Audience**: Frontend dev 接手新页面 / 修旧页面
> **Visual ground truth**: http://localhost:12121/ · `docs/superpowers/specs/2026-07-04-aifood-design-system-screenshots/`

## 1. 一句话总结

**所有页面必须用设计 token 写样式。颜色 / 阴影 / 圆角 / 间距 全部走 CSS 变量，不写硬编码。**

硬编码 = 技术债。新增 / 改 view 时，第一件事是 grep `frontend/src/styles/_tokens.scss` 找到对应 token。

---

## 2. Token 系统

### 2.1 入口

**单一 source of truth**: `frontend/src/styles/_tokens.scss`

通过 `frontend/src/assets/styles/main.scss` 的 `@use "../../styles/tokens" as *;` 引入。所有 token 在 `:root` 定义，可在任何 `*.vue` 的 `<style scoped>` 中通过 `var(--xxx)` 引用。

### 2.2 类别速查

| 类别 | Token 前缀 | 示例 | 数量 |
|---|---|---|---|
| 主色 | `--color-primary*` | `--color-primary` (#4a8dd5) · `--color-primary-grad-top/bottom` | 6 |
| 暖色 | `--color-accent-warm*` | `--color-accent-warm` (#d97849, AA on white) | 3 |
| 冷色 | `--color-cyan*` | `--color-cyan` · `--color-cyan-deep` · `--color-cyan-bright` | 3 |
| 表面 | `--color-surface*` | `--color-surface` · `--color-surface-low` · `--color-surface-lowest` | 3 |
| 文字 | `--color-on-surface*` | `--color-on-surface` (12.5:1 AAA) · `-variant` (6.3:1 AA) · `-faint` (4.7:1 AA) | 3 |
| 反转 | `--color-inverse-surface` · `--color-on-primary` · `--color-nav-active` | `#0b0f10` · `#ffffff` · `#22d3ee` | 3 |
| 语义 | `--color-danger*` · `--color-success*` | `--color-danger` (4.8:1 AA) · `--color-success` (3.1:1 AA Large) | 4 |
| 圆角 | `--radius-*` | `--radius-md` (12px, 按钮/Cell) · `--radius-2xl` (32px, hero) | 6 |
| 间距 | `--space-*` | `--space-3` (12px) · `--space-6` (24px) | 10 |
| 阴影 | `--shadow-*` + `--shadow-flat-*` | `--shadow-glow` (蓝主题) · `--shadow-warm` (橙主题) · `--shadow-flat-md` (中性黑) | 10 |
| 玻璃 | `--glass-*` | `--glass-bg` 0.62 · `--glass-blur` 20px · `--glass-border` | 5 |
| 动效 | `--ease-*` · `--dur-*` | `--ease-out-soft` · `--dur-base` (280ms) | 7 |
| a11y | `--focus-ring*` · `--safe-area-*` · `--min-tap-target` | `--focus-ring` 3px blue · `--safe-area-top` | 7 |
| z-index | `--z-*` | `--z-nav` 100 · `--z-dialog` 200 · `--z-toast` 300 | 5 |
| 骨架 | `--color-skeleton` · `--color-skeleton-shimmer` | 灰色 + 浅灰高光 | 2 |

**总计**: 70+ tokens。覆盖率达设计系统所有色彩 / 阴影 / 间距 / 动效需求。

### 2.3 用法示例

```scss
.my-card {
  background: var(--color-surface-lowest);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
  box-shadow: var(--shadow-md);
  transition: all var(--dur-fast) var(--ease-out-soft);
}
.my-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg);
}
.my-card__title {
  font-family: var(--font-sans);
  font-weight: 700;
  font-size: 18px;
  color: var(--color-on-surface);
}
```

---

## 3. 字体层级 (Text Style)

| Class | 字号 / 行高 / 字重 | 用途 |
|---|---|---|
| `.text-hero-serif` | 40 / 1.05 / 600 italic · serif | 仅英文/数字 hero（如 "The quiet evening."） |
| `.text-h1` | 24 / 1.25 / 700 · sans | **中文 hero**（如 "今晚想吃什么？"）+ 页面顶部 H1 |
| `.text-h2` | 18 / 1.3 / 700 · sans | 区块标题 / 卡片标题 |
| `.text-body` | 14 / 1.45 / 400 · sans | 正文 |
| `.text-meta` | 12 / 1.4 / 500 · sans | 次级信息 / 时间戳 |
| `.text-caption` | 11 / 1.4 / 600 · sans uppercase | 标签 / 分类 / 状态 |

⚠️ **Newsreader italic 不含中文字形** — 中文 hero 一律 `.text-h1`，仅英文/纯数字用 `.text-hero-serif`。

---

## 4. 5 个自写组件

### 4.1 `<RecommendationCard>` — 玻璃主推荐卡

**用途**: Home 页主推荐 / Detail 页顶部强调

```vue
<RecommendationCard
  caption="AI 驱动"
  title="为你推荐恰到好处的美味"
  subtitle="通过智能对话..."
/>
```

Props:
- `caption?: string` — 上方小标签（暖色 uppercase）
- `title: string` — 主标题（蓝）
- `subtitle?: string` — 副文案（中性灰）

样式: `.glass-card-strong` 玻璃 + `--shadow-glow` + `--radius-xl` (24px)

### 4.2 `<MoodChip>` — 心情 pill

**用途**: Home / Profile 心情/偏好快捷入口

```vue
<MoodChip label="热汤面" :selected="mood === 'hot'" @toggle="setMood('hot')" />
<MoodChip label="暖心菜" />
<MoodChip label="夜宵" />
```

Props:
- `label: string` — 文案
- `selected?: boolean` — 选中态（暖色边框 + 暖色文字 + 暖色底）

Events:
- `(e: 'toggle'): void` — 点击触发

样式: 默认 `--color-surface-lowest` 白底 · selected 时 `rgba(232, 133, 90, 0.12)` 暖色底 + `--color-accent-warm` 边框

### 4.3 `<MessageBubble>` — 聊天气泡

**用途**: Chat / ChatRoom 消息列表

```vue
<MessageBubble role="user">我是用户消息</MessageBubble>
<MessageBubble role="ai">我是 AI 回复</MessageBubble>
<MessageBubble role="ai" modifier="retry">我是追问消息</MessageBubble>
<MessageBubble role="ai" modifier="interrupt">我是打断消息</MessageBubble>
```

Props:
- `role: "user" | "ai"` — 必填
- `modifier?: "retry" | "interrupt"` — 特殊样式

样式:
- `user`: 蓝底（`--color-primary`）+ 白字 + 右对齐
- `ai`: 暖色淡底（`#fef0e8 → #fff5ee`）+ 暖橙字 + 暖橙头像 + 左对齐
- `retry`: 虚线边框蓝
- `interrupt`: 红色边框

### 4.4 `<DSLoadingOrb>` — AI 思考指示

**用途**: Chat 思考中状态

```vue
<DSLoadingOrb />
```

Props: 无
样式: 暖色淡底 + 3 个暖橙点 (1.2s ease-in-out 无限循环，150ms stagger)

### 4.5 `<StatCard>` — 统计卡

**用途**: Profile 数据展示

```vue
<StatCard :number="42" label="对话" />
<StatCard :number="18" label="收藏" />
```

Props:
- `number: number | string` — 数字
- `label: string` — 标签

样式: 白底 95% + `--shadow-md` + Newsreader italic 数字 26px (仅数字 OK) + 顶部多留白 (`pt-4 pb-2 px-3`)

---

## 5. Utility Classes

| Class | 用途 |
|---|---|
| `.glass-card` | 玻璃卡片（20px blur + 62% white） |
| `.glass-card-strong` | 强玻璃（32px blur + 82% white） |
| `.shadow-glow` | 蓝主题阴影（var(--shadow-glow)） |
| `.shadow-warm` | 橙主题阴影（var(--shadow-warm)） |
| `.pill-nav` | 浮动 nav 同款（dark glass + radius-pill） |
| `.btn-primary-gradient` | 主按钮渐变（上 #5b9be0 → 下 #3a7bc0） |
| `.btn-breath` | 悬停呼吸（hover 触发 2s scale 1↔1.05） |
| `.bg-winter-sunrise` | 暖橙 + 冷蓝双 radial 装饰背景 |
| `.bg-cold-canvas` | 蓝紫 radial 冷淡背景 |
| `.text-truncate` / `.text-clamp-2/3` | 文本截断 |
| `.font-sans` / `.font-serif` | 字体独立 utility |
| `.no-scrollbar` | 隐藏滚动条 |

---

## 6. 设计规则（不可破坏）

### 6.1 暖色 ≤ 3 处 / 屏

单屏同时出现的暖色元素不超过 3 个。优先场景: hero CTA / 美食配图 / 状态徽章。

### 6.2 Newsreader italic 不套中文

中文 hero 一律 `.text-h1` (sans bold)。`.text-hero-serif` 仅用于英文 / 纯数字。

### 6.3 Editorial 克制

Hero 段位用满装饰（玻璃 / 大圆角 / Newsreader italic / 阴影）。密集场景保持紧凑（`--radius-md` 12px / 小间距）。

### 6.4 Vant 保留

Vant 4 通过 3 个 CSS 变量覆盖 (`--van-primary-color` / `--van-text-color` / `--van-background`)。其他 Vant 组件继续用，业务逻辑不动。

### 6.5 焦点环 + safe-area

所有可交互元素必须有 `--focus-ring` (3px 蓝色)。底部 padding 必须包含 `--safe-area-bottom` (iPhone home indicator)。

---

## 7. 暗色模式

```css
@media (prefers-color-scheme: dark) {
  /* 系统自动生效，无需手动切换 */
}
```

实施位置: `_tokens.scss` 的 `@media (prefers-color-scheme: dark)` 块。**自动响应系统设置**，不需要应用代码介入。

⚠️ **手动 toggle 未实施**: Phase 11+ 候选。当前仅系统级 dark mode。如果需要"用户手动切换 light/dark/system"，需要:
- Pinia store (`useThemeStore`) 持久化
- `<html>` 元素 data-theme 属性切换
- UI 控件（在 Profile / 设置页）

---

## 8. 可访问性 (WCAG AA)

| Token | 验证组合 | 状态 |
|---|---|---|
| `--color-on-surface` | on `--color-surface` | 12.5:1 ✅ AAA |
| `--color-on-surface-variant` | on `--color-surface` | 6.3:1 ✅ AA |
| `--color-on-surface-faint` | on `--color-surface` | 4.7:1 ✅ AA |
| `--color-danger` | on `--color-surface` | 4.8:1 ✅ AA |
| `--color-accent-warm` | on `--color-surface-lowest` (white) | 4.6:1 ✅ AA |
| `--color-primary` | on `--color-surface-lowest` | 3.5:1 ⚠️ AA Large (headlines/bold only) |
| `--color-success` | on `--color-surface` | 3.1:1 ⚠️ AA Large (badges/icons only) |
| `--color-cyan-deep` | on `--color-surface-lowest` | 2.4:1 ⚠️ Fail (decorative borders only) |

**规则**: 文字永远不要用 `--color-primary` / `--color-success` / `--color-cyan-deep` 当正文。只能做大字号 / bold / 图标 / border。

---

## 9. 其他 20 view 迁移指南

**当前状态**: 仅有 Home / Chat / Profile 3 个 view 完成深耕。其他 20 个 view (Feed / Records / Login / Match / Friends / Result / RecordDetail / FeedDetail / ChatRoom / ChatList / Notifications / Share / FollowList / ProfileEdit / UserSearch / Contacts / UploadPhoto / EmojiPicker / CachedImage / 等等) 用旧 Vant 默认样式 + 部分硬编码。

**迁移优先级** (按 spec §3 影响范围):
1. **高**: Feed / Records (流量大, 用户主路径) — Phase 12+ 候选
2. **中**: Login / Match / Result (用户关键操作) — Phase 13+ 候选
3. **低**: ChatRoom / ChatList / Notifications (子系统已用 DSMessageBubble) — 增量迁移

**迁移步骤** (per view):
1. Read view `*.vue` 文件
2. grep 硬编码 `#22d3ee` / `#22c55e` / `#ef4444` / `cubic-bezier` / `rgba(0,0,0)` 等 → 替换为 token
3. 替换 `padding: 16px` → `padding: var(--space-4)`
4. 替换 `border-radius: 8px` → `border-radius: var(--radius-sm)`
5. 替换 `box-shadow: 0 2px 8px rgba(...)` → `box-shadow: var(--shadow-md)`
6. 中文 italic 检查：font-family: serif italic + 中文 → 改 sans bold
7. 中文 hero 检查：`font-family: serif italic` 不允许中文
8. 暖色 ≤ 3 检查

**预计每 view**: 30-60 分钟。20 view × 0.5h = 10h。可分批（按优先级）。

---

## 10. 检查清单（新 view / 改 view）

- [ ] 颜色 / 间距 / 圆角 / 阴影 / 动效全部用 token，无硬编码 hex / px / rgba
- [ ] 中文 hero 用 `.text-h1`，不用 serif italic
- [ ] 暖色 ≤ 3 处 / 屏
- [ ] 焦点环 `--focus-ring` 已在 button / input 上
- [ ] safe-area 在底部
- [ ] min-tap-target ≥ 44pt
- [ ] 暗色模式下文字仍可读（在线切换浏览器 dark mode 看一遍）
- [ ] 按钮 hover 有 transition（180-280ms ease-out-soft）
- [ ] 主 CTA 用 `.btn-primary-gradient` + `.btn-breath`

---

## 11. 已知遗留（Phase 11+ 候选）

| 项 | 工作量 | 优先级 |
|---|---|---|
| 39 处残留 `rgba(0,0,0,X)` 阴影（非标准 spread/blur） | 1h | 中 |
| App.vue 浮动 nav rgba 玻璃效果深度 token 化 | 30min | 低 |
| `@media (hover: hover)` 限定 btn-breath 在 touch 设备 | 15min | 中 |
| frontend-UI/ 预览与 frontend/ tokens 同步 | 30min | 低 |
| 暗色手动 toggle (useThemeStore + UI) | 4-6h | 中 |
| 其他 20 view 迁移 | 10h+ | 中 |
| Feed 页深耕 (Vant CSS variable 扩展点) | 3-4h | 中 |

---

## 12. 相关文件

- 设计令牌源: `frontend/src/styles/_tokens.scss`
- 入口: `frontend/src/assets/styles/main.scss`
- 组件: `frontend/src/components/ui/{RecommendationCard,MoodChip,MessageBubble,DSLoadingOrb,StatCard}.vue`
- 三页: `frontend/src/views/{Home,Chat,Profile}.vue`
- 预览源: `frontend-UI/src/index.css` (sandbox 设计探索，不影响生产)
- 预览服务: http://localhost:12121/ (公网可达, 仅非敏感静态)
- Spec: `docs/superpowers/specs/2026-07-04-aifood-design-system-design.md`
- Plan: `docs/superpowers/plans/2026-07-04-aifood-design-system-rollout.md`
- 截图: `docs/superpowers/specs/2026-07-04-aifood-design-system-screenshots/`

---

## 13. 紧急修复 / 常用查询

### "我想加个按钮"
```vue
<button class="btn-primary-gradient btn-breath">操作</button>
```
搞定。

### "我想加张卡片"
```vue
<div class="glass-card" style="padding: var(--space-6); border-radius: var(--radius-xl);">
  内容
</div>
```

### "我想加个暖色 chip"
```vue
<MoodChip label="新建" :selected="true" @toggle="..." />
```

### "我的按钮 hover 没动效"
检查 `disabled` 属性。`.btn-breath` 用 `:not(:disabled)` 排除 disabled。

### "颜色调起来还是怪"
大概率是硬编码了 hex。grep `--color` 查 var() 使用，找 `.sass` 里的 hex 替换。

---

**版本**: v0.1 (2026-07-05) · 13 commits 实施后
**下次更新**: Phase 11+ 候选实施后 / 其他 view 迁移时