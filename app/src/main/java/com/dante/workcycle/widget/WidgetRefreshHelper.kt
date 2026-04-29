package com.dante.workcycle.widget

import android.content.Context
import com.dante.workcycle.widget.base.WidgetRefreshDispatcher

/**
 * Backward-compatible facade for callers that need to refresh all widgets.
 *
 * New code should prefer [WidgetRefreshDispatcher] when it knows whether only
 * Work Cycle or only Work Time widgets need updating.
 */
object WidgetRefreshHelper {

    fun refresh(context: Context) {
        WidgetRefreshDispatcher.refreshAllWidgets(context)
    }
}
