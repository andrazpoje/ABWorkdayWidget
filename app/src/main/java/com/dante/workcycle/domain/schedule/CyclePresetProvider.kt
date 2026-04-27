package com.dante.workcycle.domain.schedule

import com.dante.workcycle.R

object CyclePresetProvider {

    fun getPresets(): List<CyclePreset> = listOf(

        CyclePreset(
            id = "ab",
            nameRes = R.string.preset_ab,
            cycleDaysProvider = { _ -> listOf("A", "B") },
            defaultFirstDayProvider = { _ -> "A" }
        ),

        CyclePreset(
            id = "two_shift",
            nameRes = R.string.preset_two_shift,
            cycleDaysProvider = { context ->
                listOf(
                    context.getString(R.string.template_label_two_shift_morning),
                    context.getString(R.string.template_label_two_shift_afternoon)
                )
            },
            defaultFirstDayProvider = { context ->
                context.getString(R.string.template_label_two_shift_morning)
            }
        ),

        CyclePreset(
            id = "three_shift",
            nameRes = R.string.preset_three_shift,
            cycleDaysProvider = { context ->
                listOf(
                    context.getString(R.string.template_label_three_shift_morning),
                    context.getString(R.string.template_label_three_shift_afternoon),
                    context.getString(R.string.template_label_night)
                )
            },
            defaultFirstDayProvider = { context ->
                context.getString(R.string.template_label_three_shift_morning)
            }
        )
    )

}
