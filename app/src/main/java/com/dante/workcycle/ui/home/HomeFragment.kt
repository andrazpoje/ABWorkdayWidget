package com.dante.workcycle.ui.home

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.dante.workcycle.R
import com.dante.workcycle.core.ui.applySystemBarsBottomInsetAsPadding
import com.dante.workcycle.core.ui.applySystemBarsHorizontalInsetAsPadding
import com.dante.workcycle.core.util.DateProvider
import com.dante.workcycle.data.prefs.Prefs
import com.dante.workcycle.data.prefs.StatusLabelsPrefs
import com.dante.workcycle.domain.holiday.HolidayManager
import com.dante.workcycle.domain.schedule.DefaultScheduleResolver
import com.dante.workcycle.ui.adapter.CyclePreviewAdapter
import com.dante.workcycle.widget.WorkCycleWidgetProvider
import com.google.android.material.button.MaterialButton
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Home/Teden operational weekly overview.
 *
 * v2.8 moved primary cycle setup to onboarding and Settings, so this screen
 * should remain focused on today, weekly preview, upcoming events, and widget
 * guidance. Some legacy cycle-configuration bindings still exist here and
 * should be removed in a dedicated cleanup after parity with Settings is
 * verified.
 */
class HomeFragment : Fragment(R.layout.fragment_home) {

    companion object {
        private const val UPCOMING_EVENTS_LOOKAHEAD_DAYS = 30
        private const val UPCOMING_EVENTS_MAX_ITEMS = 5
    }

    private data class UpcomingEvent(
        val dateText: String,
        val label: String
    )

    lateinit var activityRoot: View

    lateinit var widgetPromptContainer: View
    lateinit var githubLinkText: TextView
    lateinit var mainScrollView: NestedScrollView

    lateinit var previewRecyclerView: RecyclerView
    lateinit var previewAdapter: CyclePreviewAdapter
    lateinit var previewWeekTitle: TextView
    lateinit var previousPreviewWeekButton: ImageButton
    lateinit var nextPreviewWeekButton: ImageButton
    lateinit var previewTodayButton: ImageButton

    lateinit var openWidgetsButton: MaterialButton
    lateinit var dismissWidgetTipButton: MaterialButton

    lateinit var widgetHint: TextView
    lateinit var upcomingEventsEmptyText: TextView
    lateinit var upcomingEventsList: View
    lateinit var upcomingEventRows: List<View>
    lateinit var upcomingEventDateTexts: List<TextView>
    lateinit var upcomingEventLabelTexts: List<TextView>

