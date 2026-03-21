import React from "react";
import { motion } from "framer-motion";
import {
  Clock,
  Flame,
  BarChart3,
  Play,
  Heart,
  TrendingUp,
  Calendar,
  Sparkles,
} from "lucide-react";

const fadeInUp = {
  hidden: { opacity: 0, y: 20 },
  visible: (i: number) => ({
    opacity: 1,
    y: 0,
    transition: { delay: i * 0.1, duration: 0.5, ease: "easeOut" },
  }),
};

const scaleIn = {
  hidden: { opacity: 0, scale: 0.9 },
  visible: {
    opacity: 1,
    scale: 1,
    transition: { duration: 0.4, ease: "easeOut" },
  },
};

const statCardHover = {
  rest: { scale: 1 },
  hover: { scale: 1.03, transition: { duration: 0.2 } },
};

interface StatCardProps {
  icon: React.ReactNode;
  label: string;
  value: string | number;
  unit?: string;
  color: string;
  index: number;
}

const StatCard: React.FC<StatCardProps> = ({
  icon,
  label,
  value,
  unit,
  color,
  index,
}) => (
  <motion.div
    custom={index}
    initial="hidden"
    animate="visible"
    variants={fadeInUp}
    whileHover="hover"
    rest="rest"
    className="bg-white/80 backdrop-blur-sm rounded-2xl p-5 shadow-sm border border-white/50 cursor-default"
  >
    <motion.div variants={statCardHover} className="flex items-start justify-between">
      <div className={`p-2.5 rounded-xl ${color}`}>{icon}</div>
      <TrendingUp className="w-4 h-4 text-emerald-500 mt-1" />
    </motion.div>
    <div className="mt-4">
      <p className="text-3xl font-bold text-gray-900">
        {value}
        {unit && <span className="text-lg font-medium text-gray-400 ml-1">{unit}</span>}
      </p>
      <p className="text-sm text-gray-500 mt-1">{label}</p>
    </div>
  </motion.div>
);

interface WeeklyBarProps {
  day: string;
  minutes: number;
  maxMinutes: number;
  isToday: boolean;
  index: number;
}

const WeeklyBar: React.FC<WeeklyBarProps> = ({
  day,
  minutes,
  maxMinutes,
  isToday,
  index,
}) => {
  const heightPercent = maxMinutes > 0 ? (minutes / maxMinutes) * 100 : 0;

  return (
    <motion.div
      custom={index}
      initial="hidden"
      animate="visible"
      variants={fadeInUp}
      className="flex flex-col items-center gap-2 flex-1"
    >
      <span className="text-xs font-medium text-gray-500">{minutes}m</span>
      <div className="w-full h-32 bg-gray-100 rounded-xl relative overflow-hidden flex items-end justify-center">
        <motion.div
          initial={{ height: 0 }}
          animate={{ height: `${heightPercent}%` }}
          transition={{ delay: index * 0.08, duration: 0.6, ease: "easeOut" }}
          className={`w-3/4 rounded-t-lg ${
            isToday
              ? "bg-gradient-to-t from-violet-600 to-purple-400"
              : "bg-gradient-to-t from-violet-500/60 to-purple-400/60"
          }`}
        />
      </div>
      <span
        className={`text-xs font-semibold ${
          isToday ? "text-violet-600" : "text-gray-400"
        }`}
      >
        {day}
      </span>
    </motion.div>
  );
};

const weeklyData = [
  { day: "Mon", minutes: 15 },
  { day: "Tue", minutes: 20 },
  { day: "Wed", minutes: 10 },
  { day: "Thu", minutes: 25 },
  { day: "Fri", minutes: 18 },
  { day: "Sat", minutes: 30 },
  { day: "Sun", minutes: 22 },
];

const todayStats = {
  totalMinutes: 45,
  sessionsCompleted: 3,
  currentStreak: 12,
};

const recommendedSession = {
  title: "Evening Wind Down",
  description:
    "A gentle guided meditation to release the tension of the day and prepare your mind for restful sleep.",
  duration: 15,
  category: "Sleep",
  image:
    "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=600&q=80",
};

