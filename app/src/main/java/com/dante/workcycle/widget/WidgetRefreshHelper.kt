package com.dante.workcycle.widget

import android.content.Context
import com.dante.workcycle.widget.base.WidgetRefreshDispatcher

object WidgetRefreshHelper {

    fun refresh(context: Context) {
        WidgetRefreshDispatcher.refreshAllWidgets(context)
    }
}
