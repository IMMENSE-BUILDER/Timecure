import React, { useEffect, useState } from 'react';
import {
  Camera,
  Check,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  Clock,
  Footprints,
  Grid,
  Keyboard,
  Loader2,
  MapPin,
  Mic,
  Plus,
  Search,
  Send,
  ShieldCheck,
  Sparkles,
  SquareSplitHorizontal,
  Volume2,
  X,
} from 'lucide-react';

export type PhoneTab = 'alarms' | 'focus' | 'tasks' | 'insights' | 'settings';

export type MissionId =
  | 'item_hunt'
  | 'word_beat'
  | 'color_tiles'
  | 'typing'
  | 'math'
  | 'step';

export type PhoneScreenTarget =
  | { kind: 'tab'; tab: PhoneTab }
  | { kind: 'alarm_editor' }
  | { kind: 'mission_picker' }
  | { kind: 'mission_play'; mission: MissionId }
  | { kind: 'wake_check' }
  | { kind: 'prevent_power_off' }
  | { kind: 'ringing' }
  | { kind: 'ai_sheet' }
  | { kind: 'task_alert' };

export interface SimAlarm {
  id: number;
  time: string;
  ampm: string;
  label: string;
  prompt: string;
  days: string;
  difficulty: 'Gentle' | 'Sharp' | 'Relentless';
  enabled: boolean;
  missions: MissionId[];
  wakeCheckMinutes: number;
  preventPowerOff: boolean;
  extraLoud: boolean;
  voiceReminders: boolean;
  wallpaper: string;
}

export interface SimStudyTask {
  id: number;
  slot: string;
  task: string;
  tech: string;
  done: boolean;
}

export interface SimPlace {
  id: number;
  name: string;
  tag: string;
  radius: number;
  coords: string;
}

export interface SimSmartTask {
  id: number;
  title: string;
  triggerType: 'location' | 'time';
  scheduleOrPlace: string;
  mode: string;
  enabled: boolean;
  done: boolean;
}

export const MISSION_META: Record<
  MissionId,
  { name: string; group: 'Popular' | 'Wake your brain' | 'Wake your body'; badge?: string }
> = {
  item_hunt: { name: 'Household Item Hunt', group: 'Popular', badge: 'AI' },
  word_beat: { name: 'Say the word on beat', group: 'Popular', badge: 'NEW' },
  color_tiles: { name: 'Find Color Tiles', group: 'Wake your brain' },
  typing: { name: 'Typing', group: 'Wake your brain' },
  math: { name: 'Math', group: 'Wake your brain', badge: 'BEST' },
  step: { name: 'Step', group: 'Wake your body', badge: 'HOT' },
};

const MISSION_ICON: Record<MissionId, React.FC<{ className?: string }>> = {
  item_hunt: Search,
  word_beat: Mic,
  color_tiles: Grid,
  typing: Keyboard,
  math: SquareSplitHorizontal,
  step: Footprints,
};

