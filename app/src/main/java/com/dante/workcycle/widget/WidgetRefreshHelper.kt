package com.dante.workcycle.widget

import android.content.Context

object WidgetRefreshHelper {

    fun refresh(context: Context) {
        WidgetUpdater.updateAllWidgets(context)
    }
}