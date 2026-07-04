# AI 美食推荐 · 设计系统与三页深耕 · Design Spec

> **Date**: 2026-07-04
> **Status**: Draft · 待用户审阅
> **Author**: Orchestrator + @oracle + @designer reviews
> **Visual preview**: http://localhost:12121/ (frontend-UI/dist/)
> **Token source**: `frontend-UI/src/index.css` (single source of truth)
> **Brainstorming sessions**:
> - `ses_0d3cb1fc5ffe4kWE37C48UOSpr` (@oracle · 技术复审)
> - `ses_0d3cb1ecaffeKZoYTRjwhp4uC9` (@designer · 美学复审)
> - `ses_0d39735e6ffeQMhiROcxj8tA1j` (@observer · 图位定位)

---

## 0. 决策摘要 (Decision Log)

通过 brainstorming 锁定的 4 个核心决策：

| 决策 | 选择 | 理由 |
|---|---|---|
| **范围** | 地基（tokens + 11 个组件）+ 3 个核心页（Home / Chat / Profile） | 用户明确"慢慢对齐", 23 view 不重写 |
| **调性** | 冷蓝主 + 暖色点缀 · "冬日晴空" | 用户原话: "冬天的旭日, 但不过度" |
| **浓度** | Editorial 克制 · hero 用满装饰 / 密集场景紧凑 | 移动端不是桌面 editorial, 需要密度适配 |
| **组件策略** | Vant 保留 + **CSS 变量覆盖**（非 SCSS 穿透） | Vant 4 已转 CSS 变量; 当前代码 Vant 实际用量 0; 改 SCSS 是对空气开炮 |

---

## 1. 设计令牌 (Design Tokens)

完整 source: `frontend-UI/src/index.css` (Tailwind v4 `@theme` + `:root`)
文档镜像: `frontend-UI/DESIGN_TOKENS.md` (含 Vant 映射表)

### 1.1 色板（冬日晴空 · 带白）

```css
--color-primary:           #4a8dd5   /* 主色 · 雪天+蓝天(带云) */
--color-primary-hover:     #2c5fa0   /* 主色 hover · 仍有读感 */
--color-primary-soft:      #c5dcf0   /* 主色淡 · 带白提示 */
--color-primary-grad-top:  #5b9be0   /* 主按钮渐变顶端 · 晴空浅 */
--color-primary-grad-bottom: #3a7bc0 /* 主按钮渐变底端 · 晴空深 */
--color-cyan:              #8ce1f3   /* 次级冷色 · 进度 / 数据条 */
--color-accent-warm:       #e8855a   /* 旭日橙 · 仅 hero CTA */
--color-accent-warm-2:     #f5b78a   /* 暖橙淡 · hover / 装饰渐变 */
--color-surface:           #f4f7f9   /* 全局底色 */
--color-surface-low:       #edf1f3   /* 卡片底 */
--color-surface-lowest:    #ffffff   /* 浮起底 */
--color-on-surface:        #2b2f31   /* 正文 */
--color-on-surface-variant:#585c5e   /* 次级正文 */
--color-on-surface-faint:  #8b9094   /* 提示 */
--color-inverse-surface:   #0b0f10   /* App.vue 浮动 nav 底 */
--color-nav-active:        #22d3ee   /* nav active 文字 / dot */
--color-danger:            #c8344a   /* 警告 / 错误 */
--color-success:           #2f9e6e   /* 成功 */
```

**暖色使用规则**（防视觉污染）：
- 单屏 ≤ **3 处**暖色
- 尺寸 ≤ **80×80px** 或字号 ≤ **32px**
- 仅 hero / CTA / 美食配图 / 状态徽章; 其余保持冷调

**主按钮 utility**:
```css
.btn-primary-gradient {
  background: linear-gradient(180deg, var(--color-primary-grad-top) 0%, var(--color-primary-grad-bottom) 100%);
}
```

### 1.2 字体（Plus Jakarta Sans + Newsreader）

```css
--font-sans:  "Plus Jakarta Sans", "PingFang SC", "Microsoft YaHei", "Noto Sans SC", ui-sans-serif, system-ui, sans-serif;
--font-serif: "Newsreader", "Songti SC", "STSong", Georgia, serif;
```

**⚠️ Newsreader italic 使用规则**（复审发现的关键约束）：
- Newsreader **不含中文字形** → 浏览器套到中文会得到假斜体（机械倾斜 12°）
- ✅ 用 `text-hero-serif`：纯英文 hero / 装饰引文 / 纯数字 hero（Profile 42/18/7）
- ❌ **中文 hero 一律用 `text-h1`**（Plus Jakarta sans bold 700）

