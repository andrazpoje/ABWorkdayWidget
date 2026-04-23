package com.dante.workcycle.ui.fragments

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.dante.workcycle.BuildConfig
import com.dante.workcycle.R
import com.dante.workcycle.core.ui.applySystemBarsBottomInsetAsPadding
import com.dante.workcycle.core.ui.applySystemBarsHorizontalInsetAsPadding
import com.dante.workcycle.data.prefs.LaunchPrefs
import com.google.android.material.button.MaterialButton

class WhatsNewFragment : Fragment(R.layout.fragment_whats_new) {

    companion object {
        private const val CHANGELOG_URL =
            "https://github.com/andrazpoje/ABWorkdayWidget/blob/master/CHANGELOG.md"
    }

    private lateinit var whatsNewContentContainer: View
    private lateinit var whatsNewVersion: TextView
    private lateinit var buttonFullChangelog: MaterialButton
    private lateinit var buttonContinue: MaterialButton
    private lateinit var whatsNewBottomBar: View

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)

        view.findViewById<View>(R.id.whatsNewScrollView).applySystemBarsBottomInsetAsPadding()
        whatsNewContentContainer.applySystemBarsHorizontalInsetAsPadding()
        whatsNewBottomBar.applySystemBarsBottomInsetAsPadding()

        whatsNewVersion.text =
            getString(R.string.whats_new_version_format, BuildConfig.VERSION_NAME)

        buttonFullChangelog.setOnClickListener {
            openChangelog()
        }

        buttonContinue.setOnClickListener {
            closeWhatsNew()
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    closeWhatsNew()
                }
            }
        )
    }

    private fun bindViews(root: View) {
        whatsNewContentContainer = root.findViewById(R.id.whatsNewContentContainer)
        whatsNewVersion = root.findViewById(R.id.whatsNewVersion)
        buttonFullChangelog = root.findViewById(R.id.buttonFullChangelog)
        buttonContinue = root.findViewById(R.id.buttonContinue)
        whatsNewBottomBar = root.findViewById(R.id.whatsNewBottomBar)
    }

    private fun closeWhatsNew() {
        LaunchPrefs(requireContext()).markWhatsNewSeen()
        val popped = parentFragmentManager.popBackStackImmediate()
        if (!popped) {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun openChangelog() {
        val uri = Uri.parse(CHANGELOG_URL.trim())
        Log.d("CHANGELOG_URL", uri.toString())
        val intent = Intent(Intent.ACTION_VIEW, uri)
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
