package com.dante.workcycle.ui.dialogs

import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.dante.workcycle.R
import com.dante.workcycle.core.status.StatusVisuals
import com.dante.workcycle.core.util.CycleColorHelper
import com.dante.workcycle.data.prefs.AssignmentCyclePrefs
import com.dante.workcycle.data.prefs.AssignmentLabelsPrefs
import com.dante.workcycle.data.prefs.StatusLabelsPrefs
import com.dante.workcycle.domain.model.AssignmentLabel
import com.dante.workcycle.domain.model.ResolvedDay
import com.dante.workcycle.domain.model.StatusLabel
import com.dante.workcycle.domain.schedule.CycleManager
import com.dante.workcycle.domain.schedule.CycleOverrideRepository
import com.dante.workcycle.domain.schedule.DefaultScheduleResolver
import com.dante.workcycle.domain.schedule.ManualScheduleRepository
import com.dante.workcycle.domain.schedule.StatusRepository
import com.dante.workcycle.domain.schedule.StatusTagRules
import com.dante.workcycle.domain.template.TemplateManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.divider.MaterialDivider
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class EditAssignmentDayBottomSheet(
    private val date: LocalDate,
    private val onSaved: (() -> Unit)? = null
) : DialogFragment() {

    private var draftCycleOverrideLabel: String? = null

    private var draftAssignmentLabel: String? = null

    private var draftStatusTags = linkedSetOf<String>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()

        val dialog = BottomSheetDialog(context, theme)
        val contentParent = FrameLayout(context)
        val view = layoutInflater.inflate(
            R.layout.bottom_sheet_edit_secondary_day_v2,
            contentParent,
            false
        )

        dialog.setContentView(view)

        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            bottomSheet?.setBackgroundColor(
                resolveThemeColor(com.google.android.material.R.attr.colorSurface)
            )
        }

        val statusRepository = StatusRepository(context)
        val statusLabelsPrefs = StatusLabelsPrefs(context)
        draftStatusTags = statusRepository.getStatusTags(date)

        val canEditCycleOverride = TemplateManager.allowsCycleOverrides(context)
        val assignmentPrefs = AssignmentCyclePrefs(context)
        val isAssignmentEnabled = assignmentPrefs.isEnabled()

        val resolver = DefaultScheduleResolver(context)
        val cycleOverrideRepository = CycleOverrideRepository(context)
        val manualScheduleRepository = ManualScheduleRepository(context)
        val labelsPrefs = AssignmentLabelsPrefs(context)

        val resolved = resolver.resolve(date)
        val cycle = CycleManager.loadCycle(context)

        draftCycleOverrideLabel = cycleOverrideRepository.getOverrideLabel(date)
            ?.trim()
            ?.ifBlank { null }

        draftAssignmentLabel = manualScheduleRepository.getSecondaryManualLabel(date)
            ?.trim()
            ?.ifBlank { null }

        bindHeader(view)
        bindDaySummaryCard(view, resolved)
        bindStatusSection(view, resolved)
        applySecondarySectionTitles(view)
        bindCycleSection(
            view = view,
            cycle = cycle,
            resolved = resolved,
            canEditCycleOverride = canEditCycleOverride
        )

        val assignmentSectionContainer =
            view.findViewById<ViewGroup>(R.id.assignmentSectionContainer)
        val assignmentDisabledMessage =
            view.findViewById<TextView>(R.id.assignmentDisabledMessage)

        assignmentSectionContainer.isVisible = isAssignmentEnabled
        assignmentDisabledMessage.isVisible = !isAssignmentEnabled

        if (isAssignmentEnabled) {
            bindAssignmentSection(
                view = view,
                labelsPrefs = labelsPrefs,
                resolved = resolved
            )
        }

        view.findViewById<MaterialButton>(R.id.btnSave).setOnClickListener {
            if (canEditCycleOverride) {
                cycleOverrideRepository.setOverrideLabel(date, draftCycleOverrideLabel)
            }

            if (isAssignmentEnabled) {
                manualScheduleRepository.setSecondaryManualLabel(date, draftAssignmentLabel)
                draftAssignmentLabel
                    ?.takeIf { it.isNotBlank() }
                    ?.let(labelsPrefs::markLabelUsed)
            } else {
                manualScheduleRepository.setSecondaryManualLabel(date, null)
            }

            if (!StatusTagRules.isValid(draftStatusTags, statusLabelsPrefs.getLabels())) {
                Toast.makeText(
                    context,
                    getString(R.string.status_tags_conflict_message),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            statusRepository.setStatusTags(date, draftStatusTags)

            onSaved?.invoke()
            dialog.dismiss()
        }

        view.findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        return dialog
    }

    override fun onStart() {
        super.onStart()

        val dialog = dialog as? BottomSheetDialog ?: return
        val bottomSheet = dialog.findViewById<View>(
            com.google.android.material.R.id.design_bottom_sheet
        ) ?: return

        bottomSheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT

        val behavior = BottomSheetBehavior.from(bottomSheet)
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.skipCollapsed = true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(view) { root, insets ->
            val systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            root.setPadding(
                root.paddingLeft,
                root.paddingTop,
                root.paddingRight,
                systemInsets.bottom
            )

            insets
        }
    }

    private fun bindHeader(view: View) {
        val dateText = view.findViewById<TextView>(R.id.dateText)
        dateText.text = date.format(
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                .withLocale(Locale.getDefault())
        )
    }

    private fun bindDaySummaryCard(
        view: View,
        resolved: ResolvedDay
    ) {
        val primary = view.findViewById<TextView>(R.id.daySummaryPrimary)
        val secondary = view.findViewById<TextView>(R.id.daySummarySecondary)
        val status = view.findViewById<TextView>(R.id.daySummaryStatus)
        val card = view.findViewById<MaterialCardView>(R.id.daySummaryCard)

        val activePrimary = draftCycleOverrideLabel ?: resolved.baseCycleLabel
        val activeSecondary = draftAssignmentLabel ?: resolved.secondaryEffectiveLabel ?: "-"
        val activeStatusTags = draftStatusTags
        val activeStatus = formatStatusTags(activeStatusTags) ?: "-"

        primary.text = activePrimary
        secondary.text = getString(R.string.day_summary_secondary_value, activeSecondary)
        status.text = getString(R.string.day_summary_status_value, activeStatus)

        applySummaryCardStyle(
            card = card,
            activePrimary = activePrimary,
            activeStatusTags = activeStatusTags
        )
    }

    private fun bindStatusSection(
        view: View,
        resolved: ResolvedDay
    ) {
        val statusChipGroup = view.findViewById<ChipGroup>(R.id.statusChipGroup)
        val statusExclusiveHelper = view.findViewById<TextView>(R.id.textStatusExclusiveHelper)
        val btnClearStatus = view.findViewById<MaterialButton>(R.id.btnClearStatus)
        val labels = StatusLabelsPrefs(requireContext()).getSelectableLabels()

        statusChipGroup.removeAllViews()

        labels.forEach { label ->
            val chip = createStatusChip(
                label = label,
                isSelected = label.name in draftStatusTags,
                isEnabled = StatusTagRules.canSelect(label, draftStatusTags, labels)
            )
            chip.setOnCheckedChangeListener { _, isChecked ->
                draftStatusTags = if (isChecked) {
                    StatusTagRules.applySelection(label, draftStatusTags, labels)
                } else {
                    LinkedHashSet(draftStatusTags).apply {
                        remove(label.name)
                    }
                }

                refreshStatusUi(view, resolved)
            }
            statusChipGroup.addView(chip)
        }

        val showExclusiveHelper = labels.any { label ->
            label.name in draftStatusTags && StatusVisuals.usesExclusiveHelper(label)
        }
        statusExclusiveHelper.visibility = if (showExclusiveHelper) {
            View.VISIBLE
        } else {
            View.GONE
        }

        updateStatusActionVisibility(view)

        btnClearStatus.setOnClickListener {
            draftStatusTags = linkedSetOf()
            refreshStatusUi(view, resolved)
        }
    }

    private fun bindCycleSection(
        view: View,
        cycle: List<String>,
        resolved: ResolvedDay,
        canEditCycleOverride: Boolean
    ) {
        val textCycleOverrideTitle = view.findViewById<TextView>(R.id.textCycleOverrideTitle)
        val cycleChipGroup = view.findViewById<ChipGroup>(R.id.cycleChipGroup)
        val btnClearCycleOverride = view.findViewById<MaterialButton>(R.id.btnClearCycleOverride)
        val dividerCycleTop = view.findViewById<MaterialDivider>(R.id.dividerCycleTop)
        val dividerCycleBottom = view.findViewById<MaterialDivider>(R.id.dividerCycleBottom)
        val cycleSectionContainer = view.findViewById<View>(R.id.cycleSectionContainer)
        val hint = view.findViewById<TextView>(R.id.textCycleOverrideHint)

        textCycleOverrideTitle.isVisible = true
        cycleChipGroup.isVisible = true
        dividerCycleTop.isVisible = true
        dividerCycleBottom.isVisible = true
        cycleSectionContainer.isVisible = true

        hint.isVisible = !canEditCycleOverride

        cycleSectionContainer.alpha = if (canEditCycleOverride) 1f else 0.45f
        cycleChipGroup.isEnabled = canEditCycleOverride
        btnClearCycleOverride.isEnabled = canEditCycleOverride

        setupCycleChips(
            chipGroup = cycleChipGroup,
            cycle = cycle,
            selectedLabel = draftCycleOverrideLabel
        )

        for (i in 0 until cycleChipGroup.childCount) {
            cycleChipGroup.getChildAt(i).isEnabled = canEditCycleOverride
        }

        if (!canEditCycleOverride) {
            btnClearCycleOverride.isVisible = false
        } else {
            updateCycleActionVisibility(view)
        }

        cycleChipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            if (!canEditCycleOverride) return@setOnCheckedStateChangeListener

            val selectedId = checkedIds.firstOrNull()
            draftCycleOverrideLabel = if (selectedId != null) {
                group.findViewById<Chip>(selectedId)
                    ?.text
                    ?.toString()
                    ?.trim()
                    ?.ifBlank { null }
            } else {
                null
            }

            refreshCycleUi(view, resolved)
        }

        btnClearCycleOverride.setOnClickListener {
            if (!canEditCycleOverride) return@setOnClickListener

            draftCycleOverrideLabel = null
            cycleChipGroup.clearCheck()
            refreshCycleUi(view, resolved)
        }
    }

    private fun applySecondarySectionTitles(view: View) {
        view.findViewById<TextView>(R.id.textSystemAssignments)?.text =
            getString(R.string.secondary_label_title)

        view.findViewById<TextView>(R.id.textManualAssignments)?.text =
            getString(R.string.secondary_override_title)

        view.findViewById<MaterialButton>(R.id.btnClearAssignment)?.text =
            getString(R.string.secondary_clear)
    }

    private fun bindAssignmentSection(
        view: View,
        labelsPrefs: AssignmentLabelsPrefs,
        resolved: ResolvedDay
    ) {
        val systemChipGroup = view.findViewById<ChipGroup>(R.id.systemChipGroup)
        val manualChipGroup = view.findViewById<ChipGroup>(R.id.manualChipGroup)
        val textSystemAssignments = view.findViewById<TextView>(R.id.textSystemAssignments)
        val textManualAssignments = view.findViewById<TextView>(R.id.textManualAssignments)
        val btnClearAssignment = view.findViewById<MaterialButton>(R.id.btnClearAssignment)

        val selectable = labelsPrefs.getSelectableLabels()

        val systemLabels = selectable.filter {
            it.isSystem && it.iconKey !in listOf("sick", "vacation", "standby")
        }
        val manualLabels = selectable.filterNot { it.isSystem }

        textSystemAssignments.isVisible = systemLabels.isNotEmpty()
        systemChipGroup.isVisible = systemLabels.isNotEmpty()
        textManualAssignments.isVisible = manualLabels.isNotEmpty()
        manualChipGroup.isVisible = manualLabels.isNotEmpty()

        setupAssignmentChips(
            chipGroup = systemChipGroup,
            labels = systemLabels,
            selectedLabel = draftAssignmentLabel
        )

        setupAssignmentChips(
            chipGroup = manualChipGroup,
            labels = manualLabels,
            selectedLabel = draftAssignmentLabel
        )

        updateSecondaryActionVisibility(view)

        systemChipGroup.setOnCheckedStateChangeListener(
            systemCheckedListener(
                view = view,
                resolved = resolved,
                systemChipGroup = systemChipGroup,
                manualChipGroup = manualChipGroup
            )
        )

        manualChipGroup.setOnCheckedStateChangeListener(
            manualCheckedListener(
                view = view,
                resolved = resolved,
                systemChipGroup = systemChipGroup,
                manualChipGroup = manualChipGroup
            )
        )

        btnClearAssignment.setOnClickListener {
            draftAssignmentLabel = null

            systemChipGroup.setOnCheckedStateChangeListener(null)
            manualChipGroup.setOnCheckedStateChangeListener(null)

            systemChipGroup.clearCheck()
            manualChipGroup.clearCheck()

            systemChipGroup.setOnCheckedStateChangeListener(
                systemCheckedListener(
                    view = view,
                    resolved = resolved,
                    systemChipGroup = systemChipGroup,
                    manualChipGroup = manualChipGroup
                )
            )

            manualChipGroup.setOnCheckedStateChangeListener(
                manualCheckedListener(
                    view = view,
                    resolved = resolved,
                    systemChipGroup = systemChipGroup,
                    manualChipGroup = manualChipGroup
                )
            )

            refreshAssignmentUi(view, resolved)
        }
    }

    private fun systemCheckedListener(
        view: View,
        resolved: ResolvedDay,
        systemChipGroup: ChipGroup,
        manualChipGroup: ChipGroup
    ): ChipGroup.OnCheckedStateChangeListener {
        return ChipGroup.OnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                manualChipGroup.setOnCheckedStateChangeListener(null)
                manualChipGroup.clearCheck()
                manualChipGroup.setOnCheckedStateChangeListener(
                    manualCheckedListener(
                        view = view,
                        resolved = resolved,
                        systemChipGroup = systemChipGroup,
                        manualChipGroup = manualChipGroup
                    )
                )
            }

            val selectedId = checkedIds.firstOrNull()
            draftAssignmentLabel = if (selectedId != null) {
                group.findViewById<Chip>(selectedId)
                    ?.text
                    ?.toString()
                    ?.trim()
                    ?.ifBlank { null }
            } else if (manualChipGroup.checkedChipId == ViewGroup.NO_ID) {
                null
            } else {
                draftAssignmentLabel
            }

            refreshAssignmentUi(view, resolved)
        }
    }

    private fun manualCheckedListener(
        view: View,
        resolved: ResolvedDay,
        systemChipGroup: ChipGroup,
        manualChipGroup: ChipGroup
    ): ChipGroup.OnCheckedStateChangeListener {
        return ChipGroup.OnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                systemChipGroup.setOnCheckedStateChangeListener(null)
                systemChipGroup.clearCheck()
                systemChipGroup.setOnCheckedStateChangeListener(
                    systemCheckedListener(
                        view = view,
                        resolved = resolved,
                        systemChipGroup = systemChipGroup,
                        manualChipGroup = manualChipGroup
                    )
                )
            }

            val selectedId = checkedIds.firstOrNull()
            draftAssignmentLabel = if (selectedId != null) {
                group.findViewById<Chip>(selectedId)
                    ?.text
                    ?.toString()
                    ?.trim()
                    ?.ifBlank { null }
            } else {
                null
            }

            refreshAssignmentUi(view, resolved)
        }
    }

    private fun refreshCycleUi(
        view: View,
        resolved: ResolvedDay
    ) {
        bindDaySummaryCard(view, resolved)
        updateCycleActionVisibility(view)
    }

    private fun refreshAssignmentUi(
        view: View,
        resolved: ResolvedDay
    ) {
        bindDaySummaryCard(view, resolved)
        updateSecondaryActionVisibility(view)
    }

    private fun refreshStatusUi(
        view: View,
        resolved: ResolvedDay
    ) {
        bindDaySummaryCard(view, resolved)
        bindStatusSection(view, resolved)
        updateStatusActionVisibility(view)
    }

    private fun updateCycleActionVisibility(view: View) {
        val btnClearCycleOverride =
            view.findViewById<MaterialButton>(R.id.btnClearCycleOverride)

        btnClearCycleOverride.isVisible = !draftCycleOverrideLabel.isNullOrBlank()
    }

    private fun updateStatusActionVisibility(view: View) {
        val btnClearStatus = view.findViewById<MaterialButton>(R.id.btnClearStatus)
        btnClearStatus.isVisible = draftStatusTags.isNotEmpty()
    }

    private fun updateSecondaryActionVisibility(view: View) {
        val btnClearAssignment =
            view.findViewById<MaterialButton>(R.id.btnClearAssignment)

        btnClearAssignment.isVisible = !draftAssignmentLabel.isNullOrBlank()
    }

    private fun applySummaryCardStyle(
        card: MaterialCardView,
        activePrimary: String,
        activeStatusTags: Set<String>
    ) {
        val cycle = CycleManager.loadCycle(requireContext())

        val baseColor = CycleColorHelper.getBackgroundColor(
            context = requireContext(),
            label = activePrimary,
            cycle = cycle
        )

        val background = if (isDarkTheme()) {
            ColorUtils.blendARGB(baseColor, Color.BLACK, 0.72f)
        } else {
            ColorUtils.blendARGB(baseColor, Color.WHITE, 0.88f)
        }

        card.setCardBackgroundColor(background)

        val statusStrokeColor = getStatusHighlightColor(activeStatusTags)
        card.strokeColor = statusStrokeColor ?: CycleColorHelper.getSoftStroke(baseColor)
        card.strokeWidth = if (activeStatusTags.isNotEmpty()) 4 else 2
        card.cardElevation = if (activeStatusTags.isNotEmpty()) 8f else 4f
    }

    private fun isDarkTheme(): Boolean {
        val nightModeFlags = resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }

    private fun setupCycleChips(
        chipGroup: ChipGroup,
        cycle: List<String>,
        selectedLabel: String?
    ) {
        chipGroup.removeAllViews()

        cycle.forEach { label ->
            chipGroup.addView(
                createCycleChip(
                    label = label,
                    cycle = cycle,
                    isSelected = label == selectedLabel
                )
            )
        }
    }

    private fun setupAssignmentChips(
        chipGroup: ChipGroup,
        labels: List<AssignmentLabel>,
        selectedLabel: String?
    ) {
        chipGroup.removeAllViews()

        labels.forEach { label ->
            chipGroup.addView(
                createAssignmentChip(
                    label = label,
                    isSelected = label.name == selectedLabel
                )
            )
        }
    }

    private fun createCycleChip(
        label: String,
        cycle: List<String>,
        isSelected: Boolean
    ): Chip {
        val context = requireContext()
        val backgroundColor = CycleColorHelper.getBackgroundColor(context, label, cycle)
        val textColor = CycleColorHelper.getTextColorForBackground(backgroundColor)

        return Chip(context).apply {
            id = ViewGroup.generateViewId()
            text = label
            isCheckable = true
            isClickable = !isSelected
            isEnabled = !isSelected
            isChecked = isSelected

            layoutParams = ChipGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            chipBackgroundColor = ColorStateList.valueOf(backgroundColor)
            setTextColor(textColor)

            chipStrokeWidth = if (isSelected) 3f else 1.5f
            chipStrokeColor = ColorStateList.valueOf(
                if (isSelected) {
                    ColorUtils.blendARGB(backgroundColor, Color.BLACK, 0.30f)
                } else {
                    ColorUtils.blendARGB(backgroundColor, Color.BLACK, 0.12f)
                }
            )

            checkedIcon = null
            rippleColor = ColorStateList.valueOf(Color.TRANSPARENT)

            setOnClickListener {
                performSelectionFeedback(this)
            }
        }
    }

    private fun createAssignmentChip(
        label: AssignmentLabel,
        isSelected: Boolean
    ): Chip {
        val context = requireContext()
        val textColor = getReadableTextColor(label.color)

        return Chip(context).apply {
            id = ViewGroup.generateViewId()
            text = label.name
            isCheckable = true
            isClickable = !isSelected
            isEnabled = !isSelected
            isChecked = isSelected

            layoutParams = ChipGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            chipBackgroundColor = ColorStateList.valueOf(label.color)
            setTextColor(textColor)

            chipStrokeWidth = if (label.isSystem) 2f else 0f
            chipStrokeColor = if (label.isSystem) {
                ColorStateList.valueOf(adjustStrokeColor(label.color))
            } else {
                null
            }

            val iconRes = getIconRes(label.iconKey)
            if (iconRes != null) {
                chipIcon = AppCompatResources.getDrawable(context, iconRes)
                chipIconTint = ColorStateList.valueOf(textColor)
                isChipIconVisible = true
            } else {
                isChipIconVisible = false
            }

            setOnClickListener {
                performSelectionFeedback(this)
            }
        }
    }

    private fun createStatusChip(
        label: StatusLabel,
        isSelected: Boolean,
        isEnabled: Boolean
    ): Chip {
        val context = requireContext()

        return Chip(context).apply {
            id = View.generateViewId()
            text = StatusVisuals.getDisplayName(context, label)
            isCheckable = true
            isChecked = isSelected
            this.isClickable = isEnabled
            this.isEnabled = isEnabled

            chipBackgroundColor = ColorStateList.valueOf(label.color)
            val textColor = getReadableTextColor(label.color)
            setTextColor(textColor)

            val iconRes = StatusVisuals.getIconRes(label.iconKey)
            if (iconRes != null) {
                chipIcon = AppCompatResources.getDrawable(context, iconRes)
                chipIconTint = ColorStateList.valueOf(textColor)
                isChipIconVisible = true
            } else {
                isChipIconVisible = false
            }

            setOnClickListener {
                if (!isEnabled) return@setOnClickListener
                performSelectionFeedback(this)
            }
        }
    }

    private fun formatStatusTags(tags: Set<String>): String? {
        return tags.map { it.trim() }
            .filter { it.isNotBlank() }
            .takeIf { it.isNotEmpty() }
            ?.joinToString(", ")
    }

    private fun getStatusHighlightColor(tags: Set<String>): Int? {
        if (tags.isEmpty()) return null

        val prefs = StatusLabelsPrefs(requireContext())
        return tags.firstNotNullOfOrNull { tag ->
            prefs.getLabelByName(tag)?.color
        }
    }

    private fun performSelectionFeedback(target: View) {
        target.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    private fun getReadableTextColor(backgroundColor: Int): Int {
        return if (ColorUtils.calculateLuminance(backgroundColor) < 0.5) {
            Color.WHITE
        } else {
            Color.BLACK
        }
    }

    private fun adjustStrokeColor(color: Int): Int {
        return ColorUtils.blendARGB(color, Color.BLACK, 0.25f)
    }

    private fun getIconRes(iconKey: String?): Int? {
        return StatusVisuals.getIconRes(iconKey)
    }

    private fun resolveThemeColor(attrRes: Int): Int {
        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(attrRes, typedValue, true)
        return typedValue.data
    }
}
