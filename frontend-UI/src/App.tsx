/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { useState, useRef, useEffect } from "react";
import { motion, AnimatePresence } from "motion/react";
import { 
  Bell, 
  Utensils, 
  Activity, 
  Moon, 
  Droplets, 
  Timer, 
  Flame, 
  LayoutDashboard, 
  User,
  ChevronRight,
  MessageSquare,
  Send,
  MoreHorizontal,
  Smile,
  Heart,
  Zap,
  TrendingUp,
  Clock,
  Settings,
  Award,
  Shield,
  LogOut,
  ChevronLeft,
  Users,
  UserPlus,
  ChevronDown,
  Search
} from "lucide-react";

import { GoogleGenAI } from "@google/genai";

const ai = new GoogleGenAI({ apiKey: process.env.GEMINI_API_KEY });

const VitalityGauge = ({ value = 84 }: { value?: number }) => {
  const radius = 90;
  const circumference = 2 * Math.PI * radius;
  const offset = circumference - (value / 100) * circumference;

  return (
    <div className="relative w-56 h-56 flex items-center justify-center">
      <svg className="w-full h-full transform -rotate-90">
        <circle
          cx="112"
          cy="112"
          r={radius}
          fill="transparent"
          stroke="currentColor"
          strokeWidth="2"
          className="text-slate-200"
        />
        <motion.circle
          cx="112"
          cy="112"
          r={radius}
          fill="transparent"
          stroke="url(#vitalityGradient)"
          strokeWidth="10"
          strokeDasharray={circumference}
          initial={{ strokeDashoffset: circumference }}
          animate={{ strokeDashoffset: offset }}
          transition={{ duration: 1.5, ease: "easeOut" }}
          strokeLinecap="round"
        />
        <defs>
          <linearGradient id="vitalityGradient" x1="0%" y1="0%" x2="100%" y2="0%">
            <stop offset="0%" stopColor="#8ce1f3" />
            <stop offset="100%" stopColor="#0059b6" />
          </linearGradient>
        </defs>
      </svg>
      <div className="absolute inset-4 rounded-full glass-card flex flex-col items-center justify-center shadow-inner border border-white/40">
        <span className="text-6xl font-serif italic text-primary leading-none">{value}</span>
        <span className="text-[10px] uppercase tracking-[0.2em] text-on-surface-variant font-bold mt-2">Vitality</span>
      </div>
    </div>
  );
};

const MetabolicFlux = () => (
  <div className="space-y-3">
    <div className="flex justify-between items-end">
      <span className="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Metabolic Flux</span>
      <span className="text-[10px] font-bold text-primary uppercase tracking-widest">Optimal</span>
    </div>
    <div className="h-3 w-full bg-slate-100 rounded-full flex overflow-hidden">
      <div className="h-full w-[20%] bg-orange-500" />
      <div className="h-full w-[35%] bg-lime-500" />
      <div className="h-full w-[25%] bg-cyan-400" />
      <div className="h-full w-[20%] bg-slate-200" />
    </div>
    <div className="flex justify-between px-1">
      <div className="w-1 h-1 rounded-full bg-orange-500" />
      <div className="w-1 h-1 rounded-full bg-lime-500" />
      <div className="w-1 h-1 rounded-full bg-cyan-400" />
      <div className="w-1 h-1 rounded-full bg-slate-300" />
    </div>
  </div>
);

