package com.ml.shubham0204.docqa.domain.sms

import android.content.Context
import android.provider.CallLog
import java.lang.Exception
import android.util.Log

data class CallLogEntry(
    val number: String,
    val name: String?,
    val date: Long,
    val duration: Long,
    val type: String
)

class CallLogsReader(private val context: Context) {

    fun readLastCallLogs(count: Int = 10): List<CallLogEntry> {
        val callLogs = mutableListOf<CallLogEntry>()
        try {
            val projection =
                arrayOf(
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.CACHED_NAME,
                    CallLog.Calls.DATE,
                    CallLog.Calls.DURATION,
                    CallLog.Calls.TYPE)

            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                "${CallLog.Calls.DATE} DESC"
            )?.use {
                if (it.moveToFirst()) {
                    val numberColumn = it.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                    val nameColumn = it.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
                    val dateColumn = it.getColumnIndexOrThrow(CallLog.Calls.DATE)
                    val durationColumn = it.getColumnIndexOrThrow(CallLog.Calls.DURATION)
                    val typeColumn = it.getColumnIndexOrThrow(CallLog.Calls.TYPE)
                    var processedCount = 0
                    do {
                        val callType =
                            when (it.getInt(typeColumn)) {
                                CallLog.Calls.INCOMING_TYPE -> "INCOMING"
                                CallLog.Calls.OUTGOING_TYPE -> "OUTGOING"
                                CallLog.Calls.MISSED_TYPE -> "MISSED"
                                CallLog.Calls.VOICEMAIL_TYPE -> "VOICEMAIL"
                                CallLog.Calls.REJECTED_TYPE -> "REJECTED"
                                CallLog.Calls.BLOCKED_TYPE -> "BLOCKED"
                                else -> "UNKNOWN"
                            }

                        callLogs.add(
                            CallLogEntry(
                                number = it.getString(numberColumn),
                                name = it.getString(nameColumn),
                                date = it.getLong(dateColumn),
                                duration = it.getLong(durationColumn),
                                type = callType))
                        processedCount++
                    } while (it.moveToNext() && processedCount < count)
                }
            }
        } catch (e: Exception) {
            Log.e("CallLogsReader", "Error reading call logs", e)
        }
        return callLogs
    }
} 