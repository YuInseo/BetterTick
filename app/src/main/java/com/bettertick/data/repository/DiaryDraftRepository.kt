package com.bettertick.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiaryDraftRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs by lazy {
        context.getSharedPreferences("diary_drafts", Context.MODE_PRIVATE)
    }

    fun saveDraft(dateStr: String, content: String) {
        prefs.edit().putString(dateStr, content).apply()
    }

    fun getDraft(dateStr: String): String? = prefs.getString(dateStr, null)

    fun deleteDraft(dateStr: String) {
        prefs.edit().remove(dateStr).apply()
    }
}
