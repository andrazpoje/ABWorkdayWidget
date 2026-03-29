package com.dante.workcycle.ui

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView

class ClickableRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}