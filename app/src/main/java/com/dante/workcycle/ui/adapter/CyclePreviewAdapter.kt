package com.dante.workcycle.ui.adapter

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dante.workcycle.core.util.CycleColorHelper
import com.dante.workcycle.data.prefs.Prefs
import com.dante.workcycle.databinding.ItemCyclePreviewBinding
import java.time.LocalDate

class CyclePreviewAdapter :
    ListAdapter<CyclePreviewAdapter.PreviewItem, CyclePreviewAdapter.PreviewViewHolder>(
        PreviewDiffCallback()
    ) {

    data class PreviewItem(
        val date: LocalDate,
        val title: String,
        val dateText: String,
        val cycleLabel: String,
        val colorLabel: String,
        val secondaryLabel: String? = null,
        val statusLabel: String? = null,
        val statusColor: Int? = null,
        val helperText: String? = null,
        val isOffDay: Boolean = false
    )

    private var cycleLabels: List<String> = emptyList()

    var onItemClick: ((PreviewItem) -> Unit)? = null

    fun submitPreviewItems(newItems: List<PreviewItem>, cycle: List<String>) {
        cycleLabels = cycle
        submitList(newItems)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreviewViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemCyclePreviewBinding.inflate(inflater, parent, false)
        return PreviewViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: PreviewViewHolder, position: Int) {
        holder.bind(getItem(position), cycleLabels)
    }

    class PreviewViewHolder(
        private val binding: ItemCyclePreviewBinding,
        private val onClick: ((PreviewItem) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PreviewItem, cycle: List<String>) {
            val context = binding.root.context

            binding.dayTitleText.text = item.title
            binding.dayDateText.text = item.dateText

            binding.dayCycleText.setTypeface(null, Typeface.NORMAL)
            binding.daySecondaryText.setTypeface(null, android.graphics.Typeface.NORMAL)

            val bgColor = getCycleBackgroundColor(
                context = context,
                label = item.colorLabel,
                cycle = cycle
            )

            val textColor = if (ColorUtils.calculateLuminance(bgColor) > 0.5) {
                Color.BLACK
            } else {
                Color.WHITE
            }

            binding.root.setCardBackgroundColor(bgColor)
            binding.dayTitleText.setTextColor(textColor)
            binding.dayDateText.setTextColor(textColor)
            bindCycleAndSecondaryLabel(
                context = context,
                primaryLabel = item.cycleLabel,
                secondaryName = item.secondaryLabel,
                textColor = textColor
            )

            bindStatusIndicator(
                status = item.statusLabel,
                statusColor = item.statusColor,
                cardBackgroundColor = bgColor,
                textColor = textColor
            )

            binding.dayCycleText.alpha = 1f
            binding.root.alpha = if (!item.helperText.isNullOrBlank()) 0.96f else 1f

            binding.root.setOnClickListener {
                onClick?.invoke(item)
            }
        }

        private fun getCycleBackgroundColor(
            context: Context,
            label: String,
            cycle: List<String>
        ): Int {
            return CycleColorHelper.getBackgroundColor(context, label, cycle)
        }

        private fun bindCycleAndSecondaryLabel(
            context: Context,
            primaryLabel: String,
            secondaryName: String?,
            textColor: Int
        ) {
            val prefs = context.getSharedPreferences(Prefs.PREFS_NAME, Context.MODE_PRIVATE)
            val showIndicators = prefs.getBoolean(
                Prefs.KEY_SHOW_ASSIGNMENT_ICONS_WEEKLY,
                Prefs.DEFAULT_SHOW_ASSIGNMENT_ICONS_WEEKLY
            )

            val container = binding.assignmentContainer
            val dot = binding.viewSecondaryDot
            val icon = binding.viewSecondaryIcon
            val text = binding.daySecondaryText

            if (!showIndicators) {
                container.visibility = View.GONE
                dot.visibility = View.GONE
                icon.visibility = View.GONE
                text.visibility = View.GONE
                text.text = ""
                text.alpha = 1f
                binding.dayCycleText.text = primaryLabel
                binding.dayCycleText.setTextColor(textColor)
                binding.dayCycleText.setTypeface(null, Typeface.BOLD)
                return
            }

            val trimmedName = secondaryName?.trim().orEmpty()

            if (trimmedName.isBlank()) {
                container.visibility = View.GONE
                dot.visibility = View.GONE
                icon.visibility = View.GONE
                text.visibility = View.GONE
                text.text = ""
                text.alpha = 1f
                binding.dayCycleText.text = primaryLabel
                binding.dayCycleText.setTextColor(textColor)
                binding.dayCycleText.setTypeface(null, Typeface.BOLD)
                return
            }

            container.visibility = View.GONE
            dot.visibility = View.GONE
            icon.visibility = View.GONE
            text.visibility = View.GONE
            text.alpha = 1f
            text.text = ""

            val combined = "$primaryLabel \u2022 $trimmedName"
            binding.dayCycleText.text = SpannableString(combined).apply {
                setSpan(
                    StyleSpan(Typeface.BOLD),
                    0,
                    primaryLabel.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                setSpan(
                    StyleSpan(Typeface.NORMAL),
                    primaryLabel.length,
                    combined.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            binding.dayCycleText.setTextColor(textColor)
            binding.dayCycleText.setTypeface(null, Typeface.NORMAL)
        }

        private fun bindStatusIndicator(
            status: String?,
            statusColor: Int?,
            cardBackgroundColor: Int,
            textColor: Int
        ) {
            val text = binding.statusText
            val trimmed = status?.trim().orEmpty()

            if (trimmed.isBlank()) {
                text.visibility = View.GONE
                text.text = ""
                text.background = null
                return
            }

            val badgeColor = resolveStatusBadgeColor(statusColor, cardBackgroundColor, textColor)
            val badgeTextColor = getReadableTextColor(badgeColor)

            text.visibility = View.VISIBLE
            text.text = trimmed
            text.background = createStatusBadgeBackground(text.context, badgeColor)
            text.setTextColor(badgeTextColor)
            text.alpha = 1f
        }

        private fun resolveStatusBadgeColor(
            statusColor: Int?,
            cardBackgroundColor: Int,
            textColor: Int
        ): Int {
            if (statusColor != null) return statusColor

            return ColorUtils.blendARGB(cardBackgroundColor, textColor, 0.28f)
        }

        private fun createStatusBadgeBackground(context: Context, color: Int): GradientDrawable {
            val radius = 999f
            val strokeColor = if (ColorUtils.calculateLuminance(color) > 0.5) {
                ColorUtils.blendARGB(color, Color.BLACK, 0.18f)
            } else {
                ColorUtils.blendARGB(color, Color.WHITE, 0.20f)
            }

            return GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = radius
                setColor(color)
                setStroke(dpToPx(context, 1f), strokeColor)
            }
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

    private class PreviewDiffCallback : DiffUtil.ItemCallback<PreviewItem>() {
        override fun areItemsTheSame(oldItem: PreviewItem, newItem: PreviewItem): Boolean {
            return oldItem.date == newItem.date
        }

        override fun areContentsTheSame(oldItem: PreviewItem, newItem: PreviewItem): Boolean {
            return oldItem == newItem
        }
    }
}
