package com.dante.workcycle.style

import android.content.Context
import android.graphics.Color
import com.dante.workcycle.core.theme.ThemePreset

object WidgetStyleManager {

    private const val PREFS_WIDGET_STYLE = "widget_style_prefs"

    private const val KEY_THEME_PRESET = "theme_preset"
    private const val KEY_SHIFT_A_COLOR = "shift_a_color"
    private const val KEY_SHIFT_B_COLOR = "shift_b_color"
    private const val KEY_SHIFT_C_COLOR = "shift_c_color"
    private const val KEY_OFF_DAY_COLOR = "off_day_color"
    private const val KEY_WIDGET_BG_COLOR = "widget_bg_color"
    private const val KEY_PRIMARY_TEXT_COLOR = "primary_text_color"
    private const val KEY_SECONDARY_TEXT_COLOR = "secondary_text_color"
    private const val KEY_BORDER_COLOR = "border_color"

    fun getCurrentPreset(context: Context): ThemePreset {
        val prefs = getPrefs(context)
        return ThemePreset.fromStorage(
            prefs.getString(KEY_THEME_PRESET, ThemePreset.CLASSIC.storageValue)
        )
    }

    fun applyPreset(context: Context, preset: ThemePreset) {
        val colors = getPresetColors(preset)

        getPrefs(context).edit()
            .putString(KEY_THEME_PRESET, preset.storageValue)
            .putInt(KEY_SHIFT_A_COLOR, colors.shiftAColor)
            .putInt(KEY_SHIFT_B_COLOR, colors.shiftBColor)
            .putInt(KEY_SHIFT_C_COLOR, colors.shiftCColor)
            .putInt(KEY_OFF_DAY_COLOR, colors.offDayColor)
            .putInt(KEY_WIDGET_BG_COLOR, colors.widgetBackgroundColor)
            .putInt(KEY_PRIMARY_TEXT_COLOR, colors.primaryTextColor)
            .putInt(KEY_SECONDARY_TEXT_COLOR, colors.secondaryTextColor)
            .putInt(KEY_BORDER_COLOR, colors.borderColor)
            .apply()
    }

    fun getColors(context: Context): WidgetColors {
        val prefs = getPrefs(context)
        val preset = getCurrentPreset(context)
        val fallback = getPresetColors(preset)

        return WidgetColors(
            shiftAColor = prefs.getInt(KEY_SHIFT_A_COLOR, fallback.shiftAColor),
            shiftBColor = prefs.getInt(KEY_SHIFT_B_COLOR, fallback.shiftBColor),
            shiftCColor = prefs.getInt(KEY_SHIFT_C_COLOR, fallback.shiftCColor),
            offDayColor = prefs.getInt(KEY_OFF_DAY_COLOR, fallback.offDayColor),
            widgetBackgroundColor = prefs.getInt(KEY_WIDGET_BG_COLOR, fallback.widgetBackgroundColor),
            primaryTextColor = prefs.getInt(KEY_PRIMARY_TEXT_COLOR, fallback.primaryTextColor),
            secondaryTextColor = prefs.getInt(KEY_SECONDARY_TEXT_COLOR, fallback.secondaryTextColor),
            borderColor = prefs.getInt(KEY_BORDER_COLOR, fallback.borderColor)
        )
    }

    fun updateShiftAColor(context: Context, color: Int) {
        saveCustom(context, KEY_SHIFT_A_COLOR, color)
    }

    fun updateShiftBColor(context: Context, color: Int) {
        saveCustom(context, KEY_SHIFT_B_COLOR, color)
    }

    fun updateShiftCColor(context: Context, color: Int) {
        saveCustom(context, KEY_SHIFT_C_COLOR, color)
    }

    fun updateOffDayColor(context: Context, color: Int) {
        saveCustom(context, KEY_OFF_DAY_COLOR, color)
    }

    fun updateWidgetBackgroundColor(context: Context, color: Int) {
        saveCustom(context, KEY_WIDGET_BG_COLOR, color)
    }

    private fun saveCustom(context: Context, key: String, color: Int) {
        getPrefs(context).edit()
            .putString(KEY_THEME_PRESET, ThemePreset.CUSTOM.storageValue)
            .putInt(key, color)
            .apply()
    }

    private fun getPrefs(context: Context) =
        context.getSharedPreferences(PREFS_WIDGET_STYLE, Context.MODE_PRIVATE)

    fun getPresetColors(preset: ThemePreset): WidgetColors {
        return when (preset) {
            ThemePreset.CLASSIC -> WidgetColors(
                shiftAColor = Color.parseColor("#1976D2"),
                shiftBColor = Color.parseColor("#2E7D32"),
                shiftCColor = Color.parseColor("#F57C00"),
                offDayColor = Color.parseColor("#757575"),
                widgetBackgroundColor = Color.WHITE,
                primaryTextColor = Color.parseColor("#111111"),
                secondaryTextColor = Color.parseColor("#666666"),
                borderColor = Color.parseColor("#DDDDDD")
            )

            ThemePreset.DARK -> WidgetColors(
                shiftAColor = Color.parseColor("#0D47A1"),
                shiftBColor = Color.parseColor("#1B5E20"),
                shiftCColor = Color.parseColor("#E65100"),
                offDayColor = Color.parseColor("#616161"),
                widgetBackgroundColor = Color.parseColor("#121212"),
                primaryTextColor = Color.WHITE,
                secondaryTextColor = Color.parseColor("#BDBDBD"),
                borderColor = Color.parseColor("#2A2A2A")
            )

            ThemePreset.CUSTOM -> getPresetColors(ThemePreset.CLASSIC)
        }
    }
}
