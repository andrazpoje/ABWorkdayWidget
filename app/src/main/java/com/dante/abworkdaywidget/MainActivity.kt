package com.dante.abworkdaywidget

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import java.time.LocalDate

class MainActivity : BaseActivity() {

    companion object {
        const val PREFS_UI = "ui_prefs"
        const val KEY_LAST_OPEN_SECTION = "last_open_section"

        const val SECTION_CYCLE = "cycle"
        const val SECTION_RULES = "rules"
        const val SECTION_DISPLAY = "display"

        const val REQUEST_NOTIFICATION_PERMISSION = 1001
        const val MAX_CYCLE_ITEMS = 16
        const val MAX_LABEL_LENGTH = 24
    }

    override val activityRootView: View
        get() = activityRoot

    override val topInsetTargetView: View
        get() = findViewById(R.id.main)

    override val bottomNavigationView: com.google.android.material.bottomnavigation.BottomNavigationView?
        get() = findViewById(R.id.bottomNavigation)

    override val imeInsetTargetView: View
        get() = saveBarContainer

    override val selectedBottomNavItemId: Int
        get() = R.id.nav_home

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
        AppLanguageManager.applySavedLanguage(this)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        bindViews()

        setupBaseUi()

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

        versionText.text = getString(R.string.app_version, BuildConfig.VERSION_NAME)

        setupSection(cycleHeader, cycleSection, cycleArrow, SECTION_CYCLE)
        setupSection(rulesHeader, rulesSection, rulesArrow, SECTION_RULES)
        setupSection(displayHeader, displaySection, displayArrow, SECTION_DISPLAY)

        restoreLastOpenSection()
        clearUnsavedChanges()

        showWhatsNewIfAppUpdated(savedInstanceState)

        openWidgetsButton.setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
            } catch (_: Exception) {
            }
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
                "https://github.com/andrazpoje/ABWorkdayWidget".toUri()
            )
            startActivity(intent)
        }

        setupBackHandling()
    }

    private fun showWhatsNewIfAppUpdated(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) return

        val prefs = getSharedPreferences(AppPrefs.NAME, MODE_PRIVATE)
        val currentVersion = BuildConfig.VERSION_NAME
        val lastSeenVersion = prefs.getString(AppPrefs.KEY_LAST_SEEN_WHATS_NEW_VERSION, null)

        if (lastSeenVersion != currentVersion) {
            prefs.edit()
                .putString(AppPrefs.KEY_LAST_SEEN_WHATS_NEW_VERSION, currentVersion)
                .apply()

            startActivity(Intent(this, WhatsNewActivity::class.java))
        }
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