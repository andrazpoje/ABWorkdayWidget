package com.dante.workcycle.domain.schedule

import com.dante.workcycle.domain.model.StatusLabel

object StatusTagRules {

    private const val GROUP_ABSENCE = "absence"

    fun applySelection(
        selected: StatusLabel,
        current: Set<String>,
        available: List<StatusLabel>
    ): LinkedHashSet<String> {
        val result = LinkedHashSet(current)
        val selectedGroup = groupFor(selected)

        if (isExclusive(selected)) {
            result.clear()
            result.add(selected.name)
            return result
        }

        if (selectedGroup != null) {
            val conflictingNames = available
                .filter { groupFor(it) == selectedGroup }
                .map { it.name }
                .toSet()

            result.removeAll(conflictingNames)
        }

        result.add(selected.name)
        return result
    }

    fun canSelect(
        candidate: StatusLabel,
        current: Set<String>,
        available: List<StatusLabel>
    ): Boolean {
        if (candidate.name in current) return true

        val selectedLabels = available.filter { it.name in current }
        val selectedExclusive = selectedLabels.firstOrNull(::isExclusive)

        return selectedExclusive == null || isExclusive(candidate)
    }

    fun isValid(
        selectedNames: Set<String>,
        available: List<StatusLabel>
    ): Boolean {
        val selectedLabels = available.filter { it.name in selectedNames }
        val absenceCount = selectedLabels.count { groupFor(it) == GROUP_ABSENCE }
        if (absenceCount > 1) return false

        val exclusiveCount = selectedLabels.count(::isExclusive)
        if (exclusiveCount == 0) return true

        return selectedLabels.size == 1
    }

    private fun groupFor(label: StatusLabel): String? {
        return when (label.iconKey) {
            "sick", "vacation" -> GROUP_ABSENCE
            else -> null
        }
    }

    private fun isExclusive(label: StatusLabel): Boolean {
        return label.iconKey == "sick" || label.iconKey == "vacation"
    }
}