const MeditationDashboard: React.FC = () => {
  const maxMinutes = Math.max(...weeklyData.map((d) => d.minutes));

  return (
    <div className="min-h-screen bg-gradient-to-br from-violet-50 via-white to-purple-50">
      {/* Header */}
      <motion.header
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
        className="px-4 sm:px-6 lg:px-8 pt-8 pb-4 max-w-6xl mx-auto"
      >
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl sm:text-3xl font-bold text-gray-900">
              Good evening
            </h1>
            <p className="text-gray-500 mt-1 text-sm sm:text-base">
              Let's continue your mindfulness journey
            </p>
          </div>
          <motion.div
            whileHover={{ scale: 1.1 }}
            whileTap={{ scale: 0.95 }}
            className="w-10 h-10 rounded-full bg-gradient-to-br from-violet-500 to-purple-600 flex items-center justify-center text-white font-semibold shadow-lg shadow-violet-200 cursor-pointer"
          >
            M
          </motion.div>
        </div>
      </motion.header>

      <main className="px-4 sm:px-6 lg:px-8 pb-12 max-w-6xl mx-auto space-y-8">
        {/* Today's Stats */}
        <section>
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.2 }}
            className="flex items-center gap-2 mb-4"
          >
            <Calendar className="w-4 h-4 text-violet-500" />
            <h2 className="text-lg font-semibold text-gray-800">Today's Progress</h2>
          </motion.div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <StatCard
              index={0}
              icon={<Clock className="w-5 h-5 text-violet-600" />}
              label="Minutes Meditated"
              value={todayStats.totalMinutes}
              unit="min"
              color="bg-violet-100"
            />
            <StatCard
              index={1}
              icon={<BarChart3 className="w-5 h-5 text-emerald-600" />}
              label="Sessions Completed"
              value={todayStats.sessionsCompleted}
              color="bg-emerald-100"
            />
            <StatCard
              index={2}
              icon={<Flame className="w-5 h-5 text-orange-600" />}
              label="Current Streak"
              value={todayStats.currentStreak}
              unit="days"
              color="bg-orange-100"
            />
          </div>
        </section>

        {/* Weekly Progress Chart */}
        <motion.section
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.4, duration: 0.5 }}
          className="bg-white/80 backdrop-blur-sm rounded-2xl p-6 shadow-sm border border-white/50"
        >
          <div className="flex items-center justify-between mb-6">
            <div className="flex items-center gap-2">
              <TrendingUp className="w-4 h-4 text-violet-500" />
              <h2 className="text-lg font-semibold text-gray-800">
                Weekly Progress
              </h2>
            </div>
            <span className="text-sm text-gray-400">Last 7 days</span>
          </div>

          <div className="flex gap-2 sm:gap-4 items-end">
            {weeklyData.map((data, i) => (
              <WeeklyBar
                key={data.day}
                day={data.day}
                minutes={data.minutes}
                maxMinutes={maxMinutes}
                isToday={i === weeklyData.length - 1}
                index={i}
              />
            ))}
          </div>

          <div className="mt-6 pt-4 border-t border-gray-100 flex items-center justify-between text-sm">
            <span className="text-gray-500">
              Total this week:{" "}
              <span className="font-semibold text-gray-800">
                {weeklyData.reduce((sum, d) => sum + d.minutes, 0)} minutes
              </span>
            </span>
            <span className="text-emerald-600 font-medium flex items-center gap-1">
              <TrendingUp className="w-3.5 h-3.5" />
              +15% vs last week
            </span>
          </div>
        </motion.section>

        {/* Recommended Session */}
        <motion.section
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.5, duration: 0.5 }}
        >
          <div className="flex items-center gap-2 mb-4">
            <Sparkles className="w-4 h-4 text-violet-500" />
            <h2 className="text-lg font-semibold text-gray-800">
              Recommended for You
            </h2>
          </div>

          <motion.div
            variants={scaleIn}
            initial="hidden"
            animate="visible"
            whileHover={{ y: -4, transition: { duration: 0.2 } }}
            className="relative bg-white rounded-2xl shadow-sm border border-white/50 overflow-hidden group cursor-pointer"
          >
            <div className="flex flex-col sm:flex-row">
              <div className="sm:w-2/5 h-48 sm:h-auto relative overflow-hidden">
                <img
                  src={recommendedSession.image}
                  alt={recommendedSession.title}
                  className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110"
                />
                <div className="absolute inset-0 bg-gradient-to-t from-black/30 to-transparent sm:bg-gradient-to-r" />
              </div>

              <div className="sm:w-3/5 p-6 flex flex-col justify-between">
                <div>
                  <div className="flex items-center gap-2 mb-3">
                    <span className="px-3 py-1 text-xs font-medium text-violet-700 bg-violet-100 rounded-full">
                      {recommendedSession.category}
                    </span>
                    <span className="flex items-center gap-1 text-xs text-gray-500">
                      <Clock className="w-3 h-3" />
                      {recommendedSession.duration} min
                    </span>
                  </div>
                  <h3 className="text-xl font-bold text-gray-900 mb-2">
                    {recommendedSession.title}
                  </h3>
                  <p className="text-gray-500 text-sm leading-relaxed">
                    {recommendedSession.description}
                  </p>
                </div>

                <div className="flex items-center gap-3 mt-5">
                  <motion.button
                    whileHover={{ scale: 1.05 }}
                    whileTap={{ scale: 0.95 }}
                    className="flex items-center gap-2 px-5 py-2.5 bg-gradient-to-r from-violet-600 to-purple-600 text-white text-sm font-semibold rounded-xl shadow-lg shadow-violet-200 hover:shadow-violet-300 transition-shadow"
                  >
                    <Play className="w-4 h-4" fill="currentColor" />
                    Start Session
                  </motion.button>
                  <motion.button
                    whileHover={{ scale: 1.1 }}
                    whileTap={{ scale: 0.9 }}
                    className="p-2.5 rounded-xl border border-gray-200 text-gray-400 hover:text-rose-500 hover:border-rose-200 transition-colors"
                  >
                    <Heart className="w-5 h-5" />
                  </motion.button>
                </div>
              </div>
            </div>
          </motion.div>
        </motion.section>
      </main>
    </div>
  );
};

export default MeditationDashboard;
