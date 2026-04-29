package com.dante.workcycle.ui.worklog

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dante.workcycle.R
import com.dante.workcycle.data.repository.RepositoryProvider
import com.dante.workcycle.ui.components.SlideToConfirmView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class WorkLogDashboardFragment :
    Fragment(R.layout.fragment_work_log_dashboard) {

    private lateinit var viewModel: WorkLogDashboardViewModel
    private lateinit var eventAdapter: WorkEventAdapter

    private lateinit var textTodayDate: TextView
    private lateinit var textWorkState: TextView
    private lateinit var textWorkStateDetail: TextView
    private lateinit var textStartedAt: TextView
    private lateinit var textWorkedToday: TextView
    private lateinit var textBreakAction: TextView
    private lateinit var textExpectedStart: TextView
    private lateinit var textExpectedEnd: TextView
    private lateinit var textStartDeviation: TextView
    private lateinit var textEndDeviation: TextView
    private lateinit var groupStartedAt: View
    private lateinit var groupWorkedToday: View
    private lateinit var groupBreakInfo: View
    private lateinit var groupExpectedTimes: View
    private lateinit var groupExpectedStart: View
    private lateinit var groupExpectedEnd: View
    private lateinit var spacerExpectedTimes: View
    private lateinit var groupTargetAndBalance: View
    private lateinit var groupTarget: View
    private lateinit var groupBalance: View
    private lateinit var groupCreditedTime: View
    private lateinit var spacerTargetBalance: View
    private lateinit var groupSecondaryActions: View
    private lateinit var spacerActionBreak: View

    private lateinit var slidePrimaryAction: SlideToConfirmView

    private lateinit var cardActionBreak: MaterialCardView
    private lateinit var cardActionNote: MaterialCardView
    private lateinit var btnMealAction: com.google.android.material.button.MaterialButton

    private lateinit var groupRecentEventsEmpty: View
    private lateinit var recyclerRecentEvents: RecyclerView

    private lateinit var cardTodayStatus: MaterialCardView

    private lateinit var textBreakStartedAt: TextView
    private lateinit var textBreakDurationLabel: TextView
    private lateinit var textBreakDuration: TextView

    private lateinit var textTargetWork: TextView
    private lateinit var textBalance: TextView
    private lateinit var textCreditedTime: TextView

    private var latestUiState: WorkLogDashboardUiState = WorkLogDashboardUiState()
    private var hasHandledNotificationPermissionPromptThisSession = false

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasHandledNotificationPermissionPromptThisSession = true

            if (!granted) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.work_log_notification_permission_denied_message),
                    Toast.LENGTH_SHORT
                ).show()
            }

            viewModel.onSliderAction()
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val repository = RepositoryProvider.workEventRepository(requireContext())
        viewModel = ViewModelProvider(
            this,
            WorkLogDashboardViewModel.Factory(
                requireActivity().application,
                repository
            )
        )[WorkLogDashboardViewModel::class.java]

        setupViews(view)
        setupRecyclerView()
        setupActions()
        observeUi()
    }

    private fun setupViews(view: View) {
        cardTodayStatus = view.findViewById(R.id.cardTodayStatus)

        textTodayDate = view.findViewById(R.id.textTodayDate)
        textWorkState = view.findViewById(R.id.textWorkState)
        textWorkStateDetail = view.findViewById(R.id.textWorkStateDetail)
        textStartedAt = view.findViewById(R.id.textStartedAt)
        textWorkedToday = view.findViewById(R.id.textWorkedToday)
        textBreakAction = view.findViewById(R.id.textBreakAction)
        textExpectedStart = view.findViewById(R.id.textExpectedStart)
        textExpectedEnd = view.findViewById(R.id.textExpectedEnd)
        textStartDeviation = view.findViewById(R.id.textStartDeviation)
        textEndDeviation = view.findViewById(R.id.textEndDeviation)
        groupStartedAt = view.findViewById(R.id.groupStartedAt)
        groupWorkedToday = view.findViewById(R.id.groupWorkedToday)
        groupBreakInfo = view.findViewById(R.id.groupBreakInfo)
        groupExpectedTimes = view.findViewById(R.id.groupExpectedTimes)
        groupExpectedStart = view.findViewById(R.id.groupExpectedStart)
        groupExpectedEnd = view.findViewById(R.id.groupExpectedEnd)
        spacerExpectedTimes = view.findViewById(R.id.spacerExpectedTimes)
        groupTargetAndBalance = view.findViewById(R.id.groupTargetAndBalance)
        groupTarget = view.findViewById(R.id.groupTarget)
        groupBalance = view.findViewById(R.id.groupBalance)
        groupCreditedTime = view.findViewById(R.id.groupCreditedTime)
        spacerTargetBalance = view.findViewById(R.id.spacerTargetBalance)
        groupSecondaryActions = view.findViewById(R.id.groupSecondaryActions)
        spacerActionBreak = view.findViewById(R.id.spacerActionBreak)

        slidePrimaryAction = view.findViewById(R.id.slidePrimaryAction)

        cardActionBreak = view.findViewById(R.id.cardActionBreak)
        cardActionNote = view.findViewById(R.id.cardActionNote)
        btnMealAction = view.findViewById(R.id.btnMealAction)

        groupRecentEventsEmpty = view.findViewById(R.id.groupRecentEventsEmpty)
        recyclerRecentEvents = view.findViewById(R.id.recyclerRecentEvents)

        textBreakStartedAt = view.findViewById(R.id.textBreakStartedAt)
        textBreakDurationLabel = view.findViewById(R.id.textBreakDurationLabel)
        textBreakDuration = view.findViewById(R.id.textBreakDuration)

        textTargetWork = view.findViewById(R.id.textTargetWork)
        textBalance = view.findViewById(R.id.textBalance)
        textCreditedTime = view.findViewById(R.id.textCreditedTime)
    }

    private fun setupRecyclerView() {
        eventAdapter = WorkEventAdapter { item ->
            showEditEventBottomSheet(item.event)
        }
        recyclerRecentEvents.layoutManager = LinearLayoutManager(requireContext())
        recyclerRecentEvents.adapter = eventAdapter
        recyclerRecentEvents.isNestedScrollingEnabled = false
    }

    private fun setupActions() {
        slidePrimaryAction.setOnSlideCompleteListener {
            handleSliderAction()
        }

        cardActionBreak.setOnClickListener {
            viewModel.onBreakAction()
        }

        btnMealAction.setOnClickListener {
            viewModel.onMealAction()
        }

        cardActionNote.setOnClickListener {
            showAddNoteDialog()
        }
    }

    private fun observeUi() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                latestUiState = state
                textTodayDate.text = state.todayText
                textWorkState.text = state.stateText
                textWorkStateDetail.text = state.stateDetailText
                textStartedAt.text = state.startedAtText
                textWorkedToday.text = state.workedTodayText
                textExpectedStart.text = state.expectedStartText
                textExpectedEnd.text = state.expectedEndText
                textStartDeviation.text = state.startDeviationText
                textEndDeviation.text = state.endDeviationText
                textTargetWork.text = state.targetWorkText
                textBalance.text = state.balanceText
                textCreditedTime.text = state.creditedTimeText
                textBreakStartedAt.text = state.breakStartedAtText
                textBreakDurationLabel.text = state.breakDurationLabelText
                textBreakDuration.text = state.breakDurationText
                textBreakAction.text = state.breakActionText
                btnMealAction.text = state.mealActionText

                slidePrimaryAction.setLabelText(state.sliderActionText)
                slidePrimaryAction.setLeadingIcon(state.sliderIconRes)
                slidePrimaryAction.setHandleIcon(R.drawable.ic_arrow_forward_24)
                slidePrimaryAction.isVisible = state.showPrimaryAction
                slidePrimaryAction.setSlideEnabled(state.sliderEnabled)

                groupStartedAt.isVisible = state.showStartedAt
                groupWorkedToday.isVisible = state.showWorkedToday
                groupBreakInfo.isVisible = state.showBreakInfo
                groupExpectedTimes.isVisible = state.showExpectedStart || state.showExpectedEnd
                groupExpectedStart.isVisible = state.showExpectedStart
                groupExpectedEnd.isVisible = state.showExpectedEnd
                spacerExpectedTimes.isVisible = state.showExpectedStart && state.showExpectedEnd
                groupTargetAndBalance.isVisible =
                    state.showTarget || state.showBalance || state.showCreditedTime
                groupTarget.isVisible = state.showTarget
                groupBalance.isVisible = state.showBalance
                groupCreditedTime.isVisible = state.showCreditedTime
                spacerTargetBalance.isVisible = state.showTarget && state.showBalance
                groupSecondaryActions.isVisible = state.showSecondaryActions
                cardActionBreak.isVisible = state.showBreakActionButton
                spacerActionBreak.isVisible = state.showBreakActionButton

                cardActionBreak.alpha = if (state.breakButtonEnabled) 1f else 0.45f
                cardActionBreak.isEnabled = state.breakButtonEnabled

                btnMealAction.alpha = if (state.mealButtonEnabled) 1f else 0.6f
                btnMealAction.isEnabled = state.mealButtonEnabled

                textStartDeviation.isVisible = state.showStartDeviation
                textEndDeviation.isVisible = state.showEndDeviation
                applyStatusCardStyle(state.visualState)
                applyDeviationTone(textStartDeviation, state.startDeviationTone)
                applyDeviationTone(textEndDeviation, state.endDeviationTone)
                renderRecentEvents(state.recentEvents)

                state.message?.let { message ->
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    viewModel.clearMessage()
                }
            }
        }
    }

    private fun handleSliderAction() {
        if (latestUiState.sliderAction != WorkLogSliderAction.START_WORK) {
            viewModel.onSliderAction()
            return
        }

        latestUiState.startWarning?.let { warning ->
            showStartWarningDialog(warning)
            return
        }

        continueStartWorkFlow()
    }

    private fun continueStartWorkFlow() {
        if (!shouldRequestNotificationPermissionForStartWork()) {
            viewModel.onSliderAction()
            return
        }

        if (hasHandledNotificationPermissionPromptThisSession) {
            viewModel.onSliderAction()
            return
        }

        showNotificationPermissionExplainer()
    }

    private fun showStartWarningDialog(warning: WorkLogStartWarning) {
        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.work_log_start_warning_title))
            .setMessage(getString(R.string.work_log_start_warning_message_multi, warning.reasonText))
            .setNegativeButton(getString(R.string.work_log_start_warning_cancel), null)
            .setPositiveButton(getString(R.string.work_log_start_warning_confirm)) { _, _ ->
                continueStartWorkFlow()
            }

        val removableLabels = warning.removableStatusLabels
        if (removableLabels.isNotEmpty()) {
            val removeButtonText = if (removableLabels.size == 1) {
                getString(
                    R.string.work_log_start_warning_remove_status_and_start,
                    removableLabels.first()
                )
            } else {
                getString(R.string.work_log_start_warning_remove_statuses_and_start)
            }

            builder.setNeutralButton(removeButtonText) { _, _ ->
                viewModel.removeStartWarningStatusesForSelectedDate()
                continueStartWorkFlow()
            }
        }

        builder.show()
    }

    private fun shouldRequestNotificationPermissionForStartWork(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return false
        }

        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
    }

    private fun showNotificationPermissionExplainer() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.work_log_notification_permission_title))
            .setMessage(getString(R.string.work_log_notification_permission_message))
            .setPositiveButton(getString(R.string.work_log_notification_permission_positive)) { _, _ ->
                hasHandledNotificationPermissionPromptThisSession = true
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            .setNegativeButton(getString(R.string.work_log_notification_permission_negative)) { _, _ ->
                hasHandledNotificationPermissionPromptThisSession = true
                viewModel.onSliderAction()
            }
            .show()
    }

    private fun applyStatusCardStyle(visualState: WorkLogDashboardVisualState) {
        val context = requireContext()

        val (containerAttr, outlineAttr, contentAttr) = when (visualState) {
            WorkLogDashboardVisualState.WORKING -> Triple(
                com.google.android.material.R.attr.colorPrimaryContainer,
                androidx.appcompat.R.attr.colorPrimary,
                com.google.android.material.R.attr.colorOnPrimaryContainer
            )

            WorkLogDashboardVisualState.BREAK -> Triple(
                com.google.android.material.R.attr.colorTertiaryContainer,
                com.google.android.material.R.attr.colorTertiary,
                com.google.android.material.R.attr.colorOnTertiaryContainer
            )

            WorkLogDashboardVisualState.FINISHED -> Triple(
                com.google.android.material.R.attr.colorSurfaceVariant,
                com.google.android.material.R.attr.colorOutline,
                com.google.android.material.R.attr.colorOnSurfaceVariant
            )

            WorkLogDashboardVisualState.NOT_STARTED -> Triple(
                com.google.android.material.R.attr.colorSurface,
                com.google.android.material.R.attr.colorOutline,
                com.google.android.material.R.attr.colorOnSurface
            )
        }

        val container = MaterialColors.getColor(
            context,
            containerAttr,
            ContextCompat.getColor(context, android.R.color.black)
        )
        val outline = MaterialColors.getColor(
            context,
            outlineAttr,
            ContextCompat.getColor(context, android.R.color.darker_gray)
        )
        val content = MaterialColors.getColor(
            context,
            contentAttr,
            ContextCompat.getColor(context, android.R.color.white)
        )

        cardTodayStatus.setCardBackgroundColor(container)
        cardTodayStatus.strokeColor = outline
        textTodayDate.setTextColor(content)
        textWorkState.setTextColor(content)
        textWorkStateDetail.setTextColor(content)
        textWorkedToday.setTextColor(content)
    }

    private fun applyDeviationTone(textView: TextView, tone: WorkLogDeviationTone) {
        val colorAttr = when (tone) {
            WorkLogDeviationTone.DEFAULT -> com.google.android.material.R.attr.colorOnSurfaceVariant
            WorkLogDeviationTone.ACCENT -> androidx.appcompat.R.attr.colorPrimary
            WorkLogDeviationTone.ERROR -> androidx.appcompat.R.attr.colorError
        }

        textView.setTextColor(
            MaterialColors.getColor(
                textView,
                colorAttr
            )
        )
    }

    private fun renderRecentEvents(events: List<WorkEventListItem>) {
        val isEmpty = events.isEmpty()
        groupRecentEventsEmpty.isVisible = isEmpty
        recyclerRecentEvents.isVisible = !isEmpty
        eventAdapter.submitList(events)
    }

    private fun showEditEventBottomSheet(event: com.dante.workcycle.domain.model.WorkEvent) {
        EditWorkLogEventBottomSheet(
            event = event
        ).show(childFragmentManager, "edit_work_log_event")
    }

    private fun showAddNoteDialog() {
        val input = EditText(requireContext()).apply {
            hint = getString(R.string.work_log_add_note_hint)
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.work_log_add_note_title))
            .setView(input)
            .setPositiveButton(getString(R.string.work_log_add_note_save)) { _, _ ->
                val note = input.text?.toString().orEmpty().trim()

                if (note.isBlank()) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.work_log_add_note_empty),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                viewModel.onNoteAdded(note)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
}