### 1.3 字号层级（Editorial 克制）

| Class | 字号 / 行高 / 字重 | 用途 |
|---|---|---|
| `.text-hero-serif` | 40 / 1.05 / 600 italic · serif | 仅 hero 英文 / 数字 |
| `.text-h1` | 24 / 1.25 / 700 · sans | 页面顶部 H1 · **中文 hero** |
| `.text-h2` | 18 / 1.3 / 700 · sans | 区块标题 |
| `.text-body` | 14 / 1.45 / 400 · sans | 正文 |
| `.text-meta` | 12 / 1.4 / 500 · sans | 次级信息 |
| `.text-caption` | 11 / 1.4 / 600 · sans uppercase · tracking .04em | 标签 / 分类 |

### 1.4 圆角 / 间距 / 阴影 / 玻璃 / 动效

| 类别 | Token 列表 |
|---|---|
| **圆角** | `--radius-sm` 8 / `--radius-md` 12 / `--radius-lg` 16 / `--radius-xl` 24 / `--radius-2xl` 32 / `--radius-pill` 999 |
| **间距** | `--space-1..16`: 4 / 8 / 12 / 16 / 20 / 24 / 32 / 40 / 48 / 64 |
| **阴影** | `--shadow-xs/sm/md/lg/xl` + `--shadow-glow` (主题蓝 0.22) + `--shadow-warm` (暖橙 0.25) |
| **玻璃** | `--glass-bg` 0.62 / `--glass-bg-strong` 0.82 / `--glass-blur` 20px / `--glass-blur-strong` 32px |
| **动效** | `--ease-out-soft` cubic-bezier(0.22, 1, 0.36, 1) · `--ease-spring` cubic-bezier(0.34, 1.56, 0.64, 1) |
| **时长** | `--dur-fast` 180ms / `--dur-base` 280ms / `--dur-slow` 480ms / `--dur-spring` 680ms |

### 1.5 装饰背景

| Class | 描述 |
|---|---|
| `.bg-winter-sunrise` | 暖橙 + 浅青双 radial + 冷蓝渐变（冬日旭日） |
| `.bg-cold-canvas` | 蓝紫 radial + 冷底渐变（冷淡 · Profile 用） |

### 1.6 a11y / 移动端 / 工程化令牌

```css
--focus-ring-width: 3px;
--focus-ring-color: rgba(0, 89, 182, 0.25);
--focus-ring: 0 0 0 var(--focus-ring-width) var(--focus-ring-color);

--safe-area-top: env(safe-area-inset-top, 0px);
--safe-area-bottom: env(safe-area-inset-bottom, 0px);

--z-base: 1; --z-sticky: 50; --z-nav: 100; --z-dialog: 200; --z-toast: 300;

--min-tap-target: 44px;

--color-skeleton: #e8ecf0;
--color-skeleton-shimmer: #f4f6f8;
```

**Motion Reduce 媒体查询**（自动生效）:
```css
@media (prefers-reduced-motion: reduce) {
  :root {
    --dur-fast: 0ms; --dur-base: 0ms; --dur-slow: 0ms; --dur-spring: 0ms;
  }
}
```

### 1.7 复用 utility classes

- `.glass-card` / `.glass-card-strong` / `.shadow-glow` / `.shadow-warm`
- `.pill-nav` / `.no-scrollbar` / `.btn-primary-gradient`
- `.text-truncate` / `.text-clamp-2` / `.text-clamp-3`
- `.bg-winter-sunrise` / `.bg-cold-canvas`
- `.font-serif` / `.font-sans`

---

## 2. 基础组件（11 个 · 砍 6 个过度工程）

> 原计划 17 个, 经 @oracle 复审砍到 11 个. 砍掉的 6 个用 CSS utility class 替代:
> ❌ DSButton / DSIcon / DSAvatar / DSBadge / DSField / DSTextArea / DSStack

### 2.1 自写组件清单