    var previewWeekOffset: Int = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)

        mainScrollView.applySystemBarsBottomInsetAsPadding()
        activityRoot.applySystemBarsHorizontalInsetAsPadding()

        setupPreviewRecyclerView()
        setupPreviewWeekNavigator()
        updateTodayStatus()
        updateCyclePreview()
        updateWidgetHint()
        updateUpcomingEvents()

        openWidgetsButton.setOnClickListener {
            requestAddWidget()
        }

        dismissWidgetTipButton.setOnClickListener {
            requireContext().getSharedPreferences(Prefs.PREFS_NAME, Context.MODE_PRIVATE).edit {
                putBoolean(Prefs.KEY_HOME_WIDGET_TIP_DISMISSED, true)
            }
            widgetPromptContainer.visibility = View.GONE
        }

        githubLinkText.setOnClickListener {
            val intent = Intent(
                Intent.ACTION_VIEW,
                "https://github.com/andrazpoje/ABWorkdayWidget".toUri()
            )
            startActivity(intent)
        }

        setupBackHandling()
    }

    override fun onResume() {
        super.onResume()
        updateCyclePreview()
        updateWidgetHint()
        updateUpcomingEvents()
    }

    private fun requestAddWidget() {
        val context = requireContext()
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val provider = ComponentName(context, WorkCycleWidgetProvider::class.java)

        if (appWidgetManager.isRequestPinAppWidgetSupported) {
            appWidgetManager.requestPinAppWidget(provider, null, null)
        } else {
            Toast.makeText(
                context,
                getString(R.string.widget_add_manual_hint),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun bindViews(root: View) {
        activityRoot = root
        mainScrollView = root.findViewById(R.id.main)

        widgetPromptContainer = root.findViewById(R.id.widgetPromptContainer)
        githubLinkText = root.findViewById(R.id.githubLinkText)

        widgetHint = root.findViewById(R.id.widgetHint)
        upcomingEventsEmptyText = root.findViewById(R.id.upcomingEventsEmptyText)
        upcomingEventsList = root.findViewById(R.id.upcomingEventsList)
        upcomingEventRows = listOf(
            root.findViewById(R.id.upcomingEventRow1),
            root.findViewById(R.id.upcomingEventRow2),
            root.findViewById(R.id.upcomingEventRow3),
            root.findViewById(R.id.upcomingEventRow4),
            root.findViewById(R.id.upcomingEventRow5)
        )
        upcomingEventDateTexts = listOf(
            root.findViewById(R.id.upcomingEventDate1),
            root.findViewById(R.id.upcomingEventDate2),
            root.findViewById(R.id.upcomingEventDate3),
            root.findViewById(R.id.upcomingEventDate4),
            root.findViewById(R.id.upcomingEventDate5)
        )
        upcomingEventLabelTexts = listOf(
            root.findViewById(R.id.upcomingEventLabel1),
            root.findViewById(R.id.upcomingEventLabel2),
            root.findViewById(R.id.upcomingEventLabel3),
            root.findViewById(R.id.upcomingEventLabel4),
            root.findViewById(R.id.upcomingEventLabel5)
        )
        openWidgetsButton = root.findViewById(R.id.openWidgetsButton)
        dismissWidgetTipButton = root.findViewById(R.id.dismissWidgetTipButton)

        previewRecyclerView = root.findViewById(R.id.previewRecyclerView)
        previewWeekTitle = root.findViewById(R.id.previewWeekTitle)
        previousPreviewWeekButton = root.findViewById(R.id.previousPreviewWeekButton)
        nextPreviewWeekButton = root.findViewById(R.id.nextPreviewWeekButton)
        previewTodayButton = root.findViewById(R.id.previewTodayButton)
    }

    fun onNotificationPermissionDenied() {
        Toast.makeText(
            requireContext(),
            getString(R.string.notification_permission_denied),
            Toast.LENGTH_SHORT
        ).show()
    }

    fun updateUpcomingEvents() {
        val events = collectUpcomingEvents()

        upcomingEventsEmptyText.isVisible = events.isEmpty()
        upcomingEventsList.isVisible = events.isNotEmpty()
        upcomingEventsEmptyText.text = getString(R.string.home_upcoming_events_empty)

        upcomingEventRows.forEachIndexed { index, row ->
            val event = events.getOrNull(index)
            row.isVisible = event != null
            upcomingEventDateTexts[index].text = event?.dateText.orEmpty()
            upcomingEventLabelTexts[index].text = event?.label.orEmpty()
        }
    }

    private fun collectUpcomingEvents(): List<UpcomingEvent> {
        val context = requireContext()
        val resolver = DefaultScheduleResolver(context)
        val statusLabelsPrefs = StatusLabelsPrefs(context)
        val statusLabels = statusLabelsPrefs.getLabels()
        val offDayLabel = getString(R.string.off_day_label)
        val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(Locale.getDefault())
        val separator = getString(R.string.home_upcoming_event_reason_separator)
        val events = mutableListOf<UpcomingEvent>()

        for (dayOffset in 1..UPCOMING_EVENTS_LOOKAHEAD_DAYS) {
            val date = DateProvider.today().plusDays(dayOffset.toLong())
            val resolved = runCatching { resolver.resolve(date) }.getOrNull() ?: continue
            val reasons = mutableListOf<String>()
            val isWeekend = date.dayOfWeek.value >= 6
            val isHoliday = HolidayManager.isHoliday(context, date)
            val isOffDay = resolved.effectiveCycleLabel.equals(offDayLabel, ignoreCase = true)

            if (isHoliday) {
                reasons.add(getString(R.string.home_upcoming_event_holiday))
            } else if (isOffDay && !isWeekend) {
                reasons.add(getString(R.string.home_upcoming_event_off_day))
            }

            resolved.statusTags.forEach { rawStatus ->
                val statusLabel = statusLabels.firstOrNull {
                    it.name.equals(rawStatus, ignoreCase = true)
                }
                val displayName = statusLabel?.let(statusLabelsPrefs::getDisplayName)
                    ?: rawStatus.trim()

                if (displayName.isNotBlank()) {
                    reasons.add(displayName)
                }
            }

            if (reasons.isNotEmpty()) {
                events.add(
                    UpcomingEvent(
                        dateText = date.format(dateFormatter),
                        label = reasons.distinct().joinToString(separator)
                    )
                )
            }

            if (events.size >= UPCOMING_EVENTS_MAX_ITEMS) break
        }

        return events
    }

    fun setupBackHandling() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    requireActivity().finish()
                }
            }
        )
    }
}
