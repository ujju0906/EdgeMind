package com.ml.EdgeMind.docqa.data

import io.objectbox.Box
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChatHistoryDB(private val box: Box<ChatMessage>) {

    suspend fun saveMessage(question: String, response: String, contextUsed: String = "", detailedContext: String = "") {
        withContext(Dispatchers.IO) {
            val message = ChatMessage(
                question = question,
                response = response,
                timestamp = System.currentTimeMillis(),
                isUserMessage = false,
                contextUsed = contextUsed,
                detailedContext = detailedContext
            )
            box.put(message)
        }
    }

    suspend fun saveUserMessage(question: String) {
        withContext(Dispatchers.IO) {
            val message = ChatMessage(
                question = question,
                response = "",
                timestamp = System.currentTimeMillis(),
                isUserMessage = true,
                contextUsed = ""
            )
            box.put(message)
            android.util.Log.d("ChatHistoryDB", "Saved user message: ${message.messageId} - ${message.question}")
        }
    }

    suspend fun getAllMessages(): List<ChatMessage> {
        return withContext(Dispatchers.IO) {
            box.query()
                .order(ChatMessage_.timestamp)
                .build()
                .find()
        }
    }

    suspend fun getRecentMessages(limit: Int = 50): List<ChatMessage> {
        return withContext(Dispatchers.IO) {
            val allMessages = box.query()
                .order(ChatMessage_.timestamp)
                .build()
                .find()
            val recentMessages = allMessages.takeLast(limit)
            android.util.Log.d("ChatHistoryDB", "Total messages in DB: ${allMessages.size}, returning ${recentMessages.size} recent messages")
            recentMessages
        }
    }

    suspend fun clearAllMessages() {
        withContext(Dispatchers.IO) {
            box.removeAll()
        }
    }

    suspend fun deleteMessage(messageId: Long) {
        withContext(Dispatchers.IO) {
            box.remove(messageId)
        }
    }

    suspend fun getMessageCount(): Long {
        return withContext(Dispatchers.IO) {
            box.count()
        }
    }

    suspend fun searchMessages(query: String): List<ChatMessage> {
        return withContext(Dispatchers.IO) {
            val questionQuery = box.query(ChatMessage_.question.contains(query))
                .build()
                .find()
            
            val responseQuery = box.query(ChatMessage_.response.contains(query))
                .build()
                .find()
            
            (questionQuery + responseQuery)
                .distinctBy { it.messageId }
                .sortedByDescending { it.timestamp }
        }
    }

    suspend fun saveStreamingAssistantMessage(message: ChatMessage): Long {
        return withContext(Dispatchers.IO) {
            box.put(message)
            android.util.Log.d("ChatHistoryDB", "Saved streaming assistant message: ${message.messageId} - ${message.question}")
            message.messageId
        }
    }

    suspend fun updateAssistantMessage(messageId: Long, response: String, contextUsed: String, detailedContext: String = "") {
        withContext(Dispatchers.IO) {
            val message = box.get(messageId)
            if (message != null) {
                message.response = response
                message.contextUsed = contextUsed
                message.detailedContext = detailedContext
                box.put(message)
                android.util.Log.d("ChatHistoryDB", "Updated assistant message: $messageId with response length: ${response.length}")
            } else {
                android.util.Log.e("ChatHistoryDB", "Failed to find message with ID: $messageId")
            }
        }
    }
} 