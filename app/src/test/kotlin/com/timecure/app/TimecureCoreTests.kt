package com.timecure.app

import com.timecure.app.alarm.AlarmScheduler
import com.timecure.app.alarm.AlarmVoiceAssistant
import com.timecure.app.alarm.MissionAiHelper
import com.timecure.app.alarm.PowerOffGuardService
import com.timecure.app.audio.Mp3Trimmer
import com.timecure.app.data.local.ChallengeGenerator
import com.timecure.app.data.remote.TimecureAiAgent
import com.timecure.app.data.remote.VercelAiClient
import com.timecure.app.model.Alarm
import com.timecure.app.model.ChallengeCategory
import com.timecure.app.model.ChallengeDifficulty
import com.timecure.app.model.CognitiveChallenge
import com.timecure.app.model.GeofenceTriggerMode
import com.timecure.app.model.PomodoroPhase
import com.timecure.app.model.PomodoroPreset
import com.timecure.app.model.PomodoroState
import com.timecure.app.model.SmartTask
import com.timecure.app.model.StepUnit
import com.timecure.app.model.TaskTriggerType
import com.timecure.app.model.WakeEvent
import com.timecure.app.model.WakeMissionCatalog
import com.timecure.app.model.WakeMissionConfig
import com.timecure.app.model.WakeMissionType
import com.timecure.app.pomodoro.PomodoroEngine
import com.timecure.app.tasks.LocationGeoMath
import com.timecure.app.util.StatsCalculator
import com.timecure.app.util.TimeFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import kotlin.random.Random

class TimecureCoreTests {

