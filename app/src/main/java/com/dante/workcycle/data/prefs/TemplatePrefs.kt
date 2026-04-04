package com.dante.workcycle.data.prefs

import android.content.Context
import androidx.core.content.edit

object TemplatePrefs {

    private const val PREFS_NAME = "template_prefs"
    private const val KEY_ACTIVE_TEMPLATE_ID = "active_template_id"

    fun getActiveTemplateId(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ACTIVE_TEMPLATE_ID, null)
    }

    fun setActiveTemplateId(context: Context, templateId: String?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                putString(KEY_ACTIVE_TEMPLATE_ID, templateId)
            }
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                remove(KEY_ACTIVE_TEMPLATE_ID)
            }
    }
}