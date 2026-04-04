package com.dante.workcycle

import android.app.Application
import com.dante.workcycle.core.theme.AppThemeManager

class WorkCycleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppThemeManager.applyFromPreferences(this)
    }
}