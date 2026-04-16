package com.dante.workcycle.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dante.workcycle.R
import com.dante.workcycle.data.prefs.StatusLabelsPrefs
import com.dante.workcycle.databinding.ItemLabelBinding
import com.dante.workcycle.domain.model.StatusLabel
import android.graphics.Typeface

class StatusLabelsAdapter(
    private var items: List<StatusLabel>,
    private val prefs: StatusLabelsPrefs,
    private val onEdit: (StatusLabel) -> Unit,
    private val onToggleEnabled: (StatusLabel, Boolean) -> Unit
) : RecyclerView.Adapter<StatusLabelsAdapter.ViewHolder>() {

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
        holder.binding.textLabel.setTypeface(null, Typeface.BOLD)
        holder.binding.textMeta.text = context.getString(R.string.assignment_label_type_system)
        holder.binding.textMeta.alpha = 0.6f

        holder.binding.viewColor.setBackgroundColor(label.color)

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

        holder.binding.switchEnabled.setOnCheckedChangeListener(null)
        holder.binding.switchEnabled.isChecked = label.isEnabled
        holder.binding.switchEnabled.setOnCheckedChangeListener { _, isChecked ->
            onToggleEnabled(label, isChecked)
        }

        holder.binding.btnEdit.visibility = View.VISIBLE
        holder.binding.btnDelete.visibility = View.GONE
        holder.binding.btnEdit.text = context.getString(R.string.edit)
        holder.binding.btnEdit.setOnClickListener { onEdit(label) }
    }

    fun update(newItems: List<StatusLabel>) {
        items = newItems
        notifyDataSetChanged()
    }
}