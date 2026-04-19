package com.dante.workcycle.ui.worklog

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dante.workcycle.R
import com.dante.workcycle.data.repository.RepositoryProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import androidx.core.content.ContextCompat
import com.google.android.material.color.MaterialColors

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

    private lateinit var btnPrimaryAction: MaterialButton

    private lateinit var cardActionBreak: MaterialCardView
    private lateinit var cardActionMeal: MaterialCardView
    private lateinit var cardActionNote: MaterialCardView

    private lateinit var textRecentEventsEmpty: TextView
    private lateinit var recyclerRecentEvents: RecyclerView

    private lateinit var cardTodayStatus: MaterialCardView

    private lateinit var textBreakStartedAt: TextView
    private lateinit var textBreakDuration: TextView

    private lateinit var textMealAction: TextView

    private lateinit var textTargetWork: TextView
    private lateinit var textBalance: TextView


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

        btnPrimaryAction = view.findViewById(R.id.btnPrimaryAction)

        cardActionBreak = view.findViewById(R.id.cardActionBreak)
        cardActionMeal = view.findViewById(R.id.cardActionMeal)
        cardActionNote = view.findViewById(R.id.cardActionNote)

        textRecentEventsEmpty = view.findViewById(R.id.textRecentEventsEmpty)
        recyclerRecentEvents = view.findViewById(R.id.recyclerRecentEvents)

        textBreakStartedAt = view.findViewById(R.id.textBreakStartedAt)
        textBreakDuration = view.findViewById(R.id.textBreakDuration)
        textMealAction = view.findViewById(R.id.textMealAction)

        textTargetWork = view.findViewById(R.id.textTargetWork)
        textBalance = view.findViewById(R.id.textBalance)
    }

    private fun setupRecyclerView() {
        eventAdapter = WorkEventAdapter()
        recyclerRecentEvents.layoutManager = LinearLayoutManager(requireContext())
        recyclerRecentEvents.adapter = eventAdapter
    }

    private fun setupActions() {
        btnPrimaryAction.setOnClickListener {
            viewModel.onPrimaryAction()
        }

        cardActionBreak.setOnClickListener {
            viewModel.onBreakAction()
        }

        cardActionMeal.setOnClickListener {
            viewModel.onMealAction()
        }

        cardActionNote.setOnClickListener {
            showAddNoteDialog()
        }
    }

    private fun observeUi() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                textTodayDate.text = state.todayText
                textWorkState.text = state.stateText
                textWorkStateDetail.text = state.stateDetailText
                textStartedAt.text = state.startedAtText
                textWorkedToday.text = state.workedTodayText
                textTargetWork.text = state.targetWorkText
                textBalance.text = state.balanceText
                textBreakStartedAt.text = state.breakStartedAtText
                textBreakDuration.text = state.breakDurationText
                textBreakAction.text = state.breakActionText
                textMealAction.text = state.mealActionText
                btnPrimaryAction.text = state.primaryButtonText

                cardActionBreak.alpha = if (state.breakButtonEnabled) 1f else 0.45f
                cardActionBreak.isEnabled = state.breakButtonEnabled

                cardActionMeal.alpha = if (state.mealButtonEnabled) 1f else 0.45f
                cardActionMeal.isEnabled = state.mealButtonEnabled

                applyStatusCardStyle(isOnBreak = state.isOnBreak)

                renderRecentEvents(state.recentEvents)

                state.message?.let { message ->
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    viewModel.clearMessage()
                }
            }
        }
    }

    private fun applyStatusCardStyle(isOnBreak: Boolean) {
        val context = requireContext()

        if (isOnBreak) {
            val breakContainer = MaterialColors.getColor(
                context,
                com.google.android.material.R.attr.colorTertiaryContainer,
                ContextCompat.getColor(context, android.R.color.darker_gray)
            )
            val breakOutline = MaterialColors.getColor(
                context,
                com.google.android.material.R.attr.colorTertiary,
                ContextCompat.getColor(context, android.R.color.white)
            )

            cardTodayStatus.setCardBackgroundColor(breakContainer)
            cardTodayStatus.strokeColor = breakOutline
        } else {
            val surface = MaterialColors.getColor(
                context,
                com.google.android.material.R.attr.colorSurface,
                ContextCompat.getColor(context, android.R.color.black)
            )
            val outline = MaterialColors.getColor(
                context,
                com.google.android.material.R.attr.colorOutline,
                ContextCompat.getColor(context, android.R.color.darker_gray)
            )

            cardTodayStatus.setCardBackgroundColor(surface)
            cardTodayStatus.strokeColor = outline
        }
    }

    private fun renderRecentEvents(events: List<String>) {
        val isEmpty = events.isEmpty()
        textRecentEventsEmpty.isVisible = isEmpty
        recyclerRecentEvents.isVisible = !isEmpty
        eventAdapter.submitList(events)
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