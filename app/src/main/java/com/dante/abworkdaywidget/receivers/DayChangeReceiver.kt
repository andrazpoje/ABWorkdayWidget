package com.dante.abworkdaywidget.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dante.abworkdaywidget.CycleManager
import com.dante.abworkdaywidget.R
import com.dante.abworkdaywidget.notifications.NotificationHelper
import com.dante.abworkdaywidget.widget.WidgetUpdater
import java.time.LocalDate

class DayChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_DATE_CHANGED -> {
                WidgetUpdater.updateAllWidgets(context)

                if (!NotificationHelper.areNotificationsEnabled(context)) return

                val today = LocalDate.now()
                val tomorrow = today.plusDays(1)

                val todayLabel = CycleManager.getCycleDayForDate(context, today)
                val tomorrowLabel = CycleManager.getCycleDayForDate(context, tomorrow)

                val message = context.getString(
                    R.string.notification_today_tomorrow,
                    todayLabel,
                    tomorrowLabel
                )

                NotificationHelper.showDayChangeNotification(
                    context = context,
                    title = context.getString(R.string.notification_day_changed_title),
                    message = message
                )
            }

            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED -> {
                WidgetUpdater.updateAllWidgets(context)
            }
        }
    }
}