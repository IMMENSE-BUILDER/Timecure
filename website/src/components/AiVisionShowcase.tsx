import React, { useState } from 'react';
import {
  Camera,
  CheckCircle2,
  Cpu,
  FileImage,
  MessageSquareText,
  Sparkles,
  Terminal,
  Wrench,
} from 'lucide-react';

interface AiVisionShowcaseProps {
  selectedModel: string;
  onSelectModel: (model: string) => void;
}

type ScenarioId = 'timetable' | 'prompt' | 'study';

const SCENARIOS: Record<
  ScenarioId,
  {
    title: string;
    badge: string;
    inputType: string;
    userInput: string;
    toolCalls: { name: string; args: string }[];
    assistantReply: string;
  }
> = {
  timetable: {
    title: 'Timetable Photo → Task Table',
    badge: 'Multimodal Vision',
    inputType: 'Camera Scan (college_timetable.jpg)',
    userInput: 'Uploaded my semester timetable photo — plan my alarms and study blocks.',
    toolCalls: [
      {
        name: 'save_task_table',
        args: '{ planTitle: "Semester Weekdays", rows: [ { slot: "06:30", task: "Calculus III Prep", technique: "Active Recall" }, { slot: "09:00", task: "OS Kernel Lab", technique: "25m Pomodoro" } ] }',
      },
      {
        name: 'create_alarm',
        args: '{ hour: 6, minute: 30, label: "Get up for college", difficulty: "HARD", wakeCheck: true }',
      },
    ],
    assistantReply:
      'Extracted 4 lecture windows from your timetable, saved your daily Task Table to Insights, and armed a 6:30 AM Relentless alarm with a 5-min anti-relapse check.',
  },
  prompt: {
    title: 'Natural Language → Tool Execution',
    badge: '7 Autonomous Tools',
    inputType: 'Voice / Chat Command',
    userInput:
      '“Set a 5:45 AM alarm for my DSA marathon, switch app theme to Emerald dark, and launch a 25m focus sprint.”',
    toolCalls: [
      {
        name: 'create_alarm',
        args: '{ hour: 5, minute: 45, goal: "DSA Graph Marathon", wakePrompt: "Solve 2 medium trees before breakfast" }',
      },
      {
        name: 'set_theme',
        args: '{ palette: "EMERALD_FOREST", mode: "DARK" }',
      },
      {
        name: 'start_pomodoro',
        args: '{ goal: "DSA Graph Marathon", minutes: 25 }',
      },
    ],
    assistantReply:
      'Done! Your 5:45 AM alarm is armed, Emerald Dark theme is active app-wide, and your 25-minute Focus chronometer is rolling.',
  },
  study: {
    title: 'Study Sheet → Custom Wake Puzzles',
    badge: 'Cognitive Coaching',
    inputType: 'Uploaded Notes (operating_systems_ch4.png)',
    userInput: 'Quiz me on process scheduling & deadlocks when my 7:00 AM alarm rings.',
    toolCalls: [
      {
        name: 'update_profile',
        args: '{ defaultGoal: "OS Midterms: Deadlocks & Scheduling", difficulty: "MEDIUM" }',
      },
      {
        name: 'save_task_table',
        args: '{ planTitle: "OS Midterm Sprint", rows: [ { slot: "07:15", task: "Banker Algorithm Drill", technique: "Spaced Repetition" } ] }',
      },
    ],
    assistantReply:
      'Updated your wake-up coach context with Banker’s Algorithm & Coffman conditions — tomorrow’s wake-up puzzles will test your exam concepts directly.',
  },
};

