package com.dante.abworkdaywidget

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment

class HelpFragment : Fragment(R.layout.fragment_help) {

    private lateinit var helpContentContainer: View

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        helpContentContainer = view.findViewById(R.id.helpContentContainer)

        view.findViewById<View>(R.id.helpScrollView).applySystemBarsBottomInsetAsPadding()
        helpContentContainer.applySystemBarsHorizontalInsetAsPadding()
    }
}