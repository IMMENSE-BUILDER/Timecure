import React, { useEffect, useRef, useState } from 'react';
import {
  AlarmClock,
  BarChart3,
  BellRing,
  Check,
  CheckCircle2,
  Flame,
  Pause,
  Play,
  RotateCcw,
  Settings,
  SkipForward,
  Sparkles,
  Sun,
  Moon,
  Timer,
  Volume2,
  X,
} from 'lucide-react';
import { PaletteId, TIMECURE_PALETTES } from '../themes';
import { TimecureLogo } from './TimecureLogo';

type PhoneTab = 'alarms' | 'focus' | 'insights' | 'settings';

interface SimAlarm {
  id: number;
  time: string;
  ampm: string;
  label: string;
  prompt: string;
  days: string;
  difficulty: 'Gentle' | 'Sharp' | 'Relentless';
  enabled: boolean;
}

interface InteractivePhoneProps {
  paletteId: PaletteId;
  darkMode: boolean;
  selectedModel: string;
  onSelectPalette: (id: PaletteId) => void;
  onToggleDarkMode: () => void;
  onSelectModel: (model: string) => void;
}

const INITIAL_ALARMS: SimAlarm[] = [
  {
    id: 1,
    time: '06:30',
    ampm: 'AM',
    label: 'College Morning',
    prompt: 'Get up for college — no snooze!',
    days: 'Mon – Fri',
    difficulty: 'Relentless',
    enabled: true,
  },
  {
    id: 2,
    time: '07:45',
    ampm: 'AM',
    label: 'DSA Deep Sprint',
    prompt: 'Solve 2 graph problems before class',
    days: 'Tue, Thu, Sat',
    difficulty: 'Sharp',
    enabled: true,
  },
  {
    id: 3,
    time: '10:30',
    ampm: 'PM',
    label: 'Night Shutdown',
    prompt: 'Prep bag & lock phone',
    days: 'Daily',
    difficulty: 'Gentle',
    enabled: false,
  },
];

const INITIAL_TASKS = [
  { id: 1, slot: '06:30 AM', task: 'Calculus III Lecture Prep', tech: 'Active Recall', done: true },
  { id: 2, slot: '09:00 AM', task: 'OS Kernel Lab (Threads)', tech: '25m Pomodoro', done: false },
  { id: 3, slot: '04:30 PM', task: 'System Design Notes', tech: 'Feynman Method', done: false },
];

