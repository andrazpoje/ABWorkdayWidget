package com.dante.workcycle.ui.worklog

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.dante.workcycle.R
import com.dante.workcycle.data.repository.RepositoryProvider
import com.dante.workcycle.widget.base.WidgetRefreshDispatcher
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

class WorkLogDebugFragment : Fragment(R.layout.fragment_work_log_debug) {

    private lateinit var viewModel: WorkLogViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val repository = RepositoryProvider.workLogRepository(requireContext())
        viewModel = ViewModelProvider(
            this,
            WorkLogViewModel.Factory(repository)
        )[WorkLogViewModel::class.java]

        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbarWorkLog)
        val textDate = view.findViewById<TextView>(R.id.textWorkLogDate)
        val textTotal = view.findViewById<TextView>(R.id.textTotal)

        val editStartTime = view.findViewById<EditText>(R.id.editStartTime)
        val editEndTime = view.findViewById<EditText>(R.id.editEndTime)
        val editBreakMinutes = view.findViewById<EditText>(R.id.editBreakMinutes)
        val editNote = view.findViewById<EditText>(R.id.editNote)

        val btnLoadToday = view.findViewById<MaterialButton>(R.id.btnLoadToday)
        val btnNowStart = view.findViewById<MaterialButton>(R.id.btnNowStart)
        val btnNowEnd = view.findViewById<MaterialButton>(R.id.btnNowEnd)
        val btnSave = view.findViewById<MaterialButton>(R.id.btnSave)
        val btnDelete = view.findViewById<MaterialButton>(R.id.btnDelete)

        val chipBreak0 = view.findViewById<Chip>(R.id.chipBreak0)
        val chipBreak30 = view.findViewById<Chip>(R.id.chipBreak30)
        val chipBreak45 = view.findViewById<Chip>(R.id.chipBreak45)

        toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        editStartTime.setOnClickListener {
            showTimePicker(
                initial = editStartTime.text?.toString().orEmpty()
            ) { selected ->
                editStartTime.setText(selected)
                viewModel.setStartTime(selected)
            }
        }

        editEndTime.setOnClickListener {
            showTimePicker(
                initial = editEndTime.text?.toString().orEmpty()
            ) { selected ->
                editEndTime.setText(selected)
                viewModel.setEndTime(selected)
            }
        }

        editBreakMinutes.addSimpleWatcher { viewModel.setBreakMinutes(it) }
        editNote.addSimpleWatcher { viewModel.setNote(it) }

        btnLoadToday.setOnClickListener {
            viewModel.loadForDate(LocalDate.now())
        }

        btnNowStart.setOnClickListener {
            val now = formatNow()
            editStartTime.setText(now)
            viewModel.setStartTime(now)
        }

        btnNowEnd.setOnClickListener {
            val now = formatNow()
            editEndTime.setText(now)
            viewModel.setEndTime(now)
        }

        chipBreak0.setOnClickListener {
            editBreakMinutes.setText(getString(R.string.work_log_break_option_0))
            viewModel.setBreakMinutes("0")
        }

        chipBreak30.setOnClickListener {
            editBreakMinutes.setText(getString(R.string.work_log_break_option_30))
            viewModel.setBreakMinutes("30")
        }

        chipBreak45.setOnClickListener {
            editBreakMinutes.setText(getString(R.string.work_log_break_option_45))
            viewModel.setBreakMinutes("45")
        }

        btnSave.setOnClickListener {
            viewModel.save(
                onInvalidTime = {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.work_log_invalid_time),
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onSaved = {
                    WidgetRefreshDispatcher.refreshWorkLogWidgets(requireContext())
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.work_log_saved),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }

        btnDelete.setOnClickListener {
            viewModel.delete {
                WidgetRefreshDispatcher.refreshWorkLogWidgets(requireContext())
                Toast.makeText(
                    requireContext(),
                    getString(R.string.work_log_deleted),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                textDate.text = state.dateText
                textTotal.text = getString(R.string.work_log_total_value, state.totalText)

                if (editStartTime.text?.toString() != state.startTimeText) {
                    editStartTime.setText(state.startTimeText)
                }
                if (editEndTime.text?.toString() != state.endTimeText) {
                    editEndTime.setText(state.endTimeText)
                }
                if (editBreakMinutes.text?.toString() != state.breakMinutesText) {
                    editBreakMinutes.setText(state.breakMinutesText)
                }
                if (editNote.text?.toString() != state.noteText) {
                    editNote.setText(state.noteText)
                }

                btnDelete.isEnabled = state.isExisting

                when (state.breakMinutesText) {
                    "0" -> chipBreak0.isChecked = true
                    "30" -> chipBreak30.isChecked = true
                    "45" -> chipBreak45.isChecked = true
                    else -> {
                        chipBreak0.isChecked = false
                        chipBreak30.isChecked = false
                        chipBreak45.isChecked = false
                    }
                }
            }
        }
    }

    private fun showTimePicker(
        initial: String,
        onTimeSelected: (String) -> Unit
    ) {
        val initialTime = parseInitialTime(initial)

        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(initialTime.first)
            .setMinute(initialTime.second)
            .setTitleText(getString(R.string.work_log_select_time))
            .build()

        picker.addOnPositiveButtonClickListener {
            val formatted = String.format(
                Locale.getDefault(),
                "%02d:%02d",
                picker.hour,
                picker.minute
            )
            onTimeSelected(formatted)
        }

        picker.show(parentFragmentManager, "time_picker")
    }

    private fun parseInitialTime(value: String): Pair<Int, Int> {
        val trimmed = value.trim()

        if (trimmed.isNotBlank()) {
            return try {
                val time = LocalTime.parse(trimmed)
                time.hour to time.minute
            } catch (_: Exception) {
                currentTimePair()
            }
        }

        return currentTimePair()
    }

    private fun currentTimePair(): Pair<Int, Int> {
        val now = LocalTime.now()
        return now.hour to now.minute
    }

    private fun formatNow(): String {
        val now = LocalTime.now()
        return String.format(
            Locale.getDefault(),
            "%02d:%02d",
            now.hour,
            now.minute
        )
    }

    private fun EditText.addSimpleWatcher(onChanged: (String) -> Unit) {
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                onChanged(s?.toString().orEmpty())
            }
        })
    }
}
