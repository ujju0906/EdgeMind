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
    private var currentModelId: String? = null

    private val _initializationState =
        MutableStateFlow<LLMInitializationState>(LLMInitializationState.NotInitialized)
    val initializationState = _initializationState.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized = _isInitialized.asStateFlow()

    fun initialize(factory: LLMFactory, modelId: String? = null) {
        Log.d("AppLLMProvider", "=== INITIALIZATION REQUESTED ===")
        Log.d("AppLLMProvider", "Requested model: $modelId")
        Log.d("AppLLMProvider", "Current state: ${_initializationState.value}")
        
        // If already initializing, don't start another initialization
        if (_initializationState.value == LLMInitializationState.Initializing) {
            Log.d("AppLLMProvider", "Initialization already in progress, skipping...")
            return
        }
        
        // If already initialized with the same type and model, don't reinitialize
        val currentState = _initializationState.value
        if (currentState is LLMInitializationState.Initialized) {
            val currentProvider = currentState.llmProvider
            if (currentProvider is LocalLLMAPI) {
                val currentLocalModelId = currentProvider.getCurrentModelId()
                Log.d("AppLLMProvider", "Current local model: $currentLocalModelId")
                if (currentLocalModelId == modelId) {
                    Log.d("AppLLMProvider", "Already initialized with correct model $modelId, skipping...")
                    return
                }
            } else if (currentProvider is GeminiRemoteAPI && modelId == null) {
                Log.d("AppLLMProvider", "Already initialized with remote LLM, skipping...")
                return
            }
        }
        
        Log.d("AppLLMProvider", "Proceeding with initialization for model: $modelId")
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
                Log.d("AppLLMProvider", "Creating LLM provider with type: $llmType, model: $modelId")
                val provider = factory.create(llmType, modelId)
                Log.d("AppLLMProvider", "Provider created, initializing...")
                provider.init()
                llmProvider = provider
                currentModelId = modelId
                _isInitialized.value = true
                _initializationState.value = LLMInitializationState.Initialized(provider)
                Log.d("AppLLMProvider", "=== INITIALIZATION COMPLETED ===")
                Log.d("AppLLMProvider", "LLM type: $llmType")
                Log.d("AppLLMProvider", "Model ID: $modelId")
                Log.d("AppLLMProvider", "Provider: ${provider.javaClass.simpleName}")
            } catch (e: Exception) {
                Log.e("AppLLMProvider", "Failed to initialize LLM Provider", e)
                _initializationState.value = LLMInitializationState.Error(e)
                _isInitialized.value = false
                currentModelId = null
            }
        }
    }

    suspend fun switchModel(factory: LLMFactory, modelId: String) {
        Log.d("AppLLMProvider", "=== MODEL SWITCH REQUESTED ===")
        Log.d("AppLLMProvider", "Current model: ${getCurrentModelId()}")
        Log.d("AppLLMProvider", "Target model: $modelId")
        
        // Check if model is downloaded
        if (!factory.isModelDownloaded(modelId)) {
            Log.e("AppLLMProvider", "Model $modelId is not downloaded!")
            throw IllegalStateException("Model $modelId is not downloaded")
        }
        
        Log.d("AppLLMProvider", "Model $modelId is available, proceeding with switch...")
        
        // Initialize with the new model
        initialize(factory, modelId)
        
        Log.d("AppLLMProvider", "=== MODEL SWITCH COMPLETED ===")
        Log.d("AppLLMProvider", "Current model after switch: ${getCurrentModelId()}")
    }

    fun getInstance(): LLMProvider {
        return llmProvider ?: throw IllegalStateException("LLMProvider not initialized. Call initialize() first.")
    }

    fun getCurrentModelId(): String? = currentModelId

    fun getCurrentModelInfo(): ModelInfo? {
        val provider = llmProvider
        return if (provider is LocalLLMAPI) {
            provider.getCurrentModelInfo()
        } else {
            null
        }
    }
    
    fun getCurrentModelName(): String {
        val provider = llmProvider
        return if (provider is LocalLLMAPI) {
            provider.getCurrentModelName()
        } else {
            "Remote (Gemini)"
        }
    }

    fun close() {
        llmProvider?.close()
        llmProvider = null
        _isInitialized.value = false
        _initializationState.value = LLMInitializationState.NotInitialized
        currentModelId = null
        Log.d("AppLifecycle", "AppLLMProvider: Closed.")
    }
    
    fun forceReinitialize(factory: LLMFactory, modelId: String? = null) {
        Log.d("AppLLMProvider", "Force reinitializing LLM with model $modelId...")
        close()
        initialize(factory, modelId)
    }
} 