/**
 * AI 美食推荐 · 设计系统预览页
 * 仅作设计令牌展示用, 不含业务逻辑.
 * 备份原参考实现: App.original.tsx
 */
import { motion } from "motion/react";
import {
  Sun, ArrowRight, Sparkles, ChevronRight, AlertCircle, CheckCircle2,
  Search, Mail, Lock, Heart, MapPin, Coffee, Clock, User, Bell,
  ChevronDown, Plus
} from "lucide-react";

const SECTIONS = [
  { id: "hero", label: "概述" },
  { id: "color", label: "色彩" },
  { id: "type", label: "字体" },
  { id: "space", label: "间距" },
  { id: "radius", label: "圆角" },
  { id: "shadow", label: "阴影" },
  { id: "glass", label: "玻璃" },
  { id: "motion", label: "动效" },
  { id: "button", label: "按钮" },
  { id: "card", label: "卡片" },
  { id: "field", label: "表单" },
  { id: "mobile", label: "移动端" },
];

const COLOR_GROUPS: Array<{ title: string; subtitle: string; items: Array<{ name: string; token: string; hex: string; text?: "light" | "dark" }> }> = [
  {
    title: "主色 · 冷蓝", subtitle: "Primary",
    items: [
      { name: "primary", token: "--color-primary", hex: "#0059b6" },
      { name: "primary-hover", token: "--color-primary-hover", hex: "#003f80" },
      { name: "primary-soft", token: "--color-primary-soft", hex: "#68a0ff" },
      { name: "cyan", token: "--color-cyan", hex: "#8ce1f3" },
    ],
  },
  {
    title: "点缀 · 旭日暖色", subtitle: "Accent Warm · 节制使用",
    items: [
      { name: "accent-warm", token: "--color-accent-warm", hex: "#e8855a", text: "light" },
      { name: "accent-warm-2", token: "--color-accent-warm-2", hex: "#f5b78a", text: "dark" },
    ],
  },
  {
    title: "表面 · 色阶", subtitle: "Surface",
    items: [
      { name: "surface", token: "--color-surface", hex: "#f4f7f9", text: "dark" },
      { name: "surface-low", token: "--color-surface-low", hex: "#edf1f3", text: "dark" },
      { name: "surface-lowest", token: "--color-surface-lowest", hex: "#ffffff", text: "dark" },
    ],
  },
  {
    title: "文字 · 色阶", subtitle: "On-surface",
    items: [
      { name: "on-surface", token: "--color-on-surface", hex: "#2b2f31", text: "light" },
      { name: "on-surface-variant", token: "--color-on-surface-variant", hex: "#585c5e", text: "light" },
      { name: "on-surface-faint", token: "--color-on-surface-faint", hex: "#8b9094", text: "light" },
      { name: "inverse-surface", token: "--color-inverse-surface", hex: "#0b0f10", text: "light" },
    ],
  },
  {
    title: "语义", subtitle: "Semantic",
    items: [
      { name: "danger", token: "--color-danger", hex: "#c8344a", text: "light" },
      { name: "success", token: "--color-success", hex: "#2f9e6e", text: "light" },
    ],
  },
];

const RADII = [
  { name: "sm", token: "--radius-sm", value: "8px", usage: "字段 / 小标签" },
  { name: "md", token: "--radius-md", value: "12px", usage: "按钮 / 列表项" },
  { name: "lg", token: "--radius-lg", value: "16px", usage: "卡片 / 弹层" },
  { name: "xl", token: "--radius-xl", value: "24px", usage: "大卡片 / 浮起菜单" },
  { name: "2xl", token: "--radius-2xl", value: "32px", usage: "hero 装饰" },
  { name: "pill", token: "--radius-pill", value: "999px", usage: "胶囊 / 头像 / 标签" },
];

const SHADOWS = [
  { name: "xs", token: "--shadow-xs", value: "0 1px 2px rgba(11,15,16,.05)" },
  { name: "sm", token: "--shadow-sm", value: "0 1px 2px / 0 1px 3px" },
  { name: "md", token: "--shadow-md", value: "0 4px 12px -2px" },
  { name: "lg", token: "--shadow-lg", value: "0 8px 24px -4px" },
  { name: "xl", token: "--shadow-xl", value: "0 16px 40px -8px" },
  { name: "glow", token: "--shadow-glow", value: "主题蓝 · 0 12px 32px rgba(0,89,182,.22)" },
  { name: "warm", token: "--shadow-warm", value: "暖橙 · 0 12px 32px rgba(232,133,90,.25)" },
];

const SPACE_SCALE = [
  { name: "1", value: "4px" }, { name: "2", value: "8px" }, { name: "3", value: "12px" },
  { name: "4", value: "16px" }, { name: "5", value: "20px" }, { name: "6", value: "24px" },
  { name: "8", value: "32px" }, { name: "10", value: "40px" }, { name: "12", value: "48px" },
  { name: "16", value: "64px" },
];

