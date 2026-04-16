package com.dante.workcycle.ui.dialogs

import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.dante.workcycle.R
import com.dante.workcycle.core.util.CycleColorHelper
import com.dante.workcycle.data.prefs.AssignmentCyclePrefs
import com.dante.workcycle.data.prefs.AssignmentLabelsPrefs
import com.dante.workcycle.domain.model.AssignmentLabel
import com.dante.workcycle.domain.model.ResolvedDay
import com.dante.workcycle.domain.schedule.CycleManager
import com.dante.workcycle.domain.schedule.CycleOverrideRepository
import com.dante.workcycle.domain.schedule.DefaultScheduleResolver
import com.dante.workcycle.domain.schedule.ManualScheduleRepository
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
import com.dante.workcycle.data.prefs.StatusLabelsPrefs
import com.dante.workcycle.domain.model.StatusLabel
import com.dante.workcycle.domain.schedule.StatusRepository
import android.view.HapticFeedbackConstants


class EditAssignmentDayBottomSheet(
    private val date: LocalDate,
    private val onSaved: (() -> Unit)? = null
) : DialogFragment() {

    private var savedCycleOverrideLabel: String? = null
    private var draftCycleOverrideLabel: String? = null

    private var savedAssignmentLabel: String? = null
    private var draftAssignmentLabel: String? = null

    private var savedStatusLabel: String? = null
    private var draftStatusLabel: String? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()

        val dialog = BottomSheetDialog(context, theme)

        val view = LayoutInflater.from(context)
            .inflate(R.layout.bottom_sheet_edit_secondary_day_v2, null, false)

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

        savedStatusLabel = statusRepository.getStatusLabel(date)
        draftStatusLabel = savedStatusLabel

        val canEditCycleOverride = TemplateManager.allowsCycleOverrides(context)
        val assignmentPrefs = AssignmentCyclePrefs(context)
        val isAssignmentEnabled = assignmentPrefs.isEnabled()

        val resolver = DefaultScheduleResolver(context)
        val cycleOverrideRepository = CycleOverrideRepository(context)
        val manualScheduleRepository = ManualScheduleRepository(context)
        val labelsPrefs = AssignmentLabelsPrefs(context)

        val resolved = resolver.resolve(date)
        bindStatusSection(view, resolved)
        val cycle = CycleManager.loadCycle(context)

        savedCycleOverrideLabel = cycleOverrideRepository.getOverrideLabel(date)
            ?.trim()
            ?.ifBlank { null }
        draftCycleOverrideLabel = savedCycleOverrideLabel

        savedAssignmentLabel = manualScheduleRepository.getSecondaryManualLabel(date)
            ?.trim()
            ?.ifBlank { null }
        draftAssignmentLabel = savedAssignmentLabel

        bindHeader(view)
        bindDaySummaryCard(view, resolved)
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

        val btnSave = view.findViewById<MaterialButton>(R.id.btnSave)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancel)

        btnSave.setOnClickListener {
            if (canEditCycleOverride) {
                cycleOverrideRepository.setOverrideLabel(date, draftCycleOverrideLabel)
            }

            if (isAssignmentEnabled) {
                manualScheduleRepository.setSecondaryManualLabel(date, draftAssignmentLabel)

                draftAssignmentLabel
                    ?.takeIf { it.isNotBlank() }
                    ?.let { labelsPrefs.markLabelUsed(it) }
            } else {
                manualScheduleRepository.setSecondaryManualLabel(date, null)
            }

            statusRepository.setStatusLabel(date, draftStatusLabel)

            onSaved?.invoke()
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
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

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.setPadding(
                v.paddingLeft,
                v.paddingTop,
                v.paddingRight,
                systemInsets.bottom
            )

            insets
        }
    }

    private fun createStatusChip(
        label: StatusLabel,
        isSelected: Boolean
    ): Chip {
        val context = requireContext()

        return Chip(context).apply {
            id = View.generateViewId()
            text = label.name
            isCheckable = true
            isChecked = isSelected
            isClickable = !isSelected
            isEnabled = !isSelected

            chipBackgroundColor = ColorStateList.valueOf(label.color)
            setTextColor(getReadableTextColor(label.color))

            val iconRes = getIconRes(label.iconKey)
            if (iconRes != null) {
                chipIcon = context.getDrawable(iconRes)
                isChipIconVisible = true
            }

            setOnClickListener {
                performSelectionFeedback(this)
            }
        }
    }

    private fun bindStatusSection(
        view: View,
        resolved: ResolvedDay
    ) {
        val statusChipGroup = view.findViewById<ChipGroup>(R.id.statusChipGroup)
        val btnClearStatus = view.findViewById<MaterialButton>(R.id.btnClearStatus)

        val prefs = StatusLabelsPrefs(requireContext())
        val labels: List<StatusLabel> = prefs.getSelectableLabels()

        statusChipGroup.removeAllViews()

        labels.forEach { label: StatusLabel ->
            val chip = createStatusChip(label, label.name == draftStatusLabel)
            statusChipGroup.addView(chip)
        }
        updateStatusActionVisibility(view)

        statusChipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            val selectedId = checkedIds.firstOrNull()

            draftStatusLabel = if (selectedId != null) {
                group.findViewById<Chip>(selectedId)?.text?.toString()?.trim()
            } else {
                null
            }

            refreshStatusUi(view, resolved)
        }

        btnClearStatus.setOnClickListener {
            draftStatusLabel = null
            statusChipGroup.clearCheck()
            refreshStatusUi(view, resolved)
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

        val activeSecondary = draftAssignmentLabel
            ?: resolved.secondaryEffectiveLabel
            ?: "—"

        val activeStatus = draftStatusLabel
            ?: resolved.statusLabel
            ?: "—"

        primary.text = activePrimary
        secondary.text = getString(R.string.day_summary_secondary_value, activeSecondary)
        status.text = getString(R.string.day_summary_status_value, activeStatus)

        applySummaryCardStyle(
            card = card,
            activePrimary = activePrimary,
            activeStatus = if (activeStatus == "—") null else activeStatus
        )
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

            refreshCycleUi(view, resolved, cycle)
        }

        btnClearCycleOverride.setOnClickListener {
            if (!canEditCycleOverride) return@setOnClickListener

            draftCycleOverrideLabel = null
            cycleChipGroup.clearCheck()
            refreshCycleUi(view, resolved, cycle)
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
            it.isSystem &&
                    it.iconKey !in listOf("sick", "vacation", "standby")
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
        resolved: ResolvedDay,
        cycle: List<String>
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
        updateStatusActionVisibility(view)
    }

    private fun getStatusHighlightColor(status: String?): Int? {
        return when (status?.trim()) {
            "Bolniška" -> Color.parseColor("#E53935")
            "Dopust" -> Color.parseColor("#F9A825")
            "Dežurstvo" -> Color.parseColor("#8E24AA")
            "Sick leave" -> Color.parseColor("#E53935")
            "Vacation" -> Color.parseColor("#F9A825")
            "Standby" -> Color.parseColor("#8E24AA")
            else -> null
        }
    }

    private fun performSelectionFeedback(target: View) {
        target.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    private fun updateCycleActionVisibility(view: View) {
        val btnClearCycleOverride =
            view.findViewById<MaterialButton>(R.id.btnClearCycleOverride)

        btnClearCycleOverride.isVisible = !draftCycleOverrideLabel.isNullOrBlank()
    }

    private fun updateStatusActionVisibility(view: View) {
        val btnClearStatus =
            view.findViewById<MaterialButton>(R.id.btnClearStatus)

        btnClearStatus.isVisible = !draftStatusLabel.isNullOrBlank()
    }

    private fun updateSecondaryActionVisibility(view: View) {
        val btnClearAssignment =
            view.findViewById<MaterialButton>(R.id.btnClearAssignment)

        btnClearAssignment.isVisible = !draftAssignmentLabel.isNullOrBlank()
    }


    private fun applySummaryCardStyle(
        card: MaterialCardView,
        activePrimary: String,
        activeStatus: String?
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

        val statusStrokeColor = getStatusHighlightColor(activeStatus)
        card.strokeColor = statusStrokeColor ?: CycleColorHelper.getSoftStroke(baseColor)
        card.strokeWidth = if (activeStatus != null) 4 else 2
        card.cardElevation = if (activeStatus != null) 8f else 4f

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
            val chip = createCycleChip(
                label = label,
                cycle = cycle,
                isSelected = label == selectedLabel
            )
            chipGroup.addView(chip)
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

    private fun updateCycleInfoText(
        textView: TextView,
        baseLabel: String,
        overrideLabel: String?
    ) {
        textView.text = if (overrideLabel.isNullOrBlank()) {
            getString(R.string.bottom_sheet_cycle_info_base_only, baseLabel)
        } else {
            getString(R.string.bottom_sheet_cycle_info_with_override, baseLabel, overrideLabel)
        }
    }

    private fun updateSecondaryInfoText(
        textView: TextView,
        baseLabel: String?,
        overrideLabel: String?
    ) {
        val base = baseLabel?.trim()?.ifBlank { null }
        val override = overrideLabel?.trim()?.ifBlank { null }
        val active = override ?: base
        val display = active ?: getString(R.string.none_label)

        textView.text = if (override != null) {
            getString(R.string.bottom_sheet_secondary_info_with_override, display)
        } else {
            getString(R.string.bottom_sheet_secondary_info, display)
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
                chipIcon = context.getDrawable(iconRes)
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
        return when (iconKey) {
            "sick" -> R.drawable.ic_assignment_sick_24
            "vacation" -> R.drawable.ic_assignment_vacation_24
            "standby" -> R.drawable.ic_assignment_standby_24
            "field" -> R.drawable.ic_assignment_field_24
            else -> null
        }
    }

    private fun resolveThemeColor(attrRes: Int): Int {
        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(attrRes, typedValue, true)
        return typedValue.data
    }
}