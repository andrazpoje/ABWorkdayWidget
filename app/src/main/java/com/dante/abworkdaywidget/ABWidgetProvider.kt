package com.dante.abworkdaywidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import android.app.AlarmManager
import java.time.LocalDateTime
import java.time.ZoneId

class ABWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {

        try {

            for (widgetId in appWidgetIds) {

                val views = RemoteViews(
                    context.packageName,
                    R.layout.widget_layout
                )

                val letter = ABLogic.getTodayLetter(context)

                val prefs = context.getSharedPreferences("abprefs", Context.MODE_PRIVATE)

                val prefix = prefs.getString("prefixText","") ?: ""

                val labelA = prefs.getString("labelA", "A") ?: "A"
                val labelB = prefs.getString("labelB", "B") ?: "B"
                val labelX = prefs.getString("labelX", "X") ?: "X"

                val display = when (letter) {
                    "A" -> labelA
                    "B" -> labelB
                    else -> labelX
                }

                views.setTextViewText(R.id.abText, display)

                val color = when (letter) {
                    "A" -> ContextCompat.getColor(context, R.color.shiftA)
                    "B" -> ContextCompat.getColor(context, R.color.shiftB)
                    else -> ContextCompat.getColor(context, R.color.shiftOff)
                }

                views.setTextColor(R.id.abText, color)




                val options = appWidgetManager.getAppWidgetOptions(widgetId)
                val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,0)
                val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,0)

                val showPrefix = prefix.isNotBlank() && (minWidth >= 110 || minHeight >= 110)

                if (showPrefix) {
                    views.setViewVisibility(R.id.prefixText, View.VISIBLE)
                    views.setTextViewText(R.id.prefixText, prefix)
                } else {
                    views.setViewVisibility(R.id.prefixText, View.GONE)
                }

                val intent = Intent(context, MainActivity::class.java)

                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE
                )

                views.setOnClickPendingIntent(
                    R.id.abText,
                    pendingIntent
                )

                appWidgetManager.updateAppWidget(widgetId, views)
            }

            scheduleNextUpdate(context)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun scheduleNextUpdate(context: Context) {

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ABWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextMidnight = LocalDateTime.now()
            .plusDays(1)
            .toLocalDate()
            .atStartOfDay()

        val triggerTime = nextMidnight
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (
            intent.action == Intent.ACTION_DATE_CHANGED ||
            intent.action == Intent.ACTION_TIME_CHANGED ||
            intent.action == Intent.ACTION_TIMEZONE_CHANGED ||
            intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE
        ) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, ABWidgetProvider::class.java)
            )
            onUpdate(context, manager, ids)
        }
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            scheduleNextUpdate(context)
        }
    }
}