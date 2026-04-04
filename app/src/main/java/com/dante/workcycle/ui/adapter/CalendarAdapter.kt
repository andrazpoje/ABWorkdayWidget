package com.dante.workcycle.ui.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.dante.workcycle.CalendarDayItem
import com.dante.workcycle.R
import com.dante.workcycle.data.prefs.AssignmentLabelsPrefs
import com.dante.workcycle.data.prefs.Prefs
import com.google.android.material.card.MaterialCardView

class CalendarAdapter(
    private val items: List<CalendarDayItem>,
    private val onDayClick: (CalendarDayItem) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.VH>() {

    class VH(card: MaterialCardView) : RecyclerView.ViewHolder(card) {
        val dayCard: MaterialCardView = card
        val dayCardFrame: FrameLayout = card.findViewById(R.id.dayCardFrame)
        val dayContent: View = card.findViewById(R.id.dayContent)

        val dayNumber: TextView = card.findViewById(R.id.dayNumber)
        val dayLabel: TextView = card.findViewById(R.id.dayLabel)

        val secondaryContainer: View = card.findViewById(R.id.secondaryContainer)
        val secondaryDot: View = card.findViewById(R.id.secondaryDot)
        val secondaryLabel: TextView = card.findViewById(R.id.secondaryLabel)
        val secondaryIcon: ImageView = card.findViewById(R.id.secondaryIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_day, parent, false) as MaterialCardView
        return VH(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]

        if (item.isEmpty || item.date == null) {
            bindEmptyCell(holder)
        } else {
            bindNormalCell(holder, item)
        }
    }

    private fun bindEmptyCell(holder: VH) {
        holder.dayNumber.text = ""
        holder.dayLabel.text = ""
        holder.secondaryLabel.text = ""

        holder.dayCard.setCardBackgroundColor(Color.TRANSPARENT)
        holder.dayCard.strokeWidth = 0
        holder.dayCard.strokeColor = Color.TRANSPARENT
        holder.dayCard.cardElevation = 0f

        holder.dayCardFrame.foreground = null

        holder.dayNumber.alpha = 0f
        holder.dayLabel.alpha = 0f

        holder.secondaryContainer.isVisible = false
        holder.secondaryIcon.isVisible = false
        holder.secondaryDot.isVisible = false
        holder.secondaryLabel.isVisible = false

        holder.dayCard.isClickable = false
        holder.dayCard.isFocusable = false
        holder.dayCard.isEnabled = false
        holder.dayCard.setOnClickListener(null)

        holder.dayCard.scaleX = 1f
        holder.dayCard.scaleY = 1f
    }

    private fun bindNormalCell(holder: VH, item: CalendarDayItem) {
        val context = holder.itemView.context

        holder.secondaryContainer.isVisible = false
        holder.secondaryLabel.text = ""
        holder.secondaryLabel.isVisible = false
        holder.secondaryIcon.isVisible = false
        holder.secondaryDot.isVisible = false

        holder.dayCard.strokeWidth = 0
        holder.dayCard.strokeColor = Color.TRANSPARENT
        holder.dayCard.cardElevation = 0f
        holder.dayCardFrame.foreground = null
        holder.dayCard.scaleX = 1f
        holder.dayCard.scaleY = 1f

        holder.dayNumber.alpha = 1f
        holder.dayLabel.alpha = 1f

        holder.dayNumber.text = item.dayNumber
        holder.dayLabel.text = item.effectiveCycleLabel

        val backgroundColor = if (item.isOffDay) {
            Color.parseColor("#9E9E9E")
        } else {
            item.cycleColor ?: Color.GRAY
        }

        holder.dayCard.setCardBackgroundColor(backgroundColor)

        val textColor = if (ColorUtils.calculateLuminance(backgroundColor) > 0.5) {
            Color.BLACK
        } else {
            Color.WHITE
        }

        holder.dayNumber.setTextColor(textColor)
        holder.dayLabel.setTextColor(textColor)
        holder.secondaryLabel.setTextColor(textColor)

        bindSecondaryContent(
            context = context,
            holder = holder,
            secondaryLabel = item.secondaryLabel,
            assignmentColor = item.assignmentColor,
            fallbackTextColor = textColor
        )

        applySelectionState(holder, item)

        holder.dayCard.isClickable = true
        holder.dayCard.isFocusable = true
        holder.dayCard.isEnabled = true
        holder.dayCard.setOnClickListener {
            val position = holder.adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                onDayClick(items[position])
            }
        }
    }

    private fun bindSecondaryContent(
        context: Context,
        holder: VH,
        secondaryLabel: String?,
        assignmentColor: Int?,
        fallbackTextColor: Int
    ) {
        val prefs = context.getSharedPreferences(Prefs.PREFS_NAME, Context.MODE_PRIVATE)
        val showIndicators = prefs.getBoolean(
            Prefs.KEY_SHOW_ASSIGNMENT_ICONS_CALENDAR,
            Prefs.DEFAULT_SHOW_ASSIGNMENT_ICONS_CALENDAR
        )

        if (!showIndicators) {
            holder.secondaryContainer.isVisible = false
            holder.secondaryLabel.text = ""
            holder.secondaryLabel.isVisible = false
            holder.secondaryIcon.isVisible = false
            holder.secondaryDot.isVisible = false
            return
        }

        val trimmedLabel = secondaryLabel?.trim().orEmpty()

        if (trimmedLabel.isBlank()) {
            holder.secondaryContainer.isVisible = false
            holder.secondaryLabel.text = ""
            holder.secondaryLabel.isVisible = false
            holder.secondaryIcon.isVisible = false
            holder.secondaryDot.isVisible = false
            return
        }

        holder.secondaryContainer.isVisible = true
        holder.secondaryLabel.text = trimmedLabel
        holder.secondaryLabel.setTextColor(fallbackTextColor)
        holder.secondaryLabel.isVisible = true

        val labelsPrefs = AssignmentLabelsPrefs(context)
        val cleanName = trimmedLabel.removeSuffix("*").trim()
        val label = labelsPrefs.getLabelByName(cleanName)
        val iconRes = getIconRes(label?.iconKey)

        if (iconRes != null) {
            holder.secondaryIcon.setImageResource(iconRes)
            holder.secondaryIcon.isVisible = true
            holder.secondaryDot.isVisible = false
            return
        }

        holder.secondaryIcon.isVisible = false
        holder.secondaryDot.isVisible = true

        val baseColor = assignmentColor ?: fallbackTextColor
        val finalColor = adjustDotColor(baseColor)

        val dotDrawable = holder.secondaryDot.background?.mutate()
        if (dotDrawable != null) {
            val wrapped = DrawableCompat.wrap(dotDrawable)
            DrawableCompat.setTint(wrapped, finalColor)
            holder.secondaryDot.background = wrapped
        } else {
            holder.secondaryDot.setBackgroundColor(finalColor)
        }
    }

    private fun getIconRes(iconKey: String?): Int? {
        return when (iconKey) {
            "sick" -> R.drawable.ic_assignment_sick_24
            "vacation" -> R.drawable.ic_assignment_vacation_24
            "standby" -> R.drawable.ic_assignment_standby_24
            "field" -> R.drawable.ic_assignment_field_24
            else -> null
        }
    }

    private fun applySelectionState(holder: VH, item: CalendarDayItem) {
        val baseColor = if (
            item.effectiveCycleLabel.equals(holder.itemView.context.getString(R.string.off_day_label), ignoreCase = true) ||
            item.effectiveCycleLabel.equals(holder.itemView.context.getString(R.string.off_day_label).take(5), ignoreCase = true)
        ) {
            Color.parseColor("#9E9E9E")
        } else {
            item.primaryColor ?: Color.GRAY
        }

        when {
            item.isSelected -> {
                holder.dayCard.strokeWidth = 3
                holder.dayCard.strokeColor = darkenColor(baseColor)
                holder.dayCard.cardElevation = 3f
            }

            item.isToday -> {
                holder.dayCard.strokeWidth = 2
                holder.dayCard.strokeColor = darkenColor(baseColor)
                holder.dayCard.cardElevation = 2f
            }

            else -> {
                holder.dayCard.strokeWidth = 0
                holder.dayCard.strokeColor = Color.TRANSPARENT
                holder.dayCard.cardElevation = 0f
            }
        }

        holder.dayCard.scaleX = 1f
        holder.dayCard.scaleY = 1f
    }

    private fun adjustDotColor(color: Int): Int {
        return ColorUtils.blendARGB(color, Color.WHITE, 0.22f)
    }

    private fun darkenColor(color: Int): Int {
        return ColorUtils.blendARGB(color, Color.BLACK, 0.22f)
    }
}