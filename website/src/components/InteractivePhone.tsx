import React, { useEffect, useRef, useState } from 'react';
import {
  AlarmClock,
  BarChart3,
  BellRing,
  Check,
  Clock,
  Flame,
  MapPin,
  Moon,
  Pause,
  Play,
  Plus,
  Radar,
  RotateCcw,
  Settings,
  ShieldCheck,
  SkipForward,
  Sparkles,
  Sun,
  Timer,
  Volume2,
  X,
} from 'lucide-react';
import { PaletteId, TIMECURE_PALETTES } from '../themes';
import { TimecureLogo } from './TimecureLogo';
import {
  AiSheetOverlay,
  AlarmEditorOverlay,
  MISSION_META,
  MissionId,
  MissionSuiteOverlay,
  PhoneScreenTarget,
  PhoneTab,
  PreventPowerOffOverlay,
  SimAlarm,
  SimPlace,
  SimSmartTask,
  SimStudyTask,
  TaskAlertOverlay,
  WakeCheckOverlay,
} from './InteractivePhoneOverlays';

export interface ScreenJumpRequest {
  target: PhoneScreenTarget;
  seq: number;
}

interface TouchRipplePoint {
  id: number;
  x: number;
  y: number;
}

interface InteractivePhoneProps {
  paletteId: PaletteId;
  darkMode: boolean;
  onSelectPalette: (id: PaletteId) => void;
  onToggleDarkMode: () => void;
  screenJump?: ScreenJumpRequest | null;
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
    missions: ['math', 'step'],
    wakeCheckMinutes: 2,
    preventPowerOff: true,
    extraLoud: true,
    voiceReminders: true,
    wallpaper: 'Aurora Mesh',
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
    missions: ['color_tiles'],
    wakeCheckMinutes: 1,
    preventPowerOff: false,
    extraLoud: false,
    voiceReminders: true,
    wallpaper: 'Deep Night',
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
    missions: ['typing'],
    wakeCheckMinutes: 0,
    preventPowerOff: false,
    extraLoud: false,
    voiceReminders: false,
    wallpaper: 'Minimal',
  },
];

const INITIAL_STUDY_TASKS: SimStudyTask[] = [
  { id: 1, slot: '06:30 AM', task: 'Calculus III Lecture Prep', tech: 'Active Recall', done: true },
  { id: 2, slot: '09:00 AM', task: 'OS Kernel Lab (Threads)', tech: '25m Pomodoro', done: false },
  { id: 3, slot: '04:30 PM', task: 'System Design Notes', tech: 'Feynman Method', done: false },
];

const INITIAL_PLACES: SimPlace[] = [
  { id: 1, name: 'College Campus', tag: 'College', radius: 150, coords: '28.6139, 77.2090' },
  { id: 2, name: 'Spice Garden Restaurant', tag: 'Restaurant', radius: 120, coords: '28.6182, 77.2141' },
  { id: 3, name: 'Central Library', tag: 'Study', radius: 100, coords: '28.6104, 77.2063' },
];

const INITIAL_SMART_TASKS: SimSmartTask[] = [
  {
    id: 1,
    title: 'Submit OS Lab Journal & Silence Phone',
    triggerType: 'location',
    scheduleOrPlace: 'College Campus (150m)',
    mode: 'On arrive • Alarm Ring',
    enabled: true,
    done: false,
  },
  {
    id: 2,
    title: 'Pick up Parcel Receipt at Restaurant',
    triggerType: 'location',
    scheduleOrPlace: 'Spice Garden Restaurant (120m)',
    mode: 'On arrive • Notification',
    enabled: true,
    done: false,
  },
  {
    id: 3,
    title: 'Evening DSA Spaced-Repetition Drill',
    triggerType: 'time',
    scheduleOrPlace: '08:30 PM • Mon–Fri',
    mode: 'Exact Alarm • Repeat',
    enabled: true,
    done: false,
  },
];

