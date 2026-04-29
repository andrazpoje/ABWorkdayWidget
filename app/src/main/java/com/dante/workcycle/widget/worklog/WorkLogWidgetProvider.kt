package com.dante.workcycle.widget.worklog

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/**
 * AppWidgetProvider for the Work Time widget.
 *
 * The provider coordinates state creation, RemoteViews rendering, and optional
 * minute refresh scheduling. Work Log session decisions belong in
 * [WorkLogWidgetStateFactory] and must stay consistent with the dashboard,
 * recent events, manual edit audit safety, and future multiple-session support.
 */
class WorkLogWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == WorkLogWidgetRefreshScheduler.ACTION_REFRESH) {
            val appContext = context.applicationContext
            val appWidgetManager = AppWidgetManager.getInstance(appContext)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(appContext, WorkLogWidgetProvider::class.java)
            )
            onUpdate(appContext, appWidgetManager, appWidgetIds)
            return
        }

        super.onReceive(context, intent)
    }

    /**
     * Renders every widget instance and schedules minute refresh only when the
     * current Work Log state explicitly requires live active-session updates.
     */
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val appContext = context.applicationContext
        val stateFactory = WorkLogWidgetStateFactory(appContext)
        val renderer = WorkLogWidgetRenderer(appContext)
        var shouldScheduleNextRefresh = false

        for (appWidgetId in appWidgetIds) {
            val state = stateFactory.createCurrentState()
            val views = renderer.render(appWidgetId, state)
            appWidgetManager.updateAppWidget(appWidgetId, views)
            shouldScheduleNextRefresh = shouldScheduleNextRefresh || state.requiresMinuteRefresh
        }

        if (shouldScheduleNextRefresh) {
            WorkLogWidgetRefreshScheduler.scheduleNext(appContext)
        } else {
            WorkLogWidgetRefreshScheduler.cancel(appContext)
        }
    }

    override fun onDisabled(context: Context) {
        WorkLogWidgetRefreshScheduler.cancel(context.applicationContext)
        super.onDisabled(context)
    }
}
