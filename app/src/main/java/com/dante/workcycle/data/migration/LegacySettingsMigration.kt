package com.dante.workcycle.data.migration

import android.content.Context
import androidx.core.content.edit
import com.dante.workcycle.R
import com.dante.workcycle.data.prefs.AppPrefs
import com.dante.workcycle.domain.holiday.HolidayManager
import com.dante.workcycle.domain.schedule.CycleManager
import com.dante.workcycle.domain.schedule.CyclePresetProvider
import com.dante.workcycle.domain.template.TemplateManager
import java.time.LocalDate

/**
 * Idempotent migration for legacy SharedPreferences-based cycle settings.
 *
 * Older installs may not have the newer CycleManager prefs, start date, first
 * cycle label, skipped-day defaults, or holiday country initialized. This
 * helper keeps that safety net outside Home/Teden so Home can remain an
 * operational weekly overview while existing users upgrading from older
 * versions still get the same default setup.
 */
object LegacySettingsMigration {

    fun migrateIfNeeded(context: Context) {
        val cyclePrefs = context.getSharedPreferences(CycleManager.PREFS_NAME, Context.MODE_PRIVATE)
        val hasCycle = cyclePrefs.contains(CycleManager.KEY_CYCLE_DAYS)
        val hasStartDate = cyclePrefs.contains(CycleManager.KEY_CYCLE_START_DATE)

        val prefs = context.getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)

        if (!hasCycle || !hasStartDate) {
            val threeShiftPreset = CyclePresetProvider.getPresets().firstOrNull {
                it.id == "three_shift"
            }

            val defaultCycle = threeShiftPreset?.cycleDaysProvider?.invoke(context)
                ?: listOf(
                    context.getString(R.string.label_morning),
                    context.getString(R.string.label_afternoon),
                    context.getString(R.string.label_night)
                )

            val defaultFirstDay = threeShiftPreset?.defaultFirstDayProvider?.invoke(context)
                ?: defaultCycle.first()

            val defaultStartDate = LocalDate.now()

            CycleManager.saveCycle(context, defaultCycle)
            CycleManager.saveStartDate(context, defaultStartDate)

            prefs.edit {
                putInt(AppPrefs.KEY_START_YEAR, defaultStartDate.year)
                putInt(AppPrefs.KEY_START_MONTH, defaultStartDate.monthValue)
                putInt(AppPrefs.KEY_START_DAY, defaultStartDate.dayOfMonth)
                putString(AppPrefs.KEY_FIRST_CYCLE_DAY, defaultFirstDay)
                remove(AppPrefs.KEY_FIRST_CYCLE_DAY_INDEX)
            }

            TemplateManager.clearTemplate(context)
        }

        if (!prefs.contains(AppPrefs.KEY_OVERRIDE_SKIPPED)) {
            prefs.edit {
                putBoolean(AppPrefs.KEY_OVERRIDE_SKIPPED, true)
            }
        }

        HolidayManager.ensureCountrySelected(context)
    }
}
