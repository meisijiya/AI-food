import { motion } from "motion/react";
import {
  Dumbbell,
  Clock,
  Flame,
  Award,
  TrendingUp,
  Target,
  Zap,
  Heart,
  Trophy,
  Star,
} from "lucide-react";

const user = {
  name: "Anika Patel",
  avatar: "https://api.dicebear.com/9.x/notionists/svg?seed=Anika",
  membershipTier: "Platinum",
  joinDate: "Jan 2024",
  stats: [
    { label: "Workouts", value: "284", icon: Dumbbell, accent: "text-lime-400" },
    { label: "Hours", value: "412", icon: Clock, accent: "text-cyan-400" },
    { label: "Calories", value: "96.4k", icon: Flame, accent: "text-orange-400" },
  ],
  achievements: [
    {
      title: "Century Club",
      description: "Completed 100 workouts",
      icon: Trophy,
      color: "bg-amber-100 text-amber-600",
      date: "2 days ago",
    },
    {
      title: "Iron Will",
      description: "30-day streak maintained",
      icon: Target,
      color: "bg-lime-100 text-lime-600",
      date: "1 week ago",
    },
    {
      title: "Calorie Crusher",
      description: "Burned 50,000 calories total",
      icon: Zap,
      color: "bg-orange-50 text-orange-500",
      date: "2 weeks ago",
    },
    {
      title: "Early Bird",
      description: "10 workouts before 7 AM",
      icon: Star,
      color: "bg-cyan-100 text-cyan-600",
      date: "3 weeks ago",
    },
    {
      title: "Heart Hero",
      description: "Average HR zone optimized",
      icon: Heart,
      color: "bg-red-50 text-red-500",
      date: "1 month ago",
    },
    {
      title: "Trending Up",
      description: "5% strength gain this month",
      icon: TrendingUp,
      color: "bg-primary/10 text-primary",
      date: "1 month ago",
    },
  ],
};

