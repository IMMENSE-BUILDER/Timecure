# ⏰ Timecure — Material Design 3 AI Alarm & Wake Engine

## Architectural Specification & Implementation Plan

> **Design Bible:** Adhering strictly to [`skydashnet/material-design-3-ui-skill`](https://github.com/skydashnet/material-design-3-ui-skill)  
> **Target Directory:** `C:\Users\Administrator\Desktop\Timecure`  
> **Platform:** Android (Min SDK 26, Target SDK 35, Jetpack Compose Material 3)

---

## 1. Deep Analysis of the M3 UI Skill & Direct Application to Timecure

The `material-design-3-ui-skill` defines that Material Design 3 is a **semantic, adaptive, accessible system**, not a superficial styling exercise of "rounded cards, pastel colors, and purple buttons."

### Key Directives Applied to Timecure

| Skill Rule / Anti-Pattern | Violation to Avoid ("Fake Material") | Timecure Implementation |
|---|---|---|
| **Semantic Token Discipline** | Hardcoding hex colors (`#FFFFFF`, `#1A1A1A`) | Strict adherence to `MaterialTheme.colorScheme` tokens: `surface`, `surfaceContainer`, `surfaceContainerHigh`, `primary`, `onPrimary`, `primaryContainer`, `errorContainer` |
| **Component Purpose** | Wrapping every list item in an elevated card | Alarms displayed using clean M3 **List Items** (`ListItem`) separated by subtle `HorizontalDivider` (`outlineVariant`). Elevated surfaces reserved exclusively for the active ringing challenge card |
| **Action Hierarchy** | Multiple competing FABs or filled buttons | Single dominant **ExtendedFloatingActionButton** for "Add Alarm". Secondary actions use `FilledTonalButton`, low-emphasis actions use `OutlinedButton` or `TextButton` |
| **Immediate Toggles** | Toggle switch paired with redundant "Save" button | Alarm enable/disable `Switch` takes effect immediately, persisting to Room and rescheduling via `AlarmManager` with zero redundant confirm clicks |
| **Accessibility (WCAG AA)** | Sub-48dp icon targets, low contrast text | 48×48dp minimum touch targets on all interactive elements; high contrast text pairing (`onSurface` on `surface`, `onPrimary` on `primary`); full dynamic font scaling support (`sp`) |
| **Edge-to-Edge & Insets** | Fixed padded layouts ignoring system bars | True edge-to-edge with `WindowInsets.systemBars`, `WindowInsets.ime` (keyboard padding for answer entry), and cutout handling |
| **Expressive Restraint** | Decorative bounce animations on every click | Expressive motion reserved strictly for the **Ringing & Awakening state** (pulsing alarm ring, state transitions); routine alarm list and settings remain calm and legible |

---

## 2. Core Functional Pillars

### 2.1 The Unstoppable Alarm Engine
```
AlarmManager.setAlarmClock() (Doze immune)
               ↓
          AlarmReceiver
               ↓
     AlarmForegroundService (SPECIAL_USE)
    ┌──────────┴──────────┐
    ▼                     ▼
SoundManager        FullScreenIntent
(STREAM_ALARM max,  (Lock screen takeover,
 DND bypass channel, FLAG_TURN_SCREEN_ON,
 volume override)    FLAG_SHOW_WHEN_LOCKED)
```
* **Bypass Silent/Mute:** Audio stream forced via `AudioManager.setStreamVolume(STREAM_ALARM, maxVol, 0)` with `AudioAttributes.USAGE_ALARM`.
* **Bypass Do Not Disturb (DND):** High-importance `NotificationChannel` with `setBypassDnd(true)` and audio attributes routed through alarm usage.
* **Volume Key Interception:** Lock-screen ringing activity intercepts hardware volume up/down events, preventing the user from lowering volume or muting without answering.
* **Reboot Resilience:** `BootReceiver` listens for `ACTION_BOOT_COMPLETED` and `ACTION_LOCKED_BOOT_COMPLETED` to reschedule all active alarms in Room database.

### 2.2 Cognitive Awakening Engine (Defeating Sleep Inertia)
Sleep inertia is caused by high adenosine levels and suppressed prefrontal cortex activity upon waking. Timecure targets the prefrontal cortex via active multi-step cognitive challenges:

1. **Arithmetic Chains:** E.g., `(14 × 3) - (28 ÷ 4)` — triggers working memory and executive calculation.
2. **Sequential Logic & Patterns:** E.g., `Sequence: 3, 7, 15, 31, __?` — activates pattern recognition.
3. **Word Unscrambling:** E.g., `Unscramble "L P C O M X E" (Hint: Opposite of simple)` — triggers verbal retrieval.
4. **Spatial Reasoning:** Visual rotation and relationship problems.

### 2.3 Escalating Re-Ring Loop
* **Ring 1:** Initial alarm trigger + Level 1 cognitive challenge.
* **If dismissed/unanswered within 90 seconds:** Silence temporary, re-schedule for **+2 minutes**.
* **Ring 2:** Escalated volume, Level 2 cognitive challenge (increased complexity).
* **If still unanswered:** Re-schedule for **+5 minutes**.
* **Ring 3+:** Level 3 challenge + maximum vibration, repeating every **5 minutes indefinitely** until the correct answer is validated.

### 2.4 AI Integration (Vercel AI Gateway)
* Direct HTTPS client calling Vercel AI Gateway endpoint.
* Prompt structured to return structured JSON: `{ "question": "...", "answer": "...", "hint": "..." }`.
* **Fuzzy Answer Normalization:** Trims whitespace, ignores punctuation and casing, and computes Levenshtein distance for close typos on text answers.
* **Local Offline Fallback:** Pre-seeded Room database with 150+ categorized cognitive questions (math, logic, verbal), ensuring the alarm challenges the user even in airplane mode or with no WiFi.

---

## 3. Material 3 Design Specifications (Timecure)

### 3.1 Color Roles (Semantic Palette)

```
Screen background:        MaterialTheme.colorScheme.surface
App bar background:       MaterialTheme.colorScheme.surface
Alarm card/container:     MaterialTheme.colorScheme.surfaceContainer
Active alarm text:        MaterialTheme.colorScheme.onSurface
Inactive/sub text:        MaterialTheme.colorScheme.onSurfaceVariant
Primary CTA (Add Alarm):  MaterialTheme.colorScheme.primaryContainer + onPrimaryContainer
Switch (Checked):         MaterialTheme.colorScheme.primary
Ringing Challenge Banner: MaterialTheme.colorScheme.primaryContainer + onPrimaryContainer
Destructive / Dismiss:    MaterialTheme.colorScheme.errorContainer + onErrorContainer
Dividers:                 MaterialTheme.colorScheme.outlineVariant
```

* **Dynamic Theming:** Enabled on Android 12+ (`dynamicLightColorScheme` / `dynamicDarkColorScheme`).
* **Custom Baseline:** Calming Deep Indigo (`#2B3A67`) primary with Sky Blue (`#496F5D` / `#4B88A2`) tertiary for wakefulness and clarity.

### 3.2 Typography Scale
* **Alarm Time Display:** `MaterialTheme.typography.displayLarge` (72sp, SemiBold)
* **Screen Titles:** `MaterialTheme.typography.headlineMedium` (28sp)
* **Alarm Labels & Section Titles:** `MaterialTheme.typography.titleMedium` (16sp, Medium)
* **Question Body:** `MaterialTheme.typography.bodyLarge` (16sp, Regular)
* **Metadata & Badges:** `MaterialTheme.typography.labelMedium` (12sp, Medium)

### 3.3 Component Architecture

```
┌──────────────────────────────────────────────┐
│  Timecure                    [⚙ Settings]   │ ← CenterAlignedTopAppBar
├──────────────────────────────────────────────┤
│  Next alarm in 6 hours 23 minutes            │ ← Headline banner (surfaceContainerLow)
│                                              │
│  06:30 AM                       [ Switch:ON ]│ ← M3 ListItem
│  Mon, Tue, Wed, Thu, Fri                     │   supportingContent: onSurfaceVariant
│  ──────────────────────────────────────────  │ ← HorizontalDivider (outlineVariant)
│                                              │
│  08:00 AM                      [ Switch:OFF ]│ ← M3 ListItem
│  Saturday, Sunday                            │
│  ──────────────────────────────────────────  │
│                                              │
│                                              │
│                            ┌──────────────┐  │
│                            │ ＋ Add Alarm │  │ ← ExtendedFloatingActionButton
│                            └──────────────┘  │
└──────────────────────────────────────────────┘

═══ FULL-SCREEN RINGING CHALLENGE (M3 Over Lock Screen) ═══

┌──────────────────────────────────────────────┐
│                                              │
│                  06:30 AM                    │ ← DisplayLarge (prominent)
│              Time to Wake Up                 │ ← TitleLarge
│                                              │
│  ┌────────────────────────────────────────┐  │
│  │ 🧠 COGNITIVE CHALLENGE — LEVEL 1       │  │ ← ElevatedCard (surfaceContainerHigh)
│  │                                        │  │
│  │ What is (17 × 3) - 12?                 │  │ ← BodyLarge
│  │                                        │  │
│  │ ┌────────────────────────────────────┐ │  │
│  │ │ Enter answer                       │ │  │ ← OutlinedTextField
│  │ └────────────────────────────────────┘ │  │
│  │                                        │  │
│  │ ┌────────────────────────────────────┐ │  │
│  │ │          SUBMIT ANSWER             │ │  │ ← FilledButton (primary + onPrimary)
│  │ └────────────────────────────────────┘ │  │
│  └────────────────────────────────────────┘  │
│                                              │
│  ⚠ Alarm will escalate in 90 seconds if      │ ← LabelSmall (onSurfaceVariant)
│    unanswered.                               │
└──────────────────────────────────────────────┘
```

---

## 4. Desktop Project Structure (`Desktop\Timecure`)

```
C:\Users\Administrator\Desktop\Timecure/
├── material-design-3-ui-skill/        # Analyzed design reference
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml        # Exact alarms, boot, wake lock, full screen intent
│   │   ├── java/com/timecure/app/
│   │   │   ├── TimecureApp.kt
│   │   │   ├── di/
│   │   │   │   ├── AppModule.kt
│   │   │   │   ├── DatabaseModule.kt
│   │   │   │   └── NetworkModule.kt
│   │   │   ├── data/
│   │   │   │   ├── local/
│   │   │   │   │   ├── TimecureDatabase.kt
│   │   │   │   │   ├── AlarmDao.kt
│   │   │   │   │   ├── AlarmEntity.kt
│   │   │   │   │   ├── QuestionEntity.kt
│   │   │   │   │   └── PreferencesDataStore.kt
│   │   │   │   ├── remote/
│   │   │   │   │   ├── VercelAiApi.kt
│   │   │   │   │   └── AiChallengeDto.kt
│   │   │   │   └── repository/
│   │   │   │       ├── AlarmRepositoryImpl.kt
│   │   │   │       └── ChallengeRepositoryImpl.kt
│   │   │   ├── domain/
│   │   │   │   ├── model/
│   │   │   │   │   ├── Alarm.kt
│   │   │   │   │   ├── CognitiveChallenge.kt
│   │   │   │   │   └── ChallengeDifficulty.kt
│   │   │   │   └── repository/
│   │   │   │       ├── AlarmRepository.kt
│   │   │   │       └── ChallengeRepository.kt
│   │   │   ├── alarm/
│   │   │   │   ├── AlarmScheduler.kt
│   │   │   │   ├── AlarmReceiver.kt
│   │   │   │   ├── BootReceiver.kt
│   │   │   │   ├── AlarmService.kt
│   │   │   │   └── SoundManager.kt
│   │   │   └── ui/
│   │   │       ├── theme/
│   │   │       │   ├── Color.kt
│   │   │       │   ├── Type.kt
│   │   │       │   ├── Shape.kt
│   │   │       │   └── Theme.kt
│   │   │       ├── components/
│   │   │       │   ├── TimecureTopAppBar.kt
│   │   │       │   ├── AlarmListItem.kt
│   │   │       │   └── ChallengeCard.kt
│   │   │       ├── screens/
│   │   │       │   ├── alarms/
│   │   │       │   │   ├── AlarmsScreen.kt
│   │   │       │   │   └── AlarmsViewModel.kt
│   │   │       │   ├── addalarm/
│   │   │       │   │   ├── AddEditAlarmScreen.kt
│   │   │       │   │   └── AddEditAlarmViewModel.kt
│   │   │       │   ├── ringing/
│   │   │       │   │   ├── RingingChallengeActivity.kt
│   │   │       │   │   ├── RingingChallengeScreen.kt
│   │   │       │   │   └── RingingViewModel.kt
│   │   │       │   └── settings/
│   │   │       │       ├── SettingsScreen.kt
│   │   │       │       └── SettingsViewModel.kt
│   │   │       └── navigation/
│   │   │           └── TimecureNavGraph.kt
│   │   └── res/
│   │       ├── values/
│   │       │   ├── strings.xml
│   │       │   └── themes.xml
│   │       └── raw/                   # Built-in aggressive alarm tones
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

---

## 5. M3 UI Skill Self-Audit Checklist

| Category | Check | Score | Implementation Notes |
|---|---|:---:|---|
| **Task clarity** | Primary user goal and action obvious | **2/2** | Alarms prominently displayed; single Extended FAB to add |
| **Information hierarchy** | Grouping and emphasis are coherent | **2/2** | Lists with subtle dividers; elevated cards used only for ringing challenge |
| **Component semantics** | Controls match actual behavior | **2/2** | Switch operates immediately; M3 TimePicker for time; OutlinedTextField for inputs |
| **Token discipline** | Semantic roles used consistently | **2/2** | All UI bound to `MaterialTheme.colorScheme` tokens; no raw hex scattered |
| **Adaptive behavior** | Responsive window sizing | **2/2** | Edge-to-edge support; responsive layout for compact and medium screens |
| **States & feedback** | Loading, error, empty states covered | **2/2** | Empty alarm screen state, error messages with recovery, input validation |
| **Accessibility** | 48dp touch targets, AA contrast, dynamic type | **2/2** | All buttons ≥48dp; high-contrast on* pairings; sp typography scale |
| **Expressive restraint** | Selective expression | **2/2** | Calm routine list; energetic wake-up sequence on ringing screen |

---

## 6. Execution Roadmap

1. **Phase 1: Project Scaffolding & Build Configuration**
   - Setup `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, and gradle wrapper.
   - Configure dependencies: AndroidX Core, Jetpack Compose Material 3 (1.3+), Room, Ktor/OkHttp, Hilt/Kotlin Serialization.
2. **Phase 2: Data & Alarm Core**
   - Room DB (`AlarmEntity`, `QuestionEntity`), `AlarmDao`, `TimecureDatabase`.
   - `SoundManager` (audio focus, volume override, DND bypass).
   - `AlarmScheduler`, `AlarmReceiver`, `AlarmService`, and `BootReceiver`.
3. **Phase 3: Material 3 Theme & Components**
   - Color tokens (`Color.kt`, `Theme.kt`), Typography (`Type.kt`), Shapes (`Shape.kt`).
   - M3 components: `TimecureTopAppBar`, `AlarmListItem`, `TimePicker` dialog.
4. **Phase 4: Screen Implementation**
   - `AlarmsScreen` (list of alarms, switch toggles, swipe to delete, FAB).
   - `AddEditAlarmScreen` (M3 TimePicker, repeat day chips, label, difficulty selection).
   - `RingingChallengeActivity` (lock screen override, AI challenge presentation, escalating re-ring timer).
   - `SettingsScreen` (Vercel AI Gateway URL, API Key, Model name, tone selection).
5. **Phase 5: Vercel AI Gateway Integration & Local Fallback**
   - HTTP client calling Vercel Gateway.
   - Local question bank pre-population.
   - Answer normalization & validation.
