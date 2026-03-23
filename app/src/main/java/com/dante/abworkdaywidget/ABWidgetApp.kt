package com.dante.abworkdaywidget

import android.app.Application

class ABWidgetApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppThemeManager.applyFromPreferences(this)
    }
}