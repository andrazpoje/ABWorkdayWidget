package com.dante.abworkdaywidget.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.dante.abworkdaywidget.MainActivity
import com.dante.abworkdaywidget.R
import com.dante.abworkdaywidget.data.Prefs

object NotificationHelper {

    private const val CHANNEL_NORMAL = "ab_day_change_normal"
    private const val CHANNEL_SILENT = "ab_day_change_silent"
    private const val NOTIFICATION_ID = 1001

    fun areNotificationsEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(Prefs.PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(Prefs.KEY_NOTIFICATIONS_ENABLED, false)
    }

    private fun isSilentNotificationEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(Prefs.PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(Prefs.KEY_SILENT_NOTIFICATION, false)
    }

    fun showDayChangeNotification(
        context: Context,
        title: String,
        message: String
    ) {
        if (!areNotificationsEnabled(context)) return

        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createChannels(context, manager)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            2001,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val silent = isSilentNotificationEnabled(context)
        val channelId = if (silent) CHANNEL_SILENT else CHANNEL_NORMAL

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(
                if (silent) NotificationCompat.PRIORITY_LOW
                else NotificationCompat.PRIORITY_DEFAULT
            )
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun createChannels(context: Context, manager: NotificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val normalChannel = NotificationChannel(
            CHANNEL_NORMAL,
            context.getString(R.string.notification_channel_normal_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_normal_desc)
        }

        val silentChannel = NotificationChannel(
            CHANNEL_SILENT,
            context.getString(R.string.notification_channel_silent_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.notification_channel_silent_desc)
            setSound(null, null)
            enableVibration(false)
        }

        manager.createNotificationChannel(normalChannel)
        manager.createNotificationChannel(silentChannel)
    }
}