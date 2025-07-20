package com.ml.shubham0204.docqa.domain.llm

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.ml.shubham0204.docqa.domain.llm.LocalLLMAPI
import com.ml.shubham0204.docqa.domain.llm.GeminiRemoteAPI

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
        // If already initializing, don't start another initialization
        if (_initializationState.value == LLMInitializationState.Initializing) {
            Log.d("AppLLMProvider", "Initialization already in progress, skipping...")
            return
        }
        
        // If already initialized with the same type, don't reinitialize
        val currentState = _initializationState.value
        if (currentState is LLMInitializationState.Initialized) {
            val currentType = if (factory.isLocalModelAvailable()) LLMFactory.LLMType.LOCAL else LLMFactory.LLMType.REMOTE
            val currentProvider = currentState.llmProvider
            if ((currentType == LLMFactory.LLMType.LOCAL && currentProvider is LocalLLMAPI) ||
                (currentType == LLMFactory.LLMType.REMOTE && currentProvider is GeminiRemoteAPI)) {
                Log.d("AppLLMProvider", "Already initialized with correct type, skipping...")
                return
            }
        }
        
        Log.d("AppLifecycle", "AppLLMProvider: Initialization started.")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Close existing provider if any
                llmProvider?.close()
                llmProvider = null
                _isInitialized.value = false
                
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
                Log.d("AppLifecycle", "AppLLMProvider: Initialization complete with type: $llmType")
            } catch (e: Exception) {
                Log.e("AppLLMProvider", "Failed to initialize LLM Provider", e)
                _initializationState.value = LLMInitializationState.Error(e)
                _isInitialized.value = false
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
        _initializationState.value = LLMInitializationState.NotInitialized
        Log.d("AppLifecycle", "AppLLMProvider: Closed.")
    }
    
    fun forceReinitialize(factory: LLMFactory) {
        Log.d("AppLLMProvider", "Force reinitializing LLM...")
        close()
        initialize(factory)
    }
} 