| Tier | 组件 | 用途 | 关键 token |
|---|---|---|---|
| Layout | `DSAppShell` | safe-area + 页面根 | `--safe-area-*`, `--z-nav` |
| Layout | `DSTopBar` | 顶部标题 + 左/右 slot | `--color-on-surface`, `--space-4` |
| Layout | `DSGlassCard` | 玻璃卡片 | `--glass-bg`, `--glass-blur`, `--radius-xl` |
| Layout | `DSSurfaceCard` | 白底浅阴影 | `--color-surface-lowest`, `--shadow-md` |
| Layout | `DSSegmented` | 选项分段（Feed 三 Tab / 偏好切换） | `--color-primary`, `--radius-md` |
| Feedback | `DSLoadingOrb` | 三点跳动（仅 Chat 流式响应时） | `--color-on-surface-faint` |
| Feedback | `DSEmptyState` | 图标 + 主/副文案 + 可选 CTA | `--color-on-surface-variant` |
| Chat | `MessageBubble` | 用户右气泡 / AI 左气泡（暖色淡渐变） | `--color-primary`, `--color-accent-warm-2` |
| Home | `MoodChip` | 心情/偏好 pill | `--color-accent-warm`, `--radius-pill` |
| Home | `RecommendationCard` | 玻璃主推荐卡 | `--glass-bg-strong`, `--shadow-glow` |
| Profile | `StatCard` | 玻璃统计卡 | `--shadow-md`, `pt-4 pb-2 px-3` |

### 2.2 Vant 保留清单（仅 CSS 变量覆盖，不重写）

`Cell / List / PullRefresh / SwipeCell / Tab / Tabbar / Picker / DatetimePicker / Switch / Slider / Stepper / Dialog / Toast / Notify / ImagePreview / ActionSheet`

---

## 3. 三页方向（Per-page direction）

### 3.1 Home · Editorial 满

**布局**（从上到下）：
1. **问候区**（`pt-9` 安全区 + 顶部 8px gap）："晚上好 / 小柚" + 右上 Bell 图标按钮
2. **主推荐玻璃卡** `RecommendationCard`：英文 hero serif "The quiet evening." 或 `text-h1`"今晚想吃什么？"（中文），副文案 + 暖橙 `MoodChip × 3`（hot/random/distance）
3. **主 CTA** `btn-primary-gradient` 全宽 "开始 30 秒对话"
4. **底部浮动 nav** `pill-nav` 4 项（首页/大厅/记录/我的），active 用 `--color-nav-active`

**关键 Token**：
- `bg-winter-sunrise` 装饰背景
- `--glass-bg-strong` + `--shadow-glow` 主推荐卡
- `--radius-2xl` (32px) hero 圆角
- `--color-primary-grad-top/bottom` 主按钮渐变
- `--color-nav-active` nav 激活态

**Motion**：
- 整页 fade-up 入场（`--dur-slow` 480ms）
- CTA hover breathing（`whileHover scale: [1, 1.05, 1]` + 2s 循环 + easeInOut + transformOrigin center）
- 仅 hover 时触发，移开即停

### 3.2 Chat · Editorial 紧凑

**布局**：
1. **TopBar**（fixed top，`pt-9` + safe-area）：AI 头像渐变 + "AI 小厨" + "正在思考…" 状态文字
2. **消息流**（scroll-container）：AI 消息用 **暖色淡气泡**（`bg-gradient-to-br from-[#fef0e8] to-[#fff5ee]` + 暖橙头像），用户消息用 `bg-primary`（新色 #4a8dd5）实色
3. **输入栏**（bottom safe-area）：圆角输入框 + `btn-primary-gradient` 发送按钮

**关键 Token**：
- `--color-surface-lowest` bg
- `--color-primary`（新色 #4a8dd5）用户气泡
- `--color-accent-warm` 系列 AI 气泡
- `--radius-md` (12px) 气泡圆角
- `--space-3/4`（12/16px）紧凑间距

**Motion**：
- 消息 fade-up 入场（stagger 80ms）
- AI 思考时 `DSLoadingOrb` 三点跳动（150ms 节奏 · 无限循环 · **仅在流式响应时激活**）

### 3.3 Profile · Editorial 满

**布局**：
1. **渐变 hero**（`pt-6 pb-6` 紧凑）：`bg-gradient-to-br from-primary to-[#2c5fa0]`、暖橙渐变 avatar（64×64 带白色描边 + `--shadow-warm`）、@xiaoyu / 小柚 / "偏爱：暖食/辣/不挑"
2. **Stat 卡片行**（`pt-4` 段间距）：3 个白底卡片（`bg-white/95` + `--shadow-md`），数字 Newsreader italic 26px、label `text-caption`，**`pt-4 pb-2 px-3` 顶部多留白**
3. **列表项**：`bg-surface-lowest` + border + ChevronRight，icon 暖橙 / 文字 primary
4. **底部 nav**：复用 pill-nav

