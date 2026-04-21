package com.dante.workcycle.ui.worklog

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.dante.workcycle.R
import com.dante.workcycle.core.ui.applySystemBarsBottomInsetAsPadding
import com.dante.workcycle.core.ui.applySystemBarsHorizontalInsetAsPadding

class WorkLogHelpFragment : Fragment(R.layout.fragment_work_log_help) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.workLogHelpScrollView)
            .applySystemBarsBottomInsetAsPadding()

        view.findViewById<View>(R.id.workLogHelpContentContainer)
            .applySystemBarsHorizontalInsetAsPadding()
    }
}
