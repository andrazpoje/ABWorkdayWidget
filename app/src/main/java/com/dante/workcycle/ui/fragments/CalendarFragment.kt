package com.dante.workcycle.ui.fragments

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.os.Bundle
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dante.workcycle.CalendarDayItem
import com.dante.workcycle.R
import com.dante.workcycle.core.ui.applySystemBarsBottomInsetAsPadding
import com.dante.workcycle.core.ui.applySystemBarsHorizontalInsetAsPadding
import com.dante.workcycle.core.util.CycleColorHelper
import com.dante.workcycle.data.prefs.AssignmentCyclePrefs
import com.dante.workcycle.data.prefs.AssignmentLabelsPrefs
import com.dante.workcycle.domain.holiday.HolidayManager
import com.dante.workcycle.domain.schedule.CycleManager
import com.dante.workcycle.domain.schedule.DefaultScheduleResolver
import com.dante.workcycle.ui.adapter.CalendarAdapter
import com.dante.workcycle.ui.dialogs.EditAssignmentDayBottomSheet
import com.dante.workcycle.widget.WidgetRefreshHelper
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import com.dante.workcycle.core.util.DateProvider

class CalendarFragment : Fragment(R.layout.fragment_calendar) {

    private lateinit var calendarRoot: View
    private lateinit var calendarContentContainer: View
    private lateinit var calendarScrollView: NestedScrollView

    private lateinit var recycler: RecyclerView
    private lateinit var monthTitle: TextView
    private lateinit var previousMonthButton: ImageButton
    private lateinit var nextMonthButton: ImageButton
    private lateinit var weekHeader: LinearLayout

    private lateinit var displayedMonth: LocalDate
    private lateinit var gestureDetector: GestureDetector

