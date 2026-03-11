package com.dante.abworkdaywidget

import android.app.DatePickerDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.ViewGroup


class MainActivity : AppCompatActivity() {

    private lateinit var cycleHeader: View
    private lateinit var rulesHeader: View
    private lateinit var displayHeader: View

    private lateinit var widgetHeader: View

    private lateinit var cycleArrow: TextView
    private lateinit var rulesArrow: TextView
    private lateinit var displayArrow: TextView

    private lateinit var widgetPromptContainer: View
    private lateinit var githubLinkText: TextView
    private lateinit var versionText: TextView

    private lateinit var cycleSection: View
    private lateinit var rulesSection: View
    private lateinit var displaySection: View
    private lateinit var widgetSection: View

    private lateinit var dateText: TextView
    private lateinit var pickDateButton: Button
    private lateinit var radioA: RadioButton
    private lateinit var radioB: RadioButton

    private lateinit var statusCard: MaterialCardView
    private lateinit var todayStatusText: TextView
    private lateinit var tomorrowStatusText: TextView

    private lateinit var switchSaturdays: SwitchMaterial
    private lateinit var switchSundays: SwitchMaterial
    private lateinit var switchHolidays: SwitchMaterial

    private lateinit var saveButton: Button
    private lateinit var checkDateButton: Button
    private lateinit var openWidgetsButton: Button

    private lateinit var dateResultText: TextView
    private lateinit var widgetHint: TextView
    private lateinit var prefixEdit: EditText
    private lateinit var shiftText: TextView
    private lateinit var shiftPlus: Button
    private lateinit var shiftMinus: Button

    private lateinit var labelAEdit: EditText
    private lateinit var labelBEdit: EditText
    private lateinit var labelXEdit: EditText

    private lateinit var selectedDate: LocalDate

    private var shift: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindViews()

        updateWidgetHint()
        loadSettings()
        updateStartDayLabels()
        updateTodayStatus()

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

        shiftPlus.setOnClickListener {
            shift++
            saveShiftAndRefresh()
        }

        shiftMinus.setOnClickListener {
            shift--
            saveShiftAndRefresh()
        }

        githubLinkText.setOnClickListener {
            val intent = Intent(
                Intent.ACTION_VIEW,
                android.net.Uri.parse("https://github.com/andrazpoje/ABWorkdayWidget")
            )
            startActivity(intent)
        }

        versionText.text = "v${BuildConfig.VERSION_NAME}"

        setupSection(cycleHeader, cycleSection, cycleArrow)
        setupSection(rulesHeader, rulesSection, rulesArrow)
        setupSection(displayHeader, displaySection, displayArrow)

