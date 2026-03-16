package com.dante.abworkdaywidget

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.DatePickerDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import android.util.Log


class MainActivity : AppCompatActivity() {

    private lateinit var cycleHeader: View
    private lateinit var rulesHeader: View
    private lateinit var displayHeader: View

    private lateinit var cycleArrow: TextView
    private lateinit var rulesArrow: TextView
    private lateinit var displayArrow: TextView

    private lateinit var widgetPromptContainer: View
    private lateinit var githubLinkText: TextView
    private lateinit var versionText: TextView
    private lateinit var mainScrollView: ScrollView

    private lateinit var cycleSection: View
    private lateinit var rulesSection: View
    private lateinit var displaySection: View

    private lateinit var dateText: TextView
    private lateinit var pickDateButton: Button

    private lateinit var cycleDaysEdit: EditText
    private lateinit var firstCycleDayEdit: EditText

    private lateinit var statusCard: MaterialCardView
    private lateinit var todayStatusText: TextView
    private lateinit var tomorrowStatusText: TextView

    private lateinit var previewRecyclerView: RecyclerView
    private lateinit var previewAdapter: CyclePreviewAdapter

    private lateinit var switchSaturdays: SwitchMaterial
    private lateinit var switchSundays: SwitchMaterial
    private lateinit var switchHolidays: SwitchMaterial

    private lateinit var saveButton: Button
    private lateinit var checkDateButton: Button
    private lateinit var openWidgetsButton: Button

    private lateinit var dateResultText: TextView
    private lateinit var widgetHint: TextView
    private lateinit var prefixEdit: EditText
    private lateinit var settingsButton: ImageButton

    private lateinit var themeClassic: RadioButton
    private lateinit var themePastel: RadioButton
    private lateinit var themeDark: RadioButton

    private lateinit var selectedDate: LocalDate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindViews()
        setupPreviewRecyclerView()
        migrateLegacySettingsIfNeeded()

        updateWidgetHint()
        loadSettings()
        updateTodayStatus()
        updateCyclePreview()

        checkDateButton.setOnClickListener {
            showCheckDatePicker()
        }

