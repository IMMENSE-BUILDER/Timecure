import React, { useEffect, useState } from 'react';
import {
  AlarmClock,
  ArrowRight,
  BellOff,
  Check,
  CheckCircle2,
  Copy,
  Download,
  Gauge,
  Github,
  Layers,
  Lock,
  Moon,
  Palette,
  RefreshCcw,
  ShieldAlert,
  Sparkles,
  Sun,
  Terminal,
  Timer,
  Volume2,
  Zap,
} from 'lucide-react';
import {
  APK_DOWNLOAD_URL,
  PaletteId,
  REPO_URL,
  TIMECURE_PALETTES,
} from './themes';
import { TimecureLogo } from './components/TimecureLogo';
import { InteractivePhone } from './components/InteractivePhone';
import { CognitiveDojoDemo } from './components/CognitiveDojoDemo';
import { Mp3TrimmerDemo } from './components/Mp3TrimmerDemo';
import { AiVisionShowcase } from './components/AiVisionShowcase';

export function App() {
  const [paletteId, setPaletteId] = useState<PaletteId>('OCEAN_DAWN');
  const [darkMode, setDarkMode] = useState(true);
  const [selectedModel, setSelectedModel] = useState('minimax/minimax-m3.1-flash');
  const [copiedAdb, setCopiedAdb] = useState(false);

  // Sync the exact selected Material 3 palette into CSS custom properties site-wide
  useEffect(() => {
    const pal = TIMECURE_PALETTES.find((p) => p.id === paletteId) ?? TIMECURE_PALETTES[0];
    const t = darkMode ? pal.dark : pal.light;
    const root = document.documentElement;
    root.classList.toggle('dark', darkMode);
    root.style.setProperty('--tc-bg', t.bg);
    root.style.setProperty('--tc-surface', t.surface);
    root.style.setProperty('--tc-surface-low', t.surfaceLow);
    root.style.setProperty('--tc-surface-high', t.surfaceHigh);
    root.style.setProperty('--tc-surface-highest', t.surfaceHighest);
    root.style.setProperty('--tc-primary', t.primary);
    root.style.setProperty('--tc-on-primary', t.onPrimary);
    root.style.setProperty('--tc-primary-container', t.primaryContainer);
    root.style.setProperty('--tc-on-primary-container', t.onPrimaryContainer);
    root.style.setProperty('--tc-secondary', t.secondary);
    root.style.setProperty('--tc-secondary-container', t.secondaryContainer);
    root.style.setProperty('--tc-tertiary', t.tertiary);
    root.style.setProperty('--tc-tertiary-container', t.tertiaryContainer);
    root.style.setProperty('--tc-on-surface', t.onSurface);
    root.style.setProperty('--tc-on-surface-variant', t.onSurfaceVariant);
    root.style.setProperty('--tc-outline', t.outline);
    root.style.setProperty('--tc-outline-variant', t.outlineVariant);
  }, [paletteId, darkMode]);

  const copyAdbCommand = () => {
    navigator.clipboard?.writeText('adb install -r Timecure.apk');
    setCopiedAdb(true);
    window.setTimeout(() => setCopiedAdb(false), 2000);
  };

  const activePalette =
    TIMECURE_PALETTES.find((p) => p.id === paletteId) ?? TIMECURE_PALETTES[0];

  return (
    <div className="relative min-h-screen overflow-x-hidden">
      {/* Subtle Ambient Architectural Background */}
      <div
        className="pointer-events-none fixed inset-0 -z-10 tc-grid-bg opacity-60"
        aria-hidden="true"
      />
      <div
        className="pointer-events-none fixed -top-40 left-1/2 -z-10 h-[540px] w-[960px] -translate-x-1/2 rounded-full blur-[140px] opacity-20 transition-colors duration-700"
        style={{
          background:
            'radial-gradient(circle, var(--tc-primary) 0%, var(--tc-tertiary) 55%, transparent 75%)',
        }}
        aria-hidden="true"
      />

      {/* Floating Glass Navigation Bar */}
      <header className="sticky top-0 z-40 px-4 pt-4">
        <nav
          className="mx-auto flex max-w-6xl items-center justify-between rounded-full px-4 py-2.5 backdrop-blur-xl transition-colors"
          style={{
            backgroundColor: 'color-mix(in srgb, var(--tc-surface) 82%, transparent)',
            border: '1px solid var(--tc-outline-variant)',
          }}
        >
          <a href="#top" className="flex items-center gap-2.5">
            <TimecureLogo size={34} />
            <div className="leading-none">
              <div className="text-sm font-extrabold tracking-tight">Timecure</div>
              <div
                className="font-mono-tech text-[10px]"
                style={{ color: 'var(--tc-on-surface-variant)' }}
              >
                v2.0 • Android M3
              </div>
            </div>
          </a>

          <div
            className="hidden items-center gap-6 text-xs font-semibold md:flex"
            style={{ color: 'var(--tc-on-surface-variant)' }}
          >
            <a href="#architecture" className="hover:opacity-100 opacity-85 transition">
              Alarm Engine
            </a>
            <a href="#dojo" className="hover:opacity-100 opacity-85 transition">
              Cognitive Dojo
            </a>
            <a href="#ai-studio" className="hover:opacity-100 opacity-85 transition">
              Vercel AI
            </a>
            <a href="#audio" className="hover:opacity-100 opacity-85 transition">
              MP3 Trimmer
            </a>
            <a href="#themes" className="hover:opacity-100 opacity-85 transition">
              Themes
            </a>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={() => setDarkMode((d) => !d)}
              aria-label="Toggle Light/Dark mode"
              className="tc-pressable flex h-9 w-9 cursor-pointer items-center justify-center rounded-full"
              style={{
                backgroundColor: 'var(--tc-surface-low)',
                border: '1px solid var(--tc-outline-variant)',
              }}
            >
              {darkMode ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
            </button>

            <a
              href={REPO_URL}
              target="_blank"
              rel="noreferrer"
              className="tc-pressable hidden sm:inline-flex h-9 items-center gap-1.5 rounded-full px-3.5 text-xs font-semibold"
              style={{
                backgroundColor: 'var(--tc-surface-low)',
                border: '1px solid var(--tc-outline-variant)',
              }}
            >
              <Github className="h-3.5 w-3.5" />
              GitHub
            </a>

            <a
              href="#download"
              className="tc-pressable inline-flex h-9 items-center gap-1.5 rounded-full px-4 text-xs font-bold"
              style={{
                backgroundColor: 'var(--tc-primary)',
                color: 'var(--tc-on-primary)',
              }}
            >
              <Download className="h-3.5 w-3.5" />
              Get APK
            </a>
          </div>
        </nav>
      </header>

      <main id="top" className="mx-auto max-w-6xl px-4 pb-24">
        {/* HERO SECTION */}
        <section className="grid items-center gap-12 pt-12 pb-16 lg:grid-cols-12 lg:pt-16">
          <div className="space-y-6 lg:col-span-7">
            <div className="inline-flex flex-wrap items-center gap-2">
              <span
                className="inline-flex items-center gap-2 rounded-full px-3.5 py-1.5 text-xs font-semibold"
                style={{
                  backgroundColor: 'var(--tc-primary-container)',
                  color: 'var(--tc-on-primary-container)',
                }}
              >
                <span
                  className="h-2 w-2 rounded-full animate-tc-pulse"
                  style={{ backgroundColor: 'var(--tc-tertiary)' }}
                />
                Android 14+ • Material Design 3 • Zero-Latency Compose
              </span>
            </div>

            <h1 className="text-4xl sm:text-6xl lg:text-[64px] font-extrabold tracking-tight leading-[1.03]">
              The alarm that{' '}
              <span
                className="font-serif-display italic font-normal"
                style={{ color: 'var(--tc-primary)' }}
              >
                refuses
              </span>{' '}
              to let you lose the morning.
            </h1>

            <p
              className="max-w-xl text-base sm:text-lg leading-relaxed"
              style={{ color: 'var(--tc-on-surface-variant)' }}
            >
              <strong>Timecure</strong> overrides silent mode and DND, locks your screen until you
              solve cognitive awakening puzzles, trims your favorite phone MP3s frame-accurately,
              and chains straight into a 60-tick Pomodoro chronometer — supercharged by{' '}
              <strong>Vercel AI Gateway</strong> vision and autonomous tool calling.
            </p>

            {/* Primary Action Row */}
            <div className="flex flex-wrap items-center gap-3 pt-1">
              <a
                href={APK_DOWNLOAD_URL}
                className="tc-pressable inline-flex items-center gap-2.5 rounded-2xl px-6 py-3.5 text-sm font-bold shadow-lg"
                style={{
                  backgroundColor: 'var(--tc-primary)',
                  color: 'var(--tc-on-primary)',
                }}
              >
                <Download className="h-4 w-4" />
                Download Timecure.apk
                <ArrowRight className="h-4 w-4" />
              </a>

              <a
                href={REPO_URL}
                target="_blank"
                rel="noreferrer"
                className="tc-pressable inline-flex items-center gap-2 rounded-2xl px-5 py-3.5 text-sm font-semibold"
                style={{
                  backgroundColor: 'var(--tc-surface-low)',
                  border: '1px solid var(--tc-outline-variant)',
                }}
              >
                <Github className="h-4 w-4" />
                View Source Code
              </a>
            </div>

            {/* Live Quick-Switch Palette Strip */}
            <div className="pt-2">
              <div
                className="mb-2 flex items-center gap-2 text-[11px] font-bold uppercase tracking-wider"
                style={{ color: 'var(--tc-on-surface-variant)' }}
              >
                <Palette className="h-3.5 w-3.5" style={{ color: 'var(--tc-primary)' }} />
                Live Theme Engine ({activePalette.name} — {darkMode ? 'Dark' : 'Light'})
              </div>
              <div className="flex flex-wrap gap-2">
                {TIMECURE_PALETTES.map((pal) => {
                  const active = pal.id === paletteId;
                  return (
                    <button
                      key={pal.id}
                      onClick={() => setPaletteId(pal.id)}
                      className="tc-pressable inline-flex cursor-pointer items-center gap-2 rounded-full px-3 py-1.5 text-xs font-semibold"
                      style={{
                        backgroundColor: active
                          ? 'var(--tc-primary-container)'
                          : 'var(--tc-surface-low)',
                        color: active
                          ? 'var(--tc-on-primary-container)'
                          : 'var(--tc-on-surface)',
                        border: `1.5px solid ${
                          active ? 'var(--tc-primary)' : 'var(--tc-outline-variant)'
                        }`,
                      }}
                    >
                      <span
                        className="h-2.5 w-2.5 rounded-full"
                        style={{ backgroundColor: pal.preview[0] }}
                      />
                      {pal.name}
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Key Engineering Stats */}
            <div
              className="grid grid-cols-2 gap-3 pt-2 sm:grid-cols-4"
            >
              {[
                { val: '0ms', label: 'Warm Tab Switch' },
                { val: '6', label: 'Cognitive Engines' },
                { val: '60-tick', label: 'Canvas Chronometer' },
                { val: '7 Tools', label: 'Vercel AI Agent' },
              ].map((stat) => (
                <div
                  key={stat.label}
                  className="rounded-2xl p-3.5"
                  style={{
                    backgroundColor: 'var(--tc-surface-low)',
                    border: '1px solid var(--tc-outline-variant)',
                  }}
                >
                  <div
                    className="font-mono-tech text-xl font-bold"
                    style={{ color: 'var(--tc-primary)' }}
                  >
                    {stat.val}
                  </div>
                  <div
                    className="mt-0.5 text-[11px] font-medium"
                    style={{ color: 'var(--tc-on-surface-variant)' }}
                  >
                    {stat.label}
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* RIGHT: LIVE INTERACTIVE ANDROID PHONE SIMULATOR */}
          <div className="flex flex-col items-center lg:col-span-5">
            <InteractivePhone
              paletteId={paletteId}
              darkMode={darkMode}
              selectedModel={selectedModel}
              onSelectPalette={setPaletteId}
              onToggleDarkMode={() => setDarkMode((d) => !d)}
              onSelectModel={setSelectedModel}
            />
            <p
              className="mt-4 text-center text-xs"
              style={{ color: 'var(--tc-on-surface-variant)' }}
            >
              Interactive preview — tap tabs, start Pomodoro, switch themes, or click{' '}
              <strong style={{ color: 'var(--tc-primary)' }}>Simulate Unstoppable Ring</strong>.
            </p>
          </div>
        </section>

        {/* SECTION 2: UNSTOPPABLE ALARM LIFECYCLE */}
        <section id="architecture" className="py-12">
          <div className="mb-8 flex flex-wrap items-end justify-between gap-4">
            <div>
              <div
                className="inline-flex items-center gap-1.5 text-xs font-bold uppercase tracking-wider"
                style={{ color: 'var(--tc-primary)' }}
              >
                <ShieldAlert className="h-4 w-4" />
                Unstoppable Alarm Engine
              </div>
              <h2 className="mt-1 text-3xl sm:text-4xl font-extrabold tracking-tight">
                Engineered so sleep-drunk fingers can’t cheat.
              </h2>
            </div>
            <span
              className="font-mono-tech text-xs rounded-full px-3.5 py-1.5"
              style={{
                backgroundColor: 'var(--tc-surface-low)',
                border: '1px solid var(--tc-outline-variant)',
                color: 'var(--tc-on-surface-variant)',
              }}
            >
              AlarmManager.setAlarmClock() • STREAM_ALARM • BootReceiver
            </span>
          </div>

          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
            {[
              {
                step: '01',
                icon: AlarmClock,
                title: 'Doze-Immune Exact Firing',
                desc: 'Scheduled via AlarmManager.setAlarmClock() with automatic reboot, timezone, and package-update re-arming through BootReceiver.',
              },
              {
                step: '02',
                icon: Volume2,
                title: 'Max Volume & DND Override',
                desc: 'SoundManager forces STREAM_ALARM to maximum, claims exclusive alarm audio focus, bypasses Do Not Disturb, and pairs with an escalating haptic waveform.',
              },
              {
                step: '03',
                icon: Lock,
                title: 'Lock-Screen Takeover',
                desc: 'RingingActivity wakes over the keyguard, intercepts hardware VOLUME_UP / VOLUME_DOWN / MUTE keys, and blocks accidental Back presses.',
              },
              {
                step: '04',
                icon: BellOff,
                title: 'Escalating Re-Ring Defense',
                desc: 'Force-closing or dodging the alarm triggers an automatic escalating re-ring (+2 min → +5 min recurring) until every cognitive step is solved.',
              },
              {
                step: '05',
                icon: RefreshCcw,
                title: '5-Min Anti-Relapse Wake Check',
                desc: 'Optional per-alarm follow-up confirmation 5 minutes after silencing. Fall back asleep and Timecure immediately re-fires.',
              },
              {
                step: '06',
                icon: Timer,
                title: 'Instant Pomodoro Handoff',
                desc: 'Victory card transitions directly into a 25-minute Deep Focus sprint pre-loaded with your alarm goal (like “Get up for college”).',
              },
            ].map((item) => {
              const Icon = item.icon;
              return (
                <div
                  key={item.step}
                  className="group rounded-3xl p-6 transition-all duration-200 hover:-translate-y-0.5"
                  style={{
                    backgroundColor: 'var(--tc-surface-low)',
                    border: '1px solid var(--tc-outline-variant)',
                  }}
                >
                  <div className="flex items-center justify-between">
                    <div
                      className="flex h-11 w-11 items-center justify-center rounded-2xl"
                      style={{
                        backgroundColor: 'var(--tc-primary-container)',
                        color: 'var(--tc-on-primary-container)',
                      }}
                    >
                      <Icon className="h-5 w-5" />
                    </div>
                    <span
                      className="font-mono-tech text-xs font-bold opacity-50"
                    >
                      STAGE {item.step}
                    </span>
                  </div>
                  <h3 className="mt-4 text-lg font-bold tracking-tight">
                    {item.title}
                  </h3>
                  <p
                    className="mt-2 text-sm leading-relaxed"
                    style={{ color: 'var(--tc-on-surface-variant)' }}
                  >
                    {item.desc}
                  </p>
                </div>
              );
            })}
          </div>
        </section>

        {/* SECTION 3: INTERACTIVE COGNITIVE DOJO */}
        <section id="dojo" className="py-10">
          <CognitiveDojoDemo />
        </section>

        {/* SECTION 4: VERCEL AI GATEWAY & MULTIMODAL VISION STUDIO */}
        <section id="ai-studio" className="py-10">
          <AiVisionShowcase
            selectedModel={selectedModel}
            onSelectModel={setSelectedModel}
          />
        </section>

        {/* SECTION 5: INTERACTIVE MP3 WAVEFORM TRIMMER */}
        <section id="audio" className="py-10">
          <Mp3TrimmerDemo />
        </section>

        {/* SECTION 6: ZERO-LATENCY PERFORMANCE + 6 THEMES BENTO */}
        <section id="themes" className="grid gap-6 py-10 lg:grid-cols-12">
          {/* Zero-Latency Architecture Card */}
          <div
            className="rounded-3xl p-6 sm:p-8 lg:col-span-6"
            style={{
              backgroundColor: 'var(--tc-surface-low)',
              border: '1px solid var(--tc-outline-variant)',
            }}
          >
            <div
              className="inline-flex items-center gap-1.5 text-xs font-bold uppercase tracking-wider"
              style={{ color: 'var(--tc-primary)' }}
            >
              <Gauge className="h-4 w-4" />
              Zero-Latency Jetpack Compose Engine
            </div>
            <h3 className="mt-1 text-2xl font-bold tracking-tight">
              Tuned for 120Hz fluidity on real hardware.
            </h3>
            <p
              className="mt-2 text-sm leading-relaxed"
              style={{ color: 'var(--tc-on-surface-variant)' }}
            >
              Built in pure Kotlin + Jetpack Compose + Material 3 with zero bloat and surgical
              frame-time optimization verified on Android 14 hardware.
            </p>

            <div className="mt-5 space-y-3">
              {[
                {
                  icon: Zap,
                  title: 'Warm Tab Caching (0ms Switch)',
                  desc: 'All 4 destinations pre-warm after startup and stay mounted underneath overlays — zero screen teardown lag.',
                },
                {
                  icon: Layers,
                  title: 'GPU graphicsLayer & DrawScope Canvas',
                  desc: 'Directional slide transitions, 60-tick chronometer, and breathing rings read animated state solely in DrawScope — zero 60fps recompositions.',
                },
                {
                  icon: Sparkles,
                  title: 'Tactile Spring Click Feedback',
                  desc: 'Instant finger-down spring scale (0.88–0.96x), bounded M3 ripples, and crisp haptic ticks on every interactive control.',
                },
              ].map((perf) => {
                const Icon = perf.icon;
                return (
                  <div
                    key={perf.title}
                    className="flex gap-3.5 rounded-2xl p-3.5"
                    style={{ backgroundColor: 'var(--tc-surface)' }}
                  >
                    <div
                      className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl"
                      style={{
                        backgroundColor: 'var(--tc-primary-container)',
                        color: 'var(--tc-on-primary-container)',
                      }}
                    >
                      <Icon className="h-4 w-4" />
                    </div>
                    <div>
                      <div className="text-sm font-bold">{perf.title}</div>
                      <div
                        className="mt-0.5 text-xs leading-relaxed"
                        style={{ color: 'var(--tc-on-surface-variant)' }}
                      >
                        {perf.desc}
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* 6 Material 3 Themes Studio Card */}
          <div
            className="rounded-3xl p-6 sm:p-8 lg:col-span-6"
            style={{
              backgroundColor: 'var(--tc-surface-low)',
              border: '1px solid var(--tc-outline-variant)',
            }}
          >
            <div className="flex items-center justify-between gap-2">
              <div>
                <div
                  className="inline-flex items-center gap-1.5 text-xs font-bold uppercase tracking-wider"
                  style={{ color: 'var(--tc-tertiary)' }}
                >
                  <Palette className="h-4 w-4" />
                  Material 3 Color Studio
                </div>
                <h3 className="mt-1 text-2xl font-bold tracking-tight">
                  6 handcrafted Light & Dark palettes.
                </h3>
              </div>
              <button
                onClick={() => setDarkMode((d) => !d)}
                className="tc-pressable inline-flex cursor-pointer items-center gap-1.5 rounded-full px-3.5 py-2 text-xs font-bold"
                style={{
                  backgroundColor: 'var(--tc-surface-high)',
                }}
              >
                {darkMode ? <Moon className="h-3.5 w-3.5" /> : <Sun className="h-3.5 w-3.5" />}
                {darkMode ? 'Dark' : 'Light'}
              </button>
            </div>

            <p
              className="mt-2 text-sm"
              style={{ color: 'var(--tc-on-surface-variant)' }}
            >
              Tap any card below to re-theme the entire page and the live phone simulator using
              Timecure’s exact Android <code className="font-mono-tech text-xs">Theme.kt</code>{' '}
              tokens (plus Android 12+ Dynamic Wallpaper support on device).
            </p>

            <div className="mt-5 grid grid-cols-2 gap-3">
              {TIMECURE_PALETTES.map((pal) => {
                const active = pal.id === paletteId;
                return (
                  <button
                    key={pal.id}
                    onClick={() => setPaletteId(pal.id)}
                    className="tc-pressable flex cursor-pointer flex-col gap-2 rounded-2xl p-4 text-left"
                    style={{
                      backgroundColor: active
                        ? 'var(--tc-primary-container)'
                        : 'var(--tc-surface)',
                      color: active
                        ? 'var(--tc-on-primary-container)'
                        : 'var(--tc-on-surface)',
                      border: `2px solid ${
                        active ? 'var(--tc-primary)' : 'var(--tc-outline-variant)'
                      }`,
                    }}
                  >
                    <div className="flex items-center justify-between">
                      <div className="flex gap-1.5">
                        {pal.preview.map((hex) => (
                          <span
                            key={hex}
                            className="h-4 w-4 rounded-full ring-1 ring-black/15"
                            style={{ backgroundColor: hex }}
                          />
                        ))}
                      </div>
                      {active && (
                        <CheckCircle2
                          className="h-4 w-4"
                          style={{ color: 'var(--tc-primary)' }}
                        />
                      )}
                    </div>
                    <div>
                      <div className="text-sm font-bold">{pal.name}</div>
                      <div className="text-[11px] opacity-75">{pal.tagline}</div>
                    </div>
                  </button>
                );
              })}
            </div>
          </div>
        </section>

        {/* SECTION 7: DOWNLOAD & GITHUB DEPLOYMENT CTA */}
        <section id="download" className="pt-10">
          <div
            className="relative overflow-hidden rounded-[32px] p-8 sm:p-12"
            style={{
              backgroundColor: 'var(--tc-primary-container)',
              color: 'var(--tc-on-primary-container)',
            }}
          >
            <div className="grid items-center gap-8 lg:grid-cols-12">
              <div className="space-y-4 lg:col-span-7">
                <div className="inline-flex items-center gap-2 rounded-full bg-black/15 px-3.5 py-1 text-xs font-bold uppercase tracking-wider">
                  Open Source • Kotlin + Compose • API 26–36
                </div>
                <h2 className="text-3xl sm:text-5xl font-extrabold tracking-tight leading-tight">
                  Own every morning with{' '}
                  <span className="font-serif-display italic font-normal">Timecure.</span>
                </h2>
                <p className="max-w-xl text-sm sm:text-base opacity-90 leading-relaxed">
                  Download the signed debug APK directly from the GitHub Release, or clone the full
                  Android & Web repository and build locally with Gradle.
                </p>

                <div className="flex flex-wrap items-center gap-3 pt-2">
                  <a
                    href={APK_DOWNLOAD_URL}
                    className="tc-pressable inline-flex items-center gap-2 rounded-2xl px-6 py-3.5 text-sm font-bold shadow-lg"
                    style={{
                      backgroundColor: 'var(--tc-primary)',
                      color: 'var(--tc-on-primary)',
                    }}
                  >
                    <Download className="h-4 w-4" />
                    Download Timecure.apk (v2.0)
                  </a>
                  <a
                    href={REPO_URL}
                    target="_blank"
                    rel="noreferrer"
                    className="tc-pressable inline-flex items-center gap-2 rounded-2xl bg-black/15 px-5 py-3.5 text-sm font-bold backdrop-blur-sm"
                  >
                    <Github className="h-4 w-4" />
                    IMMENSE-BUILDER/Timecure
                  </a>
                </div>
              </div>

              <div className="lg:col-span-5">
                <div className="rounded-2xl bg-black/30 p-5 font-mono-tech text-xs backdrop-blur-md">
                  <div className="flex items-center justify-between border-b border-white/15 pb-2.5 text-[11px] opacity-85">
                    <span className="inline-flex items-center gap-1.5">
                      <Terminal className="h-3.5 w-3.5" />
                      Install via ADB
                    </span>
                    <button
                      onClick={copyAdbCommand}
                      className="tc-pressable inline-flex cursor-pointer items-center gap-1 rounded-lg bg-white/10 px-2.5 py-1 text-[10px] font-bold hover:bg-white/20"
                    >
                      {copiedAdb ? (
                        <>
                          <Check className="h-3 w-3" /> Copied
                        </>
                      ) : (
                        <>
                          <Copy className="h-3 w-3" /> Copy
                        </>
                      )}
                    </button>
                  </div>
                  <div className="mt-3 space-y-2 text-[11px] leading-relaxed">
                    <div>
                      <span className="opacity-50">$ </span>adb install -r Timecure.apk
                    </div>
                    <div>
                      <span className="opacity-50">$ </span>./gradlew testDebugUnitTest assembleDebug
                    </div>
                  </div>
                  <div className="mt-4 grid grid-cols-2 gap-2 border-t border-white/15 pt-3 text-[10px] opacity-85">
                    <div>✓ Exact Alarm Access</div>
                    <div>✓ Full-Screen Intent</div>
                    <div>✓ DND Bypass Channel</div>
                    <div>✓ Unrestricted Battery</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>
      </main>

      {/* Footer */}
      <footer
        className="border-t py-8 text-center text-xs"
        style={{
          borderColor: 'var(--tc-outline-variant)',
          color: 'var(--tc-on-surface-variant)',
        }}
      >
        <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-4 px-4">
          <div className="flex items-center gap-2">
            <TimecureLogo size={24} />
            <span className="font-semibold" style={{ color: 'var(--tc-on-surface)' }}>
              Timecure
            </span>
            <span>• Built with Kotlin, Jetpack Compose, Material 3 & Vercel AI Gateway</span>
          </div>
          <div className="flex items-center gap-4">
            <a href={REPO_URL} target="_blank" rel="noreferrer" className="hover:underline">
              GitHub Repository
            </a>
            <a href={APK_DOWNLOAD_URL} className="hover:underline">
              Latest APK Release
            </a>
          </div>
        </div>
      </footer>
    </div>
  );
}

export default App;
