# AI 美食推荐 · 设计系统令牌 (Design Tokens)

> **调性**: 冷蓝主 + 暖色点缀 — 冬日的旭日
> **浓度**: Editorial 克制 (hero 用满装饰, 密集场景紧凑)
> **策略**: Vant 保留 + SCSS 变量穿透, 视觉变化自动传递到全部 23 view
> **来源**: `frontend-UI/src/index.css` (Tailwind v4 `@theme` + `:root` + 自定义 utility class)
> **预览**: http://localhost:12121 (构建产物) · 源码 `frontend-UI/src/App.tsx`

---

## 1. 色彩 (Color)

### 1.1 主色 · 冷蓝（冬日晴空 · 带白）

| Token | Hex | 用途 |
|---|---|---|
| `--color-primary` | `#4a8dd5` | 主操作 / 链接 / 标题（雪天+蓝天带云） |
| `--color-primary-hover` | `#2c5fa0` | 主操作 hover（深一档仍有读感）|
| `--color-primary-soft` | `#c5dcf0` | 主色淡 · 装饰 / 浅底（带白提示） |
| `--color-primary-grad-top` | `#5b9be0` | 主按钮渐变顶端（晴空浅）|
| `--color-primary-grad-bottom` | `#3a7bc0` | 主按钮渐变底端（晴空深）|
| `--color-cyan` | `#8ce1f3` | 次级冷色 · 进度 / 数据条 |

**主按钮专用 utility class**：
```css
.btn-primary-gradient {
  background: linear-gradient(180deg, var(--color-primary-grad-top) 0%, var(--color-primary-grad-bottom) 100%);
}
```
模拟晴空从上至下：上轻下深，有"雪天+蓝天"的微妙纵深感，不夸张。

### 1.2 点缀 · 旭日暖色 (节制)

| Token | Hex | 用途 |
|---|---|---|
| `--color-accent-warm` | `#e8855a` | 旭日橙 · hero CTA / 美食配图 |
| `--color-accent-warm-2` | `#f5b78a` | 暖橙淡 · hover / 装饰渐变 |

**使用规则 (防视觉污染)**:
- 单个屏幕同时出现 ≤ **3 处** 暖色
- 尺寸 ≤ **80×80px** 或字号 ≤ **32px**
- 仅 hero / CTA / 美食配图 / 状态徽章, 其余保持冷调

### 1.3 表面 · 色阶

| Token | Hex | 用途 |
|---|---|---|
| `--color-surface` | `#f4f7f9` | 全局底色 |
| `--color-surface-low` | `#edf1f3` | 卡片底 / 输入框底 |
| `--color-surface-lowest` | `#ffffff` | 浮起底 / 弹层 |

### 1.4 文字 · 色阶

| Token | Hex | 用途 |
|---|---|---|
| `--color-on-surface` | `#2b2f31` | 正文 |
| `--color-on-surface-variant` | `#585c5e` | 次级正文 |
| `--color-on-surface-faint` | `#8b9094` | 提示 / 占位 |
| `--color-inverse-surface` | `#0b0f10` | 反转 · App.vue 浮动 nav 底 |

### 1.5 语义

| Token | Hex | 用途 |
|---|---|---|
| `--color-danger` | `#c8344a` | 警告 / 错误 |
| `--color-success` | `#2f9e6e` | 成功 |

### 1.6 Nav 激活态

| Token | 值 | 用途 |
|---|---|---|
| `--color-nav-active` | `#22d3ee` | App.vue 浮动 nav active 文字 + dot 背景 |

---

## 2. 字体 (Typography)

| Token | Stack | 用途 |
|---|---|---|
| `--font-sans` | `"Plus Jakarta Sans", "PingFang SC", "Microsoft YaHei", "Noto Sans SC", ui-sans-serif, system-ui, sans-serif` | 正文 / UI / 中文回退 PingFang / 雅黑 / Noto |
| `--font-serif` | `"Newsreader", "Songti SC", "STSong", Georgia, serif` | 仅 hero **英文 / 数字** · 装饰引文 · italic 600 |

字体引入: `frontend/index.html` 已经在 preconnect + 加载这两个字体。

### 2.0 ⚠️ Newsreader italic 使用规则

**Newsreader 不含中文字形** —— 浏览器套到中文上会**机械倾斜 12° 当假斜体**，字符像被风吹倒，跟真 italic 西文标点对不上。

| 场景 | 应该用 |
|---|---|
| 纯英文 hero / 装饰引文 | `text-hero-serif`（Newsreader italic 600）✅ |
| 纯数字 hero（Profile 42/18/7） | `text-hero-serif` ✅ |
| **中文 hero 标题 / 装饰** | **`text-h1`（Plus Jakarta sans bold 700）** ❌ 不套 serif italic |
| 中文数字与英文混排 | 用 sans，serif italic 仅作用英文部分 |

### 2.1 字号层级 (Editorial 克制)

