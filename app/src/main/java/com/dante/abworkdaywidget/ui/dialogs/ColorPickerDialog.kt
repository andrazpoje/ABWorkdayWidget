package com.dante.abworkdaywidget.ui.dialogs

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageButton
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.DialogFragment
import com.dante.abworkdaywidget.R

class ColorPickerDialog(
    private val onColorSelected: (Int) -> Unit
) : DialogFragment() {

    private val colors = listOf(
        "#1976D2", "#4FC3F7", "#2E7D32", "#81C784",
        "#C62828", "#F9A825", "#EF6C00", "#6A1B9A",
        "#00838F", "#546E7A", "#9E9E9E", "#757575",
        "#FFFFFF", "#E0E0E0", "#212121", "#000000"
    ).map { Color.parseColor(it) }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_color_picker, null, false)

        val buttonIds = listOf(
            R.id.colorOption1, R.id.colorOption2, R.id.colorOption3, R.id.colorOption4,
            R.id.colorOption5, R.id.colorOption6, R.id.colorOption7, R.id.colorOption8,
            R.id.colorOption9, R.id.colorOption10, R.id.colorOption11, R.id.colorOption12,
            R.id.colorOption13, R.id.colorOption14, R.id.colorOption15, R.id.colorOption16
        )

        buttonIds.forEachIndexed { index, id ->
            val button = view.findViewById<ImageButton>(id)
            val wrapped = DrawableCompat.wrap(button.background.mutate())
            DrawableCompat.setTint(wrapped, colors[index])
            button.background = wrapped

            button.setOnClickListener {
                onColorSelected(colors[index])
                dismiss()
            }
        }

        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.select_color)
            .setView(view)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }
}