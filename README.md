# Timecure v2.0 — Cognitive Wake-Up Alarm, Location Tasks & Server-Side AI

**Interactive Website (Touch-Simulated Screens):** [https://timecure.vercel.app](https://timecure.vercel.app)  
**Latest Android APK:** [Download Timecure.apk](https://github.com/IMMENSE-BUILDER/Timecure/releases/latest/download/Timecure.apk)

**Timecure** is a personal Android alarm, location/time task, and deep-focus app built with Kotlin, Jetpack Compose, and Material Design 3. It forces maximum alarm volume over silent/mute/DND, prevents power-off attempts until wake verification completes, challenges your brain and body with 6 customizable wake-up missions, triggers GPS location & scheduled tasks, chains straight into a 60-tick Pomodoro chronometer, and connects zero-config to a server-side Vision & tool-calling AI assistant.

---

## Install on Android

Download `Timecure.apk` from [GitHub Releases](https://github.com/IMMENSE-BUILDER/Timecure/releases/latest) (zero embedded API keys — works out of the box with server-side AI), or install over USB via ADB:

```bash
adb install -r Timecure.apk
```

### Build from Source

1. Configure your Android SDK in `local.properties` (git-ignored):
   ```properties
   sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk
   ```
2. Run unit tests & build the APK (`./gradlew` connects AI calls directly to the server-side proxy, zero client keys required):
   ```bash
   ./gradlew testDebugUnitTest assembleDebug
   ```

---

## Features

### 1. Unstoppable Alarm Engine & Wake-Up Safeguards
- **Doze-immune exact scheduling:** `AlarmManager.setAlarmClock()` with reboot, timezone, and package-update recovery via `BootReceiver`.
- **Overrides silent, mute & DND + Extra Loud (+9dB):** `SoundManager` forces `STREAM_ALARM` to maximum volume, optionally applies hardware `LoudnessEnhancer` (+9 dB gain), ramps volume with Gentle Wake-up, and speaks live Time, Open-Meteo Weather, and Label voice reminders.
- **Lock-screen takeover & anti-escape:** `RingingActivity` wakes over the keyguard, intercepts hardware volume keys, blocks Back presses, and triggers escalating re-rings (+2 min → +5 min recurring) until solved.
- **Prevent Power-Off Safeguard:** Two-step `Display over apps` + `PowerOffGuardService` (`AccessibilityService`) blocks system power-off dialogs until the alarm is verified awake.
- **Customizable Wake-Up Check (`3, 2, 1` popup):** Post-dismissal check (1m, 2m, 3m, 5m or custom delay + configurable countdown) confirms you are awake before silencing all alarms sharing that goal.

### 2. Six Customizable Wake-Up Missions (1–5 Chained per Alarm)
- **Popular:** *Household Item Hunt (AI Vision camera verification)* & *Say the Word on Beat* (8-word rhythmic speech recognizer).
- **Wake Your Brain:** *Find Color Tiles* (memory matrix), *Typing* (motivational phrase accuracy check), & *Math* (customizable arithmetic).
- **Wake Your Body:** *Step* (real pedometer & accelerometer step verification in steps or meters).

### 3. Time & Location GPS Tasks (Center Navigation Tab)
- Mark recurring real-world places (`College`, `Restaurant`, `Home`, `Work`, `Gym`, `Shop`) using current GPS + Geocoder or custom coordinates with configurable **100m–500m** radius and `+30m` anti-bounce hysteresis.
- Background `LocationTaskService` and exact `TaskScheduler` trigger notifications or full-screen `TaskAlarmActivity` alerts on arrival/departure or scheduled time.

### 4. Server-Side Multimodal AI (Zero Client Keys or Config)
- Powered strictly server-side through Vercel (`/api/ai`) with zero API keys, gateway settings, or model dropdowns inside the app.
- **Camera & Gallery Vision:** Upload timetables, study sheets, or routines to build an interactive **AI Task Table** persisted in SQLite and launch alarms or Pomodoro sprints in one tap.
- **7 Native Function-Calling Tools:** `create_alarm`, `save_task_table`, `toggle_alarm`, `delete_alarm`, `start_pomodoro`, `update_preferences`, and `ultimate_morning_unlock`.

### 5. Integrated Pomodoro Focus Chronometer & MP3 Waveform Trimmer
- Precision **60-tick Canvas chronometer dial**, rolling odometer digits, foreground `PomodoroService`, configurable Sprint/Classic/Deep Work/Ultradian presets, SQLite focus telemetry, and local MP3 frame-accurate trimming (`Mp3Trimmer`).

### 6. Material Design 3 Themes & 5-Tab Zero-Latency Shell
- **6 Light & Dark Material 3 Palettes** (`Ocean Dawn`, `Emerald`, `Sunset Coral`, `Amethyst`, `Graphite`, `Crimson`) plus Android 12+ Dynamic Wallpaper color across all **5 pre-warmed tabs** (`Alarms`, `Focus`, `Tasks`, `Insights`, `Settings`).

---

## Repository Structure

- `app/` — Native Android application (Kotlin, Jetpack Compose, Material 3, SQLite v9, OkHttp)
- `website/` — Interactive touch-simulated product showcase & serverless AI proxy deployed on Vercel ([timecure.vercel.app](https://timecure.vercel.app))
