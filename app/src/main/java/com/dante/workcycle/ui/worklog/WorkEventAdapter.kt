package com.dante.workcycle.ui.worklog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
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
            .inflate(R.layout.item_work_event, parent, false)
        return EventViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class EventViewHolder(
        view: View,
        private val onItemClick: (WorkEventListItem) -> Unit
    ) : RecyclerView.ViewHolder(view) {

        private val textTime: TextView = view.findViewById(R.id.textEventTime)
        private val iconEvent: ImageView = view.findViewById(R.id.iconEvent)
        private val textTitle: TextView = view.findViewById(R.id.textEventTitle)
        private val textEditedBadge: TextView = view.findViewById(R.id.textEventEditedBadge)
        private val textDetail: TextView = view.findViewById(R.id.textEventDetail)

        fun bind(item: WorkEventListItem) {
            textTime.text = item.timeText
            iconEvent.setImageResource(item.iconRes)
            textTitle.text = item.titleText
            textEditedBadge.text = item.editBadgeText.orEmpty()
            textEditedBadge.isVisible = !item.editBadgeText.isNullOrBlank()
            textDetail.text = item.detailText.orEmpty()
            textDetail.isVisible = !item.detailText.isNullOrBlank()

            itemView.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}
