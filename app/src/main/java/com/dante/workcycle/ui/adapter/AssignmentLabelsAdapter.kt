package com.dante.workcycle.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.dante.workcycle.R
import com.dante.workcycle.data.prefs.AssignmentLabelsPrefs
import com.dante.workcycle.databinding.ItemLabelBinding
import com.dante.workcycle.domain.model.AssignmentLabel

class AssignmentLabelsAdapter(
    private var items: List<AssignmentLabel>,
    private val prefs: AssignmentLabelsPrefs,
    private val onEdit: (AssignmentLabel) -> Unit,
    private val onDelete: (AssignmentLabel) -> Unit,
    private val onToggleEnabled: (AssignmentLabel, Boolean) -> Unit
) : RecyclerView.Adapter<AssignmentLabelsAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemLabelBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLabelBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val label = items[position]
        val context = holder.binding.root.context

        holder.binding.textLabel.text = prefs.getDisplayName(label)
        holder.binding.textMeta.text = if (label.isSystem) {
            context.getString(R.string.assignment_label_type_system)
        } else {
            context.resources.getQuantityString(
                R.plurals.assignment_label_type_manual_usage,
                label.usageCount,
                label.usageCount
            )
        }

        holder.binding.viewColor.setBackgroundColor(label.color)

        if (label.isSystem) {
            holder.binding.iconSystem.visibility = View.VISIBLE

            val iconRes = when (label.iconKey) {
                "sick" -> R.drawable.ic_assignment_sick_24
                "vacation" -> R.drawable.ic_assignment_vacation_24
                "standby" -> R.drawable.ic_assignment_standby_24
                "field" -> R.drawable.ic_assignment_field_24
                else -> null
            }

            if (iconRes != null) {
                holder.binding.iconSystem.setImageResource(iconRes)
            } else {
                holder.binding.iconSystem.visibility = View.GONE
            }
        } else {
            holder.binding.iconSystem.visibility = View.GONE
        }

        holder.binding.switchEnabled.setOnCheckedChangeListener(null)
        holder.binding.switchEnabled.isChecked = label.isEnabled
        holder.binding.switchEnabled.setOnCheckedChangeListener { _, isChecked ->
            onToggleEnabled(label, isChecked)
        }

        if (label.isSystem) {
            holder.binding.btnEdit.visibility = View.VISIBLE
            holder.binding.btnDelete.visibility = View.GONE
            holder.binding.btnEdit.text = context.getString(R.string.edit)
            holder.binding.btnEdit.setOnClickListener { onEdit(label) }
        } else {
            holder.binding.btnEdit.visibility = View.VISIBLE
            holder.binding.btnDelete.visibility = View.VISIBLE
            holder.binding.btnEdit.text = context.getString(R.string.edit)
            holder.binding.btnEdit.setOnClickListener { onEdit(label) }
            holder.binding.btnDelete.setOnClickListener { onDelete(label) }
        }
    }

    fun update(newItems: List<AssignmentLabel>) {
        val oldItems = items
        items = newItems
        DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = oldItems.size

            override fun getNewListSize(): Int = newItems.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldItems[oldItemPosition].name.equals(
                    newItems[newItemPosition].name,
                    ignoreCase = true
                )
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldItems[oldItemPosition] == newItems[newItemPosition]
            }
        }).dispatchUpdatesTo(this)
    }
}
