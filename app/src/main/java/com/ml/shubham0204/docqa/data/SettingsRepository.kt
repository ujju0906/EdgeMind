package com.ml.shubham0204.docqa.data

import android.content.Context
import android.content.SharedPreferences

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("docqa_settings", Context.MODE_PRIVATE)

    companion object {
        const val KEY_TOP_P = "top_p"
        const val KEY_TOP_K = "top_k"
        const val KEY_TEMPERATURE = "temperature"
        const val KEY_MAX_TOKENS = "max_tokens"
        const val KEY_RECENT_MESSAGES = "recent_messages"
        const val KEY_RECENT_CALL_LOGS = "recent_call_logs"
    }

    fun saveTopP(value: Float) = prefs.edit().putFloat(KEY_TOP_P, value).apply()
    fun getTopP(): Float = prefs.getFloat(KEY_TOP_P, 0.9f)

    fun saveTopK(value: Int) = prefs.edit().putInt(KEY_TOP_K, value).apply()
    fun getTopK(): Int = prefs.getInt(KEY_TOP_K, 40)

    fun saveTemperature(value: Float) = prefs.edit().putFloat(KEY_TEMPERATURE, value).apply()
    fun getTemperature(): Float = prefs.getFloat(KEY_TEMPERATURE, 0.8f)

    fun saveMaxTokens(value: Int) = prefs.edit().putInt(KEY_MAX_TOKENS, value).apply()
    fun getMaxTokens(): Int = prefs.getInt(KEY_MAX_TOKENS, 1024)

    fun saveRecentMessages(value: Int) = prefs.edit().putInt(KEY_RECENT_MESSAGES, value).apply()
    fun getRecentMessages(): Int = prefs.getInt(KEY_RECENT_MESSAGES, 10)

    fun saveRecentCallLogs(value: Int) = prefs.edit().putInt(KEY_RECENT_CALL_LOGS, value).apply()
    fun getRecentCallLogs(): Int = prefs.getInt(KEY_RECENT_CALL_LOGS, 10)
} 