    @Test
    fun alarmOneShotSchedulesTodayWhenInFutureAndTomorrowWhenPassed() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 20, 6, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val futureAlarm = Alarm(hour = 7, minute = 0)
        val futureCal = Calendar.getInstance().apply { timeInMillis = futureAlarm.computeNextTriggerMillis(now) }
        assertEquals(20, futureCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(7, futureCal.get(Calendar.HOUR_OF_DAY))

        val pastAlarm = Alarm(hour = 6, minute = 0)
        val pastCal = Calendar.getInstance().apply { timeInMillis = pastAlarm.computeNextTriggerMillis(now) }
        assertEquals(21, pastCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(6, pastCal.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun alarmRepeatingSkipsToNextSelectedWeekday() {
        // Friday 2026-11-20 at 08:00, alarm is set for Monday 07:00
        val friday = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 20, 8, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val mondayAlarm = Alarm(hour = 7, minute = 0, daysOfWeek = setOf(Calendar.MONDAY))
        val target = Calendar.getInstance().apply { timeInMillis = mondayAlarm.computeNextTriggerMillis(friday) }
        assertEquals(Calendar.MONDAY, target.get(Calendar.DAY_OF_WEEK))
        assertEquals(23, target.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun challengeGeneratorProducesValidSolvableChallengesAcrossAllCategories() {
        val rng = Random(42)
        for (category in ChallengeCategory.entries) {
            for (difficulty in ChallengeDifficulty.entries) {
                repeat(10) {
                    val c = ChallengeGenerator.generate(category, difficulty, rng)
                    assertTrue("Question blank for $category/$difficulty", c.question.isNotBlank())
                    assertTrue("Answer blank for $category/$difficulty", c.answer.isNotBlank())
                    assertTrue("Own answer rejected for ${c.id}: ${c.answer}", c.isAnswerCorrect(c.answer))
                    if (c.category == ChallengeCategory.STROOP) {
                        val s = c.stroop
                        assertNotNull(s)
                        assertNotEquals(s!!.word, s.inkColorId)
                        assertTrue(s.inkColorId in s.options)
                        assertEquals(s.inkColorId, c.answer)
                    }
                }
            }
        }
    }

    @Test
    fun cognitiveChallengeAnswerMatchingHandlesNumbersTimeAndTypos() {
        val decimal = CognitiveChallenge("1", "q", "7.5")
        assertTrue(decimal.isAnswerCorrect("7.50"))
        assertTrue(decimal.isAnswerCorrect("7,5"))
        assertFalse(decimal.isAnswerCorrect("7.6"))

        val negative = CognitiveChallenge("2", "q", "-5")
        assertTrue(negative.isAnswerCorrect("-5"))
        assertFalse(negative.isAnswerCorrect("5"))

        val timeQ = CognitiveChallenge("3", "q", "8:05", category = ChallengeCategory.TIME_MATH)
        assertTrue(timeQ.isAnswerCorrect("08:05"))
        assertTrue(timeQ.isAnswerCorrect("8:05"))
        assertFalse(timeQ.isAnswerCorrect("8:50"))

        val digits = CognitiveChallenge("4", "q", "04921", category = ChallengeCategory.MEMORY)
        assertTrue(digits.isAnswerCorrect("04921"))
        assertFalse(digits.isAnswerCorrect("04922"))

        val word = CognitiveChallenge("5", "q", "MORNING", category = ChallengeCategory.WORDS)
        assertTrue(word.isAnswerCorrect("morning"))
        assertTrue(word.isAnswerCorrect("mornin")) // 1 typo allowed on >=5 letters
        assertFalse(word.isAnswerCorrect("evening"))
        assertFalse(word.isAnswerCorrect("   "))
    }

    @Test
    fun statsCalculatorComputesConsecutiveStreakAndSevenDayWindow() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 20, 9, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        fun daysAgo(days: Int): Long = (now.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -days)
        }.timeInMillis

        val events = listOf(
            WakeEvent(daysAgo(0), solveSeconds = 18),
            WakeEvent(daysAgo(1), solveSeconds = 24),
            WakeEvent(daysAgo(2), solveSeconds = 30),
            // gap at day 3
            WakeEvent(daysAgo(4), solveSeconds = 12)
        )
        val stats = StatsCalculator.compute(events, now)
        assertEquals(3, stats.streakDays)
        assertEquals(4, stats.totalWakes)
        assertEquals(12, stats.fastestSolveSeconds)
        assertEquals(21, stats.averageSolveSeconds)
        assertEquals(7, stats.last7Days.size)
        assertEquals(1, stats.last7Days.last().solvedCount)
    }

    @Test
    fun vercelAiClientExtractsJsonFromThinkBlocksAndMarkdownFences() {
        val raw = """
            <think>
            Let's make a short wake-up question tied to deep work.
            </think>
            ```json
            {"question": "You code 3 sprints of 25 min. Total minutes?", "answer": "75", "hint": "3 x 25"}
            ```
        """.trimIndent()
        val parsed = VercelAiClient.parseChallenge(raw, ChallengeDifficulty.MEDIUM)
        assertEquals("75", parsed.answer)
        assertTrue(parsed.isAnswerCorrect("75"))
    }

    @Test
    fun reringEscalationDelaysAndTimeFormatting() {
        assertEquals(2, AlarmScheduler.reringDelayMinutes(0))
        assertEquals(5, AlarmScheduler.reringDelayMinutes(1))
        assertEquals(5, AlarmScheduler.reringDelayMinutes(4))

        val now = 1_000_000L
        assertEquals("in under a minute", TimeFormat.until(now + 20_000L, now))
        assertEquals("in 15m", TimeFormat.until(now + 15 * 60_000L, now))
        assertEquals("in 2h 30m", TimeFormat.until(now + 150 * 60_000L, now))
    }

    @Test
    fun pomodoroCycleTransitionsAndFormattingWorkCorrectly() {
        val preset = PomodoroPreset.CLASSIC // 4 cycles before long break
        val (p1, c1) = PomodoroEngine.nextPhaseAfter(PomodoroPhase.FOCUS, 0, preset, countFocus = true)
        assertEquals(PomodoroPhase.SHORT_BREAK, p1)
        assertEquals(1, c1)

        val (p2, c2) = PomodoroEngine.nextPhaseAfter(PomodoroPhase.SHORT_BREAK, c1, preset, countFocus = true)
        assertEquals(PomodoroPhase.FOCUS, p2)
        assertEquals(1, c2)

        // 4th completed focus block triggers LONG_BREAK
        val (p4, c4) = PomodoroEngine.nextPhaseAfter(PomodoroPhase.FOCUS, 3, preset, countFocus = true)
        assertEquals(PomodoroPhase.LONG_BREAK, p4)
        assertEquals(4, c4)

        val state = PomodoroState(remainingSeconds = 754, totalSeconds = 1500)
        assertEquals("12:34", state.formattedTime)
        assertTrue(state.progress in 0.49f..0.51f)
    }

    @Test
    fun aiAgentToolSchemasAndLocalFallbackParseCollegeAlarmAndPreferences() {
        val schemas = TimecureAiAgent.buildToolSchemas()
        assertEquals(7, schemas.length())
        assertEquals("Done!", TimecureAiAgent.stripThinkTags("<think>internal</think>Done!"))

        val actions = TimecureAiAgent.parseLocalIntent(
            "Call me Satyam, switch to Emerald theme and dark mode, and set a 7:15 AM alarm to get up for college",
            "Win the morning",
            hadImage = true
        )
        val byName = actions.toMap()
        assertTrue("save_task_table" in byName)
        assertTrue(byName["save_task_table"]!!.getJSONArray("tasks").length() >= 3)

        assertTrue("update_preferences" in byName)
        assertEquals("Satyam", byName["update_preferences"]!!.getString("user_name"))
        assertEquals("DARK", byName["update_preferences"]!!.getString("theme_mode"))
        assertEquals("EMERALD_FOREST", byName["update_preferences"]!!.getString("theme_palette"))

        assertTrue("create_alarm" in byName)
        val alarmArgs = byName["create_alarm"]!!
        assertEquals(7, alarmArgs.getInt("hour"))
        assertEquals(15, alarmArgs.getInt("minute"))
        assertEquals("Get up for college", alarmArgs.getString("goal"))
        assertEquals(5, alarmArgs.getJSONArray("days_of_week").length())
    }

    @Test
    fun mp3TrimmerSlicesValidMpegFramesInTimeRange() {
        // Construct 100 synthetic MPEG-1 Layer III 128kbps 44.1kHz frames (each 417 bytes, ~26.122ms -> ~2.61s total)
        val frameSize = 417
        val frameCount = 100
        val raw = ByteArray(frameSize * frameCount)
        for (i in 0 until frameCount) {
            val o = i * frameSize
            raw[o] = 0xFF.toByte()       // sync
            raw[o + 1] = 0xFB.toByte()   // MPEG-1, Layer III, no CRC
            raw[o + 2] = 0x90.toByte()   // 128 kbps (idx 9), 44100 Hz (idx 0), padding 0
            raw[o + 3] = 0x00.toByte()
        }
        val trimmed = Mp3Trimmer.trimMp3Bytes(raw, startMs = 500L, endMs = 1500L)
        assertNotNull(trimmed)
        // ~1000ms / 26.122ms ≈ 38 frames => ~15,846 bytes (strictly smaller than full 41,700 bytes)
        assertTrue(trimmed!!.size in 14_000..17_500)
        assertEquals(0, trimmed.size % frameSize)
    }

    @Test
    fun locationGeoMathComputesHaversineDistanceAndGeofenceTransitionsWithHysteresis() {
        val placeLat = 28.6139
        val placeLon = 77.2090

        // 0.001 degree latitude is ~111.2m
        val dist111 = LocationGeoMath.distanceMeters(placeLat + 0.001, placeLon, placeLat, placeLon)
        assertTrue("Expected ~111m, got $dist111", dist111 in 105.0..118.0)

        val now = 10_000_000L
        // Enter 150m radius from outside -> triggers ARRIVE and flips inside state to true
        val enterRes = LocationGeoMath.evaluateGeofence(
            userLat = placeLat + 0.001,
            userLon = placeLon,
            placeLat = placeLat,
            placeLon = placeLon,
            radiusMeters = 150,
            mode = GeofenceTriggerMode.ARRIVE,
            wasInside = false,
            lastTriggeredMillis = 0L,
            nowMillis = now
        )
        assertTrue(enterRes.shouldTrigger)
        assertTrue(enterRes.nextInsideState)

        // Move slightly outside 150m (~167m, still within +30m hysteresis) -> stays inside
        val boundaryRes = LocationGeoMath.evaluateGeofence(
            userLat = placeLat + 0.0015,
            userLon = placeLon,
            placeLat = placeLat,
            placeLon = placeLon,
            radiusMeters = 150,
            mode = GeofenceTriggerMode.ARRIVE,
            wasInside = true,
            lastTriggeredMillis = now,
            nowMillis = now + 60_000L
        )
        assertFalse(boundaryRes.shouldTrigger)
        assertTrue(boundaryRes.nextInsideState)

        // Move well beyond 150m + 30m hysteresis (~222m) -> exits zone
        val exitRes = LocationGeoMath.evaluateGeofence(
            userLat = placeLat + 0.002,
            userLon = placeLon,
            placeLat = placeLat,
            placeLon = placeLon,
            radiusMeters = 150,
            mode = GeofenceTriggerMode.ARRIVE,
            wasInside = true,
            lastTriggeredMillis = now,
            nowMillis = now + 120_000L
        )
        assertFalse(exitRes.shouldTrigger)
        assertFalse(exitRes.nextInsideState)

        // Re-entering WITHIN 15m cooldown must NOT trigger again
        val cooldownBlocked = LocationGeoMath.evaluateGeofence(
            userLat = placeLat,
            userLon = placeLon,
            placeLat = placeLat,
            placeLon = placeLon,
            radiusMeters = 150,
            mode = GeofenceTriggerMode.ARRIVE,
            wasInside = false,
            lastTriggeredMillis = now,
            nowMillis = now + 5 * 60_000L
        )
        assertFalse(cooldownBlocked.shouldTrigger)
        assertTrue(cooldownBlocked.nextInsideState)

        // Re-entering AFTER cooldown triggers cleanly
        val reEnterOk = LocationGeoMath.evaluateGeofence(
            userLat = placeLat,
            userLon = placeLon,
            placeLat = placeLat,
            placeLon = placeLon,
            radiusMeters = 150,
            mode = GeofenceTriggerMode.ARRIVE,
            wasInside = false,
            lastTriggeredMillis = now,
            nowMillis = now + 16 * 60_000L
        )
        assertTrue(reEnterOk.shouldTrigger)

        // LEAVE mode triggers when stepping outside > radius + hysteresis
        val leaveRes = LocationGeoMath.evaluateGeofence(
            userLat = placeLat + 0.0025,
            userLon = placeLon,
            placeLat = placeLat,
            placeLon = placeLon,
            radiusMeters = 150,
            mode = GeofenceTriggerMode.LEAVE,
            wasInside = true,
            lastTriggeredMillis = 0L,
            nowMillis = now
        )
        assertTrue(leaveRes.shouldTrigger)
        assertFalse(leaveRes.nextInsideState)
    }

    @Test
    fun smartTaskTimeTriggerSchedulesNextOccurrenceCorrectly() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 20, 10, 15, 0) // Friday 10:15 AM
            set(Calendar.MILLISECOND, 0)
        }
        val futureToday = SmartTask(
            title = "Pay college fee",
            triggerType = TaskTriggerType.TIME,
            hour = 14,
            minute = 30
        )
        val calFuture = Calendar.getInstance().apply {
            timeInMillis = futureToday.computeNextTimeTriggerMillis(now)
        }
        assertEquals(20, calFuture.get(Calendar.DAY_OF_MONTH))
        assertEquals(14, calFuture.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, calFuture.get(Calendar.MINUTE))

        val pastToday = SmartTask(
            title = "Morning vitamin",
            triggerType = TaskTriggerType.TIME,
            hour = 8,
            minute = 0
        )
        val calTomorrow = Calendar.getInstance().apply {
            timeInMillis = pastToday.computeNextTimeTriggerMillis(now)
        }
        assertEquals(21, calTomorrow.get(Calendar.DAY_OF_MONTH))
        assertEquals(8, calTomorrow.get(Calendar.HOUR_OF_DAY))

        val repeatingMonday = SmartTask(
            title = "Lab submission",
            triggerType = TaskTriggerType.TIME,
            hour = 9,
            minute = 0,
            daysOfWeek = setOf(Calendar.MONDAY)
        )
        val calMonday = Calendar.getInstance().apply {
            timeInMillis = repeatingMonday.computeNextTimeTriggerMillis(now)
        }
        assertEquals(Calendar.MONDAY, calMonday.get(Calendar.DAY_OF_WEEK))
        assertEquals(23, calMonday.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun alarmGentleWakeAndSnoozeSummariesFormatCleanly() {
        assertEquals("Off", Alarm(hour = 7, minute = 0, gentleWakeSeconds = 0).gentleWakeSummary)
        assertEquals("15 seconds", Alarm(hour = 7, minute = 0, gentleWakeSeconds = 15).gentleWakeSummary)
        assertEquals("30 seconds", Alarm(hour = 7, minute = 0, gentleWakeSeconds = 30).gentleWakeSummary)
        assertEquals("1 minute", Alarm(hour = 7, minute = 0, gentleWakeSeconds = 60).gentleWakeSummary)
        assertEquals("2 minutes", Alarm(hour = 7, minute = 0, gentleWakeSeconds = 120).gentleWakeSummary)

        assertEquals(
            "Off",
            Alarm(hour = 7, minute = 0, snoozeEnabled = false).snoozeSummary
        )
        assertEquals(
            "5 min, 3 times",
            Alarm(hour = 7, minute = 0, snoozeEnabled = true, snoozeIntervalMinutes = 5, snoozeMaxCount = 3).snoozeSummary
        )
        assertEquals(
            "10 min, unlimited",
            Alarm(hour = 7, minute = 0, snoozeEnabled = true, snoozeIntervalMinutes = 10, snoozeMaxCount = 0).snoozeSummary
        )
    }

    @Test
    fun weatherCodeMapperDescribesStandardWmoCodes() {
        assertEquals("clear sky", AlarmVoiceAssistant.describeWmoWeatherCode(0))
        assertEquals("partly cloudy", AlarmVoiceAssistant.describeWmoWeatherCode(2))
        assertEquals("rainy", AlarmVoiceAssistant.describeWmoWeatherCode(61))
        assertEquals("snowy", AlarmVoiceAssistant.describeWmoWeatherCode(73))
        assertEquals("thunderstorms", AlarmVoiceAssistant.describeWmoWeatherCode(95))
    }

    @Test
    fun wakeMissionJsonRoundTripPreservesAllSixMissionTypes() {
        assertEquals(6, WakeMissionType.entries.size)
        val input = listOf(
            WakeMissionConfig(
                type = WakeMissionType.ITEM_HUNT,
                rounds = 2,
                selectedItems = listOf("Cup", "Laptop", "Keys")
            ),
            WakeMissionConfig(
                type = WakeMissionType.SAY_WORD_ON_BEAT,
                rounds = 1,
                selectedTopics = listOf("Morning Motivation", "Zero Snooze")
            ),
            WakeMissionConfig(
                type = WakeMissionType.FIND_COLOR_TILES,
                difficultyLevel = 2,
                rounds = 4
            ),
            WakeMissionConfig(
                type = WakeMissionType.TYPING,
                difficultyLevel = 3,
                rounds = 2,
                useAiPhrase = false
            ),
            WakeMissionConfig(
                type = WakeMissionType.MATH,
                difficultyLevel = 4,
                rounds = 3
            )
        )
        val encoded = WakeMissionConfig.encodeList(input)
        val decoded = WakeMissionConfig.decodeList(encoded)
        assertEquals(input, decoded)

        val stepConfig = WakeMissionConfig(
            type = WakeMissionType.STEP,
            stepTarget = 100,
            stepUnit = StepUnit.METERS
        )
        val decodedStep = WakeMissionConfig.decodeList(WakeMissionConfig.encodeList(listOf(stepConfig)))
        assertEquals(listOf(stepConfig), decodedStep)
        assertEquals("100 Meters", decodedStep.first().summaryText())
    }

    @Test
    fun alarmWakeCheckAndPreventPowerOffSummariesFormatCleanly() {
        val offAlarm = Alarm(hour = 7, minute = 10, wakeCheckEnabled = false, preventPowerOff = false)
        assertEquals("Off", offAlarm.wakeCheckSummary)
        assertEquals("Off", offAlarm.preventPowerOffSummary)

        val armedAlarm = Alarm(
            hour = 7,
            minute = 10,
            wakeCheckEnabled = true,
            wakeCheckDelayMinutes = 2,
            wakeCheckCountdownSeconds = 15,
            preventPowerOff = true
        )
        assertEquals("2 min • 15s", armedAlarm.wakeCheckSummary)
        assertEquals("On", armedAlarm.preventPowerOffSummary)
    }

    @Test
    fun powerOffGuardDetectsAndroidAndOemPowerDialogs() {
        assertTrue(
            PowerOffGuardService.isPowerMenuEvent(
                className = "com.android.server.policy.GlobalActionsDialog",
                packageName = "android",
                texts = emptyList()
            )
        )
        assertTrue(
            PowerOffGuardService.isPowerMenuEvent(
                className = "android.widget.FrameLayout",
                packageName = "com.android.systemui",
                texts = listOf("Emergency", "Power off", "Restart")
            )
        )
        assertFalse(
            PowerOffGuardService.isPowerMenuEvent(
                className = "androidx.compose.ui.platform.ComposeView",
                packageName = "com.whatsapp",
                texts = listOf("Messages", "Online")
            )
        )
    }

    @Test
    fun stepDistanceConversionAndBeatWordMatchingWorkAccurately() {
        assertEquals(20, WakeMissionCatalog.stepsRequiredFor(20, StepUnit.STEPS))
        assertEquals(135, WakeMissionCatalog.stepsRequiredFor(100, StepUnit.METERS))

        val words = listOf("wake", "up", "right", "now", "do", "not", "be", "late")
        val afterFirst = WakeMissionCatalog.matchSpokenWords(words, emptySet(), "wake up rigth now")
        assertEquals(setOf(0, 1, 2, 3), afterFirst)

        val afterSecond = WakeMissionCatalog.matchSpokenWords(words, afterFirst, "do not be late please")
        assertEquals((0..7).toSet(), afterSecond)
    }

    @Test
    fun serverSideAiParsingHandlesThinkBlocksProxyFormatAndItemVerification() {
        assertEquals("Final answer", TimecureAiAgent.stripThinkTags("<think>reasoning</think>Final answer"))
        assertEquals("Prefix", TimecureAiAgent.stripThinkTags("Prefix<think>unclosed reasoning..."))
        assertEquals("", TimecureAiAgent.stripThinkTags("<think>only reasoning"))

        val proxyReply = VercelAiClient.extractReplyContent(
            """{"content":"<think>hidden</think>Set your alarm for 6:30 AM.","tool_calls":[],"finish_reason":"stop"}"""
        )
        assertEquals("Set your alarm for 6:30 AM.", proxyReply)

        val choicesReply = VercelAiClient.extractReplyContent(
            """{"choices":[{"message":{"content":"Legacy reply"}}]}"""
        )
        assertEquals("Legacy reply", choicesReply)

        val verified = MissionAiHelper.parseItemVerifyJson(
            """<think>looks like a ceramic mug</think>{"found":true,"detected":"Coffee Mug","reason":"Ceramic mug clearly held in frame"}"""
        )
        assertTrue(verified.found)
        assertEquals("Coffee Mug", verified.detected)

        val rejected = MissionAiHelper.parseItemVerifyJson(
            """{"found":false,"detected":"Keyboard","reason":"No cup or mug visible"}"""
        )
        assertFalse(rejected.found)
        assertEquals("Keyboard", rejected.detected)
    }
}
