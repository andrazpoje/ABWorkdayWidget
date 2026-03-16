package com.dante.abworkdaywidget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class ABWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        try {
            for (widgetId in appWidgetIds) {
                updateSingleWidget(context, appWidgetManager, widgetId)
            }
            scheduleNextUpdate(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateSingleWidget(context, appWidgetManager, appWidgetId)
    }

    private fun updateSingleWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int
    ) {
        val cycle = CycleManager.loadCycle(context)
        val today = LocalDate.now()
        val todayCycle = CycleManager.getCycleDayForDate(context, today)

        val prefs = context.getSharedPreferences("abprefs", Context.MODE_PRIVATE)
        val prefix = prefs.getString("prefixText", "") ?: ""

        val todayColor = CycleColorHelper.getBackgroundColor(
            context = context,
            label = todayCycle,
            cycle = cycle
        )

        val views = RemoteViews(
            context.packageName,
            R.layout.widget_layout
        )

        val options = appWidgetManager.getAppWidgetOptions(widgetId)
        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)

        val mode = resolveWidgetMode(minWidth, minHeight)

        val showPrefix = prefix.isNotBlank() && mode != WidgetMode.SMALL
        val showTodayLabel = mode != WidgetMode.SMALL
        val showExtraDays = mode != WidgetMode.SMALL

        views.setTextViewText(R.id.abText, todayCycle)
        views.setTextColor(R.id.abText, todayColor)
        views.setInt(R.id.leftColorBar, "setBackgroundColor", todayColor)

        if (showPrefix) {
            views.setViewVisibility(R.id.prefixText, View.VISIBLE)
            views.setTextViewText(R.id.prefixText, prefix)
        } else {
            views.setViewVisibility(R.id.prefixText, View.GONE)
        }

        if (showTodayLabel) {
            views.setViewVisibility(R.id.todayLabelText, View.VISIBLE)
            views.setTextViewText(R.id.todayLabelText, context.getString(R.string.today_label))
        } else {
            views.setViewVisibility(R.id.todayLabelText, View.GONE)
        }

        if (showExtraDays) {
            views.setViewVisibility(R.id.extraDaysContainer, View.VISIBLE)

            bindExtraDayRow(
                context = context,
                views = views,
                date = today.plusDays(1),
                titleViewId = R.id.day1Title,
                valueViewId = R.id.day1Value,
                dotViewId = R.id.day1Dot,
                isTomorrow = true
            )

            if (mode == WidgetMode.MEDIUM) {
                hideRow(views, R.id.day2Dot, R.id.day2Title, R.id.day2Value)
                hideRow(views, R.id.day3Dot, R.id.day3Title, R.id.day3Value)
                hideRow(views, R.id.day4Dot, R.id.day4Title, R.id.day4Value)
                hideRow(views, R.id.day5Dot, R.id.day5Title, R.id.day5Value)
                hideRow(views, R.id.day6Dot, R.id.day6Title, R.id.day6Value)
            } else {
                showRow(views, R.id.day2Dot, R.id.day2Title, R.id.day2Value)
                showRow(views, R.id.day3Dot, R.id.day3Title, R.id.day3Value)
                showRow(views, R.id.day4Dot, R.id.day4Title, R.id.day4Value)
                showRow(views, R.id.day5Dot, R.id.day5Title, R.id.day5Value)
                showRow(views, R.id.day6Dot, R.id.day6Title, R.id.day6Value)

                bindExtraDayRow(context, views, today.plusDays(2), R.id.day2Title, R.id.day2Value, R.id.day2Dot, false)
                bindExtraDayRow(context, views, today.plusDays(3), R.id.day3Title, R.id.day3Value, R.id.day3Dot, false)
                bindExtraDayRow(context, views, today.plusDays(4), R.id.day4Title, R.id.day4Value, R.id.day4Dot, false)
                bindExtraDayRow(context, views, today.plusDays(5), R.id.day5Title, R.id.day5Value, R.id.day5Dot, false)
                bindExtraDayRow(context, views, today.plusDays(6), R.id.day6Title, R.id.day6Value, R.id.day6Dot, false)
            }
        } else {
            views.setViewVisibility(R.id.extraDaysContainer, View.GONE)
        }

        val intent = Intent(context, MainActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(
            context,
            widgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        views.setOnClickPendingIntent(R.id.rootLayout, pendingIntent)

        appWidgetManager.updateAppWidget(widgetId, views)
    }

    private fun bindExtraDayRow(
        context: Context,
        views: RemoteViews,
        date: LocalDate,
        titleViewId: Int,
        valueViewId: Int,
        dotViewId: Int,
        isTomorrow: Boolean
    ) {
        val cycle = CycleManager.loadCycle(context)
        val label = CycleManager.getCycleDayForDate(context, date)

        val title = if (isTomorrow) {
            context.getString(R.string.tomorrow_label)
        } else {
            date.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault()))
                .replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                }
        }

        val color = CycleColorHelper.getBackgroundColor(
            context = context,
            label = label,
            cycle = cycle
        )

        views.setTextViewText(titleViewId, title)
        views.setTextViewText(valueViewId, label)

        views.setTextColor(titleViewId, 0xCCFFFFFF.toInt())
        views.setTextColor(valueViewId, 0xFFFFFFFF.toInt())
        views.setInt(dotViewId, "setBackgroundColor", color)
    }

    private fun hideRow(views: RemoteViews, dotId: Int, titleId: Int, valueId: Int) {
        views.setViewVisibility(dotId, View.GONE)
        views.setViewVisibility(titleId, View.GONE)
        views.setViewVisibility(valueId, View.GONE)
    }

    private fun showRow(views: RemoteViews, dotId: Int, titleId: Int, valueId: Int) {
        views.setViewVisibility(dotId, View.VISIBLE)
        views.setViewVisibility(titleId, View.VISIBLE)
        views.setViewVisibility(valueId, View.VISIBLE)
    }

    private fun resolveWidgetMode(minWidth: Int, minHeight: Int): WidgetMode {
        return when {
            minWidth >= 180 || minHeight >= 220 -> WidgetMode.LARGE
            minWidth >= 120 || minHeight >= 120 -> WidgetMode.MEDIUM
            else -> WidgetMode.SMALL
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

    private enum class WidgetMode {
        SMALL,
        MEDIUM,
        LARGE
    }
}