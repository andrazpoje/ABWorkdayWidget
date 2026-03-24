package com.dante.abworkdaywidget

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class CalendarActivity : BaseActivity() {

    override val activityRootView: View
        get() = findViewById(R.id.calendarRoot)

    override val topInsetTargetView: View
        get() = findViewById(R.id.calendarContentContainer)

    override val bottomNavigationView: com.google.android.material.bottomnavigation.BottomNavigationView?
        get() = findViewById(R.id.bottomNavigation)

    override val selectedBottomNavItemId: Int?
        get() = R.id.nav_calendar

    private lateinit var recycler: RecyclerView
    private lateinit var monthTitle: TextView
    private lateinit var previousMonthButton: ImageButton
    private lateinit var nextMonthButton: ImageButton

    private lateinit var displayedMonth: LocalDate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        setupBaseUi()

        recycler = findViewById(R.id.calendarRecycler)
        monthTitle = findViewById(R.id.monthTitle)
        previousMonthButton = findViewById(R.id.previousMonthButton)
        nextMonthButton = findViewById(R.id.nextMonthButton)

        setupWeekHeader()

        recycler.layoutManager = GridLayoutManager(this, 7)

        displayedMonth = LocalDate.now().withDayOfMonth(1)
        renderMonth(displayedMonth)

        previousMonthButton.setOnClickListener {
            displayedMonth = displayedMonth.minusMonths(1).withDayOfMonth(1)
            renderMonth(displayedMonth)
        }

        nextMonthButton.setOnClickListener {
            displayedMonth = displayedMonth.plusMonths(1).withDayOfMonth(1)
            renderMonth(displayedMonth)
        }
    }

    override fun onResume() {
        super.onResume()
        renderMonth(displayedMonth)
    }

    private fun setupWeekHeader() {
        val header = findViewById<LinearLayout>(R.id.weekHeader)
        val days = resources.getStringArray(R.array.week_days_short)

        header.removeAllViews()

        for (day in days) {
            val tv = TextView(this).apply {
                text = day
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
                gravity = android.view.Gravity.CENTER
                textSize = 12f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                alpha = 0.7f
            }
            header.addView(tv)
        }
    }

    private fun renderMonth(monthDate: LocalDate) {
        monthTitle.text =
            monthDate.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                .replaceFirstChar { it.titlecase(Locale.getDefault()) } +
                    " ${monthDate.year}"

        val items = buildMonth(monthDate)

        recycler.adapter = CalendarAdapter(
            items = items,
            getLabel = { date ->
                CycleManager.getCycleDayForDate(this, date)
            },
            getBackgroundColor = { date, label ->
                val cycle = CycleManager.loadCycle(this)
                CycleColorHelper.getBackgroundColor(
                    context = this,
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
        val label = CycleManager.getCycleDayForDate(this, date)

        val dateText = date.format(
            DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.getDefault())
        )

        val isHoliday = HolidayManager.isHoliday(this, date)

        val dialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this)
            .inflate(R.layout.bottom_sheet_day_details, null, false)

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