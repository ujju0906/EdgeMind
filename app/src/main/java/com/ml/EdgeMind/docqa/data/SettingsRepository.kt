package com.ml.EdgeMind.docqa.data

import android.content.Context
import android.content.SharedPreferences

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("docqa_settings", Context.MODE_PRIVATE)

    companion object {
        const val KEY_MAX_TOKENS = "max_tokens"
        const val KEY_RECENT_MESSAGES = "recent_messages"
        const val KEY_RECENT_CALL_LOGS = "recent_call_logs"
        const val KEY_RAG_TOP_K = "rag_top_k"
        const val KEY_HF_TOKEN = "hf_token"
    }

    fun saveMaxTokens(value: Int) = prefs.edit().putInt(KEY_MAX_TOKENS, value).apply()
    fun getMaxTokens(): Int = prefs.getInt(KEY_MAX_TOKENS, 1024)

    fun saveRecentMessages(value: Int) = prefs.edit().putInt(KEY_RECENT_MESSAGES, value).apply()
    fun getRecentMessages(): Int = prefs.getInt(KEY_RECENT_MESSAGES, 5)

    fun saveRecentCallLogs(value: Int) = prefs.edit().putInt(KEY_RECENT_CALL_LOGS, value).apply()
    fun getRecentCallLogs(): Int = prefs.getInt(KEY_RECENT_CALL_LOGS, 10)

    fun saveRagTopK(value: Int) = prefs.edit().putInt(KEY_RAG_TOP_K, value).apply()
    fun getRagTopK(): Int = prefs.getInt(KEY_RAG_TOP_K, 3)

    fun saveHuggingFaceToken(token: String) = prefs.edit().putString(KEY_HF_TOKEN, token).apply()
    fun getHuggingFaceToken(): String? = prefs.getString(KEY_HF_TOKEN, null)
} 