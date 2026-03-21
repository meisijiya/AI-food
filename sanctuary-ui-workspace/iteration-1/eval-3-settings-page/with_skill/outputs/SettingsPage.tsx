import { useState } from "react";
import { motion } from "motion/react";
import {
  Bell,
  Shield,
  Palette,
  User,
  ChevronRight,
  Moon,
  Sun,
  Monitor,
  Mail,
  MessageSquare,
  Heart,
  LogOut,
  Trash2,
  Lock,
  Eye,
  EyeOff,
  Smartphone,
  Globe,
  ChevronDown,
} from "lucide-react";

/* ────────────────────────────── Toggle Switch ────────────────────────────── */

function Toggle({
  enabled,
  onChange,
}: {
  enabled: boolean;
  onChange: (v: boolean) => void;
}) {
  return (
    <button
      onClick={() => onChange(!enabled)}
      className={`relative w-12 h-7 rounded-full transition-colors duration-300 ${
        enabled ? "bg-primary" : "bg-on-surface-variant/20"
      }`}
    >
      <motion.div
        layout
        transition={{ type: "spring", stiffness: 500, damping: 30 }}
        className={`absolute top-1 w-5 h-5 rounded-full bg-white shadow-md ${
          enabled ? "left-6" : "left-1"
        }`}
      />
    </button>
  );
}

/* ────────────────────────────── Dropdown ────────────────────────────── */

function Dropdown({
  value,
  options,
  onChange,
}: {
  value: string;
  options: { label: string; value: string }[];
  onChange: (v: string) => void;
}) {
  return (
    <div className="relative">
      <select
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="appearance-none bg-white/60 backdrop-blur-sm border border-white rounded-2xl px-5 py-2.5 pr-10 text-sm font-sans text-on-surface cursor-pointer hover:bg-white/80 transition-colors focus:outline-none focus:ring-2 focus:ring-primary/30 shadow-sm"
      >
        {options.map((opt) => (
          <option key={opt.value} value={opt.value}>
            {opt.label}
          </option>
        ))}
      </select>
      <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-on-surface-variant pointer-events-none" />
    </div>
  );
}

/* ────────────────────────────── Section Card ────────────────────────────── */

function SettingsSection({
  icon: Icon,
  title,
  glowColor,
  children,
  delay = 0,
}: {
  icon: React.ElementType;
  title: string;
  glowColor: string;
  children: React.ReactNode;
  delay?: number;
}) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 24, scale: 0.97 }}
      animate={{ opacity: 1, y: 0, scale: 1 }}
      transition={{ delay, duration: 0.5, ease: "easeOut" }}
      whileHover={{ y: -4 }}
      className="relative group"
    >
      {/* Decorative glow */}
      <div
        className={`absolute top-0 right-0 w-48 h-48 ${glowColor} rounded-full blur-3xl -mr-16 -mt-16 group-hover:opacity-100 opacity-60 transition-opacity duration-700`}
      />

      <div className="relative bg-surface-container-lowest rounded-[2rem] sm:rounded-[2.5rem] p-8 sm:p-10 border border-white shadow-sm overflow-hidden">
        {/* Section header */}
        <div className="flex items-center gap-4 mb-8">
          <div className="w-11 h-11 rounded-2xl bg-primary/10 flex items-center justify-center">
            <Icon className="w-5 h-5 text-primary" />
          </div>
          <h2 className="font-serif italic text-2xl text-on-surface">{title}</h2>
        </div>

        <div className="space-y-1">{children}</div>
      </div>
    </motion.div>
  );
}

/* ────────────────────────────── Setting Row ────────────────────────────── */

function SettingRow({
  icon: Icon,
  label,
  description,
  children,
}: {
  icon: React.ElementType;
  label: string;
  description?: string;
  children: React.ReactNode;
}) {
  return (
    <motion.div
      whileHover={{ x: 4 }}
      className="flex items-center justify-between gap-4 py-4 px-4 -mx-4 rounded-2xl hover:bg-surface-container-low/60 transition-colors"
    >
      <div className="flex items-center gap-4 min-w-0">
        <div className="w-9 h-9 rounded-xl bg-surface-container-low flex items-center justify-center shrink-0">
          <Icon className="w-4 h-4 text-on-surface-variant" />
        </div>
        <div className="min-w-0">
          <p className="text-sm font-semibold text-on-surface">{label}</p>
          {description && (
            <p className="text-xs text-on-surface-variant mt-0.5">
              {description}
            </p>
          )}
        </div>
      </div>
      {children}
    </motion.div>
  );
}

