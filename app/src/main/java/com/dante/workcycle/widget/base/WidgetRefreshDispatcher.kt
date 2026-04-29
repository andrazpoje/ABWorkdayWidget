package com.dante.workcycle.widget.base

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.annotation.VisibleForTesting
import com.dante.workcycle.widget.WidgetUpdater
import com.dante.workcycle.widget.worklog.WorkLogWidgetProvider

/**
 * Central dispatcher for explicit widget refresh requests from app flows.
 *
 * Use this instead of sending ad-hoc broadcasts from feature code. It keeps
 * Work Cycle and Work Time refresh paths separate while allowing settings,
 * Work Log events, and schedule changes to refresh the relevant widget surface.
 */
object WidgetRefreshDispatcher {

    fun refreshWorkLogWidgets(context: Context) {
        refreshProvider(
            context = context,
            providerClass = WorkLogWidgetProvider::class.java
        )
    }

    fun refreshCycleWidgets(context: Context) {
        WidgetUpdater.updateAllWidgets(context)
    }

    fun refreshAllWidgets(context: Context) {
        refreshCycleWidgets(context)
        refreshWorkLogWidgets(context)
    }

    @VisibleForTesting
    internal fun refreshProvider(
        context: Context,
        providerClass: Class<*>
    ) {
        val appContext = context.applicationContext
        val appWidgetManager = AppWidgetManager.getInstance(appContext)
        val componentName = ComponentName(appContext, providerClass)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

        if (appWidgetIds.isEmpty()) return

        val intent = Intent(appContext, providerClass).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
        }

        appContext.sendBroadcast(intent)
    }
}