function SectionHeader({ index, title, subtitle, id }: { index: string; title: string; subtitle: string; id: string }) {
  return (
    <div id={id} className="mb-10 scroll-mt-24">
      <div className="flex items-baseline gap-4 mb-2">
        <span className="text-caption text-on-surface-faint">{index}</span>
        <h2 className="text-h1">{title}</h2>
        <span className="text-meta text-on-surface-variant">— {subtitle}</span>
      </div>
    </div>
  );
}

function Code({ children }: { children: string }) {
  return (
    <code className="px-1.5 py-0.5 rounded-md bg-surface-low text-[12px] font-mono text-on-surface-variant border border-surface-low">
      {children}
    </code>
  );
}

export default function App() {
  return (
    <div className="min-h-screen bg-cold-canvas text-on-surface font-sans">
      {/* ============ 浮动节导航 ============ */}
      <nav className="fixed top-4 left-1/2 -translate-x-1/2 z-50 pill-nav no-scrollbar overflow-x-auto max-w-[calc(100vw-32px)]">
        <ul className="flex items-center gap-1 px-3 py-2 whitespace-nowrap">
          {SECTIONS.map((s) => (
            <li key={s.id}>
              <a
                href={`#${s.id}`}
                className="block px-3 py-1.5 rounded-pill text-[12px] font-medium text-white/55 hover:text-white hover:bg-white/10 transition-colors duration-[180ms]"
              >
                {s.label}
              </a>
            </li>
          ))}
        </ul>
      </nav>

      {/* ============ HERO ============ */}
      <section id="hero" className="relative pt-32 pb-20 px-6 overflow-hidden bg-winter-sunrise">
        <div className="max-w-3xl mx-auto text-center">
          <motion.div
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, ease: [0.22, 1, 0.36, 1] }}
          >
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-pill bg-white/60 backdrop-blur-md border border-white/40 mb-6">
              <Sun size={12} className="text-accent-warm" strokeWidth={2.5} />
              <span className="text-caption text-on-surface-variant">冬日的旭日 · v0 · 2026-07-04</span>
            </div>

            <h1 className="text-hero-serif text-primary mb-4">
              冷蓝为底,<br />
              <span className="bg-gradient-to-br from-accent-warm to-accent-warm-2 bg-clip-text text-transparent">
                暖色点缀
              </span>
              。
            </h1>

            <p className="text-body text-on-surface-variant max-w-xl mx-auto mb-8">
              AI 美食推荐 · 设计系统预览。<br />
              Editorial 克制: 出场瞬间用满装饰, 密集场景保持紧凑。
            </p>

            <div className="flex items-center justify-center gap-3 flex-wrap">
              <a
                href="#color"
                className="inline-flex items-center gap-2 px-5 py-2.5 rounded-md btn-primary-gradient text-on-primary font-semibold text-meta shadow-glow transition-all duration-[180ms] active:scale-[0.97]"
              >
                查看令牌 <ArrowRight size={14} strokeWidth={2.5} />
              </a>
              <a
                href="#mobile"
                className="inline-flex items-center gap-2 px-5 py-2.5 rounded-md bg-white/70 backdrop-blur-md border border-white/40 text-on-surface font-semibold text-meta hover:bg-white transition-all duration-[180ms]"
              >
                移动端预览
              </a>
            </div>
          </motion.div>

          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.4, duration: 0.6 }}
            className="mt-20 flex flex-wrap items-center justify-center gap-x-8 gap-y-2 text-meta text-on-surface-faint"
          >
            <span>调性: 冷蓝主</span>
            <span className="opacity-40">·</span>
            <span>暖色节制 (≤3 处/屏)</span>
            <span className="opacity-40">·</span>
            <span>Plus Jakarta Sans + Newsreader</span>
            <span className="opacity-40">·</span>
            <span>大圆角 + 玻璃 + 主题阴影</span>
          </motion.div>
        </div>

        <div className="absolute bottom-6 left-1/2 -translate-x-1/2 text-on-surface-faint animate-bounce">
          <ChevronDown size={18} />
        </div>
      </section>

      {/* ============ COLOR PALETTE ============ */}
      <section className="max-w-5xl mx-auto px-6 py-20">
        <SectionHeader index="§1" title="色彩" subtitle="冷蓝主 + 暖色点缀" id="color" />

        {COLOR_GROUPS.map((group, gi) => (
          <div key={group.title} className="mb-12 last:mb-0">
            <div className="flex items-baseline gap-3 mb-4">
              <h3 className="text-h2">{group.title}</h3>
              <span className="text-meta text-on-surface-faint">{group.subtitle}</span>
            </div>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
              {group.items.map((c) => (
                <div key={c.token} className="group">
                  <div
                    className="aspect-[4/3] rounded-lg shadow-sm border border-surface-low mb-2 flex items-end p-3 transition-transform duration-[180ms] group-hover:-translate-y-0.5 group-hover:shadow-md"
                    style={{ backgroundColor: c.hex }}
                  >
                    <span className={`text-caption ${c.text === "light" ? "text-white/90" : "text-on-surface/70"}`}>
                      {c.hex.toUpperCase()}
                    </span>
                  </div>
                  <div className="text-meta font-semibold">{c.name}</div>
                  <Code>{c.token}</Code>
                </div>
              ))}
            </div>
          </div>
        ))}

        {/* 暖色使用规则 */}
        <div className="mt-8 p-5 rounded-lg bg-surface-lowest border-l-4 border-accent-warm">
          <div className="flex items-start gap-3">
            <Sparkles size={16} className="text-accent-warm mt-0.5 shrink-0" strokeWidth={2.5} />
            <div>
              <h4 className="text-meta font-semibold text-on-surface mb-1">暖色使用规则 · 防视觉污染</h4>
              <ul className="text-meta text-on-surface-variant space-y-0.5 list-disc list-inside">
                <li>单个屏幕同时出现 ≤ 3 处暖色</li>
                <li>尺寸 ≤ 80×80px 或字号 ≤ 32px</li>
                <li>仅 hero / CTA / 美食配图 / 状态徽章使用, 其余保持冷调</li>
              </ul>
            </div>
          </div>
        </div>
      </section>

      {/* ============ TYPOGRAPHY ============ */}
      <section className="max-w-5xl mx-auto px-6 py-20 border-t border-surface-low">
        <SectionHeader index="§2" title="字体" subtitle="Plus Jakarta Sans + Newsreader" id="type" />

        <div className="space-y-8">
          <div className="p-6 rounded-lg bg-surface-lowest shadow-sm">
            <div className="flex items-baseline gap-3 mb-3">
              <span className="text-caption text-on-surface-faint">hero-serif</span>
              <span className="text-meta text-on-surface-variant">仅 hero · 装饰 · Newsreader italic 600</span>
            </div>
            <div className="text-hero-serif text-primary">
              "The quiet morning light."
            </div>
            <div className="text-meta text-on-surface-faint mt-2">
              ⚠️ Newsreader italic 不含中文字形, 中文 hero 一律用 <code className="font-mono">text-h1</code> (sans bold)
            </div>
          </div>

          {[
            { cls: "text-h1", name: "h1", sample: "页面标题 · 24px / 700", usage: "页面顶部 H1" },
            { cls: "text-h2", name: "h2", sample: "区块标题 · 18px / 700", usage: "区块标题 / 卡片标题" },
            { cls: "text-body", name: "body", sample: "AI 美食推荐用对话理解你的口味、心情与场合, 给出一份不敷衍的推荐。", usage: "正文 / 描述" },
            { cls: "text-meta", name: "meta", sample: "今天 12:34 · 5 人已尝试", usage: "次级信息 / 时间戳" },
            { cls: "text-caption", name: "caption", sample: "RECOMMEND · 2026", usage: "标签 / 分类 / 状态" },
          ].map((t) => (
            <div key={t.cls} className="flex items-start gap-6 p-4 rounded-lg hover:bg-surface-low transition-colors duration-[180ms]">
              <div className="w-32 shrink-0">
                <div className="text-meta font-semibold">{t.name}</div>
                <Code>{t.cls}</Code>
              </div>
              <div className="flex-1">
                <div className={t.cls}>{t.sample}</div>
                <div className="text-meta text-on-surface-faint mt-1">{t.usage}</div>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* ============ SPACING ============ */}
      <section className="max-w-5xl mx-auto px-6 py-20 border-t border-surface-low">
        <SectionHeader index="§3" title="间距" subtitle="4-based scale" id="space" />

        <div className="space-y-2">
          {SPACE_SCALE.map((s) => (
            <div key={s.name} className="flex items-center gap-4">
              <div className="w-16 text-meta font-mono">--space-{s.name}</div>
              <div className="w-12 text-meta text-on-surface-variant text-right">{s.value}</div>
              <div
                className="h-3 bg-primary-soft rounded-sm"
                style={{ width: s.value }}
              />
            </div>
          ))}
        </div>
      </section>

      {/* ============ RADII ============ */}
      <section className="max-w-5xl mx-auto px-6 py-20 border-t border-surface-low">
        <SectionHeader index="§4" title="圆角" subtitle="8 → 32 → pill" id="radius" />

        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
          {RADII.map((r) => (
            <div key={r.token} className="group">
              <div
                className="aspect-square bg-gradient-to-br from-primary-soft to-cyan shadow-sm border border-surface-low mb-3 transition-transform duration-[280ms] group-hover:-translate-y-1 group-hover:shadow-md"
                style={{ borderRadius: r.value }}
              />
              <div className="text-meta font-semibold">{r.name} · {r.value}</div>
              <div className="text-meta text-on-surface-faint">{r.usage}</div>
            </div>
          ))}
        </div>
      </section>

      {/* ============ SHADOWS ============ */}
      <section className="max-w-5xl mx-auto px-6 py-20 border-t border-surface-low">
        <SectionHeader index="§5" title="阴影" subtitle="主题色着色 · 暖色稀有" id="shadow" />

        <div className="grid grid-cols-2 md:grid-cols-4 gap-8">
          {SHADOWS.map((s) => (
            <div key={s.token} className="flex flex-col items-center">
              <div
                className="w-full aspect-[4/3] bg-surface-lowest rounded-lg mb-3"
                style={{ boxShadow: s.value }}
              />
              <div className="text-meta font-semibold">{s.name}</div>
              <Code>{s.token}</Code>
            </div>
          ))}
        </div>
      </section>

      {/* ============ GLASS ============ */}
      <section className="relative py-20 px-6 overflow-hidden">
        <div className="absolute inset-0 bg-gradient-to-br from-cyan/30 via-surface to-surface-low" />
        <div className="absolute inset-0">
          <div className="absolute top-20 right-20 w-48 h-48 rounded-full bg-accent-warm/25 blur-3xl" />
        </div>

        <div className="relative max-w-5xl mx-auto">
          <SectionHeader index="§6" title="玻璃" subtitle="backdrop-filter: blur(20-32px)" id="glass" />

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="glass-card rounded-xl p-6">
              <div className="text-meta text-on-surface-variant mb-2">.glass-card</div>
              <h3 className="text-h2 mb-2">柔光卡片</h3>
              <p className="text-body">
                透过毛玻璃看世界的内容。半透白底 + 20px 模糊 + 1px 白边,
                用于 hero 浮层、顶部卡片、模态背景。
              </p>
            </div>

            <div className="glass-card-strong rounded-xl p-6">
              <div className="text-meta text-on-surface-variant mb-2">.glass-card-strong</div>
              <h3 className="text-h2 mb-2">强玻璃 (浮起菜单)</h3>
              <p className="text-body">
                更不透、更模糊 (32px)。用于底部弹起菜单、键盘顶部、确认对话框,
                需要明确前景层次的场景。
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* ============ MOTION ============ */}
      <section className="max-w-5xl mx-auto px-6 py-20 border-t border-surface-low">
        <SectionHeader index="§7" title="动效" subtitle="ease-out-soft · spring" id="motion" />

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <motion.div
            className="p-6 rounded-lg bg-surface-lowest shadow-sm border border-surface-low"
            whileHover={{ y: -4, boxShadow: "var(--shadow-lg)" }}
            transition={{ duration: 0.28, ease: [0.22, 1, 0.36, 1] }}
          >
            <div className="text-meta text-on-surface-faint mb-1">Hover Lift</div>
            <h4 className="text-h2 mb-2">浮起 · 280ms</h4>
            <p className="text-body text-on-surface-variant">默认进 / hover · ease-out-soft</p>
          </motion.div>

          <motion.button
            className="p-6 rounded-lg btn-primary-gradient text-on-primary shadow-glow text-left"
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.96 }}
            transition={{ duration: 0.18 }}
          >
            <div className="text-caption opacity-80 mb-1">Tap Spring</div>
            <h4 className="text-h2 mb-2">点按 · 180ms</h4>
            <p className="text-body opacity-90">scale 0.96 → 1.02 → 1, 体感反馈</p>
          </motion.button>

          <motion.div
            className="p-6 rounded-lg bg-gradient-to-br from-accent-warm to-accent-warm-2 shadow-warm text-[#7a3d22] cursor-pointer text-center"
            whileHover={{
              scale: [1, 1.05, 1],
              transition: {
                duration: 2,
                repeat: Infinity,
                ease: "easeInOut",
              },
            }}
            style={{ transformOrigin: "center" }}
          >
            <div className="text-caption opacity-70 mb-1">Hover · Breathing</div>
            <h4 className="text-h2 mb-2">悬停呼吸 · 中心向外</h4>
            <p className="text-body opacity-80">鼠标悬停时, 文字以中心为参照向外膨胀再回收, 2s 循环</p>
          </motion.div>
        </div>
      </section>

      {/* ============ BUTTONS ============ */}
      <section className="max-w-5xl mx-auto px-6 py-20 border-t border-surface-low">
        <SectionHeader index="§8" title="按钮" subtitle="主操作 / 次操作 / 暖色 CTA / 危险" id="button" />

        <div className="space-y-4">
          <div className="p-5 rounded-lg bg-surface-lowest border border-surface-low">
            <div className="text-meta text-on-surface-faint mb-3">主操作 · 冬日晴空渐变</div>
            <div className="flex flex-wrap items-center gap-3">
              <button className="inline-flex items-center gap-2 px-5 py-2.5 rounded-md btn-primary-gradient text-on-primary font-semibold text-meta shadow-glow hover:shadow-lg transition-all duration-[180ms] active:scale-[0.97]">
                开始对话 <ArrowRight size={14} strokeWidth={2.5} />
              </button>
              <button className="inline-flex items-center gap-2 px-5 py-2.5 rounded-md btn-primary-gradient text-on-primary font-semibold text-meta shadow-glow opacity-60 cursor-not-allowed" disabled>
                加载中…
              </button>
            </div>
          </div>

          <div className="p-5 rounded-lg bg-surface-lowest border border-surface-low">
            <div className="text-meta text-on-surface-faint mb-3">次操作</div>
            <div className="flex flex-wrap items-center gap-3">
              <button className="inline-flex items-center gap-2 px-5 py-2.5 rounded-md bg-white text-on-surface font-semibold text-meta border border-surface-low shadow-xs hover:shadow-sm hover:border-primary-soft transition-all duration-[180ms] active:scale-95">
                查看记录
              </button>
              <button className="inline-flex items-center gap-2 px-4 py-2 rounded-md text-on-surface font-medium text-meta hover:bg-surface-low transition-colors duration-[180ms]">
                取消
              </button>
            </div>
          </div>

          <div className="p-5 rounded-lg bg-surface-lowest border-l-4 border-accent-warm">
            <div className="text-meta text-on-surface-variant mb-3">暖色 CTA · 节制 · 仅 hero / 美食配图</div>
            <div className="flex flex-wrap items-center gap-3">
              <button className="inline-flex items-center gap-2 px-5 py-2.5 rounded-md bg-gradient-to-br from-accent-warm to-accent-warm-2 text-white font-semibold text-meta shadow-warm hover:shadow-lg transition-all duration-[180ms] active:scale-95">
                <Sparkles size={14} strokeWidth={2.5} />
                立刻尝试
              </button>
              <button className="inline-flex items-center gap-2 px-5 py-2.5 rounded-md bg-surface-low text-accent-warm font-semibold text-meta border border-accent-warm hover:bg-[#fef0e8] transition-all duration-[180ms] active:scale-95">
                收藏
              </button>
            </div>
          </div>

          <div className="p-5 rounded-lg bg-surface-lowest border border-surface-low">
            <div className="text-meta text-on-surface-faint mb-3">语义</div>
            <div className="flex flex-wrap items-center gap-3">
              <button className="inline-flex items-center gap-2 px-4 py-2 rounded-md text-danger font-semibold text-meta bg-[#fce4e8] hover:bg-[#f9d0d8] transition-colors duration-[180ms]">
                <AlertCircle size={14} strokeWidth={2.5} />
                删除
              </button>
              <button className="inline-flex items-center gap-2 px-4 py-2 rounded-md text-success font-semibold text-meta bg-[#dff3e9] hover:bg-[#c9ebd9] transition-colors duration-[180ms]">
                <CheckCircle2 size={14} strokeWidth={2.5} />
                已完成
              </button>
            </div>
          </div>
        </div>
      </section>

      {/* ============ CARDS ============ */}
      <section className="max-w-5xl mx-auto px-6 py-20 border-t border-surface-low">
        <SectionHeader index="§9" title="卡片" subtitle="surface / glass / elevated / warm" id="card" />

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Surface */}
          <div className="p-6 rounded-lg bg-surface-lowest shadow-sm border border-surface-low hover:shadow-md transition-shadow duration-[280ms]">
            <div className="flex items-center gap-3 mb-4">
              <div className="w-12 h-12 rounded-md bg-primary/10 flex items-center justify-center">
                <Coffee size={20} className="text-primary" strokeWidth={2.2} />
              </div>
              <div>
                <div className="text-meta text-on-surface-faint">Surface</div>
                <h3 className="text-h2">基础卡片</h3>
              </div>
            </div>
            <p className="text-body text-on-surface-variant">
              白底 + 浅阴影, 用于大多数列表项 / 信息块。
              hover 时阴影加深 1 档。
            </p>
            <div className="mt-4 flex items-center gap-2 text-meta text-primary font-semibold">
              查看详情 <ChevronRight size={14} />
            </div>
          </div>

          {/* Glass on gradient */}
          <div className="relative p-6 rounded-xl overflow-hidden bg-gradient-to-br from-primary-soft via-[#f4f7f9] to-cyan">
            <div className="absolute -top-12 -right-12 w-32 h-32 rounded-full bg-white/30 blur-2xl" />
            <div className="relative glass-card rounded-lg p-4">
              <div className="flex items-center gap-3 mb-3">
                <div className="w-10 h-10 rounded-pill bg-gradient-to-br from-accent-warm to-accent-warm-2 flex items-center justify-center">
                  <Heart size={16} className="text-white" fill="white" strokeWidth={2.2} />
                </div>
                <div>
                  <div className="text-meta text-on-surface-faint">Glass · Hero</div>
                  <h3 className="text-h2">推荐时刻</h3>
                </div>
              </div>
              <p className="text-body">
                一碗热气腾腾的兰州牛肉面, 适合这周降温的晚上。
              </p>
              <div className="mt-3 inline-flex items-center gap-1 px-2 py-1 rounded-pill bg-accent-warm/15 text-accent-warm text-meta font-semibold">
                <MapPin size={12} strokeWidth={2.5} />
                步行 8 分钟
              </div>
            </div>
          </div>

          {/* Elevated (homepage hero card preview) */}
