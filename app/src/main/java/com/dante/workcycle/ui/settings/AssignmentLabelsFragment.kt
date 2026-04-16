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
import com.dante.workcycle.data.prefs.AssignmentLabelsPrefs
import com.dante.workcycle.databinding.FragmentSecondaryLabelsBinding
import com.dante.workcycle.domain.model.AssignmentLabel
import com.dante.workcycle.ui.adapter.AssignmentLabelsAdapter
import com.dante.workcycle.ui.dialogs.ColorPickerDialog
import com.dante.workcycle.widget.WidgetRefreshHelper

class AssignmentLabelsFragment : Fragment(R.layout.fragment_secondary_labels) {

    private var _binding: FragmentSecondaryLabelsBinding? = null
    private val binding get() = _binding!!

    private lateinit var prefs: AssignmentLabelsPrefs
    private lateinit var systemAdapter: AssignmentLabelsAdapter
    private lateinit var manualAdapter: AssignmentLabelsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSecondaryLabelsBinding.bind(view)
        prefs = AssignmentLabelsPrefs(requireContext())

        binding.textDescription.text =
            getString(R.string.secondary_labels_description)

        setupToolbar()
        setupLists()

        binding.btnAdd.setOnClickListener {
            showAddDialog()
        }

        loadData()
    }

    private fun setupToolbar() {
        binding.toolbarSecondaryLabels.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupLists() {
        systemAdapter = AssignmentLabelsAdapter(
            items = emptyList(),
            prefs = prefs,
            onEdit = { showEditDialog(it) },
            onDelete = { deleteLabel(it) },
            onToggleEnabled = { label, enabled ->
                prefs.setEnabled(label.name, enabled)
                loadData()
                WidgetRefreshHelper.refresh(requireContext())
            }
        )

        manualAdapter = AssignmentLabelsAdapter(
            items = emptyList(),
            prefs = prefs,
            onEdit = { showEditDialog(it) },
            onDelete = { deleteLabel(it) },
            onToggleEnabled = { label, enabled ->
                prefs.setEnabled(label.name, enabled)
                loadData()
                WidgetRefreshHelper.refresh(requireContext())
            }
        )

        binding.recyclerSystemLabels.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerSystemLabels.adapter = systemAdapter
        binding.recyclerSystemLabels.setHasFixedSize(false)

        binding.recyclerManualLabels.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerManualLabels.adapter = manualAdapter
        binding.recyclerManualLabels.setHasFixedSize(false)
    }

    private fun loadData() {
        val all = prefs.getLabels()
        val system = all.filter { it.isSystem }
        val manual = all.filterNot { it.isSystem }

        systemAdapter.update(system)
        manualAdapter.update(manual)

        binding.textSystemSection.visibility = if (system.isEmpty()) View.GONE else View.VISIBLE
        binding.recyclerSystemLabels.visibility = if (system.isEmpty()) View.GONE else View.VISIBLE

        binding.textManualSection.visibility = View.VISIBLE
        binding.textManualSectionHelper.visibility = if (manual.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerManualLabels.visibility = if (manual.isEmpty()) View.GONE else View.VISIBLE
    }
    private fun showAddDialog() {
        showLabelDialog(
            title = getString(R.string.add_label),
            initialLabel = null
        ) { newLabel ->
            prefs.addLabel(newLabel)
            loadData()
            WidgetRefreshHelper.refresh(requireContext())
        }
    }

    private fun showEditDialog(old: AssignmentLabel) {
        showLabelDialog(
            title = if (old.isSystem) {
                getString(R.string.edit_system_label)
            } else {
                getString(R.string.edit_label)
            },
            initialLabel = old
        ) { updatedLabel ->
            prefs.updateLabel(old.name, updatedLabel)
            loadData()
            WidgetRefreshHelper.refresh(requireContext())
        }
    }

    private fun deleteLabel(label: AssignmentLabel) {
        if (label.isSystem) return

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete_label))
            .setMessage(getString(R.string.delete_label_message, label.name))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                prefs.deleteLabel(label.name)
                loadData()
                WidgetRefreshHelper.refresh(requireContext())
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showLabelDialog(
        title: String,
        initialLabel: AssignmentLabel?,
        onSave: (AssignmentLabel) -> Unit
    ) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_secondary_label, null, false)

        val editLabelName = dialogView.findViewById<EditText>(R.id.editLabelName)
        val viewSelectedColor = dialogView.findViewById<View>(R.id.viewSelectedColor)
        val buttonPickColor = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.buttonPickColor)

        var selectedColor = initialLabel?.color ?: 0xFF9E9E9E.toInt()

        editLabelName.setText(initialLabel?.name.orEmpty())
        editLabelName.isEnabled = initialLabel?.isSystem != true

        applyColorPreview(viewSelectedColor, selectedColor)

        buttonPickColor.setOnClickListener {
            ColorPickerDialog { color ->
                selectedColor = color
                applyColorPreview(viewSelectedColor, selectedColor)
            }.show(parentFragmentManager, "labelColorPicker")
        }

        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(dialogView)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val text = editLabelName.text?.toString().orEmpty().trim()
                val finalName = if (initialLabel?.isSystem == true) {
                    initialLabel.name
                } else {
                    text
                }

                if (finalName.isNotBlank()) {
                    onSave(
                        AssignmentLabel(
                            name = finalName,
                            color = selectedColor,
                            isSystem = initialLabel?.isSystem ?: false,
                            isEnabled = initialLabel?.isEnabled ?: true,
                            iconKey = initialLabel?.iconKey,
                            usageCount = initialLabel?.usageCount ?: 0,
                            lastUsedAt = initialLabel?.lastUsedAt
                        )
                    )
                }
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