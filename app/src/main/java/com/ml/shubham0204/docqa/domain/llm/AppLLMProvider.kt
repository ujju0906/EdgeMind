package com.ml.shubham0204.docqa.domain.llm

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object AppLLMProvider {

    private var llmProvider: LLMProvider? = null

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized = _isInitialized.asStateFlow()

    fun initialize(factory: LLMFactory) {
        if (llmProvider != null) return
        Log.d("AppLifecycle", "AppLLMProvider: Initialization started.")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val llmType = if (factory.isLocalModelAvailable()) {
                    LLMFactory.LLMType.LOCAL
                } else {
                    LLMFactory.LLMType.REMOTE
                }
                val provider = factory.create(llmType)
                provider.init()
                llmProvider = provider
                _isInitialized.value = true
                Log.d("AppLifecycle", "AppLLMProvider: Initialization complete.")
            } catch (e: Exception) {
                Log.e("AppLLMProvider", "Failed to initialize LLM Provider", e)
            }
        }
    }

    fun getInstance(): LLMProvider {
        return llmProvider ?: throw IllegalStateException("LLMProvider not initialized. Call initialize() first.")
    }

    fun close() {
        llmProvider?.close()
        llmProvider = null
        _isInitialized.value = false
        Log.d("AppLifecycle", "AppLLMProvider: Closed.")
    }
} 