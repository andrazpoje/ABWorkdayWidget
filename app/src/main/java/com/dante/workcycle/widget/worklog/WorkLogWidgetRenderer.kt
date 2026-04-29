package com.dante.workcycle.widget.worklog

import android.app.PendingIntent
import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import com.dante.workcycle.R
import com.dante.workcycle.data.prefs.Prefs
import com.dante.workcycle.style.WidgetStyleManager
import com.dante.workcycle.ui.activity.WorkLogNavigationHelper

/**
 * RemoteViews renderer for the Work Time widget.
 *
 * Rendering owns layout visibility, colors, click targets, and classic/minimal
 * style application only. Work Log session state, totals, balance, and audit
 * semantics must already be resolved before a [WorkLogWidgetState] reaches this
 * class.
 */
class WorkLogWidgetRenderer(
    private val context: Context
) {

    /**
     * Creates a RemoteViews tree for one widget instance from a pre-resolved
     * display state.
     */
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

            applyWidgetStyle()

            setOnClickPendingIntent(
                R.id.workLogWidgetRoot,
                createOpenAppPendingIntent(appWidgetId)
            )
        }
    }

    private fun RemoteViews.applyWidgetStyle() {
        val prefs = context.getSharedPreferences(Prefs.PREFS_NAME, Context.MODE_PRIVATE)
        val style = prefs.getString(
            Prefs.KEY_WIDGET_STYLE,
            Prefs.WIDGET_STYLE_CLASSIC
        ) ?: Prefs.WIDGET_STYLE_CLASSIC

        if (style != Prefs.WIDGET_STYLE_MINIMAL) {
            applyClassicWidgetStyle()
            return
        }

        val colors = WidgetStyleManager.getColors(context)
        setInt(R.id.workLogWidgetRoot, "setBackgroundColor", Color.TRANSPARENT)
        setTextColor(R.id.workLogWidgetTitle, colors.secondaryTextColor)
        setTextColor(R.id.workLogWidgetStatus, colors.primaryTextColor)
        setTextColor(R.id.workLogWidgetPrimaryValue, colors.primaryTextColor)
        setTextColor(R.id.workLogWidgetSecondaryValue, colors.secondaryTextColor)
        setTextColor(R.id.workLogWidgetTertiaryValue, colors.secondaryTextColor)
    }

    private fun RemoteViews.applyClassicWidgetStyle() {
        setInt(R.id.workLogWidgetRoot, "setBackgroundResource", R.drawable.bg_widget_classic)
        setTextColor(R.id.workLogWidgetTitle, Color.parseColor("#CCFFFFFF"))
        setTextColor(R.id.workLogWidgetStatus, Color.WHITE)
        setTextColor(R.id.workLogWidgetPrimaryValue, Color.parseColor("#F2FFFFFF"))
        setTextColor(R.id.workLogWidgetSecondaryValue, Color.parseColor("#CCFFFFFF"))
        setTextColor(R.id.workLogWidgetTertiaryValue, Color.parseColor("#CCFFFFFF"))
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
