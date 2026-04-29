package com.dante.workcycle.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.dante.workcycle.R
import com.dante.workcycle.core.util.DateProvider
import com.dante.workcycle.core.util.CycleColorHelper
import com.dante.workcycle.data.prefs.AssignmentLabelsPrefs
import com.dante.workcycle.data.prefs.Prefs
import com.dante.workcycle.domain.schedule.CycleManager
import com.dante.workcycle.domain.schedule.DefaultScheduleResolver
import com.dante.workcycle.style.WidgetStyleManager
import com.dante.workcycle.ui.activity.MainActivity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.dante.workcycle.data.prefs.SecondaryCyclePrefs
import com.dante.workcycle.domain.model.CycleMode

/**
 * AppWidgetProvider for the Work Cycle home-screen widget.
 *
 * This widget must render the same resolved schedule data as Home and Calendar:
 * all primary labels, skipped days, secondary labels, holidays, and overrides
 * should come through [DefaultScheduleResolver] instead of independent widget
 * business logic. RemoteViews code in this class should stay focused on compact
 * widget presentation, sizing, and click handling.
 */
class WorkCycleWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        try {
            for (widgetId in appWidgetIds) {
                updateSingleWidget(context, appWidgetManager, widgetId)
            }
            scheduleNextUpdate(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateSingleWidget(context, appWidgetManager, appWidgetId)
    }

