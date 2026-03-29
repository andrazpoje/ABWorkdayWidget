package com.dante.workcycle

import android.content.Context

data class CyclePreset(
    val id: String,
    val nameRes: Int,
    val cycleDaysProvider: (Context) -> List<String>,
    val defaultFirstDayProvider: (Context) -> String
)