        hideAllSections()
        cycleSection.visibility = View.VISIBLE
        cycleArrow.text = "▲"
    }

    private fun bindViews() {
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
        radioA = findViewById(R.id.radioA)
        radioB = findViewById(R.id.radioB)

        switchSaturdays = findViewById(R.id.switchSaturdays)
        switchSundays = findViewById(R.id.switchSundays)
        switchHolidays = findViewById(R.id.switchHolidays)

        saveButton = findViewById(R.id.saveButton)
        checkDateButton = findViewById(R.id.checkDateButton)
        openWidgetsButton = findViewById(R.id.openWidgetsButton)

        dateResultText = findViewById(R.id.dateResultText)
        prefixEdit = findViewById(R.id.prefixEdit)
        shiftText = findViewById(R.id.shiftText)
        shiftPlus = findViewById(R.id.shiftPlus)
        shiftMinus = findViewById(R.id.shiftMinus)

        statusCard = findViewById(R.id.statusCard)
        todayStatusText = findViewById(R.id.todayStatusText)
        tomorrowStatusText = findViewById(R.id.tomorrowStatusText)

        labelAEdit = findViewById(R.id.labelAEdit)
        labelBEdit = findViewById(R.id.labelBEdit)
        labelXEdit = findViewById(R.id.labelXEdit)
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
                updateStartDayLabels()
                refreshWidget()
                updateTodayStatus()
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
                val raw = ABLogic.getLetterForDate(this, date)
                val label = getDisplayLabel(raw)

                dateResultText.text = getString(R.string.date_result, label)
            },
            today.year,
            today.monthValue - 1,
            today.dayOfMonth
        )

        dialog.show()
    }

    private fun updateStartDayLabels() {
        val labelA = getDisplayLabel("A")
        val labelB = getDisplayLabel("B")

        radioA.text = getString(R.string.start_day_prefix, labelA)
        radioB.text = getString(R.string.start_day_prefix, labelB)
    }

    private fun updateTodayStatus() {
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)

        val todayRaw = ABLogic.getLetterForDate(this, today)
        val tomorrowRaw = ABLogic.getLetterForDate(this, tomorrow)

        val todayLabel = getDisplayLabel(todayRaw)
        val tomorrowLabel = getDisplayLabel(tomorrowRaw)

        todayStatusText.text = getString(R.string.today_status, todayLabel)
        tomorrowStatusText.text = getString(R.string.tomorrow_status, tomorrowLabel)

        val cardColor = when (todayRaw) {
            "A" -> ContextCompat.getColor(this, R.color.shiftA)
            "B" -> ContextCompat.getColor(this, R.color.shiftB)
            else -> ContextCompat.getColor(this, R.color.shiftOff)
        }

        statusCard.setCardBackgroundColor(cardColor)
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

        val year = prefs.getInt("startYear", 2026)
        val month = prefs.getInt("startMonth", 3)
        val day = prefs.getInt("startDay", 2)

        selectedDate = LocalDate.of(year, month, day)

        val startIsA = prefs.getBoolean("startIsA", true)
        val skipSaturdays = prefs.getBoolean("skipSaturdays", true)
        val skipSundays = prefs.getBoolean("skipSundays", true)
        val skipHolidays = prefs.getBoolean("skipHolidays", true)

        shift = prefs.getInt("cycleShift", 0)

        radioA.isChecked = startIsA
        radioB.isChecked = !startIsA
        switchSaturdays.isChecked = skipSaturdays
        switchSundays.isChecked = skipSundays
        switchHolidays.isChecked = skipHolidays

        prefixEdit.setText(prefs.getString("prefixText", "") ?: "")
        labelAEdit.setText(prefs.getString("labelA", "A"))
        labelBEdit.setText(prefs.getString("labelB", "B"))
        labelXEdit.setText(prefs.getString("labelX", "X"))

        shiftText.text = getString(R.string.cycle_shift, shift)
        updateDateText()
    }

    private fun saveSettings() {
        val prefs = getSharedPreferences("abprefs", MODE_PRIVATE)

        prefs.edit()
            .putInt("startYear", selectedDate.year)
            .putInt("startMonth", selectedDate.monthValue)
            .putInt("startDay", selectedDate.dayOfMonth)
            .putBoolean("startIsA", radioA.isChecked)
            .putBoolean("skipSaturdays", switchSaturdays.isChecked)
            .putBoolean("skipSundays", switchSundays.isChecked)
            .putBoolean("skipHolidays", switchHolidays.isChecked)
            .putString("prefixText", prefixEdit.text.toString())
            .putInt("cycleShift", shift)
            .putString("labelA", labelAEdit.text.toString())
            .putString("labelB", labelBEdit.text.toString())
            .putString("labelX", labelXEdit.text.toString())
            .apply()
    }

    private fun getDisplayLabel(raw: String): String {
        val prefs = getSharedPreferences("abprefs", MODE_PRIVATE)

        val labelA = prefs.getString("labelA", "A") ?: "A"
        val labelB = prefs.getString("labelB", "B") ?: "B"
        val labelX = prefs.getString("labelX", "X") ?: "X"

        return when (raw) {
            "A" -> labelA
            "B" -> labelB
            else -> labelX
        }
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

    private fun saveShiftAndRefresh() {
        shiftText.text = getString(R.string.cycle_shift, shift)

        getSharedPreferences("abprefs", MODE_PRIVATE)
            .edit()
            .putInt("cycleShift", shift)
            .apply()

        updateTodayStatus()
    }
}