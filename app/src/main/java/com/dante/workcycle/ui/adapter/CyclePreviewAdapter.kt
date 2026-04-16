package com.dante.workcycle.ui.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dante.workcycle.R
import com.dante.workcycle.data.prefs.AssignmentLabelsPrefs
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
            binding.dayCycleText.text = item.cycleLabel

            binding.dayCycleText.setTypeface(null, android.graphics.Typeface.BOLD)
            binding.daySecondaryText.setTypeface(null, android.graphics.Typeface.NORMAL)

            val bgColor = if (item.isOffDay) {
                Color.parseColor("#9E9E9E")
            } else {
                getCycleBackgroundColor(
                    label = item.colorLabel,
                    cycle = cycle
                )
            }

            val textColor = if (ColorUtils.calculateLuminance(bgColor) > 0.5) {
                Color.BLACK
            } else {
                Color.WHITE
            }

            binding.root.setCardBackgroundColor(bgColor)
            binding.daySecondaryText.setTextColor(
                ColorUtils.setAlphaComponent(textColor, 180)
            )
            binding.dayTitleText.setTextColor(textColor)
            binding.dayDateText.setTextColor(textColor)
            binding.dayCycleText.setTextColor(textColor)

            bindSecondaryIndicator(
                context = context,
                secondaryName = item.secondaryLabel,
                textColor = textColor
            )

            bindStatusIndicator(
                status = item.statusLabel,
                textColor = textColor
            )

            val hasStatus = !item.statusLabel.isNullOrBlank()

            binding.dayCycleText.alpha = 1f
            binding.root.alpha = if (!item.helperText.isNullOrBlank()) 0.96f else 1f

            binding.root.setOnClickListener {
                onClick?.invoke(item)
            }

            binding.daySecondaryText.alpha = when {
                hasStatus -> 0.48f
                item.secondaryLabel?.contains("*") == true -> 1f
                else -> 0.65f
            }
        }

        private fun getCycleBackgroundColor(
            label: String,
            cycle: List<String>
        ): Int {
            val cleanedCycle = cycle
                .map { it.trim() }
                .filter { it.isNotBlank() }

            if (cleanedCycle.isEmpty()) return Color.GRAY

            val normalizedLabel = label.trim()
            val index = cleanedCycle.indexOfFirst {
                it.equals(normalizedLabel, ignoreCase = true)
            }

            return when (if (index == -1) 0 else index) {
                0 -> Color.parseColor("#1976D2")
                1 -> Color.parseColor("#388E3C")
                2 -> Color.parseColor("#F57C00")
                3 -> Color.parseColor("#7B1FA2")
                4 -> Color.parseColor("#D32F2F")
                else -> Color.parseColor("#1976D2")
            }
        }

        private fun bindSecondaryIndicator(
            context: Context,
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
                return
            }

            val trimmedName = secondaryName?.trim().orEmpty()

            if (trimmedName.isBlank()) {
                container.visibility = View.GONE
                dot.visibility = View.GONE
                icon.visibility = View.GONE
                text.visibility = View.GONE
                text.text = ""
                return
            }

            val cleanName = trimmedName.removeSuffix("*").trim()
            val labelsPrefs = AssignmentLabelsPrefs(context)
            val secondary = labelsPrefs.getLabelByName(cleanName)
            val iconRes = getIconRes(secondary?.iconKey)

            container.visibility = View.VISIBLE
            text.text = "• $trimmedName"
            text.visibility = View.VISIBLE
            text.setTextColor(ColorUtils.setAlphaComponent(textColor, 180))

            when {
                iconRes != null -> {
                    icon.setImageResource(iconRes)
                    icon.setColorFilter(textColor)
                    icon.visibility = View.VISIBLE
                    dot.visibility = View.GONE
                }

                secondary != null -> {
                    dot.visibility = View.VISIBLE
                    dot.setBackgroundColor(secondary.color)
                    icon.visibility = View.GONE
                }

                else -> {
                    dot.visibility = View.GONE
                    icon.visibility = View.GONE
                }
            }
        }

        private fun bindStatusIndicator(
            status: String?,
            textColor: Int
        ) {
            val text = binding.statusText
            val trimmed = status?.trim().orEmpty()

            if (trimmed.isBlank()) {
                text.visibility = View.GONE
                text.text = ""
                return
            }

            text.visibility = View.VISIBLE
            text.text = trimmed

            val statusColor = getStatusColor(trimmed)
            text.setTextColor(statusColor ?: ColorUtils.setAlphaComponent(textColor, 220))
            text.alpha = 1f
        }

        private fun getStatusColor(status: String): Int? {
            return when (status.trim()) {
                "Bolniška" -> Color.parseColor("#E53935")
                "Dopust" -> Color.parseColor("#F9A825")
                "Dežurstvo" -> Color.parseColor("#8E24AA")
                "Sick leave" -> Color.parseColor("#E53935")
                "Vacation" -> Color.parseColor("#F9A825")
                "Standby" -> Color.parseColor("#8E24AA")
                else -> null
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