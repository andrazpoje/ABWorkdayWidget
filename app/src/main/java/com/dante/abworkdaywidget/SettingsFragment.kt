package com.dante.abworkdaywidget

import android.appwidget.AppWidgetManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dante.abworkdaywidget.data.Prefs
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    companion object {
        private const val CHANGELOG_URL =
            "https://github.com/andrazpoje/ABWorkdayWidget/blob/master/CHANGELOG.md"
    }

    private lateinit var settingsContentContainer: View

    private lateinit var appThemeSystem: RadioButton
    private lateinit var appThemeLight: RadioButton
    private lateinit var appThemeDark: RadioButton

    private lateinit var widgetStyleClassic: RadioButton
    private lateinit var widgetStyleMinimal: RadioButton

    private lateinit var switchNotificationsEnabled: SwitchMaterial
    private lateinit var switchSilentNotification: SwitchMaterial

    private lateinit var settingsVersion: MaterialButton
    private lateinit var buttonViewWhatsNew: MaterialButton
    private lateinit var buttonFullChangelog: MaterialButton

    private var isInitializing = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)

        view.findViewById<View>(R.id.settingsScrollView).applySystemBarsBottomInsetAsPadding()
        settingsContentContainer.applySystemBarsHorizontalInsetAsPadding()

        isInitializing = true
        bindCurrentValues()
        setupListeners()
        isInitializing = false
    }

    private fun bindViews(root: View) {
        settingsContentContainer = root.findViewById(R.id.settingsContentContainer)

        appThemeSystem = root.findViewById(R.id.settingsAppThemeSystem)
        appThemeLight = root.findViewById(R.id.settingsAppThemeLight)
        appThemeDark = root.findViewById(R.id.settingsAppThemeDark)

        widgetStyleClassic = root.findViewById(R.id.settingsWidgetStyleClassic)
        widgetStyleMinimal = root.findViewById(R.id.settingsWidgetStyleMinimal)

        switchNotificationsEnabled = root.findViewById(R.id.settingsSwitchNotificationsEnabled)
        switchSilentNotification = root.findViewById(R.id.settingsSwitchSilentNotification)

        settingsVersion = root.findViewById(R.id.settingsVersionButton)
        buttonViewWhatsNew = root.findViewById(R.id.buttonSettingsViewWhatsNew)
        buttonFullChangelog = root.findViewById(R.id.buttonSettingsFullChangelog)
    }

    private fun bindCurrentValues() {
        when (AppThemeManager.loadTheme(requireContext())) {
            AppThemeManager.THEME_LIGHT -> appThemeLight.isChecked = true
            AppThemeManager.THEME_DARK -> appThemeDark.isChecked = true
            else -> appThemeSystem.isChecked = true
        }

        val prefs = requireContext().getSharedPreferences(Prefs.PREFS_NAME, Context.MODE_PRIVATE)
        val widgetStyle = prefs.getString(
            Prefs.KEY_WIDGET_STYLE,
            Prefs.WIDGET_STYLE_CLASSIC
        ) ?: Prefs.WIDGET_STYLE_CLASSIC

        widgetStyleClassic.isChecked = widgetStyle == Prefs.WIDGET_STYLE_CLASSIC
        widgetStyleMinimal.isChecked = widgetStyle == Prefs.WIDGET_STYLE_MINIMAL

        val notificationsEnabled = prefs.getBoolean(Prefs.KEY_NOTIFICATIONS_ENABLED, false)
        val silentEnabled = prefs.getBoolean(Prefs.KEY_SILENT_NOTIFICATION, false)

        switchNotificationsEnabled.isChecked = notificationsEnabled
        switchSilentNotification.isChecked = notificationsEnabled && silentEnabled
        switchSilentNotification.isEnabled = notificationsEnabled

        settingsVersion.text = getString(R.string.app_version, BuildConfig.VERSION_NAME)
    }

    private fun setupListeners() {
        appThemeSystem.setOnClickListener {
            if (!isInitializing) saveAppTheme(AppThemeManager.THEME_SYSTEM)
        }

        appThemeLight.setOnClickListener {
            if (!isInitializing) saveAppTheme(AppThemeManager.THEME_LIGHT)
        }

        appThemeDark.setOnClickListener {
            if (!isInitializing) saveAppTheme(AppThemeManager.THEME_DARK)
        }

        widgetStyleClassic.setOnClickListener {
            if (!isInitializing) saveWidgetStyle(Prefs.WIDGET_STYLE_CLASSIC)
        }

        widgetStyleMinimal.setOnClickListener {
            if (!isInitializing) saveWidgetStyle(Prefs.WIDGET_STYLE_MINIMAL)
        }

        switchNotificationsEnabled.setOnCheckedChangeListener { _, isChecked ->
            if (isInitializing) return@setOnCheckedChangeListener

            switchSilentNotification.isEnabled = isChecked
            if (!isChecked) {
                switchSilentNotification.isChecked = false
            } else {
                (activity as? MainActivity)?.requestNotificationPermissionIfNeeded()
            }

            saveNotificationSettings(
                enabled = isChecked,
                silent = if (isChecked) switchSilentNotification.isChecked else false
            )
        }

        switchSilentNotification.setOnCheckedChangeListener { _, isChecked ->
            if (isInitializing) return@setOnCheckedChangeListener

            saveNotificationSettings(
                enabled = switchNotificationsEnabled.isChecked,
                silent = if (switchNotificationsEnabled.isChecked) isChecked else false
            )
        }

        buttonViewWhatsNew.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_whatsNewFragment)
        }

        buttonFullChangelog.setOnClickListener {
            openChangelog()
        }
    }

    private fun saveAppTheme(theme: String) {
        AppThemeManager.saveTheme(requireContext(), theme)
        AppThemeManager.apply(theme)
    }

    private fun saveWidgetStyle(style: String) {
        val prefs = requireContext().getSharedPreferences(Prefs.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(Prefs.KEY_WIDGET_STYLE, style)
            .apply()

        val manager = AppWidgetManager.getInstance(requireContext())
        val ids = manager.getAppWidgetIds(
            ComponentName(requireContext(), ABWidgetProvider::class.java)
        )
        ABWidgetProvider().onUpdate(requireContext(), manager, ids)
    }

    private fun saveNotificationSettings(enabled: Boolean, silent: Boolean) {
        val prefs = requireContext().getSharedPreferences(Prefs.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(Prefs.KEY_NOTIFICATIONS_ENABLED, enabled)
            .putBoolean(Prefs.KEY_SILENT_NOTIFICATION, silent)
            .apply()
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