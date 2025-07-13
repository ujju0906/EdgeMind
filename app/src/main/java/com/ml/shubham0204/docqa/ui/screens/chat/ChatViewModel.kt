package com.ml.shubham0204.docqa.ui.screens.chat

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ml.shubham0204.docqa.data.ChunksDB
import com.ml.shubham0204.docqa.data.ChatHistoryDB
import com.ml.shubham0204.docqa.data.ChatMessage
import com.ml.shubham0204.docqa.data.DocumentsDB
import com.ml.shubham0204.docqa.data.GeminiAPIKey
import com.ml.shubham0204.docqa.data.RetrievedContext
import com.ml.shubham0204.docqa.domain.actions.ActionMatcher
import com.ml.shubham0204.docqa.domain.embeddings.SentenceEmbeddingProvider
import com.ml.shubham0204.docqa.domain.llm.AppLLMProvider
import com.ml.shubham0204.docqa.domain.llm.LLMFactory
import com.ml.shubham0204.docqa.domain.llm.LLMInitializationState
import com.ml.shubham0204.docqa.domain.llm.LLMProvider
import com.ml.shubham0204.docqa.domain.sms.CallLogsReader
import com.ml.shubham0204.docqa.domain.sms.SmsReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.util.Log

class ChatViewModel(
    private val documentsDB: DocumentsDB,
    private val chunksDB: ChunksDB,
    private val chatHistoryDB: ChatHistoryDB,
    private val geminiAPIKey: GeminiAPIKey,
    private val sentenceEncoder: SentenceEmbeddingProvider,
    private val llmFactory: LLMFactory,
    private val smsReader: SmsReader,
    private val callLogsReader: CallLogsReader,
    private val actionMatcher: ActionMatcher
) : ViewModel() {

    private val _questionState = MutableStateFlow("")
    val questionState: StateFlow<String> = _questionState

    private val _responseState = MutableStateFlow("")
    val responseState: StateFlow<String> = _responseState

    private val _isGeneratingResponseState = MutableStateFlow(false)
    val isGeneratingResponseState: StateFlow<Boolean> = _isGeneratingResponseState

    val isModelInitialized = AppLLMProvider.isInitialized

    val llmInitializationState = AppLLMProvider.initializationState

    private val _isSmsContextEnabled = MutableStateFlow(false)
    val isSmsContextEnabled: StateFlow<Boolean> = _isSmsContextEnabled

    private val _isCallLogContextEnabled = MutableStateFlow(false)
    val isCallLogContextEnabled: StateFlow<Boolean> = _isCallLogContextEnabled

    private val _isDocumentContextEnabled = MutableStateFlow(false)
    val isDocumentContextEnabled: StateFlow<Boolean> = _isDocumentContextEnabled

    private val _retrievedContextListState = MutableStateFlow(emptyList<RetrievedContext>())
    val retrievedContextListState: StateFlow<List<RetrievedContext>> = _retrievedContextListState

    private val _currentContextState = MutableStateFlow("")
    val currentContextState: StateFlow<String> = _currentContextState

    private val _chatHistoryState = MutableStateFlow(emptyList<ChatMessage>())
    val chatHistoryState: StateFlow<List<ChatMessage>> = _chatHistoryState

    private val _actionsEnabled = MutableStateFlow(false)
    val actionsEnabled: StateFlow<Boolean> = _actionsEnabled

    fun toggleActionsEnabled() {
        _actionsEnabled.value = !_actionsEnabled.value
    }

    private var currentLLMProvider: LLMProvider? = null

    init {
        loadChatHistory()
        // Debug chat history on init
        debugChatHistory()
        
        // Force create a test message for debugging
        viewModelScope.launch {
            delay(1000) // Wait a bit for initialization
            try {
                val count = chatHistoryDB.getMessageCount()
                if (count == 0L) {
                    Log.d("ChatViewModel", "Creating test messages for debugging")
                    chatHistoryDB.saveUserMessage("Test user message")
                    chatHistoryDB.saveMessage("Test user message", "Test assistant response", "SMS", "Test detailed context")
                    loadChatHistory()
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error creating test messages: ${e.message}", e)
            }
        }
    }

    fun toggleSmsContext() {
        _isSmsContextEnabled.value = !_isSmsContextEnabled.value
    }

    fun toggleCallLogContext() {
        _isCallLogContextEnabled.value = !_isCallLogContextEnabled.value
    }

    fun toggleDocumentContext() {
        _isDocumentContextEnabled.value = !_isDocumentContextEnabled.value
    }

    fun stopGeneration() {
        Log.d("ChatViewModel", "Stopping generation")
        currentLLMProvider?.stopGeneration()
        currentLLMProvider = null
        _isGeneratingResponseState.value = false
        _currentContextState.value = "" // Clear context when stopping
        
        // Add a small delay to ensure proper cleanup
        viewModelScope.launch {
            delay(100) // Small delay to ensure LLM is properly reset
        }
        
        // Clear the response state to indicate cancellation
        if (_responseState.value.isNotEmpty()) {
            _responseState.value += "\n\n[Response stopped by user"
        }
    }

    fun getAnswer(
        query: String,
        prompt: String,
    ) {
        // Prevent multiple simultaneous generations
        if (_isGeneratingResponseState.value) {
            Log.d("ChatViewModel", "Generation already in progress, ignoring new request")
            return
        }
        
        viewModelScope.launch {
            val action = actionMatcher.findBestAction(query)
            if (action != null) {
                if (!_actionsEnabled.value) {
                    // Actions are disabled, show message in chat
                    chatHistoryDB.saveUserMessage(query)
                    chatHistoryDB.saveMessage(query, "[Actions are currently disabled. Enable actions to use this feature.]", "Action: Blocked")
                    loadChatHistory()
                    _questionState.value = query
                    _responseState.value = "[Actions are currently disabled. Enable actions to use this feature.]"
                    return@launch
                }
                chatHistoryDB.saveUserMessage(query)
                chatHistoryDB.saveMessage(query, actionMatcher.executeAction(action, query), "Action: ${action.javaClass.simpleName}")
                loadChatHistory()
                _questionState.value = query
                _responseState.value = actionMatcher.executeAction(action, query)
                return@launch
            }

            // Immediately add user message to chat history
            chatHistoryDB.saveUserMessage(query)
            loadChatHistory()
            _questionState.value = query
            _responseState.value = ""
            _currentContextState.value = "" // Clear previous context
            _isGeneratingResponseState.value = true
            
            // Add a pending assistant message to chat history
            var assistantMessageId: Long? = null
            viewModelScope.launch {
                val pendingMessage = ChatMessage(
                    question = query,
                    response = "",
                    timestamp = System.currentTimeMillis(),
                    isUserMessage = false,
                    contextUsed = ""
                )
                assistantMessageId = chatHistoryDB.saveStreamingAssistantMessage(pendingMessage)
                Log.d("ChatViewModel", "Created assistant message with ID: $assistantMessageId")
                loadChatHistory()
            }

            try {
                var jointContext = ""
                val retrievedContextList = ArrayList<RetrievedContext>()
                
                // Build context with proper error handling
                try {

                if (_isSmsContextEnabled.value) {
                    try {
                        if (smsReader.hasPermission()) {
                            // Reduced count from default 10 to 3 to reduce context length
                            val smsMessages = smsReader.readLastSmsMessages(count = 3)
                            if (smsMessages.isNotEmpty()) {
                                jointContext += "📱 RECENT SMS MESSAGES:\n"
                                smsMessages.forEachIndexed { index, sms ->
                                    jointContext += "${index + 1}. From: ${sms.sender}\n"
                                    jointContext += "   📅 Date: ${sms.formattedDate} at ${sms.formattedTime}\n"
                                    jointContext += "   💬 Message: ${sms.body}\n"
                                    jointContext += "\n"
                                    retrievedContextList.add(
                                        RetrievedContext(fileName = sms.sender, context = "SMS from ${sms.sender}: ${sms.body}")
                                    )
                                }
                            }
                        } else {
                            Log.w("ChatViewModel", "SMS permission not granted")
                        }
                    } catch (e: Exception) {
                        Log.e("ChatViewModel", "Error reading SMS messages: ${e.message}", e)
                        // Continue without SMS if there's an error
                    }
                }
                if (_isCallLogContextEnabled.value) {
                    try {
                        if (callLogsReader.hasPermission()) {
                            // Reduced count from default 10 to 3 to reduce context length
                            val callLogs = callLogsReader.readLastCallLogs(count = 3)
                            if (callLogs.isNotEmpty()) {
                                jointContext += "📞 RECENT CALL LOGS:\n"
                                callLogs.forEachIndexed { index, call ->
                                    val callName = call.name ?: "Unknown"
                                    jointContext += "${index + 1}. Contact: $callName (${call.number})\n"
                                    jointContext += "   📅 Date: ${call.formattedDate} at ${call.formattedTime}\n"
                                    jointContext += "   📞 Type: ${call.type}\n"
                                    jointContext += "   ⏱️ Duration: ${call.formattedDuration}\n"
                                    jointContext += "\n"
                                    retrievedContextList.add(
                                        RetrievedContext(fileName = "Call Log", context = "Call with $callName: ${call.type} call, ${call.formattedDuration}")
                                    )
                                }
                            }
                        } else {
                            Log.w("ChatViewModel", "Call log permission not granted")
                        }
                    } catch (e: Exception) {
                        Log.e("ChatViewModel", "Error reading call logs: ${e.message}", e)
                        // Continue without call logs if there's an error
                    }
                }

                if (_isDocumentContextEnabled.value) {
                    Log.d("AppPerformance", "Starting: Encode query and retrieve context")
                    val queryStartTime = System.currentTimeMillis()
                    val queryEmbedding = sentenceEncoder.encodeText(query)
                    // Reduced from 5 to 3 chunks to reduce context length
                    val similarChunks = chunksDB.getSimilarChunks(queryEmbedding, n = 3)
                    if (similarChunks.isNotEmpty()) {
                        jointContext += "📄 RELEVANT DOCUMENT CONTENT:\n"
                        similarChunks.forEachIndexed { index, chunk ->
                            jointContext += "${index + 1}. From: ${chunk.second.docFileName}\n"
                            jointContext += "   📝 Content: ${chunk.second.chunkData}\n"
                            jointContext += "\n"
                            retrievedContextList.add(
                                RetrievedContext(chunk.second.docFileName, chunk.second.chunkData)
                            )
                        }
                    }
                    val queryEndTime = System.currentTimeMillis()
                    Log.d(
                        "AppPerformance",
                        "Finished: Encode query and retrieve context. Time taken: ${queryEndTime - queryStartTime}ms"
                    )
                }
                
                } catch (e: Exception) {
                    Log.e("ChatViewModel", "Error building context: ${e.message}", e)
                    // Continue with empty context if there's an error
                    jointContext = "No additional context available due to an error."
                }
                
                // If no context was built, provide a helpful message
                if (jointContext.isEmpty()) {
                    jointContext = ""
                }
                
                _retrievedContextListState.value = retrievedContextList
                
                // Store the original context for display
                _currentContextState.value = jointContext
                
                // Truncate context if it's too long to prevent token limit issues
                val truncatedContext = truncateContext(jointContext, maxLength = 1500)
                Log.d("ChatViewModel", "Original context length: ${jointContext.length}, Truncated context length: ${truncatedContext.length}")
                
                // Enhance the prompt based on query type
                val enhancedPrompt = enhancePromptForQuery(prompt, query, truncatedContext)
                val inputPrompt = enhancedPrompt.replace("\$CONTEXT", truncatedContext).replace("\$QUERY", query)
                Log.d("ChatViewModel", "Final input prompt length: ${inputPrompt.length}")
                
                // Additional safety check for input length
                if (inputPrompt.length > 3000) {
                    Log.w("ChatViewModel", "Input prompt is very long (${inputPrompt.length} chars), this may cause token limit issues")
                }
                
                val inferenceStartTime = System.currentTimeMillis()
                try {
                    val llm =
                        (AppLLMProvider.initializationState.value as? LLMInitializationState.Initialized)
                            ?.llmProvider
                            ?: throw IllegalStateException("LLM not initialized")

                    currentLLMProvider = llm

                    var isFirstToken = true
                    var streamingResponse = ""
                    llm.generateResponse(inputPrompt)
                        .catch {
                            _isGeneratingResponseState.value = false
                            _questionState.value = ""
                            currentLLMProvider = null
                            throw it
                        }
                        .onCompletion {
                            _isGeneratingResponseState.value = false
                            currentLLMProvider = null
                            // Finalize the assistant message in chat history (do not create a new one)
                            if (assistantMessageId != null && streamingResponse.isNotEmpty()) {
                                val contextUsed = buildContextUsedString()
                                chatHistoryDB.updateAssistantMessage(assistantMessageId!!, streamingResponse, contextUsed, jointContext)
                                loadChatHistory()
                            }
                        }
                        .collect { response ->
                            if (isFirstToken) {
                                val firstTokenTime = System.currentTimeMillis()
                                Log.d(
                                    "AppPerformance",
                                    "Time to first token: ${firstTokenTime - inferenceStartTime}ms"
                                )
                                isFirstToken = false
                            }
                            streamingResponse += response
                            _responseState.value = streamingResponse
                            // Update the assistant message in chat history
                            if (assistantMessageId != null) {
                                Log.d("ChatViewModel", "Updating assistant message $assistantMessageId with response length: ${streamingResponse.length}")
                                chatHistoryDB.updateAssistantMessage(assistantMessageId!!, streamingResponse, "", jointContext)
                                loadChatHistory()
                            }
                        }
                } catch (e: Exception) {
                    _isGeneratingResponseState.value = false
                    _questionState.value = ""
                    currentLLMProvider = null
                    throw e
                }
            } catch (e: Exception) {
                _isGeneratingResponseState.value = false
                _questionState.value = ""
                currentLLMProvider = null
                throw e
            }
        }
    }

    fun checkNumDocuments(): Boolean = documentsDB.getDocsCount() > 0

    fun checkValidAPIKey(): Boolean = geminiAPIKey.getAPIKey() != null

    private fun loadChatHistory() {
        viewModelScope.launch {
            try {
                val messages = chatHistoryDB.getRecentMessages(100)
                Log.d("ChatViewModel", "Loaded ${messages.size} messages from chat history")
                _chatHistoryState.value = messages
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error loading chat history: ${e.message}", e)
            }
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            try {
                chatHistoryDB.clearAllMessages()
                _chatHistoryState.value = emptyList()
                _questionState.value = ""
                _responseState.value = ""
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error clearing chat history: ${e.message}", e)
            }
        }
    }

    fun deleteMessage(messageId: Long) {
        viewModelScope.launch {
            try {
                chatHistoryDB.deleteMessage(messageId)
                loadChatHistory() // Reload the history
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error deleting message: ${e.message}", e)
            }
        }
    }

    fun searchChatHistory(query: String) {
        viewModelScope.launch {
            try {
                val results = chatHistoryDB.searchMessages(query)
                _chatHistoryState.value = results
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error searching chat history: ${e.message}", e)
            }
        }
    }

    fun resetChatHistory() {
        loadChatHistory()
    }
    
    fun resetDatabase() {
        viewModelScope.launch {
            try {
                Log.d("ChatViewModel", "Resetting database...")
                chatHistoryDB.clearAllMessages()
                // Create test messages
                chatHistoryDB.saveUserMessage("Test user message")
                chatHistoryDB.saveMessage("Test user message", "Test assistant response", "SMS", "Test detailed context")
                loadChatHistory()
                Log.d("ChatViewModel", "Database reset complete")
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error resetting database: ${e.message}", e)
            }
        }
    }

    private fun buildContextUsedString(): String {
        val contexts = mutableListOf<String>()
        if (_isSmsContextEnabled.value) contexts.add("SMS")
        if (_isCallLogContextEnabled.value) contexts.add("Call Logs")
        if (_isDocumentContextEnabled.value) contexts.add("Documents")
        return if (contexts.isNotEmpty()) contexts.joinToString(", ") else "No context"
    }

    fun isLocalModelAvailable(): Boolean = llmFactory.isLocalModelAvailable()

    fun isRemoteModelAvailable(): Boolean = llmFactory.isRemoteModelAvailable()

    // Debug function to test chat history
    fun debugChatHistory() {
        viewModelScope.launch {
            try {
                val count = chatHistoryDB.getMessageCount()
                Log.d("ChatViewModel", "Total messages in DB: $count")
                
                val allMessages = chatHistoryDB.getAllMessages()
                Log.d("ChatViewModel", "All messages: ${allMessages.size}")
                allMessages.forEach { message ->
                    Log.d("ChatViewModel", "Message ${message.messageId}: ${if (message.isUserMessage) "USER" else "ASSISTANT"} - ${message.question}")
                }
                
                // If no messages, create a test message
                if (count == 0L) {
                    Log.d("ChatViewModel", "No messages found, creating test message")
                    chatHistoryDB.saveUserMessage("Test message")
                    chatHistoryDB.saveMessage("Test message", "Test response", "Test context", "Test detailed context")
                    loadChatHistory()
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error in debug: ${e.message}", e)
            }
        }
    }

    /**
     * Truncates the context to prevent token limit issues
     * @param context The context string to truncate
     * @param maxLength Maximum length in characters
     * @return Truncated context string
     */
    private fun truncateContext(context: String, maxLength: Int): String {
        return if (context.length <= maxLength) {
            context
        } else {
            val truncated = context.substring(0, maxLength)
            // Try to truncate at a word boundary to avoid cutting words
            val lastSpaceIndex = truncated.lastIndexOf(' ')
            if (lastSpaceIndex > maxLength * 0.8) { // If we can find a space in the last 20%
                truncated.substring(0, lastSpaceIndex) + "..."
            } else {
                truncated + "..."
            }
        }
    }

    /**
     * Enhances the prompt based on the query type and available context
     * @param basePrompt The base prompt template
     * @param query The user's query
     * @param context The available context
     * @return Enhanced prompt string
     */
    private fun enhancePromptForQuery(basePrompt: String, query: String, context: String): String {
        val queryLower = query.lowercase()
        
        // Add specific instructions based on query type
        val additionalInstructions = when {
            queryLower.contains("recent") || queryLower.contains("latest") -> {
                "\n**Special Instructions:** Focus on the most recent information in the context. Highlight timestamps and chronological order."
            }
            queryLower.contains("bank") || queryLower.contains("transaction") || queryLower.contains("debit") || queryLower.contains("credit") -> {
                "\n**Special Instructions:** Pay special attention to financial messages, transaction details, and banking information. Extract amounts, account numbers, and transaction types carefully."
            }
            queryLower.contains("call") || queryLower.contains("phone") -> {
                "\n**Special Instructions:** Focus on call patterns, durations, and frequency. Identify missed calls, important contacts, and call history trends."
            }
            queryLower.contains("message") || queryLower.contains("sms") || queryLower.contains("text") -> {
                "\n**Special Instructions:** Analyze message content, sender patterns, and communication frequency. Look for important information, appointments, or urgent messages."
            }
            queryLower.contains("summary") || queryLower.contains("overview") -> {
                "\n**Special Instructions:** Provide a comprehensive summary of all available information. Organize by category (SMS, calls, documents) and highlight key points."
            }
            queryLower.contains("who") || queryLower.contains("contact") -> {
                "\n**Special Instructions:** Focus on identifying people, contacts, and relationships. Extract names, phone numbers, and communication patterns."
            }
            queryLower.contains("when") || queryLower.contains("time") || queryLower.contains("date") -> {
                "\n**Special Instructions:** Pay attention to dates, times, and scheduling information. Organize information chronologically."
            }
            queryLower.contains("how much") || queryLower.contains("amount") || queryLower.contains("cost") -> {
                "\n**Special Instructions:** Extract and highlight any monetary amounts, costs, or financial figures mentioned in the context."
            }
            else -> {
                "\n**Special Instructions:** Provide a helpful and informative response based on the available context. Be specific and reference the source information."
            }
        }
        
        return basePrompt + additionalInstructions
    }

    override fun onCleared() {
        super.onCleared()
        currentLLMProvider?.stopGeneration()
        currentLLMProvider = null
    }
}
