package com.dante.abworkdaywidget

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dante.abworkdaywidget.databinding.ItemCyclePreviewBinding

class CyclePreviewAdapter :
    ListAdapter<CyclePreviewAdapter.PreviewItem, CyclePreviewAdapter.PreviewViewHolder>(PreviewDiffCallback()) {

    data class PreviewItem(
        val title: String,
        val dateText: String,
        val cycleLabel: String
    )

    private var cycleLabels: List<String> = emptyList()

    fun submitList(newItems: List<PreviewItem>, cycle: List<String>) {
        cycleLabels = cycle
        submitList(newItems)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreviewViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemCyclePreviewBinding.inflate(inflater, parent, false)
        return PreviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PreviewViewHolder, position: Int) {
        holder.bind(getItem(position), cycleLabels)
    }

    class PreviewViewHolder(
        private val binding: ItemCyclePreviewBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PreviewItem, cycle: List<String>) {
            val context = binding.root.context

            binding.dayTitleText.text = item.title
            binding.dayDateText.text = item.dateText
            binding.dayCycleText.text = item.cycleLabel

            val bgColor = CycleColorHelper.getBackgroundColor(
                context = context,
                label = item.cycleLabel,
                cycle = cycle
            )

            val textColor = CycleColorHelper.getTextColorForBackground(bgColor)

            binding.root.setCardBackgroundColor(bgColor)
            binding.dayTitleText.setTextColor(textColor)
            binding.dayDateText.setTextColor(textColor)
            binding.dayCycleText.setTextColor(textColor)
        }
    }

    private class PreviewDiffCallback : DiffUtil.ItemCallback<PreviewItem>() {
        override fun areItemsTheSame(oldItem: PreviewItem, newItem: PreviewItem): Boolean {
            return oldItem.title == newItem.title &&
                    oldItem.dateText == newItem.dateText
        }

        override fun areContentsTheSame(oldItem: PreviewItem, newItem: PreviewItem): Boolean {
            return oldItem == newItem
        }
    }
}