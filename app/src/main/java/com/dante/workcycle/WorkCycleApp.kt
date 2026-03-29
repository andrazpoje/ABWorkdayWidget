package com.dante.workcycle

import android.app.Application

class WorkCycleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppThemeManager.applyFromPreferences(this)
    }
}