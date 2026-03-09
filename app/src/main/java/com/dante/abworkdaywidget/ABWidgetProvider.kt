package com.dante.abworkdaywidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews

class ABWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {

        try {

            for (widgetId in appWidgetIds) {

                val views = RemoteViews(
                    context.packageName,
                    R.layout.widget_layout
                )

                // A / B / X
                views.setTextViewText(
                    R.id.abText,
                    ABLogic.getTodayLetter(context)
                )

                val prefs = context.getSharedPreferences("abprefs", Context.MODE_PRIVATE)
                val prefix = prefs.getString("prefixText","") ?: ""

                val options = appWidgetManager.getAppWidgetOptions(widgetId)
                val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,0)
                val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,0)

                val showPrefix = prefix.isNotBlank() && (minWidth >= 110 || minHeight >= 110)

                if (showPrefix) {
                    views.setViewVisibility(R.id.prefixText, View.VISIBLE)
                    views.setTextViewText(R.id.prefixText, prefix)
                } else {
                    views.setViewVisibility(R.id.prefixText, View.GONE)
                }

                // klik na widget → odpre aplikacijo
                val intent = Intent(context, MainActivity::class.java)

                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE
                )

                views.setOnClickPendingIntent(
                    R.id.abText,
                    pendingIntent
                )

                appWidgetManager.updateAppWidget(widgetId, views)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (
            intent.action == Intent.ACTION_DATE_CHANGED ||
            intent.action == Intent.ACTION_BOOT_COMPLETED
        ) {

            val manager = AppWidgetManager.getInstance(context)

            val ids = manager.getAppWidgetIds(
                ComponentName(context, ABWidgetProvider::class.java)
            )

            onUpdate(context, manager, ids)
        }
    }
}