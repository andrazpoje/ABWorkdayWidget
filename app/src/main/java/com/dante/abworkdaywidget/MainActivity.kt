package com.dante.abworkdaywidget

import android.Manifest
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.DatePickerDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dante.abworkdaywidget.data.Prefs
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class MainActivity : AppCompatActivity() {

    companion object {
        const val PREFS_UI = "ui_prefs"
        const val KEY_LAST_OPEN_SECTION = "last_open_section"

        const val SECTION_CYCLE = "cycle"
        const val SECTION_RULES = "rules"
        const val SECTION_DISPLAY = "display"

        const val REQUEST_NOTIFICATION_PERMISSION = 1001
        const val MAX_CYCLE_ITEMS = 16
        const val MAX_LABEL_LENGTH = 24

        const val KEY_FIRST_CYCLE_DAY = "firstCycleDay"
    }

    var hasUnsavedChanges = false

    lateinit var activityRoot: View
    lateinit var saveBarContainer: View

    lateinit var cycleHeader: View
    lateinit var rulesHeader: View
    lateinit var displayHeader: View

    lateinit var cycleArrow: TextView
    lateinit var rulesArrow: TextView
    lateinit var displayArrow: TextView

    lateinit var widgetPromptContainer: View
    lateinit var githubLinkText: TextView
    lateinit var versionText: TextView
    lateinit var mainScrollView: NestedScrollView

    lateinit var cycleSection: View
    lateinit var rulesSection: View
    lateinit var displaySection: View

    lateinit var dateText: TextView
    lateinit var pickDateButton: Button

    lateinit var cycleDaysEdit: EditText
    lateinit var firstCycleDayEdit: EditText

    lateinit var statusCard: MaterialCardView
    lateinit var todayStatusText: TextView
    lateinit var tomorrowStatusText: TextView

    lateinit var previewRecyclerView: RecyclerView
    lateinit var previewAdapter: CyclePreviewAdapter

    lateinit var switchSaturdays: SwitchMaterial
    lateinit var switchSundays: SwitchMaterial
    lateinit var switchHolidays: SwitchMaterial
    lateinit var switchOverrideSkippedDays: SwitchMaterial

    lateinit var holidayCountryDropdown: MaterialAutoCompleteTextView

    lateinit var saveButton: Button
    lateinit var checkDateButton: Button
    lateinit var openWidgetsButton: Button

    lateinit var dateResultText: TextView
    lateinit var widgetHint: TextView
    lateinit var prefixEdit: EditText
    lateinit var skippedDayLabelEdit: EditText
    lateinit var settingsButton: ImageButton

    lateinit var themeClassic: RadioButton
    lateinit var themePastel: RadioButton
    lateinit var themeDark: RadioButton

    lateinit var selectedDate: LocalDate
    lateinit var supportedCountries: List<HolidayManager.HolidayCountryItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindViews()
        setupPreviewRecyclerView()
        setupHolidayCountryDropdown()
        migrateLegacySettingsIfNeeded()
        applyEdgeToEdgeInsets()
        setupBackHandling()

        updateWidgetHint()
        loadSettings()
        updateTodayStatus()
        updateCyclePreview()
        setupWidgetStyleSettings()
        setupNotificationSettings()
        setupChangeListeners()

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
            if (!hasUnsavedChanges) return@setOnClickListener
            saveChangesAndRefresh()
        }

        settingsButton.setOnClickListener {
            if (displaySection.visibility != View.VISIBLE) {
                hideAllSections()
                resetArrows()
                displaySection.visibility = View.VISIBLE
                displayArrow.text = "▲"
            }
            saveLastOpenSection(SECTION_DISPLAY)

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

        Log.d("LANG_TEST", "locale=${Locale.getDefault()}")
        Log.d("LANG_TEST", "resources_locale=${resources.configuration.locales[0].toLanguageTag()}")
        Log.d("LANG_TEST", "save_string=${getString(R.string.save)}")
        Log.d("LANG_TEST", "check_date_string=${getString(R.string.check_date)}")

        versionText.text = "v${BuildConfig.VERSION_NAME}"

        setupSection(cycleHeader, cycleSection, cycleArrow, SECTION_CYCLE)
        setupSection(rulesHeader, rulesSection, rulesArrow, SECTION_RULES)
        setupSection(displayHeader, displaySection, displayArrow, SECTION_DISPLAY)

        restoreLastOpenSection()
        clearUnsavedChanges()
    }

    fun bindViews() {
        activityRoot = findViewById(R.id.activityRoot)
        saveBarContainer = findViewById(R.id.saveBarContainer)

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
        switchOverrideSkippedDays = findViewById(R.id.switchOverrideSkippedDays)

        holidayCountryDropdown = findViewById(R.id.holidayCountryDropdown)

        saveButton = findViewById(R.id.saveButton)
        checkDateButton = findViewById(R.id.checkDateButton)
        openWidgetsButton = findViewById(R.id.openWidgetsButton)

        dateResultText = findViewById(R.id.dateResultText)
        prefixEdit = findViewById(R.id.prefixEdit)
        skippedDayLabelEdit = findViewById(R.id.skippedDayLabelEdit)

        statusCard = findViewById(R.id.statusCard)
        todayStatusText = findViewById(R.id.todayStatusText)
        tomorrowStatusText = findViewById(R.id.tomorrowStatusText)

        previewRecyclerView = findViewById(R.id.previewRecyclerView)

        settingsButton = findViewById(R.id.settingsButton)

        themeClassic = findViewById(R.id.themeClassic)
        themePastel = findViewById(R.id.themePastel)
        themeDark = findViewById(R.id.themeDark)
    }

    fun setupPreviewRecyclerView() {
        previewAdapter = CyclePreviewAdapter()
        previewRecyclerView.layoutManager = LinearLayoutManager(this)
        previewRecyclerView.adapter = previewAdapter
        previewRecyclerView.isNestedScrollingEnabled = false
    }

    fun setupHolidayCountryDropdown() {
        supportedCountries = HolidayManager.getSupportedCountries(this)

        val displayItems = supportedCountries.map { it.displayName }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            displayItems
        )

        holidayCountryDropdown.setAdapter(adapter)
        holidayCountryDropdown.keyListener = null
    }

    fun migrateLegacySettingsIfNeeded() {
        val cyclePrefs = getSharedPreferences(CycleManager.PREFS_NAME, MODE_PRIVATE)
        val hasCycle = cyclePrefs.contains(CycleManager.KEY_CYCLE_DAYS)
        val hasStartDate = cyclePrefs.contains(CycleManager.KEY_CYCLE_START_DATE)

        val prefs = getSharedPreferences("abprefs", MODE_PRIVATE)

        if (!hasCycle || !hasStartDate) {
            val year = prefs.getInt("startYear", 2026)
            val month = prefs.getInt("startMonth", 3)
            val day = prefs.getInt("startDay", 2)
            val startIsA = prefs.getBoolean("startIsA", true)

            val labelA = sanitizeLabel(prefs.getString("labelA", "A") ?: "A", "A")
            val labelB = sanitizeLabel(prefs.getString("labelB", "B") ?: "B", "B")

            val selectedLegacyDate = LocalDate.of(year, month, day)
            val cycleStartDate = if (startIsA) selectedLegacyDate else selectedLegacyDate.minusDays(1)
            val legacyFirstDay = if (startIsA) labelA else labelB

            CycleManager.saveCycle(this, listOf(labelA, labelB))
            CycleManager.saveStartDate(this, cycleStartDate)

            prefs.edit()
                .putString(KEY_FIRST_CYCLE_DAY, legacyFirstDay)
                .apply()
        }

        if (!prefs.contains("overrideSkippedDays")) {
            prefs.edit().putBoolean("overrideSkippedDays", true).apply()
        }

        if (!prefs.contains(HolidayManager.KEY_HOLIDAY_COUNTRY)) {
            prefs.edit()
                .putString(HolidayManager.KEY_HOLIDAY_COUNTRY, HolidayManager.DEFAULT_COUNTRY)
                .apply()
        }
    }

    fun showCheckDatePicker() {
        val today = LocalDate.now()

        val dialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val date = LocalDate.of(year, month + 1, dayOfMonth)
                val label = CycleManager.getCycleDayForDate(this, date)

                val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                    .withLocale(Locale.getDefault())

                val formattedDate = date.format(formatter)

                dateResultText.text = getString(
                    R.string.date_result_with_date,
                    formattedDate,
                    label
                )
            },
            today.year,
            today.monthValue - 1,
            today.dayOfMonth
        )

        dialog.show()
    }

    fun updateTodayStatus() {
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

    fun updateCyclePreview() {
        val today = LocalDate.now()
        val dayFormatter = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
        val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(Locale.getDefault())

        val items = (2..6).map { offset ->
            val date = today.plusDays(offset.toLong())
            val title = date.format(dayFormatter).replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
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

    fun updateWidgetHint() {
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

    fun refreshWidget() {
        val manager = AppWidgetManager.getInstance(this)
        val ids = manager.getAppWidgetIds(
            ComponentName(this, ABWidgetProvider::class.java)
        )

        ABWidgetProvider().onUpdate(this, manager, ids)
        updateWidgetHint()
    }

    fun showDatePicker() {
        val dialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                updateDateText()
                clearDateCheckResult()
                markUnsavedChanges()
            },
            selectedDate.year,
            selectedDate.monthValue - 1,
            selectedDate.dayOfMonth
        )

        dialog.show()
    }

    fun clearDateCheckResult() {
        dateResultText.text = ""
    }

    fun updateDateText() {
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(Locale.getDefault())

        dateText.text = getString(
            R.string.start_date,
            selectedDate.format(formatter)
        )
    }

    fun animateStatusCardColor(toColor: Int) {
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

    fun setupWidgetStyleSettings() {
        val prefs = getSharedPreferences(Prefs.PREFS_NAME, MODE_PRIVATE)

        val radioClassic = findViewById<RadioButton>(R.id.radioClassic)
        val radioMinimal = findViewById<RadioButton>(R.id.radioMinimal)

        val currentStyle = prefs.getString(
            Prefs.KEY_WIDGET_STYLE,
            Prefs.WIDGET_STYLE_CLASSIC
        ) ?: Prefs.WIDGET_STYLE_CLASSIC

        radioClassic.isChecked = currentStyle == Prefs.WIDGET_STYLE_CLASSIC
        radioMinimal.isChecked = currentStyle == Prefs.WIDGET_STYLE_MINIMAL
    }

    fun setupNotificationSettings() {
        val prefs = getSharedPreferences(Prefs.PREFS_NAME, MODE_PRIVATE)

        val enabledSwitch = findViewById<SwitchMaterial?>(R.id.switchNotificationsEnabled) ?: return
        val silentSwitch = findViewById<SwitchMaterial?>(R.id.switchSilentNotification) ?: return

        val notificationsEnabled = prefs.getBoolean(Prefs.KEY_NOTIFICATIONS_ENABLED, false)
        val silentEnabled = prefs.getBoolean(Prefs.KEY_SILENT_NOTIFICATION, false)

        enabledSwitch.isChecked = notificationsEnabled
        silentSwitch.isChecked = notificationsEnabled && silentEnabled
        silentSwitch.isEnabled = notificationsEnabled

        enabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            silentSwitch.isEnabled = isChecked

            if (!isChecked) {
                silentSwitch.isChecked = false
            } else {
                requestNotificationPermissionIfNeeded()
            }

            markUnsavedChanges()
        }

        silentSwitch.setOnCheckedChangeListener { _, _ ->
            markUnsavedChanges()
        }
    }

    fun setupChangeListeners() {
        switchSaturdays.setOnCheckedChangeListener { _, _ ->
            clearDateCheckResult()
            markUnsavedChanges()
        }

        switchSundays.setOnCheckedChangeListener { _, _ ->
            clearDateCheckResult()
            markUnsavedChanges()
        }

        switchHolidays.setOnCheckedChangeListener { _, _ ->
            clearDateCheckResult()
            markUnsavedChanges()
        }

        switchOverrideSkippedDays.setOnCheckedChangeListener { _, isChecked ->
            skippedDayLabelEdit.isEnabled = isChecked
            skippedDayLabelEdit.alpha = if (isChecked) 1f else 0.5f
            clearDateCheckResult()
            markUnsavedChanges()
        }

        holidayCountryDropdown.setOnItemClickListener { _, _, _, _ ->
            clearDateCheckResult()
            markUnsavedChanges()
        }

        themeClassic.setOnClickListener { markUnsavedChanges() }
        themePastel.setOnClickListener { markUnsavedChanges() }
        themeDark.setOnClickListener { markUnsavedChanges() }

        prefixEdit.addTextChangedListener {
            markUnsavedChanges()
        }

        cycleDaysEdit.addTextChangedListener {
            clearDateCheckResult()
            markUnsavedChanges()
        }

        firstCycleDayEdit.addTextChangedListener {
            clearDateCheckResult()
            markUnsavedChanges()
        }

        skippedDayLabelEdit.addTextChangedListener {
            clearDateCheckResult()
            markUnsavedChanges()
        }

        findViewById<RadioGroup>(R.id.widgetStyleRadioGroup)
            .setOnCheckedChangeListener { _, _ -> markUnsavedChanges() }
    }

    fun animateSaveButtonActivated() {
        saveButton.scaleX = 0.96f
        saveButton.scaleY = 0.96f

        saveButton.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(180)
            .start()
    }

    fun updateSaveButtonVisualState() {
        if (hasUnsavedChanges) {
            saveButton.isEnabled = true
            saveButton.alpha = 1f
        } else {
            saveButton.isEnabled = false
            saveButton.alpha = 0.55f
            saveButton.scaleX = 1f
            saveButton.scaleY = 1f
        }
    }

    fun markUnsavedChanges() {
        if (!hasUnsavedChanges) {
            hasUnsavedChanges = true
            updateSaveButtonVisualState()
            animateSaveButtonActivated()
        }
    }

    fun clearUnsavedChanges() {
        hasUnsavedChanges = false
        updateSaveButtonVisualState()
    }

    fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            REQUEST_NOTIFICATION_PERMISSION
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode != REQUEST_NOTIFICATION_PERMISSION) return

        val granted = grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            val enabledSwitch = findViewById<SwitchMaterial?>(R.id.switchNotificationsEnabled)
            val silentSwitch = findViewById<SwitchMaterial?>(R.id.switchSilentNotification)

            enabledSwitch?.setOnCheckedChangeListener(null)
            silentSwitch?.setOnCheckedChangeListener(null)

            enabledSwitch?.isChecked = false
            silentSwitch?.isChecked = false
            silentSwitch?.isEnabled = false

            setupNotificationSettings()
            markUnsavedChanges()

            Toast.makeText(
                this,
                getString(R.string.notification_permission_denied),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}