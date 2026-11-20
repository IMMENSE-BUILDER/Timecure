import React, { useEffect, useMemo, useRef, useState } from 'react';
import { AudioWaveform, Pause, Play, Scissors } from 'lucide-react';

const TOTAL_SECONDS = 184; // 3:04 track

function fmtTime(sec: number): string {
  const m = Math.floor(sec / 60);
  const s = Math.floor(sec % 60);
  const d = Math.floor((sec % 1) * 10);
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}.${d}`;
}

export const Mp3TrimmerDemo: React.FC = () => {
  const [startPct, setStartPct] = useState(0.24);
  const [endPct, setEndPct] = useState(0.56);
  const [playing, setPlaying] = useState(false);
  const [playheadPct, setPlayheadPct] = useState(0.24);
  const [dragging, setDragging] = useState<'start' | 'end' | null>(null);
  const barRef = useRef<HTMLDivElement | null>(null);
  const audioCtxRef = useRef<AudioContext | null>(null);
  const audioTimerRef = useRef<number | null>(null);

  // Deterministic musical waveform bars
  const bars = useMemo(() => {
    return Array.from({ length: 64 }, (_, i) => {
      const wave =
        Math.sin(i * 0.42) * 0.28 +
        Math.cos(i * 1.15) * 0.22 +
        Math.sin(i * 2.3) * 0.14 +
        0.52;
      return Math.max(0.16, Math.min(0.98, wave));
    });
  }, []);

  // Gentle Web Audio synth preview when user clicks Preview Slice
  useEffect(() => {
    if (!playing) {
      if (audioTimerRef.current) window.clearInterval(audioTimerRef.current);
      return;
    }

    const AudioCtx = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
    if (!audioCtxRef.current && AudioCtx) {
      audioCtxRef.current = new AudioCtx();
    }
    const ctx = audioCtxRef.current;
    const notes = [329.63, 392.0, 440.0, 523.25, 587.33, 523.25, 440.0, 392.0];
    let step = 0;

    const triggerNote = () => {
      if (!ctx) return;
      if (ctx.state === 'suspended') ctx.resume();
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = 'triangle';
      osc.frequency.value = notes[step % notes.length];
      step++;
      const now = ctx.currentTime;
      gain.gain.setValueAtTime(0.001, now);
      gain.gain.exponentialRampToValueAtTime(0.055, now + 0.03);
      gain.gain.exponentialRampToValueAtTime(0.0001, now + 0.26);
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.start(now);
      osc.stop(now + 0.27);
    };

    triggerNote();
    audioTimerRef.current = window.setInterval(triggerNote, 260);

    let raf = 0;
    let last = performance.now();
    const tick = (now: number) => {
      const dt = (now - last) / 1000;
      last = now;
      setPlayheadPct((p) => {
        const next = p + dt * 0.08;
        return next >= endPct ? startPct : next;
      });
      raf = requestAnimationFrame(tick);
    };
    raf = requestAnimationFrame(tick);

    return () => {
      cancelAnimationFrame(raf);
      if (audioTimerRef.current) window.clearInterval(audioTimerRef.current);
    };
  }, [playing, startPct, endPct]);

  const handlePointerMove = (e: React.PointerEvent<HTMLDivElement>) => {
    if (!dragging || !barRef.current) return;
    const rect = barRef.current.getBoundingClientRect();
    const pct = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width));
    if (dragging === 'start') {
      const clamped = Math.min(pct, endPct - 0.08);
      setStartPct(clamped);
      setPlayheadPct(clamped);
    } else {
      const clamped = Math.max(pct, startPct + 0.08);
      setEndPct(clamped);
    }
  };

  const startSec = startPct * TOTAL_SECONDS;
  const endSec = endPct * TOTAL_SECONDS;
  const clipDuration = endSec - startSec;

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
            style={{ color: 'var(--tc-tertiary)' }}
          >
            <AudioWaveform className="h-4 w-4" />
            Local MP3 Scanner & Frame-Accurate Trimmer
          </div>
          <h3 className="mt-1 text-2xl font-bold tracking-tight">
            Drop straight into the chorus of any phone MP3.
          </h3>
        </div>
        <span
          className="rounded-full px-3 py-1 font-mono-tech text-xs font-semibold"
          style={{ backgroundColor: 'var(--tc-surface-high)' }}
        >
          midnight_drive_chorus.mp3
        </span>
      </div>

      <p className="mt-2 text-sm" style={{ color: 'var(--tc-on-surface-variant)' }}>
        Drag the start and end handles below — Timecure scans local storage via{' '}
        <code className="font-mono-tech text-xs">READ_MEDIA_AUDIO</code> / SAF and slices exact MPEG Layer III frames so your favorite hook plays instantly on wake.
      </p>

      {/* Interactive Waveform Trimmer */}
      <div
        ref={barRef}
        onPointerMove={handlePointerMove}
        onPointerUp={() => setDragging(null)}
        onPointerLeave={() => setDragging(null)}
        className="relative mt-6 h-32 select-none touch-none rounded-2xl p-4"
        style={{
          backgroundColor: 'var(--tc-surface)',
          border: '1px solid var(--tc-outline-variant)',
        }}
      >
        {/* Highlighted trimmed window */}
        <div
          className="pointer-events-none absolute top-2 bottom-2 rounded-xl transition-colors"
          style={{
            left: `calc(${startPct * 100}% + 12px * ${1 - startPct * 2})`,
            right: `calc(${(1 - endPct) * 100}% + 12px * ${endPct * 2 - 1})`,
            backgroundColor: 'color-mix(in srgb, var(--tc-primary) 16%, transparent)',
            borderTop: '2px solid var(--tc-primary)',
            borderBottom: '2px solid var(--tc-primary)',
          }}
        />

        {/* Bars */}
        <div className="relative z-10 flex h-full items-center gap-[3px]">
          {bars.map((h, i) => {
            const pct = i / (bars.length - 1);
            const inRange = pct >= startPct && pct <= endPct;
            const passed = playing && pct >= startPct && pct <= playheadPct;
            return (
              <div
                key={i}
                className="flex-1 rounded-full transition-colors duration-100"
                style={{
                  height: `${Math.round(h * 100)}%`,
                  backgroundColor: passed
                    ? 'var(--tc-tertiary)'
                    : inRange
                    ? 'var(--tc-primary)'
                    : 'var(--tc-outline-variant)',
                  opacity: inRange ? 1 : 0.45,
                }}
              />
            );
          })}
        </div>

        {/* Playhead */}
        {playing && (
          <div
            className="pointer-events-none absolute top-2 bottom-2 z-20 w-[2px]"
            style={{
              left: `${playheadPct * 100}%`,
              backgroundColor: 'var(--tc-tertiary)',
            }}
          />
        )}

        {/* Start Handle */}
        <button
          onPointerDown={(e) => {
            e.currentTarget.setPointerCapture(e.pointerId);
            setDragging('start');
          }}
          aria-label="Drag trim start"
          className="absolute top-1.5 bottom-1.5 z-30 flex w-5 -translate-x-1/2 cursor-ew-resize items-center justify-center rounded-lg shadow-lg"
          style={{
            left: `${startPct * 100}%`,
            backgroundColor: 'var(--tc-primary)',
            color: 'var(--tc-on-primary)',
          }}
        >
          <span className="h-6 w-0.5 rounded-full bg-current" />
        </button>

        {/* End Handle */}
        <button
          onPointerDown={(e) => {
            e.currentTarget.setPointerCapture(e.pointerId);
            setDragging('end');
          }}
          aria-label="Drag trim end"
          className="absolute top-1.5 bottom-1.5 z-30 flex w-5 -translate-x-1/2 cursor-ew-resize items-center justify-center rounded-lg shadow-lg"
          style={{
            left: `${endPct * 100}%`,
            backgroundColor: 'var(--tc-primary)',
            color: 'var(--tc-on-primary)',
          }}
        >
          <span className="h-6 w-0.5 rounded-full bg-current" />
        </button>
      </div>

      {/* Controls & Readout */}
      <div className="mt-4 flex flex-wrap items-center justify-between gap-3">
        <div className="flex flex-wrap items-center gap-3 font-mono-tech text-xs">
          <span
            className="rounded-xl px-3 py-1.5"
            style={{ backgroundColor: 'var(--tc-surface-high)' }}
          >
            IN <strong style={{ color: 'var(--tc-primary)' }}>{fmtTime(startSec)}</strong>
          </span>
          <span
            className="rounded-xl px-3 py-1.5"
            style={{ backgroundColor: 'var(--tc-surface-high)' }}
          >
            OUT <strong style={{ color: 'var(--tc-primary)' }}>{fmtTime(endSec)}</strong>
          </span>
          <span
            className="inline-flex items-center gap-1 rounded-xl px-3 py-1.5"
            style={{
              backgroundColor: 'var(--tc-primary-container)',
              color: 'var(--tc-on-primary-container)',
            }}
          >
            <Scissors className="h-3.5 w-3.5" />
            {clipDuration.toFixed(1)}s loop
          </span>
        </div>

        <button
          onClick={() => {
            setPlayheadPct(startPct);
            setPlaying((p) => !p);
          }}
          className="tc-pressable inline-flex cursor-pointer items-center gap-2 rounded-full px-5 py-2.5 text-xs font-bold"
          style={{
            backgroundColor: playing
              ? 'var(--tc-primary-container)'
              : 'var(--tc-primary)',
            color: playing
              ? 'var(--tc-on-primary-container)'
              : 'var(--tc-on-primary)',
          }}
        >
          {playing ? <Pause className="h-4 w-4" /> : <Play className="h-4 w-4" />}
          {playing ? 'Stop Audio Preview' : 'Play Trimmed Preview'}
        </button>
      </div>
    </div>
  );
};
