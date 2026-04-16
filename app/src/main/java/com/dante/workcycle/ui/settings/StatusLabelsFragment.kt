package com.dante.workcycle.ui.settings

import android.app.AlertDialog
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.dante.workcycle.R
import com.dante.workcycle.data.prefs.StatusLabelsPrefs
import com.dante.workcycle.databinding.FragmentStatusLabelsBinding
import com.dante.workcycle.domain.model.StatusLabel
import com.dante.workcycle.ui.adapter.StatusLabelsAdapter
import com.dante.workcycle.ui.dialogs.ColorPickerDialog
import com.dante.workcycle.widget.WidgetRefreshHelper

class StatusLabelsFragment : Fragment(R.layout.fragment_status_labels) {

    private var _binding: FragmentStatusLabelsBinding? = null
    private val binding get() = _binding!!

    private lateinit var prefs: StatusLabelsPrefs
    private lateinit var systemAdapter: StatusLabelsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentStatusLabelsBinding.bind(view)
        prefs = StatusLabelsPrefs(requireContext())

        setupToolbar()
        setupList()
        loadData()
    }

    private fun setupToolbar() {
        binding.toolbarStatusLabels.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupList() {
        systemAdapter = StatusLabelsAdapter(
            items = emptyList(),
            prefs = prefs,
            onEdit = { showEditDialog(it) },
            onToggleEnabled = { label, enabled ->
                val updated = prefs.getLabels().map {
                    if (it.name.equals(label.name, ignoreCase = true)) {
                        it.copy(isEnabled = enabled)
                    } else {
                        it
                    }
                }
                prefs.saveLabels(updated)
                loadData()
                WidgetRefreshHelper.refresh(requireContext())
            }
        )

        binding.recyclerSystemLabels.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerSystemLabels.adapter = systemAdapter
        binding.recyclerSystemLabels.setHasFixedSize(false)
    }

    private fun loadData() {
        val system = prefs.getLabels()

        systemAdapter.update(system)

        binding.textSystemSection.visibility = if (system.isEmpty()) View.GONE else View.VISIBLE
        binding.recyclerSystemLabels.visibility = if (system.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showEditDialog(old: StatusLabel) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_secondary_label, null, false)

        val editLabelName = dialogView.findViewById<EditText>(R.id.editLabelName)
        val viewSelectedColor = dialogView.findViewById<View>(R.id.viewSelectedColor)
        val buttonPickColor = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.buttonPickColor)

        var selectedColor = old.color

        editLabelName.setText(prefs.getDisplayName(old))
        editLabelName.isEnabled = false

        applyColorPreview(viewSelectedColor, selectedColor)

        buttonPickColor.setOnClickListener {
            ColorPickerDialog { color ->
                selectedColor = color
                applyColorPreview(viewSelectedColor, selectedColor)
            }.show(parentFragmentManager, "statusLabelColorPicker")
        }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.edit_status_label))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val updated = prefs.getLabels().map {
                    if (it.name.equals(old.name, ignoreCase = true)) {
                        it.copy(color = selectedColor)
                    } else {
                        it
                    }
                }
                prefs.saveLabels(updated)
                loadData()
                WidgetRefreshHelper.refresh(requireContext())
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun applyColorPreview(view: View, color: Int) {
        val drawable = (view.background.mutate() as GradientDrawable)
        drawable.setColor(color)
        view.background = drawable
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}