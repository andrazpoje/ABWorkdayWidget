package com.dante.workcycle.worklog.notification

import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import com.dante.workcycle.R
import com.dante.workcycle.ui.activity.WorkLogNavigationHelper

class WorkLogNotificationFactory(
    private val context: Context
) {

    fun create(state: WorkLogNotificationState): NotificationCompat.Builder {
        val detailLines = buildDetailLines(state)

        return NotificationCompat.Builder(
            context,
            WorkLogNotificationConstants.CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_work_time_24)
            .setContentTitle(state.title)
            .setContentText(state.summaryText)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    listOf(state.summaryText)
                        .plus(detailLines)
                        .joinToString(separator = "\n")
                )
            )
            .setContentIntent(createContentIntent())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setShowWhen(false)
    }

    private fun createContentIntent(): PendingIntent {
        return WorkLogNavigationHelper.createOpenWorkLogPendingIntent(
            context = context,
            requestCode = WorkLogNotificationConstants.CONTENT_INTENT_REQUEST_CODE
        )
    }

    private fun buildDetailLines(state: WorkLogNotificationState): List<String> {
        return listOfNotNull(
            state.startTimeText?.takeIf { it.isNotBlank() },
            state.workedTodayText?.takeIf { it.isNotBlank() },
            state.breakText?.takeIf { it.isNotBlank() }
        )
    }
}
