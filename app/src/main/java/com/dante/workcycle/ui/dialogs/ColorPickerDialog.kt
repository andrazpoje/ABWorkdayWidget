package com.dante.workcycle.ui.dialogs

import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.toColorInt
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
        private val DEFAULT_COLOR = "#6750A4".toColorInt()

        private val COLOR_PALETTE = listOf(
            "#0D47A1".toColorInt(),
            "#1565C0".toColorInt(),
            "#1976D2".toColorInt(),
            "#1E88E5".toColorInt(),
            "#42A5F5".toColorInt(),

            "#004D40".toColorInt(),
            "#00695C".toColorInt(),
            "#00796B".toColorInt(),
            "#00897B".toColorInt(),
            "#26A69A".toColorInt(),

            "#1B5E20".toColorInt(),
            "#2E7D32".toColorInt(),
            "#388E3C".toColorInt(),
            "#43A047".toColorInt(),
            "#66BB6A".toColorInt(),

            "#E65100".toColorInt(),
            "#EF6C00".toColorInt(),
            "#F57C00".toColorInt(),
            "#FB8C00".toColorInt(),
            "#FFA726".toColorInt(),

            "#BF360C".toColorInt(),
            "#D84315".toColorInt(),
            "#E64A19".toColorInt(),
            "#F4511E".toColorInt(),
            "#FF7043".toColorInt(),

            "#B71C1C".toColorInt(),
            "#C62828".toColorInt(),
            "#D32F2F".toColorInt(),
            "#E53935".toColorInt(),
            "#EF5350".toColorInt(),

            "#4A148C".toColorInt(),
            "#6A1B9A".toColorInt(),
            "#7B1FA2".toColorInt(),
            "#8E24AA".toColorInt(),
            "#AB47BC".toColorInt(),

            "#311B92".toColorInt(),
            "#4527A0".toColorInt(),
            "#512DA8".toColorInt(),
            "#5E35B1".toColorInt(),
            "#7E57C2".toColorInt(),

            "#263238".toColorInt(),
            "#37474F".toColorInt(),
            "#455A64".toColorInt(),
            "#607D8B".toColorInt(),
            "#90A4AE".toColorInt(),

            "#212121".toColorInt(),
            "#424242".toColorInt(),
            "#616161".toColorInt(),
            "#9E9E9E".toColorInt(),
            "#E0E0E0".toColorInt(),

            "#FFFFFF".toColorInt(),
            "#F5F5F5".toColorInt(),
            "#EEEEEE".toColorInt(),
            "#DDDDDD".toColorInt(),
            "#000000".toColorInt()
        )
    }
}
