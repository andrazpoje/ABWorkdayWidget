package com.dante.workcycle.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.dante.workcycle.R
import com.dante.workcycle.applySystemBarsBottomInsetAsPadding
import com.dante.workcycle.applySystemBarsHorizontalInsetAsPadding

class HelpFragment : Fragment(R.layout.fragment_help) {

    private lateinit var helpContentContainer: View

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        helpContentContainer = view.findViewById(R.id.helpContentContainer)

        view.findViewById<View>(R.id.helpScrollView).applySystemBarsBottomInsetAsPadding()
        helpContentContainer.applySystemBarsHorizontalInsetAsPadding()
    }
}