package com.dante.abworkdaywidget

import android.content.Context

object CyclePresetProvider {

    fun getPresets(): List<CyclePreset> = listOf(

        CyclePreset(
            id = "ab",
            nameRes = R.string.preset_ab,
            cycleDaysProvider = { _ -> listOf("A", "B") },
            defaultFirstDayProvider = { _ -> "A" }
        ),

        CyclePreset(
            id = "shift_4",
            nameRes = R.string.preset_shift_4,
            cycleDaysProvider = { context ->
                listOf(
                    context.getString(R.string.label_morning),
                    context.getString(R.string.label_afternoon),
                    context.getString(R.string.label_night),
                    context.getString(R.string.label_off)
                )
            },
            defaultFirstDayProvider = { context ->
                context.getString(R.string.label_morning)
            }
        ),

        CyclePreset(
            id = "day_night",
            nameRes = R.string.preset_day_night,
            cycleDaysProvider = { context ->
                listOf(
                    context.getString(R.string.label_day),
                    context.getString(R.string.label_night),
                    context.getString(R.string.label_recovery),
                    context.getString(R.string.label_off)
                )
            },
            defaultFirstDayProvider = { context ->
                context.getString(R.string.label_day)
            }
        )
    )

    fun findByDisplayName(context: Context, displayName: String): CyclePreset? {
        return getPresets().firstOrNull {
            context.getString(it.nameRes) == displayName
        }
    }
}