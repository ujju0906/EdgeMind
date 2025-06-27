package com.ml.shubham0204.docqa.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ml.shubham0204.docqa.data.ChunksDB
import com.ml.shubham0204.docqa.data.DocumentsDB
import com.ml.shubham0204.docqa.data.GeminiAPIKey
import com.ml.shubham0204.docqa.data.RetrievedContext
import com.ml.shubham0204.docqa.domain.embeddings.SentenceEmbeddingProvider
import com.ml.shubham0204.docqa.domain.llm.AppLLMProvider
import com.ml.shubham0204.docqa.domain.llm.LLMFactory
import com.ml.shubham0204.docqa.domain.llm.LLMProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import android.util.Log

class ChatViewModel(
    private val documentsDB: DocumentsDB,
    private val chunksDB: ChunksDB,
    private val geminiAPIKey: GeminiAPIKey,
    private val sentenceEncoder: SentenceEmbeddingProvider,
    private val llmFactory: LLMFactory,
) : ViewModel() {

    private val _questionState = MutableStateFlow("")
    val questionState: StateFlow<String> = _questionState

    private val _responseState = MutableStateFlow("")
    val responseState: StateFlow<String> = _responseState

    private val _isGeneratingResponseState = MutableStateFlow(false)
    val isGeneratingResponseState: StateFlow<Boolean> = _isGeneratingResponseState

    val isModelInitialized = AppLLMProvider.isInitialized

    private val _retrievedContextListState = MutableStateFlow(emptyList<RetrievedContext>())
    val retrievedContextListState: StateFlow<List<RetrievedContext>> = _retrievedContextListState

    fun getAnswer(
        query: String,
        prompt: String,
    ) {
        _questionState.value = query
        _responseState.value = ""
        _isGeneratingResponseState.value = true
        try {
            var jointContext = ""
            val retrievedContextList = ArrayList<RetrievedContext>()
            Log.d("AppPerformance", "Starting: Encode query and retrieve context")
            val queryStartTime = System.currentTimeMillis()
            val queryEmbedding = sentenceEncoder.encodeText(query)
            chunksDB.getSimilarChunks(queryEmbedding, n = 5).forEach {
                jointContext += " " + it.second.chunkData
                retrievedContextList.add(RetrievedContext(it.second.docFileName, it.second.chunkData))
            }
            val queryEndTime = System.currentTimeMillis()
            Log.d("AppPerformance", "Finished: Encode query and retrieve context. Time taken: ${queryEndTime - queryStartTime}ms")
            _retrievedContextListState.value = retrievedContextList
            val inputPrompt = prompt.replace("\$CONTEXT", jointContext).replace("\$QUERY", query)
            val inferenceStartTime = System.currentTimeMillis()
            viewModelScope.launch {
                try {
                    val llm = AppLLMProvider.getInstance()

                    var isFirstToken = true
                    llm.generateResponse(inputPrompt)
                        .catch {
                            _isGeneratingResponseState.value = false
                            _questionState.value = ""
                            throw it
                        }
                        .onCompletion {
                            _isGeneratingResponseState.value = false
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
                            _responseState.value += response
                        }
                } catch (e: Exception) {
                    _isGeneratingResponseState.value = false
                    _questionState.value = ""
                    throw e
                }
            }
        } catch (e: Exception) {
            _isGeneratingResponseState.value = false
            _questionState.value = ""
            throw e
        }
    }

    fun checkNumDocuments(): Boolean = documentsDB.getDocsCount() > 0

    fun checkValidAPIKey(): Boolean = geminiAPIKey.getAPIKey() != null

    fun isLocalModelAvailable(): Boolean = llmFactory.isLocalModelAvailable()

    fun isRemoteModelAvailable(): Boolean = llmFactory.isRemoteModelAvailable()

    override fun onCleared() {
        super.onCleared()
    }
}
