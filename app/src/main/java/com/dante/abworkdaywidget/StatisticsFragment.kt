package com.dante.abworkdaywidget

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment

class StatisticsFragment : Fragment(R.layout.fragment_statistics) {

    private lateinit var statisticsContentContainer: View
    private lateinit var textMonthSummary: TextView
    private lateinit var textMonthTotal: TextView
    private lateinit var textYearSummary: TextView
    private lateinit var textYearTotal: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)

        view.findViewById<View>(R.id.statisticsScrollView).applySystemBarsBottomInsetAsPadding()
        statisticsContentContainer.applySystemBarsHorizontalInsetAsPadding()

        bindStatistics()
    }

    override fun onResume() {
        super.onResume()
        bindStatistics()
    }

    private fun bindViews(root: View) {
        statisticsContentContainer = root.findViewById(R.id.statisticsContentContainer)
        textMonthSummary = root.findViewById(R.id.textMonthSummary)
        textMonthTotal = root.findViewById(R.id.textMonthTotal)
        textYearSummary = root.findViewById(R.id.textYearSummary)
        textYearTotal = root.findViewById(R.id.textYearTotal)
    }

    private fun bindStatistics() {
        val monthSummary = StatisticsCalculator.buildCurrentMonthSummary(requireContext())
        val yearSummary = StatisticsCalculator.buildCurrentYearSummary(requireContext())

        textMonthSummary.text = StatisticsCalculator.formatSummaryLines(monthSummary)
        textMonthTotal.text = getString(R.string.statistics_total_days, monthSummary.totalDays)

        textYearSummary.text = StatisticsCalculator.formatSummaryLines(yearSummary)
        textYearTotal.text = getString(R.string.statistics_total_days, yearSummary.totalDays)
    }
}