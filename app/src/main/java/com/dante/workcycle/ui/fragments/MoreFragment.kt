package com.dante.workcycle.ui.fragments

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dante.workcycle.BuildConfig
import com.dante.workcycle.R
import com.dante.workcycle.core.ui.applySystemBarsBottomInsetAsPadding
import com.dante.workcycle.core.ui.applySystemBarsHorizontalInsetAsPadding
import com.google.android.material.card.MaterialCardView

class MoreFragment : Fragment(R.layout.fragment_more) {

    companion object {
        private const val AUTHOR_EMAIL = "danteprodukcija@gmail.com"
        private const val DONATE_URL = "https://buymeacoffee.com/poje"
        private const val GITHUB_URL = "https://github.com/andrazpoje/ABWorkdayWidget"
    }

    private lateinit var moreContentContainer: View
    private lateinit var moreVersion: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)

        view.findViewById<View>(R.id.moreScrollView).applySystemBarsBottomInsetAsPadding()
        moreContentContainer.applySystemBarsHorizontalInsetAsPadding()

        moreVersion.text = getString(R.string.app_version, BuildConfig.VERSION_NAME)

        view.findViewById<MaterialCardView>(R.id.cardWhatsNew).setOnClickListener {
            findNavController().navigate(R.id.action_moreFragment_to_whatsNewFragment)
        }

        view.findViewById<MaterialCardView>(R.id.cardCheckUpdates).setOnClickListener {
            openAppInPlayStore()
        }

        view.findViewById<MaterialCardView>(R.id.cardContactAuthor).setOnClickListener {
            contactAuthor()
        }

        view.findViewById<MaterialCardView>(R.id.cardOpenGithub).setOnClickListener {
            openUrl(GITHUB_URL)
        }

        view.findViewById<MaterialCardView>(R.id.cardReportBug).setOnClickListener {
            reportBug()
        }

        view.findViewById<MaterialCardView>(R.id.cardDonate).setOnClickListener {
            openUrl(DONATE_URL)
        }

        view.findViewById<MaterialCardView>(R.id.cardStatistics).setOnClickListener {
            findNavController().navigate(R.id.action_moreFragment_to_statisticsFragment)
        }
    }

    private fun bindViews(root: View) {
        moreContentContainer = root.findViewById(R.id.moreContentContainer)
        moreVersion = root.findViewById(R.id.moreVersion)
    }

    private fun contactAuthor() {
        val subject = getString(R.string.email_subject_contact, getString(R.string.app_name))

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = "mailto:".toUri()
            putExtra(Intent.EXTRA_EMAIL, arrayOf(AUTHOR_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, subject)
        }

        launchEmailIntent(intent)
    }

    private fun reportBug() {
        val device = "${Build.MANUFACTURER} ${Build.MODEL}"
        val androidVersion = Build.VERSION.RELEASE

        val subject = getString(R.string.email_subject_bug_report, getString(R.string.app_name))
        val body = getString(
            R.string.email_body_bug_report,
            BuildConfig.VERSION_NAME,
            device,
            androidVersion
        )

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = "mailto:".toUri()
            putExtra(Intent.EXTRA_EMAIL, arrayOf(AUTHOR_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }

        launchEmailIntent(intent)
    }

    private fun launchEmailIntent(intent: Intent) {
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(
                requireContext(),
                getString(R.string.no_email_app_found),
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

    private fun openAppInPlayStore() {
        val packageName = requireContext().packageName
        val marketIntent = Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri())
        val webIntent = Intent(
            Intent.ACTION_VIEW,
            "https://play.google.com/store/apps/details?id=$packageName".toUri()
        )

        try {
            startActivity(marketIntent)
        } catch (_: ActivityNotFoundException) {
            try {
                startActivity(webIntent)
            } catch (_: Exception) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.unable_to_open_link),
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (_: Exception) {
            Toast.makeText(
                requireContext(),
                getString(R.string.unable_to_open_link),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
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