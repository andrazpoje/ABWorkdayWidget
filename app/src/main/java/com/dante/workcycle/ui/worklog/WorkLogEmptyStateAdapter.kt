package com.dante.workcycle.ui.worklog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dante.workcycle.R

class WorkLogEmptyStateAdapter : RecyclerView.Adapter<WorkLogEmptyStateAdapter.EmptyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmptyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_work_log_empty, parent, false) as ViewGroup
        return EmptyViewHolder(view)
    }

    override fun onBindViewHolder(holder: EmptyViewHolder, position: Int) = Unit

    override fun getItemCount(): Int = 1

    class EmptyViewHolder(root: ViewGroup) : RecyclerView.ViewHolder(root)
}
