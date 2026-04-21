package com.dante.workcycle.widget.worklog

import android.app.PendingIntent
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import com.dante.workcycle.R
import com.dante.workcycle.ui.activity.WorkLogNavigationHelper

class WorkLogWidgetRenderer(
    private val context: Context
) {

    fun render(
        appWidgetId: Int,
        state: WorkLogWidgetState
    ): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_work_log).apply {
            setTextViewText(R.id.workLogWidgetTitle, state.title)
            setTextViewText(R.id.workLogWidgetStatus, state.statusText)
            setTextViewText(R.id.workLogWidgetPrimaryValue, state.primaryValueText)

            bindOptionalText(
                viewId = R.id.workLogWidgetSecondaryValue,
                value = state.secondaryValueText
            )
            bindOptionalText(
                viewId = R.id.workLogWidgetTertiaryValue,
                value = state.tertiaryValueText
            )

            setOnClickPendingIntent(
                R.id.workLogWidgetRoot,
                createOpenAppPendingIntent(appWidgetId)
            )
        }
    }

    private fun RemoteViews.bindOptionalText(
        viewId: Int,
        value: String?
    ) {
        if (value.isNullOrBlank()) {
            setViewVisibility(viewId, View.GONE)
            setTextViewText(viewId, "")
        } else {
            setViewVisibility(viewId, View.VISIBLE)
            setTextViewText(viewId, value)
        }
    }

    private fun createOpenAppPendingIntent(appWidgetId: Int): PendingIntent {
        return WorkLogNavigationHelper.createOpenWorkLogPendingIntent(
            context = context,
            requestCode = appWidgetId
        )
    }
}