| Class | 字号 / 行高 / 字重 | 用途 |
|---|---|---|
| `.text-hero-serif` | 40 / 1.05 / 600 italic · serif | **仅** hero 标题 / 装饰数字 |
| `.text-h1` | 24 / 1.25 / 700 · sans | 页面顶部 H1 |
| `.text-h2` | 18 / 1.3 / 700 · sans | 区块标题 / 卡片标题 |
| `.text-body` | 14 / 1.45 / 400 · sans | 正文 |
| `.text-meta` | 12 / 1.4 / 500 · sans | 次级信息 / 时间戳 |
| `.text-caption` | 11 / 1.4 / 600 · sans uppercase · tracking .04em | 标签 / 分类 / 状态 |

**font-feature-settings**: Plus Jakarta Sans 默认已开启 `ss01` 备用样式, 不强制开启.

---

## 3. 圆角 (Radii)

| Token | Value | 用途 |
|---|---|---|
| `--radius-sm` | 8px | 字段 / 小标签 |
| `--radius-md` | 12px | 按钮 / 列表项 / Vant Cell 默认 |
| `--radius-lg` | 16px | 卡片 / 弹层 |
| `--radius-xl` | 24px | 大卡片 / 浮起菜单 |
| `--radius-2xl` | 32px | hero 装饰 · 仅 Home / Profile 顶部 |
| `--radius-pill` | 999px | 胶囊 / 头像 / 标签 |

**移动端建议**: 密集页面 (Chat / Feed) 用 `--radius-md` (12px) 保持紧凑, hero / 详情页用 `--radius-xl` (24px).

---

## 4. 间距 (Spacing) · 4-based

| Token | Value |
|---|---|
| `--space-1` | 4px |
| `--space-2` | 8px |
| `--space-3` | 12px |
| `--space-4` | 16px |
| `--space-5` | 20px |
| `--space-6` | 24px |
| `--space-8` | 32px |
| `--space-10` | 40px |
| `--space-12` | 48px |
| `--space-16` | 64px |

---

## 5. 阴影 (Shadow) · 主题色着色

| Token | 值 | 用途 |
|---|---|---|
| `--shadow-xs` | `0 1px 2px rgba(11,15,16,.05)` | 极轻 · 内嵌 |
| `--shadow-sm` | `0 1px 2px + 0 1px 3px` | 卡片 |
| `--shadow-md` | `0 4px 12px -2px` | 浮起 |
| `--shadow-lg` | `0 8px 24px -4px` | 弹层 / 菜单 |
| `--shadow-xl` | `0 16px 40px -8px` | 模态 / hero |
| `--shadow-glow` | `0 12px 32px rgba(0,89,182,.22)` | **主题蓝** · hero / 主 CTA |
| `--shadow-warm` | `0 12px 32px rgba(232,133,90,.25)` | **暖橙** · 旭日 CTA · 稀有 |

---

## 6. 玻璃 (Glass)

| Token | 值 | 用途 |
|---|---|---|
| `--glass-bg` | `rgba(255,255,255,.62)` | 玻璃卡片底 |
| `--glass-bg-strong` | `rgba(255,255,255,.82)` | 强玻璃 · 弹层 |
| `--glass-blur` | `blur(20px)` | 默认玻璃模糊 |
| `--glass-blur-strong` | `blur(32px)` | 强玻璃模糊 |
| `--glass-border` | `rgba(255,255,255,.4)` | 玻璃边框 |

**使用类**: `.glass-card` / `.glass-card-strong` (在 `index.css` 已封装)

**移动端**: 玻璃效果仅用于 hero / 浮起菜单 / 顶部, 聊天 / Feed 内层不放玻璃 (性能 + 视觉污染).

---

## 7. 动效 (Motion)

### 7.1 曲线

| Token | 值 | 用途 |
|---|---|---|
| `--ease-out-soft` | `cubic-bezier(0.22, 1, 0.36, 1)` | 默认进 / hover |
| `--ease-in-soft` | `cubic-bezier(0.55, 0, 0.45, 0)` | 退场 |
| `--ease-spring` | `cubic-bezier(0.34, 1.56, 0.64, 1)` | 弹簧 · 仅装饰 |

### 7.2 时长

| Token | Value |
|---|---|
| `--dur-fast` | 180ms (hover, tap) |
| `--dur-base` | 280ms (default) |
| `--dur-slow` | 480ms (page transition) |
| `--dur-spring` | 680ms (hero motion) |

---

## 8. 装饰背景 (Decorative Backgrounds)

| Class | 描述 |
|---|---|
| `.bg-winter-sunrise` | 暖橙 + 浅青双 radial + 冷蓝渐变 (冬日旭日) |
| `.bg-cold-canvas` | 蓝紫 radial + 冷底渐变 (冷淡 · Profile 用) |

---

## 9. 复用类

