package com.dante.workcycle.ui.help

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.dante.workcycle.databinding.ItemHelpSectionBinding

class HelpSectionAdapter(
    private val items: List<HelpSectionItem>
) : RecyclerView.Adapter<HelpSectionAdapter.HelpViewHolder>() {

    inner class HelpViewHolder(
        private val binding: ItemHelpSectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HelpSectionItem) {
            binding.helpIcon.setImageResource(item.iconRes)
            binding.helpTitle.setText(item.titleRes)
            binding.helpContent.setText(item.contentRes)

            binding.helpContent.isVisible = item.isExpanded
            binding.helpArrow.rotation = if (item.isExpanded) 180f else 0f

            binding.helpHeader.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    item.isExpanded = !item.isExpanded
                    notifyItemChanged(position)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HelpViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemHelpSectionBinding.inflate(inflater, parent, false)
        return HelpViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HelpViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}