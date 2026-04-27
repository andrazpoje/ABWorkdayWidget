package com.dante.workcycle.ui.onboarding

import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.content.edit
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dante.workcycle.R
import com.dante.workcycle.data.prefs.AppPrefs
import com.dante.workcycle.data.prefs.LaunchPrefs
import com.dante.workcycle.databinding.FragmentOnboardingCycleSetupBinding
import com.dante.workcycle.domain.schedule.CycleManager
import com.dante.workcycle.domain.template.ScheduleTemplate
import com.dante.workcycle.domain.template.ScheduleTemplateProvider
import com.dante.workcycle.domain.template.TemplateManager
import com.dante.workcycle.ui.template.TemplatePickerBottomSheet
import com.dante.workcycle.widget.WidgetRefreshHelper
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class CycleSetupOnboardingFragment : Fragment(R.layout.fragment_onboarding_cycle_setup) {

    private enum class Step {
        TEMPLATE,
        START_DATE,
        FIRST_LABEL,
        DONE
    }

    private var _binding: FragmentOnboardingCycleSetupBinding? = null
    private val binding get() = _binding!!

    private var currentStep = Step.TEMPLATE
    private var selectedTemplateId: String? = ScheduleTemplateProvider.TEMPLATE_SINGLE_SHIFT
    private var selectedStartDate: LocalDate = LocalDate.now()
    private var selectedFirstLabel: String = ""

    private val dateFormatter: DateTimeFormatter by lazy {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentOnboardingCycleSetupBinding.bind(view)

        initializeSelection()
        setupClicks()
        setupBackHandling()
        render()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun initializeSelection() {
        val template = selectedTemplate()
        selectedStartDate = template?.fixedStartDate ?: LocalDate.now()
        selectedFirstLabel = template?.resolveFixedFirstCycleDay(requireContext())
            ?: labelsForCurrentSelection().first()
    }

    private fun setupClicks() {
        binding.onboardingBackButton.setOnClickListener {
            moveBack()
        }

        binding.onboardingNextButton.setOnClickListener {
            moveNext()
        }

        binding.onboardingFinishButton.setOnClickListener {
            finishOnboarding()
        }
    }

    private fun setupBackHandling() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (stepsForCurrentSelection().indexOf(currentStep) > 0) {
                        moveBack()
                    }
                }
            }
        )
    }

    private fun showTemplatePicker() {
        if (parentFragmentManager.findFragmentByTag(TEMPLATE_PICKER_TAG) != null) return

        TemplatePickerBottomSheet(
            sections = buildTemplatePickerSections(),
            selectedTemplateId = selectedTemplateId ?: CUSTOM_TEMPLATE_ID,
            onTemplateSelected = { templateId ->
                applyTemplateSelection(templateId)
            }
        ).show(parentFragmentManager, TEMPLATE_PICKER_TAG)
    }

    private fun buildTemplatePickerSections(): List<TemplatePickerBottomSheet.Section> {
        val customItem = TemplatePickerBottomSheet.Item(
            templateId = CUSTOM_TEMPLATE_ID,
            title = getString(R.string.template_none),
            description = getString(R.string.onboarding_custom_template_description)
        )

        return listOf(
            TemplatePickerBottomSheet.Section(
                title = shortTemplateGroupTitle(getString(R.string.template_group_general)),
                items = listOf(customItem) +
                    ScheduleTemplateProvider.getGeneralTemplates().map(::toTemplatePickerItem)
            ),
            TemplatePickerBottomSheet.Section(
                title = shortTemplateGroupTitle(getString(R.string.template_group_special)),
                items = ScheduleTemplateProvider.getSpecialTemplates().map(::toTemplatePickerItem)
            )
        )
    }

    private fun toTemplatePickerItem(template: ScheduleTemplate): TemplatePickerBottomSheet.Item {
        return TemplatePickerBottomSheet.Item(
            templateId = template.id,
            title = getString(template.getPickerTitleRes()),
            description = getString(template.getPickerDescriptionRes())
        )
    }

    private fun applyTemplateSelection(templateId: String) {
        selectedTemplateId = templateId.takeIf { it != CUSTOM_TEMPLATE_ID }

        val template = selectedTemplate()
        selectedStartDate = template?.fixedStartDate ?: LocalDate.now()
        selectedFirstLabel = template?.resolveFixedFirstCycleDay(requireContext())
            ?: labelsForCurrentSelection().first()

        currentStep = Step.TEMPLATE
        render()
    }

    private fun showStartDatePicker() {
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                selectedStartDate = LocalDate.of(year, month + 1, dayOfMonth)
                render()
            },
            selectedStartDate.year,
            selectedStartDate.monthValue - 1,
            selectedStartDate.dayOfMonth
        ).show()
    }

    private fun moveNext() {
        val steps = stepsForCurrentSelection()
        val nextIndex = steps.indexOf(currentStep) + 1
        if (nextIndex in steps.indices) {
            currentStep = steps[nextIndex]
            render()
        }
    }

    private fun moveBack() {
        val steps = stepsForCurrentSelection()
        val previousIndex = steps.indexOf(currentStep) - 1
        if (previousIndex in steps.indices) {
            currentStep = steps[previousIndex]
            render()
        }
    }

    private fun render() {
        val steps = stepsForCurrentSelection()
        if (currentStep !in steps) currentStep = steps.last()

        val currentIndex = steps.indexOf(currentStep)
        binding.onboardingTitle.setText(R.string.onboarding_welcome_title)
        binding.onboardingSubtitle.setText(R.string.onboarding_welcome_subtitle)
        binding.onboardingProgressText.text = getString(
            R.string.onboarding_step_count_format,
            currentIndex + 1,
            steps.size
        )
        binding.onboardingProgress.setProgressCompat(
            ((currentIndex + 1) * 100) / steps.size,
            true
        )

        binding.onboardingStepsContainer.removeAllViews()
        steps.forEachIndexed { index, step ->
            binding.onboardingStepsContainer.addView(
                createStepCard(
                    step = step,
                    index = index,
                    total = steps.size,
                    state = when {
                        index < currentIndex -> StepState.COMPLETED
                        index == currentIndex -> StepState.ACTIVE
                        else -> StepState.FUTURE
                    }
                )
            )
        }

        renderButtons()
    }

    private fun createStepCard(
        step: Step,
        index: Int,
        total: Int,
        state: StepState
    ): MaterialCardView {
        val context = requireContext()
        val isActive = state == StepState.ACTIVE
        val isCompleted = state == StepState.COMPLETED
        val primaryColor = resolveThemeColor(androidx.appcompat.R.attr.colorPrimary)
        val onSurface = resolveThemeColor(com.google.android.material.R.attr.colorOnSurface)
        val onSurfaceVariant = resolveThemeColor(com.google.android.material.R.attr.colorOnSurfaceVariant)
        val surface = resolveThemeColor(com.google.android.material.R.attr.colorSurface)

        val card = MaterialCardView(context).apply {
            radius = dp(18).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(surface)
            strokeWidth = if (isActive) dp(1) else 0
            strokeColor = if (isActive) primaryColor else ColorUtils.setAlphaComponent(onSurfaceVariant, 70)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(10)
            }
        }

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }

        content.addView(
            TextView(context).apply {
                text = when {
                    isCompleted -> getString(
                        R.string.onboarding_completed_step_format,
                        getString(step.titleRes())
                    )
                    isActive -> getString(
                        R.string.onboarding_active_step_title_format,
                        getString(R.string.onboarding_step_count_format, index + 1, total),
                        getString(step.titleRes())
                    )
                    else -> getString(step.titleRes())
                }
                textSize = if (isActive) 16f else 14f
                typeface = Typeface.DEFAULT_BOLD
                includeFontPadding = false
                setTextColor(if (state == StepState.FUTURE) onSurfaceVariant else onSurface)
            }
        )

        when (state) {
            StepState.COMPLETED -> {
                content.addText(
                    text = completedSummary(step),
                    textSize = 13f,
                    color = onSurfaceVariant,
                    topMargin = 6
                )
            }
            StepState.ACTIVE -> {
                content.addText(
                    text = getString(step.subtitleRes()),
                    textSize = 13f,
                    color = onSurfaceVariant,
                    topMargin = 6
                )
                addActiveStepContent(content, step)
            }
            StepState.FUTURE -> {
                content.alpha = 0.62f
            }
        }

        card.addView(content)
        return card
    }

    private fun addActiveStepContent(content: LinearLayout, step: Step) {
        when (step) {
            Step.TEMPLATE -> addTemplateContent(content)
            Step.START_DATE -> addStartDateContent(content)
            Step.FIRST_LABEL -> addFirstLabelContent(content)
            Step.DONE -> addDoneContent(content)
        }
    }

    private fun addTemplateContent(content: LinearLayout) {
        val template = selectedTemplate()
        val onSurface = resolveThemeColor(com.google.android.material.R.attr.colorOnSurface)
        val onSurfaceVariant = resolveThemeColor(com.google.android.material.R.attr.colorOnSurfaceVariant)

        content.addText(
            text = template?.let { getString(it.getPickerTitleRes()) } ?: getString(R.string.template_none),
            textSize = 16f,
            color = onSurface,
            topMargin = 14,
            bold = true
        )
        content.addText(
            text = template?.let { getString(it.getPickerDescriptionRes()) }
                ?: getString(R.string.onboarding_custom_template_description),
            textSize = 13f,
            color = onSurfaceVariant,
            topMargin = 5
        )
        content.addView(
            MaterialButton(requireContext()).apply {
                setText(R.string.onboarding_choose_template)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(12)
                }
                setOnClickListener { showTemplatePicker() }
            }
        )
    }

    private fun addStartDateContent(content: LinearLayout) {
        val onSurface = resolveThemeColor(com.google.android.material.R.attr.colorOnSurface)
        content.addText(
            text = getString(
                R.string.onboarding_summary_start_date_format,
                selectedStartDate.format(dateFormatter)
            ),
            textSize = 16f,
            color = onSurface,
            topMargin = 14,
            bold = true
        )
        content.addView(
            MaterialButton(requireContext()).apply {
                setText(R.string.select_date)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(12)
                }
                setOnClickListener { showStartDatePicker() }
            }
        )
    }

    private fun addFirstLabelContent(content: LinearLayout) {
        val labels = labelsForCurrentSelection().distinct()
        val onSurface = resolveThemeColor(com.google.android.material.R.attr.colorOnSurface)

        if (selectedFirstLabel !in labels) {
            selectedFirstLabel = labels.first()
        }

        content.addText(
            text = getString(R.string.onboarding_summary_first_label_format, selectedFirstLabel),
            textSize = 16f,
            color = onSurface,
            topMargin = 14,
            bold = true
        )

        val chipGroup = ChipGroup(requireContext()).apply {
            isSingleSelection = true
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(10)
            }
        }

        labels.forEach { label ->
            val chip = Chip(requireContext()).apply {
                id = View.generateViewId()
                text = label
                isCheckable = true
                isChecked = label == selectedFirstLabel
                setOnClickListener {
                    selectedFirstLabel = label
                    render()
                }
            }
            chipGroup.addView(chip)
            if (chip.isChecked) chipGroup.check(chip.id)
        }

        content.addView(chipGroup)
    }

    private fun addDoneContent(content: LinearLayout) {
        val onSurface = resolveThemeColor(com.google.android.material.R.attr.colorOnSurface)
        content.addText(
            text = summaryLines().joinToString(separator = "\n"),
            textSize = 14f,
            color = onSurface,
            topMargin = 14
        )
    }

    private fun completedSummary(step: Step): String {
        return when (step) {
            Step.TEMPLATE -> getString(
                R.string.onboarding_summary_template_format,
                selectedTemplateName()
            )
            Step.START_DATE -> getString(
                R.string.onboarding_summary_start_date_format,
                selectedStartDate.format(dateFormatter)
            )
            Step.FIRST_LABEL -> getString(
                R.string.onboarding_summary_first_label_format,
                selectedFirstLabel
            )
            Step.DONE -> getString(R.string.onboarding_completed)
        }
    }

    private fun summaryLines(): List<String> {
        return listOf(
            getString(R.string.onboarding_summary_template_format, selectedTemplateName()),
            getString(
                R.string.onboarding_summary_start_date_format,
                selectedStartDate.format(dateFormatter)
            ),
            getString(R.string.onboarding_summary_first_label_format, selectedFirstLabel)
        )
    }

    private fun renderButtons() {
        val steps = stepsForCurrentSelection()
        val currentIndex = steps.indexOf(currentStep)
        val isDone = currentStep == Step.DONE

        binding.onboardingBackButton.isVisible = currentIndex > 0
        binding.onboardingNextButton.isVisible = !isDone
        binding.onboardingFinishButton.isVisible = isDone
    }

    private fun finishOnboarding() {
        val context = requireContext()
        val template = selectedTemplate()
        val templateId = selectedTemplateId

        if (templateId != null) {
            TemplateManager.applyTemplate(context, templateId)
        } else {
            TemplateManager.clearTemplate(context)
            CycleManager.saveCycle(context, labelsForCurrentSelection())
        }

        if (template?.allowsStartDateEditing != false) {
            CycleManager.saveStartDate(context, selectedStartDate)
            context.getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE).edit {
                putInt(AppPrefs.KEY_START_YEAR, selectedStartDate.year)
                putInt(AppPrefs.KEY_START_MONTH, selectedStartDate.monthValue)
                putInt(AppPrefs.KEY_START_DAY, selectedStartDate.dayOfMonth)
            }
        }

        if (template?.locksCycleEditing != true) {
            context.getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE).edit {
                putString(AppPrefs.KEY_FIRST_CYCLE_DAY, selectedFirstLabel)
            }
        }

        LaunchPrefs(context).apply {
            setOnboardingCompleted(true)
            markWhatsNewSeen()
        }
        WidgetRefreshHelper.refresh(context)

        if (!findNavController().popBackStack(R.id.homeFragment, false)) {
            findNavController().navigate(R.id.homeFragment)
        }
    }

    private fun stepsForCurrentSelection(): List<Step> {
        val steps = mutableListOf(Step.TEMPLATE)
        val template = selectedTemplate()

        if (template?.allowsStartDateEditing != false) {
            steps += Step.START_DATE
        }
        if (template?.locksCycleEditing != true) {
            steps += Step.FIRST_LABEL
        }

        steps += Step.DONE
        return steps
    }

    private fun labelsForCurrentSelection(): List<String> {
        return selectedTemplate()?.resolveFixedCycle(requireContext())
            ?: CycleManager.loadCycle(requireContext()).ifEmpty { listOf("A", "B") }
    }

    private fun selectedTemplate(): ScheduleTemplate? {
        return ScheduleTemplateProvider.getById(selectedTemplateId)
    }

    private fun selectedTemplateName(): String {
        return selectedTemplate()?.let {
            getString(it.getPickerTitleRes())
        } ?: getString(R.string.template_none)
    }

    private fun Step.titleRes(): Int {
        return when (this) {
            Step.TEMPLATE -> R.string.onboarding_template_title
            Step.START_DATE -> R.string.onboarding_start_date_title
            Step.FIRST_LABEL -> R.string.onboarding_first_label_title
            Step.DONE -> R.string.onboarding_done_title
        }
    }

    private fun Step.subtitleRes(): Int {
        return when (this) {
            Step.TEMPLATE -> R.string.onboarding_template_subtitle
            Step.START_DATE -> R.string.onboarding_start_date_subtitle
            Step.FIRST_LABEL -> R.string.onboarding_first_label_subtitle
            Step.DONE -> R.string.onboarding_done_subtitle
        }
    }

    private fun shortTemplateGroupTitle(title: String): String {
        return title.substringBefore(" ").uppercase(Locale.getDefault())
    }

    private fun LinearLayout.addText(
        text: String,
        textSize: Float,
        color: Int,
        topMargin: Int = 0,
        bold: Boolean = false
    ) {
        addView(
            TextView(requireContext()).apply {
                this.text = text
                this.textSize = textSize
                includeFontPadding = false
                setTextColor(color)
                if (bold) typeface = Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    this.topMargin = dp(topMargin)
                }
            }
        )
    }

    private fun resolveThemeColor(attrRes: Int): Int {
        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(attrRes, typedValue, true)
        return typedValue.data
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun ScheduleTemplate.getPickerTitleRes(): Int {
        return when (id) {
            ScheduleTemplateProvider.TEMPLATE_POSTA_SLOVENIJE_AB ->
                R.string.template_posta_slovenije_picker_title
            else -> titleRes
        }
    }

    private fun ScheduleTemplate.getPickerDescriptionRes(): Int {
        return when (id) {
            ScheduleTemplateProvider.TEMPLATE_SINGLE_SHIFT ->
                R.string.template_single_shift_picker_description
            ScheduleTemplateProvider.TEMPLATE_TWO_SHIFT ->
                R.string.template_two_shift_picker_description
            ScheduleTemplateProvider.TEMPLATE_THREE_SHIFT ->
                R.string.template_three_shift_picker_description
            ScheduleTemplateProvider.TEMPLATE_AB ->
                R.string.template_ab_picker_description
            ScheduleTemplateProvider.TEMPLATE_POSTA_SLOVENIJE_AB ->
                R.string.template_posta_slovenije_picker_description
            else -> descriptionRes
        }
    }

    private enum class StepState {
        COMPLETED,
        ACTIVE,
        FUTURE
    }

    companion object {
        private const val CUSTOM_TEMPLATE_ID = "custom_cycle"
        private const val TEMPLATE_PICKER_TAG = "cycleSetupTemplatePicker"
    }
}
