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

class EditAssignmentDayBottomSheet(
    private val date: LocalDate,
    private val onSaved: (() -> Unit)? = null
) : DialogFragment() {

    private var savedCycleOverrideLabel: String? = null
    private var draftCycleOverrideLabel: String? = null

    private var savedAssignmentLabel: String? = null
    private var draftAssignmentLabel: String? = null

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

        val canEditCycleOverride = TemplateManager.allowsCycleOverrides(context)
        val assignmentPrefs = AssignmentCyclePrefs(context)
        val isAssignmentEnabled = assignmentPrefs.isEnabled()

        val resolver = DefaultScheduleResolver(context)
        val cycleOverrideRepository = CycleOverrideRepository(context)
        val manualScheduleRepository = ManualScheduleRepository(context)
        val labelsPrefs = AssignmentLabelsPrefs(context)

        val resolved = resolver.resolve(date)
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

        bindSummary(
            view = view,
            resolved = resolved,
            cycle = cycle,
            canEditCycleOverride = canEditCycleOverride,
            isAssignmentEnabled = isAssignmentEnabled
        )

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
            }

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
            ?: resolved.secondaryBaseLabel
            ?: getString(R.string.none_label)

        primary.text = activePrimary
        secondary.text = getString(R.string.day_summary_secondary_value, activeSecondary)

        val hasPrimaryOverride = !draftCycleOverrideLabel.isNullOrBlank()
        val hasSecondaryOverride = !draftAssignmentLabel.isNullOrBlank()

        status.text = when {
            hasPrimaryOverride && hasSecondaryOverride ->
                getString(R.string.day_summary_status_both_overrides)

            hasPrimaryOverride ->
                getString(R.string.day_summary_status_primary_override)

            hasSecondaryOverride ->
                getString(R.string.day_summary_status_secondary_override)

            else ->
                getString(R.string.day_summary_status_default)
        }

        applySummaryCardStyle(card, activePrimary)
    }

    private fun bindSummary(
        view: View,
        resolved: ResolvedDay,
        cycle: List<String>,
        canEditCycleOverride: Boolean,
        isAssignmentEnabled: Boolean
    ) {
        val cycleInfoText = view.findViewById<TextView>(R.id.cycleInfoText)
        val cycleInfoHelperText = view.findViewById<TextView>(R.id.cycleInfoHelperText)
        val assignmentInfoText = view.findViewById<TextView>(R.id.assignmentInfoText)
        val assignmentInfoHelperText = view.findViewById<TextView>(R.id.assignmentInfoHelperText)

        updateCycleInfoText(
            textView = cycleInfoText,
            baseLabel = resolved.baseCycleLabel,
            overrideLabel = draftCycleOverrideLabel
        )

        if (isAssignmentEnabled) {
            assignmentInfoHelperText.isVisible = true
            updateSecondaryInfoText(
                textView = assignmentInfoText,
                baseLabel = resolved.secondaryBaseLabel,
                overrideLabel = draftAssignmentLabel
            )
            assignmentInfoHelperText.text = getString(R.string.day_editor_secondary_helper)
        } else {
            assignmentInfoText.text = getString(R.string.secondary_disabled_short)
            assignmentInfoHelperText.text = ""
            assignmentInfoHelperText.isVisible = false
        }

        cycleInfoHelperText.text = if (canEditCycleOverride) {
            getString(R.string.day_editor_cycle_helper)
        } else {
            getString(R.string.template_cycle_override_locked_message)
        }

        renderCycleStateCards(view, resolved, cycle)
        renderSecondaryStateCards(view, resolved, isAssignmentEnabled)
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

        textCycleOverrideTitle.isVisible = canEditCycleOverride
        cycleChipGroup.isVisible = canEditCycleOverride
        btnClearCycleOverride.isVisible = canEditCycleOverride
        dividerCycleTop.isVisible = canEditCycleOverride
        dividerCycleBottom.isVisible = canEditCycleOverride
        cycleSectionContainer.isVisible = canEditCycleOverride

        if (!canEditCycleOverride) return

        setupCycleChips(
            chipGroup = cycleChipGroup,
            cycle = cycle,
            selectedLabel = draftCycleOverrideLabel
        )

        cycleChipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
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
            draftCycleOverrideLabel = null
            cycleChipGroup.clearCheck()
            refreshCycleUi(view, resolved, cycle)
        }
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
        val systemLabels = selectable.filter { it.isSystem }
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
        val cycleInfoText = view.findViewById<TextView>(R.id.cycleInfoText)

        updateCycleInfoText(
            textView = cycleInfoText,
            baseLabel = resolved.baseCycleLabel,
            overrideLabel = draftCycleOverrideLabel
        )

        renderCycleStateCards(view, resolved, cycle)
        bindDaySummaryCard(view, resolved)
    }

    private fun refreshAssignmentUi(
        view: View,
        resolved: ResolvedDay
    ) {
        val assignmentInfoText = view.findViewById<TextView>(R.id.assignmentInfoText)

        updateSecondaryInfoText(
            textView = assignmentInfoText,
            baseLabel = resolved.secondaryBaseLabel,
            overrideLabel = draftAssignmentLabel
        )

        renderSecondaryStateCards(view, resolved, isAssignmentEnabled = true)
        bindDaySummaryCard(view, resolved)
    }

    private fun renderCycleStateCards(
        view: View,
        resolved: ResolvedDay,
        cycle: List<String>
    ) {
        val textCycleBaseValue = view.findViewById<TextView>(R.id.textCycleBaseValue)
        val textCycleOverrideValue = view.findViewById<TextView>(R.id.textCycleOverrideValue)
        val textCycleActiveValue = view.findViewById<TextView>(R.id.textCycleActiveValue)

        val cardCycleBase = view.findViewById<MaterialCardView>(R.id.cardCycleBase)
        val cardCycleOverride = view.findViewById<MaterialCardView>(R.id.cardCycleOverride)
        val cardCycleActive = view.findViewById<MaterialCardView>(R.id.cardCycleActive)

        val base = resolved.baseCycleLabel
        val override = draftCycleOverrideLabel
        val active = override ?: base

        textCycleBaseValue.text = base
        textCycleOverrideValue.text = override ?: getString(R.string.none_label)
        textCycleActiveValue.text = active

        applyCycleCardStyle(
            card = cardCycleBase,
            valueView = textCycleBaseValue,
            label = base,
            cycle = cycle,
            enabled = true,
            emphasize = false
        )

        applyCycleCardStyle(
            card = cardCycleOverride,
            valueView = textCycleOverrideValue,
            label = override,
            cycle = cycle,
            enabled = !override.isNullOrBlank(),
            emphasize = false
        )

        applyCycleCardStyle(
            card = cardCycleActive,
            valueView = textCycleActiveValue,
            label = active,
            cycle = cycle,
            enabled = true,
            emphasize = true
        )
    }

    private fun renderSecondaryStateCards(
        view: View,
        resolved: ResolvedDay,
        isAssignmentEnabled: Boolean
    ) {
        val textSecondaryBaseValue = view.findViewById<TextView>(R.id.textSecondaryBaseValue)
        val textSecondaryOverrideValue = view.findViewById<TextView>(R.id.textSecondaryOverrideValue)
        val textSecondaryActiveValue = view.findViewById<TextView>(R.id.textSecondaryActiveValue)

        val cardSecondaryBase = view.findViewById<MaterialCardView>(R.id.cardSecondaryBase)
        val cardSecondaryOverride = view.findViewById<MaterialCardView>(R.id.cardSecondaryOverride)
        val cardSecondaryActive = view.findViewById<MaterialCardView>(R.id.cardSecondaryActive)

        if (!isAssignmentEnabled) {
            val disabled = getString(R.string.disabled_label)
            textSecondaryBaseValue.text = disabled
            textSecondaryOverrideValue.text = disabled
            textSecondaryActiveValue.text = disabled

            applyNeutralStateCardStyle(cardSecondaryBase, textSecondaryBaseValue, disabled = true)
            applyNeutralStateCardStyle(cardSecondaryOverride, textSecondaryOverrideValue, disabled = true)
            applyNeutralStateCardStyle(cardSecondaryActive, textSecondaryActiveValue, disabled = true)
            return
        }

        val baseLabel = resolved.secondaryBaseLabel?.trim()?.ifBlank { null }
        val overrideLabel = draftAssignmentLabel?.trim()?.ifBlank { null }
        val activeLabel = overrideLabel ?: baseLabel

        textSecondaryBaseValue.text = baseLabel ?: getString(R.string.none_label)
        textSecondaryOverrideValue.text = overrideLabel ?: getString(R.string.none_label)
        textSecondaryActiveValue.text = activeLabel ?: getString(R.string.none_label)
        textSecondaryActiveValue.textSize = 16f

        applySecondaryStateCardStyle(
            card = cardSecondaryBase,
            valueView = textSecondaryBaseValue,
            label = baseLabel,
            emphasize = false
        )

        applySecondaryStateCardStyle(
            card = cardSecondaryOverride,
            valueView = textSecondaryOverrideValue,
            label = overrideLabel,
            emphasize = false
        )

        applySecondaryStateCardStyle(
            card = cardSecondaryActive,
            valueView = textSecondaryActiveValue,
            label = activeLabel,
            emphasize = true
        )
    }

    private fun applyCycleCardStyle(
        card: MaterialCardView,
        valueView: TextView,
        label: String?,
        cycle: List<String>,
        enabled: Boolean,
        emphasize: Boolean
    ) {
        if (!enabled || label.isNullOrBlank()) {
            card.setCardBackgroundColor(Color.TRANSPARENT)
            card.strokeColor = ColorUtils.setAlphaComponent(Color.BLACK, 28)
            valueView.alpha = 0.78f
            return
        }

        val baseColor = CycleColorHelper.getBackgroundColor(
            context = requireContext(),
            label = label,
            cycle = cycle
        )

        val isDark = isDarkTheme()

        val background = if (emphasize) {
            if (isDark) {
                ColorUtils.blendARGB(baseColor, Color.BLACK, 0.6f)
            } else {
                ColorUtils.blendARGB(baseColor, Color.WHITE, 0.78f)
            }
        } else {
            if (isDark) {
                ColorUtils.blendARGB(baseColor, Color.BLACK, 0.75f)
            } else {
                CycleColorHelper.getSoftTint(baseColor)
            }
        }

        card.setCardBackgroundColor(background)
        card.strokeColor = CycleColorHelper.getSoftStroke(baseColor)
        valueView.alpha = 1f
    }

    private fun applySummaryCardStyle(
        card: MaterialCardView,
        activePrimary: String
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
        card.strokeColor = CycleColorHelper.getSoftStroke(baseColor)
    }

    private fun isDarkTheme(): Boolean {
        val nightModeFlags = resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }

    private fun applySecondaryStateCardStyle(
        card: MaterialCardView,
        valueView: TextView,
        label: String?,
        emphasize: Boolean
    ) {
        if (label.isNullOrBlank()) {
            applyNeutralStateCardStyle(card, valueView, disabled = false)
            return
        }

        val tintBase = if (isDarkTheme()) {
            Color.parseColor("#3A2F4A")
        } else {
            Color.parseColor("#E8E1F5")
        }

        val tint = if (emphasize) {
            ColorUtils.blendARGB(tintBase, Color.WHITE, 0.10f)
        } else {
            ColorUtils.blendARGB(tintBase, Color.WHITE, 0.28f)
        }

        val stroke = ColorUtils.blendARGB(tintBase, Color.BLACK, 0.10f)

        card.setCardBackgroundColor(tint)
        card.strokeColor = stroke
        valueView.alpha = 1f
    }

    private fun applyNeutralStateCardStyle(
        card: MaterialCardView,
        valueView: TextView,
        disabled: Boolean
    ) {
        val neutralBackground = if (disabled) {
            ColorUtils.setAlphaComponent(Color.GRAY, 18)
        } else {
            Color.TRANSPARENT
        }

        val neutralStroke = if (disabled) {
            ColorUtils.setAlphaComponent(Color.GRAY, 70)
        } else {
            ColorUtils.setAlphaComponent(Color.BLACK, 28)
        }

        card.setCardBackgroundColor(neutralBackground)
        card.strokeColor = neutralStroke
        valueView.alpha = if (disabled) 0.72f else 0.90f
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
            isClickable = true
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
            isClickable = true
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