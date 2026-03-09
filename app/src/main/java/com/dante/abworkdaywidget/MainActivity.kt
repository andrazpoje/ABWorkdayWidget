package com.dante.abworkdaywidget

import android.app.DatePickerDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {

    private lateinit var dateText: TextView
    private lateinit var pickDateButton: Button
    private lateinit var radioA: RadioButton
    private lateinit var radioB: RadioButton
    private lateinit var switchWeekends: Switch
    private lateinit var switchHolidays: Switch
    private lateinit var saveButton: Button
    private lateinit var refreshButton: Button

    private lateinit var widgetHint: TextView
    private lateinit var openWidgetsButton: Button
    private lateinit var prefixEdit: EditText
    private lateinit var shiftText: TextView
    private lateinit var shiftPlus: Button
    private lateinit var shiftMinus: Button

    private lateinit var selectedDate: LocalDate

    private var shift: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        widgetHint = findViewById(R.id.widgetHint)
        dateText = findViewById(R.id.dateText)
        pickDateButton = findViewById(R.id.pickDateButton)
        radioA = findViewById(R.id.radioA)
        radioB = findViewById(R.id.radioB)
        switchWeekends = findViewById(R.id.switchWeekends)
        switchHolidays = findViewById(R.id.switchHolidays)
        saveButton = findViewById(R.id.saveButton)
        refreshButton = findViewById(R.id.refreshButton)

        prefixEdit = findViewById(R.id.prefixEdit)
        shiftText = findViewById(R.id.shiftText)
        shiftPlus = findViewById(R.id.shiftPlus)
        shiftMinus = findViewById(R.id.shiftMinus)
        openWidgetsButton = findViewById(R.id.openWidgetsButton)

        updateWidgetHint()
        loadSettings()

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
            saveSettings()
            refreshWidget()
        }

        refreshButton.setOnClickListener {
            refreshWidget()
        }

        shiftPlus.setOnClickListener {
            shift++
            shiftText.text = "Zamik cikla: $shift"
            getSharedPreferences("abprefs", MODE_PRIVATE)
                .edit()
                .putInt("cycleShift", shift)
                .apply()
        }

        shiftMinus.setOnClickListener {
            shift--
            shiftText.text = "Zamik cikla: $shift"
            getSharedPreferences("abprefs", MODE_PRIVATE)
                .edit()
                .putInt("cycleShift", shift)
                .apply()
        }
    }

    private fun updateWidgetHint() {
        val manager = AppWidgetManager.getInstance(this)
        val ids = manager.getAppWidgetIds(
            ComponentName(this, ABWidgetProvider::class.java)
        )

        widgetHint.text =
            if (ids.isNotEmpty()) {
                "Widget je dodan na začetni zaslon."
            } else {
                "Dodajte widget na začetni zaslon (Home screen → Widgets → AB Workday Widget)."
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
        val skipWeekends = prefs.getBoolean("skipWeekends", true)
        val skipHolidays = prefs.getBoolean("skipHolidays", true)

        shift = prefs.getInt("cycleShift", 0)

        radioA.isChecked = startIsA
        radioB.isChecked = !startIsA
        switchWeekends.isChecked = skipWeekends
        switchHolidays.isChecked = skipHolidays
        prefixEdit.setText(prefs.getString("prefixText", "") ?: "")

        shiftText.text = "Zamik cikla: $shift"
        updateDateText()
    }

    private fun saveSettings() {
        val prefs = getSharedPreferences("abprefs", MODE_PRIVATE)

        prefs.edit()
            .putInt("startYear", selectedDate.year)
            .putInt("startMonth", selectedDate.monthValue)
            .putInt("startDay", selectedDate.dayOfMonth)
            .putBoolean("startIsA", radioA.isChecked)
            .putBoolean("skipWeekends", switchWeekends.isChecked)
            .putBoolean("skipHolidays", switchHolidays.isChecked)
            .putString("prefixText", prefixEdit.text.toString())
            .putInt("cycleShift", shift)
            .apply()
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
        val formatter = DateTimeFormatter.ofPattern("d.M.yyyy")
        dateText.text = "Začetni datum: ${selectedDate.format(formatter)}"
    }
}