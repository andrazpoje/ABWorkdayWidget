package com.dante.abworkdaywidget

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.net.toUri
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.dante.abworkdaywidget.databinding.FragmentHomeBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.ChipGroup
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import java.time.LocalDate

class HomeFragment : Fragment(R.layout.fragment_home) {

    companion object {
        const val PREFS_UI = "ui_prefs"
        const val KEY_LAST_OPEN_SECTION = "last_open_section"

        const val SECTION_CYCLE = "cycle"
        const val SECTION_RULES = "rules"
        const val SECTION_DISPLAY = "display"

        const val MAX_CYCLE_ITEMS = 16
        const val MAX_LABEL_LENGTH = 24
    }

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    var hasUnsavedChanges = false
    var isInitializing = false

    lateinit var activityRoot: View
    lateinit var saveBarContainer: View

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

    lateinit var revertButton: MaterialButton

    lateinit var cycleSection: View
    lateinit var rulesSection: View
    lateinit var displaySection: View

    lateinit var dateText: TextView
    lateinit var pickDateButton: MaterialButton

    lateinit var presetDropdown: MaterialAutoCompleteTextView

    lateinit var cycleDaysEdit: EditText
    lateinit var firstCycleDayChipGroup: ChipGroup
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

    var selectedDate: LocalDate = LocalDate.now()
    lateinit var supportedCountries: List<HolidayCountry>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentHomeBinding.bind(view)
        bindViews(view)

        mainScrollView.applySystemBarsBottomInsetAsPadding()
        activityRoot.applySystemBarsHorizontalInsetAsPadding()
        saveBarContainer.applyImeInsetAsPadding()

        isInitializing = true

        setupFirstCycleDayDropdown()
        setupPresetDropdown()
        setupPreviewRecyclerView()
        setupHolidayCountryDropdown()
        migrateLegacySettingsIfNeeded()
        loadSettings()
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

        isInitializing = false

        showWhatsNewIfAppUpdated(savedInstanceState)

        revertButton.setOnClickListener {
            revertToSavedState()
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
                Toast.makeText(requireContext(), getString(R.string.fix_errors), Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            saveChangesAndRefresh()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showWhatsNewIfAppUpdated(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) return

        val prefs = requireContext().getSharedPreferences(
            AppPrefs.NAME,
            android.content.Context.MODE_PRIVATE
        )
        val currentVersion = BuildConfig.VERSION_NAME
        val lastSeenVersion = prefs.getString(AppPrefs.KEY_LAST_SEEN_WHATS_NEW_VERSION, null)

        if (lastSeenVersion != currentVersion) {
            prefs.edit()
                .putString(AppPrefs.KEY_LAST_SEEN_WHATS_NEW_VERSION, currentVersion)
                .apply()

            findNavController().navigate(R.id.whatsNewFragment)
        }
    }

    private fun bindViews(root: View) {
        activityRoot = root
        saveBarContainer = root.findViewById(R.id.saveBarContainer)
        mainScrollView = root.findViewById(R.id.main)

        cycleHeader = root.findViewById(R.id.cycleHeader)
        rulesHeader = root.findViewById(R.id.rulesHeader)
        displayHeader = root.findViewById(R.id.displayHeader)

        cycleArrow = root.findViewById(R.id.cycleArrow)
        rulesArrow = root.findViewById(R.id.rulesArrow)
        displayArrow = root.findViewById(R.id.displayArrow)

        widgetPromptContainer = root.findViewById(R.id.widgetPromptContainer)
        githubLinkText = root.findViewById(R.id.githubLinkText)
        versionText = root.findViewById(R.id.versionText)

        cycleSection = root.findViewById(R.id.cycleSection)
        rulesSection = root.findViewById(R.id.rulesSection)
        displaySection = root.findViewById(R.id.displaySection)

        widgetHint = root.findViewById(R.id.widgetHint)
        dateText = root.findViewById(R.id.dateText)
        pickDateButton = root.findViewById(R.id.pickDateButton)

        presetDropdown = root.findViewById(R.id.presetDropdown)

        cycleDaysInputLayout = root.findViewById(R.id.cycleDaysInputLayout)
        cycleDaysEdit = root.findViewById(R.id.cycleDaysEdit)
        firstCycleDayChipGroup = root.findViewById(R.id.firstCycleDayChipGroup)
        firstCycleDayDropdown = root.findViewById(R.id.firstCycleDayDropdown)

        switchSaturdays = root.findViewById(R.id.switchSaturdays)
        switchSundays = root.findViewById(R.id.switchSundays)
        switchHolidays = root.findViewById(R.id.switchHolidays)
        switchOverrideSkippedDays = root.findViewById(R.id.switchOverrideSkippedDays)

        holidayCountryDropdown = root.findViewById(R.id.holidayCountryDropdown)

        saveButton = root.findViewById(R.id.saveButton)
        revertButton = root.findViewById(R.id.revertButton)
        openWidgetsButton = root.findViewById(R.id.openWidgetsButton)
        prefixEdit = root.findViewById(R.id.prefixEdit)
        skippedDayLabelEdit = root.findViewById(R.id.skippedDayLabelEdit)

        statusCard = root.findViewById(R.id.statusCard)
        todayStatusText = root.findViewById(R.id.todayStatusText)
        tomorrowStatusText = root.findViewById(R.id.tomorrowStatusText)

        previewRecyclerView = root.findViewById(R.id.previewRecyclerView)

        themeClassic = root.findViewById(R.id.themeClassic)
        themePastel = root.findViewById(R.id.themePastel)
        themeDark = root.findViewById(R.id.themeDark)
    }

    fun requestNotificationPermissionIfNeeded() {
        (activity as? MainActivity)?.requestNotificationPermissionIfNeeded()
    }

    fun onNotificationPermissionDenied() {
        Toast.makeText(
            requireContext(),
            getString(R.string.notification_permission_denied),
            Toast.LENGTH_SHORT
        ).show()
    }

    fun setupBackHandling() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (!hasUnsavedChanges) {
                        requireActivity().finish()
                        return
                    }

                    showUnsavedChangesDialog(
                        onSave = {
                            val saved = saveChangesAndRefresh()
                            if (saved) requireActivity().finish()
                        },
                        onDiscard = {
                            requireActivity().finish()
                        }
                    )
                }
            }
        )
    }
}