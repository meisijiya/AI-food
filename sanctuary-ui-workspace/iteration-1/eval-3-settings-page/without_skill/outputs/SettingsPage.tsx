/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { useState } from "react";
import { motion, AnimatePresence } from "motion/react";
import {
  Bell,
  BellOff,
  Shield,
  Eye,
  EyeOff,
  Palette,
  User,
  Moon,
  Sun,
  Monitor,
  ChevronRight,
  ChevronDown,
  LogOut,
  Trash2,
  Lock,
  Globe,
  Mail,
  Smartphone,
  Volume2,
  Vibrate,
  Clock,
  Sparkles,
  Type,
  Languages,
  HelpCircle,
  Info,
  Camera,
  KeyRound,
  Download,
  Loader2,
} from "lucide-react";

interface ToggleProps {
  enabled: boolean;
  onChange: (value: boolean) => void;
  label: string;
  description?: string;
  icon: React.ReactNode;
  color: string;
  bg: string;
}

const Toggle = ({ enabled, onChange, label, description, icon, color, bg }: ToggleProps) => (
  <motion.div
    whileHover={{ scale: 1.01 }}
    className="flex items-center justify-between py-4 px-2 group"
  >
    <div className="flex items-center gap-4 flex-1 min-w-0">
      <div className={`p-2.5 rounded-2xl shadow-sm shrink-0 transition-all duration-300 group-hover:scale-110 ${bg} ${color}`}>
        {icon}
      </div>
      <div className="min-w-0">
        <span className="font-bold text-on-surface block text-sm sm:text-base">{label}</span>
        {description && (
          <span className="text-xs text-on-surface-variant mt-0.5 block truncate">{description}</span>
        )}
      </div>
    </div>
    <button
      onClick={() => onChange(!enabled)}
      className={`relative w-12 h-7 rounded-full transition-all duration-300 shrink-0 ${
        enabled ? "bg-primary shadow-lg shadow-primary/20" : "bg-slate-200"
      }`}
    >
      <motion.div
        layout
        transition={{ type: "spring", stiffness: 500, damping: 30 }}
        className={`absolute top-0.5 w-6 h-6 bg-white rounded-full shadow-md ${
          enabled ? "left-[22px]" : "left-0.5"
        }`}
      />
    </button>
  </motion.div>
);

interface DropdownOption {
  value: string;
  label: string;
  icon?: React.ReactNode;
}

interface DropdownProps {
  value: string;
  onChange: (value: string) => void;
  options: DropdownOption[];
  label: string;
  description?: string;
  icon: React.ReactNode;
  color: string;
  bg: string;
}

