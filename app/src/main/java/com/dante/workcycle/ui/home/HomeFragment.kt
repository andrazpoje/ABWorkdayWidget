package com.dante.workcycle.ui.home

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.dante.workcycle.BuildConfig
import com.dante.workcycle.R
import com.dante.workcycle.core.ui.applyImeInsetAsPadding
import com.dante.workcycle.core.ui.applySystemBarsBottomInsetAsPadding
import com.dante.workcycle.core.ui.applySystemBarsHorizontalInsetAsPadding
import com.dante.workcycle.data.prefs.AppPrefs
import com.dante.workcycle.databinding.FragmentHomeBinding
import com.dante.workcycle.domain.holiday.HolidayCountry
import com.dante.workcycle.domain.template.TemplateManager
import com.dante.workcycle.ui.adapter.CyclePreviewAdapter
import com.dante.workcycle.widget.WorkCycleWidgetProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import java.time.LocalDate

class HomeFragment : Fragment(R.layout.fragment_home) {

    companion object {
        const val SECTION_CYCLE = "cycle"
        const val SECTION_RULES = "rules"
        const val MAX_CYCLE_ITEMS = 16
        const val MAX_LABEL_LENGTH = 24
    }

    private var _binding: FragmentHomeBinding? = null

    var hasUnsavedChanges = false
    var isInitializing = false
    var draftFirstCycleDayIndex: Int? = null
    lateinit var activityRoot: View
    lateinit var saveBarContainer: View

    lateinit var cycleHeader: View
    lateinit var rulesHeader: View

    lateinit var cycleDaysInputLayout: TextInputLayout

    lateinit var cycleArrow: ImageView
    lateinit var rulesArrow: ImageView

    lateinit var widgetPromptContainer: View
    lateinit var githubLinkText: TextView
    lateinit var versionText: TextView
    lateinit var mainScrollView: NestedScrollView

    lateinit var revertButton: MaterialButton

    lateinit var cycleSection: View
    lateinit var rulesSection: View

    lateinit var dateText: TextView
    lateinit var pickDateButton: MaterialButton

    lateinit var presetInputLayout: TextInputLayout
    lateinit var presetDropdown: MaterialAutoCompleteTextView

    lateinit var activeTemplateCard: View
    lateinit var activeTemplateTitle: TextView
    lateinit var activeTemplateDescription: TextView
    lateinit var templateLockedMessage: TextView

    lateinit var cycleDaysEdit: EditText
    lateinit var firstCycleDayChipGroup: ChipGroup
    lateinit var firstCycleDayDropdown: MaterialAutoCompleteTextView

    lateinit var previewRecyclerView: RecyclerView
    lateinit var previewAdapter: CyclePreviewAdapter
    lateinit var previewWeekTitle: TextView
    lateinit var previousPreviewWeekButton: ImageButton
    lateinit var nextPreviewWeekButton: ImageButton
    lateinit var previewTodayButton: ImageButton

    lateinit var switchSaturdays: SwitchMaterial
    lateinit var switchSundays: SwitchMaterial
    lateinit var switchHolidays: SwitchMaterial

    lateinit var holidayCountryDropdown: MaterialAutoCompleteTextView

    lateinit var saveButton: MaterialButton
    lateinit var openWidgetsButton: MaterialButton

    lateinit var widgetHint: TextView

    var selectedDate: LocalDate = LocalDate.now()
    var previewWeekOffset: Int = 0
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
        setupPreviewWeekNavigator()
        setupHolidayCountryDropdown()
        migrateLegacySettingsIfNeeded()
        loadSettings()
        updateTemplateUiState()
        setupChangeListeners()
        updateTodayStatus()
        updateCyclePreview()
        updateWidgetHint()

        versionText.text = getString(R.string.app_version, BuildConfig.VERSION_NAME)

        setupSection(cycleHeader, cycleSection, cycleArrow, SECTION_CYCLE)
        setupSection(rulesHeader, rulesSection, rulesArrow, SECTION_RULES)

        restoreLastOpenSection()
        clearUnsavedChanges()

        isInitializing = false

        showWhatsNewIfAppUpdated(savedInstanceState)

        revertButton.setOnClickListener {
            revertToSavedState()
        }

        openWidgetsButton.setOnClickListener {
            requestAddWidget()
        }

        pickDateButton.setOnClickListener {
            showDatePicker()
        }

