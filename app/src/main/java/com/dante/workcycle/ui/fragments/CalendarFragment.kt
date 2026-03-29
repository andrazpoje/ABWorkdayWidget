package com.dante.workcycle.ui.fragments

import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import android.annotation.SuppressLint
import android.graphics.Typeface
import android.view.Gravity
import com.dante.workcycle.ui.adapter.CalendarAdapter
import com.dante.workcycle.CalendarDayItem
import com.dante.workcycle.CycleColorHelper
import com.dante.workcycle.CycleManager
import com.dante.workcycle.HolidayManager
import com.dante.workcycle.R
import com.dante.workcycle.applySystemBarsBottomInsetAsPadding
import com.dante.workcycle.applySystemBarsHorizontalInsetAsPadding

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

        val items = buildMonth(monthDate)

        recycler.adapter = CalendarAdapter(
            items = items,
            getLabel = { date ->
                CycleManager.getCycleDayForDate(requireContext(), date)
            },
            getBackgroundColor = { _, label ->
                val cycle = CycleManager.loadCycle(requireContext())
                CycleColorHelper.getBackgroundColor(
                    context = requireContext(),
                    label = label,
                    cycle = cycle
                )
            },
            onDayClick = { date ->
                showDayDetails(date)
            }
        )
    }

    private fun buildMonth(date: LocalDate): List<CalendarDayItem> {
        val start = date.withDayOfMonth(1)
        val end = date.withDayOfMonth(date.lengthOfMonth())

        val result = mutableListOf<CalendarDayItem>()

        val leadingEmptyDays = start.dayOfWeek.toMondayBasedIndex()
        repeat(leadingEmptyDays) {
            result.add(CalendarDayItem(null))
        }

        var current = start
        while (!current.isAfter(end)) {
            result.add(CalendarDayItem(current))
            current = current.plusDays(1)
        }

        while (result.size % 7 != 0) {
            result.add(CalendarDayItem(null))
        }

        return result
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
        val label = CycleManager.getCycleDayForDate(requireContext(), date)

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