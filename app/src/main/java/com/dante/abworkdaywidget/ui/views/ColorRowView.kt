package com.dante.abworkdaywidget.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.dante.abworkdaywidget.R
import com.google.android.material.card.MaterialCardView

class ColorRowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val card: MaterialCardView
    private val label: TextView
    private val preview: View

    init {
        LayoutInflater.from(context).inflate(R.layout.view_color_row, this, true)
        card = findViewById(R.id.colorRowCard)
        label = findViewById(R.id.colorLabel)
        preview = findViewById(R.id.colorPreview)
    }

    fun setLabel(text: String) {
        label.text = text
    }

    fun setColor(color: Int) {
        preview.background.setTint(color)
    }

    fun setOnRowClick(action: () -> Unit) {
        card.setOnClickListener { action() }
    }
}