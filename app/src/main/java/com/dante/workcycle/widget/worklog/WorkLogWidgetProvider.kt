package com.dante.workcycle.widget.worklog

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

class WorkLogWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val appContext = context.applicationContext
        val stateFactory = WorkLogWidgetStateFactory(appContext)
        val renderer = WorkLogWidgetRenderer(appContext)

        for (appWidgetId in appWidgetIds) {
            val state = stateFactory.createCurrentState()
            val views = renderer.render(appWidgetId, state)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
