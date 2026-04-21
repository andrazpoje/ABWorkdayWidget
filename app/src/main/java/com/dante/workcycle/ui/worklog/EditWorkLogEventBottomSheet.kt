package com.dante.workcycle.ui.worklog

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.dante.workcycle.R
import com.dante.workcycle.data.repository.RepositoryProvider
import com.dante.workcycle.domain.model.WorkEvent
import com.dante.workcycle.domain.model.WorkEventType
import com.dante.workcycle.widget.base.WidgetRefreshDispatcher
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class EditWorkLogEventBottomSheet(
    private val event: WorkEvent,
    private val onSaved: (() -> Unit)? = null
) : BottomSheetDialogFragment(R.layout.bottom_sheet_edit_work_log_event) {

    private val viewModel: EditWorkLogEventViewModel by viewModels {
        EditWorkLogEventViewModel.Factory(
            event = event,
            repository = RepositoryProvider.workEventRepository(requireContext())
        )
    }

    private lateinit var textTitle: TextView
    private lateinit var textSummary: TextView
    private lateinit var valueDate: TextView
    private lateinit var valueTime: TextView
    private lateinit var buttonDate: MaterialButton
    private lateinit var buttonTime: MaterialButton
    private lateinit var buttonCancel: MaterialButton
    private lateinit var buttonSave: MaterialButton

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textTitle = view.findViewById(R.id.textEditWorkLogEventTitle)
        textSummary = view.findViewById(R.id.textEditWorkLogEventSummary)
        valueDate = view.findViewById(R.id.textEditWorkLogEventDateValue)
        valueTime = view.findViewById(R.id.textEditWorkLogEventTimeValue)
        buttonDate = view.findViewById(R.id.buttonEditWorkLogEventDate)
        buttonTime = view.findViewById(R.id.buttonEditWorkLogEventTime)
        buttonCancel = view.findViewById(R.id.buttonEditWorkLogEventCancel)
        buttonSave = view.findViewById(R.id.buttonEditWorkLogEventSave)

        textTitle.text = getString(
            R.string.work_log_edit_event_title,
            getEventTypeLabel(event.type)
        )
        textSummary.text = getString(
            R.string.work_log_edit_event_summary,
            getEventTypeLabel(event.type)
        )

        buttonDate.setOnClickListener {
            showDatePicker()
        }

        buttonTime.setOnClickListener {
            showTimePicker()
        }

        buttonCancel.setOnClickListener {
            dismiss()
        }

        buttonSave.setOnClickListener {
            save()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                valueDate.text = state.date.format(dateFormatter)
                valueTime.text = state.time.format(timeFormatter)
            }
        }
    }

    private fun showDatePicker() {
        val state = viewModel.uiState.value
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.work_log_edit_event_date_label))
            .setSelection(
                state.date.atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            )
            .build()

        picker.addOnPositiveButtonClickListener { selection ->
            val selectedDate = Instant.ofEpochMilli(selection)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            viewModel.setDate(selectedDate)
        }

        picker.show(parentFragmentManager, "edit_work_log_event_date")
    }

    private fun showTimePicker() {
        val state = viewModel.uiState.value
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(state.time.hour)
            .setMinute(state.time.minute)
            .setTitleText(getString(R.string.work_log_edit_event_time_label))
            .build()

        picker.addOnPositiveButtonClickListener {
            viewModel.setTime(LocalTime.of(picker.hour, picker.minute))
        }

        picker.show(parentFragmentManager, "edit_work_log_event_time")
    }

    private fun save() {
        viewLifecycleOwner.lifecycleScope.launch {
            val validationMessageRes = viewModel.validate()
            if (validationMessageRes != null) {
                Toast.makeText(
                    requireContext(),
                    getString(validationMessageRes),
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }

            viewModel.save()
            WidgetRefreshDispatcher.refreshWorkLogWidgets(requireContext())
            onSaved?.invoke()

            Toast.makeText(
                requireContext(),
                getString(R.string.work_log_edit_event_saved),
                Toast.LENGTH_SHORT
            ).show()

            dismiss()
        }
    }

    private fun getEventTypeLabel(type: WorkEventType): String {
        val resId = when (type) {
            WorkEventType.CLOCK_IN -> R.string.work_log_event_clock_in
            WorkEventType.CLOCK_OUT -> R.string.work_log_event_clock_out_label
            WorkEventType.BREAK_START -> R.string.work_log_event_break_start
            WorkEventType.BREAK_END -> R.string.work_log_event_break_end
            WorkEventType.MEAL -> R.string.work_log_event_meal
            WorkEventType.NOTE -> R.string.work_log_event_note
        }
        return getString(resId)
    }

    data class UiState(
        val date: LocalDate,
        val time: LocalTime
    )

    class EditWorkLogEventViewModel(
        private val originalEvent: WorkEvent,
        private val repository: com.dante.workcycle.data.repository.WorkEventRepository
    ) : ViewModel() {

        private val _uiState = kotlinx.coroutines.flow.MutableStateFlow(
            UiState(
                date = originalEvent.date,
                time = originalEvent.time
            )
        )
        val uiState: kotlinx.coroutines.flow.StateFlow<UiState> = _uiState

        fun setDate(date: LocalDate) {
            _uiState.value = _uiState.value.copy(date = date)
        }

        fun setTime(time: LocalTime) {
            _uiState.value = _uiState.value.copy(time = time)
        }

        suspend fun validate(): Int? {
            val updatedEvent = originalEvent.copy(
                date = uiState.value.date,
                time = uiState.value.time
            )

            val originalDateEvents = repository.getByDate(originalEvent.date)
            val updatedDateEvents = repository.getByDate(updatedEvent.date)

            return WorkLogEventValidator.validateEditedEvent(
                originalEvent = originalEvent,
                updatedEvent = updatedEvent,
                originalDateEvents = originalDateEvents,
                updatedDateEvents = updatedDateEvents
            )
        }

        suspend fun save() {
            repository.update(
                originalEvent.copy(
                    date = uiState.value.date,
                    time = uiState.value.time
                )
            )
        }

        class Factory(
            private val event: WorkEvent,
            private val repository: com.dante.workcycle.data.repository.WorkEventRepository
        ) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return EditWorkLogEventViewModel(
                    originalEvent = event,
                    repository = repository
                ) as T
            }
        }
    }

    companion object {
        private val dateFormatter: DateTimeFormatter =
            DateTimeFormatter.ofPattern("d. MMM yyyy", Locale.getDefault())

        private val timeFormatter: DateTimeFormatter =
            DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    }
}
