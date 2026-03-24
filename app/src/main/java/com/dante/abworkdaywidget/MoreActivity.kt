package com.dante.abworkdaywidget

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView

class MoreActivity : BaseActivity() {

    override val activityRootView: View
        get() = findViewById(R.id.moreRoot)

    override val topInsetTargetView: View
        get() = findViewById(R.id.moreContentContainer)

    override val bottomNavigationView: com.google.android.material.bottomnavigation.BottomNavigationView?
        get() = findViewById(R.id.bottomNavigation)

    override val selectedBottomNavItemId: Int?
        get() = R.id.nav_more

    companion object {
        private const val AUTHOR_EMAIL = "danteprodukcija@gmail.com"
        private const val DONATE_URL = "https://buymeacoffee.com/poje"
        private const val GITHUB_URL = "https://github.com/andrazpoje/ABWorkdayWidget"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_more)

        setupBaseUi()

        findViewById<TextView>(R.id.moreVersion).text = "v${BuildConfig.VERSION_NAME}"

        findViewById<MaterialCardView>(R.id.cardOpenCalendar).setOnClickListener {
            startActivity(Intent(this, CalendarActivity::class.java))
        }

        findViewById<MaterialCardView>(R.id.cardContactAuthor).setOnClickListener {
            contactAuthor()
        }

        findViewById<MaterialCardView>(R.id.cardOpenGithub).setOnClickListener {
            openUrl(GITHUB_URL)
        }

        findViewById<MaterialCardView>(R.id.cardReportBug).setOnClickListener {
            reportBug()
        }

        findViewById<MaterialCardView>(R.id.cardDonate).setOnClickListener {
            openUrl(DONATE_URL)
        }
    }

    private fun contactAuthor() {
        val subject = getString(R.string.email_subject_contact, getString(R.string.app_name))

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
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
            data = Uri.parse("mailto:")
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
                this,
                getString(R.string.no_email_app_found),
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

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
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