<div className="md:col-span-2 p-8 rounded-2xl bg-gradient-to-br from-primary to-[#2c5fa0] text-white shadow-xl relative overflow-hidden">
              <div className="absolute top-0 right-0 w-64 h-64 rounded-full bg-cyan/20 blur-3xl" />
              <div className="absolute bottom-0 left-0 w-48 h-48 rounded-full bg-accent-warm/30 blur-3xl" />
              <div className="relative">
                <div className="text-caption opacity-70 mb-2">Editorial Hero · 32px 圆角 · 大标题</div>
                <h3 className="text-h1 mb-3">今晚, 想吃点什么?</h3>
                <p className="text-body opacity-90 mb-6 max-w-md">
                  告诉 AI 你的心情、时间、预算和同行者,
                  它会用 7 个问题理解你的口味。
                </p>
                <button className="inline-flex items-center gap-2 px-6 py-3 rounded-pill bg-white text-primary font-semibold text-meta shadow-warm hover:shadow-lg transition-all duration-[180ms] active:scale-95">
                  <Sparkles size={14} strokeWidth={2.5} />
                  开始 30 秒对话
                </button>
              </div>
            </div>
        </div>
      </section>

      {/* ============ FIELDS ============ */}
      <section className="max-w-5xl mx-auto px-6 py-20 border-t border-surface-low">
        <SectionHeader index="§10" title="表单" subtitle="rest / focus / error" id="field" />

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {[
            { label: "邮箱", placeholder: "you@example.com", icon: <Mail size={16} />, state: "rest" },
            { label: "搜索美食", placeholder: "牛肉面、奶茶、火锅…", icon: <Search size={16} />, state: "focus" },
            { label: "密码", placeholder: "••••••••", icon: <Lock size={16} />, state: "error" },
          ].map((f) => (
            <div key={f.label} className="space-y-2">
              <label className="text-meta font-semibold text-on-surface">{f.label}</label>
              <div className={`relative flex items-center gap-2 h-11 px-3.5 rounded-md bg-surface-lowest border transition-all duration-[180ms] ${
                f.state === "focus"
                  ? "border-primary shadow-[0_0_0_3px_rgba(0,89,182,0.12)] bg-white"
                  : f.state === "error"
                  ? "border-danger bg-[#fce4e8]"
                  : "border-surface-low"
              }`}>
                <span className={`shrink-0 ${f.state === "error" ? "text-danger" : "text-on-surface-variant"}`}>{f.icon}</span>
                <input
                  className="flex-1 bg-transparent outline-none text-meta text-on-surface placeholder:text-on-surface-faint"
                  placeholder={f.placeholder}
                  readOnly
                />
              </div>
              <div className={`text-meta ${f.state === "error" ? "text-danger" : "text-on-surface-faint"}`}>
                {f.state === "error" ? "格式不正确,请检查后重试" : f.state === "focus" ? "正在输入…" : "辅助说明文字"}
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* ============ MOBILE MOCK ============ */}
      <section className="max-w-5xl mx-auto px-6 py-20 border-t border-surface-low">
        <SectionHeader index="§11" title="移动端预览" subtitle="375 × 812 · iPhone 框架" id="mobile" />

        <div className="flex flex-wrap items-start gap-12 justify-center">
          {/* Home (Hero) */}
          <PhoneMock label="Home · Editorial 满">
            <div className="h-full bg-winter-sunrise p-4 flex flex-col">
              <div className="flex items-center justify-between mb-6 pt-2">
                <div>
                  <div className="text-meta text-on-surface-variant">晚上好</div>
                  <div className="text-h2">小柚</div>
                </div>
                <div className="w-9 h-9 rounded-pill bg-white/70 backdrop-blur-md border border-white/40 flex items-center justify-center">
                  <Bell size={14} className="text-primary" />
                </div>
              </div>

              <div className="glass-card rounded-xl p-4 mb-3">
                <div className="text-caption text-accent-warm mb-1">今晚的推荐</div>
                <div className="text-h1 text-primary leading-[1.2]">
                  一碗热汤面,<br />暖心暖胃。
                </div>
              </div>

              <div className="flex gap-2 mb-3">
                {[
                  { icon: <Coffee size={14} />, label: "热汤面" },
                  { icon: <Heart size={14} />, label: "暖心菜" },
                  { icon: <Sparkles size={14} />, label: "夜宵" },
                ].map((c) => (
                  <div key={c.label} className="flex-1 px-3 py-2 rounded-md bg-white/70 backdrop-blur-md border border-white/40 flex items-center gap-1.5">
                    <span className="text-primary">{c.icon}</span>
                    <span className="text-meta font-semibold">{c.label}</span>
                  </div>
                ))}
              </div>

              <div className="mt-auto">
                <button className="w-full inline-flex items-center justify-center gap-2 px-5 py-3 rounded-md btn-primary-gradient text-on-primary font-semibold text-meta shadow-glow active:scale-[0.97]">
                  开始 30 秒对话 <ArrowRight size={14} />
                </button>
              </div>

              {/* Bottom nav */}
              <div className="mt-3 pill-nav mx-auto flex items-center gap-1 px-2 py-1.5">
                {[
                  { icon: <Coffee size={14} />, active: true },
                  { icon: <User size={14} /> },
                  { icon: <Bell size={14} /> },
                  { icon: <Heart size={14} /> },
                ].map((n, i) => (
                  <div key={i} className={`w-8 h-8 rounded-pill flex items-center justify-center ${n.active ? "text-[#22d3ee]" : "text-white/40"}`}>
                    {n.icon}
                  </div>
                ))}
              </div>
            </div>
          </PhoneMock>

          {/* Chat (Compact) */}
          <PhoneMock label="Chat · 紧凑">
            <div className="h-full bg-surface-lowest flex flex-col">
              {/* Header */}
              <div className="px-4 pt-3 pb-2 border-b border-surface-low flex items-center gap-3">
                <div className="w-9 h-9 rounded-pill bg-gradient-to-br from-primary to-cyan flex items-center justify-center text-white">
                  <Sparkles size={14} />
                </div>
                <div className="flex-1">
                  <div className="text-meta font-semibold">AI 小厨</div>
                  <div className="text-meta text-on-surface-faint">正在思考…</div>
                </div>
                <Plus size={16} className="text-on-surface-variant" />
              </div>

              {/* Messages */}
              <div className="flex-1 p-3 space-y-2 overflow-hidden">
                <div className="flex gap-2 items-start">
                  <div className="w-7 h-7 rounded-md bg-gradient-to-br from-accent-warm to-accent-warm-2 shrink-0" />
                  <div className="max-w-[70%] px-3 py-2 rounded-md bg-gradient-to-br from-[#fef0e8] to-[#fff5ee] text-[#7a3d22] text-meta">
                    你今天想吃什么类型的?
                  </div>
                </div>

                <div className="flex gap-2 items-start justify-end">
                  <div className="max-w-[70%] px-3 py-2 rounded-md bg-primary text-on-primary text-meta">
                    想吃暖一点的, 预算 30 块以内
                  </div>
                </div>

                <div className="flex gap-2 items-start">
                  <div className="w-7 h-7 rounded-md bg-gradient-to-br from-accent-warm to-accent-warm-2 shrink-0" />
                  <div className="max-w-[70%] px-3 py-2 rounded-md bg-gradient-to-br from-[#fef0e8] to-[#fff5ee] text-[#7a3d22] text-meta">
                    收到, 推荐一碗兰州牛肉面如何?
                  </div>
                </div>

                <div className="flex gap-2 items-start">
                  <div className="w-7 h-7 rounded-md bg-gradient-to-br from-accent-warm to-accent-warm-2 shrink-0" />
                  <div className="px-3 py-2 rounded-md bg-gradient-to-br from-[#fef0e8] to-[#fff5ee] flex gap-1">
                    <span className="w-1.5 h-1.5 rounded-pill bg-[#7a3d22]/40 animate-bounce" style={{ animationDelay: "0ms" }} />
                    <span className="w-1.5 h-1.5 rounded-pill bg-[#7a3d22]/40 animate-bounce" style={{ animationDelay: "150ms" }} />
                    <span className="w-1.5 h-1.5 rounded-pill bg-[#7a3d22]/40 animate-bounce" style={{ animationDelay: "300ms" }} />
                  </div>
                </div>
              </div>

              {/* Input */}
              <div className="p-2 border-t border-surface-low flex gap-2">
                <div className="flex-1 h-9 px-3 rounded-pill bg-surface-low flex items-center text-meta text-on-surface-faint">
                  说点什么…
                </div>
                <button className="w-9 h-9 rounded-pill btn-primary-gradient text-on-primary flex items-center justify-center">
                  <ArrowRight size={14} />
                </button>
              </div>
            </div>
          </PhoneMock>

          {/* Profile (Editorial) */}
          <PhoneMock label="Profile · Editorial 满">
            <div className="h-full bg-cold-canvas flex flex-col">
              {/* Top bar with avatar */}
              <div className="relative bg-gradient-to-br from-primary to-[#2c5fa0] pt-6 pb-6 px-4 text-white">
                <div className="absolute top-3 right-4 w-8 h-8 rounded-pill bg-white/20 backdrop-blur-md flex items-center justify-center">
                  <Clock size={14} />
                </div>
                <div className="flex items-center gap-3">
                  <div className="w-16 h-16 rounded-pill bg-gradient-to-br from-accent-warm to-accent-warm-2 border-2 border-white shadow-warm" />
                  <div>
                    <div className="text-meta opacity-70">@xiaoyu</div>
                    <div className="text-h2">小柚</div>
                    <div className="text-meta opacity-80">偏爱: 暖食 / 辣 / 不挑</div>
                  </div>
                </div>
              </div>

              {/* Stats cards (glass on gradient) */}
              <div className="px-4 pt-4 grid grid-cols-3 gap-2">
                {[
                  { num: 42, label: "对话" },
                  { num: 18, label: "收藏" },
                  { num: 7, label: "天数" },
                ].map((s, i) => (
                  <div key={i} className="bg-white/95 backdrop-blur-md rounded-lg pt-4 pb-2 px-3 text-center shadow-md">
                    <div className="text-hero-serif text-primary text-[26px]">{s.num}</div>
                    <div className="text-caption text-on-surface-variant mt-0.5">{s.label}</div>
                  </div>
                ))}
              </div>

              {/* List */}
              <div className="p-4 space-y-2 flex-1">
                {[
                  { icon: <Heart size={16} />, label: "我的收藏", count: 18 },
                  { icon: <Coffee size={16} />, label: "历史记录", count: 42 },
                  { icon: <MapPin size={16} />, label: "附近店铺", count: null },
                  { icon: <User size={16} />, label: "编辑资料", count: null },
                ].map((item) => (
                  <div key={item.label} className="flex items-center gap-3 p-3 rounded-md bg-surface-lowest border border-surface-low">
                    <span className="text-primary">{item.icon}</span>
                    <span className="flex-1 text-meta font-semibold">{item.label}</span>
                    {item.count && <span className="text-meta text-on-surface-variant">{item.count}</span>}
                    <ChevronRight size={14} className="text-on-surface-faint" />
                  </div>
                ))}
              </div>
            </div>
          </PhoneMock>
        </div>
      </section>

      {/* ============ FOOTER ============ */}
      <footer className="border-t border-surface-low bg-surface-lowest py-12 px-6">
        <div className="max-w-5xl mx-auto flex flex-wrap items-start justify-between gap-8">
          <div>
            <div className="text-hero-serif text-primary text-[28px] leading-[1] mb-2">
              "Winter Sunrise"
            </div>
            <p className="text-meta text-on-surface-variant">
              冷蓝为底, 暖色点缀。<br />
              Editorial 克制 · 移动端优先。
            </p>
          </div>
          <div className="text-right text-meta text-on-surface-faint space-y-1">
            <div>version 0 · 2026-07-04</div>
            <div>src/index.css + App.tsx</div>
            <div className="text-caption">
              {SECTIONS.length} 个章节
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
}

function PhoneMock({ children, label }: { children: React.ReactNode; label: string }) {
  return (
    <div className="flex flex-col items-center">
      <div className="w-[300px] h-[620px] rounded-[40px] bg-[#0b0f10] p-2 shadow-xl">
        <div className="relative w-full h-full rounded-[32px] overflow-hidden bg-surface">
          {/* Dynamic island — 居中小岛, 颜色统一用 bezel 色, 不抢戏 */}
          <div className="absolute top-2.5 left-1/2 -translate-x-1/2 w-20 h-4 rounded-full bg-[#0b0f10] z-10" />
          <div className="h-full pt-9 overflow-hidden">
            {children}
          </div>
        </div>
      </div>
      <div className="mt-4 text-caption text-on-surface-faint">{label}</div>
    </div>
  );
}
