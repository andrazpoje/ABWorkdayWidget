package com.dante.workcycle.ui.fragments

import android.appwidget.AppWidgetManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dante.workcycle.AppPrefs
import com.dante.workcycle.AppThemeManager
import com.dante.workcycle.BuildConfig
import com.dante.workcycle.R
import com.dante.workcycle.widget.WorkCycleWidgetProvider
import com.dante.workcycle.applySystemBarsBottomInsetAsPadding
import com.dante.workcycle.applySystemBarsHorizontalInsetAsPadding
import com.dante.workcycle.data.Prefs
import com.dante.workcycle.notifications.MidnightAlarmScheduler
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.dante.workcycle.style.WidgetStyleManager
import com.dante.workcycle.style.ThemePreset
import com.dante.workcycle.ui.activity.MainActivity
import com.dante.workcycle.widget.WidgetRefreshHelper
import com.dante.workcycle.ui.views.ColorRowView
import com.dante.workcycle.ui.dialogs.ColorPickerDialog

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    companion object {
        private const val CHANGELOG_URL =
            "https://github.com/andrazpoje/WorkCycle/blob/master/CHANGELOG.md"
    }

    private lateinit var settingsContentContainer: View

    private lateinit var appThemeSystem: RadioButton
    private lateinit var appThemeLight: RadioButton
    private lateinit var appThemeDark: RadioButton

    private lateinit var themeClassic: RadioButton
    private lateinit var themeDark: RadioButton
    private lateinit var prefixEdit: TextInputEditText

    private lateinit var colorShiftA: ColorRowView
    private lateinit var colorShiftB: ColorRowView
    private lateinit var colorBackground: ColorRowView

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

        themeClassic = root.findViewById(R.id.settingsThemeClassic)
        themeDark = root.findViewById(R.id.settingsThemeDark)
        prefixEdit = root.findViewById(R.id.settingsPrefixEdit)

        colorShiftA = root.findViewById(R.id.colorShiftA)
        colorShiftB = root.findViewById(R.id.colorShiftB)
        colorBackground = root.findViewById(R.id.colorBackground)

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

        prefixEdit.setText(
            requireContext()
                .getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)
                .getString(AppPrefs.KEY_PREFIX_TEXT, "") ?: ""
        )

        when (WidgetStyleManager.getCurrentPreset(requireContext())) {
            ThemePreset.DARK -> themeDark.isChecked = true
            ThemePreset.CLASSIC -> themeClassic.isChecked = true
            ThemePreset.CUSTOM -> {
                // optional: lahko pustiš neoznačeno ali označiš Classic
                themeClassic.isChecked = false
                themeDark.isChecked = false
            }
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

        val colors = WidgetStyleManager.getColors(requireContext())

        colorShiftA.setLabel("Shift A")
        colorShiftA.setColor(colors.shiftAColor)

        colorShiftB.setLabel("Shift B")
        colorShiftB.setColor(colors.shiftBColor)

        colorBackground.setLabel("Background")
        colorBackground.setColor(colors.widgetBackgroundColor)

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

        themeClassic.setOnClickListener {
            if (!isInitializing) {
                WidgetStyleManager.applyPreset(requireContext(), ThemePreset.CLASSIC)
                WidgetRefreshHelper.refresh(requireContext())
            }
        }

        themeDark.setOnClickListener {
            if (!isInitializing) {
                WidgetStyleManager.applyPreset(requireContext(), ThemePreset.DARK)
                WidgetRefreshHelper.refresh(requireContext())
            }
        }

        colorShiftA.setOnRowClick {
            ColorPickerDialog {
                WidgetStyleManager.updateShiftAColor(requireContext(), it)
                WidgetRefreshHelper.refresh(requireContext())
                bindCurrentValues()
            }.show(parentFragmentManager, "colorA")
        }

        colorShiftB.setOnRowClick {
            ColorPickerDialog {
                WidgetStyleManager.updateShiftBColor(requireContext(), it)
                WidgetRefreshHelper.refresh(requireContext())
                bindCurrentValues()
            }.show(parentFragmentManager, "colorB")
        }

        colorBackground.setOnRowClick {
            ColorPickerDialog {
                WidgetStyleManager.updateWidgetBackgroundColor(requireContext(), it)
                WidgetRefreshHelper.refresh(requireContext())
                bindCurrentValues()
            }.show(parentFragmentManager, "colorBg")
        }

        prefixEdit.doAfterTextChanged {
            if (!isInitializing) {
                savePrefixText(it?.toString().orEmpty())
            }
        }

        widgetStyleClassic.setOnClickListener {
            if (!isInitializing) saveWidgetStyle(Prefs.WIDGET_STYLE_CLASSIC)
        }

        widgetStyleMinimal.setOnClickListener {
            if (!isInitializing) saveWidgetStyle(Prefs.WIDGET_STYLE_MINIMAL)
        }

        switchNotificationsEnabled.setOnCheckedChangeListener { _, isChecked ->
            if (isInitializing) return@setOnCheckedChangeListener

            if (isChecked) {
                val mainActivity = activity as? MainActivity
                if (mainActivity == null) {
                    isInitializing = true
                    switchNotificationsEnabled.isChecked = false
                    switchSilentNotification.isChecked = false
                    switchSilentNotification.isEnabled = false
                    isInitializing = false

                    saveNotificationSettings(enabled = false, silent = false)
                    MidnightAlarmScheduler.cancel(requireContext())
                    return@setOnCheckedChangeListener
                }

                mainActivity.requestNotificationPermissionIfNeeded { granted ->
                    if (granted) {
                        switchSilentNotification.isEnabled = true

                        saveNotificationSettings(
                            enabled = true,
                            silent = switchSilentNotification.isChecked
                        )
                        MidnightAlarmScheduler.scheduleNext(requireContext())
                    } else {
                        isInitializing = true
                        switchNotificationsEnabled.isChecked = false
                        switchSilentNotification.isChecked = false
                        switchSilentNotification.isEnabled = false
                        isInitializing = false

                        saveNotificationSettings(enabled = false, silent = false)
                        MidnightAlarmScheduler.cancel(requireContext())
                    }
                }
            } else {
                switchSilentNotification.isChecked = false
                switchSilentNotification.isEnabled = false

                saveNotificationSettings(enabled = false, silent = false)
                MidnightAlarmScheduler.cancel(requireContext())
            }
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

    private fun savePrefixText(prefix: String) {
        val prefs = requireContext().getSharedPreferences(AppPrefs.NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putString(AppPrefs.KEY_PREFIX_TEXT, prefix.trim())
        }
        refreshWidget()
    }

    private fun saveWidgetStyle(style: String) {
        val prefs = requireContext().getSharedPreferences(Prefs.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putString(Prefs.KEY_WIDGET_STYLE, style)
        }
        refreshWidget()
    }

    private fun saveNotificationSettings(enabled: Boolean, silent: Boolean) {
        val prefs = requireContext().getSharedPreferences(Prefs.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putBoolean(Prefs.KEY_NOTIFICATIONS_ENABLED, enabled)
            putBoolean(Prefs.KEY_SILENT_NOTIFICATION, silent)
        }
    }

    private fun refreshWidget() {
        val manager = AppWidgetManager.getInstance(requireContext())
        val ids = manager.getAppWidgetIds(
            ComponentName(requireContext(), WorkCycleWidgetProvider::class.java)
        )
        WorkCycleWidgetProvider().onUpdate(requireContext(), manager, ids)
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
