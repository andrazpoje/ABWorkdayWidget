package com.dante.workcycle.ui.template

import android.app.Dialog
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.card.MaterialCardView

class TemplatePickerBottomSheet(
    private val sections: List<Section>,
    private val selectedTemplateId: String?,
    private val onTemplateSelected: (String) -> Unit
) : BottomSheetDialogFragment() {

    data class Section(
        val title: String,
        val items: List<Item>
    )

    data class Item(
        val templateId: String,
        val title: String,
        val description: String
    )

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext(), theme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val context = requireContext()
        val scrollView = ScrollView(context)
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(24))
        }

        scrollView.addView(
            content,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        sections.forEachIndexed { index, section ->
            content.addView(createHeader(section.title, index > 0))
            section.items.forEach { item ->
                content.addView(createItem(item))
            }
        }

        return scrollView
    }

    private fun createHeader(title: String, hasTopSpacing: Boolean): TextView {
        return TextView(requireContext()).apply {
            text = title
            textSize = 12f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(resolveThemeColor(com.google.android.material.R.attr.colorOnSurfaceVariant))
            includeFontPadding = false
            setPadding(0, if (hasTopSpacing) dp(22) else 0, 0, dp(8))
        }
    }

    private fun createItem(item: Item): MaterialCardView {
        val context = requireContext()
        val isSelected = item.templateId == selectedTemplateId
        val colorPrimary = resolveThemeColor(com.google.android.material.R.attr.colorPrimary)
        val surfaceColor = resolveThemeColor(com.google.android.material.R.attr.colorSurface)
        val selectedColor = ColorUtils.setAlphaComponent(colorPrimary, 28)

        val card = MaterialCardView(context).apply {
            radius = dp(12).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(if (isSelected) selectedColor else surfaceColor)
            strokeWidth = if (isSelected) dp(1) else 0
            strokeColor = if (isSelected) colorPrimary else android.graphics.Color.TRANSPARENT
            isClickable = true
            isFocusable = true
            foreground = resolveSelectableForeground()
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(6)
            }
        }

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(9), dp(12), dp(9))
        }

        val texts = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val title = TextView(context).apply {
            text = item.title
            textSize = 15f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(resolveThemeColor(com.google.android.material.R.attr.colorOnSurface))
        }

        val description = TextView(context).apply {
            text = item.description
            textSize = 12.5f
            setTextColor(resolveThemeColor(com.google.android.material.R.attr.colorOnSurfaceVariant))
            setPadding(0, dp(2), 0, 0)
        }

        val check = TextView(context).apply {
            text = "\u2713"
            textSize = 20f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(colorPrimary)
            isVisible = isSelected
            setPadding(dp(12), 0, 0, 0)
        }

        texts.addView(title)
        texts.addView(description)
        row.addView(texts)
        row.addView(check)
        card.addView(row)

        card.setOnClickListener {
            dismiss()
            onTemplateSelected(item.templateId)
        }

        return card
    }

    private fun resolveSelectableForeground(): android.graphics.drawable.Drawable? {
        val outValue = TypedValue()
        requireContext().theme.resolveAttribute(
            android.R.attr.selectableItemBackground,
            outValue,
            true
        )
        return ResourcesCompat.getDrawable(
            resources,
            outValue.resourceId,
            requireContext().theme
        )
    }

    private fun resolveThemeColor(attrRes: Int): Int {
        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(attrRes, typedValue, true)
        return typedValue.data
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