const Dropdown = ({ value, onChange, options, label, description, icon, color, bg }: DropdownProps) => {
  const [isOpen, setIsOpen] = useState(false);
  const selected = options.find((o) => o.value === value);

  return (
    <div className="py-4 px-2">
      <div className="flex items-center gap-4 mb-3">
        <div className={`p-2.5 rounded-2xl shadow-sm ${bg} ${color}`}>{icon}</div>
        <div>
          <span className="font-bold text-on-surface block text-sm sm:text-base">{label}</span>
          {description && <span className="text-xs text-on-surface-variant mt-0.5 block">{description}</span>}
        </div>
      </div>
      <div className="relative ml-0 sm:ml-14">
        <button
          onClick={() => setIsOpen(!isOpen)}
          className="w-full flex items-center justify-between px-5 py-3.5 bg-white/80 rounded-2xl border border-black/5 hover:border-primary/20 transition-all text-sm font-medium text-on-surface"
        >
          <div className="flex items-center gap-3">
            {selected?.icon}
            <span>{selected?.label}</span>
          </div>
          <ChevronDown
            size={16}
            className={`text-on-surface-variant transition-transform duration-300 ${isOpen ? "rotate-180" : ""}`}
          />
        </button>
        <AnimatePresence>
          {isOpen && (
            <motion.div
              initial={{ opacity: 0, y: -8, scale: 0.96 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, y: -8, scale: 0.96 }}
              transition={{ duration: 0.2 }}
              className="absolute top-full left-0 right-0 mt-2 bg-white rounded-2xl border border-black/5 shadow-xl shadow-black/5 overflow-hidden z-20"
            >
              {options.map((option) => (
                <button
                  key={option.value}
                  onClick={() => {
                    onChange(option.value);
                    setIsOpen(false);
                  }}
                  className={`w-full flex items-center gap-3 px-5 py-3.5 text-sm hover:bg-primary/5 transition-colors ${
                    option.value === value ? "bg-primary/5 text-primary font-bold" : "text-on-surface"
                  }`}
                >
                  {option.icon}
                  <span>{option.label}</span>
                  {option.value === value && (
                    <motion.div
                      layoutId="dropdown-check"
                      className="ml-auto w-2 h-2 bg-primary rounded-full"
                    />
                  )}
                </button>
              ))}
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </div>
  );
};

interface SectionCardProps {
  icon: React.ReactNode;
  title: string;
  subtitle: string;
  iconBg: string;
  iconColor: string;
  children: React.ReactNode;
  delay?: number;
}

const SectionCard = ({ icon, title, subtitle, iconBg, iconColor, children, delay = 0 }: SectionCardProps) => (
  <motion.div
    initial={{ opacity: 0, y: 24 }}
    animate={{ opacity: 1, y: 0 }}
    transition={{ delay, duration: 0.5, ease: [0.22, 1, 0.36, 1] }}
    className="bg-surface-container-lowest rounded-[2.5rem] sm:rounded-[3rem] border border-white shadow-sm overflow-hidden vibrant-shadow"
  >
    <div className="p-6 sm:p-8 border-b border-black/5">
      <div className="flex items-center gap-4">
        <motion.div
          whileHover={{ rotate: [0, -10, 10, 0] }}
          transition={{ duration: 0.5 }}
          className={`p-3.5 rounded-2xl shadow-sm ${iconBg} ${iconColor}`}
        >
          {icon}
        </motion.div>
        <div>
          <h2 className="font-serif text-2xl sm:text-3xl italic text-on-surface">{title}</h2>
          <p className="text-xs text-on-surface-variant mt-0.5">{subtitle}</p>
        </div>
      </div>
    </div>
    <div className="divide-y divide-black/5 px-4 sm:px-6">{children}</div>
  </motion.div>
);

export default function SettingsPage() {
  const [notifications, setNotifications] = useState({
    pushEnabled: true,
    emailEnabled: true,
    ritualReminders: true,
    communityUpdates: false,
    aiInsights: true,
    soundEnabled: true,
    vibrationEnabled: true,
    quietHours: true,
  });

  const [privacy, setPrivacy] = useState({
    profileVisible: true,
    activityVisible: false,
    dataCollection: true,
    analyticsEnabled: false,
    twoFactorEnabled: true,
  });

  const [appearance, setAppearance] = useState({
    theme: "system",
    fontSize: "medium",
    language: "en",
  });

  const themeOptions: DropdownOption[] = [
    { value: "light", label: "Light", icon: <Sun size={16} className="text-amber-500" /> },
    { value: "dark", label: "Dark", icon: <Moon size={16} className="text-indigo-500" /> },
    { value: "system", label: "System", icon: <Monitor size={16} className="text-slate-500" /> },
  ];

  const fontSizeOptions: DropdownOption[] = [
    { value: "small", label: "Small" },
    { value: "medium", label: "Medium" },
    { value: "large", label: "Large" },
    { value: "extra-large", label: "Extra Large" },
  ];

  const languageOptions: DropdownOption[] = [
    { value: "en", label: "English" },
    { value: "es", label: "Español" },
    { value: "fr", label: "Français" },
    { value: "de", label: "Deutsch" },
    { value: "ja", label: "日本語" },
  ];

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="space-y-8 sm:space-y-12 pb-24 max-w-4xl mx-auto"
    >
      {/* Header */}
      <motion.section
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.6 }}
        className="space-y-3 pt-4"
      >
        <h1 className="text-4xl sm:text-5xl lg:text-6xl font-serif italic text-on-surface tracking-tight">
          Sanctuary Settings
        </h1>
        <p className="text-on-surface-variant text-base sm:text-lg max-w-2xl leading-relaxed">
          Customize your sanctuary experience. Every detail, tuned to your rhythm.
        </p>
      </motion.section>

      {/* Notifications Section */}
      <SectionCard
        icon={<Bell size={26} />}
        title="Notifications"
        subtitle="How your sanctuary speaks to you"
        iconBg="bg-orange-50"
        iconColor="text-orange-500"
        delay={0.1}
      >
        <Toggle
          enabled={notifications.pushEnabled}
          onChange={(v) => setNotifications((s) => ({ ...s, pushEnabled: v }))}
          label="Push Notifications"
          description="Receive alerts on your device"
          icon={<Smartphone size={18} />}
          color="text-blue-500"
          bg="bg-blue-50"
        />
        <Toggle
          enabled={notifications.emailEnabled}
          onChange={(v) => setNotifications((s) => ({ ...s, emailEnabled: v }))}
          label="Email Digest"
          description="Weekly summary to your inbox"
          icon={<Mail size={18} />}
          color="text-green-500"
          bg="bg-green-50"
        />
        <Toggle
          enabled={notifications.ritualReminders}
          onChange={(v) => setNotifications((s) => ({ ...s, ritualReminders: v }))}
          label="Ritual Reminders"
          description="Gentle nudges for daily habits"
          icon={<Clock size={18} />}
          color="text-purple-500"
          bg="bg-purple-50"
        />
        <Toggle
          enabled={notifications.communityUpdates}
          onChange={(v) => setNotifications((s) => ({ ...s, communityUpdates: v }))}
          label="Community Updates"
          description="Messages from your sanctuary circle"
          icon={<Globe size={18} />}
          color="text-cyan-500"
          bg="bg-cyan-50"
        />
        <Toggle
          enabled={notifications.aiInsights}
          onChange={(v) => setNotifications((s) => ({ ...s, aiInsights: v }))}
          label="AI Insights"
          description="Personalized wellness suggestions"
          icon={<Sparkles size={18} />}
          color="text-amber-500"
          bg="bg-amber-50"
        />
        <Toggle
          enabled={notifications.soundEnabled}
          onChange={(v) => setNotifications((s) => ({ ...s, soundEnabled: v }))}
          label="Sound"
          description="Notification sounds and tones"
          icon={<Volume2 size={18} />}
          color="text-slate-500"
          bg="bg-slate-50"
        />
        <Toggle
          enabled={notifications.vibrationEnabled}
          onChange={(v) => setNotifications((s) => ({ ...s, vibrationEnabled: v }))}
          label="Haptic Feedback"
          description="Subtle vibrations on interaction"
          icon={<Vibrate size={18} />}
          color="text-pink-500"
          bg="bg-pink-50"
        />
        <Toggle
          enabled={notifications.quietHours}
          onChange={(v) => setNotifications((s) => ({ ...s, quietHours: v }))}
          label="Quiet Hours"
          description="Silence notifications from 10 PM to 7 AM"
          icon={<Moon size={18} />}
          color="text-indigo-500"
          bg="bg-indigo-50"
        />
      </SectionCard>

      {/* Privacy Section */}
      <SectionCard
        icon={<Shield size={26} />}
        title="Privacy"
        subtitle="Your data, your sovereignty"
        iconBg="bg-green-50"
        iconColor="text-green-500"
        delay={0.2}
      >
        <Toggle
          enabled={privacy.profileVisible}
          onChange={(v) => setPrivacy((s) => ({ ...s, profileVisible: v }))}
          label="Profile Visibility"
          description="Allow others to discover your profile"
          icon={privacy.profileVisible ? <Eye size={18} /> : <EyeOff size={18} />}
          color="text-blue-500"
          bg="bg-blue-50"
        />
        <Toggle
          enabled={privacy.activityVisible}
          onChange={(v) => setPrivacy((s) => ({ ...s, activityVisible: v }))}
          label="Activity Sharing"
          description="Share wellness milestones with community"
          icon={<Globe size={18} />}
          color="text-cyan-500"
          bg="bg-cyan-50"
        />
        <Toggle
          enabled={privacy.dataCollection}
          onChange={(v) => setPrivacy((s) => ({ ...s, dataCollection: v }))}
          label="Personalized Experience"
          description="Allow data to improve your recommendations"
          icon={<Sparkles size={18} />}
          color="text-purple-500"
          bg="bg-purple-50"
        />
        <Toggle
          enabled={privacy.analyticsEnabled}
          onChange={(v) => setPrivacy((s) => ({ ...s, analyticsEnabled: v }))}
          label="Usage Analytics"
          description="Anonymous data to improve the app"
          icon={<Info size={18} />}
          color="text-slate-500"
          bg="bg-slate-50"
        />
        <Toggle
          enabled={privacy.twoFactorEnabled}
          onChange={(v) => setPrivacy((s) => ({ ...s, twoFactorEnabled: v }))}
          label="Two-Factor Authentication"
          description="Extra security for your sanctuary"
          icon={<KeyRound size={18} />}
          color="text-red-500"
          bg="bg-red-50"
        />
        <div className="py-4 px-2">
          <button className="w-full flex items-center justify-between py-3 px-1 hover:bg-primary/5 rounded-2xl transition-all group">
            <div className="flex items-center gap-4">
              <div className="p-2.5 rounded-2xl shadow-sm bg-amber-50 text-amber-500">
                <Download size={18} />
              </div>
              <div>
                <span className="font-bold text-on-surface block text-sm sm:text-base">Download My Data</span>
                <span className="text-xs text-on-surface-variant">Export a copy of your sanctuary data</span>
              </div>
            </div>
            <ChevronRight size={18} className="text-on-surface-variant group-hover:translate-x-1 transition-transform" />
          </button>
        </div>
      </SectionCard>

      {/* Appearance Section */}
      <SectionCard
        icon={<Palette size={26} />}
        title="Appearance"
        subtitle="The aesthetics of your sanctuary"
        iconBg="bg-violet-50"
        iconColor="text-violet-500"
        delay={0.3}
      >
        <Dropdown
          value={appearance.theme}
          onChange={(v) => setAppearance((s) => ({ ...s, theme: v }))}
          options={themeOptions}
          label="Theme"
          description="Choose your visual sanctuary"
          icon={<Palette size={18} />}
          color="text-violet-500"
          bg="bg-violet-50"
        />
        <Dropdown
          value={appearance.fontSize}
          onChange={(v) => setAppearance((s) => ({ ...s, fontSize: v }))}
          options={fontSizeOptions}
          label="Font Size"
          description="Adjust text readability"
          icon={<Type size={18} />}
          color="text-teal-500"
          bg="bg-teal-50"
        />
        <Dropdown
          value={appearance.language}
          onChange={(v) => setAppearance((s) => ({ ...s, language: v }))}
          options={languageOptions}
          label="Language"
          description="Your preferred language"
          icon={<Languages size={18} />}
          color="text-blue-500"
          bg="bg-blue-50"
        />
      </SectionCard>

      {/* Account Management Section */}
      <SectionCard
        icon={<User size={26} />}
        title="Account"
        subtitle="Your sanctuary identity"
        iconBg="bg-slate-100"
        iconColor="text-slate-600"
        delay={0.4}
      >
        <div className="py-4 px-2 space-y-1">
          {/* Profile Photo */}
          <motion.button
            whileHover={{ x: 6 }}
            className="w-full flex items-center justify-between py-3.5 px-1 hover:bg-primary/5 rounded-2xl transition-all group"
          >
            <div className="flex items-center gap-4">
              <div className="p-2.5 rounded-2xl shadow-sm bg-blue-50 text-blue-500">
                <Camera size={18} />
              </div>
              <div>
                <span className="font-bold text-on-surface block text-sm sm:text-base">Profile Photo</span>
                <span className="text-xs text-on-surface-variant">Update your sanctuary avatar</span>
              </div>
            </div>
            <ChevronRight size={18} className="text-on-surface-variant group-hover:translate-x-1 transition-transform" />
          </motion.button>

          {/* Change Password */}
          <motion.button
            whileHover={{ x: 6 }}
            className="w-full flex items-center justify-between py-3.5 px-1 hover:bg-primary/5 rounded-2xl transition-all group"
          >
            <div className="flex items-center gap-4">
              <div className="p-2.5 rounded-2xl shadow-sm bg-amber-50 text-amber-500">
                <Lock size={18} />
              </div>
              <div>
                <span className="font-bold text-on-surface block text-sm sm:text-base">Change Password</span>
                <span className="text-xs text-on-surface-variant">Update your sanctuary credentials</span>
              </div>
            </div>
            <ChevronRight size={18} className="text-on-surface-variant group-hover:translate-x-1 transition-transform" />
          </motion.button>

          {/* Help & Support */}
          <motion.button
            whileHover={{ x: 6 }}
            className="w-full flex items-center justify-between py-3.5 px-1 hover:bg-primary/5 rounded-2xl transition-all group"
          >
            <div className="flex items-center gap-4">
              <div className="p-2.5 rounded-2xl shadow-sm bg-green-50 text-green-500">
                <HelpCircle size={18} />
              </div>
              <div>
                <span className="font-bold text-on-surface block text-sm sm:text-base">Help & Support</span>
                <span className="text-xs text-on-surface-variant">FAQs, guides, and contact</span>
              </div>
            </div>
            <ChevronRight size={18} className="text-on-surface-variant group-hover:translate-x-1 transition-transform" />
          </motion.button>

          {/* About */}
          <motion.button
            whileHover={{ x: 6 }}
            className="w-full flex items-center justify-between py-3.5 px-1 hover:bg-primary/5 rounded-2xl transition-all group"
          >
            <div className="flex items-center gap-4">
              <div className="p-2.5 rounded-2xl shadow-sm bg-slate-50 text-slate-500">
                <Info size={18} />
              </div>
              <div>
                <span className="font-bold text-on-surface block text-sm sm:text-base">About Sanctuary</span>
                <span className="text-xs text-on-surface-variant">Version, licenses, and credits</span>
              </div>
            </div>
            <ChevronRight size={18} className="text-on-surface-variant group-hover:translate-x-1 transition-transform" />
          </motion.button>
        </div>

        {/* Danger Zone */}
        <div className="p-4 sm:p-6 bg-red-50/50 border-t border-red-100">
          <div className="space-y-4">
            <div className="flex items-center gap-3">
              <div className="p-2 rounded-xl bg-red-100 text-red-500">
                <Loader2 size={16} />
              </div>
              <span className="text-xs font-bold uppercase tracking-[0.2em] text-red-400">Danger Zone</span>
            </div>
            <div className="flex flex-col sm:flex-row gap-3">
              <motion.button
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
                className="flex-1 py-3.5 px-6 rounded-2xl bg-white border border-red-200 text-red-600 font-bold text-sm flex items-center justify-center gap-2 hover:bg-red-50 transition-all"
              >
                <LogOut size={16} />
                Sign Out
              </motion.button>
              <motion.button
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
                className="flex-1 py-3.5 px-6 rounded-2xl bg-red-600 text-white font-bold text-sm flex items-center justify-center gap-2 hover:bg-red-700 transition-all shadow-lg shadow-red-600/20"
              >
                <Trash2 size={16} />
                Delete Account
              </motion.button>
            </div>
            <p className="text-[10px] text-red-400 leading-relaxed">
              Deleting your account is permanent and will remove all your sanctuary data, rituals, and community connections.
            </p>
          </div>
        </div>
      </SectionCard>

      {/* Footer */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 0.6 }}
        className="text-center pt-4 pb-8"
      >
        <p className="text-[10px] text-on-surface-variant/40 uppercase tracking-[0.2em] font-bold">
          Sanctuary v2.4.0 &middot; Secure & Private
        </p>
      </motion.div>
    </motion.div>
  );
}