        openWidgetsButton.setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
            } catch (_: Exception) {
            }
        }

        pickDateButton.setOnClickListener {
            showDatePicker()
        }

        saveButton.setOnClickListener {
            showSaveConfirmation()
        }

        settingsButton.setOnClickListener {
            if (displaySection.visibility != View.VISIBLE) {
                hideAllSections()
                resetArrows()
                displaySection.visibility = View.VISIBLE
                displayArrow.text = "▲"
            }

            displayHeader.post {
                mainScrollView.smoothScrollTo(0, displayHeader.top)
            }
        }

        githubLinkText.setOnClickListener {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://github.com/andrazpoje/ABWorkdayWidget")
            )
            startActivity(intent)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                v.paddingLeft,
                v.paddingTop,
                v.paddingRight,
                bars.bottom
            )
            insets
        }

        Log.d("LANG_TEST", "locale=" + Locale.getDefault().toString())
        Log.d("LANG_TEST", "resources_locale=" + resources.configuration.locales[0].toLanguageTag())
        Log.d("LANG_TEST", "save_string=" + getString(R.string.save))
        Log.d("LANG_TEST", "check_date_string=" + getString(R.string.check_date))


        versionText.text = "v${BuildConfig.VERSION_NAME}"

        setupSection(cycleHeader, cycleSection, cycleArrow)
        setupSection(rulesHeader, rulesSection, rulesArrow)
        setupSection(displayHeader, displaySection, displayArrow)

        hideAllSections()
        cycleSection.visibility = View.VISIBLE
        cycleArrow.text = "▲"
    }

    private fun bindViews() {
        mainScrollView = findViewById(R.id.main)

        cycleHeader = findViewById(R.id.cycleHeader)
        rulesHeader = findViewById(R.id.rulesHeader)
        displayHeader = findViewById(R.id.displayHeader)

        cycleArrow = findViewById(R.id.cycleArrow)
        rulesArrow = findViewById(R.id.rulesArrow)
        displayArrow = findViewById(R.id.displayArrow)

        widgetPromptContainer = findViewById(R.id.widgetPromptContainer)
        githubLinkText = findViewById(R.id.githubLinkText)
        versionText = findViewById(R.id.versionText)

        cycleSection = findViewById(R.id.cycleSection)
        rulesSection = findViewById(R.id.rulesSection)
        displaySection = findViewById(R.id.displaySection)

        widgetHint = findViewById(R.id.widgetHint)
        dateText = findViewById(R.id.dateText)
        pickDateButton = findViewById(R.id.pickDateButton)

        cycleDaysEdit = findViewById(R.id.cycleDaysEdit)
        firstCycleDayEdit = findViewById(R.id.firstCycleDayEdit)

        switchSaturdays = findViewById(R.id.switchSaturdays)
        switchSundays = findViewById(R.id.switchSundays)
        switchHolidays = findViewById(R.id.switchHolidays)

        saveButton = findViewById(R.id.saveButton)
        checkDateButton = findViewById(R.id.checkDateButton)
        openWidgetsButton = findViewById(R.id.openWidgetsButton)

        dateResultText = findViewById(R.id.dateResultText)
        prefixEdit = findViewById(R.id.prefixEdit)

        statusCard = findViewById(R.id.statusCard)
        todayStatusText = findViewById(R.id.todayStatusText)
        tomorrowStatusText = findViewById(R.id.tomorrowStatusText)

        previewRecyclerView = findViewById(R.id.previewRecyclerView)

        settingsButton = findViewById(R.id.settingsButton)

        themeClassic = findViewById(R.id.themeClassic)
        themePastel = findViewById(R.id.themePastel)
        themeDark = findViewById(R.id.themeDark)
    }

    private fun setupPreviewRecyclerView() {
        previewAdapter = CyclePreviewAdapter()
        previewRecyclerView.layoutManager = LinearLayoutManager(this)
        previewRecyclerView.adapter = previewAdapter
        previewRecyclerView.isNestedScrollingEnabled = false
    }

    private fun migrateLegacySettingsIfNeeded() {
        val cyclePrefs = getSharedPreferences("ab_cycle_prefs", MODE_PRIVATE)
        val hasCycle = cyclePrefs.contains("cycle_days")
        val hasStartDate = cyclePrefs.contains("cycle_start_date")

        if (hasCycle && hasStartDate) return

        val prefs = getSharedPreferences("abprefs", MODE_PRIVATE)

        val year = prefs.getInt("startYear", 2026)
        val month = prefs.getInt("startMonth", 3)
        val day = prefs.getInt("startDay", 2)
        val startIsA = prefs.getBoolean("startIsA", true)

        val labelA = sanitizeLabel(prefs.getString("labelA", "A") ?: "A", "A")
        val labelB = sanitizeLabel(prefs.getString("labelB", "B") ?: "B", "B")

        val selectedLegacyDate = LocalDate.of(year, month, day)
        val cycleStartDate = if (startIsA) {
            selectedLegacyDate
        } else {
            selectedLegacyDate.minusDays(1)
        }

        CycleManager.saveCycle(this, listOf(labelA, labelB))
        CycleManager.saveStartDate(this, cycleStartDate)
    }

    private fun hideAllSections() {
        cycleSection.visibility = View.GONE
        rulesSection.visibility = View.GONE
        displaySection.visibility = View.GONE
    }

    private fun setupSection(header: View, section: View, arrow: TextView) {
        header.setOnClickListener {
            val parent = section.parent as ViewGroup
            TransitionManager.beginDelayedTransition(parent, AutoTransition())

            if (section.visibility == View.VISIBLE) {
                section.visibility = View.GONE
                arrow.text = "▼"
            } else {
                hideAllSections()
                resetArrows()

                section.visibility = View.VISIBLE
                arrow.text = "▲"
            }
        }
    }

    private fun resetArrows() {
        cycleArrow.text = "▼"
        rulesArrow.text = "▼"
        displayArrow.text = "▼"
    }

    private fun showSaveConfirmation() {
        AlertDialog.Builder(this)
            .setTitle(R.string.confirm_changes_title)
            .setMessage(R.string.confirm_changes_message)
            .setPositiveButton(R.string.yes) { _, _ ->
                saveSettings()
                refreshWidget()
                updateTodayStatus()
                updateCyclePreview()
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun showCheckDatePicker() {
        val today = LocalDate.now()

        val dialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val date = LocalDate.of(year, month + 1, dayOfMonth)
                val label = CycleManager.getCycleDayForDate(this, date)
                dateResultText.text = getString(R.string.date_result, label)
            },
            today.year,
            today.monthValue - 1,
            today.dayOfMonth
        )

        dialog.show()
    }

    private fun updateTodayStatus() {
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)

        val todayLabel = CycleManager.getCycleDayForDate(this, today)
        val tomorrowLabel = CycleManager.getCycleDayForDate(this, tomorrow)

        todayStatusText.text = todayLabel
        tomorrowStatusText.text = getString(R.string.tomorrow_status, tomorrowLabel)

        val cycle = CycleManager.loadCycle(this)
        val cardColor = CycleColorHelper.getBackgroundColor(
            context = this,
            label = todayLabel,
            cycle = cycle
        )

        animateStatusCardColor(cardColor)
        todayStatusText.setTextColor(ContextCompat.getColor(this, android.R.color.white))
        tomorrowStatusText.setTextColor(ContextCompat.getColor(this, android.R.color.white))
    }

    private fun updateCyclePreview() {
        val today = LocalDate.now()
        val dayFormatter = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
        val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(Locale.getDefault())

        val items = (0 until 7).map { offset ->
            val date = today.plusDays(offset.toLong())
            val title = when (offset) {
                0 -> "Danes"
                1 -> "Jutri"
                else -> date.format(dayFormatter).replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                }
            }

            CyclePreviewAdapter.PreviewItem(
                title = title,
                dateText = date.format(dateFormatter),
                cycleLabel = CycleManager.getCycleDayForDate(this, date)
            )
        }

        val cycle = CycleManager.loadCycle(this)
        previewAdapter.submitList(items, cycle)
    }

    private fun updateWidgetHint() {
        val manager = AppWidgetManager.getInstance(this)
        val ids = manager.getAppWidgetIds(
            ComponentName(this, ABWidgetProvider::class.java)
        )

        if (ids.isNotEmpty()) {
            widgetPromptContainer.visibility = View.GONE
        } else {
            widgetPromptContainer.visibility = View.VISIBLE
            widgetHint.text = getString(R.string.widget_not_added_short)
        }
    }

    private fun refreshWidget() {
        val manager = AppWidgetManager.getInstance(this)
        val ids = manager.getAppWidgetIds(
            ComponentName(this, ABWidgetProvider::class.java)
        )

        ABWidgetProvider().onUpdate(this, manager, ids)
        updateWidgetHint()
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences("abprefs", MODE_PRIVATE)

        val cycle = CycleManager.loadCycle(this)
        val cycleStartDate = CycleManager.loadStartDate(this)

        selectedDate = cycleStartDate

        cycleDaysEdit.setText(cycle.joinToString(","))
        firstCycleDayEdit.setText(cycle.firstOrNull() ?: "A")

        switchSaturdays.isChecked = prefs.getBoolean("skipSaturdays", true)
        switchSundays.isChecked = prefs.getBoolean("skipSundays", true)
        switchHolidays.isChecked = prefs.getBoolean("skipHolidays", true)

        prefixEdit.setText(prefs.getString("prefixText", "") ?: "")

        when (CycleThemeManager.loadTheme(this)) {
            CycleThemeManager.THEME_PASTEL -> themePastel.isChecked = true
            CycleThemeManager.THEME_DARK -> themeDark.isChecked = true
            else -> themeClassic.isChecked = true
        }

        updateDateText()
    }

    private fun saveSettings() {
        val prefs = getSharedPreferences("abprefs", MODE_PRIVATE)

        val cycle = parseCycleInput(cycleDaysEdit.text.toString())
        val selectedFirstDay = sanitizeLabel(
            firstCycleDayEdit.text.toString(),
            cycle.firstOrNull() ?: "A"
        )

        val normalizedCycle = ensureFirstDayAtStart(cycle, selectedFirstDay)

        CycleManager.saveCycle(this, normalizedCycle)
        CycleManager.saveStartDate(this, selectedDate)

        when {
            themePastel.isChecked ->
                CycleThemeManager.saveTheme(this, CycleThemeManager.THEME_PASTEL)

            themeDark.isChecked ->
                CycleThemeManager.saveTheme(this, CycleThemeManager.THEME_DARK)

            else ->
                CycleThemeManager.saveTheme(this, CycleThemeManager.THEME_CLASSIC)
        }

        firstCycleDayEdit.setText(normalizedCycle.firstOrNull() ?: "A")
        cycleDaysEdit.setText(normalizedCycle.joinToString(","))

        prefs.edit()
            .putInt("startYear", selectedDate.year)
            .putInt("startMonth", selectedDate.monthValue)
            .putInt("startDay", selectedDate.dayOfMonth)
            .putString("prefixText", prefixEdit.text.toString())
            .putBoolean("skipSaturdays", switchSaturdays.isChecked)
            .putBoolean("skipSundays", switchSundays.isChecked)
            .putBoolean("skipHolidays", switchHolidays.isChecked)
            .apply()
    }

    private fun parseCycleInput(input: String): List<String> {
        val parsed = input
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        return if (parsed.isEmpty()) listOf("A", "B") else parsed
    }

    private fun ensureFirstDayAtStart(cycle: List<String>, firstDay: String): List<String> {
        val index = cycle.indexOf(firstDay)
        if (index <= 0) return cycle
        if (index == -1) return cycle

        val firstPart = cycle.subList(index, cycle.size)
        val secondPart = cycle.subList(0, index)

        return firstPart + secondPart
    }

    private fun sanitizeLabel(text: String, fallback: String): String {
        val cleaned = text.trim()
        return if (cleaned.isEmpty()) fallback else cleaned
    }

    private fun showDatePicker() {
        val dialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                updateDateText()
            },
            selectedDate.year,
            selectedDate.monthValue - 1,
            selectedDate.dayOfMonth
        )

        dialog.show()
    }

    private fun updateDateText() {
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(Locale.getDefault())

        dateText.text = getString(
            R.string.start_date,
            selectedDate.format(formatter)
        )
    }

    private fun animateStatusCardColor(toColor: Int) {
        val fromColor = statusCard.cardBackgroundColor.defaultColor

        ValueAnimator.ofObject(ArgbEvaluator(), fromColor, toColor).apply {
            duration = 220
            addUpdateListener { animator ->
                val animatedColor = animator.animatedValue as Int
                statusCard.setCardBackgroundColor(animatedColor)
            }
            start()
        }
    }
}