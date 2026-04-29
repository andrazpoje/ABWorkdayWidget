package com.dante.workcycle.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dante.workcycle.R
import com.dante.workcycle.core.util.DateProvider
import com.dante.workcycle.domain.schedule.DefaultScheduleResolver
import com.dante.workcycle.notifications.MidnightAlarmScheduler
import com.dante.workcycle.notifications.NotificationHelper
import com.dante.workcycle.widget.WidgetUpdater

/**
 * Handles date/time boundary broadcasts that can invalidate schedule-based UI.
 *
 * The receiver refreshes Work Cycle widgets and notification text from
 * [DefaultScheduleResolver]. Keep this path conservative: it should react to
 * real date/time changes, not become a high-frequency widget update mechanism.
 */
class DayChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_DATE_CHANGED,
            MidnightAlarmScheduler.ACTION_MIDNIGHT_FALLBACK -> {
                WidgetUpdater.updateAllWidgets(context)

                if (!NotificationHelper.areNotificationsEnabled(context)) {
                    MidnightAlarmScheduler.scheduleNext(context)
                    return
                }

                val today = DateProvider.today()
                val tomorrow = today.plusDays(1)
                val resolver = DefaultScheduleResolver(context)

                val todayLabel = resolver.resolve(today).effectiveCycleLabel
                val tomorrowLabel = resolver.resolve(tomorrow).effectiveCycleLabel

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

                MidnightAlarmScheduler.scheduleNext(context)
            }

            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED -> {
                WidgetUpdater.updateAllWidgets(context)
                MidnightAlarmScheduler.scheduleNext(context)
            }
        }
    }
}