export default function FitnessProfile() {
  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -20 }}
      transition={{ duration: 0.4 }}
      className="min-h-screen bg-surface text-on-surface font-sans antialiased"
    >
      <div className="max-w-2xl mx-auto px-4 sm:px-6 py-12 sm:py-16 space-y-8">
        {/* Header */}
        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
          className="text-center"
        >
          <h1 className="font-serif italic text-3xl sm:text-4xl text-on-surface">
            Your Sanctuary
          </h1>
          <p className="text-on-surface-variant text-sm mt-2">
            Every rep brings you closer to stillness.
          </p>
        </motion.div>

        {/* Profile Card */}
        <motion.div
          initial={{ opacity: 0, scale: 0.98 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ delay: 0.15 }}
          whileHover={{ y: -4 }}
          className="relative bg-surface-container-lowest rounded-[2rem] sm:rounded-[3rem] p-8 sm:p-10 border border-white shadow-sm vibrant-shadow overflow-hidden group"
        >
          {/* Decorative glow */}
          <div className="absolute top-0 right-0 w-48 h-48 bg-secondary-fixed/10 rounded-full blur-3xl -mr-16 -mt-16 group-hover:bg-secondary-fixed/20 transition-colors duration-700" />
          <div className="absolute bottom-0 left-0 w-32 h-32 bg-primary/5 rounded-full blur-3xl -ml-10 -mb-10" />

          <div className="relative flex flex-col items-center text-center">
            {/* Avatar */}
            <motion.div
              initial={{ scale: 0.8, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              transition={{ delay: 0.25, type: "spring", stiffness: 120 }}
              className="relative"
            >
              <div className="w-28 h-28 sm:w-32 sm:h-32 rounded-full overflow-hidden ring-4 ring-white shadow-lg">
                <img
                  src={user.avatar}
                  alt={user.name}
                  className="w-full h-full object-cover"
                />
              </div>
              {/* Award badge */}
              <motion.div
                initial={{ scale: 0 }}
                animate={{ scale: 1 }}
                transition={{ delay: 0.5, type: "spring", stiffness: 200 }}
                className="absolute -bottom-1 -right-1 w-10 h-10 bg-gradient-to-br from-primary-container to-primary rounded-full flex items-center justify-center shadow-md shadow-primary/20"
              >
                <Award className="w-5 h-5 text-white" strokeWidth={2.5} />
              </motion.div>
            </motion.div>

            {/* Name */}
            <motion.h2
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.35 }}
              className="font-serif text-2xl sm:text-3xl mt-5 text-on-surface"
            >
              {user.name}
            </motion.h2>

            {/* Membership badge */}
            <motion.div
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.4 }}
              className="mt-3 inline-flex items-center gap-2 px-5 py-1.5 rounded-full bg-primary/10 text-primary"
            >
              <Award className="w-3.5 h-3.5" />
              <span className="text-[10px] font-bold uppercase tracking-[0.25em]">
                {user.membershipTier} Member
              </span>
            </motion.div>

            <motion.p
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.45 }}
              className="text-on-surface-variant text-xs mt-2"
            >
              Member since {user.joinDate}
            </motion.p>
          </div>
        </motion.div>

        {/* Stats Row */}
        <div className="grid grid-cols-3 gap-3 sm:gap-4">
          {user.stats.map((stat, i) => (
            <motion.div
              key={stat.label}
              initial={{ opacity: 0, y: 16 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.3 + i * 0.08 }}
              whileHover={{ y: -6 }}
              className="relative bg-inverse-surface text-white rounded-[2rem] sm:rounded-[2.5rem] p-5 sm:p-6 overflow-hidden group"
            >
              {/* Glow */}
              <div className="absolute top-0 right-0 w-20 h-20 bg-lime-500/10 rounded-full blur-2xl -mr-6 -mt-6 group-hover:bg-lime-500/20 transition-colors duration-500" />

              <div className="relative">
                <stat.icon className={`w-5 h-5 ${stat.accent} mb-3`} strokeWidth={2} />
                <div className="font-serif italic text-2xl sm:text-3xl font-bold tracking-tight text-white">
                  {stat.value}
                </div>
                <div className="text-[10px] text-white/40 uppercase tracking-[0.2em] mt-2 font-bold">
                  {stat.label}
                </div>
              </div>
            </motion.div>
          ))}
        </div>

        {/* Achievements Section */}
        <motion.div
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.55 }}
          className="relative bg-surface-container-lowest rounded-[2rem] sm:rounded-[3rem] p-8 sm:p-10 border border-white shadow-sm overflow-hidden"
        >
          {/* Decorative glow */}
          <div className="absolute top-0 left-0 w-40 h-40 bg-primary/5 rounded-full blur-3xl -ml-12 -mt-12" />

          <div className="relative">
            <div className="flex items-center justify-between mb-6">
              <h3 className="font-serif italic text-xl sm:text-2xl text-on-surface">
                Recent Achievements
              </h3>
              <span className="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">
                {user.achievements.length} earned
              </span>
            </div>

            <div className="space-y-3">
              {user.achievements.map((achievement, i) => (
                <motion.div
                  key={achievement.title}
                  initial={{ opacity: 0, x: -12 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ delay: 0.6 + i * 0.06 }}
                  whileHover={{ x: 8 }}
                  className="bg-white/60 backdrop-blur-sm rounded-2xl sm:rounded-3xl p-5 flex items-center gap-4 border border-white shadow-sm hover:shadow-md transition-shadow cursor-pointer group"
                >
                  {/* Icon */}
                  <div
                    className={`w-11 h-11 rounded-2xl flex items-center justify-center shrink-0 ${achievement.color}`}
                  >
                    <achievement.icon className="w-5 h-5" strokeWidth={2} />
                  </div>

                  {/* Text */}
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-semibold text-on-surface truncate">
                      {achievement.title}
                    </p>
                    <p className="text-xs text-on-surface-variant mt-0.5">
                      {achievement.description}
                    </p>
                  </div>

                  {/* Date */}
                  <span className="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant whitespace-nowrap">
                    {achievement.date}
                  </span>
                </motion.div>
              ))}
            </div>
          </div>
        </motion.div>
      </div>
    </motion.div>
  );
}