- `.glass-card` / `.glass-card-strong` — 玻璃
- `.shadow-glow` / `.shadow-warm` — 主题阴影
- `.pill-nav` — App.vue 浮动 nav 同款 (深色胶囊 + blur)
- `.no-scrollbar` — 隐藏滚动条
- `.text-hero-serif` / `.text-h1` / `.text-h2` / `.text-body` / `.text-meta` / `.text-caption` — 字体层级
- `.text-truncate` / `.text-clamp-2` / `.text-clamp-3` — 文本截断
- `.bg-winter-sunrise` / `.bg-cold-canvas` — 装饰背景
- `.font-serif` / `.font-sans` — 字体 utility

---

## 9.5 a11y / 移动端 / 工程化令牌 (新增 2026-07-04 复审)

| Token | 值 | 用途 |
|---|---|---|
| `--color-nav-active` | `#22d3ee` | App.vue 浮动 nav active 文字 / dot |
| `--focus-ring-width` | `3px` | 键盘焦点环宽度 (WCAG 2.4.7) |
| `--focus-ring-color` | `rgba(0,89,182,0.25)` | 焦点环颜色 |
| `--focus-ring` | `0 0 0 3px rgba(0,89,182,0.25)` | 焦点环合成 |
| `--safe-area-top` | `env(safe-area-inset-top, 0px)` | iPhone 刘海 |
| `--safe-area-bottom` | `env(safe-area-inset-bottom, 0px)` | 底部小条 |
| `--z-base` / `--z-sticky` / `--z-nav` / `--z-dialog` / `--z-toast` | 1 / 50 / 100 / 200 / 300 | Z-index scale |
| `--min-tap-target` | `44px` | 最小点击区 (WCAG 2.5.5) |
| `--color-skeleton` | `#e8ecf0` | 骨架屏底色 |
| `--color-skeleton-shimmer` | `#f4f6f8` | 骨架屏高光 |

**motion-reduce 媒体查询**（自动生效）：

```css
@media (prefers-reduced-motion: reduce) {
  :root {
    --dur-fast: 0ms;
    --dur-base: 0ms;
    --dur-slow: 0ms;
    --dur-spring: 0ms;
  }
}
```

---

## 10. Vant 主题变量映射 (地基 phase 2)

> ⚠️ **修正**: 原方案计划用 SCSS 变量穿透 (`$primary` 等) 覆盖 Vant, 经多视角复审发现:
> - Vant 4 已全面转向 **CSS 变量** + Theme Object, SCSS 变量穿透已被弃用
> - 当前 `frontend/` 代码里 Vant 实际用量极低 (1 VanIcon + 3 showConfirmDialog), 大部分 view 用 vanilla HTML
> - 因此 SCSS 覆盖策略**不再适用**, 改为最小 CSS 变量覆盖:

```css
/* 在 main.scss :root 顶部覆盖 3 个变量即可 */
:root {
  --van-primary-color: var(--color-primary);
  --van-text-color: var(--color-on-surface);
  --van-background: var(--color-surface);
}
```

Vant 其余组件若用到, 按 Vant 官方 CSS 变量文档 (https://vant-ui.github.io/vant/v4/#/en-US/theme) 增量添加.

---

## 11. 字体引入 (复用)

`frontend/index.html` 已加载:

```html
<link rel="preconnect" href="https://fonts.googleapis.com" />
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin />
<link
  href="https://fonts.googleapis.com/css2?family=Newsreader:ital,opsz,wght@0,6..72,200..800;1,6..72,200..800&family=Plus+Jakarta+Sans:wght@200..800&display=swap"
  rel="stylesheet"
/>
```

`frontend-UI/index.html` 同样加载. 直接复用.

---

## 12. 设计原则

1. **冷为主, 暖为辅** — 单屏暖色 ≤ 3 处, 视觉重量不超过 hero CTA
2. **Editorial 克制** — `Newsreader italic` 仅用于 hero 数字, 全文 sans
3. **移动优先** — 密集页 (Chat / Feed) 用 `--radius-md`, hero / 详情用 `--radius-xl`+
4. **玻璃稀有** — 仅 hero / 浮起菜单 / 顶部装饰, 不滥用
5. **主题阴影** — 默认阴影用 `--shadow-md`, 强调元素用 `--shadow-glow` (蓝) / `--shadow-warm` (橙)
6. **动效节制** — 默认 280ms ease-out-soft, 仅 hero CTA 可用 spring

---

## 13. 文件清单

| 文件 | 角色 |
|---|---|
| `frontend-UI/src/index.css` | 全部令牌 (CSS) |
| `frontend-UI/src/App.tsx` | 可视化预览 (本页所述即所见) |
| `frontend-UI/src/App.original.tsx` | 参考原貌备份 (不要删) |
| `frontend-UI/DESIGN_TOKENS.md` | 本文档 |

---

*版本 v0 · 2026-07-04 · 待地基 phase 1 在 frontend/ 实施后回填 "Vant 变量映射" 实际值.*