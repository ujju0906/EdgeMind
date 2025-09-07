package com.ml.EdgeMind.docqa.domain.sms

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

    fun isCallLogAccessible(): Boolean {
        if (!hasPermission()) {
            Log.w("CallLogsReader", "Call log not accessible - permission not granted")
            return false
        }
        
        return try {
            val projection = arrayOf(CallLog.Calls.NUMBER)
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                null
            )
            cursor?.close()
            true
        } catch (e: SecurityException) {
            Log.e("CallLogsReader", "Security exception checking call log access: ${e.message}", e)
            false
        } catch (e: Exception) {
            Log.e("CallLogsReader", "Exception checking call log access: ${e.message}", e)
            false
        }
    }

    fun readLastCallLogs(count: Int = 10): List<CallLogEntry> {
        val callLogs = mutableListOf<CallLogEntry>()
        
        // Check permission first
        if (!hasPermission()) {
            Log.w("CallLogsReader", "No READ_CALL_LOG permission - cannot read call logs")
            return callLogs
        }
        
        // Check if call log is accessible
        if (!isCallLogAccessible()) {
            Log.w("CallLogsReader", "Call log not accessible despite permission being granted")
            return callLogs
        }
        
        Log.d("CallLogsReader", "Starting call log read operation for $count entries on Android ${Build.VERSION.SDK_INT}")
        
        try {
            val projection = arrayOf(
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION,
                CallLog.Calls.TYPE
            )

            // Use safer column index checking
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                "${CallLog.Calls.DATE} DESC"
            )?.use { cursor ->
                Log.d("CallLogsReader", "Call log cursor returned with ${cursor.count} rows")
                
                if (cursor.moveToFirst()) {
                    // Safely get column indices
                    val numberColumn = cursor.getColumnIndex(CallLog.Calls.NUMBER)
                    val nameColumn = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
                    val dateColumn = cursor.getColumnIndex(CallLog.Calls.DATE)
                    val durationColumn = cursor.getColumnIndex(CallLog.Calls.DURATION)
                    val typeColumn = cursor.getColumnIndex(CallLog.Calls.TYPE)
                    
                    // Check if required columns exist
                    if (numberColumn == -1 || dateColumn == -1 || typeColumn == -1) {
                        Log.e("CallLogsReader", "Required columns not found in call log cursor")
                        return callLogs
                    }
                    
                    var processedCount = 0
                    
                    do {
                        try {
                            // Safely read values with null checks
                            val callDate = if (dateColumn != -1) cursor.getLong(dateColumn) else 0L
                            val callDuration = if (durationColumn != -1) cursor.getLong(durationColumn) else 0L
                            val callNumber = if (numberColumn != -1) cursor.getString(numberColumn) ?: "Unknown" else "Unknown"
                            val callName = if (nameColumn != -1) cursor.getString(nameColumn) else null
                            
                            val callType = if (typeColumn != -1) {
                                when (cursor.getInt(typeColumn)) {
                                    CallLog.Calls.INCOMING_TYPE -> "INCOMING"
                                    CallLog.Calls.OUTGOING_TYPE -> "OUTGOING"
                                    CallLog.Calls.MISSED_TYPE -> "MISSED"
                                    CallLog.Calls.VOICEMAIL_TYPE -> "VOICEMAIL"
                                    CallLog.Calls.REJECTED_TYPE -> "REJECTED"
                                    CallLog.Calls.BLOCKED_TYPE -> "BLOCKED"
                                    else -> "UNKNOWN"
                                }
                            } else "UNKNOWN"

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
                            
                            // Limit the number of entries
                            if (processedCount >= count) {
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
        } catch (e: SecurityException) {
            Log.e("CallLogsReader", "Security exception reading call logs - permission may have been revoked: ${e.message}", e)
        } catch (e: IllegalArgumentException) {
            Log.e("CallLogsReader", "Illegal argument exception - column may not exist: ${e.message}", e)
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
            return "No call logs found. This could be due to:\n" +
                   "1. No call history on this device\n" +
                   "2. Call log permission not granted\n" +
                   "3. Call log access restricted by system"
        }

        val stringBuilder = StringBuilder()
        stringBuilder.append("Recent Call Logs (${callLogs.size} entries):\n\n")
        
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

    fun getDiagnosticInfo(): String {
        val hasPermission = hasPermission()
        val isAccessible = isCallLogAccessible()
        val testRead = readLastCallLogs(count = 1)
        
        return """
            Call Log Diagnostic Information:
            - Permission granted: $hasPermission
            - Call log accessible: $isAccessible
            - Test read successful: ${testRead.isNotEmpty()}
            - Android version: ${Build.VERSION.SDK_INT}
            - Sample entries found: ${testRead.size}
        """.trimIndent()
    }
} 