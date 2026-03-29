package com.dante.abworkdaywidget.style

enum class ThemePreset(val storageValue: String) {
    CLASSIC("classic"),
    DARK("dark"),
    CUSTOM("custom");

    companion object {
        fun fromStorage(value: String?): ThemePreset {
            return entries.firstOrNull { it.storageValue == value } ?: CLASSIC
        }
    }
}