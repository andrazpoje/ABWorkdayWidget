package com.dante.workcycle.ui.dialogs

import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.dante.workcycle.R
import com.dante.workcycle.data.prefs.AssignmentLabelsPrefs
import com.dante.workcycle.domain.model.AssignmentLabel
import com.dante.workcycle.domain.schedule.CycleManager
import com.dante.workcycle.domain.schedule.CycleOverrideRepository
import com.dante.workcycle.domain.schedule.DefaultScheduleResolver
import com.dante.workcycle.domain.schedule.ManualScheduleRepository
import com.dante.workcycle.domain.template.TemplateManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
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
        val canEditCycleOverride = TemplateManager.allowsCycleOverrides(context)

        val view = LayoutInflater.from(context)
            .inflate(R.layout.bottom_sheet_edit_secondary_day_v2, null, false)

        val resolver = DefaultScheduleResolver(context)
        val cycleOverrideRepository = CycleOverrideRepository(context)
        val manualScheduleRepository = ManualScheduleRepository(context)
        val labelsPrefs = AssignmentLabelsPrefs(context)

        val dateText = view.findViewById<TextView>(R.id.dateText)
        val cycleInfoText = view.findViewById<TextView>(R.id.cycleInfoText)
        val cycleInfoHelperText = view.findViewById<TextView>(R.id.cycleInfoHelperText)
        val assignmentInfoText = view.findViewById<TextView>(R.id.assignmentInfoText)

        val textCycleOverrideTitle = view.findViewById<TextView>(R.id.textCycleOverrideTitle)
        val cycleChipGroup = view.findViewById<ChipGroup>(R.id.cycleChipGroup)
        val systemChipGroup = view.findViewById<ChipGroup>(R.id.systemChipGroup)
        val manualChipGroup = view.findViewById<ChipGroup>(R.id.manualChipGroup)

        val dividerCycleTop = view.findViewById<MaterialDivider>(R.id.dividerCycleTop)
        val dividerCycleBottom = view.findViewById<MaterialDivider>(R.id.dividerCycleBottom)

        val textSystemAssignments = view.findViewById<TextView>(R.id.textSystemAssignments)
        val textManualAssignments = view.findViewById<TextView>(R.id.textManualAssignments)

        val btnClearCycleOverride = view.findViewById<MaterialButton>(R.id.btnClearCycleOverride)
        val btnClearAssignment = view.findViewById<MaterialButton>(R.id.btnClearAssignment)
        val btnSave = view.findViewById<MaterialButton>(R.id.btnSave)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancel)

        val resolved = resolver.resolve(date)
        val cycle = CycleManager.loadCycle(context)

        savedCycleOverrideLabel = cycleOverrideRepository.getOverrideLabel(date)?.trim()?.ifBlank { null }
        draftCycleOverrideLabel = savedCycleOverrideLabel

        savedAssignmentLabel = manualScheduleRepository.getSecondaryManualLabel(date)?.trim()?.ifBlank { null }
        draftAssignmentLabel = savedAssignmentLabel

        dateText.text = date.format(
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                .withLocale(Locale.getDefault())
        )

        if (!canEditCycleOverride) {
            cycleInfoHelperText.text = getString(R.string.template_cycle_override_locked_message)
        }

        updateCycleInfoText(
            textView = cycleInfoText,
            baseLabel = resolved.baseCycleLabel,
            overrideLabel = draftCycleOverrideLabel
        )

        updateAssignmentInfoText(
            textView = assignmentInfoText,
            label = draftAssignmentLabel ?: resolved.assignmentLabel
        )

        setupCycleChips(
            chipGroup = cycleChipGroup,
            cycle = cycle,
            selectedLabel = draftCycleOverrideLabel
        )

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

        textCycleOverrideTitle.isVisible = canEditCycleOverride
        cycleChipGroup.isVisible = canEditCycleOverride
        btnClearCycleOverride.isVisible = canEditCycleOverride
        dividerCycleTop.isVisible = canEditCycleOverride
        dividerCycleBottom.isVisible = canEditCycleOverride

        cycleChipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            val selectedId = checkedIds.firstOrNull()
            draftCycleOverrideLabel = if (selectedId != null) {
                group.findViewById<Chip>(selectedId)?.text?.toString()?.trim()?.ifBlank { null }
            } else {
                null
            }

            updateCycleInfoText(
                textView = cycleInfoText,
                baseLabel = resolved.baseCycleLabel,
                overrideLabel = draftCycleOverrideLabel
            )
        }

        systemChipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                manualChipGroup.setOnCheckedStateChangeListener(null)
                manualChipGroup.clearCheck()
                manualChipGroup.setOnCheckedStateChangeListener(manualCheckedListener(resolved, assignmentInfoText))
            }

            val selectedId = checkedIds.firstOrNull()
            draftAssignmentLabel = if (selectedId != null) {
                group.findViewById<Chip>(selectedId)?.text?.toString()?.trim()?.ifBlank { null }
            } else if (manualChipGroup.checkedChipId == ViewGroup.NO_ID) {
                null
            } else {
                draftAssignmentLabel
            }

            updateAssignmentInfoText(
                textView = assignmentInfoText,
                label = draftAssignmentLabel ?: resolved.assignmentLabel.takeIf { savedAssignmentLabel == null }
            )
        }

        manualChipGroup.setOnCheckedStateChangeListener(
            manualCheckedListener(
                resolved = resolved,
                assignmentInfoText = assignmentInfoText
            )
        )

        btnClearCycleOverride.setOnClickListener {
            if (!canEditCycleOverride) return@setOnClickListener

            draftCycleOverrideLabel = null
            cycleChipGroup.clearCheck()
            updateCycleInfoText(
                textView = cycleInfoText,
                baseLabel = resolved.baseCycleLabel,
                overrideLabel = null
            )
        }

        btnClearAssignment.setOnClickListener {
            draftAssignmentLabel = null
            systemChipGroup.clearCheck()
            manualChipGroup.clearCheck()
            updateAssignmentInfoText(
                textView = assignmentInfoText,
                label = resolved.assignmentLabel.takeIf { savedAssignmentLabel == null }
            )
        }

        val dialog = BottomSheetDialog(context)
        dialog.setContentView(view)

        btnSave.setOnClickListener {
            if (canEditCycleOverride) {
                cycleOverrideRepository.setOverrideLabel(date, draftCycleOverrideLabel)
            }

            manualScheduleRepository.setSecondaryManualLabel(date, draftAssignmentLabel)

            draftAssignmentLabel
                ?.takeIf { it.isNotBlank() }
                ?.let { labelsPrefs.markLabelUsed(it) }

            onSaved?.invoke()
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        return dialog
    }

    private fun manualCheckedListener(
        resolved: com.dante.workcycle.domain.model.ResolvedDay,
        assignmentInfoText: TextView
    ): ChipGroup.OnCheckedStateChangeListener {
        return ChipGroup.OnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val root = dialog?.findViewById<ChipGroup>(R.id.systemChipGroup)
                root?.setOnCheckedStateChangeListener(null)
                root?.clearCheck()
                root?.setOnCheckedStateChangeListener { g, ids ->
                    if (ids.isNotEmpty()) {
                        val manualGroup = dialog?.findViewById<ChipGroup>(R.id.manualChipGroup)
                        manualGroup?.setOnCheckedStateChangeListener(null)
                        manualGroup?.clearCheck()
                        manualGroup?.setOnCheckedStateChangeListener(manualCheckedListener(resolved, assignmentInfoText))
                    }

                    val selectedId = ids.firstOrNull()
                    draftAssignmentLabel = if (selectedId != null) {
                        g.findViewById<Chip>(selectedId)?.text?.toString()?.trim()?.ifBlank { null }
                    } else if (group.checkedChipId == ViewGroup.NO_ID) {
                        null
                    } else {
                        draftAssignmentLabel
                    }

                    updateAssignmentInfoText(
                        textView = assignmentInfoText,
                        label = draftAssignmentLabel ?: resolved.assignmentLabel.takeIf { savedAssignmentLabel == null }
                    )
                }
            }

            val selectedId = checkedIds.firstOrNull()
            draftAssignmentLabel = if (selectedId != null) {
                group.findViewById<Chip>(selectedId)?.text?.toString()?.trim()?.ifBlank { null }
            } else {
                null
            }

            updateAssignmentInfoText(
                textView = assignmentInfoText,
                label = draftAssignmentLabel ?: resolved.assignmentLabel.takeIf { savedAssignmentLabel == null }
            )
        }
    }

    private fun setupCycleChips(
        chipGroup: ChipGroup,
        cycle: List<String>,
        selectedLabel: String?
    ) {
        chipGroup.removeAllViews()

        cycle.forEach { label ->
            val chip = createSimpleChip(
                text = label,
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

    private fun updateAssignmentInfoText(
        textView: TextView,
        label: String?
    ) {
        val display = label?.trim()?.ifBlank { null } ?: getString(R.string.none_label)
        textView.text = getString(R.string.bottom_sheet_assignment_info, display)
    }

    private fun createSimpleChip(
        text: String,
        isSelected: Boolean
    ): Chip {
        return Chip(requireContext()).apply {
            id = ViewGroup.generateViewId()
            this.text = text
            isCheckable = true
            isClickable = true
            isChecked = isSelected

            layoutParams = ChipGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
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
}