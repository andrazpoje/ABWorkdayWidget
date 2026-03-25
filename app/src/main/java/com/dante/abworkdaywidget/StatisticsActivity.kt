package com.dante.abworkdaywidget

import android.os.Bundle
import android.view.View
import android.widget.TextView

class StatisticsActivity : BaseActivity() {

    override val activityRootView: View
        get() = findViewById(R.id.statisticsRoot)

    override val topInsetTargetView: View
        get() = findViewById(R.id.statisticsContentContainer)

    override val bottomNavigationView: com.google.android.material.bottomnavigation.BottomNavigationView?
        get() = findViewById(R.id.bottomNavigation)

    override val selectedBottomNavItemId: Int
        get() = R.id.nav_more

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        setupBaseUi()
        bindStatistics()
    }

    private fun bindStatistics() {
        val monthSummary = StatisticsCalculator.buildCurrentMonthSummary(this)
        val yearSummary = StatisticsCalculator.buildCurrentYearSummary(this)

        findViewById<TextView>(R.id.textMonthSummary).text =
            StatisticsCalculator.formatSummaryLines(monthSummary)

        findViewById<TextView>(R.id.textMonthTotal).text =
            getString(R.string.statistics_total_days, monthSummary.totalDays)

        findViewById<TextView>(R.id.textYearSummary).text =
            StatisticsCalculator.formatSummaryLines(yearSummary)

        findViewById<TextView>(R.id.textYearTotal).text =
            getString(R.string.statistics_total_days, yearSummary.totalDays)
    }
}