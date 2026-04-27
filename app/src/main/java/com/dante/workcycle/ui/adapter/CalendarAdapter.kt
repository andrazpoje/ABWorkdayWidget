package com.dante.workcycle.ui.adapter

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.dante.workcycle.CalendarDayItem
import com.dante.workcycle.R
import com.dante.workcycle.data.prefs.Prefs
import com.google.android.material.card.MaterialCardView
import java.util.Locale

class CalendarAdapter(
    private val items: List<CalendarDayItem>,
    private val onDayClick: (CalendarDayItem) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.VH>() {

    class VH(card: MaterialCardView) : RecyclerView.ViewHolder(card) {
        val dayCard: MaterialCardView = card
        val dayCardFrame: FrameLayout = card.findViewById(R.id.dayCardFrame)
        val dayNumber: TextView = card.findViewById(R.id.dayNumber)
        val dayLabel: TextView = card.findViewById(R.id.dayLabel)

        val secondaryContainer: View = card.findViewById(R.id.secondaryContainer)
        val secondaryDot: View = card.findViewById(R.id.secondaryDot)
        val secondaryLabel: TextView = card.findViewById(R.id.secondaryLabel)
        val secondaryIcon: ImageView = card.findViewById(R.id.secondaryIcon)
        val statusIconsContainer: View = card.findViewById(R.id.statusIconsContainer)
        val statusIconFirst: ImageView = card.findViewById(R.id.statusIconFirst)
        val statusIconSecond: ImageView = card.findViewById(R.id.statusIconSecond)
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
        holder.dayLabel.alpha = 1f

        holder.secondaryLabel.alpha = 1f
        holder.secondaryLabel.text = ""
        holder.secondaryLabel.background = null

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
        holder.statusIconsContainer.visibility = View.GONE
        holder.statusIconFirst.isVisible = false
        holder.statusIconSecond.isVisible = false

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
        holder.secondaryLabel.background = null
        holder.secondaryLabel.isVisible = false
        holder.secondaryIcon.isVisible = false
        holder.secondaryDot.isVisible = false
        holder.statusIconsContainer.visibility = View.INVISIBLE
        holder.statusIconFirst.isVisible = false
        holder.statusIconSecond.isVisible = false
        holder.statusIconFirst.background = null
        holder.statusIconSecond.background = null
        holder.statusIconFirst.clearColorFilter()
        holder.statusIconSecond.clearColorFilter()

        holder.dayCard.strokeWidth = 0
        holder.dayCard.strokeColor = Color.TRANSPARENT
        holder.dayCard.cardElevation = 0f
        holder.dayCardFrame.foreground = null
        holder.dayCard.scaleX = 1f
        holder.dayCard.scaleY = 1f

        holder.dayNumber.alpha = 1f
        holder.dayLabel.alpha = 1f

        holder.dayNumber.text = item.dayNumber
        holder.dayLabel.text = getShortCycleLabel(item.effectiveCycleLabel)
        holder.dayLabel.setTypeface(null, android.graphics.Typeface.BOLD)
        holder.secondaryLabel.setTypeface(null, android.graphics.Typeface.NORMAL)

        val backgroundColor = item.cycleColor ?: Color.GRAY

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
        bindStatusIcons(
            holder = holder,
            iconResIds = item.statusIconResIds,
            iconColors = item.statusIconColors,
            cellBackgroundColor = backgroundColor
        )

        applySelectionState(holder, item)

        holder.dayCard.isClickable = true
        holder.dayCard.isFocusable = true
        holder.dayCard.isEnabled = true
        holder.dayCard.setOnClickListener {
            val position = holder.bindingAdapterPosition
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
            holder.secondaryLabel.background = null
            holder.secondaryLabel.isVisible = false
            holder.secondaryIcon.isVisible = false
            holder.secondaryDot.isVisible = false
            return
        }

        val trimmedLabel = secondaryLabel?.trim().orEmpty()

        if (trimmedLabel.isBlank()) {
            holder.secondaryContainer.isVisible = false
            holder.secondaryLabel.text = ""
            holder.secondaryLabel.background = null
            holder.secondaryLabel.isVisible = false
            holder.secondaryIcon.isVisible = false
            holder.secondaryDot.isVisible = false
            return
        }

        val isOverride = trimmedLabel.contains("*")

        holder.secondaryContainer.isVisible = true
        holder.secondaryIcon.isVisible = false
        holder.secondaryDot.isVisible = false
        val badgeColor = assignmentColor ?: ColorUtils.setAlphaComponent(fallbackTextColor, 220)
        holder.secondaryLabel.background = createSecondaryBadgeBackground(context, badgeColor)
        holder.secondaryLabel.setTextColor(getReadableTextColor(badgeColor))
        holder.secondaryLabel.isVisible = true
        holder.secondaryLabel.alpha = 1f

        val shortLabel = getShortSecondaryLabel(trimmedLabel)

        val finalLabel = if (isOverride) "$shortLabel*" else shortLabel
        holder.secondaryLabel.text = finalLabel
    }

    private fun bindStatusIcons(
        holder: VH,
        iconResIds: List<Int>,
        iconColors: List<Int>,
        cellBackgroundColor: Int
    ) {
        val firstIcon = iconResIds.getOrNull(0)
        val secondIcon = iconResIds.getOrNull(1)
        val hasIcons = firstIcon != null || secondIcon != null

        holder.statusIconsContainer.visibility = if (hasIcons) View.VISIBLE else View.INVISIBLE

        bindStatusIcon(holder.statusIconFirst, firstIcon, iconColors.getOrNull(0), cellBackgroundColor)
        bindStatusIcon(holder.statusIconSecond, secondIcon, iconColors.getOrNull(1), cellBackgroundColor)
    }

    private fun bindStatusIcon(
        iconView: ImageView,
        iconRes: Int?,
        iconColor: Int?,
        cellBackgroundColor: Int
    ) {
        if (iconRes == null) {
            iconView.isVisible = false
            iconView.background = null
            iconView.clearColorFilter()
            return
        }

        val statusColor = iconColor ?: Color.GRAY
        val circleColor = getContrastingStatusCircleColor(cellBackgroundColor)
        iconView.setImageResource(iconRes)
        iconView.background = createStatusIconBackground(iconView.context, circleColor)
        iconView.setColorFilter(statusColor)
        iconView.isVisible = true
    }

    private fun createStatusIconBackground(context: Context, color: Int): GradientDrawable {
        val strokeColor = if (ColorUtils.calculateLuminance(color) > 0.5) {
            ColorUtils.blendARGB(color, Color.BLACK, 0.16f)
        } else {
            ColorUtils.blendARGB(color, Color.WHITE, 0.22f)
        }

        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            setStroke(dpToPx(context, 1f), strokeColor)
        }
    }

    private fun getContrastingStatusCircleColor(cellBackgroundColor: Int): Int {
        return if (ColorUtils.calculateLuminance(cellBackgroundColor) > 0.5) {
            ColorUtils.blendARGB(cellBackgroundColor, Color.BLACK, 0.82f)
        } else {
            ColorUtils.blendARGB(cellBackgroundColor, Color.WHITE, 0.88f)
        }
    }

    private fun createSecondaryBadgeBackground(context: Context, color: Int): GradientDrawable {
        val strokeColor = if (ColorUtils.calculateLuminance(color) > 0.5) {
            ColorUtils.blendARGB(color, Color.BLACK, 0.18f)
        } else {
            ColorUtils.blendARGB(color, Color.WHITE, 0.22f)
        }

        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(context, 999f).toFloat()
            setColor(color)
            setStroke(dpToPx(context, 1f), strokeColor)
        }
    }

    private fun applySelectionState(holder: VH, item: CalendarDayItem) {
        val baseColor = item.primaryColor ?: Color.GRAY

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

    private fun darkenColor(color: Int): Int {
        return ColorUtils.blendARGB(color, Color.BLACK, 0.22f)
    }
    private fun getShortCycleLabel(label: String): String {
        val normalized = label.trim()
        val lower = normalized.lowercase(Locale.getDefault())

        return when {
            lower.startsWith("dopold") -> "Dop"
            lower.startsWith("popold") -> "Pop"
            lower.startsWith("noc") || lower.startsWith("no") -> "No\u010D"
            lower.startsWith("prosto") -> "Pro"
            normalized.length <= 3 -> normalized
            else -> normalized.take(3)
        }
    }

    private fun getShortSecondaryLabel(label: String): String {
        val baseLabel = label.removeSuffix("*").trim()
        if (baseLabel.length <= 4) return baseLabel

        val rajonMatch = Regex("(?i)^rajon\\s*(\\d+)$").find(baseLabel)
        if (rajonMatch != null) {
            return "R${rajonMatch.groupValues[1]}"
        }

        val wordInitials = baseLabel
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")

        if (wordInitials.length in 2..4) return wordInitials

        return baseLabel.take(3).uppercase(Locale.getDefault())
    }

    private fun getReadableTextColor(backgroundColor: Int): Int {
        return if (ColorUtils.calculateLuminance(backgroundColor) > 0.5) {
            Color.BLACK
        } else {
            Color.WHITE
        }
    }

    private fun dpToPx(context: Context, dp: Float): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

}
