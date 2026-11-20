package com.timecure.app.util

object TimeFormat {

    /** "in 7h 12m", "in 12m", "in 1d 3h", "in under a minute". */
    fun until(targetMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val totalMinutes = ((targetMillis - nowMillis).coerceAtLeast(0) / 60_000L)
        val days = totalMinutes / (24 * 60)
        val hours = (totalMinutes % (24 * 60)) / 60
        val minutes = totalMinutes % 60
        return when {
            days > 0 -> "in ${days}d ${hours}h"
            hours > 0 -> "in ${hours}h ${minutes}m"
            minutes > 0 -> "in ${minutes}m"
            else -> "in under a minute"
        }
    }
}
