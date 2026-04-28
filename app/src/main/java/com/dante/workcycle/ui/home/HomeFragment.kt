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
import com.dante.workcycle.core.util.DateProvider
import com.dante.workcycle.data.prefs.AppPrefs
import com.dante.workcycle.data.prefs.LaunchPrefs
import com.dante.workcycle.data.prefs.Prefs
import com.dante.workcycle.data.prefs.StatusLabelsPrefs
import com.dante.workcycle.databinding.FragmentHomeBinding
import com.dante.workcycle.domain.holiday.HolidayCountry
import com.dante.workcycle.domain.holiday.HolidayManager
import com.dante.workcycle.domain.schedule.CycleManager
import com.dante.workcycle.domain.schedule.DefaultScheduleResolver
import com.dante.workcycle.domain.template.TemplateManager
import com.dante.workcycle.ui.adapter.CyclePreviewAdapter
import com.dante.workcycle.widget.WorkCycleWidgetProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class HomeFragment : Fragment(R.layout.fragment_home) {

    companion object {
        const val SECTION_CYCLE = "cycle"
        const val SECTION_RULES = "rules"
        const val MAX_CYCLE_ITEMS = 16
        const val MAX_LABEL_LENGTH = 24
        private const val UPCOMING_EVENTS_LOOKAHEAD_DAYS = 30
        private const val UPCOMING_EVENTS_MAX_ITEMS = 5
    }

    private data class UpcomingEvent(
        val dateText: String,
        val label: String
    )

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
    lateinit var dismissWidgetTipButton: MaterialButton

    lateinit var widgetHint: TextView
    lateinit var upcomingEventsEmptyText: TextView
    lateinit var upcomingEventsList: View
    lateinit var upcomingEventRows: List<View>
    lateinit var upcomingEventDateTexts: List<TextView>
    lateinit var upcomingEventLabelTexts: List<TextView>

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
        updateUpcomingEvents()

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

        dismissWidgetTipButton.setOnClickListener {
            requireContext().getSharedPreferences(Prefs.PREFS_NAME, Context.MODE_PRIVATE).edit {
                putBoolean(Prefs.KEY_HOME_WIDGET_TIP_DISMISSED, true)
            }
            widgetPromptContainer.visibility = View.GONE
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
            loadSettings()
            updateCyclePreview()
            updateWidgetHint()
            updateUpcomingEvents()
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

        val launchPrefs = LaunchPrefs(requireContext())
        if (!launchPrefs.isOnboardingCompleted()) return

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

        cycleSection = root.findViewById(R.id.cycleSection)
        rulesSection = root.findViewById(R.id.rulesSection)

        widgetHint = root.findViewById(R.id.widgetHint)
        upcomingEventsEmptyText = root.findViewById(R.id.upcomingEventsEmptyText)
        upcomingEventsList = root.findViewById(R.id.upcomingEventsList)
        upcomingEventRows = listOf(
            root.findViewById(R.id.upcomingEventRow1),
            root.findViewById(R.id.upcomingEventRow2),
            root.findViewById(R.id.upcomingEventRow3),
            root.findViewById(R.id.upcomingEventRow4),
            root.findViewById(R.id.upcomingEventRow5)
        )
        upcomingEventDateTexts = listOf(
            root.findViewById(R.id.upcomingEventDate1),
            root.findViewById(R.id.upcomingEventDate2),
            root.findViewById(R.id.upcomingEventDate3),
            root.findViewById(R.id.upcomingEventDate4),
            root.findViewById(R.id.upcomingEventDate5)
        )
        upcomingEventLabelTexts = listOf(
            root.findViewById(R.id.upcomingEventLabel1),
            root.findViewById(R.id.upcomingEventLabel2),
            root.findViewById(R.id.upcomingEventLabel3),
            root.findViewById(R.id.upcomingEventLabel4),
            root.findViewById(R.id.upcomingEventLabel5)
        )
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
        dismissWidgetTipButton = root.findViewById(R.id.dismissWidgetTipButton)

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

    fun updateUpcomingEvents() {
        val events = collectUpcomingEvents()

        upcomingEventsEmptyText.isVisible = events.isEmpty()
        upcomingEventsList.isVisible = events.isNotEmpty()
        upcomingEventsEmptyText.text = getString(R.string.home_upcoming_events_empty)

        upcomingEventRows.forEachIndexed { index, row ->
            val event = events.getOrNull(index)
            row.isVisible = event != null
            upcomingEventDateTexts[index].text = event?.dateText.orEmpty()
            upcomingEventLabelTexts[index].text = event?.label.orEmpty()
        }
    }

    private fun collectUpcomingEvents(): List<UpcomingEvent> {
        val context = requireContext()
        val resolver = DefaultScheduleResolver(context)
        val statusLabelsPrefs = StatusLabelsPrefs(context)
        val statusLabels = statusLabelsPrefs.getLabels()
        val offDayLabel = getString(R.string.off_day_label)
        val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(Locale.getDefault())
        val separator = getString(R.string.home_upcoming_event_reason_separator)
        val events = mutableListOf<UpcomingEvent>()

        for (dayOffset in 1..UPCOMING_EVENTS_LOOKAHEAD_DAYS) {
            val date = DateProvider.today().plusDays(dayOffset.toLong())
            val resolved = runCatching { resolver.resolve(date) }.getOrNull() ?: continue
            val reasons = mutableListOf<String>()
            val isWeekend = date.dayOfWeek.value >= 6
            val isHoliday = HolidayManager.isHoliday(context, date)
            val isOffDay = resolved.effectiveCycleLabel.equals(offDayLabel, ignoreCase = true)

            if (isHoliday) {
                reasons.add(getString(R.string.home_upcoming_event_holiday))
            } else if (isOffDay && !isWeekend) {
                reasons.add(getString(R.string.home_upcoming_event_off_day))
            }

            resolved.statusTags.forEach { rawStatus ->
                val statusLabel = statusLabels.firstOrNull {
                    it.name.equals(rawStatus, ignoreCase = true)
                }
                val displayName = statusLabel?.let(statusLabelsPrefs::getDisplayName)
                    ?: rawStatus.trim()

                if (displayName.isNotBlank()) {
                    reasons.add(displayName)
                }
            }

            if (reasons.isNotEmpty()) {
                events.add(
                    UpcomingEvent(
                        dateText = date.format(dateFormatter),
                        label = reasons.distinct().joinToString(separator)
                    )
                )
            }

            if (events.size >= UPCOMING_EVENTS_MAX_ITEMS) break
        }

        return events
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