/* ════════════════════════════════ MAIN PAGE ════════════════════════════════ */

export default function SettingsPage() {
  /* ── Notification state ── */
  const [pushEnabled, setPushEnabled] = useState(true);
  const [emailDigest, setEmailDigest] = useState(true);
  const [wellnessReminders, setWellnessReminders] = useState(true);
  const [communityMessages, setCommunityMessages] = useState(false);

  /* ── Privacy state ── */
  const [profileVisible, setProfileVisible] = useState(true);
  const [activityVisible, setActivityVisible] = useState(false);
  const [dataCollection, setDataCollection] = useState(true);

  /* ── Appearance state ── */
  const [theme, setTheme] = useState("system");
  const [accentColor, setAccentColor] = useState("blue");
  const [reduceMotion, setReduceMotion] = useState(false);

  /* ── Account state ── */
  const [twoFactor, setTwoFactor] = useState(false);

  const themeOptions = [
    { label: "System Default", value: "system" },
    { label: "Light", value: "light" },
    { label: "Dark", value: "dark" },
  ];

  const accentOptions = [
    { label: "Blue", value: "blue" },
    { label: "Teal", value: "teal" },
    { label: "Lime", value: "lime" },
    { label: "Violet", value: "violet" },
  ];

  return (
    <div className="min-h-screen bg-surface">
      {/* ── Page background glows ── */}
      <div className="fixed inset-0 pointer-events-none overflow-hidden">
        <div className="absolute -top-40 -right-40 w-[500px] h-[500px] bg-secondary-fixed/8 rounded-full blur-[120px]" />
        <div className="absolute -bottom-60 -left-40 w-[600px] h-[600px] bg-primary/5 rounded-full blur-[120px]" />
      </div>

      <div className="relative max-w-2xl mx-auto px-5 sm:px-8 py-12 sm:py-16">
        {/* ── Page Header ── */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5 }}
          className="mb-12"
        >
          <p className="text-[10px] font-bold uppercase tracking-[0.25em] text-primary mb-3">
            Preferences
          </p>
          <h1 className="font-serif italic text-4xl sm:text-5xl text-on-surface leading-tight">
            Settings
          </h1>
          <p className="text-sm text-on-surface-variant mt-3 leading-relaxed max-w-md">
            Customize your sanctuary experience — notifications, privacy,
            appearance, and account controls.
          </p>
        </motion.div>

        {/* ── Sections Stack ── */}
        <div className="space-y-8">
          {/* ═══════════════ NOTIFICATIONS ═══════════════ */}
          <SettingsSection
            icon={Bell}
            title="Notifications"
            glowColor="bg-primary/10"
            delay={0.05}
          >
            <SettingRow
              icon={Smartphone}
              label="Push Notifications"
              description="Receive alerts on your device"
            >
              <Toggle enabled={pushEnabled} onChange={setPushEnabled} />
            </SettingRow>

            <SettingRow
              icon={Mail}
              label="Email Digest"
              description="Daily summary of your activity"
            >
              <Toggle enabled={emailDigest} onChange={setEmailDigest} />
            </SettingRow>

            <SettingRow
              icon={Heart}
              label="Wellness Reminders"
              description="Meditation & movement prompts"
            >
              <Toggle
                enabled={wellnessReminders}
                onChange={setWellnessReminders}
              />
            </SettingRow>

            <SettingRow
              icon={MessageSquare}
              label="Community Messages"
              description="Updates from your circles"
            >
              <Toggle
                enabled={communityMessages}
                onChange={setCommunityMessages}
              />
            </SettingRow>
          </SettingsSection>

          {/* ═══════════════ PRIVACY ═══════════════ */}
          <SettingsSection
            icon={Shield}
            title="Privacy"
            glowColor="bg-lime-400/10"
            delay={0.12}
          >
            <SettingRow
              icon={User}
              label="Profile Visibility"
              description="Allow others to view your profile"
            >
              <Toggle enabled={profileVisible} onChange={setProfileVisible} />
            </SettingRow>

            <SettingRow
              icon={activityVisible ? Eye : EyeOff}
              label="Activity Sharing"
              description="Show your activity in community feed"
            >
              <Toggle
                enabled={activityVisible}
                onChange={setActivityVisible}
              />
            </SettingRow>

            <SettingRow
              icon={Globe}
              label="Data Collection"
              description="Help improve with usage analytics"
            >
              <Toggle
                enabled={dataCollection}
                onChange={setDataCollection}
              />
            </SettingRow>
          </SettingsSection>

          {/* ═══════════════ APPEARANCE ═══════════════ */}
          <SettingsSection
            icon={Palette}
            title="Appearance"
            glowColor="bg-secondary-fixed/15"
            delay={0.19}
          >
            <SettingRow
              icon={theme === "dark" ? Moon : theme === "light" ? Sun : Monitor}
              label="Theme"
              description="Choose your preferred color scheme"
            >
              <Dropdown
                value={theme}
                options={themeOptions}
                onChange={setTheme}
              />
            </SettingRow>

            <SettingRow
              icon={Palette}
              label="Accent Color"
              description="Personalize your interface color"
            >
              <Dropdown
                value={accentColor}
                options={accentOptions}
                onChange={setAccentColor}
              />
            </SettingRow>

            <SettingRow
              icon={Monitor}
              label="Reduce Motion"
              description="Minimize animations throughout the app"
            >
              <Toggle enabled={reduceMotion} onChange={setReduceMotion} />
            </SettingRow>
          </SettingsSection>

          {/* ═══════════════ ACCOUNT ═══════════════ */}
          <SettingsSection
            icon={User}
            title="Account"
            glowColor="bg-orange-400/10"
            delay={0.26}
          >
            <SettingRow
              icon={Lock}
              label="Two-Factor Authentication"
              description="Add an extra layer of security"
            >
              <Toggle enabled={twoFactor} onChange={setTwoFactor} />
            </SettingRow>

            <SettingRow
              icon={ChevronRight}
              label="Change Password"
              description="Update your account credentials"
            >
              <button className="text-primary text-sm font-semibold hover:underline">
                Update
              </button>
            </SettingRow>

            <SettingRow
              icon={ChevronRight}
              label="Connected Accounts"
              description="Manage linked social accounts"
            >
              <button className="text-primary text-sm font-semibold hover:underline">
                Manage
              </button>
            </SettingRow>

            <div className="pt-4 mt-2 border-t border-surface-container-low">
              <motion.button
                whileHover={{ x: 4 }}
                className="flex items-center gap-3 w-full py-3 px-4 -mx-4 rounded-2xl hover:bg-red-50/60 transition-colors group"
              >
                <div className="w-9 h-9 rounded-xl bg-red-50 flex items-center justify-center">
                  <LogOut className="w-4 h-4 text-red-500" />
                </div>
                <span className="text-sm font-semibold text-red-500">
                  Sign Out
                </span>
              </motion.button>

              <motion.button
                whileHover={{ x: 4 }}
                className="flex items-center gap-3 w-full py-3 px-4 -mx-4 rounded-2xl hover:bg-red-50/60 transition-colors group"
              >
                <div className="w-9 h-9 rounded-xl bg-red-50 flex items-center justify-center">
                  <Trash2 className="w-4 h-4 text-red-500" />
                </div>
                <div>
                  <span className="text-sm font-semibold text-red-500">
                    Delete Account
                  </span>
                  <p className="text-xs text-on-surface-variant mt-0.5">
                    Permanently remove your data
                  </p>
                </div>
              </motion.button>
            </div>
          </SettingsSection>
        </div>

        {/* ── Footer ── */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.4, duration: 0.6 }}
          className="mt-12 text-center"
        >
          <p className="text-[10px] font-bold uppercase tracking-[0.25em] text-on-surface-variant/50">
            Sanctuary v2.4.0
          </p>
        </motion.div>
      </div>
    </div>
  );
}
