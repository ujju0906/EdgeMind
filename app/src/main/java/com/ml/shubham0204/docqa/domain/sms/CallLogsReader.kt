package com.ml.shubham0204.docqa.domain.sms

import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import java.lang.Exception
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.content.ContextCompat
import android.Manifest
import android.os.Build

data class CallLogEntry(
    val number: String,
    val name: String?,
    val date: Long,
    val duration: Long,
    val type: String,
    val formattedDate: String,
    val formattedTime: String,
    val formattedDateTime: String,
    val formattedDuration: String
)

class CallLogsReader(private val context: Context) {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val fullDateTimeFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())

    fun hasPermission(): Boolean {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED
        
        Log.d("CallLogsReader", "Call log permission check: $hasPermission (Android ${Build.VERSION.SDK_INT})")
        return hasPermission
    }

    fun readLastCallLogs(count: Int = 10): List<CallLogEntry> {
        val callLogs = mutableListOf<CallLogEntry>()
        
        // Check permission first
        if (!hasPermission()) {
            Log.w("CallLogsReader", "No READ_CALL_LOG permission - cannot read call logs")
            return callLogs
        }
        
        Log.d("CallLogsReader", "Starting call log read operation for $count entries on Android ${Build.VERSION.SDK_INT}")
        
        try {
            val projection =
                arrayOf(
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.CACHED_NAME,
                    CallLog.Calls.DATE,
                    CallLog.Calls.DURATION,
                    CallLog.Calls.TYPE)

            // For Android 15+, we need to be more careful with content provider access
            val query = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Android 15+ - use more specific query without LIMIT clause
                "${CallLog.Calls.DATE} DESC"
            } else {
                // Older versions
                "${CallLog.Calls.DATE} DESC"
            }
            
            Log.d("CallLogsReader", "Querying call logs with: $query")

            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                query
            )?.use { cursor ->
                Log.d("CallLogsReader", "Call log cursor returned with ${cursor.count} rows")
                
                if (cursor.moveToFirst()) {
                    val numberColumn = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                    val nameColumn = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
                    val dateColumn = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
                    val durationColumn = cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION)
                    val typeColumn = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
                    var processedCount = 0
                    
                    do {
                        try {
                            val callDate = cursor.getLong(dateColumn)
                            val callDuration = cursor.getLong(durationColumn)
                            val callNumber = cursor.getString(numberColumn) ?: "Unknown"
                            val callName = cursor.getString(nameColumn)
                            
                            val callType =
                                when (cursor.getInt(typeColumn)) {
                                    CallLog.Calls.INCOMING_TYPE -> "INCOMING"
                                    CallLog.Calls.OUTGOING_TYPE -> "OUTGOING"
                                    CallLog.Calls.MISSED_TYPE -> "MISSED"
                                    CallLog.Calls.VOICEMAIL_TYPE -> "VOICEMAIL"
                                    CallLog.Calls.REJECTED_TYPE -> "REJECTED"
                                    CallLog.Calls.BLOCKED_TYPE -> "BLOCKED"
                                    else -> "UNKNOWN"
                                }

                            // Format date and time
                            val date = Date(callDate)
                            val formattedDate = dateFormat.format(date)
                            val formattedTime = timeFormat.format(date)
                            val formattedDateTime = fullDateTimeFormat.format(date)
                            val formattedDuration = formatDuration(callDuration)

                            callLogs.add(
                                CallLogEntry(
                                    number = callNumber,
                                    name = callName,
                                    date = callDate,
                                    duration = callDuration,
                                    type = callType,
                                    formattedDate = formattedDate,
                                    formattedTime = formattedTime,
                                    formattedDateTime = formattedDateTime,
                                    formattedDuration = formattedDuration
                                ))
                            processedCount++
                            
                            // Limit the number of entries for Android 15+
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && processedCount >= count) {
                                break
                            }
                        } catch (e: Exception) {
                            Log.e("CallLogsReader", "Error processing call log entry: ${e.message}", e)
                            // Continue with next entry
                        }
                    } while (cursor.moveToNext())
                    
                    Log.d("CallLogsReader", "Successfully processed $processedCount call log entries")
                } else {
                    Log.w("CallLogsReader", "Call log cursor is empty - no entries found")
                }
            } ?: run {
                Log.e("CallLogsReader", "Call log content resolver query returned null")
            }
        } catch (e: Exception) {
            Log.e("CallLogsReader", "Error reading call logs: ${e.message}", e)
            Log.e("CallLogsReader", "Stack trace: ${e.stackTraceToString()}")
        }
        
        Log.d("CallLogsReader", "Call log read operation completed. Found ${callLogs.size} entries")
        return callLogs
    }

    private fun formatDuration(durationSeconds: Long): String {
        return when {
            durationSeconds <= 0 -> "0 seconds"
            durationSeconds < 60 -> "$durationSeconds seconds"
            durationSeconds < 3600 -> {
                val minutes = durationSeconds / 60
                val seconds = durationSeconds % 60
                if (seconds > 0) "$minutes min $seconds sec" else "$minutes min"
            }
            else -> {
                val hours = durationSeconds / 3600
                val minutes = (durationSeconds % 3600) / 60
                val seconds = durationSeconds % 60
                when {
                    minutes > 0 && seconds > 0 -> "$hours hr $minutes min $seconds sec"
                    minutes > 0 -> "$hours hr $minutes min"
                    else -> "$hours hr"
                }
            }
        }
    }

    fun getCallLogsAsText(count: Int = 10): String {
        val callLogs = readLastCallLogs(count)
        if (callLogs.isEmpty()) {
            Log.w("CallLogsReader", "No call logs found for text conversion")
            return "No call logs found."
        }

        val stringBuilder = StringBuilder()
        stringBuilder.append("Recent Call Logs:\n\n")
        
        callLogs.forEachIndexed { index, call ->
            stringBuilder.append("${index + 1}. ")
            stringBuilder.append("Type: ${call.type}\n")
            stringBuilder.append("   Number: ${call.number}\n")
            if (!call.name.isNullOrEmpty()) {
                stringBuilder.append("   Name: ${call.name}\n")
            }
            stringBuilder.append("   Date: ${call.formattedDate}\n")
            stringBuilder.append("   Time: ${call.formattedTime}\n")
            if (call.duration > 0) {
                stringBuilder.append("   Duration: ${call.formattedDuration}\n")
            }
            stringBuilder.append("\n")
        }
        
        return stringBuilder.toString()
    }
} 