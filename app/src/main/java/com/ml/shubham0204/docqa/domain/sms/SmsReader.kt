package com.ml.shubham0204.docqa.domain.sms

import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import java.lang.Exception
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.content.ContextCompat
import android.Manifest
import android.os.Build

data class SmsMessage(
    val sender: String,
    val body: String,
    val date: Long,
    val formattedDate: String,
    val formattedTime: String,
    val formattedDateTime: String
)

class SmsReader(private val context: Context) {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val fullDateTimeFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())

    fun hasPermission(): Boolean {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
        
        Log.d("SmsReader", "SMS permission check: $hasPermission (Android ${Build.VERSION.SDK_INT})")
        return hasPermission
    }

    fun readLastSmsMessages(count: Int = 10): List<SmsMessage> {
        val messages = mutableListOf<SmsMessage>()
        
        // Check permission first
        if (!hasPermission()) {
            Log.w("SmsReader", "No READ_SMS permission - cannot read SMS messages")
            return messages
        }
        
        Log.d("SmsReader", "Starting SMS read operation for $count messages on Android ${Build.VERSION.SDK_INT}")
        
        try {
            // For Android 15+, we need to be more careful with content provider access
            val query = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Android 15+ - use more specific query
                "${Telephony.Sms.DATE} DESC"
            } else {
                // Older versions
                "${Telephony.Sms.DATE} DESC LIMIT $count"
            }
            
            Log.d("SmsReader", "Querying SMS with: $query")
            
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
                null,
                null,
                query
            )?.use { cursor ->
                Log.d("SmsReader", "SMS cursor returned with ${cursor.count} rows")
                
                if (cursor.moveToFirst()) {
                    val senderColumn = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                    val bodyColumn = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                    val dateColumn = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
                    var processedCount = 0
                    
                    do {
                        try {
                            val messageDate = cursor.getLong(dateColumn)
                            val messageSender = cursor.getString(senderColumn) ?: "Unknown"
                            val messageBody = cursor.getString(bodyColumn) ?: ""
                            val date = Date(messageDate)
                            
                            messages.add(
                                SmsMessage(
                                    sender = messageSender,
                                    body = messageBody,
                                    date = messageDate,
                                    formattedDate = dateFormat.format(date),
                                    formattedTime = timeFormat.format(date),
                                    formattedDateTime = fullDateTimeFormat.format(date)
                                )
                            )
                            processedCount++
                            
                            // Limit the number of messages for Android 15+
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && processedCount >= count) {
                                break
                            }
                        } catch (e: Exception) {
                            Log.e("SmsReader", "Error processing SMS message: ${e.message}", e)
                            // Continue with next message
                        }
                    } while (cursor.moveToNext())
                    
                    Log.d("SmsReader", "Successfully processed $processedCount SMS messages")
                } else {
                    Log.w("SmsReader", "SMS cursor is empty - no messages found")
                }
            } ?: run {
                Log.e("SmsReader", "SMS content resolver query returned null")
            }
        } catch (e: Exception) {
            Log.e("SmsReader", "Error reading SMS messages: ${e.message}", e)
            Log.e("SmsReader", "Stack trace: ${e.stackTraceToString()}")
        }
        
        Log.d("SmsReader", "SMS read operation completed. Found ${messages.size} messages")
        return messages
    }

    fun getSmsMessagesAsText(count: Int = 10): String {
        val messages = readLastSmsMessages(count)
        if (messages.isEmpty()) {
            Log.w("SmsReader", "No SMS messages found for text conversion")
            return "No SMS messages found."
        }

        val stringBuilder = StringBuilder()
        stringBuilder.append("Recent SMS Messages:\n\n")
        
        messages.forEachIndexed { index, message ->
            stringBuilder.append("${index + 1}. ")
            stringBuilder.append("From: ${message.sender}\n")
            stringBuilder.append("   Date: ${message.formattedDate}\n")
            stringBuilder.append("   Time: ${message.formattedTime}\n")
            stringBuilder.append("   Message: ${message.body}\n")
            stringBuilder.append("\n")
        }
        
        return stringBuilder.toString()
    }

    fun getSmsSummary(count: Int = 10): String {
        val messages = readLastSmsMessages(count)
        if (messages.isEmpty()) {
            Log.w("SmsReader", "No SMS messages found for summary")
            return "No SMS messages found."
        }

        val stringBuilder = StringBuilder()
        stringBuilder.append("SMS Summary (Last $count messages):\n\n")
        
        // Group by sender
        val messagesBySender = messages.groupBy { it.sender }
        
        messagesBySender.forEach { (sender, senderMessages) ->
            stringBuilder.append("📱 $sender (${senderMessages.size} messages):\n")
            senderMessages.forEach { message ->
                stringBuilder.append("   • ${message.formattedDateTime}: ${message.body.take(50)}${if (message.body.length > 50) "..." else ""}\n")
            }
            stringBuilder.append("\n")
        }
        
        return stringBuilder.toString()
    }
} 