export const InteractivePhone: React.FC<InteractivePhoneProps> = ({
  paletteId,
  darkMode,
  selectedModel,
  onSelectPalette,
  onToggleDarkMode,
  onSelectModel,
}) => {
  const [tab, setTab] = useState<PhoneTab>('alarms');
  const [sweepKey, setSweepKey] = useState(0);
  const [alarms, setAlarms] = useState<SimAlarm[]>(INITIAL_ALARMS);
  const [tasks, setTasks] = useState(INITIAL_TASKS);

  // Live Pomodoro Chronometer state
  const [pomoPhase, setPomoPhase] = useState<'focus' | 'short' | 'long'>('focus');
  const [pomoRunning, setPomoRunning] = useState(true);
  const [pomoTotal, setPomoTotal] = useState(25 * 60);
  const [pomoRemaining, setPomoRemaining] = useState(19 * 60 + 42);
  const dialCanvasRef = useRef<HTMLCanvasElement | null>(null);

  // Live Ringing Takeover simulation
  const [isRinging, setIsRinging] = useState(false);
  const [ringStep, setRingStep] = useState(0);
  const [ringShake, setRingShake] = useState(false);
  const [ringVictory, setRingVictory] = useState(false);

  const switchTab = (next: PhoneTab) => {
    if (next === tab) return;
    setTab(next);
    setSweepKey((k) => k + 1);
  };

  // Pomodoro tick
  useEffect(() => {
    if (!pomoRunning) return;
    const id = window.setInterval(() => {
      setPomoRemaining((r) => (r > 1 ? r - 1 : pomoTotal));
    }, 1000);
    return () => window.clearInterval(id);
  }, [pomoRunning, pomoTotal]);

  // 60-tick Chronometer Canvas DrawScope equivalent
  useEffect(() => {
    const canvas = dialCanvasRef.current;
    if (!canvas || tab !== 'focus') return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const dpr = window.devicePixelRatio || 1;
    const size = 176;
    canvas.width = size * dpr;
    canvas.height = size * dpr;
    ctx.scale(dpr, dpr);

    const styles = getComputedStyle(document.documentElement);
    const accent =
      pomoPhase === 'focus'
        ? styles.getPropertyValue('--tc-primary').trim()
        : pomoPhase === 'short'
        ? styles.getPropertyValue('--tc-secondary').trim()
        : styles.getPropertyValue('--tc-tertiary').trim();
    const track = styles.getPropertyValue('--tc-surface-highest').trim();
    const halo = styles.getPropertyValue('--tc-primary-container').trim();

    let animId = 0;
    const render = (t: number) => {
      ctx.clearRect(0, 0, size, size);
      const cx = size / 2;
      const cy = size / 2;
      const outerR = size / 2 - 5;
      const ringR = outerR - 14;
      const progress = 1 - pomoRemaining / pomoTotal;
      const breath = pomoRunning ? 0.5 + 0.5 * Math.sin(t / 550) : 0.35;

      // Breathing inner aura
      if (pomoRunning) {
        ctx.save();
        ctx.globalAlpha = 0.2 + 0.14 * breath;
        ctx.fillStyle = halo;
        ctx.beginPath();
        ctx.arc(cx, cy, ringR * (0.85 + 0.04 * breath), 0, Math.PI * 2);
        ctx.fill();
        ctx.restore();
      }

      // 60 chronometer bezel ticks
      const litTicks = Math.floor(progress * 60);
      for (let i = 0; i < 60; i++) {
        const angle = ((-90 + i * 6) * Math.PI) / 180;
        const major = i % 5 === 0;
        const len = major ? 7.5 : 4;
        const r1 = outerR - len;
        const r2 = outerR;
        const isLit = i <= litTicks && progress > 0.001;
        ctx.beginPath();
        ctx.moveTo(cx + r1 * Math.cos(angle), cy + r1 * Math.sin(angle));
        ctx.lineTo(cx + r2 * Math.cos(angle), cy + r2 * Math.sin(angle));
        ctx.strokeStyle = isLit ? accent : track;
        ctx.globalAlpha = isLit ? (major ? 0.95 : 0.65) : major ? 0.85 : 0.45;
        ctx.lineWidth = major ? 2 : 1.1;
        ctx.lineCap = 'round';
        ctx.stroke();
      }
      ctx.globalAlpha = 1;

      // Track ring
      ctx.beginPath();
      ctx.arc(cx, cy, ringR, 0, Math.PI * 2);
      ctx.strokeStyle = track;
      ctx.lineWidth = 7.5;
      ctx.stroke();

      // Active sweep arc
      const sweep = Math.PI * 2 * progress;
      ctx.beginPath();
      ctx.arc(cx, cy, ringR, -Math.PI / 2, -Math.PI / 2 + sweep);
      ctx.strokeStyle = accent;
      ctx.lineWidth = 7.5;
      ctx.lineCap = 'round';
      ctx.stroke();

      // Orbiting head dot
      const headAngle = -Math.PI / 2 + sweep;
      const hx = cx + ringR * Math.cos(headAngle);
      const hy = cy + ringR * Math.sin(headAngle);
      ctx.beginPath();
      ctx.arc(hx, hy, 8.5, 0, Math.PI * 2);
      ctx.fillStyle = accent;
      ctx.globalAlpha = 0.25 + 0.2 * breath;
      ctx.fill();
      ctx.globalAlpha = 1;
      ctx.beginPath();
      ctx.arc(hx, hy, 4.5, 0, Math.PI * 2);
      ctx.fillStyle = accent;
      ctx.fill();

      if (pomoRunning) {
        animId = requestAnimationFrame(render);
      }
    };

    animId = requestAnimationFrame(render);
    return () => cancelAnimationFrame(animId);
  }, [tab, pomoRemaining, pomoTotal, pomoRunning, pomoPhase, paletteId, darkMode]);

  const setPhase = (p: 'focus' | 'short' | 'long') => {
    setPomoPhase(p);
    const secs = p === 'focus' ? 25 * 60 : p === 'short' ? 5 * 60 : 15 * 60;
    setPomoTotal(secs);
    setPomoRemaining(secs);
  };

  const mins = String(Math.floor(pomoRemaining / 60)).padStart(2, '0');
  const secs = String(pomoRemaining % 60).padStart(2, '0');

  const handleRingAnswer = (correct: boolean) => {
    if (!correct) {
      setRingShake(true);
      window.setTimeout(() => setRingShake(false), 360);
      return;
    }
    if (ringStep === 0) {
      setRingStep(1);
    } else {
      setRingVictory(true);
    }
  };

  const openRingingDemo = () => {
    setRingStep(0);
    setRingVictory(false);
    setIsRinging(true);
  };

  return (
    <div className="relative mx-auto w-[314px] sm:w-[334px] select-none">
      {/* Ambient aura behind phone */}
      <div
        className="pointer-events-none absolute -inset-6 rounded-[56px] blur-3xl opacity-30 transition-colors duration-500"
        style={{
          background:
            'radial-gradient(circle at 30% 20%, var(--tc-primary), transparent 60%), radial-gradient(circle at 80% 80%, var(--tc-tertiary), transparent 60%)',
        }}
      />

      {/* Hardware Frame */}
      <div
        className="relative rounded-[44px] p-[9px] shadow-2xl transition-colors duration-300"
        style={{
          backgroundColor: darkMode ? '#1c222b' : '#262d38',
          boxShadow:
            '0 30px 80px -20px rgba(0,0,0,0.65), inset 0 1px 1px rgba(255,255,255,0.2)',
        }}
      >
        {/* Inner Display */}
        <div
          className="relative flex h-[628px] flex-col overflow-hidden rounded-[36px] transition-colors duration-300"
          style={{
            backgroundColor: 'var(--tc-bg)',
            color: 'var(--tc-on-surface)',
          }}
        >
          {/* Android Status Bar + Punch-Hole Notch */}
          <div className="relative z-20 flex items-center justify-between px-6 pt-3 pb-1.5 text-[11px] font-medium font-mono-tech">
            <span>06:30</span>
            <div className="h-[14px] w-[14px] rounded-full bg-black ring-2 ring-white/10" />
            <div className="flex items-center gap-1.5 opacity-80">
              <span>5G</span>
              <span className="inline-block h-2.5 w-4 rounded-[3px] border border-current p-[1px]">
                <span className="block h-full w-3/4 rounded-[1px] bg-current" />
              </span>
            </div>
          </div>

          {/* Phase transition sweep bar (mirrors NavigationPhaseOverlay) */}
          <div className="relative h-[2.5px] w-full overflow-hidden">
            <div
              key={sweepKey}
              className="animate-tc-sweep h-full w-1/2 rounded-full"
              style={{ backgroundColor: 'var(--tc-primary)' }}
            />
          </div>

          {/* Screen Body */}
          <div className="relative flex-1 overflow-y-auto px-4 pt-2 pb-3 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
            {/* ALARMS TAB */}
            {tab === 'alarms' && (
              <div className="space-y-3 animate-[fadeIn_200ms_ease]">
                <div className="flex items-center justify-between pt-0.5">
                  <div>
                    <div className="text-[11px] font-medium" style={{ color: 'var(--tc-on-surface-variant)' }}>
                      Good morning, Satyam
                    </div>
                    <div className="text-lg font-bold tracking-tight">Timecure</div>
                  </div>
                  <button
                    onClick={() => switchTab('settings')}
                    title="Timecure AI Assistant & Theme"
                    className="tc-pressable cursor-pointer"
                  >
                    <TimecureLogo size={34} />
                  </button>
                </div>

                {/* Next Alarm Hero Summary Card */}
                <div
                  className="rounded-2xl p-3.5 transition-colors"
                  style={{
                    backgroundColor: 'var(--tc-primary-container)',
                    color: 'var(--tc-on-primary-container)',
                  }}
                >
                  <div className="flex items-center justify-between text-[11px] font-semibold uppercase tracking-wider opacity-85">
                    <span className="inline-flex items-center gap-1.5">
                      <span
                        className="inline-block h-2 w-2 rounded-full animate-tc-pulse"
                        style={{ backgroundColor: 'var(--tc-tertiary)' }}
                      />
                      Next wake-up
                    </span>
                    <span className="font-mono-tech">in 6h 14m</span>
                  </div>
                  <div className="mt-1 flex items-baseline justify-between">
                    <div className="font-mono-tech text-2xl font-bold">06:30 AM</div>
                    <span className="rounded-full bg-black/15 px-2.5 py-0.5 text-[10px] font-semibold">
                      14d streak
                    </span>
                  </div>
                  <div className="mt-1 text-xs opacity-90">
                    “Get up for college — no snooze!”
                  </div>
                  <button
                    onClick={openRingingDemo}
                    className="tc-pressable mt-2.5 flex w-full cursor-pointer items-center justify-center gap-1.5 rounded-xl py-2 text-xs font-bold"
                    style={{
                      backgroundColor: 'var(--tc-primary)',
                      color: 'var(--tc-on-primary)',
                    }}
                  >
                    <BellRing className="h-3.5 w-3.5" />
                    Simulate Unstoppable Ring
                  </button>
                </div>

                {/* Alarm List */}
                <div className="space-y-2">
                  {alarms.map((alarm) => (
                    <div
                      key={alarm.id}
                      className="rounded-2xl p-3 transition-all duration-200"
                      style={{
                        backgroundColor: 'var(--tc-surface-low)',
                        border: `1px solid var(--tc-outline-variant)`,
                        opacity: alarm.enabled ? 1 : 0.62,
                      }}
                    >
                      <div className="flex items-center justify-between">
                        <div>
                          <div className="flex items-baseline gap-1">
                            <span className="font-mono-tech text-xl font-bold">
                              {alarm.time}
                            </span>
                            <span
                              className="text-[10px] font-semibold"
                              style={{ color: 'var(--tc-on-surface-variant)' }}
                            >
                              {alarm.ampm}
                            </span>
                          </div>
                          <div className="text-xs font-semibold">{alarm.label}</div>
                        </div>
                        <button
                          onClick={() =>
                            setAlarms((prev) =>
                              prev.map((a) =>
                                a.id === alarm.id ? { ...a, enabled: !a.enabled } : a
                              )
                            )
                          }
                          aria-label={`Toggle ${alarm.label}`}
                          className="tc-pressable relative h-6 w-11 cursor-pointer rounded-full transition-colors"
                          style={{
                            backgroundColor: alarm.enabled
                              ? 'var(--tc-primary)'
                              : 'var(--tc-surface-highest)',
                          }}
                        >
                          <span
                            className="absolute top-0.5 h-5 w-5 rounded-full transition-transform duration-200"
                            style={{
                              backgroundColor: alarm.enabled
                                ? 'var(--tc-on-primary)'
                                : 'var(--tc-on-surface-variant)',
                              left: '2px',
                              transform: alarm.enabled
                                ? 'translateX(20px)'
                                : 'translateX(0)',
                            }}
                          />
                        </button>
                      </div>
                      <div
                        className="mt-1.5 flex items-center justify-between text-[10px]"
                        style={{ color: 'var(--tc-on-surface-variant)' }}
                      >
                        <span className="truncate pr-2">{alarm.prompt}</span>
                        <span
                          className="shrink-0 rounded-full px-2 py-0.5 font-semibold"
                          style={{
                            backgroundColor: 'var(--tc-surface-high)',
                            color: 'var(--tc-primary)',
                          }}
                        >
                          {alarm.difficulty}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* FOCUS POMODORO TAB */}
            {tab === 'focus' && (
              <div className="flex flex-col items-center space-y-3 pt-1">
                <div className="text-sm font-bold">Focus Pomodoro</div>

                {/* Segmented phase switcher */}
                <div
                  className="grid w-full grid-cols-3 rounded-full p-0.5 text-[11px] font-semibold"
                  style={{
                    backgroundColor: 'var(--tc-surface-low)',
                    border: '1px solid var(--tc-outline-variant)',
                  }}
                >
                  {(
                    [
                      ['focus', 'Focus'],
                      ['short', 'Short'],
                      ['long', 'Long'],
                    ] as const
                  ).map(([id, label]) => (
                    <button
                      key={id}
                      onClick={() => setPhase(id)}
                      className="tc-pressable cursor-pointer rounded-full py-1.5 transition-colors"
                      style={{
                        backgroundColor:
                          pomoPhase === id ? 'var(--tc-primary-container)' : 'transparent',
                        color:
                          pomoPhase === id
                            ? 'var(--tc-on-primary-container)'
                            : 'var(--tc-on-surface-variant)',
                      }}
                    >
                      {label}
                    </button>
                  ))}
                </div>

                {/* Goal pill */}
                <div
                  className="inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-[11px] font-medium"
                  style={{ backgroundColor: 'var(--tc-surface-low)' }}
                >
                  <Flame className="h-3.5 w-3.5" style={{ color: 'var(--tc-primary)' }} />
                  Get up for college • Deep Study
                </div>

                {/* 60-tick Chronometer Canvas */}
                <div className="relative flex h-[176px] w-[176px] items-center justify-center">
                  <canvas
                    ref={dialCanvasRef}
                    className="h-[176px] w-[176px]"
                  />
                  <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center">
                    <span
                      className="text-[9px] font-bold tracking-[0.18em] uppercase"
                      style={{ color: 'var(--tc-primary)' }}
                    >
                      {pomoPhase === 'focus' ? 'DEEP FOCUS' : 'RECOVERY'}
                    </span>
                    <span className="font-mono-tech text-3xl font-bold tracking-tight">
                      {mins}:{secs}
                    </span>
                    <div className="mt-1 flex gap-1">
                      {[0, 1, 2, 3].map((dot) => (
                        <span
                          key={dot}
                          className="h-1.5 rounded-full transition-all"
                          style={{
                            width: dot < 2 ? 12 : 6,
                            backgroundColor:
                              dot < 2 ? 'var(--tc-primary)' : 'var(--tc-outline-variant)',
                          }}
                        />
                      ))}
                    </div>
                  </div>
                </div>

                {/* Transport controls */}
                <div className="flex items-center gap-3">
                  <button
                    onClick={() => setPhase(pomoPhase)}
                    aria-label="Reset timer"
                    className="tc-pressable flex h-10 w-10 cursor-pointer items-center justify-center rounded-full"
                    style={{ backgroundColor: 'var(--tc-surface-high)' }}
                  >
                    <RotateCcw className="h-4 w-4" />
                  </button>
                  <button
                    onClick={() => setPomoRunning((r) => !r)}
                    className="tc-pressable flex h-11 cursor-pointer items-center justify-center gap-2 px-6 text-xs font-bold transition-all"
                    style={{
                      borderRadius: pomoRunning ? '14px' : '999px',
                      backgroundColor: pomoRunning
                        ? 'var(--tc-primary-container)'
                        : 'var(--tc-primary)',
                      color: pomoRunning
                        ? 'var(--tc-on-primary-container)'
                        : 'var(--tc-on-primary)',
                    }}
                  >
                    {pomoRunning ? <Pause className="h-4 w-4" /> : <Play className="h-4 w-4" />}
                    {pomoRunning ? 'Pause' : 'Start'}
                  </button>
                  <button
                    onClick={() =>
                      setPhase(pomoPhase === 'focus' ? 'short' : 'focus')
                    }
                    aria-label="Skip phase"
                    className="tc-pressable flex h-10 w-10 cursor-pointer items-center justify-center rounded-full"
                    style={{ backgroundColor: 'var(--tc-surface-high)' }}
                  >
                    <SkipForward className="h-4 w-4" />
                  </button>
                </div>

                {/* Telemetry Strip */}
                <div
                  className="grid w-full grid-cols-3 rounded-2xl p-2.5 text-center"
                  style={{ backgroundColor: 'var(--tc-surface-low)' }}
                >
                  <div>
                    <div className="font-mono-tech text-sm font-bold">115m</div>
                    <div className="text-[9px]" style={{ color: 'var(--tc-on-surface-variant)' }}>
                      Today
                    </div>
                  </div>
                  <div>
                    <div className="font-mono-tech text-sm font-bold">18</div>
                    <div className="text-[9px]" style={{ color: 'var(--tc-on-surface-variant)' }}>
                      Blocks
                    </div>
                  </div>
                  <div>
                    <div className="font-mono-tech text-sm font-bold">4</div>
                    <div className="text-[9px]" style={{ color: 'var(--tc-on-surface-variant)' }}>
                      Streak
                    </div>
                  </div>
                </div>
              </div>
            )}

            {/* INSIGHTS TAB */}
            {tab === 'insights' && (
              <div className="space-y-3 pt-1">
                <div className="text-center text-sm font-bold">Insights & Dojo</div>

                <div className="grid grid-cols-2 gap-2">
                  {[
                    { label: 'Wake streak', value: '14d', hi: true },
                    { label: 'Avg solve', value: '11.4s', hi: false },
                    { label: 'Fastest wake', value: '6.2s', hi: false },
                    { label: 'Focused today', value: '115m', hi: true },
                  ].map((m) => (
                    <div
                      key={m.label}
                      className="rounded-2xl p-2.5"
                      style={{
                        backgroundColor: m.hi
                          ? 'var(--tc-primary-container)'
                          : 'var(--tc-surface-low)',
                        color: m.hi
                          ? 'var(--tc-on-primary-container)'
                          : 'var(--tc-on-surface)',
                      }}
                    >
                      <div className="font-mono-tech text-lg font-bold">{m.value}</div>
                      <div className="text-[10px] opacity-80">{m.label}</div>
                    </div>
                  ))}
                </div>

                {/* 7-day wake chart */}
                <div
                  className="rounded-2xl p-3"
                  style={{ backgroundColor: 'var(--tc-surface-low)' }}
                >
                  <div className="mb-2 flex items-center justify-between text-[11px] font-semibold">
                    <span>7-Day Wake Velocity</span>
                    <span className="font-mono-tech" style={{ color: 'var(--tc-primary)' }}>
                      100% solved
                    </span>
                  </div>
                  <div className="flex h-16 items-end justify-between gap-1.5 px-1">
                    {[62, 85, 74, 96, 88, 79, 100].map((h, i) => (
                      <div key={i} className="flex flex-1 flex-col items-center gap-1">
                        <div
                          className="w-full rounded-t-md transition-all duration-300"
                          style={{
                            height: `${h * 0.5}px`,
                            backgroundColor:
                              i === 6 ? 'var(--tc-tertiary)' : 'var(--tc-primary)',
                            opacity: i === 6 ? 1 : 0.78,
                          }}
                        />
                        <span
                          className="text-[9px] font-mono-tech"
                          style={{ color: 'var(--tc-on-surface-variant)' }}
                        >
                          {['M', 'T', 'W', 'T', 'F', 'S', 'S'][i]}
                        </span>
                      </div>
                    ))}
                  </div>
                </div>

                {/* AI Vision Task Table */}
                <div
                  className="rounded-2xl p-3"
                  style={{ backgroundColor: 'var(--tc-surface-low)' }}
                >
                  <div className="mb-2 flex items-center gap-1.5 text-[11px] font-semibold">
                    <Sparkles className="h-3.5 w-3.5" style={{ color: 'var(--tc-primary)' }} />
                    AI Vision Task Table
                  </div>
                  <div className="space-y-1.5">
                    {tasks.map((t) => (
                      <button
                        key={t.id}
                        onClick={() =>
                          setTasks((prev) =>
                            prev.map((x) =>
                              x.id === t.id ? { ...x, done: !x.done } : x
                            )
                          )
                        }
                        className="tc-pressable flex w-full cursor-pointer items-center gap-2 rounded-xl p-1.5 text-left"
                        style={{ backgroundColor: 'var(--tc-surface-high)' }}
                      >
                        <span
                          className="flex h-4 w-4 shrink-0 items-center justify-center rounded border"
                          style={{
                            backgroundColor: t.done ? 'var(--tc-primary)' : 'transparent',
                            borderColor: t.done ? 'var(--tc-primary)' : 'var(--tc-outline)',
                            color: 'var(--tc-on-primary)',
                          }}
                        >
                          {t.done && <Check className="h-3 w-3" />}
                        </span>
                        <div className="min-w-0 flex-1">
                          <div
                            className={`truncate text-[11px] font-medium ${
                              t.done ? 'line-through opacity-55' : ''
                            }`}
                          >
                            {t.task}
                          </div>
                          <div
                            className="text-[9px]"
                            style={{ color: 'var(--tc-on-surface-variant)' }}
                          >
                            {t.slot} • {t.tech}
                          </div>
                        </div>
                      </button>
                    ))}
                  </div>
                </div>
              </div>
            )}

            {/* SETTINGS TAB */}
            {tab === 'settings' && (
              <div className="space-y-3 pt-1">
                <div className="text-center text-sm font-bold">Settings</div>

                <div className="flex items-center justify-between rounded-2xl p-2.5" style={{ backgroundColor: 'var(--tc-surface-low)' }}>
                  <span className="text-xs font-semibold">Appearance</span>
                  <button
                    onClick={onToggleDarkMode}
                    className="tc-pressable inline-flex cursor-pointer items-center gap-1.5 rounded-full px-3 py-1 text-[11px] font-semibold"
                    style={{
                      backgroundColor: 'var(--tc-primary-container)',
                      color: 'var(--tc-on-primary-container)',
                    }}
                  >
                    {darkMode ? <Moon className="h-3 w-3" /> : <Sun className="h-3 w-3" />}
                    {darkMode ? 'Dark' : 'Light'}
                  </button>
                </div>

                {/* 6 Real App Themes */}
                <div>
                  <div
                    className="mb-1.5 text-[10px] font-bold uppercase tracking-wider"
                    style={{ color: 'var(--tc-primary)' }}
                  >
                    6 Material 3 Palettes (Live Sync)
                  </div>
                  <div className="grid grid-cols-2 gap-1.5">
                    {TIMECURE_PALETTES.map((pal) => {
                      const active = pal.id === paletteId;
                      return (
                        <button
                          key={pal.id}
                          onClick={() => onSelectPalette(pal.id)}
                          className="tc-pressable flex cursor-pointer flex-col gap-1 rounded-xl p-2 text-left"
                          style={{
                            backgroundColor: active
                              ? 'var(--tc-primary-container)'
                              : 'var(--tc-surface-low)',
                            border: `1.5px solid ${
                              active ? 'var(--tc-primary)' : 'var(--tc-outline-variant)'
                            }`,
                          }}
                        >
                          <div className="flex items-center justify-between">
                            <div className="flex gap-1">
                              {pal.preview.map((c) => (
                                <span
                                  key={c}
                                  className="h-3 w-3 rounded-full"
                                  style={{ backgroundColor: c }}
                                />
                              ))}
                            </div>
                            {active && (
                              <CheckCircle2
                                className="h-3.5 w-3.5"
                                style={{ color: 'var(--tc-primary)' }}
                              />
                            )}
                          </div>
                          <div className="text-[11px] font-bold">{pal.name}</div>
                        </button>
                      );
                    })}
                  </div>
                </div>

                {/* Vercel AI Model Selector */}
                <div
                  className="rounded-2xl p-2.5"
                  style={{ backgroundColor: 'var(--tc-surface-low)' }}
                >
                  <div
                    className="mb-1.5 text-[10px] font-bold uppercase tracking-wider"
                    style={{ color: 'var(--tc-primary)' }}
                  >
                    Vercel AI Gateway Model
                  </div>
                  <div className="space-y-1.5">
                    {[
                      { id: 'minimax/minimax-m3.1-flash', label: 'MiniMax M3.1 Flash' },
                      { id: 'meta/muse-ember', label: 'Meta Muse Ember' },
                    ].map((m) => {
                      const active = selectedModel === m.id;
                      return (
                        <button
                          key={m.id}
                          onClick={() => onSelectModel(m.id)}
                          className="tc-pressable flex w-full cursor-pointer items-center justify-between rounded-xl px-2.5 py-2 text-left"
                          style={{
                            backgroundColor: active
                              ? 'var(--tc-primary-container)'
                              : 'var(--tc-surface-high)',
                            color: active
                              ? 'var(--tc-on-primary-container)'
                              : 'var(--tc-on-surface)',
                          }}
                        >
                          <div>
                            <div className="text-[11px] font-bold">{m.label}</div>
                            <div className="font-mono-tech text-[9px] opacity-75">
                              {m.id}
                            </div>
                          </div>
                          {active && <Check className="h-3.5 w-3.5" />}
                        </button>
                      );
                    })}
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* Expressive Bottom Navigation Bar (4 Tabs) */}
          <div
            className="relative z-10 grid grid-cols-4 border-t px-2 pt-2 pb-3"
            style={{
              backgroundColor: 'var(--tc-surface-low)',
              borderColor: 'var(--tc-outline-variant)',
            }}
          >
            {(
              [
                ['alarms', 'Alarms', AlarmClock],
                ['focus', 'Focus', Timer],
                ['insights', 'Insights', BarChart3],
                ['settings', 'Settings', Settings],
              ] as const
            ).map(([id, label, Icon]) => {
              const active = tab === id;
              return (
                <button
                  key={id}
                  onClick={() => switchTab(id)}
                  className="tc-pressable flex cursor-pointer flex-col items-center gap-0.5"
                >
                  <div
                    className="relative flex h-7 w-12 items-center justify-center rounded-full transition-all duration-200"
                    style={{
                      backgroundColor: active
                        ? 'var(--tc-primary-container)'
                        : 'transparent',
                      color: active
                        ? 'var(--tc-on-primary-container)'
                        : 'var(--tc-on-surface-variant)',
                    }}
                  >
                    <Icon
                      className="h-4 w-4 transition-transform duration-200"
                      style={{ transform: active ? 'scale(1.1)' : 'scale(1)' }}
                    />
                    {id === 'focus' && pomoRunning && (
                      <span
                        className="absolute top-1 right-2 h-1.5 w-1.5 rounded-full animate-tc-pulse"
                        style={{ backgroundColor: 'var(--tc-tertiary)' }}
                      />
                    )}
                  </div>
                  <span
                    className="text-[10px] font-semibold"
                    style={{
                      color: active
                        ? 'var(--tc-on-surface)'
                        : 'var(--tc-on-surface-variant)',
                    }}
                  >
                    {label}
                  </span>
                </button>
              );
            })}
          </div>

          {/* FULL-SCREEN RINGING TAKEOVER OVERLAY */}
          {isRinging && (
            <div
              className="absolute inset-0 z-30 flex flex-col justify-between p-5"
              style={{
                backgroundColor: 'var(--tc-bg)',
                backgroundImage:
                  'radial-gradient(circle at 50% 25%, color-mix(in srgb, var(--tc-primary) 26%, transparent), transparent 65%)',
              }}
            >
              <div className="flex items-center justify-between pt-5">
                <span
                  className="inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider"
                  style={{
                    backgroundColor: 'var(--tc-tertiary-container)',
                    color: 'var(--tc-tertiary)',
                  }}
                >
                  <Volume2 className="h-3 w-3" />
                  MAX STREAM_ALARM
                </span>
                <button
                  onClick={() => setIsRinging(false)}
                  aria-label="Close ringing demo"
                  className="tc-pressable flex h-7 w-7 cursor-pointer items-center justify-center rounded-full"
                  style={{ backgroundColor: 'var(--tc-surface-high)' }}
                >
                  <X className="h-4 w-4" />
                </button>
              </div>

              {ringVictory ? (
                <div className="my-auto flex flex-col items-center text-center space-y-3">
                  <div
                    className="flex h-20 w-20 items-center justify-center rounded-full"
                    style={{
                      backgroundColor: 'var(--tc-primary-container)',
                      color: 'var(--tc-primary)',
                    }}
                  >
                    <Check className="h-10 w-10 stroke-[2.6]" />
                  </div>
                  <div className="text-xl font-bold">You’re Awake!</div>
                  <p className="text-xs" style={{ color: 'var(--tc-on-surface-variant)' }}>
                    5-min anti-relapse wake check armed. Ready to jump straight into a 25m focus block?
                  </p>
                  <button
                    onClick={() => {
                      setIsRinging(false);
                      switchTab('focus');
                      setPomoRunning(true);
                    }}
                    className="tc-pressable w-full cursor-pointer rounded-2xl py-2.5 text-xs font-bold"
                    style={{
                      backgroundColor: 'var(--tc-primary)',
                      color: 'var(--tc-on-primary)',
                    }}
                  >
                    Launch 25m Focus Sprint
                  </button>
                </div>
              ) : (
                <div className={`my-auto space-y-4 ${ringShake ? 'animate-tc-shake' : ''}`}>
                  <div className="text-center">
                    <div className="font-mono-tech text-4xl font-bold">06:30</div>
                    <div className="mt-1 text-xs font-semibold" style={{ color: 'var(--tc-primary)' }}>
                      Get up for college — Step {ringStep + 1} of 2
                    </div>
                  </div>

                  {/* Step indicator */}
                  <div className="grid grid-cols-2 gap-1.5">
                    <div className="h-1.5 rounded-full" style={{ backgroundColor: 'var(--tc-primary)' }} />
                    <div
                      className="h-1.5 rounded-full transition-colors"
                      style={{
                        backgroundColor:
                          ringStep >= 1 ? 'var(--tc-primary)' : 'var(--tc-surface-highest)',
                      }}
                    />
                  </div>

                  {ringStep === 0 ? (
                    <div
                      className="rounded-2xl p-4 text-center"
                      style={{ backgroundColor: 'var(--tc-surface-low)' }}
                    >
                      <div className="text-[10px] font-bold uppercase tracking-wider opacity-70">
                        Stroop Ink-Color Test
                      </div>
                      <div className="mt-1 text-[11px] opacity-80">
                        Tap the <strong>INK COLOR</strong>, not the word:
                      </div>
                      <div className="my-3 text-3xl font-extrabold tracking-widest text-emerald-400">
                        CRIMSON
                      </div>
                      <div className="grid grid-cols-2 gap-2">
                        {[
                          { name: 'Red', ok: false },
                          { name: 'Green', ok: true },
                          { name: 'Blue', ok: false },
                          { name: 'Yellow', ok: false },
                        ].map((opt) => (
                          <button
                            key={opt.name}
                            onClick={() => handleRingAnswer(opt.ok)}
                            className="tc-pressable cursor-pointer rounded-xl py-2 text-xs font-bold"
                            style={{
                              backgroundColor: 'var(--tc-surface-high)',
                              border: '1px solid var(--tc-outline-variant)',
                            }}
                          >
                            {opt.name}
                          </button>
                        ))}
                      </div>
                    </div>
                  ) : (
                    <div
                      className="rounded-2xl p-4 text-center"
                      style={{ backgroundColor: 'var(--tc-surface-low)' }}
                    >
                      <div className="text-[10px] font-bold uppercase tracking-wider opacity-70">
                        Arithmetic Chain
                      </div>
                      <div className="my-3 font-mono-tech text-2xl font-bold">
                        (14 × 3) + 19 = ?
                      </div>
                      <div className="grid grid-cols-2 gap-2">
                        {[
                          { label: '57', ok: false },
                          { label: '61', ok: true },
                          { label: '63', ok: false },
                          { label: '59', ok: false },
                        ].map((opt) => (
                          <button
                            key={opt.label}
                            onClick={() => handleRingAnswer(opt.ok)}
                            className="tc-pressable cursor-pointer rounded-xl py-2 font-mono-tech text-sm font-bold"
                            style={{
                              backgroundColor: 'var(--tc-surface-high)',
                              border: '1px solid var(--tc-outline-variant)',
                            }}
                          >
                            {opt.label}
                          </button>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              )}

              <div className="text-center text-[10px] opacity-60">
                Volume keys & Back locked until solved
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
