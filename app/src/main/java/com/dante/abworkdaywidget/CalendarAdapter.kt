package com.dante.abworkdaywidget

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import java.time.LocalDate

class CalendarAdapter(
    private val items: List<CalendarDayItem>,
    private val getLabel: (LocalDate) -> String,
    private val getBackgroundColor: (LocalDate, String) -> Int,
    private val onDayClick: (LocalDate) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.VH>() {

    class VH(card: MaterialCardView) : RecyclerView.ViewHolder(card) {
        val dayCard: MaterialCardView = card
        val dayNumber: TextView = card.findViewById(R.id.dayNumber)
        val dayLabel: TextView = card.findViewById(R.id.dayLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_day, parent, false) as MaterialCardView
        return VH(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val context = holder.itemView.context
        val item = items[position]
        val date = item.date

        if (date == null) {
            holder.dayNumber.text = ""
            holder.dayLabel.text = ""
            holder.dayCard.setCardBackgroundColor(
                ContextCompat.getColor(context, android.R.color.transparent)
            )
            holder.dayCard.strokeWidth = 0
            holder.dayCard.isClickable = false
            holder.dayCard.isFocusable = false
            holder.dayCard.setOnClickListener(null)
            return
        }

        val label = getLabel(date)
        val isToday = date == LocalDate.now()

        holder.dayNumber.text = date.dayOfMonth.toString()
        holder.dayLabel.text = label

        val backgroundColor = getBackgroundColor(date, label)
        holder.dayCard.setCardBackgroundColor(backgroundColor)

        val textColor = ContextCompat.getColor(context, android.R.color.white)
        holder.dayNumber.setTextColor(textColor)
        holder.dayLabel.setTextColor(textColor)

        holder.dayCard.strokeWidth = if (isToday) 4 else 0
        holder.dayCard.strokeColor = if (isToday) {
            ContextCompat.getColor(context, android.R.color.white)
        } else {
            ContextCompat.getColor(context, android.R.color.transparent)
        }

        holder.dayCard.isClickable = true
        holder.dayCard.isFocusable = true
        holder.dayCard.setOnClickListener {
            onDayClick(date)
        }
    }
}