        saveButton.setOnClickListener {
            if (!hasUnsavedChanges) return@setOnClickListener

            if (!validateCycleInput()) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.fix_errors),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            saveChangesAndRefresh()
        }

        cycleDaysInputLayout.setOnClickListener {
            if (TemplateManager.isCycleEditingLocked(requireContext())) {
                showTemplateLockedMessage()
            }
        }

        holidayCountryDropdown.setOnClickListener {
            if (TemplateManager.isCycleEditingLocked(requireContext())) {
                showTemplateLockedMessage()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        if (!isInitializing) {
            updateUnsavedChangesState()
        }
    }

    private val sectionPrefs by lazy {
        requireContext().getSharedPreferences("home_fragment_sections", Context.MODE_PRIVATE)
    }

    private fun setupSection(vararg args: Any?) {
        val header = args.getOrNull(0) as? View ?: return
        val content = args.getOrNull(1) as? View ?: return
        val arrow = args.getOrNull(2) as? ImageView

        val sectionKey = args.firstOrNull { it is String } as? String
            ?: "section_${header.id}"

        val defaultExpanded = args.firstOrNull { it is Boolean } as? Boolean ?: false
        val savedExpanded = sectionPrefs.getBoolean(sectionKey, defaultExpanded)

        setSectionExpanded(content, arrow, savedExpanded)

        header.setOnClickListener {
            val newExpanded = !content.isVisible
            setSectionExpanded(content, arrow, newExpanded)

            sectionPrefs.edit {
                putBoolean(sectionKey, newExpanded)
                putString("last_open_section", sectionKey)
            }
        }
    }

    private fun restoreLastOpenSection() {
        // Safe no-op fallback.
        // Posamezne sekcije same preberejo svoj state iz SharedPreferences v setupSection().
        // To je dovolj, da se projekt normalno prevede in da collapse/expand ostane funkcionalen.
    }

    private fun setSectionExpanded(
        content: View,
        arrow: ImageView?,
        expanded: Boolean
    ) {
        content.isVisible = expanded
        arrow?.rotation = if (expanded) 180f else 0f
    }

    private fun showWhatsNewIfAppUpdated(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) return

        val prefs = requireContext().getSharedPreferences(
            AppPrefs.NAME,
            Context.MODE_PRIVATE
        )

        val isFirstLaunch = prefs.getBoolean("first_launch", true)

        // ❗ če je prvi zagon → samo označi in NE pokaži WhatsNew
        if (isFirstLaunch) {
            prefs.edit {
                putBoolean("first_launch", false)
                putString(AppPrefs.KEY_LAST_SEEN_WHATS_NEW_VERSION, BuildConfig.VERSION_NAME)
            }
            return
        }

        val currentVersion = BuildConfig.VERSION_NAME
        val lastSeenVersion = prefs.getString(AppPrefs.KEY_LAST_SEEN_WHATS_NEW_VERSION, null)

        if (lastSeenVersion != currentVersion) {
            prefs.edit {
                putString(AppPrefs.KEY_LAST_SEEN_WHATS_NEW_VERSION, currentVersion)
            }

            findNavController().navigate(R.id.whatsNewFragment)
        }
    }

    private fun requestAddWidget() {
        val context = requireContext()
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val provider = ComponentName(context, WorkCycleWidgetProvider::class.java)

        if (appWidgetManager.isRequestPinAppWidgetSupported) {
            appWidgetManager.requestPinAppWidget(provider, null, null)
        } else {
            Toast.makeText(
                context,
                getString(R.string.widget_add_manual_hint),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun bindViews(root: View) {
        activityRoot = root
        saveBarContainer = root.findViewById(R.id.saveBarContainer)
        mainScrollView = root.findViewById(R.id.main)

        cycleHeader = root.findViewById(R.id.cycleHeader)
        rulesHeader = root.findViewById(R.id.rulesHeader)

        cycleArrow = root.findViewById(R.id.cycleArrow)
        rulesArrow = root.findViewById(R.id.rulesArrow)

        widgetPromptContainer = root.findViewById(R.id.widgetPromptContainer)
        githubLinkText = root.findViewById(R.id.githubLinkText)
        versionText = root.findViewById(R.id.versionText)

        cycleSection = root.findViewById(R.id.cycleSection)
        rulesSection = root.findViewById(R.id.rulesSection)

        widgetHint = root.findViewById(R.id.widgetHint)
        dateText = root.findViewById(R.id.dateText)
        pickDateButton = root.findViewById(R.id.pickDateButton)

        presetInputLayout = root.findViewById(R.id.presetInputLayout)
        presetDropdown = root.findViewById(R.id.presetDropdown)

        activeTemplateCard = root.findViewById(R.id.activeTemplateCard)
        activeTemplateTitle = root.findViewById(R.id.activeTemplateTitle)
        activeTemplateDescription = root.findViewById(R.id.activeTemplateDescription)
        templateLockedMessage = root.findViewById(R.id.templateLockedMessage)

        cycleDaysInputLayout = root.findViewById(R.id.cycleDaysInputLayout)
        cycleDaysEdit = root.findViewById(R.id.cycleDaysEdit)
        firstCycleDayChipGroup = root.findViewById(R.id.firstCycleDayChipGroup)
        firstCycleDayDropdown = root.findViewById(R.id.firstCycleDayDropdown)

        switchSaturdays = root.findViewById(R.id.switchSaturdays)
        switchSundays = root.findViewById(R.id.switchSundays)
        switchHolidays = root.findViewById(R.id.switchHolidays)

        holidayCountryDropdown = root.findViewById(R.id.holidayCountryDropdown)

        saveButton = root.findViewById(R.id.saveButton)
        revertButton = root.findViewById(R.id.revertButton)
        openWidgetsButton = root.findViewById(R.id.openWidgetsButton)

        previewRecyclerView = root.findViewById(R.id.previewRecyclerView)
        previewWeekTitle = root.findViewById(R.id.previewWeekTitle)
        previousPreviewWeekButton = root.findViewById(R.id.previousPreviewWeekButton)
        nextPreviewWeekButton = root.findViewById(R.id.nextPreviewWeekButton)
        previewTodayButton = root.findViewById(R.id.previewTodayButton)
    }

    fun runWithoutChangeTracking(block: () -> Unit) {
        val previous = isInitializing
        isInitializing = true
        try {
            block()
        } finally {
            isInitializing = previous
        }
    }

    fun onNotificationPermissionDenied() {
        Toast.makeText(
            requireContext(),
            getString(R.string.notification_permission_denied),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun HomeFragment.showTemplateLockedMessage() {
        Toast.makeText(
            requireContext(),
            getString(R.string.template_locked_click_message),
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
