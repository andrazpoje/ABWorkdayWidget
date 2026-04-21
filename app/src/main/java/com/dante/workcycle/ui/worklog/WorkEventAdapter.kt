package com.dante.workcycle.ui.worklog

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.dante.workcycle.R

class WorkEventAdapter(
    private val onItemClick: (WorkEventListItem) -> Unit
) : RecyclerView.Adapter<WorkEventAdapter.EventViewHolder>() {

    private var items: List<WorkEventListItem> = emptyList()

    fun submitList(newItems: List<WorkEventListItem>) {
        val oldItems = items
        items = newItems
        DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = oldItems.size

            override fun getNewListSize(): Int = newItems.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldItems[oldItemPosition] == newItems[newItemPosition]
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldItems[oldItemPosition] == newItems[newItemPosition]
            }
        }).dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_work_event, parent, false) as TextView
        return EventViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class EventViewHolder(
        private val textView: TextView,
        private val onItemClick: (WorkEventListItem) -> Unit
    ) : RecyclerView.ViewHolder(textView) {

        fun bind(item: WorkEventListItem) {
            textView.text = item.text
            textView.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}