**关键 Token**：
- primary 渐变 hero bg
- `--color-accent-warm` avatar 渐变
- `--shadow-warm` avatar 阴影
- `--shadow-md` stat 卡阴影
- `--color-on-surface` 列表文字
- `--radius-lg` (16px) 列表圆角

**Motion**：
- stat 卡片 staggered fade-up（120ms 间隔）
- hero 入场 fade

---

## 4. 分阶段实施计划（6 phases · ~12-16h）

### Phase 1 · Foundation（tokens 落 `frontend/`） · 2-3h

- 读取现有 `frontend/src/assets/styles/main.scss` 删除旧 `:root` 块（Material 3 命名：`--color-primary-container` 等）
- 创建 `frontend/src/styles/_tokens.scss`（mirror `frontend-UI/src/index.css`）
- `main.scss` import `_tokens.scss` 作为唯一定义源
- 全局 grep `frontend/src/**/*.vue` 替换 `--color-primary-container` → `--color-primary-soft`、`--color-secondary-fixed` → `--color-cyan`、`--color-surface-container-low` → `--color-surface-low`
- 字体 fallback 链：Plus Jakarta Sans + PingFang SC + 微软雅黑 + Noto Sans SC
- Newsreader italic 加 `font-feature-settings: "ss01"` 备用样式（如可用）

**验证**: `npm run build` 成功 + grep 无旧变量残留

### Phase 2 · Vant Override（CSS 变量覆盖） · 30min

在 `main.scss` 顶部覆盖 3 个 CSS 变量：

```css
:root {
  --van-primary-color: var(--color-primary);
  --van-text-color: var(--color-on-surface);
  --van-background: var(--color-surface);
}
```

按需增量添加更多 `--van-*` 变量（参考 https://vant-ui.github.io/vant/v4/#/en-US/theme）。

**验证**: Home / Feed / Records 跑一遍，看 Vant 组件是否换色

### Phase 3 · Home 深耕 · 3-4h

- `Home.vue` 重构：装饰背景 `bg-winter-sunrise` + `RecommendationCard` + `MoodChip × 3` + `btn-primary-gradient` 主 CTA
- 引入 `vue-router` 路由 query 参数（保留现状）
- 顶部 status bar / 安全区适配

**验证**: 截图对比 + Lighthouse perf ≥ 90 + 320/375/414px 三档

### Phase 4 · Chat 深耕 · 3-4h

- `Chat.vue` 重构：暖气泡 + 紧凑输入栏 + `DSLoadingOrb` 集成（仅流式响应时激活）
- 消息 stagger fade-up 80ms
- WebSocket 断线状态展示（`DSEmptyState`）

**验证**: 端到端：对话 → 思考 → 回复 → 流式；`mvn test` 全绿

### Phase 5 · Profile 深耕 · 2-3h

- `Profile.vue` 重构：渐变 hero + `StatCard × 3` + 列表项 + ChevronRight
- 顶部 hero `pt-6 pb-6` 紧凑
- stat 卡 `pt-4 pb-2 px-3` 顶重底轻
- avatar 暖橙渐变 + 白边 + `--shadow-warm`

**验证**: 320/375/414px 三档截图 + 字段缺失时 `DSEmptyState` 兜底

### Phase 6 · Verify & Archive · 1-2h

- `mvn -pl ai-food-app test` 全绿（168 用例基线）
- 前端 `npx vue-tsc` 通过
- 设计令牌 grep 校验（无旧变量残留）
- a11y 简易检查（focus-ring / alt text / contrast）
- `/comet-archive` 收尾

---

## 5. Vant 主题变量映射

> ⚠️ **修正**: 原计划用 SCSS 变量穿透, 经 @oracle 复审发现 Vant 4 已转 CSS 变量 + Theme Object, SCSS 覆盖已弃用. 改为最小 CSS 变量覆盖（仅 3 个变量, 见 Phase 2）。

后续按需增量添加 `https://vant-ui.github.io/vant/v4/#/en-US/theme` 文档中的 CSS 变量。

---

## 6. 验收标准 (Acceptance Criteria)

### 6.1 视觉