    private var selectedDate: LocalDate? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)

        calendarScrollView.applySystemBarsBottomInsetAsPadding()
        calendarContentContainer.applySystemBarsHorizontalInsetAsPadding()

        setupWeekHeader()

        recycler.layoutManager = GridLayoutManager(requireContext(), 7)

        setupMonthSwipe()

        displayedMonth = LocalDate.now().withDayOfMonth(1)
        renderMonth(displayedMonth)

        previousMonthButton.setOnClickListener {
            goToPreviousMonth()
        }

        nextMonthButton.setOnClickListener {
            goToNextMonth()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::displayedMonth.isInitialized) {
            renderMonth(displayedMonth)
        }
    }

    private fun bindViews(root: View) {
        calendarRoot = root
        calendarContentContainer = root.findViewById(R.id.calendarContentContainer)
        calendarScrollView = root.findViewById(R.id.calendarScrollView)

        recycler = root.findViewById(R.id.calendarRecycler)
        monthTitle = root.findViewById(R.id.monthTitle)
        previousMonthButton = root.findViewById(R.id.previousMonthButton)
        nextMonthButton = root.findViewById(R.id.nextMonthButton)
        weekHeader = root.findViewById(R.id.weekHeader)
    }

    private fun setupWeekHeader() {
        val days = resources.getStringArray(R.array.week_days_short)

        weekHeader.removeAllViews()

        for (day in days) {
            val tv = TextView(requireContext()).apply {
                text = day
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
                gravity = Gravity.CENTER
                textSize = 12f
                setTypeface(typeface, Typeface.BOLD)
                alpha = 0.7f
            }
            weekHeader.addView(tv)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupMonthSwipe() {
        gestureDetector = GestureDetector(
            requireContext(),
            object : GestureDetector.SimpleOnGestureListener() {

                private val swipeThreshold = 120
                private val swipeVelocityThreshold = 120

                override fun onDown(e: MotionEvent): Boolean = true

                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    if (e1 == null) return false

                    val diffX = e2.x - e1.x
                    val diffY = e2.y - e1.y

                    if (abs(diffX) > abs(diffY) &&
                        abs(diffX) > swipeThreshold &&
                        abs(velocityX) > swipeVelocityThreshold
                    ) {
                        if (diffX > 0) {
                            goToPreviousMonth()
                        } else {
                            goToNextMonth()
                        }
                        return true
                    }

                    return false
                }
            }
        )

        recycler.setOnTouchListener { view, event ->
            val handled = gestureDetector.onTouchEvent(event)

            if (handled && event.actionMasked == MotionEvent.ACTION_UP) {
                view.performClick()
            }

            false
        }
    }

    private fun goToPreviousMonth() {
        displayedMonth = displayedMonth.minusMonths(1).withDayOfMonth(1)
        renderMonth(displayedMonth)
    }

    private fun goToNextMonth() {
        displayedMonth = displayedMonth.plusMonths(1).withDayOfMonth(1)
        renderMonth(displayedMonth)
    }

    private fun renderMonth(monthDate: LocalDate) {
        val monthName = monthDate.month
            .getDisplayName(TextStyle.FULL, Locale.getDefault())
            .replaceFirstChar { it.titlecase(Locale.getDefault()) }

        monthTitle.text = getString(
            R.string.calendar_month_year,
            monthName,
            monthDate.year
        )

        val items = buildMonthItems(monthDate)

        recycler.adapter = CalendarAdapter(
            items = items,
            onDayClick = { item ->
                val date = item.date ?: return@CalendarAdapter

                if (!isAdded || parentFragmentManager.isStateSaved) return@CalendarAdapter

                selectedDate = date
                renderMonth(displayedMonth)

                EditAssignmentDayBottomSheet(
                    date = date,
                    onSaved = {
                        if (!isAdded) return@EditAssignmentDayBottomSheet
                        renderMonth(displayedMonth)
                        WidgetRefreshHelper.refresh(requireContext())
                    }
                ).show(parentFragmentManager, "editDay")
            }
        )
    }

    private fun buildMonthItems(monthDate: LocalDate): List<CalendarDayItem> {
        val ctx = context ?: return emptyList()

        val start = monthDate.withDayOfMonth(1)
        val end = monthDate.withDayOfMonth(monthDate.lengthOfMonth())

        val resolver = DefaultScheduleResolver(ctx)
        val labelsPrefs = AssignmentLabelsPrefs(ctx)
        val cycle = CycleManager.loadCycle(ctx)
        val today = com.dante.workcycle.core.util.DateProvider.today()

        val result = mutableListOf<CalendarDayItem>()

        val leadingEmptyDays = start.dayOfWeek.toMondayBasedIndex()
        repeat(leadingEmptyDays) {
            result.add(
                CalendarDayItem(
                    date = null,
                    isCurrentMonth = false,
                    isEmpty = true
                )
            )
        }

        var current = start
        while (!current.isAfter(end)) {
            val resolved = resolver.resolve(current)

            val effectiveCycleLabel = resolved.effectiveCycleLabel
            val baseCycleLabel = resolved.baseCycleLabel

            val cycleColor = CycleColorHelper.getBackgroundColor(
                context = ctx,
                label = baseCycleLabel,
                cycle = cycle
            )

            val rawAssignmentLabel = resolved.assignmentLabel
                ?.trim()
                ?.ifBlank { null }

            val displayAssignmentLabel = rawAssignmentLabel?.let {
                val value = shortenSecondaryLabel(it)
                if (resolved.isAssignmentOverridden) "$value*" else value
            }

            val assignmentColor = rawAssignmentLabel
                ?.let { labelsPrefs.getLabelByName(it)?.color }

            val skippedOverrideLabel = CycleManager.getSkippedDayOverrideLabelOrNull(ctx, current)

            result.add(
                CalendarDayItem(
                    date = current,
                    dayNumber = current.dayOfMonth.toString(),
                    effectiveCycleLabel = shortenPrimaryCycleLabel(effectiveCycleLabel),
                    assignmentLabel = displayAssignmentLabel,
                    cycleColor = cycleColor,
                    assignmentColor = assignmentColor,
                    isOffDay = skippedOverrideLabel != null,
                    isToday = current == today,
                    isCurrentMonth = true,
                    isEmpty = false,
                    isSelected = current == selectedDate
                )
            )

            current = current.plusDays(1)
        }

        while (result.size % 7 != 0) {
            result.add(
                CalendarDayItem(
                    date = null,
                    isCurrentMonth = false,
                    isEmpty = true
                )
            )
        }

        return result
    }

    private fun shortenPrimaryCycleLabel(label: String): String {
        val normalized = label.trim()
        return if (normalized.length <= 5) normalized else normalized.take(5)
    }

    private fun shortenSecondaryLabel(label: String): String {
        val normalized = label.trim().removeSuffix("*").trim()

        val labelsPrefs = AssignmentLabelsPrefs(requireContext())
        val matchedLabel = labelsPrefs.getLabelByName(normalized)

        return if (matchedLabel != null && matchedLabel.isSystem) {
            labelsPrefs.getShortDisplayName(matchedLabel)
        } else {
            if (normalized.length <= 5) normalized else normalized.take(5)
        }
    }

    private fun DayOfWeek.toMondayBasedIndex(): Int {
        return when (this) {
            DayOfWeek.MONDAY -> 0
            DayOfWeek.TUESDAY -> 1
            DayOfWeek.WEDNESDAY -> 2
            DayOfWeek.THURSDAY -> 3
            DayOfWeek.FRIDAY -> 4
            DayOfWeek.SATURDAY -> 5
            DayOfWeek.SUNDAY -> 6
        }
    }

    private fun showDayDetails(date: LocalDate) {
        val resolver = DefaultScheduleResolver(requireContext())
        val label = shortenPrimaryCycleLabel(resolver.resolve(date).effectiveCycleLabel)

        val dateText = date.format(
            DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.getDefault())
        )

        val isHoliday = HolidayManager.isHoliday(requireContext(), date)

        val dialog = BottomSheetDialog(requireContext())
        val parent = requireActivity().findViewById<ViewGroup>(android.R.id.content)
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.bottom_sheet_day_details, parent, false)

        val title = view.findViewById<TextView>(R.id.dayDetailsTitle)
        val cycleLabel = view.findViewById<TextView>(R.id.dayDetailsCycleLabel)
        val holidayCard = view.findViewById<MaterialCardView>(R.id.dayDetailsHolidayCard)
        val closeButton = view.findViewById<MaterialButton>(R.id.dayDetailsCloseButton)

        title.text = dateText
        cycleLabel.text = label
        holidayCard.visibility = if (isHoliday) View.VISIBLE else View.GONE

        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setContentView(view)
        dialog.show()
    }
}