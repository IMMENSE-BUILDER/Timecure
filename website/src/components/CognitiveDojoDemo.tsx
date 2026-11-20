import React, { useEffect, useState } from 'react';
import { Brain, CheckCircle2, RefreshCw, Sparkles, Timer } from 'lucide-react';

type Modality = 'stroop' | 'math' | 'sequence' | 'memory';

interface Puzzle {
  modality: Modality;
  badge: string;
  prompt: string;
  display?: string;
  displayColor?: string;
  options: string[];
  answer: string;
  hint: string;
}

const STROOP_COLORS = [
  { name: 'BLUE', hex: '#38BDF8' },
  { name: 'GREEN', hex: '#34D399' },
  { name: 'RED', hex: '#FB7185' },
  { name: 'AMBER', hex: '#FBBF24' },
];

function generatePuzzle(modality: Modality): Puzzle {
  if (modality === 'stroop') {
    const wordIdx = Math.floor(Math.random() * STROOP_COLORS.length);
    let inkIdx = Math.floor(Math.random() * STROOP_COLORS.length);
    if (inkIdx === wordIdx) inkIdx = (inkIdx + 1) % STROOP_COLORS.length;
    return {
      modality,
      badge: 'Prefrontal Control',
      prompt: 'Tap the INK COLOR of this word — ignore what it spells:',
      display: STROOP_COLORS[wordIdx].name,
      displayColor: STROOP_COLORS[inkIdx].hex,
      options: STROOP_COLORS.map((c) => c.name),
      answer: STROOP_COLORS[inkIdx].name,
      hint: 'Activates anterior cingulate cortex to break sleep inertia.',
    };
  }

  if (modality === 'math') {
    const a = 7 + Math.floor(Math.random() * 11);
    const b = 3 + Math.floor(Math.random() * 6);
    const c = 11 + Math.floor(Math.random() * 19);
    const ans = a * b + c;
    const opts = new Set<number>([ans]);
    while (opts.size < 4) {
      opts.add(ans + (Math.floor(Math.random() * 9) - 4) * 2 || ans + 3);
    }
    return {
      modality,
      badge: 'Arithmetic Chain',
      prompt: 'Compute in your head to silence the alarm:',
      display: `(${a} × ${b}) + ${c}`,
      options: Array.from(opts).sort((x, y) => x - y).map(String),
      answer: String(ans),
      hint: 'Deterministic code-verified math — never stalls offline.',
    };
  }

  if (modality === 'sequence') {
    const start = 3 + Math.floor(Math.random() * 5);
    const step = 3 + Math.floor(Math.random() * 4);
    const seq = [start, start + step, start + step * 2 + 2, start + step * 3 + 6];
    const next = seq[3] + step + 6;
    const opts = [next - 4, next, next + 2, next + 6].sort((x, y) => x - y);
    return {
      modality,
      badge: 'Growing-Gap Logic',
      prompt: 'Find the next number in the escalating sequence:',
      display: `${seq.join('  →  ')}  →  ?`,
      options: opts.map(String),
      answer: String(next),
      hint: 'Differences increase by +2 each step.',
    };
  }

  // Reverse digit span
  const digits = Array.from({ length: 5 }, () => Math.floor(Math.random() * 9) + 1).join('');
  const reversed = digits.split('').reverse().join('');
  const alt1 = reversed.slice(0, 3) + reversed[4] + reversed[3];
  const alt2 = reversed[1] + reversed[0] + reversed.slice(2);
  const alt3 = digits.slice(0, 2) + reversed.slice(2);
  const options = Array.from(new Set([reversed, alt1, alt2, alt3])).sort();
  return {
    modality,
    badge: 'Working Memory Span',
    prompt: 'Select the EXACT reverse of this 5-digit sequence:',
    display: digits.split('').join(' '),
    options,
    answer: reversed,
    hint: 'Engages short-term working memory immediately on wake.',
  };
}

