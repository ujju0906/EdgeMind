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
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun readLastSmsMessages(count: Int = 10): List<SmsMessage> {
        val messages = mutableListOf<SmsMessage>()
        
        // Check permission first
        if (!hasPermission()) {
            Log.w("SmsReader", "No READ_SMS permission")
            return messages
        }
        
        try {
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
                null,
                null,
                "${Telephony.Sms.DATE} DESC LIMIT $count"
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val senderColumn = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                    val bodyColumn = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                    val dateColumn = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
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
                        } catch (e: Exception) {
                            Log.e("SmsReader", "Error processing SMS message: ${e.message}", e)
                            // Continue with next message
                        }
                    } while (cursor.moveToNext())
                }
            }
        } catch (e: Exception) {
            Log.e("SmsReader", "Error reading SMS messages: ${e.message}", e)
        }
        return messages
    }

    fun getSmsMessagesAsText(count: Int = 10): String {
        val messages = readLastSmsMessages(count)
        if (messages.isEmpty()) {
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