package com.dante.workcycle.ui.dialogs

import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.DialogFragment
import com.dante.workcycle.R
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textview.MaterialTextView

class ColorPickerDialog(
    private val titleText: String? = null,
    initialColor: Int? = null,
    private val onColorSelected: (Int) -> Unit
) : DialogFragment() {

    private var selectedColor: Int = initialColor ?: DEFAULT_COLOR

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val view = layoutInflater.inflate(R.layout.dialog_color_picker, null, false)

        val titleView = view.findViewById<MaterialTextView>(R.id.colorPickerTitle)
        val previewCard = view.findViewById<MaterialCardView>(R.id.selectedColorPreview)
        val previewHex = view.findViewById<MaterialTextView>(R.id.selectedColorHex)
        val paletteContainer = view.findViewById<ViewGroup>(R.id.paletteContainer)

        titleView.text = titleText ?: getString(R.string.color_picker_title)

        val rows = COLOR_PALETTE.chunked(PALETTE_COLUMNS)

        rows.forEach { rowColors ->
            val rowView = layoutInflater
                .inflate(R.layout.item_color_picker_row, paletteContainer, false) as ViewGroup

            rowColors.forEach { color ->
                val swatch = layoutInflater
                    .inflate(R.layout.item_color_swatch, rowView, false) as MaterialCardView

                bindSwatch(
                    swatch = swatch,
                    color = color,
                    isSelected = color == selectedColor,
                    onClick = {
                        selectedColor = color
                        updatePreview(previewCard, previewHex, selectedColor)
                        refreshSelection(paletteContainer)
                    }
                )

                rowView.addView(swatch)
            }

            paletteContainer.addView(rowView)
        }

        updatePreview(previewCard, previewHex, selectedColor)

        return MaterialAlertDialogBuilder(context)
            .setView(view)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.color_picker_apply) { _, _ ->
                onColorSelected(selectedColor)
            }
            .create()
    }

    private fun refreshSelection(container: ViewGroup) {
        for (i in 0 until container.childCount) {
            val row = container.getChildAt(i) as? ViewGroup ?: continue
            for (j in 0 until row.childCount) {
                val swatch = row.getChildAt(j) as? MaterialCardView ?: continue
                val color = swatch.tag as? Int ?: continue
                val isSelected = color == selectedColor
                styleSwatch(swatch, color, isSelected)
            }
        }
    }

    private fun bindSwatch(
        swatch: MaterialCardView,
        color: Int,
        isSelected: Boolean,
        onClick: () -> Unit
    ) {
        swatch.tag = color
        styleSwatch(swatch, color, isSelected)
        swatch.setOnClickListener { onClick() }
    }

    private fun styleSwatch(
        swatch: MaterialCardView,
        color: Int,
        isSelected: Boolean
    ) {
        val circleView = swatch.findViewById<View>(R.id.colorCircle)
        val checkView = swatch.findViewById<MaterialTextView>(R.id.colorCheck)

        circleView.backgroundTintList = ColorStateList.valueOf(color)
        checkView.visibility = if (isSelected) View.VISIBLE else View.GONE
        checkView.setTextColor(getReadableTextColor(color))

        swatch.strokeWidth = if (isSelected) dpToPx(2) else dpToPx(1)
        swatch.strokeColor = if (isSelected) {
            ColorUtils.blendARGB(color, Color.BLACK, 0.35f)
        } else {
            ColorUtils.setAlphaComponent(Color.BLACK, 28)
        }

        swatch.cardElevation = if (isSelected) dpToPxFloat(4) else 0f
    }

    private fun updatePreview(
        previewCard: MaterialCardView,
        previewHex: MaterialTextView,
        color: Int
    ) {
        previewCard.setCardBackgroundColor(color)
        previewHex.text = colorToHex(color)
        previewHex.setTextColor(getReadableTextColor(color))
    }

    private fun colorToHex(color: Int): String {
        return String.format("#%06X", 0xFFFFFF and color)
    }

    private fun getReadableTextColor(backgroundColor: Int): Int {
        return if (ColorUtils.calculateLuminance(backgroundColor) < 0.45) {
            Color.WHITE
        } else {
            Color.BLACK
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * requireContext().resources.displayMetrics.density).toInt()
    }

    private fun dpToPxFloat(dp: Int): Float {
        return dp * requireContext().resources.displayMetrics.density
    }

    companion object {
        private const val PALETTE_COLUMNS = 5
        private val DEFAULT_COLOR = Color.parseColor("#6750A4")

        private val COLOR_PALETTE = listOf(
            Color.parseColor("#0D47A1"),
            Color.parseColor("#1565C0"),
            Color.parseColor("#1976D2"),
            Color.parseColor("#1E88E5"),
            Color.parseColor("#42A5F5"),

            Color.parseColor("#004D40"),
            Color.parseColor("#00695C"),
            Color.parseColor("#00796B"),
            Color.parseColor("#00897B"),
            Color.parseColor("#26A69A"),

            Color.parseColor("#1B5E20"),
            Color.parseColor("#2E7D32"),
            Color.parseColor("#388E3C"),
            Color.parseColor("#43A047"),
            Color.parseColor("#66BB6A"),

            Color.parseColor("#E65100"),
            Color.parseColor("#EF6C00"),
            Color.parseColor("#F57C00"),
            Color.parseColor("#FB8C00"),
            Color.parseColor("#FFA726"),

            Color.parseColor("#BF360C"),
            Color.parseColor("#D84315"),
            Color.parseColor("#E64A19"),
            Color.parseColor("#F4511E"),
            Color.parseColor("#FF7043"),

            Color.parseColor("#B71C1C"),
            Color.parseColor("#C62828"),
            Color.parseColor("#D32F2F"),
            Color.parseColor("#E53935"),
            Color.parseColor("#EF5350"),

            Color.parseColor("#4A148C"),
            Color.parseColor("#6A1B9A"),
            Color.parseColor("#7B1FA2"),
            Color.parseColor("#8E24AA"),
            Color.parseColor("#AB47BC"),

            Color.parseColor("#311B92"),
            Color.parseColor("#4527A0"),
            Color.parseColor("#512DA8"),
            Color.parseColor("#5E35B1"),
            Color.parseColor("#7E57C2"),

            Color.parseColor("#263238"),
            Color.parseColor("#37474F"),
            Color.parseColor("#455A64"),
            Color.parseColor("#607D8B"),
            Color.parseColor("#90A4AE"),

            Color.parseColor("#212121"),
            Color.parseColor("#424242"),
            Color.parseColor("#616161"),
            Color.parseColor("#9E9E9E"),
            Color.parseColor("#E0E0E0"),

            Color.parseColor("#FFFFFF"),
            Color.parseColor("#F5F5F5"),
            Color.parseColor("#EEEEEE"),
            Color.parseColor("#DDDDDD"),
            Color.parseColor("#000000")
        )
    }
}
