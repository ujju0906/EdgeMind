package com.ml.shubham0204.docqa.domain.sms

import android.content.Context
import android.provider.Telephony
import java.lang.Exception
import android.util.Log

data class SmsMessage(
    val sender: String,
    val body: String,
    val date: Long
)

class SmsReader(private val context: Context) {

    fun readLastSmsMessages(count: Int = 10): List<SmsMessage> {
        val messages = mutableListOf<SmsMessage>()
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
                        messages.add(
                            SmsMessage(
                                sender = cursor.getString(senderColumn),
                                body = cursor.getString(bodyColumn),
                                date = cursor.getLong(dateColumn)
                            )
                        )
                    } while (cursor.moveToNext())
                }
            }
        } catch (e: Exception) {
            Log.e("SmsReader", "Error reading SMS messages", e)
        }
        return messages
    }
} 