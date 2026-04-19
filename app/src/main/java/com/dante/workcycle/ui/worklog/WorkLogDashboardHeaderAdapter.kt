package com.dante.workcycle.ui.worklog

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dante.workcycle.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class WorkLogDashboardHeaderAdapter(
    private val onPrimaryAction: () -> Unit,
    private val onBreakAction: () -> Unit,
    private val onMealAction: () -> Unit,
    private val onNoteAction: () -> Unit
) : RecyclerView.Adapter<WorkLogDashboardHeaderAdapter.HeaderViewHolder>() {

    private var state: WorkLogDashboardUiState = WorkLogDashboardUiState()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_work_log_dashboard_header, parent, false)
        return HeaderViewHolder(view as ViewGroup)
    }

    override fun onBindViewHolder(holder: HeaderViewHolder, position: Int) {
        holder.bind(state, onPrimaryAction, onBreakAction, onMealAction, onNoteAction)
    }

    override fun getItemCount(): Int = 1

    class HeaderViewHolder(
        root: ViewGroup
    ) : RecyclerView.ViewHolder(root) {

        private val textTodayDate: TextView = root.findViewById(R.id.textTodayDate)
        private val textWorkState: TextView = root.findViewById(R.id.textWorkState)
        private val textWorkStateDetail: TextView = root.findViewById(R.id.textWorkStateDetail)
        private val textStartedAt: TextView = root.findViewById(R.id.textStartedAt)
        private val textWorkedToday: TextView = root.findViewById(R.id.textWorkedToday)
        private val btnPrimaryAction: MaterialButton = root.findViewById(R.id.btnPrimaryAction)

        private val cardActionBreak: MaterialCardView = root.findViewById(R.id.cardActionBreak)
        private val cardActionMeal: MaterialCardView = root.findViewById(R.id.cardActionMeal)
        private val cardActionNote: MaterialCardView = root.findViewById(R.id.cardActionNote)
        private val textBreakAction: TextView = root.findViewById(R.id.textBreakAction)

        fun bind(
            state: WorkLogDashboardUiState,
            onPrimaryAction: () -> Unit,
            onBreakAction: () -> Unit,
            onMealAction: () -> Unit,
            onNoteAction: () -> Unit
        ) {
            textTodayDate.text = state.todayText
            textWorkState.text = state.stateText
            textWorkStateDetail.text = state.stateDetailText
            textStartedAt.text = state.startedAtText
            textWorkedToday.text = state.workedTodayText
            btnPrimaryAction.text = state.primaryButtonText
            textBreakAction.text = state.breakActionText

            btnPrimaryAction.setOnClickListener { onPrimaryAction() }
            cardActionBreak.setOnClickListener { onBreakAction() }
            cardActionMeal.setOnClickListener { onMealAction() }
            cardActionNote.setOnClickListener { onNoteAction() }

            cardActionBreak.alpha = if (state.breakButtonEnabled) 1f else 0.45f
            cardActionBreak.isEnabled = state.breakButtonEnabled

            cardActionMeal.alpha = if (state.canBreak) 1f else 0.45f
            cardActionMeal.isEnabled = state.canBreak
        }
    }
}
