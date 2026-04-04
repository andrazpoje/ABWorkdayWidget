package com.dante.workcycle.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.dante.workcycle.receivers.DayChangeReceiver
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object MidnightAlarmScheduler {

    private const val REQUEST_CODE_MIDNIGHT_FALLBACK = 4001
    const val ACTION_MIDNIGHT_FALLBACK =
        "com.dante.workcycle.action.MIDNIGHT_FALLBACK"

    fun scheduleNext(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = pendingIntent(context)

        alarmManager.cancel(pendingIntent)

        val triggerAt = nextTriggerMillis()

        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        alarmManager.canScheduleExactAlarms() -> {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAt,
                        pendingIntent
                    )
                }

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAt,
                        pendingIntent
                    )
                }

                else -> {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerAt,
                        pendingIntent
                    )
                }
            }
        } catch (_: SecurityException) {
            // fallback brez exact alarma
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            }
        }
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent(context))
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, DayChangeReceiver::class.java).apply {
            action = ACTION_MIDNIGHT_FALLBACK
        }

        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_MIDNIGHT_FALLBACK,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun nextTriggerMillis(): Long {
        val zone = ZoneId.systemDefault()
        val tomorrow = LocalDate.now(zone).plusDays(1)
        val nextRun = LocalDateTime.of(
            tomorrow.year,
            tomorrow.month,
            tomorrow.dayOfMonth,
            0,
            5
        )
        return nextRun.atZone(zone).toInstant().toEpochMilli()
    }
}