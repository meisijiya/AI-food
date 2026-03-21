import { motion } from "motion/react";
import {
  Flame,
  Play,
  Clock,
  CheckCircle2,
  Sparkles,
  ChevronRight,
  Bell,
  Search,
} from "lucide-react";

const weeklyData = [
  { day: "Mon", minutes: 25 },
  { day: "Tue", minutes: 15 },
  { day: "Wed", minutes: 30 },
  { day: "Thu", minutes: 0 },
  { day: "Fri", minutes: 20 },
  { day: "Sat", minutes: 35 },
  { day: "Sun", minutes: 18 },
];

const maxMinutes = Math.max(...weeklyData.map((d) => d.minutes));

export default function MeditationDashboard() {
  return (
    <div className="min-h-screen bg-surface">
      {/* Header */}
      <motion.header
        initial={{ opacity: 0, y: -10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4 }}
        className="sticky top-0 z-40 bg-surface/80 backdrop-blur-xl border-b border-black/5"
      >
        <div className="max-w-6xl mx-auto px-6 py-4 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-full bg-gradient-to-br from-primary-container to-primary flex items-center justify-center">
              <Sparkles className="w-4 h-4 text-white" />
            </div>
            <span className="font-serif italic text-lg text-on-surface">
              Sanctuary
            </span>
          </div>
          <div className="flex items-center gap-2">
            <button className="p-2 hover:bg-black/5 rounded-full transition-colors text-on-surface-variant">
              <Search className="w-5 h-5" />
            </button>
            <button className="p-2 hover:bg-black/5 rounded-full transition-colors text-on-surface-variant relative">
              <Bell className="w-5 h-5" />
              <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-primary rounded-full" />
            </button>
            <div className="w-9 h-9 rounded-full bg-primary/10 ml-2 flex items-center justify-center text-primary font-bold text-sm">
              M
            </div>
          </div>
        </div>
      </motion.header>

      <main className="max-w-6xl mx-auto px-6 py-10 space-y-10">
        {/* Hero Greeting */}
        <motion.section
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5 }}
        >
          <p className="text-[10px] font-bold uppercase tracking-[0.2em] text-on-surface-variant mb-3">
            Good morning
          </p>
          <h1 className="text-4xl sm:text-5xl lg:text-6xl font-serif italic text-on-surface leading-tight">
            Find your calm
          </h1>
          <p className="mt-4 text-on-surface-variant text-sm leading-relaxed max-w-lg">
            You&apos;re building a beautiful practice. Keep nurturing your mind
            with stillness and presence.
          </p>
        </motion.section>

        {/* Stats Row */}
        <motion.section
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.1 }}
          className="grid grid-cols-1 sm:grid-cols-3 gap-6"
        >
          {/* Total Minutes */}
          <motion.div
            whileHover={{ y: -8 }}
            className="relative overflow-hidden bg-surface-container-lowest rounded-[2rem] sm:rounded-[3rem] p-8 border border-white shadow-sm vibrant-shadow group"
          >
            <div className="absolute top-0 right-0 w-40 h-40 bg-primary/5 rounded-full blur-3xl -mr-16 -mt-16 group-hover:bg-primary/10 transition-colors duration-700" />
            <div className="relative">
              <div className="flex items-center gap-3 mb-5">
                <div className="w-10 h-10 rounded-2xl bg-primary/10 flex items-center justify-center">
                  <Clock className="w-5 h-5 text-primary" />
                </div>
                <span className="text-[10px] font-bold uppercase tracking-[0.2em] text-on-surface-variant">
                  Total minutes
                </span>
              </div>
              <div className="text-5xl font-serif italic text-on-surface tracking-tight">
                143
              </div>
              <p className="mt-2 text-xs text-on-surface-variant">
                <span className="text-lime-400 font-semibold">+12%</span> from
                last week
              </p>
            </div>
          </motion.div>

          {/* Sessions Completed */}
          <motion.div
            whileHover={{ y: -8 }}
            className="relative overflow-hidden bg-surface-container-lowest rounded-[2rem] sm:rounded-[3rem] p-8 border border-white shadow-sm group"
          >
            <div className="absolute top-0 right-0 w-40 h-40 bg-secondary-fixed/10 rounded-full blur-3xl -mr-16 -mt-16 group-hover:bg-secondary-fixed/20 transition-colors duration-700" />
            <div className="relative">
              <div className="flex items-center gap-3 mb-5">
                <div className="w-10 h-10 rounded-2xl bg-cyan-100 flex items-center justify-center">
                  <CheckCircle2 className="w-5 h-5 text-cyan-600" />
                </div>
                <span className="text-[10px] font-bold uppercase tracking-[0.2em] text-on-surface-variant">
                  Sessions
                </span>
              </div>
              <div className="text-5xl font-serif italic text-on-surface tracking-tight">
                12
              </div>
              <p className="mt-2 text-xs text-on-surface-variant">
                This week completed
              </p>
            </div>
          </motion.div>

          {/* Current Streak */}
          <motion.div
            whileHover={{ y: -8 }}
            className="relative overflow-hidden bg-inverse-surface text-white rounded-[2rem] sm:rounded-[3rem] p-8 group"
          >
            <div className="absolute top-0 right-0 w-40 h-40 bg-lime-500/10 rounded-full blur-3xl -mr-16 -mt-16" />
            <div className="relative">
              <div className="flex items-center gap-3 mb-5">
                <div className="w-10 h-10 rounded-2xl bg-lime-400/10 flex items-center justify-center">
                  <Flame className="w-5 h-5 text-lime-400" />
                </div>
                <span className="text-[10px] font-bold uppercase tracking-[0.2em] text-white/40">
                  Streak
                </span>
              </div>
              <div className="text-5xl font-serif italic text-lime-400 tracking-tight">
                7
              </div>
              <p className="mt-2 text-xs text-white/50">
                days in a row — don&apos;t stop now!
              </p>
            </div>
          </motion.div>
        </motion.section>

        {/* Weekly Progress + Recommended Session */}
        <section className="grid grid-cols-1 lg:grid-cols-5 gap-6">
          {/* Weekly Progress Chart */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, delay: 0.2 }}
            whileHover={{ y: -4 }}
            className="lg:col-span-3 relative overflow-hidden bg-surface-container-lowest rounded-[2rem] sm:rounded-[3rem] p-8 sm:p-10 border border-white shadow-sm group"
          >
            <div className="absolute bottom-0 left-0 w-56 h-56 bg-primary/5 rounded-full blur-3xl -ml-20 -mb-20 group-hover:bg-primary/8 transition-colors duration-700" />
            <div className="relative">
              <div className="flex items-center justify-between mb-8">
                <div>
                  <span className="text-[10px] font-bold uppercase tracking-[0.2em] text-on-surface-variant">
                    Weekly progress
                  </span>
                  <h2 className="mt-2 text-2xl font-serif italic text-on-surface">
                    This week&apos;s flow
                  </h2>
                </div>
                <button className="px-5 py-2 rounded-full border border-black/10 text-[10px] font-bold uppercase tracking-widest hover:bg-black/5 transition-colors text-on-surface-variant">
                  This week
                </button>
              </div>

              {/* Bar Chart */}
              <div className="flex items-end justify-between gap-3 sm:gap-5 h-48 sm:h-56">
                {weeklyData.map((item, i) => {
                  const heightPercent =
                    maxMinutes > 0 ? (item.minutes / maxMinutes) * 100 : 0;
                  const isToday = i === weeklyData.length - 1;
                  return (
                    <div
                      key={item.day}
                      className="flex flex-col items-center flex-1 h-full justify-end"
                    >
                      <span className="text-[10px] font-bold text-on-surface-variant mb-3">
                        {item.minutes > 0 ? `${item.minutes}m` : "—"}
                      </span>
                      <motion.div
                        initial={{ height: 0 }}
                        animate={{
                          height: `${Math.max(heightPercent, 4)}%`,
                        }}
                        transition={{
                          delay: i * 0.06,
                          type: "spring",
                          stiffness: 100,
                          damping: 15,
                        }}
                        className={`w-full max-w-[2.5rem] rounded-2xl ${
                          isToday
                            ? "bg-gradient-to-t from-primary-container to-primary"
                            : item.minutes > 0
                              ? "bg-primary/15"
                              : "bg-on-surface/5"
                        }`}
                      />
                      <span
                        className={`mt-3 text-[10px] font-bold uppercase tracking-widest ${
                          isToday
                            ? "text-primary"
                            : "text-on-surface-variant"
                        }`}
                      >
                        {item.day}
                      </span>
                    </div>
                  );
                })}
              </div>

              {/* Summary */}
              <div className="mt-8 flex items-center gap-6">
                <div>
                  <p className="text-3xl font-serif italic text-on-surface">
                    143
                  </p>
                  <p className="text-[10px] font-bold uppercase tracking-[0.2em] text-on-surface-variant mt-1">
                    Total minutes
                  </p>
                </div>
                <div className="w-px h-10 bg-black/10" />
                <div>
                  <p className="text-3xl font-serif italic text-on-surface">
                    20
                  </p>
                  <p className="text-[10px] font-bold uppercase tracking-[0.2em] text-on-surface-variant mt-1">
                    Avg / day
                  </p>
                </div>
              </div>
            </div>
          </motion.div>

          {/* Recommended Meditation */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, delay: 0.3 }}
            whileHover={{ y: -8 }}
            className="lg:col-span-2 relative overflow-hidden bg-surface-container-lowest rounded-[2rem] sm:rounded-[3rem] border border-white shadow-sm group"
          >
            <div className="absolute top-0 right-0 w-48 h-48 bg-secondary-fixed/10 rounded-full blur-3xl -mr-16 -mt-16 group-hover:bg-secondary-fixed/20 transition-colors duration-700" />

            {/* Image */}
            <div className="relative h-48 sm:h-52 overflow-hidden rounded-t-[2rem] sm:rounded-t-[3rem]">
              <img
                src="https://images.unsplash.com/photo-1506126613408-eca07ce68773?w=600&q=80"
                alt="Serene morning meditation scene"
                className="w-full h-full object-cover"
              />
              <div className="absolute inset-0 bg-gradient-to-t from-black/40 to-transparent" />
              <div className="absolute bottom-4 left-6">
                <span className="px-3 py-1 rounded-full bg-white/20 backdrop-blur-md text-white text-[10px] font-bold uppercase tracking-[0.2em]">
                  Recommended
                </span>
              </div>
            </div>

            {/* Content */}
            <div className="relative p-8">
              <span className="text-[10px] font-bold uppercase tracking-[0.2em] text-on-surface-variant">
                Morning ritual
              </span>
              <h3 className="mt-3 text-xl sm:text-2xl font-serif italic text-on-surface leading-snug">
                Gentle Awakening Meditation
              </h3>
              <p className="mt-3 text-sm text-on-surface-variant leading-relaxed">
                Start your day with soft breath awareness and body scanning.
                Perfect for cultivating presence.
              </p>

              <div className="mt-5 flex items-center gap-4">
                <div className="flex items-center gap-1.5 text-on-surface-variant">
                  <Clock className="w-3.5 h-3.5" />
                  <span className="text-xs font-medium">15 min</span>
                </div>
                <div className="flex items-center gap-1.5 text-on-surface-variant">
                  <Sparkles className="w-3.5 h-3.5" />
                  <span className="text-xs font-medium">Beginner</span>
                </div>
              </div>

              <motion.button
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
                className="mt-6 w-full py-4 rounded-full bg-gradient-to-r from-primary-container to-primary text-white font-bold text-sm tracking-widest shadow-lg shadow-primary/20 hover:shadow-primary/40 transition-shadow uppercase flex items-center justify-center gap-2"
              >
                <Play className="w-4 h-4" />
                Begin session
              </motion.button>
            </div>
          </motion.div>
        </section>

        {/* Recent Sessions */}
        <motion.section
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.4 }}
        >
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-3xl font-serif italic text-on-surface">
              Recent sessions
            </h2>
            <button className="flex items-center gap-1 text-[10px] font-bold uppercase tracking-widest text-primary hover:text-primary-container transition-colors">
              View all
              <ChevronRight className="w-3.5 h-3.5" />
            </button>
          </div>

          <div className="space-y-4">
            {[
              {
                title: "Evening Wind Down",
                duration: "20 min",
                time: "Today, 9:30 PM",
                color: "bg-cyan-100",
                iconColor: "text-cyan-600",
              },
              {
                title: "Focus & Clarity",
                duration: "15 min",
                time: "Today, 7:00 AM",
                color: "bg-primary/10",
                iconColor: "text-primary",
              },
              {
                title: "Gratitude Practice",
                duration: "10 min",
                time: "Yesterday, 8:15 PM",
                color: "bg-lime-100",
                iconColor: "text-lime-400",
              },
            ].map((session, i) => (
              <motion.div
                key={i}
                whileHover={{ x: 10 }}
                className="bg-white/60 backdrop-blur-sm rounded-3xl p-6 flex items-center justify-between border border-white shadow-sm hover:shadow-md transition-all cursor-pointer group"
              >
                <div className="flex items-center gap-4">
                  <div
                    className={`w-11 h-11 rounded-2xl ${session.color} flex items-center justify-center`}
                  >
                    <Sparkles className={`w-5 h-5 ${session.iconColor}`} />
                  </div>
                  <div>
                    <p className="font-medium text-on-surface text-sm">
                      {session.title}
                    </p>
                    <p className="text-xs text-on-surface-variant mt-0.5">
                      {session.time}
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <span className="text-sm font-medium text-on-surface-variant">
                    {session.duration}
                  </span>
                  <ChevronRight className="w-4 h-4 text-on-surface-variant group-hover:text-primary transition-colors" />
                </div>
              </motion.div>
            ))}
          </div>
        </motion.section>
      </main>

      {/* Bottom Navigation */}
      <motion.nav
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, delay: 0.5 }}
        className="fixed bottom-8 left-1/2 -translate-x-1/2 w-[90%] max-w-md bg-inverse-surface/90 backdrop-blur-2xl rounded-full px-8 py-4 z-50 flex justify-between items-center shadow-2xl shadow-primary/10 border border-white/10"
      >
        {[
          { label: "Home", active: true },
          { label: "Meditate", active: false },
          { label: "Breathe", active: false },
          { label: "Profile", active: false },
        ].map((item) => (
          <button
            key={item.label}
            className={`relative flex flex-col items-center gap-1 text-[10px] font-bold uppercase tracking-widest transition-colors ${
              item.active
                ? "text-cyan-400 scale-110"
                : "text-white/40 hover:text-white"
            }`}
          >
            {item.label}
            {item.active && (
              <motion.span
                layoutId="nav-indicator"
                className="absolute -bottom-2 w-1 h-1 bg-cyan-400 rounded-full"
              />
            )}
          </button>
        ))}
      </motion.nav>
    </div>
  );
}