export const CognitiveDojoDemo: React.FC = () => {
  const [modality, setModality] = useState<Modality>('stroop');
  const [puzzle, setPuzzle] = useState<Puzzle>(() => generatePuzzle('stroop'));
  const [solvedCount, setSolvedCount] = useState(0);
  const [wrongShake, setWrongShake] = useState(false);
  const [justSolved, setJustSolved] = useState(false);
  const [startMs, setStartMs] = useState(() => performance.now());
  const [elapsedSec, setElapsedSec] = useState(0);

  useEffect(() => {
    if (justSolved) return;
    const id = window.setInterval(() => {
      setElapsedSec((performance.now() - startMs) / 1000);
    }, 100);
    return () => window.clearInterval(id);
  }, [startMs, justSolved]);

  const loadModality = (m: Modality) => {
    setModality(m);
    setPuzzle(generatePuzzle(m));
    setJustSolved(false);
    setStartMs(performance.now());
    setElapsedSec(0);
  };

  const pickOption = (opt: string) => {
    if (justSolved) return;
    if (opt === puzzle.answer) {
      setJustSolved(true);
      setSolvedCount((c) => c + 1);
    } else {
      setWrongShake(true);
      window.setTimeout(() => setWrongShake(false), 360);
    }
  };

  return (
    <div
      className="rounded-3xl p-6 sm:p-8"
      style={{
        backgroundColor: 'var(--tc-surface-low)',
        border: '1px solid var(--tc-outline-variant)',
      }}
    >
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <div
            className="inline-flex items-center gap-1.5 text-xs font-bold uppercase tracking-wider"
            style={{ color: 'var(--tc-primary)' }}
          >
            <Brain className="h-4 w-4" />
            Interactive Brain Warm-Up Dojo
          </div>
          <h3 className="mt-1 text-2xl font-bold tracking-tight">
            Try silencing Timecure right now.
          </h3>
        </div>

        <div className="flex items-center gap-2 font-mono-tech text-xs">
          <span
            className="inline-flex items-center gap-1.5 rounded-full px-3 py-1.5"
            style={{ backgroundColor: 'var(--tc-surface-high)' }}
          >
            <Timer className="h-3.5 w-3.5" style={{ color: 'var(--tc-tertiary)' }} />
            {elapsedSec.toFixed(1)}s
          </span>
          <span
            className="inline-flex items-center gap-1.5 rounded-full px-3 py-1.5 font-bold"
            style={{
              backgroundColor: 'var(--tc-primary-container)',
              color: 'var(--tc-on-primary-container)',
            }}
          >
            <Sparkles className="h-3.5 w-3.5" />
            {solvedCount} solved
          </span>
        </div>
      </div>

      {/* Modality Selector */}
      <div className="mt-5 flex flex-wrap gap-2">
        {(
          [
            ['stroop', 'Stroop Ink Test'],
            ['math', 'Arithmetic Chain'],
            ['sequence', 'Logic Sequence'],
            ['memory', 'Reverse Digits'],
          ] as const
        ).map(([id, label]) => {
          const active = modality === id;
          return (
            <button
              key={id}
              onClick={() => loadModality(id)}
              className="tc-pressable cursor-pointer rounded-full px-4 py-2 text-xs font-semibold"
              style={{
                backgroundColor: active
                  ? 'var(--tc-primary)'
                  : 'var(--tc-surface-high)',
                color: active
                  ? 'var(--tc-on-primary)'
                  : 'var(--tc-on-surface)',
              }}
            >
              {label}
            </button>
          );
        })}
      </div>

      {/* Puzzle Arena */}
      <div
        className={`mt-6 rounded-2xl p-6 text-center transition-all ${
          wrongShake ? 'animate-tc-shake' : ''
        }`}
        style={{
          backgroundColor: 'var(--tc-surface)',
          border: `1px solid ${
            justSolved ? 'var(--tc-primary)' : 'var(--tc-outline-variant)'
          }`,
        }}
      >
        <div
          className="inline-block rounded-full px-3 py-1 text-[11px] font-bold uppercase tracking-wider"
          style={{
            backgroundColor: 'var(--tc-surface-high)',
            color: 'var(--tc-primary)',
          }}
        >
          {puzzle.badge}
        </div>
        <p
          className="mt-3 text-sm"
          style={{ color: 'var(--tc-on-surface-variant)' }}
        >
          {puzzle.prompt}
        </p>

        <div
          className="my-6 font-mono-tech text-3xl sm:text-4xl font-extrabold tracking-wider"
          style={{ color: puzzle.displayColor ?? 'var(--tc-on-surface)' }}
        >
          {puzzle.display}
        </div>

        {justSolved ? (
          <div className="flex flex-col items-center gap-3 py-2">
            <div
              className="inline-flex items-center gap-2 rounded-full px-4 py-2 text-sm font-bold"
              style={{
                backgroundColor: 'var(--tc-primary-container)',
                color: 'var(--tc-on-primary-container)',
              }}
            >
              <CheckCircle2 className="h-4 w-4" />
              Alarm Silenced in {elapsedSec.toFixed(1)}s!
            </div>
            <button
              onClick={() => loadModality(modality)}
              className="tc-pressable inline-flex cursor-pointer items-center gap-2 rounded-xl px-4 py-2 text-xs font-bold"
              style={{
                backgroundColor: 'var(--tc-surface-high)',
                color: 'var(--tc-on-surface)',
              }}
            >
              <RefreshCw className="h-3.5 w-3.5" />
              Next Challenge
            </button>
          </div>
        ) : (
          <div className="mx-auto grid max-w-md grid-cols-2 gap-3">
            {puzzle.options.map((opt) => (
              <button
                key={opt}
                onClick={() => pickOption(opt)}
                className="tc-pressable cursor-pointer rounded-2xl py-3.5 font-mono-tech text-base font-bold"
                style={{
                  backgroundColor: 'var(--tc-surface-low)',
                  border: '1px solid var(--tc-outline-variant)',
                }}
              >
                {opt}
              </button>
            ))}
          </div>
        )}

        <p
          className="mt-4 text-xs"
          style={{ color: 'var(--tc-on-surface-variant)' }}
        >
          {puzzle.hint}
        </p>
      </div>
    </div>
  );
};