export const AiVisionShowcase: React.FC<AiVisionShowcaseProps> = ({
  selectedModel,
  onSelectModel,
}) => {
  const [scenario, setScenario] = useState<ScenarioId>('timetable');
  const current = SCENARIOS[scenario];

  return (
    <div
      className="rounded-3xl p-6 sm:p-8"
      style={{
        backgroundColor: 'var(--tc-surface-low)',
        border: '1px solid var(--tc-outline-variant)',
      }}
    >
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <div
            className="inline-flex items-center gap-1.5 text-xs font-bold uppercase tracking-wider"
            style={{ color: 'var(--tc-primary)' }}
          >
            <Sparkles className="h-4 w-4" />
            Vercel AI Gateway • Multimodal & Agentic
          </div>
          <h3 className="mt-1 text-2xl sm:text-3xl font-bold tracking-tight">
            Snap your timetable. Command your whole routine.
          </h3>
          <p className="mt-2 max-w-2xl text-sm" style={{ color: 'var(--tc-on-surface-variant)' }}>
            Powered by <strong>Vercel AI Gateway</strong> with instant one-tap switching between{' '}
            <code className="font-mono-tech text-xs">minimax/minimax-m3.1-flash</code> and{' '}
            <code className="font-mono-tech text-xs">meta/muse-ember</code>, editable System Instructions, and 7 native Android function-calling tools.
          </p>
        </div>

        {/* Model Switcher Pill */}
        <div
          className="inline-flex rounded-2xl p-1"
          style={{
            backgroundColor: 'var(--tc-surface)',
            border: '1px solid var(--tc-outline-variant)',
          }}
        >
          {[
            { id: 'minimax/minimax-m3.1-flash', name: 'MiniMax M3.1 Flash' },
            { id: 'meta/muse-ember', name: 'Meta Muse Ember' },
          ].map((m) => {
            const active = selectedModel === m.id;
            return (
              <button
                key={m.id}
                onClick={() => onSelectModel(m.id)}
                className="tc-pressable inline-flex cursor-pointer items-center gap-1.5 rounded-xl px-3.5 py-2 text-xs font-bold"
                style={{
                  backgroundColor: active ? 'var(--tc-primary)' : 'transparent',
                  color: active ? 'var(--tc-on-primary)' : 'var(--tc-on-surface-variant)',
                }}
              >
                <Cpu className="h-3.5 w-3.5" />
                {m.name}
              </button>
            );
          })}
        </div>
      </div>

      {/* Scenario Tabs */}
      <div className="mt-6 grid gap-3 sm:grid-cols-3">
        {(
          [
            ['timetable', Camera, 'Timetable Vision Scan'],
            ['prompt', MessageSquareText, 'Multi-Tool Command'],
            ['study', FileImage, 'Study Notes → Puzzles'],
          ] as const
        ).map(([id, Icon, label]) => {
          const active = scenario === id;
          return (
            <button
              key={id}
              onClick={() => setScenario(id)}
              className="tc-pressable flex cursor-pointer items-center gap-2.5 rounded-2xl px-4 py-3 text-left text-xs font-bold"
              style={{
                backgroundColor: active
                  ? 'var(--tc-primary-container)'
                  : 'var(--tc-surface)',
                color: active
                  ? 'var(--tc-on-primary-container)'
                  : 'var(--tc-on-surface)',
                border: `1.5px solid ${
                  active ? 'var(--tc-primary)' : 'var(--tc-outline-variant)'
                }`,
              }}
            >
              <Icon className="h-4 w-4 shrink-0" />
              <span>{label}</span>
            </button>
          );
        })}
      </div>

      {/* Live Execution Inspector */}
      <div className="mt-5 grid gap-4 lg:grid-cols-12">
        {/* Left: User Input + Reply */}
        <div
          className="flex flex-col justify-between rounded-2xl p-5 lg:col-span-5"
          style={{
            backgroundColor: 'var(--tc-surface)',
            border: '1px solid var(--tc-outline-variant)',
          }}
        >
          <div className="space-y-3">
            <div className="flex items-center justify-between text-[11px] font-semibold">
              <span
                className="rounded-full px-2.5 py-0.5"
                style={{
                  backgroundColor: 'var(--tc-surface-high)',
                  color: 'var(--tc-primary)',
                }}
              >
                {current.badge}
              </span>
              <span className="font-mono-tech opacity-70">{selectedModel}</span>
            </div>

            <div
              className="rounded-xl p-3 text-xs"
              style={{ backgroundColor: 'var(--tc-surface-low)' }}
            >
              <div className="font-mono-tech text-[10px] opacity-70">{current.inputType}</div>
              <div className="mt-1 font-medium leading-relaxed">{current.userInput}</div>
            </div>

            <div
              className="rounded-xl p-3.5 text-xs leading-relaxed"
              style={{
                backgroundColor: 'var(--tc-primary-container)',
                color: 'var(--tc-on-primary-container)',
              }}
            >
              <div className="mb-1 flex items-center gap-1.5 text-[10px] font-bold uppercase tracking-wider">
                <CheckCircle2 className="h-3.5 w-3.5" />
                Timecure AI Response
              </div>
              {current.assistantReply}
            </div>
          </div>

          <div
            className="mt-4 flex flex-wrap gap-1.5 pt-3 border-t text-[10px] font-mono-tech"
            style={{
              borderColor: 'var(--tc-outline-variant)',
              color: 'var(--tc-on-surface-variant)',
            }}
          >
            {[
              'create_alarm',
              'toggle_alarm',
              'delete_alarm',
              'start_pomodoro',
              'save_task_table',
              'set_theme',
              'update_profile',
            ].map((tool) => (
              <span
                key={tool}
                className="rounded-md px-2 py-0.5"
                style={{ backgroundColor: 'var(--tc-surface-high)' }}
              >
                {tool}()
              </span>
            ))}
          </div>
        </div>

        {/* Right: Tool-Call Trace */}
        <div
          className="rounded-2xl p-5 font-mono-tech text-xs lg:col-span-7"
          style={{
            backgroundColor: '#090D14',
            color: '#D7E3F7',
            border: '1px solid var(--tc-outline-variant)',
          }}
        >
          <div className="mb-3 flex items-center justify-between border-b border-white/10 pb-2.5 text-[11px]">
            <span className="inline-flex items-center gap-2 font-semibold text-sky-300">
              <Terminal className="h-3.5 w-3.5" />
              POST https://ai-gateway.vercel.sh/v1/chat/completions
            </span>
            <span className="rounded-full bg-emerald-500/20 px-2 py-0.5 text-[10px] text-emerald-300">
              200 OK
            </span>
          </div>

          <div className="space-y-3">
            {current.toolCalls.map((tc, i) => (
              <div
                key={i}
                className="rounded-xl border border-white/10 bg-white/[0.03] p-3"
              >
                <div className="flex items-center gap-2 text-amber-300">
                  <Wrench className="h-3.5 w-3.5" />
                  <span className="font-bold">tool_call[{i}]: {tc.name}</span>
                </div>
                <pre className="mt-1.5 whitespace-pre-wrap break-words text-[11px] leading-relaxed text-slate-300">
                  {tc.args}
                </pre>
              </div>
            ))}
          </div>

          <div className="mt-3 flex items-center justify-between text-[10px] text-slate-400">
            <span>Offline Fallback: 6 Deterministic Local Engines Ready</span>
            <span>SQLite v6 Synced</span>
          </div>
        </div>
      </div>
    </div>
  );
};
