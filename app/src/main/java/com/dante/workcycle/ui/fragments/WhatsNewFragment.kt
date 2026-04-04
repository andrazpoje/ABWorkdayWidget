package com.dante.workcycle.ui.fragments

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.dante.workcycle.BuildConfig
import com.dante.workcycle.R
import com.dante.workcycle.core.ui.applySystemBarsBottomInsetAsPadding
import com.dante.workcycle.core.ui.applySystemBarsHorizontalInsetAsPadding
import com.google.android.material.button.MaterialButton

class WhatsNewFragment : Fragment(R.layout.fragment_whats_new) {

    companion object {
        private const val CHANGELOG_URL =
            "https://github.com/andrazpoje/WorkCycle/blob/master/CHANGELOG.md"
    }

    private lateinit var whatsNewContentContainer: View
    private lateinit var whatsNewVersion: TextView
    private lateinit var buttonFullChangelog: MaterialButton

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)

        view.findViewById<View>(R.id.whatsNewScrollView).applySystemBarsBottomInsetAsPadding()
        whatsNewContentContainer.applySystemBarsHorizontalInsetAsPadding()

        whatsNewVersion.text =
            getString(R.string.whats_new_version_format, BuildConfig.VERSION_NAME)

        buttonFullChangelog.setOnClickListener {
            openChangelog()
        }

    }

    private fun bindViews(root: View) {
        whatsNewContentContainer = root.findViewById(R.id.whatsNewContentContainer)
        whatsNewVersion = root.findViewById(R.id.whatsNewVersion)
        buttonFullChangelog = root.findViewById(R.id.buttonFullChangelog)
    }

    private fun openChangelog() {
        val intent = Intent(Intent.ACTION_VIEW, CHANGELOG_URL.toUri())
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(
                requireContext(),
                getString(R.string.unable_to_open_link),
                Toast.LENGTH_SHORT
            ).show()
        } catch (_: Exception) {
            Toast.makeText(
                requireContext(),
                getString(R.string.unable_to_open_link),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}