| 标准 | 验证方式 |
|---|---|
| 暖色 ≤ 3 处/屏 | 浏览器 devtools 测色 + 截图比对 |
| 中文 hero 不含假斜体 | 浏览器渲染 `text-h1` 而非 `text-hero-serif` |
| 主按钮 = `btn-primary-gradient` | `grep` `Home.vue` `Chat.vue` 等 |
| stat 卡内部 `pt-4 pb-2 px-3` | 截图 |
| §8 按钮区块 padding = `p-5` space-y-4 | grep + 视觉 |

### 6.2 性能

| 标准 | 验证 |
|---|---|
| Lighthouse perf ≥ 90 | 浏览器 devtools |
| 首屏 LCP < 2.5s | Lighthouse |
| CLS < 0.1 | Lighthouse |
| bundle size 无显著增加（CSS +5KB / JS +0KB 容忍） | `npm run build` 输出对比 |

### 6.3 工程

| 标准 | 验证 |
|---|---|
| 168 后端测试用例全绿 | `mvn -pl ai-food-app test` |
| 前端 `vue-tsc` 通过 | `npx vue-tsc --noEmit` |
| 无旧变量残留 | `grep -r "color-primary-container\|color-secondary-fixed" frontend/src/` 应为空 |
| Vant 覆盖生效 | Home / Feed / Records 截图比对 |

### 6.4 移动端

| 标准 | 验证 |
|---|---|
| 320 / 375 / 414 / 768px 四档无横向滚动 | 浏览器 devtools responsive 模式 |
| safe-area 适配 | iPhone 14 / 15 Pro Max 模拟 |
| 最小点击区 ≥ 44pt | Lighthouse a11y audit |

---

## 7. 实施期风险登记（来自多视角复审）

| 风险 | 缓解 |
|---|---|
| `motion` (framer-motion) 是 React 绑定 | 实施阶段全走 CSS transition/animation, 不引 `@vueuse/motion` |
| 暗色 nav 浮动胶囊激活色硬编码 `#22d3ee` | 已入令牌 `--color-nav-active`, Phase 3 引用替换 |
| Vue scoped style 引用令牌不规范 | Phase 1 制定规范 (CSS class 用 `var(--xxx)`) |
| Google Fonts FOUT 风险（首屏中文回退闪烁） | Phase 1 加 `<link rel="preload" as="font" crossorigin>` |
| `accent-warm` 配白文字对比度 ~3.0:1（仅大字号 OK） | 仅 hero CTA / 状态徽章使用, 配 `font-semibold` 加粗 |

---

## 8. 实施期延后项（不阻塞本期）

- **暗色模式**：当前令牌结构对暗色模式友好（用 `--color-on-surface` 而非硬编码 `#2b2f31`），预留 `@media (prefers-color-scheme: dark)` 块，下个 sprint 实施
- **WCAG AA 对比度审计**：跑 axe-core / pa11y，修 `--color-on-surface-faint` / `--color-accent-warm` 配白
- **`@vueuse/motion` 引入**：本期不引；下期若需要更复杂动效再评估
- **Tailwind v4 Vue 项目集成**：本期 `frontend/` 不依赖 Tailwind runtime，直接 import 设计令牌 CSS；下期若 Vue 项目要 utility class 化再装 `@tailwindcss/vite`

---

## 9. 关键文件清单

| 文件 | 角色 |
|---|---|
| `frontend-UI/src/index.css` | 设计令牌 source of truth |
| `frontend-UI/src/App.tsx` | 11 节可视化预览 |
| `frontend-UI/src/App.original.tsx` | 参考原貌备份（保留） |
| `frontend-UI/DESIGN_TOKENS.md` | 令牌文档镜像 |
| `frontend/src/assets/styles/main.scss` | **Phase 1 改造目标** |
| `frontend/src/App.vue` | **Phase 3 引用 `--color-nav-active`** |
| `frontend/src/views/Home.vue` | **Phase 3 重构目标** |
| `frontend/src/views/Chat.vue` | **Phase 4 重构目标** |
| `frontend/src/views/Profile.vue` | **Phase 5 重构目标** |
| `docs/superpowers/specs/2026-07-04-aifood-design-system-design.md` | **本文档** |

---

## 10. 一句话总结

> **方案**：把"冬日晴空"（冷蓝主 + 暖色点缀）的视觉语法，落地为单一令牌源 + 11 个自写组件 + 6 个 phase 实施；先把地基做对（Phase 1-2 砍掉 Material 3 旧命名 / Vant CSS 变量统一），再深耕 3 个高频页（Phase 3-5），最后验证归档（Phase 6）。**不要急, 慢慢对齐, 不 AI 感**。