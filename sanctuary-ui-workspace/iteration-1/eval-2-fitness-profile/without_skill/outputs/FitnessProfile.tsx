import React from "react";
import { motion } from "framer-motion";
import {
  Dumbbell,
  Clock,
  Flame,
  Trophy,
  Star,
  Zap,
  Target,
  Medal,
  Crown,
  TrendingUp,
  Award,
} from "lucide-react";

const achievements = [
  { id: 1, title: "First Workout", icon: Star, color: "text-yellow-500" },
  { id: 2, title: "10 Workouts", icon: Trophy, color: "text-amber-600" },
  { id: 3, title: "50 Workouts", icon: Medal, color: "text-orange-500" },
  { id: 4, title: "100 Workouts", icon: Crown, color: "text-purple-500" },
  { id: 5, title: "Speed Demon", icon: Zap, color: "text-blue-500" },
  { id: 6, title: "Goal Crusher", icon: Target, color: "text-green-500" },
  { id: 7, title: "Week Streak", icon: TrendingUp, color: "text-red-500" },
  { id: 8, title: "Elite Member", icon: Award, color: "text-indigo-500" },
];

const stats = [
  { label: "Workouts Completed", value: 127, icon: Dumbbell },
  { label: "Total Hours", value: 89, icon: Clock },
  { label: "Calories Burned", value: 45230, icon: Flame },
];

export default function FitnessProfile() {
  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900 text-white p-4 sm:p-6 lg:p-8">
      <div className="max-w-4xl mx-auto space-y-6">
        {/* Profile Header */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5 }}
          className="bg-slate-800/60 backdrop-blur-sm rounded-2xl p-6 sm:p-8 border border-slate-700/50"
        >
          <div className="flex flex-col sm:flex-row items-center gap-6">
            {/* Avatar */}
            <motion.div
              initial={{ scale: 0.8 }}
              animate={{ scale: 1 }}
              transition={{ duration: 0.4, delay: 0.2 }}
              className="relative"
            >
              <div className="w-24 h-24 sm:w-28 sm:h-28 rounded-full bg-gradient-to-br from-cyan-400 to-blue-600 p-1">
                <div className="w-full h-full rounded-full bg-slate-900 flex items-center justify-center text-4xl sm:text-5xl font-bold text-cyan-400">
                  AJ
                </div>
              </div>
              <motion.div
                initial={{ scale: 0 }}
                animate={{ scale: 1 }}
                transition={{ delay: 0.5, type: "spring", stiffness: 200 }}
                className="absolute -bottom-1 -right-1 bg-green-500 w-6 h-6 rounded-full border-4 border-slate-800"
              />
            </motion.div>

            {/* Name & Badge */}
            <div className="text-center sm:text-left flex-1">
              <h1 className="text-2xl sm:text-3xl font-bold">Alex Johnson</h1>
              <p className="text-slate-400 mt-1">Member since Jan 2024</p>
              <motion.div
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: 0.3, duration: 0.4 }}
                className="mt-3 inline-flex items-center gap-2 bg-gradient-to-r from-amber-500/20 to-yellow-500/20 border border-amber-500/30 rounded-full px-4 py-1.5"
              >
                <Crown className="w-4 h-4 text-amber-400" />
                <span className="text-sm font-semibold text-amber-300">
                  Gold Member
                </span>
              </motion.div>
            </div>
          </div>
        </motion.div>

        {/* Stats Grid */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.15 }}
          className="grid grid-cols-1 sm:grid-cols-3 gap-4"
        >
          {stats.map((stat, i) => (
            <motion.div
              key={stat.label}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.4, delay: 0.2 + i * 0.1 }}
              whileHover={{ scale: 1.03 }}
              className="bg-slate-800/60 backdrop-blur-sm rounded-xl p-5 border border-slate-700/50 text-center"
            >
              <stat.icon className="w-8 h-8 mx-auto text-cyan-400 mb-2" />
              <p className="text-2xl sm:text-3xl font-bold">
                {stat.value.toLocaleString()}
              </p>
              <p className="text-slate-400 text-sm mt-1">{stat.label}</p>
            </motion.div>
          ))}
        </motion.div>

        {/* Achievements */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.3 }}
          className="bg-slate-800/60 backdrop-blur-sm rounded-2xl p-6 sm:p-8 border border-slate-700/50"
        >
          <h2 className="text-xl font-bold mb-4">Recent Achievements</h2>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
            {achievements.map((ach, i) => (
              <motion.div
                key={ach.id}
                initial={{ opacity: 0, scale: 0.8 }}
                animate={{ opacity: 1, scale: 1 }}
                transition={{ duration: 0.3, delay: 0.35 + i * 0.08 }}
                whileHover={{ scale: 1.08, y: -4 }}
                className="flex flex-col items-center gap-2 bg-slate-700/40 rounded-xl p-4 cursor-default"
              >
                <div className="w-12 h-12 rounded-full bg-slate-600/50 flex items-center justify-center">
                  <ach.icon className={`w-6 h-6 ${ach.color}`} />
                </div>
                <span className="text-xs text-slate-300 text-center font-medium leading-tight">
                  {ach.title}
                </span>
              </motion.div>
            ))}
          </div>
        </motion.div>
      </div>
    </div>
  );
}
