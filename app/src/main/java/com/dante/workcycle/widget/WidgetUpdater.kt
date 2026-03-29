package com.dante.workcycle.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.dante.workcycle.WorkCycleWidgetProvider

object WidgetUpdater {

    fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, WorkCycleWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

        if (appWidgetIds.isEmpty()) return

        val intent = Intent(context, WorkCycleWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
        }

        context.sendBroadcast(intent)
    }
}