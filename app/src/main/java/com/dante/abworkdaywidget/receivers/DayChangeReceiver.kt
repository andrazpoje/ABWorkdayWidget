package com.dante.abworkdaywidget.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dante.abworkdaywidget.R
import com.dante.abworkdaywidget.notifications.NotificationHelper
import com.dante.abworkdaywidget.widget.WidgetUpdater
import com.dante.abworkdaywidget.workday.WorkdayRepository
import java.time.LocalDate
import com.dante.abworkdaywidget.ABLogic

class DayChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_DATE_CHANGED -> {
                WidgetUpdater.updateAllWidgets(context)

                if (NotificationHelper.areNotificationsEnabled(context)) {
                    val todayLabel = WorkdayRepository.getTodayLabel(context)
                    val tomorrowLabel = ABLogic.getLetterForDate(
                        context,
                        LocalDate.now().plusDays(1)
                    )

                    val message = when (todayLabel) {
                        "A", "B" -> {
                            context.getString(
                                R.string.notification_today_tomorrow,
                                todayLabel,
                                tomorrowLabel
                            )
                        }
                        "X" -> {
                            val todayText = context.getString(R.string.notification_day_off_short)
                            context.getString(
                                R.string.notification_today_tomorrow,
                                todayText,
                                tomorrowLabel
                            )
                        }
                        else -> {
                            context.getString(R.string.notification_generic_message)
                        }
                    }

                    NotificationHelper.showDayChangeNotification(
                        context = context,
                        title = context.getString(R.string.notification_day_changed_title),
                        message = message
                    )
                }
            }

            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED -> {
                WidgetUpdater.updateAllWidgets(context)
            }
        }
    }
}