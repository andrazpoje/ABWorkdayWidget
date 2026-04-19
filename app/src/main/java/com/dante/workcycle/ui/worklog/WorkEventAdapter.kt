package com.dante.workcycle.ui.worklog

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.dante.workcycle.R

class WorkEventAdapter : RecyclerView.Adapter<WorkEventAdapter.EventViewHolder>() {

    private var items: List<String> = emptyList()

    fun submitList(newItems: List<String>) {
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
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class EventViewHolder(
        private val textView: TextView
    ) : RecyclerView.ViewHolder(textView) {

        fun bind(text: String) {
            textView.text = text
        }
    }
}