const ChatRoom = ({ isNavVisible }: { isNavVisible: boolean }) => {
  const [messages, setMessages] = useState([
    { id: 1, user: "Elena", text: "Has anyone tried the citrus quinoa recipe yet?", time: "10:24 AM", avatar: "https://picsum.photos/seed/elena/100/100", isMe: false, role: 'user' },
    { id: 2, user: "Marcus", text: "Yes! It's incredible. I added a bit of extra lime.", time: "10:26 AM", avatar: "https://picsum.photos/seed/marcus/100/100", isMe: false, role: 'user' },
    { id: 3, user: "Sanctuary AI", text: "Citrus is excellent for post-workout recovery! The Vitamin C helps with collagen synthesis for your joints.", time: "10:28 AM", avatar: "https://picsum.photos/seed/bot/100/100", isMe: false, role: 'ai' },
    { id: 4, user: "Me", text: "I'm planning to make it for dinner tonight. Can't wait!", time: "10:30 AM", avatar: "https://picsum.photos/seed/olivia/100/100", isMe: true, role: 'user' },
  ]);
  const [inputValue, setInputValue] = useState("");
  const [isTyping, setIsTyping] = useState(false);
  const [isSidebarOpen, setIsSidebarOpen] = useState(true);
  const [activeChat, setActiveChat] = useState({ id: 'community', name: 'Community Sanctuary', type: 'group' });
  const [expandedMenus, setExpandedMenus] = useState<string[]>(['groups', 'private']);
  const scrollRef = useRef<HTMLDivElement>(null);

  const toggleMenu = (menu: string) => {
    setExpandedMenus(prev => 
      prev.includes(menu) ? prev.filter(m => m !== menu) : [...prev, menu]
    );
  };

  const chatGroups = [
    { id: 'community', name: 'Community Sanctuary', members: 12 },
    { id: 'rituals', name: 'Morning Rituals', members: 8 },
    { id: 'nutrition', name: 'Mindful Eating', members: 15 },
  ];

  const privateChats = [
    { id: 'elena', name: 'Elena', status: 'online', avatar: "https://picsum.photos/seed/elena/100/100" },
    { id: 'marcus', name: 'Marcus', status: 'offline', avatar: "https://picsum.photos/seed/marcus/100/100" },
    { id: 'sanctuary_ai', name: 'Sanctuary AI', status: 'online', avatar: "https://picsum.photos/seed/bot/100/100" },
  ];

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTo({
        top: scrollRef.current.scrollHeight,
        behavior: 'smooth'
      });
    }
  }, [messages, isTyping]);

  const getAIResponse = async (userText: string) => {
    setIsTyping(true);
    try {
      const response = await ai.models.generateContent({
        model: "gemini-3-flash-preview",
        contents: userText,
        config: {
          systemInstruction: "You are 'Sanctuary AI', a supportive and knowledgeable wellness guide for the Sanctuary Wellness app. Your tone is calm, editorial, and encouraging. Keep responses concise (1-2 sentences). Focus on nutrition, sleep, mindfulness, and movement. If the user asks for a recipe or health tip, provide a high-quality, science-backed one.",
        }
      });

      const aiText = response.text || "I'm here to support your journey. How are you feeling today?";
      
      const aiMessage = {
        id: Date.now(),
        user: "Sanctuary AI",
        text: aiText,
        time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        avatar: "https://picsum.photos/seed/bot/100/100",
        isMe: false,
        role: 'ai'
      };

      setMessages(prev => [...prev, aiMessage]);
    } catch (error) {
      console.error("AI Error:", error);
    } finally {
      setIsTyping(false);
    }
  };

  const handleSend = () => {
    if (!inputValue.trim()) return;
    const userText = inputValue;
    const newMessage = {
      id: Date.now(),
      user: "Me",
      text: userText,
      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      avatar: "https://picsum.photos/seed/olivia/100/100",
      isMe: true,
      role: 'user'
    };
    setMessages(prev => [...prev, newMessage]);
    setInputValue("");

    // Trigger AI response if it looks like a question or mention
    if (userText.toLowerCase().includes("ai") || userText.includes("?") || userText.length > 10) {
      setTimeout(() => getAIResponse(userText), 1000);
    }
  };

  return (
    <div className="flex h-full w-full bg-surface-container-lowest/90 backdrop-blur-3xl overflow-hidden transition-all duration-500 relative">
      {/* Sidebar */}
      <motion.aside
        initial={false}
        animate={{ 
          width: isSidebarOpen ? 280 : 0,
          opacity: isSidebarOpen ? 1 : 0,
          x: isSidebarOpen ? 0 : -20
        }}
        className="h-full bg-white/40 backdrop-blur-md border-r border-black/5 flex flex-col z-20 overflow-hidden shrink-0"
      >
        <div className="p-6 border-b border-black/5 flex justify-between items-center shrink-0">
          <h3 className="font-serif text-xl italic text-primary">Sanctuary Chat</h3>
          <button onClick={() => setIsSidebarOpen(false)} className="p-1 hover:bg-black/5 rounded-lg text-on-surface-variant transition-colors">
            <ChevronLeft size={20} />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto p-4 space-y-6 no-scrollbar">
          {/* Search */}
          <div className="relative">
            <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant/40" />
            <input 
              type="text" 
              placeholder="Search chats..." 
              className="w-full bg-white/50 border border-black/5 rounded-xl py-2 pl-10 pr-4 text-sm focus:ring-2 ring-primary/10 transition-all outline-none placeholder:text-on-surface-variant/30"
            />
          </div>

          {/* Group Chats */}
          <div className="space-y-2">
            <button 
              onClick={() => toggleMenu('groups')}
              className="w-full flex items-center justify-between text-[10px] font-bold uppercase tracking-widest text-on-surface-variant/60 px-2 hover:text-primary transition-colors"
            >
              <span>Group Sanctuaries</span>
              <ChevronDown size={14} className={`transition-transform duration-300 ${expandedMenus.includes('groups') ? '' : '-rotate-90'}`} />
            </button>
            <AnimatePresence initial={false}>
              {expandedMenus.includes('groups') && (
                <motion.div
                  initial={{ height: 0, opacity: 0 }}
                  animate={{ height: 'auto', opacity: 1 }}
                  exit={{ height: 0, opacity: 0 }}
                  className="space-y-1 overflow-hidden"
                >
                  {chatGroups.map(group => (
                    <button
                      key={group.id}
                      onClick={() => setActiveChat({ ...group, type: 'group' })}
                      className={`w-full flex items-center gap-3 p-3 rounded-2xl transition-all ${activeChat.id === group.id ? 'bg-primary text-white shadow-lg shadow-primary/20' : 'hover:bg-white/60 text-on-surface'}`}
                    >
                      <div className={`p-2 rounded-xl ${activeChat.id === group.id ? 'bg-white/20' : 'bg-primary/5 text-primary'}`}>
                        <Users size={18} />
                      </div>
                      <div className="text-left">
                        <p className="text-sm font-bold truncate">{group.name}</p>
                        <p className={`text-[10px] ${activeChat.id === group.id ? 'text-white/60' : 'text-on-surface-variant'}`}>{group.members} active</p>
                      </div>
                    </button>
                  ))}
                </motion.div>
              )}
            </AnimatePresence>
          </div>

          {/* Private Chats */}
          <div className="space-y-2">
            <button 
              onClick={() => toggleMenu('private')}
              className="w-full flex items-center justify-between text-[10px] font-bold uppercase tracking-widest text-on-surface-variant/60 px-2 hover:text-primary transition-colors"
            >
              <span>Private Rituals</span>
              <ChevronDown size={14} className={`transition-transform duration-300 ${expandedMenus.includes('private') ? '' : '-rotate-90'}`} />
            </button>
            <AnimatePresence initial={false}>
              {expandedMenus.includes('private') && (
                <motion.div
                  initial={{ height: 0, opacity: 0 }}
                  animate={{ height: 'auto', opacity: 1 }}
                  exit={{ height: 0, opacity: 0 }}
                  className="space-y-1 overflow-hidden"
                >
                  {privateChats.map(chat => (
                    <button
                      key={chat.id}
                      onClick={() => setActiveChat({ ...chat, type: 'private' })}
                      className={`w-full flex items-center gap-3 p-3 rounded-2xl transition-all ${activeChat.id === chat.id ? 'bg-primary text-white shadow-lg shadow-primary/20' : 'hover:bg-white/60 text-on-surface'}`}
                    >
                      <div className="relative">
                        <img src={chat.avatar} className="w-10 h-10 rounded-xl object-cover border-2 border-white shadow-sm" alt={chat.name} />
                        {chat.status === 'online' && (
                          <div className="absolute -bottom-1 -right-1 w-3 h-3 bg-lime-500 border-2 border-white rounded-full" />
                        )}
                      </div>
                      <div className="text-left">
                        <p className="text-sm font-bold truncate">{chat.name}</p>
                        <p className={`text-[10px] ${activeChat.id === chat.id ? 'text-white/60' : 'text-on-surface-variant'}`}>{chat.status}</p>
                      </div>
                    </button>
                  ))}
                </motion.div>
              )}
            </AnimatePresence>
          </div>
        </div>

        <div className="p-4 border-t border-black/5">
          <button className="w-full flex items-center justify-center gap-2 p-3 bg-primary/5 text-primary rounded-2xl font-bold text-xs uppercase tracking-widest hover:bg-primary/10 transition-all">
            <UserPlus size={16} />
            Invite Member
          </button>
        </div>
      </motion.aside>

      {/* Main Chat Area */}
      <div className="flex-1 flex flex-col relative overflow-hidden">
        {/* Sidebar Toggle (when closed) */}
        {!isSidebarOpen && (
          <button 
            onClick={() => setIsSidebarOpen(true)}
            className="absolute top-6 left-6 z-30 p-2 bg-white/80 backdrop-blur-md rounded-xl border border-black/5 shadow-sm text-on-surface-variant hover:text-primary transition-all"
          >
            <ChevronRight size={20} />
          </button>
        )}

        {/* Chat Content Header (Simplified) */}
        <div className="px-8 py-6 flex justify-between items-center z-10 bg-white/20 backdrop-blur-sm border-b border-black/5">
          <div className={isSidebarOpen ? "" : "pl-12"}>
            <h3 className="font-serif text-2xl italic text-on-surface">{activeChat.name}</h3>
            <div className="flex items-center gap-2 mt-1">
              <span className={`w-1.5 h-1.5 rounded-full ${activeChat.id === 'marcus' ? 'bg-slate-300' : 'bg-lime-500 animate-pulse'}`} />
              <p className="text-[10px] text-primary font-bold uppercase tracking-widest">
                {activeChat.type === 'group' ? 'Group Sanctuary' : 'Private Ritual'}
              </p>
            </div>
          </div>
          <div className="flex items-center gap-3">
             <button className="p-2 hover:bg-black/5 rounded-full transition-colors text-on-surface-variant">
              <Bell size={20} />
            </button>
            <button className="p-2 hover:bg-black/5 rounded-full transition-colors text-on-surface-variant">
              <MoreHorizontal size={20} />
            </button>
          </div>
        </div>

        {/* Messages Area */}
        <div 
          ref={scrollRef}
          className="flex-1 overflow-y-auto p-6 sm:p-8 space-y-6 no-scrollbar bg-gradient-to-b from-transparent to-surface-container-low/20"
        >
        {messages.map((msg) => (
          <motion.div 
            key={msg.id}
            initial={{ opacity: 0, y: 10, scale: 0.95 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            className={`flex items-end gap-3 ${msg.isMe ? "flex-row-reverse" : "flex-row"}`}
          >
            <div className="relative group">
              <img 
                src={msg.avatar} 
                className={`w-9 h-9 rounded-full object-cover border-2 border-white shadow-sm transition-transform group-hover:scale-110 ${msg.role === 'ai' ? "ring-2 ring-primary/20" : ""}`} 
                alt={msg.user}
              />
              {msg.role === 'ai' && (
                <div className="absolute -top-1 -right-1 bg-primary text-white p-0.5 rounded-full shadow-sm">
                  <Flame size={10} />
                </div>
              )}
            </div>
            <div className={`max-w-[80%] space-y-1 ${msg.isMe ? "items-end" : "items-start"}`}>
              {!msg.isMe && (
                <div className="flex items-center gap-2 ml-2">
                  <span className="text-[10px] font-bold text-on-surface-variant uppercase tracking-widest">{msg.user}</span>
                  {msg.role === 'ai' && <span className="text-[8px] bg-primary/10 text-primary px-1.5 py-0.5 rounded-full font-bold uppercase tracking-tighter">Guide</span>}
                </div>
              )}
              <div className={`px-5 py-3 rounded-2xl text-sm leading-relaxed shadow-sm transition-all hover:shadow-md ${
                msg.isMe 
                  ? "bg-primary text-white rounded-br-none shadow-primary/10" 
                  : msg.role === 'ai'
                    ? "bg-gradient-to-br from-white to-primary/5 text-on-surface rounded-bl-none border border-primary/10 italic font-serif text-base"
                    : "bg-white text-on-surface rounded-bl-none border border-black/5"
              }`}>
                {msg.text}
              </div>
              <span className={`text-[9px] text-on-surface-variant/60 block font-medium ${msg.isMe ? "text-right mr-1" : "text-left ml-1"}`}>{msg.time}</span>
            </div>
          </motion.div>
        ))}

        {isTyping && (
          <motion.div 
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="flex items-center gap-3"
          >
            <div className="w-9 h-9 rounded-full bg-primary/10 flex items-center justify-center border-2 border-white shadow-sm">
              <Flame size={16} className="text-primary animate-pulse" />
            </div>
            <div className="bg-white/60 backdrop-blur-sm px-4 py-2 rounded-2xl rounded-bl-none border border-black/5 flex gap-1">
              <span className="w-1.5 h-1.5 bg-primary/40 rounded-full animate-bounce" style={{ animationDelay: '0ms' }} />
              <span className="w-1.5 h-1.5 bg-primary/40 rounded-full animate-bounce" style={{ animationDelay: '150ms' }} />
              <span className="w-1.5 h-1.5 bg-primary/40 rounded-full animate-bounce" style={{ animationDelay: '300ms' }} />
            </div>
          </motion.div>
        )}
      </div>

      {/* Input Area */}
      <div className={`p-6 bg-white/60 backdrop-blur-xl border-t border-black/5 transition-all duration-500 ${isNavVisible ? 'pb-24' : 'pb-6'}`}>
        <div className="flex items-center gap-3 bg-white rounded-full px-5 py-2 border border-black/5 shadow-inner focus-within:ring-2 ring-primary/10 transition-all">
          <button className="text-on-surface-variant hover:text-primary transition-colors p-1">
            <Smile size={20} />
          </button>
          <input 
            type="text" 
            placeholder="Share your ritual or ask Sanctuary AI..." 
            className="flex-1 bg-transparent border-none focus:ring-0 text-sm py-2 placeholder:text-on-surface-variant/40"
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            onKeyPress={(e) => e.key === 'Enter' && handleSend()}
          />
          <button 
            onClick={handleSend}
            disabled={!inputValue.trim()}
            className={`p-2.5 rounded-full transition-all ${inputValue.trim() ? "bg-primary text-white scale-105 shadow-lg shadow-primary/20 hover:scale-110 active:scale-95" : "text-on-surface-variant/20"}`}
          >
            <Send size={18} />
          </button>
        </div>
        <p className="text-center text-[9px] text-on-surface-variant/40 mt-3 uppercase tracking-[0.2em] font-bold">
          End-to-end encrypted sanctuary
        </p>
      </div>
    </div>
  </div>
);
};

const ActivityView = () => (
  <motion.div
    initial={{ opacity: 0, y: 20 }}
    animate={{ opacity: 1, y: 0 }}
    exit={{ opacity: 0, y: -20 }}
    className="space-y-8 pb-24"
  >
    <section className="space-y-2">
      <h2 className="text-4xl sm:text-5xl font-serif italic text-on-surface">Activity Trends</h2>
      <p className="text-on-surface-variant text-lg">Your physical performance is peaking this week.</p>
    </section>

    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
      <div className="bg-surface-container-lowest rounded-[2.5rem] p-8 border border-white shadow-sm space-y-8 relative overflow-hidden group">
        <div className="absolute top-0 right-0 w-32 h-32 bg-red-500/5 rounded-full blur-2xl -mr-10 -mt-10" />
        <div className="flex justify-between items-center relative z-10">
          <div className="flex items-center gap-4">
            <div className="p-3 bg-red-50 rounded-2xl shadow-sm">
              <Heart className="text-red-500" size={24} />
            </div>
            <div>
              <h3 className="font-serif text-xl">Heart Rate</h3>
              <p className="text-[10px] text-on-surface-variant font-bold uppercase tracking-widest">Real-time</p>
            </div>
          </div>
          <div className="text-right">
            <span className="text-3xl font-serif italic text-primary">72</span>
            <span className="text-xs text-on-surface-variant ml-1 font-bold">BPM</span>
          </div>
        </div>
        <div className="h-32 flex items-end gap-1.5 relative z-10">
          {[40, 55, 45, 70, 60, 85, 75, 90, 65, 50, 45, 60, 75, 80].map((h, i) => (
            <motion.div 
              key={i}
              initial={{ height: 0 }}
              animate={{ height: `${h}%` }}
              transition={{ delay: i * 0.05, type: "spring", stiffness: 100 }}
              className="flex-1 bg-gradient-to-t from-red-100 to-red-300 rounded-t-full hover:to-red-500 transition-colors cursor-crosshair" 
            />
          ))}
        </div>
      </div>

      <div className="bg-surface-container-lowest rounded-[2.5rem] p-8 border border-white shadow-sm space-y-8 relative overflow-hidden">
        <div className="absolute top-0 right-0 w-32 h-32 bg-yellow-500/5 rounded-full blur-2xl -mr-10 -mt-10" />
        <div className="flex justify-between items-center relative z-10">
          <div className="flex items-center gap-4">
            <div className="p-3 bg-yellow-50 rounded-2xl shadow-sm">
              <Zap className="text-yellow-600" size={24} />
            </div>
            <div>
              <h3 className="font-serif text-xl">Metabolic Flux</h3>
              <p className="text-[10px] text-on-surface-variant font-bold uppercase tracking-widest">Efficiency</p>
            </div>
          </div>
          <span className="px-3 py-1 bg-lime-100 text-lime-700 text-[10px] font-bold rounded-full uppercase tracking-widest">Optimal</span>
        </div>
        <div className="space-y-6 relative z-10">
          <div className="flex justify-between text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">
            <span>Morning</span>
            <span>Afternoon</span>
            <span>Evening</span>
          </div>
          <div className="h-6 w-full bg-slate-100 rounded-full overflow-hidden flex p-1">
            <motion.div 
              initial={{ width: 0 }}
              animate={{ width: "30%" }}
              className="h-full bg-orange-400 rounded-l-full" 
            />
            <motion.div 
              initial={{ width: 0 }}
              animate={{ width: "50%" }}
              className="h-full bg-yellow-500" 
            />
            <motion.div 
              initial={{ width: 0 }}
              animate={{ width: "20%" }}
              className="h-full bg-yellow-200 rounded-r-full" 
            />
          </div>
          <p className="text-xs text-on-surface-variant leading-relaxed">
            Your energy is most stable between <span className="text-primary font-bold">10 AM and 2 PM</span>. Perfect for deep work.
          </p>
        </div>
      </div>
    </div>

    <div className="bg-inverse-surface text-white rounded-[3rem] p-10 space-y-10 relative overflow-hidden group">
      <div className="absolute top-0 right-0 w-96 h-96 bg-lime-400/10 rounded-full blur-[100px] -mr-32 -mt-32 group-hover:bg-lime-400/20 transition-colors duration-1000" />
      <div className="flex items-center justify-between relative z-10">
        <div className="flex items-center gap-5">
          <div className="p-4 bg-white/10 rounded-3xl backdrop-blur-md">
            <TrendingUp className="text-lime-400" size={32} />
          </div>
          <h3 className="text-4xl font-serif italic">Weekly Progress</h3>
        </div>
        <div className="text-right">
          <p className="text-[10px] text-white/40 font-bold uppercase tracking-widest">Status</p>
          <p className="text-lime-400 font-bold">On Track</p>
        </div>
      </div>
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-10 relative z-10">
        {[
          { label: 'Workouts', value: '5', unit: 'sessions', trend: '+1' },
          { label: 'Active Time', value: '320', unit: 'mins', trend: '+12%' },
          { label: 'Calories', value: '12.4k', unit: 'kcal', trend: '-5%' },
          { label: 'Distance', value: '42.8', unit: 'km', trend: '+2.4' },
        ].map((stat, i) => (
          <div key={i} className="space-y-2">
            <div className="flex items-center justify-between">
              <p className="text-[10px] text-white/40 font-bold uppercase tracking-widest">{stat.label}</p>
              <span className={`text-[8px] font-bold px-1.5 py-0.5 rounded-full ${stat.trend.startsWith('+') ? 'bg-lime-500/20 text-lime-400' : 'bg-red-500/20 text-red-400'}`}>
                {stat.trend}
              </span>
            </div>
            <p className="text-4xl font-serif italic text-lime-400">{stat.value}</p>
            <p className="text-[10px] text-white/60 uppercase tracking-tighter">{stat.unit}</p>
          </div>
        ))}
      </div>
    </div>

    <section className="space-y-8">
      <div className="flex justify-between items-end px-4">
        <div>
          <h3 className="text-3xl font-serif italic">Recent Rituals</h3>
          <p className="text-on-surface-variant text-sm mt-1">Your journey over the last 72 hours.</p>
        </div>
        <button className="px-6 py-2 rounded-full border border-black/10 text-[10px] font-bold uppercase tracking-widest hover:bg-black/5 transition-colors">View Archive</button>
      </div>
      <div className="grid grid-cols-1 gap-4">
        {[
          { title: 'Morning Flow Yoga', time: 'Today, 6:30 AM', duration: '45 min', type: 'Mindfulness', intensity: 'Low' },
          { title: 'High Intensity Interval', time: 'Yesterday, 5:15 PM', duration: '30 min', type: 'Strength', intensity: 'High' },
          { title: 'Evening Sanctuary Walk', time: '2 days ago', duration: '20 min', type: 'Recovery', intensity: 'Low' },
        ].map((ritual, i) => (
          <motion.div 
            key={i} 
            whileHover={{ x: 10 }}
            className="bg-white/60 backdrop-blur-sm rounded-3xl p-6 flex items-center justify-between border border-white shadow-sm hover:shadow-md transition-all cursor-pointer group"
          >
            <div className="flex items-center gap-6">
              <div className="w-14 h-14 rounded-2xl bg-primary/5 flex items-center justify-center group-hover:bg-primary/10 transition-colors shadow-inner">
                <Clock className="text-primary" size={24} />
              </div>
              <div>
                <h4 className="text-xl font-serif italic text-on-surface">{ritual.title}</h4>
                <p className="text-xs text-on-surface-variant font-medium mt-1">{ritual.time} • {ritual.duration}</p>
              </div>
            </div>
            <div className="flex items-center gap-4">
              <div className="text-right hidden sm:block">
                <p className="text-[10px] text-on-surface-variant font-bold uppercase tracking-widest">Intensity</p>
                <p className="text-xs font-bold text-primary">{ritual.intensity}</p>
              </div>
              <span className="text-[10px] font-bold uppercase tracking-widest bg-primary/5 px-4 py-2 rounded-full text-primary border border-primary/10">{ritual.type}</span>
            </div>
          </motion.div>
        ))}
      </div>
    </section>
  </motion.div>
);

const ProfileView = () => (
  <motion.div
    initial={{ opacity: 0, y: 20 }}
    animate={{ opacity: 1, y: 0 }}
    exit={{ opacity: 0, y: -20 }}
    className="space-y-12 pb-24"
  >
    <div className="flex flex-col items-center text-center space-y-6 pt-8">
      <div className="relative">
        <motion.div 
          animate={{ rotate: [3, -3, 3] }}
          transition={{ duration: 6, repeat: Infinity, ease: "easeInOut" }}
          className="w-40 h-40 rounded-[3rem] overflow-hidden border-8 border-white shadow-2xl relative z-10"
        >
          <img 
            src="https://picsum.photos/seed/olivia/400/400" 
            alt="Olivia" 
            className="w-full h-full object-cover"
            referrerPolicy="no-referrer"
          />
        </motion.div>
        <div className="absolute -bottom-4 -right-4 bg-primary text-white p-4 rounded-[2rem] shadow-xl rotate-12 z-20 border-4 border-white">
          <Award size={28} />
        </div>
        <div className="absolute inset-0 bg-primary/10 rounded-[3rem] blur-3xl -z-10 scale-110" />
      </div>
      <div className="space-y-2">
        <h2 className="text-5xl font-serif italic text-on-surface tracking-tight">Olivia Sterling</h2>
        <div className="flex items-center justify-center gap-3">
          <span className="px-3 py-1 bg-primary/10 text-primary text-[10px] font-bold rounded-full uppercase tracking-widest">Sanctuary Elite</span>
          <span className="text-on-surface-variant text-sm font-medium">Member since 2024</span>
        </div>
      </div>
    </div>

    <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
      {[
        { label: 'Rituals', value: '124', icon: <Activity size={16} /> },
        { label: 'Streak', value: '12', icon: <Flame size={16} /> },
        { label: 'Badges', value: '8', icon: <Award size={16} /> },
      ].map((stat, i) => (
        <motion.div 
          key={i} 
          whileHover={{ y: -5 }}
          className="bg-white/80 backdrop-blur-md rounded-[2.5rem] p-8 text-center border border-white shadow-sm hover:shadow-md transition-all"
        >
          <div className="flex justify-center text-primary/40 mb-2">{stat.icon}</div>
          <p className="text-4xl font-serif italic text-primary leading-none">{stat.value}</p>
          <p className="text-[10px] font-bold uppercase tracking-[0.25em] text-on-surface-variant mt-3">{stat.label}</p>
        </motion.div>
      ))}
    </div>

    <div className="space-y-6">
      <div className="flex justify-between items-center px-6">
        <h3 className="text-xs font-bold uppercase tracking-[0.3em] text-on-surface-variant">Sanctuary Preferences</h3>
        <Settings size={16} className="text-on-surface-variant" />
      </div>
      <div className="bg-white/60 backdrop-blur-xl rounded-[3rem] overflow-hidden border border-white shadow-sm">
        {[
          { icon: <User size={20} />, label: 'Personal Sanctuary', sub: 'Bio, goals, and metrics', color: 'text-blue-500', bg: 'bg-blue-50' },
          { icon: <Bell size={20} />, label: 'Ritual Reminders', sub: 'Daily flow notifications', color: 'text-orange-500', bg: 'bg-orange-50' },
          { icon: <Shield size={20} />, label: 'Data Sovereignty', sub: 'Privacy and encryption', color: 'text-green-500', bg: 'bg-green-50' },
          { icon: <Settings size={20} />, label: 'App Aesthetics', sub: 'Themes and typography', color: 'text-slate-500', bg: 'bg-slate-50' },
        ].map((item, i) => (
          <button key={i} className="w-full px-10 py-6 flex items-center justify-between hover:bg-white/40 transition-colors border-b border-black/5 last:border-none group">
            <div className="flex items-center gap-6">
              <div className={`p-3 rounded-2xl shadow-sm group-hover:scale-110 transition-transform ${item.bg} ${item.color}`}>
                {item.icon}
              </div>
              <div className="text-left">
                <span className="font-bold text-on-surface block text-lg">{item.label}</span>
                <span className="text-xs text-on-surface-variant">{item.sub}</span>
              </div>
            </div>
            <ChevronRight size={20} className="text-on-surface-variant group-hover:translate-x-1 transition-transform" />
          </button>
        ))}
      </div>
    </div>

    <div className="pt-4">
      <button className="w-full py-6 rounded-[2.5rem] bg-red-50 text-red-600 font-bold uppercase tracking-[0.25em] flex items-center justify-center gap-4 hover:bg-red-100 transition-all active:scale-95 shadow-sm">
        <LogOut size={22} />
        Relinquish Session
      </button>
      <p className="text-center text-[10px] text-on-surface-variant/40 mt-6 uppercase tracking-[0.2em] font-bold">
        Sanctuary v2.4.0 • Secure & Private
      </p>
    </div>
  </motion.div>
);

export default function App() {
  const [view, setView] = useState<'dashboard' | 'chat' | 'activity' | 'profile'>('dashboard');
  const [isNavVisible, setIsNavVisible] = useState(true);

  return (
    <div className={`min-h-screen selection:bg-primary/10 transition-all duration-500 ${isNavVisible ? 'pb-32' : 'pb-8'}`}>
      {/* Top App Bar */}
      <header className="fixed top-0 w-full z-50 bg-white/60 backdrop-blur-xl flex justify-between items-center px-6 h-16 border-b border-black/5">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-full overflow-hidden border-2 border-white shadow-sm cursor-pointer hover:opacity-90 transition-opacity">
            <img 
              src="https://picsum.photos/seed/olivia/200/200" 
              alt="Olivia" 
              className="w-full h-full object-cover"
              referrerPolicy="no-referrer"
            />
          </div>
        </div>
        <h1 className="text-2xl font-serif italic text-primary tracking-tight">Sanctuary</h1>
        <button className="text-primary p-2 hover:bg-primary/5 rounded-full transition-colors">
          <Bell size={22} />
        </button>
      </header>

      <main className={`pt-16 ${view === 'chat' ? 'px-0 max-w-none' : 'px-4 sm:px-6 lg:px-8 max-w-6xl mx-auto'} transition-all duration-500`}>
        <AnimatePresence mode="wait">
          {view === 'dashboard' ? (
            <motion.div
              key="dashboard"
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -20 }}
              transition={{ duration: 0.4 }}
              className="space-y-8 sm:space-y-12"
            >
              {/* Hero Section */}
              <section className="space-y-3">
                <h2 className="text-4xl sm:text-5xl lg:text-7xl font-serif italic text-on-surface tracking-tight leading-[1.1]">
                  Good morning,<br className="hidden sm:block" /> Olivia.
                </h2>
                <p className="text-on-surface-variant text-base sm:text-lg lg:text-xl max-w-2xl leading-relaxed">
                  Your vitality is at <span className="text-primary font-bold">84%</span> today. The morning air is crisp—perfect for your ritual.
                </p>
              </section>

              {/* Main Bento Grid */}
              <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 sm:gap-8">
                {/* Vitality Card */}
                <motion.div 
                  initial={{ opacity: 0, scale: 0.98 }}
                  animate={{ opacity: 1, scale: 1 }}
                  transition={{ delay: 0.1 }}
                  className="lg:col-span-8 bg-surface-container-lowest rounded-[2rem] sm:rounded-[3rem] p-6 sm:p-10 vibrant-shadow relative overflow-hidden group border border-white"
                >
                  <div className="absolute top-0 right-0 w-64 h-64 bg-secondary-fixed/10 rounded-full blur-3xl -mr-20 -mt-20 group-hover:bg-secondary-fixed/20 transition-colors duration-700" />
                  
                  <div className="relative z-10 flex flex-col sm:flex-row items-center gap-8 sm:gap-12">
                    <div className="shrink-0">
                      <VitalityGauge />
                    </div>
                    
                    <div className="flex-1 space-y-6 sm:space-y-8 w-full text-center sm:text-left">
                      <div>
                        <h3 className="font-serif text-2xl sm:text-3xl lg:text-4xl text-on-surface">Wellness Momentum</h3>
                        <p className="text-on-surface-variant text-sm sm:text-base mt-1">You've maintained your streak for 12 days.</p>
                      </div>
                      
                      <MetabolicFlux />
                      
                      <button className="w-full sm:w-auto px-10 py-4 rounded-full bg-gradient-to-r from-primary-container to-primary text-white font-bold text-sm tracking-widest shadow-lg shadow-primary/20 hover:shadow-primary/40 hover:-translate-y-0.5 active:translate-y-0 transition-all uppercase">
                        View Full Analysis
                      </button>
                    </div>
                  </div>
                </motion.div>

                {/* Calories Card */}
                <motion.div 
                  initial={{ opacity: 0, scale: 0.98 }}
                  animate={{ opacity: 1, scale: 1 }}
                  transition={{ delay: 0.2 }}
                  className="lg:col-span-4 bg-inverse-surface text-white rounded-[2rem] sm:rounded-[3rem] p-8 sm:p-10 flex flex-col justify-between relative overflow-hidden group"
                >
                  <div className="absolute bottom-0 right-0 w-48 h-48 bg-lime-500/10 rounded-full blur-3xl -mb-16 -mr-16 group-hover:bg-lime-500/20 transition-colors duration-700" />
                  <div className="flex justify-between items-start">
                    <div className="p-4 bg-white/10 rounded-2xl backdrop-blur-md">
                      <Utensils className="text-lime-400" size={28} />
                    </div>
                    <span className="text-[10px] font-bold uppercase tracking-widest bg-white/10 px-4 py-1.5 rounded-full backdrop-blur-md">Fuel Left</span>
                  </div>
                  <div className="mt-12 sm:mt-0">
                    <div className="text-5xl sm:text-6xl font-serif italic text-lime-400">1,240</div>
                    <div className="text-[10px] text-white/40 uppercase tracking-[0.2em] mt-3 font-bold">Calories remaining</div>
                  </div>
                </motion.div>
              </div>

              {/* Activity Grid */}
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6 sm:gap-8">
                {/* Movement */}
                <motion.div 
                  whileHover={{ y: -8 }}
                  className="bg-surface-container-low rounded-[2rem] p-8 space-y-8 border border-white/50 transition-shadow hover:shadow-xl hover:shadow-primary/5"
                >
                  <div className="flex items-center gap-4">
                    <div className="w-12 h-12 rounded-2xl bg-primary/10 flex items-center justify-center">
                      <Activity className="text-primary" size={24} />
                    </div>
                    <h4 className="font-serif text-2xl">Movement</h4>
                  </div>
                  <div className="space-y-5">
                    <div className="flex items-end gap-2">
                      <span className="text-4xl sm:text-5xl font-bold tracking-tight">8,432</span>
                      <span className="text-on-surface-variant text-sm mb-2 font-medium">/ 10k steps</span>
                    </div>
                    <div className="w-full h-2 bg-slate-200 rounded-full overflow-hidden">
                      <motion.div 
                        initial={{ width: 0 }}
                        animate={{ width: "84%" }}
                        className="h-full bg-primary rounded-full shadow-[0_0_8px_rgba(0,89,182,0.3)]" 
                      />
                    </div>
                  </div>
                </motion.div>

                {/* Rest */}
                <motion.div 
                  whileHover={{ y: -8 }}
                  className="bg-surface-container-low rounded-[2rem] p-8 space-y-8 border border-white/50 transition-shadow hover:shadow-xl hover:shadow-cyan-500/5"
                >
                  <div className="flex items-center gap-4">
                    <div className="w-12 h-12 rounded-2xl bg-cyan-100 flex items-center justify-center">
                      <Moon className="text-cyan-600" size={24} />
                    </div>
                    <h4 className="font-serif text-2xl">Rest</h4>
                  </div>
                  <div className="space-y-5">
                    <div className="flex items-end gap-2">
                      <span className="text-4xl sm:text-5xl font-bold tracking-tight">7h 20m</span>
                      <span className="text-on-surface-variant text-sm mb-2 font-medium">Deep sleep</span>
                    </div>
                    <div className="flex gap-2 h-12 items-end">
                      {[40, 65, 100, 50, 80, 45, 90].map((h, i) => (
                        <div key={i} className="flex-1 bg-cyan-200 rounded-t-md hover:bg-cyan-400 transition-colors cursor-pointer" style={{ height: `${h}%` }} />
                      ))}
                    </div>
                  </div>
                </motion.div>

                {/* Hydration */}
                <motion.div 
                  whileHover={{ y: -8 }}
                  className="bg-surface-container-low rounded-[2rem] p-8 space-y-8 border border-white/50 transition-shadow hover:shadow-xl hover:shadow-blue-500/5 sm:col-span-2 lg:col-span-1"
                >
                  <div className="flex items-center gap-4">
                    <div className="w-12 h-12 rounded-2xl bg-blue-100 flex items-center justify-center">
                      <Droplets className="text-blue-600" size={24} />
                    </div>
                    <h4 className="font-serif text-2xl">Hydration</h4>
                  </div>
                  <div className="flex flex-wrap gap-3">
                    {[1, 2, 3, 4].map(i => (
                      <div key={i} className="w-10 h-14 bg-primary rounded-xl shadow-sm hover:scale-105 transition-transform cursor-pointer" />
                    ))}
                    {[5, 6, 7, 8].map(i => (
                      <div key={i} className="w-10 h-14 bg-slate-200 rounded-xl hover:bg-slate-300 transition-colors cursor-pointer" />
                    ))}
                  </div>
                  <p className="text-[10px] text-on-surface-variant font-bold uppercase tracking-[0.2em]">4 of 8 glasses reached</p>
                </motion.div>
              </div>

              {/* Recipe Suggestion */}
              <motion.section 
                initial={{ opacity: 0, y: 20 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                className="bg-surface-container-lowest rounded-[2rem] sm:rounded-[3rem] overflow-hidden vibrant-shadow border border-white"
              >
                <div className="grid grid-cols-1 lg:grid-cols-2">
                  <div className="p-8 sm:p-12 lg:p-16 space-y-8">
                    <span className="inline-block px-5 py-2 rounded-full bg-orange-50 text-orange-600 text-[10px] font-bold uppercase tracking-[0.25em]">Recipe Suggestion</span>
                    <h3 className="text-4xl sm:text-5xl font-serif italic leading-tight">Glazed Salmon with Citrus Quinoa</h3>
                    <p className="text-on-surface-variant leading-relaxed text-lg sm:text-xl">
                      Rich in Omega-3 and Magnesium to support your recovery after yesterday's high-intensity session.
                    </p>
                    <div className="flex items-center gap-10 pt-4">
                      <div className="flex items-center gap-3">
                        <Timer className="text-primary" size={20} />
                        <span className="text-base font-bold">25 min</span>
                      </div>
                      <div className="flex items-center gap-3">
                        <Flame className="text-primary" size={20} />
                        <span className="text-base font-bold">420 kcal</span>
                      </div>
                    </div>
                  </div>
                  <div className="relative min-h-[350px] group overflow-hidden">
                    <img 
                      src="https://images.unsplash.com/photo-1467003909585-2f8a72700288?auto=format&fit=crop&q=80&w=1200" 
                      alt="Glazed Salmon" 
                      className="absolute inset-0 w-full h-full object-cover group-hover:scale-105 transition-transform duration-[2000ms]"
                      referrerPolicy="no-referrer"
                    />
                    <div className="absolute inset-0 bg-gradient-to-r from-surface-container-lowest via-transparent to-transparent lg:block hidden" />
                  </div>
                </div>
              </motion.section>
            </motion.div>
          ) : view === 'chat' ? (
            <motion.div
              key="chat"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="h-[calc(100vh-4rem)] w-full"
            >
              <ChatRoom isNavVisible={isNavVisible} />
            </motion.div>
          ) : view === 'activity' ? (
            <ActivityView key="activity" />
          ) : (
            <ProfileView key="profile" />
          )}
        </AnimatePresence>
      </main>

      {/* Bottom Navigation Toggle Handle (Visible when nav is hidden) */}
      {!isNavVisible && (
        <motion.button
          initial={{ y: 20, opacity: 0 }}
          animate={{ y: 0, opacity: 1 }}
          whileHover={{ scale: 1.1, backgroundColor: 'rgba(255,255,255,0.6)' }}
          onClick={() => setIsNavVisible(true)}
          className="fixed bottom-4 left-1/2 -translate-x-1/2 w-16 h-4 bg-white/40 backdrop-blur-md rounded-full z-[60] border border-black/5 shadow-lg flex items-center justify-center transition-all cursor-pointer"
          title="Show Navigation"
        >
          <div className="w-8 h-1 bg-primary/40 rounded-full" />
        </motion.button>
      )}

      {/* Bottom Navigation */}
      <motion.nav 
        initial={false}
        animate={{ 
          y: isNavVisible ? 0 : 100,
          opacity: isNavVisible ? 1 : 0,
          pointerEvents: isNavVisible ? 'auto' : 'none'
        }}
        transition={{ type: "spring", damping: 25, stiffness: 200 }}
        className="fixed bottom-8 left-1/2 -translate-x-1/2 w-[90%] max-w-md bg-inverse-surface/90 backdrop-blur-2xl rounded-full px-8 py-4 z-50 flex justify-between items-center shadow-2xl shadow-primary/10 border border-white/10"
      >
        <button 
          onClick={() => setView('dashboard')}
          className={`relative flex flex-col items-center transition-all duration-300 ${view === 'dashboard' ? "text-cyan-400 scale-110" : "text-white/40 hover:text-white"}`}
        >
          <LayoutDashboard size={24} strokeWidth={2.5} />
          {view === 'dashboard' && <div className="absolute -bottom-2 w-1 h-1 bg-cyan-400 rounded-full" />}
        </button>
        <button 
          onClick={() => setView('chat')}
          className={`relative flex flex-col items-center transition-all duration-300 ${view === 'chat' ? "text-cyan-400 scale-110" : "text-white/40 hover:text-white"}`}
        >
          <MessageSquare size={24} strokeWidth={2.5} />
          {view === 'chat' && <div className="absolute -bottom-2 w-1 h-1 bg-cyan-400 rounded-full" />}
        </button>
        <button 
          onClick={() => setView('activity')}
          className={`relative flex flex-col items-center transition-all duration-300 ${view === 'activity' ? "text-cyan-400 scale-110" : "text-white/40 hover:text-white"}`}
        >
          <Activity size={24} strokeWidth={2.5} />
          {view === 'activity' && <div className="absolute -bottom-2 w-1 h-1 bg-cyan-400 rounded-full" />}
        </button>
        <button 
          onClick={() => setView('profile')}
          className={`relative flex flex-col items-center transition-all duration-300 ${view === 'profile' ? "text-cyan-400 scale-110" : "text-white/40 hover:text-white"}`}
        >
          <User size={24} strokeWidth={2.5} />
          {view === 'profile' && <div className="absolute -bottom-2 w-1 h-1 bg-cyan-400 rounded-full" />}
        </button>
        <button 
          onClick={() => setIsNavVisible(false)}
          className="text-white/40 hover:text-white transition-colors flex flex-col items-center ml-2 pl-2 border-l border-white/10"
          title="Hide Navigation"
        >
          <ChevronRight size={20} className="rotate-90" />
        </button>
      </motion.nav>
    </div>
  );
}
