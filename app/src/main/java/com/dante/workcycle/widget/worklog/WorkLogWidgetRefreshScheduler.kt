package com.dante.workcycle.widget.worklog

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.SystemClock

object WorkLogWidgetRefreshScheduler {

    const val ACTION_REFRESH = "com.dante.workcycle.widget.worklog.ACTION_REFRESH"
    private const val REQUEST_CODE_REFRESH = 4201
    private const val REFRESH_DELAY_MS = 60_000L

    fun scheduleNext(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        alarmManager.set(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + REFRESH_DELAY_MS,
            createPendingIntent(context)
        )
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        alarmManager.cancel(createPendingIntent(context))
    }

    private fun createPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, WorkLogWidgetProvider::class.java).apply {
            action = ACTION_REFRESH
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, IntArray(0))
        }

        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_REFRESH,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