export const InteractivePhone: React.FC<InteractivePhoneProps> = ({
  paletteId,
  darkMode,
  onSelectPalette,
  onToggleDarkMode,
  screenJump,
}) => {
  const displayRef = useRef<HTMLDivElement | null>(null);
  const dialCanvasRef = useRef<HTMLCanvasElement | null>(null);

  const [tab, setTab] = useState<PhoneTab>('alarms');
  const [sweepKey, setSweepKey] = useState(0);
  const [ripples, setRipples] = useState<TouchRipplePoint[]>([]);

  // Persistent interactive state across screens
  const [userName, setUserName] = useState('Satyam');
  const [defaultGoal, setDefaultGoal] = useState('Get up for college');
  const [difficulty, setDifficulty] = useState<'Easy' | 'Medium' | 'Hard'>('Medium');
  const [alarms, setAlarms] = useState<SimAlarm[]>(INITIAL_ALARMS);
  const [studyTasks, setStudyTasks] = useState<SimStudyTask[]>(INITIAL_STUDY_TASKS);
  const [places, setPlaces] = useState<SimPlace[]>(INITIAL_PLACES);
  const [smartTasks, setSmartTasks] = useState<SimSmartTask[]>(INITIAL_SMART_TASKS);
  const [radarActive, setRadarActive] = useState(true);
  const [tasksSubTab, setTasksSubTab] = useState<'tasks' | 'places'>('tasks');
  const [newPlaceName, setNewPlaceName] = useState('');
  const [newPlaceRadius, setNewPlaceRadius] = useState(150);

  // Overlays
  const [editingAlarm, setEditingAlarm] = useState<SimAlarm | null>(null);
  const [missionOverlayOpen, setMissionOverlayOpen] = useState(false);
  const [activeMission, setActiveMission] = useState<MissionId | null>(null);
  const [showWakeCheck, setShowWakeCheck] = useState(false);
  const [showPowerOffGuard, setShowPowerOffGuard] = useState(false);
  const [showAiSheet, setShowAiSheet] = useState(false);
  const [showTaskAlert, setShowTaskAlert] = useState(false);
  const [isRinging, setIsRinging] = useState(false);
  const [ringStep, setRingStep] = useState(0);
  const [ringShake, setRingShake] = useState(false);
  const [ringVictory, setRingVictory] = useState(false);

  // Pomodoro state
  const [pomoPhase, setPomoPhase] = useState<'focus' | 'short' | 'long'>('focus');
  const [pomoPresetMins, setPomoPresetMins] = useState(25);
  const [pomoRunning, setPomoRunning] = useState(true);
  const [pomoTotal, setPomoTotal] = useState(25 * 60);
  const [pomoRemaining, setPomoRemaining] = useState(19 * 60 + 42);

  const closeAllOverlays = () => {
    setEditingAlarm(null);
    setMissionOverlayOpen(false);
    setActiveMission(null);
    setShowWakeCheck(false);
    setShowPowerOffGuard(false);
    setShowAiSheet(false);
    setShowTaskAlert(false);
    setIsRinging(false);
  };

  const switchTab = (next: PhoneTab) => {
    closeAllOverlays();
    if (next !== tab) {
      setTab(next);
      setSweepKey((k) => k + 1);
    }
  };

  // External screen jump listener from App Showcase
  useEffect(() => {
    if (!screenJump) return;
    const t = screenJump.target;
    closeAllOverlays();
    if (t.kind === 'tab') {
      setTab(t.tab);
      setSweepKey((k) => k + 1);
    } else if (t.kind === 'alarm_editor') {
      setTab('alarms');
      setEditingAlarm(alarms[0]);
    } else if (t.kind === 'mission_picker') {
      setTab('alarms');
      setMissionOverlayOpen(true);
      setActiveMission(null);
    } else if (t.kind === 'mission_play') {
      setTab('alarms');
      setMissionOverlayOpen(true);
      setActiveMission(t.mission);
    } else if (t.kind === 'wake_check') {
      setShowWakeCheck(true);
    } else if (t.kind === 'prevent_power_off') {
      setShowPowerOffGuard(true);
    } else if (t.kind === 'ringing') {
      setRingStep(0);
      setRingVictory(false);
      setIsRinging(true);
    } else if (t.kind === 'ai_sheet') {
      setTab('alarms');
      setShowAiSheet(true);
    } else if (t.kind === 'task_alert') {
      setTab('tasks');
      setShowTaskAlert(true);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [screenJump]);

  // Android pointer touch ripple simulation at exact tap coordinates
  const handleTouchCapture = (e: React.PointerEvent<HTMLDivElement>) => {
    const el = displayRef.current;
    if (!el) return;
    const rect = el.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;
    const id = Date.now() + Math.random();
    setRipples((prev) => [...prev.slice(-4), { id, x, y }]);
    window.setTimeout(() => {
      setRipples((prev) => prev.filter((r) => r.id !== id));
    }, 460);
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
    const size = 172;
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

      if (pomoRunning) {
        ctx.save();
        ctx.globalAlpha = 0.2 + 0.14 * breath;
        ctx.fillStyle = halo;
        ctx.beginPath();
        ctx.arc(cx, cy, ringR * (0.85 + 0.04 * breath), 0, Math.PI * 2);
        ctx.fill();
        ctx.restore();
      }

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

      ctx.beginPath();
      ctx.arc(cx, cy, ringR, 0, Math.PI * 2);
      ctx.strokeStyle = track;
      ctx.lineWidth = 7.5;
      ctx.stroke();

      const sweep = Math.PI * 2 * progress;
      ctx.beginPath();
      ctx.arc(cx, cy, ringR, -Math.PI / 2, -Math.PI / 2 + sweep);
      ctx.strokeStyle = accent;
      ctx.lineWidth = 7.5;
      ctx.lineCap = 'round';
      ctx.stroke();

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

  const setPhase = (p: 'focus' | 'short' | 'long', customFocusMins = pomoPresetMins) => {
    setPomoPhase(p);
    const secs = p === 'focus' ? customFocusMins * 60 : p === 'short' ? 5 * 60 : 15 * 60;
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

  return (
    <div className="relative mx-auto w-[316px] sm:w-[338px] select-none">
      {/* Ambient aura */}
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
        {/* Inner Display with Touch Simulation Capture */}
        <div
          ref={displayRef}
          onPointerDownCapture={handleTouchCapture}
          className="relative flex h-[640px] flex-col overflow-hidden rounded-[36px] transition-colors duration-300"
          style={{
            backgroundColor: 'var(--tc-bg)',
            color: 'var(--tc-on-surface)',
          }}
        >
          {/* Touch Simulation Ripple Layer */}
          {ripples.map((r) => (
            <span
              key={r.id}
              className="pointer-events-none absolute z-50 h-11 w-11 rounded-full animate-tc-touch-ripple"
              style={{
                left: r.x,
                top: r.y,
                border: '2px solid var(--tc-primary)',
                backgroundColor: 'color-mix(in srgb, var(--tc-primary) 28%, transparent)',
              }}
            />
          ))}

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

          {/* Phase sweep line */}
          <div className="relative h-[2px] w-full overflow-hidden">
            <div
              key={sweepKey}
              className="animate-tc-sweep h-full w-1/2 rounded-full"
              style={{ backgroundColor: 'var(--tc-primary)' }}
            />
          </div>

          {/* Main Tab Content */}
          <div className="relative flex-1 overflow-y-auto px-4 pt-2 pb-3 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
            {/* TAB 1: ALARMS */}
            {tab === 'alarms' && (
              <div className="space-y-3 animate-[fadeIn_180ms_ease]">
                <div className="flex items-center justify-between pt-0.5">
                  <div>
                    <div className="text-[11px] font-medium" style={{ color: 'var(--tc-on-surface-variant)' }}>
                      Good morning, {userName || 'Friend'}
                    </div>
                    <div className="text-lg font-bold tracking-tight">Timecure</div>
                  </div>
                  <button
                    onClick={() => setShowAiSheet(true)}
                    title="Open Timecure AI"
                    className="tc-pressable cursor-pointer"
                  >
                    <TimecureLogo size={34} />
                  </button>
                </div>

                {/* Hero card */}
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
                    <div className="font-mono-tech text-2xl font-bold">
                      {alarms[0]?.time || '06:30'} {alarms[0]?.ampm || 'AM'}
                    </div>
                    <span className="rounded-full bg-black/15 px-2.5 py-0.5 text-[10px] font-semibold">
                      14d streak
                    </span>
                  </div>
                  <div className="mt-1 text-xs opacity-90">
                    “{alarms[0]?.prompt || defaultGoal}”
                  </div>
                  <div className="mt-2.5 grid grid-cols-2 gap-1.5">
                    <button
                      onClick={() => {
                        setRingStep(0);
                        setRingVictory(false);
                        setIsRinging(true);
                      }}
                      className="tc-pressable flex cursor-pointer items-center justify-center gap-1.5 rounded-xl py-2 text-[11px] font-bold"
                      style={{
                        backgroundColor: 'var(--tc-primary)',
                        color: 'var(--tc-on-primary)',
                      }}
                    >
                      <BellRing className="h-3.5 w-3.5" />
                      Test Ring
                    </button>
                    <button
                      onClick={() => setShowWakeCheck(true)}
                      className="tc-pressable flex cursor-pointer items-center justify-center gap-1 rounded-xl bg-black/15 py-2 text-[11px] font-bold"
                    >
                      <Clock className="h-3.5 w-3.5" />
                      3-2-1 Check
                    </button>
                  </div>
                </div>

                {/* Alarms Header + New Button */}
                <div className="flex items-center justify-between">
                  <span className="text-[11px] font-bold uppercase tracking-wider opacity-70">
                    Tap any alarm to customize
                  </span>
                  <button
                    onClick={() =>
                      setEditingAlarm({
                        id: Date.now(),
                        time: '07:15',
                        ampm: 'AM',
                        label: 'Morning Focus',
                        prompt: `${defaultGoal} — no excuses`,
                        days: 'Mon – Fri',
                        difficulty: 'Medium' as never,
                        enabled: true,
                        missions: ['math'],
                        wakeCheckMinutes: 2,
                        preventPowerOff: true,
                        extraLoud: true,
                        voiceReminders: true,
                        wallpaper: 'Aurora Mesh',
                      })
                    }
                    className="tc-pressable inline-flex cursor-pointer items-center gap-1 rounded-full px-2.5 py-1 text-[10px] font-bold"
                    style={{
                      backgroundColor: 'var(--tc-primary-container)',
                      color: 'var(--tc-on-primary-container)',
                    }}
                  >
                    <Plus className="h-3 w-3" />
                    Add Alarm
                  </button>
                </div>

                {/* Alarm Cards */}
                <div className="space-y-2">
                  {alarms.map((alarm) => (
                    <div
                      key={alarm.id}
                      onClick={() => setEditingAlarm(alarm)}
                      className="tc-pressable cursor-pointer rounded-2xl p-3 transition-all duration-200"
                      style={{
                        backgroundColor: 'var(--tc-surface-low)',
                        border: '1px solid var(--tc-outline-variant)',
                        opacity: alarm.enabled ? 1 : 0.62,
                      }}
                    >
                      <div className="flex items-center justify-between">
                        <div>
                          <div className="flex items-baseline gap-1">
                            <span className="font-mono-tech text-xl font-bold">{alarm.time}</span>
                            <span className="text-[10px] font-semibold" style={{ color: 'var(--tc-on-surface-variant)' }}>
                              {alarm.ampm}
                            </span>
                          </div>
                          <div className="text-xs font-semibold">{alarm.label}</div>
                        </div>
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            setAlarms((prev) =>
                              prev.map((a) => (a.id === alarm.id ? { ...a, enabled: !a.enabled } : a))
                            );
                          }}
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
                              transform: alarm.enabled ? 'translateX(20px)' : 'translateX(0)',
                            }}
                          />
                        </button>
                      </div>
                      <div
                        className="mt-1.5 flex flex-wrap items-center gap-1 text-[9px]"
                        style={{ color: 'var(--tc-on-surface-variant)' }}
                      >
                        {alarm.missions.map((m) => (
                          <span
                            key={m}
                            className="rounded-full px-1.5 py-0.5 font-semibold"
                            style={{
                              backgroundColor: 'var(--tc-surface-high)',
                              color: 'var(--tc-primary)',
                            }}
                          >
                            {MISSION_META[m].name}
                          </span>
                        ))}
                        {alarm.preventPowerOff && (
                          <span
                            className="inline-flex items-center gap-0.5 rounded-full px-1.5 py-0.5 font-semibold"
                            style={{
                              backgroundColor: 'var(--tc-tertiary-container)',
                              color: 'var(--tc-tertiary)',
                            }}
                          >
                            <ShieldCheck className="h-2.5 w-2.5" /> Power Guard
                          </span>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* TAB 2: FOCUS POMODORO */}
            {tab === 'focus' && (
              <div className="flex flex-col items-center space-y-2.5 pt-1">
                <div className="text-sm font-bold">Focus Pomodoro</div>

                {/* Preset duration chips */}
                <div className="grid w-full grid-cols-4 gap-1 text-[10px] font-bold">
                  {[
                    { name: 'Sprint', m: 15 },
                    { name: 'Classic', m: 25 },
                    { name: 'Deep', m: 50 },
                    { name: '90m', m: 90 },
                  ].map((p) => {
                    const active = pomoPresetMins === p.m && pomoPhase === 'focus';
                    return (
                      <button
                        key={p.name}
                        onClick={() => {
                          setPomoPresetMins(p.m);
                          setPhase('focus', p.m);
                        }}
                        className="tc-pressable cursor-pointer rounded-xl py-1.5"
                        style={{
                          backgroundColor: active
                            ? 'var(--tc-primary-container)'
                            : 'var(--tc-surface-low)',
                          color: active
                            ? 'var(--tc-on-primary-container)'
                            : 'var(--tc-on-surface-variant)',
                        }}
                      >
                        {p.name}
                      </button>
                    );
                  })}
                </div>

                {/* Segmented phase */}
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
                      className="tc-pressable cursor-pointer rounded-full py-1 transition-colors"
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
                  {defaultGoal} • Deep Study
                </div>

                {/* 60-tick Canvas Chronometer */}
                <div className="relative flex h-[172px] w-[172px] items-center justify-center">
                  <canvas ref={dialCanvasRef} className="h-[172px] w-[172px]" />
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
                          className="h-1.5 rounded-full"
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

                {/* Controls */}
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
                    className="tc-pressable flex h-10 cursor-pointer items-center justify-center gap-2 px-6 text-xs font-bold transition-all"
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
                    onClick={() => setPhase(pomoPhase === 'focus' ? 'short' : 'focus')}
                    aria-label="Skip phase"
                    className="tc-pressable flex h-10 w-10 cursor-pointer items-center justify-center rounded-full"
                    style={{ backgroundColor: 'var(--tc-surface-high)' }}
                  >
                    <SkipForward className="h-4 w-4" />
                  </button>
                </div>

                {/* Telemetry */}
                <div
                  className="grid w-full grid-cols-3 rounded-2xl p-2 text-center"
                  style={{ backgroundColor: 'var(--tc-surface-low)' }}
                >
                  <div>
                    <div className="font-mono-tech text-sm font-bold">115m</div>
                    <div className="text-[9px]" style={{ color: 'var(--tc-on-surface-variant)' }}>Today</div>
                  </div>
                  <div>
                    <div className="font-mono-tech text-sm font-bold">18</div>
                    <div className="text-[9px]" style={{ color: 'var(--tc-on-surface-variant)' }}>Blocks</div>
                  </div>
                  <div>
                    <div className="font-mono-tech text-sm font-bold">4</div>
                    <div className="text-[9px]" style={{ color: 'var(--tc-on-surface-variant)' }}>Streak</div>
                  </div>
                </div>
              </div>
            )}

            {/* TAB 3 (CENTER): TIME & LOCATION GPS TASKS */}
            {tab === 'tasks' && (
              <div className="space-y-2.5 pt-1">
                <div className="flex items-center justify-between">
                  <div>
                    <div className="text-sm font-bold">Smart Tasks</div>
                    <div className="text-[10px]" style={{ color: 'var(--tc-on-surface-variant)' }}>
                      GPS Geofence &amp; Scheduled Triggers
                    </div>
                  </div>
                  <button
                    onClick={() => setRadarActive((r) => !r)}
                    className="tc-pressable inline-flex cursor-pointer items-center gap-1 rounded-full px-2.5 py-1 text-[10px] font-bold"
                    style={{
                      backgroundColor: radarActive
                        ? 'var(--tc-primary-container)'
                        : 'var(--tc-surface-low)',
                      color: radarActive
                        ? 'var(--tc-on-primary-container)'
                        : 'var(--tc-on-surface-variant)',
                    }}
                  >
                    <Radar className={`h-3 w-3 ${radarActive ? 'animate-tc-pulse' : ''}`} />
                    {radarActive ? 'Radar On' : 'Paused'}
                  </button>
                </div>

                {/* Sub-tabs */}
                <div
                  className="grid grid-cols-2 rounded-full p-0.5 text-[11px] font-bold"
                  style={{
                    backgroundColor: 'var(--tc-surface-low)',
                    border: '1px solid var(--tc-outline-variant)',
                  }}
                >
                  <button
                    onClick={() => setTasksSubTab('tasks')}
                    className="tc-pressable cursor-pointer rounded-full py-1.5"
                    style={{
                      backgroundColor:
                        tasksSubTab === 'tasks' ? 'var(--tc-primary-container)' : 'transparent',
                      color:
                        tasksSubTab === 'tasks'
                          ? 'var(--tc-on-primary-container)'
                          : 'var(--tc-on-surface-variant)',
                    }}
                  >
                    Tasks ({smartTasks.length})
                  </button>
                  <button
                    onClick={() => setTasksSubTab('places')}
                    className="tc-pressable cursor-pointer rounded-full py-1.5"
                    style={{
                      backgroundColor:
                        tasksSubTab === 'places' ? 'var(--tc-primary-container)' : 'transparent',
                      color:
                        tasksSubTab === 'places'
                          ? 'var(--tc-on-primary-container)'
                          : 'var(--tc-on-surface-variant)',
                    }}
                  >
                    Places ({places.length})
                  </button>
                </div>

                {tasksSubTab === 'tasks' ? (
                  <div className="space-y-2">
                    {smartTasks.map((t) => (
                      <div
                        key={t.id}
                        className="rounded-2xl p-2.5 space-y-1.5"
                        style={{
                          backgroundColor: 'var(--tc-surface-low)',
                          border: '1px solid var(--tc-outline-variant)',
                          opacity: t.enabled ? 1 : 0.6,
                        }}
                      >
                        <div className="flex items-start justify-between gap-2">
                          <div className="flex items-start gap-2">
                            <button
                              onClick={() =>
                                setSmartTasks((prev) =>
                                  prev.map((x) => (x.id === t.id ? { ...x, done: !x.done } : x))
                                )
                              }
                              className="mt-0.5 flex h-4 w-4 shrink-0 cursor-pointer items-center justify-center rounded border"
                              style={{
                                backgroundColor: t.done ? 'var(--tc-primary)' : 'transparent',
                                borderColor: t.done ? 'var(--tc-primary)' : 'var(--tc-outline)',
                                color: 'var(--tc-on-primary)',
                              }}
                            >
                              {t.done && <Check className="h-3 w-3" />}
                            </button>
                            <div>
                              <div className={`text-[11px] font-bold ${t.done ? 'line-through opacity-55' : ''}`}>
                                {t.title}
                              </div>
                              <div className="mt-0.5 flex items-center gap-1 text-[9px]" style={{ color: 'var(--tc-on-surface-variant)' }}>
                                {t.triggerType === 'location' ? (
                                  <MapPin className="h-2.5 w-2.5" style={{ color: 'var(--tc-primary)' }} />
                                ) : (
                                  <Clock className="h-2.5 w-2.5" style={{ color: 'var(--tc-tertiary)' }} />
                                )}
                                {t.scheduleOrPlace}
                              </div>
                            </div>
                          </div>
                          <button
                            onClick={() =>
                              setSmartTasks((prev) =>
                                prev.map((x) => (x.id === t.id ? { ...x, enabled: !x.enabled } : x))
                              )
                            }
                            className="tc-pressable cursor-pointer rounded-full px-2 py-0.5 text-[9px] font-bold"
                            style={{
                              backgroundColor: t.enabled
                                ? 'var(--tc-primary)'
                                : 'var(--tc-surface-highest)',
                              color: t.enabled
                                ? 'var(--tc-on-primary)'
                                : 'var(--tc-on-surface-variant)',
                            }}
                          >
                            {t.enabled ? 'ON' : 'OFF'}
                          </button>
                        </div>
                        {t.triggerType === 'location' && (
                          <div className="flex items-center justify-between pt-0.5">
                            <span className="text-[9px] opacity-70">{t.mode}</span>
                            <button
                              onClick={() => setShowTaskAlert(true)}
                              className="tc-pressable cursor-pointer rounded-lg px-2 py-0.5 text-[9px] font-bold"
                              style={{
                                backgroundColor: 'var(--tc-tertiary-container)',
                                color: 'var(--tc-tertiary)',
                              }}
                            >
                              Simulate Arrive
                            </button>
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="space-y-2">
                    {/* Mark a new place inline */}
                    <div
                      className="rounded-2xl p-2.5 space-y-1.5"
                      style={{ backgroundColor: 'var(--tc-surface-low)' }}
                    >
                      <div className="text-[10px] font-bold" style={{ color: 'var(--tc-primary)' }}>
                        Mark Current Place (100m–500m)
                      </div>
                      <div className="flex gap-1.5">
                        <input
                          type="text"
                          value={newPlaceName}
                          onChange={(e) => setNewPlaceName(e.target.value)}
                          placeholder="e.g. Coffee Shop"
                          className="flex-1 rounded-lg px-2 py-1 text-[11px] outline-none"
                          style={{
                            backgroundColor: 'var(--tc-surface)',
                            border: '1px solid var(--tc-outline-variant)',
                          }}
                        />
                        <button
                          onClick={() => {
                            const label = newPlaceName.trim();
                            if (!label) return;
                            setPlaces((prev) => [
                              {
                                id: Date.now(),
                                name: label,
                                tag: 'Custom',
                                radius: newPlaceRadius,
                                coords: '28.6151, 77.2110',
                              },
                              ...prev,
                            ]);
                            setNewPlaceName('');
                          }}
                          className="tc-pressable cursor-pointer rounded-lg px-2.5 py-1 text-[10px] font-bold"
                          style={{
                            backgroundColor: 'var(--tc-primary)',
                            color: 'var(--tc-on-primary)',
                          }}
                        >
                          Mark
                        </button>
                      </div>
                      <div className="flex items-center justify-between text-[9px]">
                        <span>Fence radius</span>
                        <span className="font-mono-tech font-bold">{newPlaceRadius}m</span>
                      </div>
                      <input
                        type="range"
                        min={100}
                        max={500}
                        step={25}
                        value={newPlaceRadius}
                        onChange={(e) => setNewPlaceRadius(Number(e.target.value))}
                        className="w-full accent-[var(--tc-primary)]"
                      />
                    </div>

                    {places.map((p) => (
                      <div
                        key={p.id}
                        className="flex items-center justify-between rounded-2xl p-2.5"
                        style={{
                          backgroundColor: 'var(--tc-surface-low)',
                          border: '1px solid var(--tc-outline-variant)',
                        }}
                      >
                        <div className="flex items-center gap-2">
                          <MapPin className="h-4 w-4" style={{ color: 'var(--tc-primary)' }} />
                          <div>
                            <div className="text-[11px] font-bold">{p.name}</div>
                            <div className="font-mono-tech text-[9px] opacity-70">
                              {p.tag} • {p.radius}m • {p.coords}
                            </div>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}

            {/* TAB 4: INSIGHTS */}
            {tab === 'insights' && (
              <div className="space-y-3 pt-1">
                <div className="text-center text-sm font-bold">Insights &amp; Dojo</div>

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
                        color: m.hi ? 'var(--tc-on-primary-container)' : 'var(--tc-on-surface)',
                      }}
                    >
                      <div className="font-mono-tech text-lg font-bold">{m.value}</div>
                      <div className="text-[10px] opacity-80">{m.label}</div>
                    </div>
                  ))}
                </div>

                <div className="rounded-2xl p-3" style={{ backgroundColor: 'var(--tc-surface-low)' }}>
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
                            backgroundColor: i === 6 ? 'var(--tc-tertiary)' : 'var(--tc-primary)',
                            opacity: i === 6 ? 1 : 0.78,
                          }}
                        />
                        <span className="text-[9px] font-mono-tech" style={{ color: 'var(--tc-on-surface-variant)' }}>
                          {['M', 'T', 'W', 'T', 'F', 'S', 'S'][i]}
                        </span>
                      </div>
                    ))}
                  </div>
                </div>

                {/* AI Vision Study Table */}
                <div className="rounded-2xl p-3" style={{ backgroundColor: 'var(--tc-surface-low)' }}>
                  <div className="mb-2 flex items-center justify-between text-[11px] font-semibold">
                    <span className="flex items-center gap-1.5">
                      <Sparkles className="h-3.5 w-3.5" style={{ color: 'var(--tc-primary)' }} />
                      AI Vision Task Table
                    </span>
                    <button
                      onClick={() => setShowAiSheet(true)}
                      className="cursor-pointer text-[10px] underline"
                      style={{ color: 'var(--tc-primary)' }}
                    >
                      Scan new
                    </button>
                  </div>
                  <div className="space-y-1.5">
                    {studyTasks.map((t) => (
                      <button
                        key={t.id}
                        onClick={() =>
                          setStudyTasks((prev) =>
                            prev.map((x) => (x.id === t.id ? { ...x, done: !x.done } : x))
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
                          <div className={`truncate text-[11px] font-medium ${t.done ? 'line-through opacity-55' : ''}`}>
                            {t.task}
                          </div>
                          <div className="text-[9px]" style={{ color: 'var(--tc-on-surface-variant)' }}>
                            {t.slot} • {t.tech}
                          </div>
                        </div>
                      </button>
                    ))}
                  </div>
                </div>
              </div>
            )}

            {/* TAB 5: SETTINGS (Clean, Zero AI settings / model names) */}
            {tab === 'settings' && (
              <div className="space-y-3 pt-1">
                <div className="text-center text-sm font-bold">Settings</div>

                {/* Personalization */}
                <div className="rounded-2xl p-2.5 space-y-2" style={{ backgroundColor: 'var(--tc-surface-low)' }}>
                  <div className="text-[10px] font-bold uppercase tracking-wider" style={{ color: 'var(--tc-primary)' }}>
                    Personalization
                  </div>
                  <input
                    type="text"
                    value={userName}
                    onChange={(e) => setUserName(e.target.value)}
                    placeholder="Your name"
                    className="w-full rounded-lg px-2.5 py-1.5 text-[11px] outline-none"
                    style={{
                      backgroundColor: 'var(--tc-surface)',
                      border: '1px solid var(--tc-outline-variant)',
                    }}
                  />
                  <input
                    type="text"
                    value={defaultGoal}
                    onChange={(e) => setDefaultGoal(e.target.value)}
                    placeholder="Default morning goal"
                    className="w-full rounded-lg px-2.5 py-1.5 text-[11px] outline-none"
                    style={{
                      backgroundColor: 'var(--tc-surface)',
                      border: '1px solid var(--tc-outline-variant)',
                    }}
                  />
                  <div className="grid grid-cols-3 gap-1">
                    {(['Easy', 'Medium', 'Hard'] as const).map((d) => (
                      <button
                        key={d}
                        onClick={() => setDifficulty(d)}
                        className="tc-pressable cursor-pointer rounded-lg py-1 text-[10px] font-bold"
                        style={{
                          backgroundColor:
                            difficulty === d ? 'var(--tc-primary-container)' : 'var(--tc-surface)',
                          color:
                            difficulty === d
                              ? 'var(--tc-on-primary-container)'
                              : 'var(--tc-on-surface-variant)',
                        }}
                      >
                        {d}
                      </button>
                    ))}
                  </div>
                </div>

                {/* Appearance */}
                <div
                  className="flex items-center justify-between rounded-2xl p-2.5"
                  style={{ backgroundColor: 'var(--tc-surface-low)' }}
                >
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
                  <div className="mb-1.5 text-[10px] font-bold uppercase tracking-wider" style={{ color: 'var(--tc-primary)' }}>
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
                          <div className="flex gap-1">
                            {pal.preview.map((c) => (
                              <span
                                key={c}
                                className="h-2.5 w-2.5 rounded-full"
                                style={{ backgroundColor: c }}
                              />
                            ))}
                          </div>
                          <div className="text-[11px] font-bold">{pal.name}</div>
                        </button>
                      );
                    })}
                  </div>
                </div>

                {/* Reliability & Testing */}
                <button
                  onClick={() => {
                    setRingStep(0);
                    setRingVictory(false);
                    setIsRinging(true);
                  }}
                  className="tc-pressable flex w-full cursor-pointer items-center justify-center gap-1.5 rounded-2xl py-2.5 text-[11px] font-bold"
                  style={{
                    backgroundColor: 'var(--tc-surface-low)',
                    border: '1px solid var(--tc-outline-variant)',
                  }}
                >
                  <AlarmClock className="h-3.5 w-3.5" />
                  Simulate full lock-screen alarm
                </button>
              </div>
            )}
          </div>

          {/* Expressive 5-Tab Bottom Navigation Bar (Tasks Centered!) */}
          <div
            className="relative z-10 grid grid-cols-5 border-t px-1.5 pt-2 pb-3"
            style={{
              backgroundColor: 'var(--tc-surface-low)',
              borderColor: 'var(--tc-outline-variant)',
            }}
          >
            {(
              [
                ['alarms', 'Alarms', AlarmClock],
                ['focus', 'Focus', Timer],
                ['tasks', 'Tasks', MapPin],
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
                    className="relative flex h-7 w-11 items-center justify-center rounded-full transition-all duration-200"
                    style={{
                      backgroundColor: active ? 'var(--tc-primary-container)' : 'transparent',
                      color: active ? 'var(--tc-on-primary-container)' : 'var(--tc-on-surface-variant)',
                    }}
                  >
                    <Icon
                      className="h-4 w-4 transition-transform duration-200"
                      style={{ transform: active ? 'scale(1.1)' : 'scale(1)' }}
                    />
                    {id === 'focus' && pomoRunning && (
                      <span
                        className="absolute top-1 right-1.5 h-1.5 w-1.5 rounded-full animate-tc-pulse"
                        style={{ backgroundColor: 'var(--tc-tertiary)' }}
                      />
                    )}
                    {id === 'tasks' && radarActive && (
                      <span
                        className="absolute top-1 right-1.5 h-1.5 w-1.5 rounded-full animate-tc-pulse"
                        style={{ backgroundColor: 'var(--tc-primary)' }}
                      />
                    )}
                  </div>
                  <span
                    className="text-[9px] font-semibold"
                    style={{
                      color: active ? 'var(--tc-on-surface)' : 'var(--tc-on-surface-variant)',
                    }}
                  >
                    {label}
                  </span>
                </button>
              );
            })}
          </div>

          {/* SUB-SCREEN OVERLAYS */}
          {editingAlarm && (
            <AlarmEditorOverlay
              alarm={editingAlarm}
              onClose={() => setEditingAlarm(null)}
              onSave={(updated) => {
                setAlarms((prev) => {
                  const exists = prev.some((x) => x.id === updated.id);
                  return exists
                    ? prev.map((x) => (x.id === updated.id ? updated : x))
                    : [updated, ...prev];
                });
                setEditingAlarm(null);
              }}
              onOpenMissionPicker={() => {
                setActiveMission(null);
                setMissionOverlayOpen(true);
              }}
              onPlayMission={(m) => {
                setActiveMission(m);
                setMissionOverlayOpen(true);
              }}
              onTestWakeCheck={() => setShowWakeCheck(true)}
              onOpenPowerOffDetail={() => setShowPowerOffGuard(true)}
            />
          )}

          {missionOverlayOpen && (
            <MissionSuiteOverlay
              activeMission={activeMission}
              goal={editingAlarm?.label || defaultGoal}
              onSelectMission={() => setActiveMission(null)}
              onAddMissionAndPlay={(m) => {
                if (editingAlarm && !editingAlarm.missions.includes(m)) {
                  setEditingAlarm({ ...editingAlarm, missions: [...editingAlarm.missions, m] });
                }
                setActiveMission(m);
              }}
              onBackToPicker={() => setActiveMission(null)}
              onClose={() => {
                setMissionOverlayOpen(false);
                setActiveMission(null);
              }}
            />
          )}

          {showPowerOffGuard && (
            <PreventPowerOffOverlay
              enabled={editingAlarm?.preventPowerOff ?? true}
              onToggle={() => {
                if (editingAlarm) {
                  setEditingAlarm({
                    ...editingAlarm,
                    preventPowerOff: !editingAlarm.preventPowerOff,
                  });
                }
              }}
              onClose={() => setShowPowerOffGuard(false)}
            />
          )}

          {showWakeCheck && (
            <WakeCheckOverlay
              goal={editingAlarm?.label || defaultGoal}
              onAwake={() => setShowWakeCheck(false)}
              onReRing={() => {
                setShowWakeCheck(false);
                setRingStep(0);
                setRingVictory(false);
                setIsRinging(true);
              }}
            />
          )}

          {showAiSheet && (
            <AiSheetOverlay
              onClose={() => setShowAiSheet(false)}
              onScanTimetable={() => {
                setStudyTasks([
                  { id: 101, slot: '06:30 AM', task: 'Signals & Systems Lecture', tech: 'Active Recall', done: false },
                  { id: 102, slot: '09:00 AM', task: 'OS Process Scheduling Lab', tech: '25m Pomodoro', done: false },
                  { id: 103, slot: '04:00 PM', task: 'Compiler Design Problem Set', tech: 'Feynman Method', done: false },
                ]);
              }}
            />
          )}

          {showTaskAlert && (
            <TaskAlertOverlay
              onComplete={() => {
                setSmartTasks((prev) =>
                  prev.map((t, idx) => (idx === 0 ? { ...t, done: true } : t))
                );
                setShowTaskAlert(false);
              }}
              onClose={() => setShowTaskAlert(false)}
            />
          )}

          {/* LOCK-SCREEN RINGING OVERLAY */}
          {isRinging && (
            <div
              className="absolute inset-0 z-40 flex flex-col justify-between p-5"
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
                  MAX STREAM_ALARM +9dB
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
                <div className="my-auto flex flex-col items-center space-y-3 text-center">
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
                    2-min wake-up check armed. Ready to jump straight into a 25m focus block?
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
                      {defaultGoal} — Step {ringStep + 1} of 2
                    </div>
                  </div>

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
                    <div className="rounded-2xl p-4 text-center" style={{ backgroundColor: 'var(--tc-surface-low)' }}>
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
                    <div className="rounded-2xl p-4 text-center" style={{ backgroundColor: 'var(--tc-surface-low)' }}>
                      <div className="text-[10px] font-bold uppercase tracking-wider opacity-70">
                        Arithmetic Chain
                      </div>
                      <div className="my-3 font-mono-tech text-2xl font-bold">(14 × 3) + 19 = ?</div>
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
                Power-off guard &amp; key lock active until solved
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
