package com.ml.shubham0204.docqa.domain.llm

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LLMInitializationState {
    object NotInitialized : LLMInitializationState()
    object Initializing : LLMInitializationState()
    data class Initialized(val llmProvider: LLMProvider) : LLMInitializationState()
    data class Error(val exception: Exception) : LLMInitializationState()
}

object AppLLMProvider {

    private var llmProvider: LLMProvider? = null

    private val _initializationState =
        MutableStateFlow<LLMInitializationState>(LLMInitializationState.NotInitialized)
    val initializationState = _initializationState.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized = _isInitialized.asStateFlow()

    fun initialize(factory: LLMFactory) {
        if (llmProvider != null || _initializationState.value == LLMInitializationState.Initializing) return

        // If no model is available (neither local nor remote), don't attempt to initialize.
        // The UI will handle this state and prompt the user to download a model or add an API key.
        if (!factory.isLocalModelAvailable() && !factory.isRemoteModelAvailable()) {
            return
        }

        Log.d("AppLifecycle", "AppLLMProvider: Initialization started.")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                _initializationState.value = LLMInitializationState.Initializing
                val llmType =
                    if (factory.isLocalModelAvailable()) {
                        LLMFactory.LLMType.LOCAL
                    } else {
                        LLMFactory.LLMType.REMOTE
                    }
                val provider = factory.create(llmType)
                provider.init()
                llmProvider = provider
                _isInitialized.value = true
                _initializationState.value = LLMInitializationState.Initialized(provider)
                Log.d("AppLifecycle", "AppLLMProvider: Initialization complete.")
            } catch (e: Exception) {
                Log.e("AppLLMProvider", "Failed to initialize LLM Provider", e)
                _initializationState.value = LLMInitializationState.Error(e)
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