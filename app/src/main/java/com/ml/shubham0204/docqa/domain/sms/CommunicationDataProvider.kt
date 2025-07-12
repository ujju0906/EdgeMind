package com.ml.shubham0204.docqa.domain.sms

import android.content.Context
import android.util.Log

class CommunicationDataProvider(private val context: Context) {
    
    private val callLogsReader = CallLogsReader(context)
    private val smsReader = SmsReader(context)
    
    fun getCommunicationSummary(callLogCount: Int = 10, smsCount: Int = 10): String {
        val stringBuilder = StringBuilder()
        
        try {
            // Get call logs summary
            val callLogsText = callLogsReader.getCallLogsAsText(callLogCount)
            stringBuilder.append("📞 CALL LOGS:\n")
            stringBuilder.append(callLogsText)
            stringBuilder.append("\n")
            
            // Get SMS summary
            val smsText = smsReader.getSmsSummary(smsCount)
            stringBuilder.append("💬 SMS MESSAGES:\n")
            stringBuilder.append(smsText)
            
        } catch (e: Exception) {
            Log.e("CommunicationDataProvider", "Error getting communication summary", e)
            stringBuilder.append("Error retrieving communication data: ${e.message}")
        }
        
        return stringBuilder.toString()
    }
    
    fun getRecentActivitySummary(hours: Int = 24): String {
        val stringBuilder = StringBuilder()
        val currentTime = System.currentTimeMillis()
        val timeThreshold = currentTime - (hours * 60 * 60 * 1000L)
        
        try {
            // Get recent call logs
            val recentCallLogs = callLogsReader.readLastCallLogs(50)
                .filter { it.date >= timeThreshold }
            
            // Get recent SMS
            val recentSms = smsReader.readLastSmsMessages(50)
                .filter { it.date >= timeThreshold }
            
            stringBuilder.append("📱 RECENT ACTIVITY (Last $hours hours):\n\n")
            
            if (recentCallLogs.isNotEmpty()) {
                stringBuilder.append("📞 Recent Calls:\n")
                recentCallLogs.take(5).forEach { call ->
                    stringBuilder.append("   • ${call.formattedDateTime} - ${call.type} call from ${call.name ?: call.number}")
                    if (call.duration > 0) {
                        stringBuilder.append(" (${call.formattedDuration})")
                    }
                    stringBuilder.append("\n")
                }
                stringBuilder.append("\n")
            }
            
            if (recentSms.isNotEmpty()) {
                stringBuilder.append("💬 Recent Messages:\n")
                recentSms.take(5).forEach { sms ->
                    stringBuilder.append("   • ${sms.formattedDateTime} - ${sms.sender}: ${sms.body.take(30)}${if (sms.body.length > 30) "..." else ""}\n")
                }
                stringBuilder.append("\n")
            }
            
            if (recentCallLogs.isEmpty() && recentSms.isEmpty()) {
                stringBuilder.append("No recent communication activity found.\n")
            }
            
        } catch (e: Exception) {
            Log.e("CommunicationDataProvider", "Error getting recent activity", e)
            stringBuilder.append("Error retrieving recent activity: ${e.message}")
        }
        
        return stringBuilder.toString()
    }
    
    fun getContactActivitySummary(contactNumber: String): String {
        val stringBuilder = StringBuilder()
        
        try {
            // Get calls with this contact
            val contactCalls = callLogsReader.readLastCallLogs(100)
                .filter { it.number.contains(contactNumber) || contactNumber.contains(it.number) }
            
            // Get SMS with this contact
            val contactSms = smsReader.readLastSmsMessages(100)
                .filter { it.sender.contains(contactNumber) || contactNumber.contains(it.sender) }
            
            stringBuilder.append("👤 CONTACT ACTIVITY SUMMARY:\n\n")
            stringBuilder.append("Contact: ${contactCalls.firstOrNull()?.name ?: contactNumber}\n\n")
            
            if (contactCalls.isNotEmpty()) {
                stringBuilder.append("📞 Call History:\n")
                contactCalls.take(10).forEach { call ->
                    stringBuilder.append("   • ${call.formattedDateTime} - ${call.type}")
                    if (call.duration > 0) {
                        stringBuilder.append(" (${call.formattedDuration})")
                    }
                    stringBuilder.append("\n")
                }
                stringBuilder.append("\n")
            }
            
            if (contactSms.isNotEmpty()) {
                stringBuilder.append("💬 Message History:\n")
                contactSms.take(10).forEach { sms ->
                    stringBuilder.append("   • ${sms.formattedDateTime}: ${sms.body.take(50)}${if (sms.body.length > 50) "..." else ""}\n")
                }
                stringBuilder.append("\n")
            }
            
            if (contactCalls.isEmpty() && contactSms.isEmpty()) {
                stringBuilder.append("No communication history found for this contact.\n")
            }
            
        } catch (e: Exception) {
            Log.e("CommunicationDataProvider", "Error getting contact activity", e)
            stringBuilder.append("Error retrieving contact activity: ${e.message}")
        }
        
        return stringBuilder.toString()
    }
    
    fun getCommunicationStats(): String {
        val stringBuilder = StringBuilder()
        
        try {
            val allCallLogs = callLogsReader.readLastCallLogs(1000)
            val allSms = smsReader.readLastSmsMessages(1000)
            
            val incomingCalls = allCallLogs.count { it.type == "INCOMING" }
            val outgoingCalls = allCallLogs.count { it.type == "OUTGOING" }
            val missedCalls = allCallLogs.count { it.type == "MISSED" }
            val totalCallDuration = allCallLogs.sumOf { it.duration }
            
            val uniqueContacts = allSms.map { it.sender }.distinct().size
            
            stringBuilder.append("📊 COMMUNICATION STATISTICS:\n\n")
            stringBuilder.append("📞 Call Statistics:\n")
            stringBuilder.append("   • Total Calls: ${allCallLogs.size}\n")
            stringBuilder.append("   • Incoming: $incomingCalls\n")
            stringBuilder.append("   • Outgoing: $outgoingCalls\n")
            stringBuilder.append("   • Missed: $missedCalls\n")
            stringBuilder.append("   • Total Duration: ${formatDuration(totalCallDuration)}\n\n")
            
            stringBuilder.append("💬 SMS Statistics:\n")
            stringBuilder.append("   • Total Messages: ${allSms.size}\n")
            stringBuilder.append("   • Unique Contacts: $uniqueContacts\n")
            
        } catch (e: Exception) {
            Log.e("CommunicationDataProvider", "Error getting communication stats", e)
            stringBuilder.append("Error retrieving communication statistics: ${e.message}")
        }
        
        return stringBuilder.toString()
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
} 