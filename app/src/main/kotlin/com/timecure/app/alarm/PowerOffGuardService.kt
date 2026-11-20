package com.timecure.app.alarm

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.timecure.app.ui.screens.ringing.RingingActivity
import com.timecure.app.ui.screens.ringing.WakeUpCheckActivity

/**
 * Safeguards an active alarm with Prevent Power-Off enabled:
 * whenever [PowerOffGuardState.isLocked] is true and Android/OEM's global power-off dialog
 * opens, immediately closes the power menu and returns the user to their alarm or wake-up check.
 */
class PowerOffGuardService : AccessibilityService() {

    private var lastToastMillis = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (!PowerOffGuardState.isLocked(this)) return

        val pkg = event.packageName?.toString().orEmpty()
        if (pkg == packageName) return

        val texts = mutableListOf<String>()
        event.text.forEach { ch -> if (!ch.isNullOrBlank()) texts += ch.toString() }
        event.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let { texts += it }

        val root = runCatching { rootInActiveWindow }.getOrNull()
        if (root != null) {
            collectNodeTexts(root, texts, maxNodes = 24)
        }

        if (isPowerMenuEvent(event.className?.toString(), pkg, texts)) {
            performGlobalAction(GLOBAL_ACTION_BACK)
            val alarmId = PowerOffGuardState.lockedAlarmId(this)
            val reopen = if (PowerOffGuardState.isWaitingWakeCheck(this)) {
                Intent(this, WakeUpCheckActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                }
            } else {
                AlarmActions.ringingIntent(this, alarmId, rerings = 0).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                }
            }
            runCatching { startActivity(reopen) }

            val now = System.currentTimeMillis()
            if (now - lastToastMillis >= 2_500L) {
                lastToastMillis = now
                Toast.makeText(
                    this,
                    "Confirm you're awake in Timecure before powering off.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onInterrupt() {}

    private fun collectNodeTexts(node: AccessibilityNodeInfo, out: MutableList<String>, maxNodes: Int) {
        if (out.size >= maxNodes) return
        node.text?.toString()?.takeIf { it.isNotBlank() }?.let { out += it }
        node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let { out += it }
        for (i in 0 until node.childCount) {
            if (out.size >= maxNodes) break
            val child = node.getChild(i) ?: continue
            collectNodeTexts(child, out, maxNodes)
        }
    }

    companion object {
        private val CLASS_HINTS = listOf(
            "globalactions",
            "poweroff",
            "shutdown",
            "rebootdialog",
            "globalactionpanel"
        )

        private val TEXT_HINTS = listOf(
            "power off",
            "shut down",
            "shutdown",
            "restart",
            "reboot",
            "slide to power",
            "hold to power",
            "switch off"
        )

        fun isPowerMenuEvent(
            className: String?,
            packageName: String?,
            texts: List<String>
        ): Boolean {
            val cls = className.orEmpty().lowercase()
            val pkg = packageName.orEmpty().lowercase()
            if (cls.isNotBlank() && CLASS_HINTS.any { cls.contains(it) }) return true
            val isSystemUiOrAndroid = pkg.isBlank() ||
                pkg == "android" ||
                pkg.contains("systemui") ||
                pkg.contains("power") ||
                pkg.contains("vivo")
            if (!isSystemUiOrAndroid && texts.isEmpty()) return false
            val joined = texts.joinToString(" ").lowercase()
            return TEXT_HINTS.any { joined.contains(it) }
        }
    }
}
