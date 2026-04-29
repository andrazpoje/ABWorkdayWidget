package com.dante.workcycle.debug

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.dante.workcycle.BuildConfig
import com.dante.workcycle.WorkCycleApp
import com.dante.workcycle.data.prefs.LaunchPrefs
import com.dante.workcycle.data.prefs.Prefs
import com.dante.workcycle.ui.activity.MainActivity
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Debug-only reset helpers for local onboarding and data testing.
 *
 * Every public entry point is guarded by [BuildConfig.DEBUG] so these actions
 * remain inert in release builds even if called accidentally.
 */
object DebugDataResetHelper {

    fun isDeveloperToolsUnlocked(context: Context): Boolean {
        if (!BuildConfig.DEBUG) return false

        return context.applicationContext
            .getSharedPreferences(Prefs.PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_DEVELOPER_TOOLS_UNLOCKED, false)
    }

    fun unlockDeveloperTools(context: Context): Boolean {
        if (!BuildConfig.DEBUG) return false

        context.applicationContext
            .getSharedPreferences(Prefs.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DEVELOPER_TOOLS_UNLOCKED, true)
            .apply()

        return true
    }

    fun resetOnboardingTestState(context: Context): Boolean {
        if (!BuildConfig.DEBUG) return false

        LaunchPrefs(context.applicationContext).clearOnboardingTestStateForDebug()
        return true
    }

    suspend fun clearLocalAppData(context: Context): Boolean {
        if (!BuildConfig.DEBUG) return false

        val appContext = context.applicationContext

        withContext(Dispatchers.IO) {
            clearRoomData(appContext)
            clearSharedPreferences(appContext)
        }

        return true
    }

    fun restartApp(activity: Activity) {
        if (!BuildConfig.DEBUG) return

        val intent = Intent(activity, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }

        activity.startActivity(intent)
        activity.finishAffinity()
    }

    private fun clearRoomData(context: Context) {
        val app = context.applicationContext as? WorkCycleApp

        val cleared = app?.let {
            runCatching {
                it.database.clearAllTables()
            }.isSuccess
        } == true

        if (!cleared) {
            context.deleteDatabase(DATABASE_NAME)
        }
    }

    private fun clearSharedPreferences(context: Context) {
        val sharedPrefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
        val files = sharedPrefsDir.listFiles { file ->
            file.isFile && file.extension == "xml"
        }.orEmpty()

        files.forEach { file ->
            context.getSharedPreferences(file.nameWithoutExtension, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit()
        }
    }

    private const val DATABASE_NAME = "workcycle.db"
    private const val KEY_DEVELOPER_TOOLS_UNLOCKED = "debug_developer_tools_unlocked"
}
