# Timecure v2.0 — Cognitive Wake-Up Alarm & AI Focus Engine

**Live Showcase Website:** [https://timecure.vercel.app](https://timecure.vercel.app)  
**Latest Android APK:** [Download Timecure.apk](https://github.com/IMMENSE-BUILDER/Timecure/releases/latest/download/Timecure.apk)

**Timecure** is a personal Android alarm & deep-focus app built with Kotlin, Jetpack Compose, and Material Design 3. It forces maximum alarm volume over silent/mute/DND, takes over the lock screen, requires solving multi-step cognitive challenges before silencing, trims custom local MP3 alarm tracks frame-accurately, chains straight into a 60-tick Pomodoro chronometer, and integrates **Vercel AI Gateway** (`minimax/minimax-m3.1-flash` & `meta/muse-ember`) with multimodal vision and autonomous tool calling.

---

## Install on Android

Download `Timecure.apk` from the [GitHub Releases](https://github.com/IMMENSE-BUILDER/Timecure/releases/latest) page, or install over USB via ADB:

```bash
adb install -r Timecure.apk
```

### Build from Source

1. Create `local.properties` in the project root (git-ignored):
   ```properties
   sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk
   AI_GATEWAY_API_KEY=<your-vercel-ai-gateway-key>
   AI_GATEWAY_URL=https://ai-gateway.vercel.sh/v1/chat/completions
   AI_MODEL=minimax/minimax-m3.1-flash
   ```
2. Run unit tests & build a **keyless** redistributable APK (add your key later inside **Settings -> Vercel AI Gateway**):
   ```bash
   ./gradlew testDebugUnitTest assembleDebug
   ```
   *(For private USB deployment to your own device only, pass `-PincludeLocalSecrets=true` and never publish the resulting APK.)*

---

## Features

### 1. Unstoppable Alarm Engine
- **Doze-immune exact scheduling:** `AlarmManager.setAlarmClock()` with automatic reboot, timezone, and package-update recovery via `BootReceiver`.
- **Overrides silent, mute & DND:** `SoundManager` forces `STREAM_ALARM` to maximum volume, claims exclusive alarm audio focus, routes audio exclusively through a silent DND-bypass foreground channel (`timecure_alarm_v2`), and runs an escalating haptic waveform.
- **Lock-screen takeover & anti-escape:** `RingingActivity` wakes over the keyguard, intercepts hardware volume keys (`VOLUME_UP`, `VOLUME_DOWN`, `VOLUME_MUTE`), blocks accidental Back presses, and triggers an escalating re-ring (**+2 min → +5 min recurring**) if dismissed without solving.
- **5-minute anti-relapse wake check:** Optional per-alarm follow-up confirmation 5 minutes after silencing.
- **Personalized wake prompts:** Attach custom goals and prompts (e.g. *"Get up for college"*) to every alarm.

### 2. Cognitive Awakening Engine (6 Local Modalities + Vercel AI)
- **Arithmetic chains** — deterministic code-computed multi-step calculations.
- **Number sequences** — arithmetic, geometric, growing-gap, Fibonacci, and shifted-square patterns.
- **Reverse digits** — working-memory digit-span reversal.
- **Word scrambles** — guaranteed-shuffled anagrams with hints.
- **Stroop color-word test** — tap the *ink color*, not the printed word.
- **Morning schedule math** — elapsed sprint time calculations (`H:MM`).

### 3. Vercel AI Gateway — Vision & 7 Autonomous Tools
- Supports **`minimax/minimax-m3.1-flash`** and **`meta/muse-ember`** via Settings dropdown, plus editable **AI System Instructions**.
- **Camera & Gallery Vision:** Upload timetables, routines, or study sheets to automatically generate a structured **AI Task Table** persisted in SQLite v6 and turn items directly into alarms or Pomodoro sprints.
- **7 Native Function-Calling Tools:** `create_alarm`, `toggle_alarm`, `delete_alarm`, `start_pomodoro`, `save_task_table`, `set_theme`, and `update_profile`.

### 4. Integrated Pomodoro Focus Chronometer
- Precision **60-tick Canvas chronometer dial** with breathing halo, rolling odometer digits, foreground `PomodoroService`, configurable presets, SQLite focus telemetry, and one-tap alarm victory → 25m focus sprint handoff.

### 5. Local MP3 Browser & Waveform Trimmer
- Scans all MP3 files on phone storage (`READ_MEDIA_AUDIO` + SAF) with an interactive dual-handle Canvas waveform trimmer (`Mp3Trimmer`) for frame-accurate MPEG Layer III slicing.

### 6. Material Design 3 Themes & Zero-Latency Architecture
- **6 Light & Dark Material 3 Palettes** (`Ocean Dawn`, `Emerald`, `Sunset Coral`, `Amethyst`, `Graphite`, `Crimson`) plus Android 12+ Dynamic Wallpaper color.
- **Warm Tab Caching & DrawScope Canvas:** All 4 destinations pre-warm at startup and stay mounted underneath overlays for instant 0ms switches, GPU `graphicsLayer` transitions, zero 60fps recomposition storms, and tactile spring press-down + haptic feedback.

---

## Repository Structure

- `app/` — Native Android application (Kotlin, Jetpack Compose, Material 3, SQLite, OkHttp)
- `website/` — Interactive product showcase website deployed on Vercel ([timecure.vercel.app](https://timecure.vercel.app))
