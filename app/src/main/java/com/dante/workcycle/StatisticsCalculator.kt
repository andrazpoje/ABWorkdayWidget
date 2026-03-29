package com.dante.workcycle

import android.content.Context
import java.time.LocalDate

data class StatisticsSummary(
    val counts: LinkedHashMap<String, Int>,
    val totalDays: Int
)

object StatisticsCalculator {

    fun buildCurrentMonthSummary(context: Context): StatisticsSummary {
        val today = LocalDate.now()
        val start = today.withDayOfMonth(1)
        val end = today.withDayOfMonth(today.lengthOfMonth())
        return buildSummary(context, start, end)
    }

    fun buildCurrentYearSummary(context: Context): StatisticsSummary {
        val today = LocalDate.now()
        val start = today.withDayOfYear(1)
        val end = today.withDayOfYear(today.lengthOfYear())
        return buildSummary(context, start, end)
    }

    private fun buildSummary(
        context: Context,
        startDate: LocalDate,
        endDate: LocalDate
    ): StatisticsSummary {

        val prefs = context.getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)

        val cycleLabels = CycleManager.loadCycle(context).ifEmpty { listOf("A", "B") }

        val orderedLabels = linkedSetOf<String>()
        orderedLabels.addAll(cycleLabels)

        val overrideSkipped = prefs.getBoolean(AppPrefs.KEY_OVERRIDE_SKIPPED, true)
        if (overrideSkipped) {
            val skippedLabel = prefs.getString(
                AppPrefs.KEY_SKIPPED_LABEL,
                AppPrefs.DEFAULT_SKIPPED_LABEL
            )?.trim().orEmpty().ifBlank { AppPrefs.DEFAULT_SKIPPED_LABEL }

            orderedLabels.add(skippedLabel)
        }

        val counts = LinkedHashMap<String, Int>()
        orderedLabels.forEach { label ->
            counts[label] = 0
        }

        var current = startDate
        var total = 0

        while (!current.isAfter(endDate)) {
            // Future-proof note:
            // Statistics intentionally uses generic cycle labels instead of hardcoded A/B logic,
            // so this can later support custom weekly cycles and more than two cycle entries.
            val label = CycleManager.getCycleDayForDate(context, current)

            if (!counts.containsKey(label)) {
                counts[label] = 0
            }

            counts[label] = (counts[label] ?: 0) + 1
            total++
            current = current.plusDays(1)
        }

        return StatisticsSummary(
            counts = counts,
            totalDays = total
        )
    }

    fun formatSummaryLines(summary: StatisticsSummary): String {
        return summary.counts.entries.joinToString(separator = "\n") { entry ->
            "${entry.key}: ${entry.value}"
        }
    }
}