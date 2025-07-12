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
                            val smsMessages = smsReader.readLastSmsMessages()
                            if (smsMessages.isNotEmpty()) {
                                jointContext += "Recent SMS messages:\n"
                                smsMessages.forEach { sms ->
                                    val smsInfo = "From: ${sms.sender}, Date: ${sms.formattedDate}, Time: ${sms.formattedTime}, Message: ${sms.body}"
                                    jointContext += "- $smsInfo\n"
                                    retrievedContextList.add(
                                        RetrievedContext(fileName = sms.sender, context = smsInfo)
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
                            val callLogs = callLogsReader.readLastCallLogs()
                            if (callLogs.isNotEmpty()) {
                                jointContext += "Recent call logs:\n"
                                callLogs.forEach { call ->
                                    val callName = call.name ?: "Unknown"
                                    val callInfo =
                                        "Name: $callName, Number: ${call.number}, Type: ${call.type}, Date: ${call.formattedDate}, Time: ${call.formattedTime}, Duration: ${call.formattedDuration}"
                                    jointContext += "- $callInfo\n"
                                    retrievedContextList.add(
                                        RetrievedContext(fileName = "Call Log", context = callInfo)
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
                    chunksDB.getSimilarChunks(queryEmbedding, n = 5).forEach {
                        jointContext += " " + it.second.chunkData
                        retrievedContextList.add(
                            RetrievedContext(it.second.docFileName, it.second.chunkData)
                        )
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
                
                _retrievedContextListState.value = retrievedContextList
                val inputPrompt = prompt.replace("\$CONTEXT", jointContext).replace("\$QUERY", query)
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
                                chatHistoryDB.updateAssistantMessage(assistantMessageId!!, streamingResponse, contextUsed)
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
                                chatHistoryDB.updateAssistantMessage(assistantMessageId!!, streamingResponse, "")
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

    private fun buildContextUsedString(): String {
        val contexts = mutableListOf<String>()
        if (_isSmsContextEnabled.value) contexts.add("SMS")
        if (_isCallLogContextEnabled.value) contexts.add("Call Logs")
        if (_isDocumentContextEnabled.value) contexts.add("Documents")
        return if (contexts.isNotEmpty()) contexts.joinToString(", ") else "No context"
    }

    fun isLocalModelAvailable(): Boolean = llmFactory.isLocalModelAvailable()

    fun isRemoteModelAvailable(): Boolean = llmFactory.isRemoteModelAvailable()

    override fun onCleared() {
        super.onCleared()
        currentLLMProvider?.stopGeneration()
        currentLLMProvider = null
    }
}