/* ---------------- 1. WAKE-UP ALARM EDITOR ---------------- */
export const AlarmEditorOverlay: React.FC<{
  alarm: SimAlarm;
  onSave: (updated: SimAlarm) => void;
  onClose: () => void;
  onOpenMissionPicker: () => void;
  onPlayMission: (m: MissionId) => void;
  onTestWakeCheck: () => void;
  onOpenPowerOffDetail: () => void;
}> = ({
  alarm,
  onSave,
  onClose,
  onOpenMissionPicker,
  onPlayMission,
  onTestWakeCheck,
  onOpenPowerOffDetail,
}) => {
  const [draft, setDraft] = useState<SimAlarm>(alarm);
  const [daysActive, setDaysActive] = useState<boolean[]>([
    true,
    true,
    true,
    true,
    true,
    false,
    false,
  ]);
  const [volume, setVolume] = useState(95);
  const [sampleText, setSampleText] = useState<string | null>(null);

  const nudgeMinutes = (delta: number) => {
    const [hStr, mStr] = draft.time.split(':');
    let h = parseInt(hStr, 10) || 6;
    let m = parseInt(mStr, 10) || 30;
    m += delta;
    if (m >= 60) {
      m -= 60;
      h = h === 12 ? 1 : h + 1;
    } else if (m < 0) {
      m += 60;
      h = h === 1 ? 12 : h - 1;
    }
    setDraft((prev) => ({
      ...prev,
      time: `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`,
    }));
  };

  return (
    <div
      className="absolute inset-0 z-30 flex flex-col"
      style={{ backgroundColor: 'var(--tc-bg)', color: 'var(--tc-on-surface)' }}
    >
      {/* Top Bar */}
      <div className="flex items-center justify-between px-4 pt-7 pb-2">
        <button
          onClick={onClose}
          className="tc-pressable flex h-8 w-8 cursor-pointer items-center justify-center rounded-full"
          style={{ backgroundColor: 'var(--tc-surface-low)' }}
          aria-label="Close alarm editor"
        >
          <X className="h-4 w-4" />
        </button>
        <div className="text-xs font-bold">Wake-up alarm</div>
        <button
          onClick={() => onSave(draft)}
          className="tc-pressable cursor-pointer rounded-full px-3 py-1 text-[11px] font-bold"
          style={{
            backgroundColor: 'var(--tc-primary)',
            color: 'var(--tc-on-primary)',
          }}
        >
          Save
        </button>
      </div>

      {/* Scrollable Editor Body */}
      <div className="flex-1 space-y-2.5 overflow-y-auto px-4 pb-5 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
        {/* Label */}
        <input
          type="text"
          value={draft.label}
          onChange={(e) => setDraft({ ...draft, label: e.target.value })}
          className="w-full rounded-xl px-3 py-2 text-xs font-semibold outline-none"
          style={{
            backgroundColor: 'var(--tc-surface-low)',
            border: '1px solid var(--tc-outline-variant)',
          }}
        />

        {/* Wheel Time Picker Simulator */}
        <div
          className="flex items-center justify-between rounded-2xl px-4 py-3"
          style={{ backgroundColor: 'var(--tc-surface-low)' }}
        >
          <button
            onClick={() => nudgeMinutes(-15)}
            className="tc-pressable cursor-pointer rounded-lg px-2 py-1 text-[10px] font-bold"
            style={{ backgroundColor: 'var(--tc-surface-high)' }}
          >
            -15m
          </button>
          <div className="text-center">
            <div className="font-mono-tech text-2xl font-bold">
              {draft.time}{' '}
              <button
                onClick={() =>
                  setDraft({ ...draft, ampm: draft.ampm === 'AM' ? 'PM' : 'AM' })
                }
                className="cursor-pointer rounded-md px-1.5 py-0.5 text-xs"
                style={{
                  backgroundColor: 'var(--tc-primary-container)',
                  color: 'var(--tc-on-primary-container)',
                }}
              >
                {draft.ampm}
              </button>
            </div>
            <div className="text-[10px]" style={{ color: 'var(--tc-on-surface-variant)' }}>
              Tap -15m / +15m or AM/PM
            </div>
          </div>
          <button
            onClick={() => nudgeMinutes(15)}
            className="tc-pressable cursor-pointer rounded-lg px-2 py-1 text-[10px] font-bold"
            style={{ backgroundColor: 'var(--tc-surface-high)' }}
          >
            +15m
          </button>
        </div>

        {/* Repeat Days */}
        <div className="grid grid-cols-7 gap-1">
          {['S', 'M', 'T', 'W', 'T', 'F', 'S'].map((d, i) => {
            const on = daysActive[i];
            return (
              <button
                key={i}
                onClick={() =>
                  setDaysActive((prev) => prev.map((v, idx) => (idx === i ? !v : v)))
                }
                className="tc-pressable flex h-7 cursor-pointer items-center justify-center rounded-lg text-[10px] font-bold"
                style={{
                  backgroundColor: on
                    ? 'var(--tc-primary-container)'
                    : 'var(--tc-surface-low)',
                  color: on
                    ? 'var(--tc-on-primary-container)'
                    : 'var(--tc-on-surface-variant)',
                }}
              >
                {d}
              </button>
            );
          })}
        </div>

        {/* Wake-up Mission Slots (1..5) */}
        <div
          className="rounded-2xl p-3 space-y-2"
          style={{ backgroundColor: 'var(--tc-surface-low)' }}
        >
          <div className="flex items-center justify-between text-[11px] font-semibold">
            <span>Wake-up mission</span>
            <span className="font-mono-tech opacity-70">{draft.missions.length}/5</span>
          </div>
          <div className="flex items-center gap-2 overflow-x-auto pb-1">
            {draft.missions.map((mId) => {
              const Icon = MISSION_ICON[mId];
              return (
                <div
                  key={mId}
                  className="relative shrink-0 rounded-xl p-2 text-center"
                  style={{
                    backgroundColor: 'var(--tc-primary-container)',
                    color: 'var(--tc-on-primary-container)',
                    minWidth: '64px',
                  }}
                >
                  <button
                    onClick={() =>
                      setDraft((prev) => ({
                        ...prev,
                        missions: prev.missions.filter((x) => x !== mId),
                      }))
                    }
                    className="absolute -top-1 -right-1 flex h-4 w-4 cursor-pointer items-center justify-center rounded-full bg-black text-[9px] text-white"
                    aria-label="Remove mission"
                  >
                    <X className="h-2.5 w-2.5" />
                  </button>
                  <button
                    onClick={() => onPlayMission(mId)}
                    className="flex w-full cursor-pointer flex-col items-center gap-1"
                  >
                    <Icon className="h-4 w-4" />
                    <span className="max-w-[56px] truncate text-[9px] font-semibold">
                      {MISSION_META[mId].name.split(' ')[0]}
                    </span>
                  </button>
                </div>
              );
            })}
            {draft.missions.length < 5 && (
              <button
                onClick={onOpenMissionPicker}
                className="tc-pressable flex h-[54px] w-[54px] shrink-0 cursor-pointer items-center justify-center rounded-xl border border-dashed"
                style={{ borderColor: 'var(--tc-outline)' }}
                aria-label="Add wake-up mission"
              >
                <Plus className="h-4 w-4" />
              </button>
            )}
          </div>

          <button
            onClick={onTestWakeCheck}
            className="tc-pressable flex w-full cursor-pointer items-center justify-between rounded-xl px-2.5 py-2 text-[11px]"
            style={{ backgroundColor: 'var(--tc-surface)' }}
          >
            <span className="font-semibold">Wake up check (3,2,1 popup)</span>
            <span className="flex items-center gap-1 font-mono-tech text-[10px]" style={{ color: 'var(--tc-primary)' }}>
              {draft.wakeCheckMinutes > 0 ? `${draft.wakeCheckMinutes} min` : 'Off'}
              <ChevronRight className="h-3.5 w-3.5" />
            </span>
          </button>

          <button
            onClick={onOpenPowerOffDetail}
            className="tc-pressable flex w-full cursor-pointer items-center justify-between rounded-xl px-2.5 py-2 text-[11px]"
            style={{ backgroundColor: 'var(--tc-surface)' }}
          >
            <span className="font-semibold">Prevent power-off</span>
            <span className="flex items-center gap-1 font-mono-tech text-[10px]" style={{ color: 'var(--tc-primary)' }}>
              {draft.preventPowerOff ? 'Active Guard' : 'Setup'}
              <ChevronRight className="h-3.5 w-3.5" />
            </span>
          </button>
        </div>

        {/* Alarm Sound, Reminders & Extra Loud */}
        <div
          className="rounded-2xl p-3 space-y-2"
          style={{ backgroundColor: 'var(--tc-surface-low)' }}
        >
          <div className="flex items-center justify-between text-[11px] font-semibold">
            <span className="flex items-center gap-1.5">
              <Volume2 className="h-3.5 w-3.5" style={{ color: 'var(--tc-primary)' }} />
              Alarm Clock
            </span>
            <span className="font-mono-tech text-[10px]">{volume}%</span>
          </div>
          <input
            type="range"
            min={30}
            max={100}
            value={volume}
            onChange={(e) => setVolume(Number(e.target.value))}
            className="w-full accent-[var(--tc-primary)]"
          />

          <div className="flex items-center justify-between text-[11px]">
            <div>
              <div className="font-semibold">Voice reminders</div>
              <div className="text-[9px]" style={{ color: 'var(--tc-on-surface-variant)' }}>
                Time, live weather &amp; label readout
              </div>
            </div>
            <div className="flex items-center gap-1.5">
              <button
                onClick={() =>
                  setSampleText('Good morning! It is 6:30 AM, 22°C clear sky — Get up for college.')
                }
                className="tc-pressable cursor-pointer rounded-full px-2 py-0.5 text-[9px] font-bold"
                style={{ backgroundColor: 'var(--tc-surface-high)' }}
              >
                Sample
              </button>
              <button
                onClick={() => setDraft({ ...draft, voiceReminders: !draft.voiceReminders })}
                className="tc-pressable cursor-pointer rounded-full px-2.5 py-0.5 text-[10px] font-bold"
                style={{
                  backgroundColor: draft.voiceReminders
                    ? 'var(--tc-primary)'
                    : 'var(--tc-surface-highest)',
                  color: draft.voiceReminders
                    ? 'var(--tc-on-primary)'
                    : 'var(--tc-on-surface-variant)',
                }}
              >
                {draft.voiceReminders ? 'ON' : 'OFF'}
              </button>
            </div>
          </div>

          <div className="flex items-center justify-between text-[11px]">
            <div>
              <div className="font-semibold">Extra loud effect (+9dB)</div>
              <div className="text-[9px]" style={{ color: 'var(--tc-on-surface-variant)' }}>
                Hardware LoudnessEnhancer boost
              </div>
            </div>
            <button
              onClick={() => setDraft({ ...draft, extraLoud: !draft.extraLoud })}
              className="tc-pressable cursor-pointer rounded-full px-2.5 py-0.5 text-[10px] font-bold"
              style={{
                backgroundColor: draft.extraLoud
                  ? 'var(--tc-tertiary)'
                  : 'var(--tc-surface-highest)',
                color: '#000',
              }}
            >
              {draft.extraLoud ? 'BOOST' : 'OFF'}
            </button>
          </div>

          {sampleText && (
            <div
              className="rounded-lg p-2 text-[10px]"
              style={{
                backgroundColor: 'var(--tc-primary-container)',
                color: 'var(--tc-on-primary-container)',
              }}
            >
              {sampleText}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

/* ---------------- 2. MISSION PICKER + 6 PLAYABLE MISSIONS ---------------- */
export const MissionSuiteOverlay: React.FC<{
  activeMission: MissionId | null;
  goal: string;
  onSelectMission: (m: MissionId) => void;
  onAddMissionAndPlay: (m: MissionId) => void;
  onBackToPicker: () => void;
  onClose: () => void;
}> = ({ activeMission, goal, onSelectMission, onAddMissionAndPlay, onBackToPicker, onClose }) => {
  // Interactive Mission State
  const [tilesTarget] = useState<number[]>([0, 5, 10, 15]);
  const [tilesShowing, setTilesShowing] = useState(true);
  const [tilesPicked, setTilesPicked] = useState<number[]>([]);
  const [mathAns, setMathAns] = useState('');
  const [typedText, setTypedText] = useState('');
  const [stepsDone, setStepsDone] = useState(0);
  const [beatIndex, setBeatIndex] = useState(0);
  const [scanDone, setScanDone] = useState(false);
  const [missionCleared, setMissionCleared] = useState(false);

  const typingTarget = 'I am awake and ready for college';
  const beatWords = ['wake', 'up', 'right', 'now', 'do', 'not', 'be', 'late'];

  useEffect(() => {
    setMissionCleared(false);
    setTilesPicked([]);
    setMathAns('');
    setTypedText('');
    setStepsDone(0);
    setBeatIndex(0);
    setScanDone(false);
    if (activeMission === 'color_tiles') {
      setTilesShowing(true);
      const id = window.setTimeout(() => setTilesShowing(false), 1600);
      return () => window.clearTimeout(id);
    }
  }, [activeMission]);

  return (
    <div
      className="absolute inset-0 z-40 flex flex-col"
      style={{ backgroundColor: 'var(--tc-bg)', color: 'var(--tc-on-surface)' }}
    >
      <div className="flex items-center justify-between px-4 pt-7 pb-2">
        <button
          onClick={activeMission ? onBackToPicker : onClose}
          className="tc-pressable flex h-8 w-8 cursor-pointer items-center justify-center rounded-full"
          style={{ backgroundColor: 'var(--tc-surface-low)' }}
        >
          {activeMission ? <ChevronLeft className="h-4 w-4" /> : <X className="h-4 w-4" />}
        </button>
        <div className="text-xs font-bold">
          {activeMission ? MISSION_META[activeMission].name : 'Mission Picker'}
        </div>
        <button
          onClick={onClose}
          className="tc-pressable flex h-8 w-8 cursor-pointer items-center justify-center rounded-full"
          style={{ backgroundColor: 'var(--tc-surface-low)' }}
        >
          <X className="h-4 w-4" />
        </button>
      </div>

      {!activeMission ? (
        <div className="flex-1 space-y-3 overflow-y-auto px-4 pb-6 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
          {(['Popular', 'Wake your brain', 'Wake your body'] as const).map((group) => {
            const entries = (Object.keys(MISSION_META) as MissionId[]).filter(
              (id) => MISSION_META[id].group === group
            );
            return (
              <div key={group} className="space-y-1.5">
                <div className="text-[10px] font-bold uppercase tracking-wider opacity-65">
                  {group}
                </div>
                {entries.map((id) => {
                  const meta = MISSION_META[id];
                  const Icon = MISSION_ICON[id];
                  return (
                    <button
                      key={id}
                      onClick={() => onAddMissionAndPlay(id)}
                      className="tc-pressable flex w-full cursor-pointer items-center justify-between rounded-2xl p-2.5 text-left"
                      style={{
                        backgroundColor: 'var(--tc-surface-low)',
                        border: '1px solid var(--tc-outline-variant)',
                      }}
                    >
                      <div className="flex items-center gap-2.5">
                        <div
                          className="flex h-8 w-8 items-center justify-center rounded-xl"
                          style={{
                            backgroundColor: 'var(--tc-primary-container)',
                            color: 'var(--tc-on-primary-container)',
                          }}
                        >
                          <Icon className="h-4 w-4" />
                        </div>
                        <span className="text-xs font-semibold">{meta.name}</span>
                      </div>
                      {meta.badge && (
                        <span
                          className="rounded-full px-2 py-0.5 text-[9px] font-bold"
                          style={{
                            backgroundColor: 'var(--tc-tertiary-container)',
                            color: 'var(--tc-tertiary)',
                          }}
                        >
                          {meta.badge}
                        </span>
                      )}
                    </button>
                  );
                })}
              </div>
            );
          })}
        </div>
      ) : missionCleared ? (
        <div className="my-auto flex flex-col items-center px-6 text-center space-y-3">
          <div
            className="flex h-16 w-16 items-center justify-center rounded-full"
            style={{
              backgroundColor: 'var(--tc-primary-container)',
              color: 'var(--tc-primary)',
            }}
          >
            <CheckCircle2 className="h-8 w-8" />
          </div>
          <div className="text-base font-bold">Mission Cleared!</div>
          <p className="text-[11px]" style={{ color: 'var(--tc-on-surface-variant)' }}>
            Goal: “{goal}” verified. Tap below to try another mission or return.
          </p>
          <button
            onClick={onBackToPicker}
            className="tc-pressable w-full cursor-pointer rounded-2xl py-2.5 text-xs font-bold"
            style={{
              backgroundColor: 'var(--tc-primary)',
              color: 'var(--tc-on-primary)',
            }}
          >
            Try Another Mission
          </button>
        </div>
      ) : (
        <div className="flex-1 flex flex-col justify-center px-4 pb-6 space-y-3">
          {activeMission === 'color_tiles' && (
            <div className="space-y-3 text-center">
              <div className="text-xs font-semibold">
                {tilesShowing ? 'Memorize the highlighted tiles...' : 'Tap the 4 remembered tiles!'}
              </div>
              <div className="mx-auto grid w-48 grid-cols-4 gap-1.5">
                {Array.from({ length: 16 }).map((_, idx) => {
                  const isTarget = tilesTarget.includes(idx);
                  const picked = tilesPicked.includes(idx);
                  const lit = (tilesShowing && isTarget) || picked;
                  return (
                    <button
                      key={idx}
                      disabled={tilesShowing}
                      onClick={() => {
                        if (!isTarget) return;
                        const next = Array.from(new Set([...tilesPicked, idx]));
                        setTilesPicked(next);
                        if (next.length === tilesTarget.length) setMissionCleared(true);
                      }}
                      className="tc-pressable h-10 cursor-pointer rounded-lg transition-colors"
                      style={{
                        backgroundColor: lit ? 'var(--tc-tertiary)' : 'var(--tc-surface-high)',
                      }}
                    />
                  );
                })}
              </div>
            </div>
          )}

          {activeMission === 'math' && (
            <div className="space-y-3 text-center">
              <div className="font-mono-tech text-2xl font-bold">(18 × 2) + 9 = ?</div>
              <input
                type="text"
                value={mathAns}
                onChange={(e) => {
                  const v = e.target.value;
                  setMathAns(v);
                  if (v.trim() === '45') setMissionCleared(true);
                }}
                placeholder="Type 45"
                className="w-full rounded-xl px-3 py-2.5 text-center font-mono-tech text-sm outline-none"
                style={{
                  backgroundColor: 'var(--tc-surface-low)',
                  border: '1px solid var(--tc-outline)',
                }}
              />
            </div>
          )}

          {activeMission === 'typing' && (
            <div className="space-y-2.5">
              <div className="rounded-xl p-2.5 text-xs font-semibold" style={{ backgroundColor: 'var(--tc-surface-low)' }}>
                {typingTarget}
              </div>
              <input
                type="text"
                value={typedText}
                onChange={(e) => {
                  setTypedText(e.target.value);
                  if (e.target.value.trim().toLowerCase() === typingTarget.toLowerCase()) {
                    setMissionCleared(true);
                  }
                }}
                placeholder="Type the phrase above..."
                className="w-full rounded-xl px-3 py-2 text-xs outline-none"
                style={{
                  backgroundColor: 'var(--tc-surface-high)',
                  border: '1px solid var(--tc-outline)',
                }}
              />
            </div>
          )}

          {activeMission === 'step' && (
            <div className="space-y-3 text-center">
              <div className="font-mono-tech text-4xl font-bold">{stepsDone} / 20</div>
              <div className="text-[11px]" style={{ color: 'var(--tc-on-surface-variant)' }}>
                Step out of bed — tap to simulate steps
              </div>
              <button
                onClick={() => {
                  const next = Math.min(20, stepsDone + 5);
                  setStepsDone(next);
                  if (next >= 20) setMissionCleared(true);
                }}
                className="tc-pressable mx-auto inline-flex cursor-pointer items-center gap-2 rounded-2xl px-5 py-3 text-xs font-bold"
                style={{
                  backgroundColor: 'var(--tc-primary)',
                  color: 'var(--tc-on-primary)',
                }}
              >
                <Footprints className="h-4 w-4" />
                Walk +5 Steps
              </button>
            </div>
          )}

          {activeMission === 'word_beat' && (
            <div className="space-y-3 text-center">
              <div className="text-[11px]" style={{ color: 'var(--tc-on-surface-variant)' }}>
                Tap mic to speak each highlighted word on beat
              </div>
              <div className="grid grid-cols-4 gap-1.5">
                {beatWords.map((w, i) => (
                  <div
                    key={w}
                    className="rounded-lg py-1.5 text-[11px] font-bold"
                    style={{
                      backgroundColor:
                        i < beatIndex
                          ? 'var(--tc-primary)'
                          : i === beatIndex
                          ? 'var(--tc-tertiary)'
                          : 'var(--tc-surface-low)',
                      color: i <= beatIndex ? '#000' : 'var(--tc-on-surface)',
                    }}
                  >
                    {w}
                  </div>
                ))}
              </div>
              <button
                onClick={() => {
                  const next = beatIndex + 1;
                  setBeatIndex(next);
                  if (next >= beatWords.length) setMissionCleared(true);
                }}
                className="tc-pressable mx-auto inline-flex cursor-pointer items-center gap-2 rounded-full px-5 py-3 text-xs font-bold"
                style={{
                  backgroundColor: 'var(--tc-primary)',
                  color: 'var(--tc-on-primary)',
                }}
              >
                <Mic className="h-4 w-4" />
                Speak “{beatWords[beatIndex]}”
              </button>
            </div>
          )}

          {activeMission === 'item_hunt' && (
            <div className="space-y-3 text-center">
              <div
                className="mx-auto flex h-32 w-full flex-col items-center justify-center rounded-2xl border-2 border-dashed"
                style={{
                  backgroundColor: 'var(--tc-surface-low)',
                  borderColor: scanDone ? 'var(--tc-primary)' : 'var(--tc-tertiary)',
                }}
              >
                <Camera className="h-7 w-7" style={{ color: 'var(--tc-primary)' }} />
                <div className="mt-1.5 text-xs font-bold">Find Item: Coffee Cup / Mug</div>
              </div>
              <button
                onClick={() => {
                  setScanDone(true);
                  window.setTimeout(() => setMissionCleared(true), 450);
                }}
                className="tc-pressable w-full cursor-pointer rounded-2xl py-2.5 text-xs font-bold"
                style={{
                  backgroundColor: 'var(--tc-primary)',
                  color: 'var(--tc-on-primary)',
                }}
              >
                Scan Photo with AI Vision
              </button>
            </div>
          )}

          <div className="pt-2 text-center">
            <button
              onClick={() => onSelectMission(activeMission)}
              className="cursor-pointer text-[10px] underline opacity-70"
            >
              Switch to another challenge
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

/* ---------------- 3. WAKE-UP CHECK COUNTDOWN POPUP ---------------- */
export const WakeCheckOverlay: React.FC<{
  goal: string;
  onAwake: () => void;
  onReRing: () => void;
}> = ({ goal, onAwake, onReRing }) => {
  const [count, setCount] = useState(12);

  useEffect(() => {
    const id = window.setInterval(() => {
      setCount((c) => (c > 0 ? c - 1 : 0));
    }, 1000);
    return () => window.clearInterval(id);
  }, []);

  return (
    <div className="absolute inset-0 z-40 flex items-center justify-center bg-black/75 p-5 backdrop-blur-sm">
      <div
        className="w-full rounded-3xl p-5 text-center space-y-3"
        style={{
          backgroundColor: 'var(--tc-surface)',
          border: '1.5px solid var(--tc-primary)',
        }}
      >
        <div
          className="mx-auto flex h-12 w-12 items-center justify-center rounded-full font-mono-tech text-xl font-bold"
          style={{
            backgroundColor: 'var(--tc-tertiary-container)',
            color: 'var(--tc-tertiary)',
          }}
        >
          {count}
        </div>
        <div className="text-base font-bold">Are you really awake?</div>
        <p className="text-[11px]" style={{ color: 'var(--tc-on-surface-variant)' }}>
          {count <= 3
            ? `Final check: ${count}... Re-arming all alarms if no response!`
            : `Confirm within ${count}s to silence all alarms for “${goal}”.`}
        </p>
        <button
          onClick={onAwake}
          className="tc-pressable w-full cursor-pointer rounded-2xl py-2.5 text-xs font-bold"
          style={{
            backgroundColor: 'var(--tc-primary)',
            color: 'var(--tc-on-primary)',
          }}
        >
          I&apos;m Awake — Silence Goal Alarms
        </button>
        <button
          onClick={onReRing}
          className="tc-pressable w-full cursor-pointer rounded-2xl py-2 text-[11px] font-semibold"
          style={{ backgroundColor: 'var(--tc-surface-high)' }}
        >
          Test Timeout → Re-Ring Alarm
        </button>
      </div>
    </div>
  );
};

/* ---------------- 4. PREVENT POWER-OFF GUARD ---------------- */
export const PreventPowerOffOverlay: React.FC<{
  enabled: boolean;
  onToggle: () => void;
  onClose: () => void;
}> = ({ enabled, onToggle, onClose }) => (
  <div
    className="absolute inset-0 z-40 flex flex-col p-4"
    style={{ backgroundColor: 'var(--tc-bg)', color: 'var(--tc-on-surface)' }}
  >
    <div className="flex items-center justify-between pt-3 pb-3">
      <button
        onClick={onClose}
        className="tc-pressable flex h-8 w-8 cursor-pointer items-center justify-center rounded-full"
        style={{ backgroundColor: 'var(--tc-surface-low)' }}
      >
        <ChevronLeft className="h-4 w-4" />
      </button>
      <div className="text-xs font-bold">Prevent power-off</div>
      <div className="w-8" />
    </div>

    <div className="space-y-3">
      <div
        className="flex items-center justify-between rounded-2xl p-3.5"
        style={{ backgroundColor: 'var(--tc-surface-low)' }}
      >
        <div className="flex items-center gap-2 text-xs font-bold">
          <ShieldCheck className="h-4 w-4" style={{ color: 'var(--tc-primary)' }} />
          Prevent power-off
        </div>
        <button
          onClick={onToggle}
          className="tc-pressable cursor-pointer rounded-full px-3 py-1 text-[10px] font-bold"
          style={{
            backgroundColor: enabled ? 'var(--tc-primary)' : 'var(--tc-surface-highest)',
            color: enabled ? 'var(--tc-on-primary)' : 'var(--tc-on-surface-variant)',
          }}
        >
          {enabled ? 'ON' : 'OFF'}
        </button>
      </div>

      {[
        {
          n: '1',
          title: 'Display over apps',
          desc: 'Allows Timecure to keep displaying the alarm dismiss screen.',
        },
        {
          n: '2',
          title: 'Accessibility',
          desc: 'Detects global power-off dialogs until wake verification completes.',
        },
      ].map((step) => (
        <div
          key={step.n}
          className="flex items-start gap-3 rounded-2xl p-3"
          style={{
            backgroundColor: 'var(--tc-surface-low)',
            border: '1px solid var(--tc-primary)',
          }}
        >
          <span
            className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-xs font-bold"
            style={{
              backgroundColor: 'var(--tc-primary)',
              color: 'var(--tc-on-primary)',
            }}
          >
            {step.n}
          </span>
          <div>
            <div className="text-xs font-bold">{step.title}</div>
            <div className="mt-0.5 text-[10px]" style={{ color: 'var(--tc-on-surface-variant)' }}>
              {step.desc}
            </div>
          </div>
        </div>
      ))}
    </div>
  </div>
);

/* ---------------- 5. LIVE TIMECURE AI BOTTOM SHEET ---------------- */
export const AiSheetOverlay: React.FC<{
  onClose: () => void;
  onScanTimetable: () => void;
}> = ({ onClose, onScanTimetable }) => {
  const [prompt, setPrompt] = useState('');
  const [loading, setLoading] = useState(false);
  const [messages, setMessages] = useState<{ user: boolean; text: string }[]>([
    {
      user: false,
      text: 'Snap your timetable or ask me to plan wake-up alarms and Pomodoro focus blocks.',
    },
  ]);

  const ask = async (q: string) => {
    const clean = q.trim();
    if (!clean || loading) return;
    setMessages((m) => [...m, { user: true, text: clean }]);
    setPrompt('');
    setLoading(true);
    try {
      const resp = await fetch('/api/ai', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          temperature: 0.5,
          max_tokens: 1400,
          messages: [
            {
              role: 'system',
              content:
                'You are Timecure AI inside a compact mobile sheet. Reply helpfully in 2 crisp sentences.',
            },
            { role: 'user', content: clean },
          ],
        }),
      });
      const data = await resp.json();
      setMessages((m) => [
        ...m,
        {
          user: false,
          text: data.content || data.error || 'Could not reach Timecure AI right now.',
        },
      ]);
    } catch {
      setMessages((m) => [
        ...m,
        { user: false, text: 'Connect via https://timecure.vercel.app to test live AI responses.' },
      ]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      className="absolute inset-x-0 bottom-0 top-10 z-40 flex flex-col rounded-t-3xl p-4 shadow-2xl"
      style={{
        backgroundColor: 'var(--tc-surface)',
        borderTop: '1px solid var(--tc-outline-variant)',
      }}
    >
      <div className="flex items-center justify-between pb-2">
        <div className="flex items-center gap-2">
          <Sparkles className="h-4 w-4" style={{ color: 'var(--tc-primary)' }} />
          <div>
            <div className="text-xs font-bold">Timecure AI</div>
            <div className="text-[9px]" style={{ color: 'var(--tc-on-surface-variant)' }}>
              Scan timetables, notes &amp; manage your day
            </div>
          </div>
        </div>
        <button
          onClick={onClose}
          className="tc-pressable flex h-7 w-7 cursor-pointer items-center justify-center rounded-full"
          style={{ backgroundColor: 'var(--tc-surface-high)' }}
        >
          <X className="h-4 w-4" />
        </button>
      </div>

      <button
        onClick={() => {
          onScanTimetable();
          setMessages((m) => [
            ...m,
            { user: true, text: 'Scanned college_timetable.jpg' },
            {
              user: false,
              text: 'Extracted your lecture windows and saved a fresh Study & Routine Table to Insights.',
            },
          ]);
        }}
        className="tc-pressable mb-2 flex w-full cursor-pointer items-center justify-center gap-1.5 rounded-xl py-2 text-[11px] font-bold"
        style={{
          backgroundColor: 'var(--tc-primary-container)',
          color: 'var(--tc-on-primary-container)',
        }}
      >
        <Camera className="h-3.5 w-3.5" />
        Simulate Timetable Vision Scan
      </button>

      <div className="flex-1 space-y-2 overflow-y-auto py-1 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
        {messages.map((m, i) => (
          <div
            key={i}
            className={`rounded-2xl p-2.5 text-[11px] leading-relaxed ${m.user ? 'ml-6' : 'mr-4'}`}
            style={{
              backgroundColor: m.user ? 'var(--tc-primary-container)' : 'var(--tc-surface-low)',
              color: m.user ? 'var(--tc-on-primary-container)' : 'var(--tc-on-surface)',
            }}
          >
            {m.text}
          </div>
        ))}
        {loading && (
          <div className="flex items-center gap-1.5 text-[10px] opacity-75">
            <Loader2 className="h-3 w-3 animate-spin" />
            Timecure AI is thinking...
          </div>
        )}
      </div>

      <form
        onSubmit={(e) => {
          e.preventDefault();
          ask(prompt);
        }}
        className="mt-2 flex gap-1.5"
      >
        <input
          type="text"
          value={prompt}
          onChange={(e) => setPrompt(e.target.value)}
          placeholder="Ask Timecure AI..."
          className="flex-1 rounded-xl px-3 py-2 text-[11px] outline-none"
          style={{
            backgroundColor: 'var(--tc-surface-low)',
            border: '1px solid var(--tc-outline-variant)',
          }}
        />
        <button
          type="submit"
          className="tc-pressable flex h-8 w-8 cursor-pointer items-center justify-center rounded-xl"
          style={{
            backgroundColor: 'var(--tc-primary)',
            color: 'var(--tc-on-primary)',
          }}
        >
          <Send className="h-3.5 w-3.5" />
        </button>
      </form>
    </div>
  );
};

/* ---------------- 6. LOCATION / TIME TASK ALERT OVERLAY ---------------- */
export const TaskAlertOverlay: React.FC<{
  onComplete: () => void;
  onClose: () => void;
}> = ({ onComplete, onClose }) => (
  <div
    className="absolute inset-0 z-40 flex flex-col justify-between p-5 text-center"
    style={{
      backgroundColor: 'var(--tc-bg)',
      backgroundImage:
        'radial-gradient(circle at 50% 30%, color-mix(in srgb, var(--tc-tertiary) 26%, transparent), transparent 65%)',
    }}
  >
    <div className="pt-6 text-[10px] font-bold uppercase tracking-wider" style={{ color: 'var(--tc-tertiary)' }}>
      Location Task Triggered • 150m Geofence
    </div>
    <div className="space-y-3">
      <div
        className="mx-auto flex h-16 w-16 items-center justify-center rounded-full"
        style={{
          backgroundColor: 'var(--tc-primary-container)',
          color: 'var(--tc-primary)',
        }}
      >
        <MapPin className="h-8 w-8" />
      </div>
      <div className="text-lg font-bold">Arrived at College Campus</div>
      <p className="text-xs" style={{ color: 'var(--tc-on-surface-variant)' }}>
        Task: Submit Operating Systems Lab Journal &amp; switch phone to Study Focus.
      </p>
    </div>
    <div className="space-y-2 pb-3">
      <button
        onClick={onComplete}
        className="tc-pressable flex w-full cursor-pointer items-center justify-center gap-1.5 rounded-2xl py-2.5 text-xs font-bold"
        style={{
          backgroundColor: 'var(--tc-primary)',
          color: 'var(--tc-on-primary)',
        }}
      >
        <Check className="h-4 w-4" />
        Mark Done
      </button>
      <button
        onClick={onClose}
        className="tc-pressable flex w-full cursor-pointer items-center justify-center gap-1.5 rounded-2xl py-2 text-[11px] font-semibold"
        style={{ backgroundColor: 'var(--tc-surface-low)' }}
      >
        <Clock className="h-3.5 w-3.5" />
        Dismiss
      </button>
    </div>
  </div>
);
