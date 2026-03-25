package com.dante.abworkdaywidget

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.net.toUri
import com.google.android.material.button.MaterialButton

class WhatsNewActivity : BaseActivity() {

    override val activityRootView: View
        get() = findViewById(R.id.whatsNewRoot)

    override val topInsetTargetView: View
        get() = findViewById(R.id.whatsNewContentContainer)

    companion object {
        private const val CHANGELOG_URL =
            "https://github.com/andrazpoje/ABWorkdayWidget/blob/master/CHANGELOG.md"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_whats_new)

        setupBaseUi()

        findViewById<TextView>(R.id.whatsNewVersion).text =
            getString(R.string.whats_new_version_format, BuildConfig.VERSION_NAME)

        findViewById<MaterialButton>(R.id.buttonFullChangelog).setOnClickListener {
            openChangelog()
        }

        findViewById<MaterialButton>(R.id.buttonWhatsNewOk).setOnClickListener {
            finish()
        }
    }

    private fun openChangelog() {
        val intent = Intent(Intent.ACTION_VIEW, CHANGELOG_URL.toUri())
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(
                this,
                getString(R.string.unable_to_open_link),
                Toast.LENGTH_SHORT
            ).show()
        } catch (_: Exception) {
            Toast.makeText(
                this,
                getString(R.string.unable_to_open_link),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}