    /**
     * Rebuilds one widget instance from the current resolved schedule and widget
     * size options.
     *
     * Upcoming rows use resolved/effective day data so they stay aligned with
     * app UI and future schedule-rule changes.
     */
    private fun updateSingleWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int
    ) {
        val cycle = CycleManager.loadCycle(context)
        val today = DateProvider.today()
        val resolver = DefaultScheduleResolver(context)
        val resolvedToday = resolver.resolve(today)

        val todayPrimary = resolvedToday.effectiveCycleLabel.trim()
        val todayBase = resolvedToday.baseCycleLabel.trim()
        val rawSecondary = resolvedToday.secondaryLabel?.trim()

        val secondaryMode = SecondaryCyclePrefs(context).getMode()

        val shouldShowSecondaryOverrideMarker =
            secondaryMode == CycleMode.CYCLIC &&
                    resolvedToday.isSecondaryOverridden &&
                    !resolvedToday.secondaryBaseLabel.isNullOrBlank()

        val todaySecondary = rawSecondary?.let {
            if (shouldShowSecondaryOverrideMarker) "$it*" else it
        }

        val widgetColors = WidgetStyleManager.getColors(context)

        val todayColor = CycleColorHelper.getBackgroundColor(
            context = context,
            label = todayBase,
            cycle = cycle
        )

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            widgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val options = appWidgetManager.getAppWidgetOptions(widgetId)
        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
        val maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
        val effectiveHeight = resolveEffectiveHeight(minHeight, maxHeight)

        val isMinimal = minWidth < 90 && minHeight < 90
        val isCompact = !isMinimal && effectiveHeight < 110

        val views = when {
            isMinimal -> RemoteViews(context.packageName, R.layout.widget_layout_minimal)
            isCompact -> RemoteViews(context.packageName, R.layout.widget_layout_compact)
            else -> RemoteViews(context.packageName, R.layout.widget_layout)
        }

        val mode = resolveWidgetMode(minWidth, effectiveHeight)
        val isVeryNarrow = minWidth < 140
        val isSingleCellLike = isMinimal
        val rowsToShow = if (isSingleCellLike) 0 else resolveExtraRows(minWidth, effectiveHeight)

// 2x1 compact: secondary je dovoljen
        val isCompactLike = isCompact

// 3x1 in širši 1-row widget: secondary za danes mora biti dovoljen
        val isWideSingleRow = !isSingleCellLike && !isCompactLike && rowsToShow == 0 && minWidth >= 180

        val canShowSecondaryLabel = !isSingleCellLike && (
                isCompactLike ||
                        minWidth >= 140 ||
                        isWideSingleRow
                )

        val showTodayLabel = mode != WidgetMode.SMALL && !isVeryNarrow && !isSingleCellLike
        val typography = resolveTypography(mode, minWidth, effectiveHeight)

        val skippedOverride = CycleManager.getSkippedDayOverrideLabelOrNull(context, today)

        val displayPrimary = when {
            isSingleCellLike -> resolveWidgetPrimaryDisplayLabel(
                context = context,
                rawLabel = todayPrimary,
                short = true
            )
            else -> resolveWidgetPrimaryDisplayLabel(
                context = context,
                rawLabel = todayPrimary,
                short = false
            )
        }

        val displaySecondary = when {
            skippedOverride != null -> null
            isSingleCellLike -> null
            todaySecondary.isNullOrBlank() -> null
            canShowSecondaryLabel -> {
                resolveWidgetAssignmentDisplayLabel(
                    context = context,
                    rawLabel = todaySecondary,
                    short = false
                )
            }
            else -> null
        }
        val displaySecondaryCompact = when {
            skippedOverride != null -> null
            todaySecondary.isNullOrBlank() -> null
            else -> resolveWidgetAssignmentDisplayLabel(
                context = context,
                rawLabel = todaySecondary,
                short = true
            )
        }

        if (isMinimal) {
            val minimalStyle = isMinimalStyleEnabled(context)

            views.setTextViewText(R.id.minimalText, displayPrimary)
            views.setTextColor(R.id.minimalText, todayColor)

            if (minimalStyle) {
                views.setInt(R.id.rootMinimal, "setBackgroundColor", android.graphics.Color.TRANSPARENT)
                views.setInt(R.id.minimalText, "setBackgroundColor", android.graphics.Color.TRANSPARENT)
            } else {
                views.setInt(R.id.rootMinimal, "setBackgroundColor", android.graphics.Color.TRANSPARENT)
                views.setInt(R.id.minimalText, "setBackgroundColor", android.graphics.Color.WHITE)
            }

            views.setOnClickPendingIntent(R.id.rootMinimal, pendingIntent)

            appWidgetManager.updateAppWidget(widgetId, views)
            return
        }
        if (isCompact) {
            val minimalStyle = isMinimalStyleEnabled(context)

            views.setTextViewText(R.id.primaryTextCompact, displayPrimary)
            views.setTextColor(R.id.primaryTextCompact, todayColor)

            if (!displaySecondaryCompact.isNullOrBlank()) {
                views.setViewVisibility(R.id.secondaryTextCompact, View.VISIBLE)
                views.setTextViewText(R.id.secondaryTextCompact, displaySecondaryCompact)
                views.setTextColor(R.id.secondaryTextCompact, widgetColors.secondaryTextColor)
            } else {
                views.setViewVisibility(R.id.secondaryTextCompact, View.GONE)
                views.setTextViewText(R.id.secondaryTextCompact, "")
            }

            if (minimalStyle) {
                views.setInt(R.id.rootCompact, "setBackgroundColor", android.graphics.Color.TRANSPARENT)
            } else {
                views.setInt(R.id.rootCompact, "setBackgroundColor", android.graphics.Color.WHITE)
            }

            views.setOnClickPendingIntent(R.id.rootCompact, pendingIntent)

            appWidgetManager.updateAppWidget(widgetId, views)
            return
        }

        applyAdaptiveTypography(views, typography)

        views.setTextViewText(R.id.primaryText, displayPrimary)
        views.setTextColor(R.id.primaryText, todayColor)
        views.setInt(R.id.leftColorBar, "setBackgroundColor", todayColor)

        if (displaySecondary.isNullOrBlank()) {
            views.setViewVisibility(R.id.secondaryText, View.GONE)
        } else {
            views.setViewVisibility(R.id.secondaryText, View.VISIBLE)
            views.setTextViewText(R.id.secondaryText, displaySecondary)
            views.setTextViewText(R.id.secondaryText, "• $displaySecondary")
            views.setTextColor(R.id.secondaryText, widgetColors.secondaryTextColor)
        }

        applyWidgetStyle(context, views)

        views.setViewVisibility(R.id.prefixText, View.GONE)

        if (showTodayLabel) {
            views.setViewVisibility(R.id.todayLabelText, View.VISIBLE)
            views.setTextViewText(R.id.todayLabelText, context.getString(R.string.today_label))
            views.setTextColor(R.id.todayLabelText, widgetColors.secondaryTextColor)
        } else {
            views.setViewVisibility(R.id.todayLabelText, View.GONE)
        }

        if (rowsToShow > 0) {
            views.setViewVisibility(R.id.extraDaysContainer, View.VISIBLE)

            bindExtraDayRow(
                context = context,
                resolver = resolver,
                views = views,
                cycle = cycle,
                date = today.plusDays(1),
                titleViewId = R.id.day1Title,
                cycleViewId = R.id.day1Cycle,
                assignmentViewId = R.id.day1Assignment,
                dotViewId = R.id.day1Dot,
                isTomorrow = true
            )

            if (rowsToShow >= 2) {
                showRow(views, R.id.day2Dot, R.id.day2Title, R.id.day2Cycle, R.id.day2Assignment)
                bindExtraDayRow(
                    context,
                    resolver,
                    views,
                    cycle,
                    today.plusDays(2),
                    R.id.day2Title,
                    R.id.day2Cycle,
                    R.id.day2Assignment,
                    R.id.day2Dot,
                    false
                )
            } else {
                hideRow(views, R.id.day2Dot, R.id.day2Title, R.id.day2Cycle, R.id.day2Assignment)
            }

            if (rowsToShow >= 3) {
                showRow(views, R.id.day3Dot, R.id.day3Title, R.id.day3Cycle, R.id.day3Assignment)
                bindExtraDayRow(
                    context,
                    resolver,
                    views,
                    cycle,
                    today.plusDays(3),
                    R.id.day3Title,
                    R.id.day3Cycle,
                    R.id.day3Assignment,
                    R.id.day3Dot,
                    false
                )
            } else {
                hideRow(views, R.id.day3Dot, R.id.day3Title, R.id.day3Cycle, R.id.day3Assignment)
            }

            if (rowsToShow >= 4) {
                showRow(views, R.id.day4Dot, R.id.day4Title, R.id.day4Cycle, R.id.day4Assignment)
                bindExtraDayRow(
                    context,
                    resolver,
                    views,
                    cycle,
                    today.plusDays(4),
                    R.id.day4Title,
                    R.id.day4Cycle,
                    R.id.day4Assignment,
                    R.id.day4Dot,
                    false
                )
            } else {
                hideRow(views, R.id.day4Dot, R.id.day4Title, R.id.day4Cycle, R.id.day4Assignment)
            }

            if (rowsToShow >= 5) {
                showRow(views, R.id.day5Dot, R.id.day5Title, R.id.day5Cycle, R.id.day5Assignment)
                bindExtraDayRow(
                    context,
                    resolver,
                    views,
                    cycle,
                    today.plusDays(5),
                    R.id.day5Title,
                    R.id.day5Cycle,
                    R.id.day5Assignment,
                    R.id.day5Dot,
                    false
                )
            } else {
                hideRow(views, R.id.day5Dot, R.id.day5Title, R.id.day5Cycle, R.id.day5Assignment)
            }

            if (rowsToShow >= 6) {
                showRow(views, R.id.day6Dot, R.id.day6Title, R.id.day6Cycle, R.id.day6Assignment)
                bindExtraDayRow(
                    context,
                    resolver,
                    views,
                    cycle,
                    today.plusDays(6),
                    R.id.day6Title,
                    R.id.day6Cycle,
                    R.id.day6Assignment,
                    R.id.day6Dot,
                    false
                )
            } else {
                hideRow(views, R.id.day6Dot, R.id.day6Title, R.id.day6Cycle, R.id.day6Assignment)
            }
        } else {
            views.setViewVisibility(R.id.extraDaysContainer, View.GONE)
        }

        views.setOnClickPendingIntent(R.id.rootLayout, pendingIntent)
        appWidgetManager.updateAppWidget(widgetId, views)
    }

    private fun applyWidgetStyle(context: Context, views: RemoteViews) {
        val prefs = context.getSharedPreferences(Prefs.PREFS_NAME, Context.MODE_PRIVATE)
        val colors = WidgetStyleManager.getColors(context)

        val style = prefs.getString(
            Prefs.KEY_WIDGET_STYLE,
            Prefs.WIDGET_STYLE_CLASSIC
        ) ?: Prefs.WIDGET_STYLE_CLASSIC

        if (style == Prefs.WIDGET_STYLE_MINIMAL) {
            views.setViewVisibility(R.id.leftColorBar, View.GONE)
            views.setInt(R.id.rootLayout, "setBackgroundColor", android.graphics.Color.TRANSPARENT)
        } else {
            views.setViewVisibility(R.id.leftColorBar, View.VISIBLE)
            views.setInt(R.id.rootLayout, "setBackgroundColor", colors.widgetBackgroundColor)
        }
    }

    private fun isMinimalStyleEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(Prefs.PREFS_NAME, Context.MODE_PRIVATE)
        val style = prefs.getString(
            Prefs.KEY_WIDGET_STYLE,
            Prefs.WIDGET_STYLE_CLASSIC
        ) ?: Prefs.WIDGET_STYLE_CLASSIC

        return style == Prefs.WIDGET_STYLE_MINIMAL
    }

    /**
     * Binds one upcoming-day row from the shared schedule resolver.
     *
     * Keep day decisions here data-driven from [DefaultScheduleResolver]; this
     * method should only compact labels and apply RemoteViews presentation.
     */
    private fun bindExtraDayRow(
        context: Context,
        resolver: DefaultScheduleResolver,
        views: RemoteViews,
        cycle: List<String>,
        date: LocalDate,
        titleViewId: Int,
        cycleViewId: Int,
        assignmentViewId: Int,
        dotViewId: Int,
        isTomorrow: Boolean
    ) {
        val resolved = resolver.resolve(date)
        val primary = resolveWidgetPrimaryDisplayLabel(
            context = context,
            rawLabel = resolved.effectiveCycleLabel.trim(),
            short = true
        )

        val rawSecondary = resolved.secondaryLabel

        val secondaryMode = SecondaryCyclePrefs(context).getMode()

        val shouldShowSecondaryOverrideMarker =
            secondaryMode == CycleMode.CYCLIC &&
                    resolved.isSecondaryOverridden &&
                    !resolved.secondaryBaseLabel.isNullOrBlank()

        val secondaryWithMarker = rawSecondary?.let {
            if (shouldShowSecondaryOverrideMarker) "$it*" else it
        }

        val secondary = resolveWidgetAssignmentDisplayLabel(
            context = context,
            rawLabel = secondaryWithMarker,
            short = true
        )
        val widgetColors = WidgetStyleManager.getColors(context)

        val title = if (isTomorrow) {
            context.getString(R.string.tomorrow_label)
        } else {
            date.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault()))
                .replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                }
        }

        val color = CycleColorHelper.getBackgroundColor(
            context = context,
            label = resolved.baseCycleLabel.trim(),
            cycle = cycle
        )

        views.setTextViewText(titleViewId, title)
        views.setTextViewText(cycleViewId, primary)
        views.setTextColor(titleViewId, widgetColors.secondaryTextColor)
        views.setTextColor(cycleViewId, widgetColors.primaryTextColor)

        if (secondary.isBlank()) {
            views.setViewVisibility(assignmentViewId, View.GONE)
            views.setTextViewText(assignmentViewId, "")
        } else {
            views.setViewVisibility(assignmentViewId, View.VISIBLE)
            views.setTextViewText(assignmentViewId, secondary)
            views.setTextColor(assignmentViewId, widgetColors.secondaryTextColor)
        }

        views.setImageViewBitmap(dotViewId, createDotBitmap(context, color))
    }

    private fun hideRow(
        views: RemoteViews,
        dotId: Int,
        titleId: Int,
        cycleId: Int,
        assignmentId: Int
    ) {
        views.setViewVisibility(dotId, View.GONE)
        views.setViewVisibility(titleId, View.GONE)
        views.setViewVisibility(cycleId, View.GONE)
        views.setViewVisibility(assignmentId, View.GONE)
    }

    private fun showRow(
        views: RemoteViews,
        dotId: Int,
        titleId: Int,
        cycleId: Int,
        assignmentId: Int
    ) {
        views.setViewVisibility(dotId, View.VISIBLE)
        views.setViewVisibility(titleId, View.VISIBLE)
        views.setViewVisibility(cycleId, View.VISIBLE)
        views.setViewVisibility(assignmentId, View.VISIBLE)
    }

    private fun resolveWidgetMode(minWidth: Int, minHeight: Int): WidgetMode {
        return when {
            minWidth >= 220 && minHeight >= 180 -> WidgetMode.LARGE
            minWidth >= 160 && minHeight >= 110 -> WidgetMode.MEDIUM
            else -> WidgetMode.SMALL
        }
    }

    private fun resolveEffectiveHeight(minHeight: Int, maxHeight: Int): Int {
        if (maxHeight <= 0) return minHeight
        if (minHeight <= 0) return maxHeight
        return maxOf(minHeight, maxHeight)
    }

    private fun resolveExtraRows(minWidth: Int, minHeight: Int): Int {
        if (minWidth < 90 || minHeight < 90) return 0

        return when {
            minHeight < 110 -> 0
            minHeight < 145 -> 1
            minHeight < 185 -> 2
            minHeight < 230 -> 4
            else -> 6
        }
    }

    private fun resolveTypography(
        mode: WidgetMode,
        minWidth: Int,
        minHeight: Int
    ): WidgetTypography {
        return when (mode) {
            WidgetMode.SMALL -> {
                if (minWidth < 90 || minHeight < 90) {
                    WidgetTypography(
                        prefixSp = 10f,
                        todayLabelSp = 9f,
                        primarySp = 26f,
                        secondarySp = 11f,
                        rowTitleSp = 11f,
                        rowCycleSp = 12f,
                        rowAssignmentSp = 11f
                    )
                } else if (minWidth < 100 || minHeight < 80) {
                    WidgetTypography(
                        prefixSp = 11f,
                        todayLabelSp = 10f,
                        primarySp = 30f,
                        secondarySp = 12f,
                        rowTitleSp = 12f,
                        rowCycleSp = 13f,
                        rowAssignmentSp = 12f
                    )
                } else {
                    WidgetTypography(
                        prefixSp = 12f,
                        todayLabelSp = 11f,
                        primarySp = 34f,
                        secondarySp = 13f,
                        rowTitleSp = 12f,
                        rowCycleSp = 13f,
                        rowAssignmentSp = 12f
                    )
                }
            }

            WidgetMode.MEDIUM -> WidgetTypography(
                prefixSp = 12f,
                todayLabelSp = 12f,
                primarySp = 36f,
                secondarySp = 14f,
                rowTitleSp = 13f,
                rowCycleSp = 14f,
                rowAssignmentSp = 12f
            )

            WidgetMode.LARGE -> WidgetTypography(
                prefixSp = 13f,
                todayLabelSp = 13f,
                primarySp = 38f,
                secondarySp = 15f,
                rowTitleSp = 13f,
                rowCycleSp = 14f,
                rowAssignmentSp = 12f
            )
        }
    }

    private fun applyAdaptiveTypography(
        views: RemoteViews,
        typography: WidgetTypography
    ) {
        views.setTextViewTextSize(R.id.prefixText, TypedValue.COMPLEX_UNIT_SP, typography.prefixSp)
        views.setTextViewTextSize(R.id.todayLabelText, TypedValue.COMPLEX_UNIT_SP, typography.todayLabelSp)
        views.setTextViewTextSize(R.id.primaryText, TypedValue.COMPLEX_UNIT_SP, typography.primarySp)
        views.setTextViewTextSize(R.id.secondaryText, TypedValue.COMPLEX_UNIT_SP, typography.secondarySp)

        views.setTextViewTextSize(R.id.day1Title, TypedValue.COMPLEX_UNIT_SP, typography.rowTitleSp)
        views.setTextViewTextSize(R.id.day1Cycle, TypedValue.COMPLEX_UNIT_SP, typography.rowCycleSp)
        views.setTextViewTextSize(R.id.day1Assignment, TypedValue.COMPLEX_UNIT_SP, typography.rowAssignmentSp)

        views.setTextViewTextSize(R.id.day2Title, TypedValue.COMPLEX_UNIT_SP, typography.rowTitleSp)
        views.setTextViewTextSize(R.id.day2Cycle, TypedValue.COMPLEX_UNIT_SP, typography.rowCycleSp)
        views.setTextViewTextSize(R.id.day2Assignment, TypedValue.COMPLEX_UNIT_SP, typography.rowAssignmentSp)

        views.setTextViewTextSize(R.id.day3Title, TypedValue.COMPLEX_UNIT_SP, typography.rowTitleSp)
        views.setTextViewTextSize(R.id.day3Cycle, TypedValue.COMPLEX_UNIT_SP, typography.rowCycleSp)
        views.setTextViewTextSize(R.id.day3Assignment, TypedValue.COMPLEX_UNIT_SP, typography.rowAssignmentSp)

        views.setTextViewTextSize(R.id.day4Title, TypedValue.COMPLEX_UNIT_SP, typography.rowTitleSp)
        views.setTextViewTextSize(R.id.day4Cycle, TypedValue.COMPLEX_UNIT_SP, typography.rowCycleSp)
        views.setTextViewTextSize(R.id.day4Assignment, TypedValue.COMPLEX_UNIT_SP, typography.rowAssignmentSp)

        views.setTextViewTextSize(R.id.day5Title, TypedValue.COMPLEX_UNIT_SP, typography.rowTitleSp)
        views.setTextViewTextSize(R.id.day5Cycle, TypedValue.COMPLEX_UNIT_SP, typography.rowCycleSp)
        views.setTextViewTextSize(R.id.day5Assignment, TypedValue.COMPLEX_UNIT_SP, typography.rowAssignmentSp)

        views.setTextViewTextSize(R.id.day6Title, TypedValue.COMPLEX_UNIT_SP, typography.rowTitleSp)
        views.setTextViewTextSize(R.id.day6Cycle, TypedValue.COMPLEX_UNIT_SP, typography.rowCycleSp)
        views.setTextViewTextSize(R.id.day6Assignment, TypedValue.COMPLEX_UNIT_SP, typography.rowAssignmentSp)
    }

    /**
     * Schedules the next conservative date-boundary refresh for the cycle
     * widget.
     *
     * Do not use this path for Work Time minute refresh; Work Log active-session
     * refresh is owned by the Work Time widget scheduler.
     */
    private fun scheduleNextUpdate(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, WorkCycleWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextMidnight = LocalDateTime.now()
            .plusDays(1)
            .toLocalDate()
            .atStartOfDay()

        val triggerTime = nextMidnight
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (
            intent.action == Intent.ACTION_DATE_CHANGED ||
            intent.action == Intent.ACTION_TIME_CHANGED ||
            intent.action == Intent.ACTION_TIMEZONE_CHANGED ||
            intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE
        ) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, WorkCycleWidgetProvider::class.java)
            )
            onUpdate(context, manager, ids)
        }

        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            scheduleNextUpdate(context)
        }
    }

    private data class WidgetTypography(
        val prefixSp: Float,
        val todayLabelSp: Float,
        val primarySp: Float,
        val secondarySp: Float,
        val rowTitleSp: Float,
        val rowCycleSp: Float,
        val rowAssignmentSp: Float
    )

    private enum class WidgetMode {
        SMALL,
        MEDIUM,
        LARGE
    }

    private fun dpToPx(context: Context, dp: Float): Int {
        return (dp * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)
    }

    private fun createDotBitmap(
        context: Context,
        color: Int,
        sizeDp: Float = 6f
    ): Bitmap {
        val sizePx = dpToPx(context, sizeDp)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }

        val radius = sizePx / 2f
        canvas.drawCircle(radius, radius, radius, paint)

        return bitmap
    }

    private fun getLocalizedOffDayLabel(
        context: Context,
        short: Boolean
    ): String {
        val full = context.getString(R.string.off_day_label)
        return if (short) compactCycleLabel(full) else full
    }

    private fun resolveWidgetAssignmentDisplayLabel(
        context: Context,
        rawLabel: String?,
        short: Boolean
    ): String {
        val clean = rawLabel?.trim().orEmpty()
        if (clean.isBlank()) return ""

        val prefs = AssignmentLabelsPrefs(context)
        val label = prefs.getLabelByName(clean) ?: return clean

        val display = prefs.getDisplayName(label)

        return if (short) {
            compactAssignmentLabel(display)
        } else {
            display
        }
    }

    private fun resolveWidgetPrimaryDisplayLabel(
        context: Context,
        rawLabel: String,
        short: Boolean
    ): String {
        val clean = rawLabel.trim()
        if (clean.isBlank()) return "X"

        val cycle = CycleManager.loadCycle(context)
        val isCycleLabel = cycle.any { it.trim().equals(clean, ignoreCase = true) }

        // 🔴 OFF / PROST DAN
        if (!isCycleLabel) {
            return if (short) "X" else getLocalizedOffDayLabel(context, false)
        }

        // 🔵 NORMALNI CIKEL
        return if (short) {
            compactCycleLabelUltra(clean)
        } else {
            clean
        }
    }

    private fun compactCycleLabelUltra(label: String): String {
        val trimmed = label.trim()

        if (trimmed.length <= 2) return trimmed

        // fallback → prva črka
        return trimmed.take(1).uppercase()
    }

    private fun compactCycleLabel(label: String): String {
        return if (label.length <= 7) label else label.take(7)
    }

    private fun compactAssignmentLabel(label: String): String {
        val trimmed = label.trim()
        if (trimmed.isBlank()) return ""

        return when (trimmed.lowercase()) {
            "dopust" -> "Dop."
            "bolniška", "bolniska" -> "Bol."
            "dežurstvo", "dezurstvo" -> "Dež."
            "teren" -> "Ter."
            else -> if (trimmed.length <= 5) trimmed else trimmed.take(5)
        }
    }
}
