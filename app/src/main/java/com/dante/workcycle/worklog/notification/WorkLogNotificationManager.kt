package com.dante.workcycle.worklog.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class WorkLogNotificationManager(
    private val context: Context
) {

    private val appContext = context.applicationContext
    private val notificationFactory = WorkLogNotificationFactory(appContext)

    fun showOrUpdate(state: WorkLogNotificationState) {
        if (!canPostNotifications()) {
            return
        }

        createChannelIfNeeded()
        val notificationManager = NotificationManagerCompat.from(appContext)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        try {
            notificationManager.notify(
                WorkLogNotificationConstants.NOTIFICATION_ID,
                notificationFactory.create(state).build()
            )
        } catch (_: SecurityException) {
            // Permission state can change after the pre-check.
        }
    }

    fun remove() {
        NotificationManagerCompat.from(appContext)
            .cancel(WorkLogNotificationConstants.NOTIFICATION_ID)
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        val channel = NotificationChannel(
            WorkLogNotificationConstants.CHANNEL_ID,
            WorkLogNotificationConstants.CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = WorkLogNotificationConstants.CHANNEL_DESCRIPTION
            setShowBadge(false)
        }

        manager.createNotificationChannel(channel)
    }

    private fun canPostNotifications(): Boolean {
        if (!NotificationManagerCompat.from(appContext).areNotificationsEnabled()) {
            return false
        }

        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
    }
}
