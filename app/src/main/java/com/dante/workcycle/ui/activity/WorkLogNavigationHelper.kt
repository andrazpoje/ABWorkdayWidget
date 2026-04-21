package com.dante.workcycle.ui.activity

import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object WorkLogNavigationHelper {

    const val EXTRA_OPEN_WORK_LOG = "com.dante.workcycle.extra.OPEN_WORK_LOG"

    fun createOpenWorkLogIntent(context: Context): Intent {
        return Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_WORK_LOG, true)
        }
    }

    fun createOpenWorkLogPendingIntent(
        context: Context,
        requestCode: Int
    ): PendingIntent {
        return PendingIntent.getActivity(
            context,
            requestCode,
            createOpenWorkLogIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun consumeOpenWorkLog(intent: Intent?): Boolean {
        val shouldOpen = intent?.getBooleanExtra(EXTRA_OPEN_WORK_LOG, false) == true
        if (shouldOpen) {
            intent?.removeExtra(EXTRA_OPEN_WORK_LOG)
        }
        return shouldOpen
    }
}
