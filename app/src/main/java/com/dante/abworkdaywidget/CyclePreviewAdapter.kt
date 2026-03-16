package com.dante.abworkdaywidget

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dante.abworkdaywidget.databinding.ItemCyclePreviewBinding

class CyclePreviewAdapter : RecyclerView.Adapter<CyclePreviewAdapter.PreviewViewHolder>() {

    data class PreviewItem(
        val title: String,
        val dateText: String,
        val cycleLabel: String
    )

    private val items = mutableListOf<PreviewItem>()
    private var cycleLabels: List<String> = emptyList()

    fun submitList(newItems: List<PreviewItem>, cycle: List<String>) {
        items.clear()
        items.addAll(newItems)
        cycleLabels = cycle
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreviewViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemCyclePreviewBinding.inflate(inflater, parent, false)
        return PreviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PreviewViewHolder, position: Int) {
        holder.bind(items[position], cycleLabels)
    }

    override fun getItemCount(): Int = items.size

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

            binding.root.setCardBackgroundColor(bgColor)

            binding.dayTitleText.setTextColor(Color.WHITE)
            binding.dayDateText.setTextColor(Color.WHITE)
            binding.dayCycleText.setTextColor(Color.WHITE)
        }
    }
}