package com.dante.abworkdaywidget

import android.Manifest
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.DatePickerDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
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
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.view.ViewGroup



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
    lateinit var bottomBarsContainer: View

    lateinit var cycleHeader: View
    lateinit var rulesHeader: View
    lateinit var displayHeader: View

    lateinit var cycleDaysInputLayout: TextInputLayout

    lateinit var cycleArrow: ImageView
    lateinit var rulesArrow: ImageView
    lateinit var displayArrow: ImageView

    lateinit var widgetPromptContainer: View
    lateinit var githubLinkText: TextView
    lateinit var versionText: TextView
    lateinit var mainScrollView: NestedScrollView

    lateinit var cycleSection: View
    lateinit var rulesSection: View
    lateinit var displaySection: View

    lateinit var dateText: TextView
    lateinit var pickDateButton: MaterialButton

    lateinit var presetDropdown: MaterialAutoCompleteTextView
    lateinit var applyPresetButton: MaterialButton

    lateinit var cycleDaysEdit: EditText
    lateinit var firstCycleDayDropdown: MaterialAutoCompleteTextView

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

    lateinit var saveButton: MaterialButton
    lateinit var openWidgetsButton: MaterialButton

    lateinit var widgetHint: TextView
    lateinit var prefixEdit: EditText
    lateinit var skippedDayLabelEdit: EditText

    lateinit var themeClassic: RadioButton
    lateinit var themePastel: RadioButton
    lateinit var themeDark: RadioButton

    lateinit var appThemeSystem: RadioButton
    lateinit var appThemeLight: RadioButton
    lateinit var appThemeDark: RadioButton

    var selectedDate: LocalDate = LocalDate.now()
    lateinit var supportedCountries: List<HolidayCountry>

    override fun onCreate(savedInstanceState: Bundle?) {
        AppThemeManager.applyFromPreferences(this)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        bindViews()
        applyTopInsetToScrollView()

        setupFirstCycleDayDropdown()
        setupPresetDropdown()
        setupPreviewRecyclerView()
        setupHolidayCountryDropdown()
        migrateLegacySettingsIfNeeded()
        loadSettings()
        setupWidgetStyleSettings()
        setupNotificationSettings()
        setupChangeListeners()
        updateTodayStatus()
        updateCyclePreview()
        updateWidgetHint()

        versionText.text = "v${BuildConfig.VERSION_NAME}"

        setupSection(cycleHeader, cycleSection, cycleArrow, SECTION_CYCLE)
        setupSection(rulesHeader, rulesSection, rulesArrow, SECTION_RULES)
        setupSection(displayHeader, displaySection, displayArrow, SECTION_DISPLAY)

        restoreLastOpenSection()
        clearUnsavedChanges()

        updateSystemBarIconContrast(activityRoot)
        setupBottomNavigation(R.id.nav_home)

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

            if (!validateCycleInput()) {
                Toast.makeText(this, getString(R.string.fix_errors), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveChangesAndRefresh()
        }

        applyPresetButton.setOnClickListener {
            val selectedName = presetDropdown.text?.toString()?.trim().orEmpty()
            val preset = CyclePresetProvider.findByDisplayName(this, selectedName)
                ?: return@setOnClickListener

            if (!wouldPresetChangeCurrentState(preset)) {
                Toast.makeText(
                    this,
                    getString(R.string.preset_already_applied),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val currentCycleText = cycleDaysEdit.text?.toString()?.trim().orEmpty()
            val currentFirstDayText = firstCycleDayDropdown.text?.toString()?.trim().orEmpty()
            val hasMeaningfulInput = currentCycleText.isNotBlank() || currentFirstDayText.isNotBlank()

            if (hasMeaningfulInput) {
                showApplyPresetDialog(preset) {
                    applyPreset(preset)
                }
            } else {
                applyPreset(preset)
            }
        }

        githubLinkText.setOnClickListener {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://github.com/andrazpoje/ABWorkdayWidget")
            )
            startActivity(intent)
        }

        setupBackHandling()
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()


    private fun applyTopStatusBarInset() {
        val mainContentContainer = findViewById<View>(R.id.mainContentContainer)

        val initialLeft = mainContentContainer.paddingLeft
        val initialTop = mainContentContainer.paddingTop
        val initialRight = mainContentContainer.paddingRight
        val initialBottom = mainContentContainer.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(mainContentContainer) { view, insets ->
            val topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top

            view.setPadding(
                initialLeft,
                initialTop + topInset,
                initialRight,
                initialBottom
            )

            insets
        }

        ViewCompat.requestApplyInsets(mainContentContainer)
    }

    fun bindViews() {
        activityRoot = findViewById(R.id.activityRoot)
        saveBarContainer = findViewById(R.id.saveBarContainer)
        bottomBarsContainer = findViewById(R.id.bottomBarsContainer)
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

        presetDropdown = findViewById(R.id.presetDropdown)
        applyPresetButton = findViewById(R.id.applyPresetButton)

        cycleDaysInputLayout = findViewById(R.id.cycleDaysInputLayout)
        cycleDaysEdit = findViewById(R.id.cycleDaysEdit)
        firstCycleDayDropdown = findViewById(R.id.firstCycleDayDropdown)

        switchSaturdays = findViewById(R.id.switchSaturdays)
        switchSundays = findViewById(R.id.switchSundays)
        switchHolidays = findViewById(R.id.switchHolidays)
        switchOverrideSkippedDays = findViewById(R.id.switchOverrideSkippedDays)

        holidayCountryDropdown = findViewById(R.id.holidayCountryDropdown)

        saveButton = findViewById(R.id.saveButton)
        openWidgetsButton = findViewById(R.id.openWidgetsButton)
        prefixEdit = findViewById(R.id.prefixEdit)
        skippedDayLabelEdit = findViewById(R.id.skippedDayLabelEdit)

        statusCard = findViewById(R.id.statusCard)
        todayStatusText = findViewById(R.id.todayStatusText)
        tomorrowStatusText = findViewById(R.id.tomorrowStatusText)

        previewRecyclerView = findViewById(R.id.previewRecyclerView)

        themeClassic = findViewById(R.id.themeClassic)
        themePastel = findViewById(R.id.themePastel)
        themeDark = findViewById(R.id.themeDark)

        appThemeSystem = findViewById(R.id.appThemeSystem)
        appThemeLight = findViewById(R.id.appThemeLight)
        appThemeDark = findViewById(R.id.appThemeDark)
    }

    private fun applyTopInsetToScrollView() {
        val scrollView = findViewById<NestedScrollView>(R.id.main)

        ViewCompat.setOnApplyWindowInsetsListener(activityRoot) { _, insets ->
            val topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top

            val lp = scrollView.layoutParams as ViewGroup.MarginLayoutParams
            if (lp.topMargin != topInset) {
                lp.topMargin = topInset
                scrollView.layoutParams = lp
            }

            insets
        }

        ViewCompat.requestApplyInsets(activityRoot)
    }

    fun validateCycleInput(): Boolean {
        val raw = cycleDaysEdit.text?.toString().orEmpty().trim()
        val parts = raw.split(",").map { it.trim() }

        if (raw.isBlank() || parts.all { it.isEmpty() }) {
            cycleDaysInputLayout.error = getString(R.string.error_cycle_empty)
            return false
        }

        if (parts.any { it.isEmpty() }) {
            cycleDaysInputLayout.error = getString(R.string.error_cycle_empty_item)
            return false
        }

        if (parts.size > MAX_CYCLE_ITEMS) {
            cycleDaysInputLayout.error = getString(
                R.string.error_cycle_too_many,
                parts.size,
                MAX_CYCLE_ITEMS
            )
            return false
        }

        if (parts.any { it.length > MAX_LABEL_LENGTH }) {
            cycleDaysInputLayout.error = getString(
                R.string.error_label_too_long,
                MAX_LABEL_LENGTH
            )
            return false
        }

        val distinct = parts.map { it.lowercase(Locale.getDefault()) }.toSet()
        if (distinct.size != parts.size) {
            cycleDaysInputLayout.error = getString(R.string.error_cycle_duplicates)
            return false
        }

        cycleDaysInputLayout.error = null
        return true
    }

    fun setupPreviewRecyclerView() {
        previewAdapter = CyclePreviewAdapter()
        previewRecyclerView.layoutManager = LinearLayoutManager(this)
        previewRecyclerView.adapter = previewAdapter
        previewRecyclerView.isNestedScrollingEnabled = false
    }

    fun buildCountryDisplayList(
        detectedCode: String,
        isManual: Boolean
    ): List<String> {
        return supportedCountries.map {
            if (!isManual && it.code == detectedCode) {
                "${it.displayName} (auto-detected)"
            } else {
                it.displayName
            }
        }
    }

    fun parseCycleLabels(raw: String): List<String> {
        return raw.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapIndexed { index, label ->
                sanitizeLabel(label, "Day ${index + 1}").take(MAX_LABEL_LENGTH)
            }
            .take(MAX_CYCLE_ITEMS)
    }

    fun getCurrentCycleLabelsFromInput(): List<String> {
        return parseCycleLabels(cycleDaysEdit.text?.toString().orEmpty())
    }

    fun getCurrentCycleInputState(): Pair<List<String>, String> {
        val currentCycle = parseCycleLabels(cycleDaysEdit.text?.toString().orEmpty())
        val currentFirstDay = firstCycleDayDropdown.text?.toString()?.trim().orEmpty()
        return currentCycle to currentFirstDay
    }

    fun wouldPresetChangeCurrentState(preset: CyclePreset): Boolean {
        val (currentCycle, currentFirstDay) = getCurrentCycleInputState()

        val normalizedPresetCycle = preset.cycleDaysProvider(this).map {
            sanitizeLabel(it, "").take(MAX_LABEL_LENGTH)
        }

        val normalizedPresetFirstDay = sanitizeLabel(
            preset.defaultFirstDayProvider(this),
            normalizedPresetCycle.firstOrNull() ?: ""
        ).take(MAX_LABEL_LENGTH)

        return currentCycle != normalizedPresetCycle || currentFirstDay != normalizedPresetFirstDay
    }

    fun applyPreset(preset: CyclePreset) {
        val labels = preset.cycleDaysProvider(this)
        val firstDay = preset.defaultFirstDayProvider(this)

        cycleDaysEdit.setText(labels.joinToString(", "))
        refreshFirstCycleDayDropdown(firstDay)
        updatePresetSelectionState()
        clearDateCheckResult()
        validateCycleInput()
        markUnsavedChanges()

        Toast.makeText(
            this,
            getString(R.string.preset_applied, getString(preset.nameRes)),
            Toast.LENGTH_SHORT
        ).show()
    }

    fun showApplyPresetDialog(
        preset: CyclePreset,
        onConfirm: () -> Unit
    ) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(R.string.apply_preset_title)
            .setMessage(
                getString(
                    R.string.apply_preset_message,
                    getString(preset.nameRes)
                )
            )
            .setPositiveButton(R.string.apply_preset) { _: android.content.DialogInterface, _: Int ->
                onConfirm()
            }
            .setNegativeButton(R.string.cancel) { dialog: android.content.DialogInterface, _: Int ->
                dialog.dismiss()
            }
            .show()
    }

    fun refreshFirstCycleDayDropdown(preferredValue: String? = null) {
        val cycleLabels = getCurrentCycleLabelsFromInput()

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            cycleLabels
        )

        firstCycleDayDropdown.setAdapter(adapter)

        if (cycleLabels.isEmpty()) {
            firstCycleDayDropdown.setText("", false)
            firstCycleDayDropdown.isEnabled = false
            return
        }

        firstCycleDayDropdown.isEnabled = true

        val currentValue = preferredValue
            ?: firstCycleDayDropdown.text?.toString()?.trim().orEmpty()

        val finalValue = when {
            currentValue in cycleLabels -> currentValue
            else -> cycleLabels.first()
        }

        firstCycleDayDropdown.setText(finalValue, false)
    }

    fun setupFirstCycleDayDropdown() {
        firstCycleDayDropdown.keyListener = null
        firstCycleDayDropdown.setOnClickListener {
            if (firstCycleDayDropdown.adapter != null && firstCycleDayDropdown.adapter.count > 0) {
                firstCycleDayDropdown.showDropDown()
            }
        }

        firstCycleDayDropdown.setOnItemClickListener { _, _, _, _ ->
            clearDateCheckResult()
            updatePresetSelectionState(markAsChanged = true)
        }
    }

    fun setupPresetDropdown() {
        val presets = CyclePresetProvider.getPresets()
        val names = presets.map { getString(it.nameRes) } + getString(R.string.preset_custom)

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            names
        )

        presetDropdown.setAdapter(adapter)
        presetDropdown.keyListener = null

        presetDropdown.setOnClickListener {
            presetDropdown.showDropDown()
        }

        presetDropdown.setOnItemClickListener { _, _, _, _ ->
            val selectedName = presetDropdown.text?.toString()?.trim().orEmpty()
            val isCustom = selectedName == getString(R.string.preset_custom)
            applyPresetButton.isEnabled = !isCustom
            applyPresetButton.alpha = if (isCustom) 0.5f else 1f
        }
    }

    fun updatePresetSelectionState(markAsChanged: Boolean = false) {
        val matchedPreset = CyclePresetProvider.getPresets().firstOrNull { preset ->
            !wouldPresetChangeCurrentState(preset)
        }

        val presetText = matchedPreset
            ?.let { getString(it.nameRes) }
            ?: getString(R.string.preset_custom)

        if (presetDropdown.text?.toString() != presetText) {
            presetDropdown.setText(presetText, false)
        }

        applyPresetButton.isEnabled = matchedPreset == null
        applyPresetButton.alpha = if (matchedPreset == null) 1f else 0.5f

        if (markAsChanged) {
            markUnsavedChanges()
        }
    }

    fun setupHolidayCountryDropdown() {
        supportedCountries = HolidayManager.supportedCountries

        val prefs = getSharedPreferences("abprefs", Context.MODE_PRIVATE)
        val detectedCode = HolidayManager.getSelectedCountry(this)
        val isManual = prefs.getBoolean(HolidayManager.KEY_COUNTRY_MANUAL, false)

        val displayItems = buildCountryDisplayList(detectedCode, isManual)

        val adapter = object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_list_item_1,
            displayItems
        ) {
            override fun getFilter(): android.widget.Filter {
                return object : android.widget.Filter() {
                    override fun performFiltering(constraint: CharSequence?): FilterResults {
                        return FilterResults().apply {
                            values = displayItems
                            count = displayItems.size
                        }
                    }

                    override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                        clear()
                        addAll(displayItems)
                        notifyDataSetChanged()
                    }

                    override fun convertResultToString(resultValue: Any?): CharSequence {
                        return resultValue as String
                    }
                }
            }
        }

        holidayCountryDropdown.setAdapter(adapter)
        holidayCountryDropdown.keyListener = null
        holidayCountryDropdown.setOnClickListener {
            holidayCountryDropdown.showDropDown()
        }
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

        HolidayManager.ensureCountrySelected(this)
    }

    fun updateTodayStatus() {
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)

        todayStatusText.text = formatDayLabel(today)

        tomorrowStatusText.text = getString(
            R.string.tomorrow_status,
            formatDayLabel(tomorrow)
        )

        val todayLabel = CycleManager.getCycleDayForDate(this, today)
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

    private fun formatDayLabel(date: LocalDate): String {
        val locale = Locale.getDefault()

        val dayName = date.dayOfWeek
            .getDisplayName(java.time.format.TextStyle.SHORT, locale)
            .replaceFirstChar { it.titlecase(locale) }

        val cycleLabel = CycleManager.getCycleDayForDate(this, date)
            .trim()
            .ifBlank { "?" }

        return "$dayName $cycleLabel"
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

    fun clearDateCheckResult() = Unit

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

        appThemeSystem.setOnClickListener { markUnsavedChanges() }
        appThemeLight.setOnClickListener { markUnsavedChanges() }
        appThemeDark.setOnClickListener { markUnsavedChanges() }

        prefixEdit.addTextChangedListener {
            markUnsavedChanges()
        }

        cycleDaysEdit.addTextChangedListener {
            clearDateCheckResult()
            refreshFirstCycleDayDropdown()
            validateCycleInput()
            updatePresetSelectionState(markAsChanged = true)
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
            if (saveBarContainer.visibility != View.VISIBLE) {
                saveBarContainer.visibility = View.VISIBLE
                saveBarContainer.alpha = 0f
                saveBarContainer.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start()
            }

            saveButton.isEnabled = true
            saveButton.alpha = 1f
        } else {
            saveButton.isEnabled = false
            saveButton.alpha = 0.6f

            if (saveBarContainer.visibility != View.GONE) {
                saveBarContainer.animate()
                    .alpha(0f)
                    .setDuration(150)
                    .withEndAction {
                        saveBarContainer.visibility = View.GONE
                        saveBarContainer.alpha = 1f
                    }
                    .start()
            }
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