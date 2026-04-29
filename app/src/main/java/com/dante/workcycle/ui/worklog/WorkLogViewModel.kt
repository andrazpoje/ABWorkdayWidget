package com.dante.workcycle.ui.worklog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dante.workcycle.data.repository.WorkLogRepository
import com.dante.workcycle.domain.model.WorkLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * ViewModel for the legacy aggregate Work Log editor.
 *
 * The current dashboard uses the event timeline for session state and audit
 * safety. Keep aggregate editing isolated so it does not conflict with event
 * based totals, widgets, or future multi-session work.
 */
class WorkLogViewModel(
    private val repository: WorkLogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkLogUiState())
    val uiState: StateFlow<WorkLogUiState> = _uiState.asStateFlow()

    private var selectedDate: LocalDate = LocalDate.now()
    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    init {
        loadForDate(LocalDate.now())
    }

    fun loadForDate(date: LocalDate) {
        selectedDate = date

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                dateText = formatDate(date),
                message = null
            )

            val log = repository.getByDate(date)

            if (log == null) {
                _uiState.value = WorkLogUiState(
                    dateText = formatDate(date),
                    breakMinutesText = "0",
                    totalText = "—",
                    isExisting = false,
                    isLoading = false
                )
                return@launch
            }

            _uiState.value = WorkLogUiState(
                dateText = formatDate(date),
                startTimeText = log.startTime?.format(timeFormatter).orEmpty(),
                endTimeText = log.endTime?.format(timeFormatter).orEmpty(),
                breakMinutesText = log.breakMinutes.toString(),
                noteText = log.note.orEmpty(),
                totalText = calculateTotalText(log.startTime, log.endTime, log.breakMinutes),
                isExisting = true,
                isLoading = false
            )
        }
    }

    fun setStartTime(value: String) {
        _uiState.value = _uiState.value.copy(
            startTimeText = value,
            totalText = recalculateFromState()
        )
    }

    fun setEndTime(value: String) {
        _uiState.value = _uiState.value.copy(
            endTimeText = value,
            totalText = recalculateFromState()
        )
    }

    fun setBreakMinutes(value: String) {
        val normalized = value.toIntOrNull()?.coerceAtLeast(0)?.toString()
            ?: if (value.isBlank()) "" else value

        _uiState.value = _uiState.value.copy(
            breakMinutesText = normalized,
            totalText = recalculateFromState()
        )
    }

    fun setNote(value: String) {
        _uiState.value = _uiState.value.copy(noteText = value)
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun save(
        onInvalidTime: () -> Unit,
        onSaved: () -> Unit
    ) {
        viewModelScope.launch {
            val start = parseTimeOrNull(_uiState.value.startTimeText)
            val end = parseTimeOrNull(_uiState.value.endTimeText)

            if (
                (_uiState.value.startTimeText.isNotBlank() && start == null) ||
                (_uiState.value.endTimeText.isNotBlank() && end == null)
            ) {
                onInvalidTime()
                return@launch
            }

            val breakMinutes = (_uiState.value.breakMinutesText.toIntOrNull() ?: 0)
                .coerceAtLeast(0)

            val workLog = WorkLog(
                date = selectedDate,
                startTime = start,
                endTime = end,
                breakMinutes = breakMinutes,
                note = _uiState.value.noteText.ifBlank { null }
            )

            repository.save(workLog)

            _uiState.value = _uiState.value.copy(
                isExisting = true,
                totalText = calculateTotalText(start, end, breakMinutes)
            )

            onSaved()
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteByDate(selectedDate)

            _uiState.value = WorkLogUiState(
                dateText = formatDate(selectedDate),
                breakMinutesText = "0",
                totalText = "—",
                isExisting = false
            )

            onDeleted()
        }
    }

    private fun parseTimeOrNull(value: String): LocalTime? {
        val trimmed = value.trim()
        if (trimmed.isBlank()) return null
        return runCatching { LocalTime.parse(trimmed, timeFormatter) }.getOrNull()
    }

    private fun recalculateFromState(): String {
        val start = parseTimeOrNull(_uiState.value.startTimeText)
        val end = parseTimeOrNull(_uiState.value.endTimeText)
        val breakMinutes = (_uiState.value.breakMinutesText.toIntOrNull() ?: 0)
            .coerceAtLeast(0)
        return calculateTotalText(start, end, breakMinutes)
    }

    private fun calculateTotalText(
        start: LocalTime?,
        end: LocalTime?,
        breakMinutes: Int
    ): String {
        if (start == null || end == null) return "—"

        val startMinutes = start.hour * 60 + start.minute
        var endMinutes = end.hour * 60 + end.minute

        if (endMinutes < startMinutes) {
            endMinutes += 24 * 60
        }

        val totalMinutes = endMinutes - startMinutes - breakMinutes
        if (totalMinutes < 0) return "—"

        val hoursPart = totalMinutes / 60
        val minutesPart = totalMinutes % 60
        return "${hoursPart}h ${minutesPart}m"
    }

    private fun formatDate(date: LocalDate): String {
        return date.format(
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                .withLocale(Locale.getDefault())
        )
    }

    class Factory(
        private val repository: WorkLogRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WorkLogViewModel(repository) as T
        }
    }
}
