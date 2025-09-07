package com.ml.EdgeMind.docqa.domain.llm

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import com.ml.EdgeMind.docqa.data.SettingsRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalLLMAPI(
    private val context: Context,
    private val modelManager: ModelManager,
    private var currentModelId: String? = null,
    private val settingsRepository: SettingsRepository
) : LLMProvider {
    private var llmInference: LlmInference? = null
    private var isGenerating = false
    private var isCancelled = false

    override suspend fun init() {
        Log.d("LocalLLMAPI", "=== INIT REQUESTED ===")
        Log.d("LocalLLMAPI", "Current model ID: $currentModelId")
        Log.d("LocalLLMAPI", "LLM Inference exists: ${llmInference != null}")
        
        if (llmInference != null && currentModelId != null) {
            Log.d("LocalLLMAPI", "Already initialized with model: $currentModelId")
            return
        }
        
        return withContext(Dispatchers.IO) {
            try {
                // If no specific model is set, use the first available downloaded model
                if (currentModelId == null) {
                    Log.d("LocalLLMAPI", "No model specified, looking for available models...")
                    val downloadedModels = modelManager.getDownloadedModels()
                    if (downloadedModels.isNotEmpty()) {
                        currentModelId = downloadedModels.first().id
                        Log.d("LocalLLMAPI", "Auto-selected model: $currentModelId")
                    } else {
                        Log.e("LocalLLMAPI", "No models are downloaded!")
                        throw IllegalStateException("No models are downloaded, cannot initialize LocalLLMAPI")
                    }
                }
                
                Log.d("LocalLLMAPI", "=== MODEL LOADING STARTED ===")
                Log.d("LocalLLMAPI", "Loading model: ${currentModelId}")
                val startTime = System.currentTimeMillis()
                
                val modelPath = modelManager.getModelPath(currentModelId!!)
                Log.d("LocalLLMAPI", "Model path: $modelPath")
                
                if (modelPath == null || !modelManager.isModelDownloaded(currentModelId!!)) {
                    Log.e("LocalLLMAPI", "Model ${currentModelId} is not downloaded!")
                    throw IllegalStateException("Model ${currentModelId} is not downloaded, cannot initialize LocalLLMAPI")
                }
                
                // Set the configuration options for the LLM Inference task
                val taskOptions =
                    LlmInferenceOptions.builder()
                        .setModelPath(modelPath)
                        .setMaxTokens(settingsRepository.getMaxTokens())
                        .build()

                // Create an instance of the LLM Inference task
                Log.d("LocalLLMAPI", "Creating LlmInference instance...")
                llmInference = LlmInference.createFromOptions(context, taskOptions)
                val endTime = System.currentTimeMillis()
                Log.d("LocalLLMAPI", "=== MODEL LOADING COMPLETED ===")
                Log.d("LocalLLMAPI", "Loading time: ${endTime - startTime}ms")
                Log.d("LocalLLMAPI", "Model loaded: ${currentModelId}")
                Log.d("LocalLLMAPI", "LLM Inference instance created successfully")
            } catch (e: Exception) {
                Log.e("LocalLLMAPI", "Failed to initialize LLM: ${e.message}", e)
                // Clean up on failure
                llmInference?.close()
                llmInference = null
                currentModelId = null
                throw e
            }
        }
    }

    suspend fun loadModel(modelId: String) {
        Log.d("LocalLLMAPI", "=== LOAD MODEL REQUESTED ===")
        Log.d("LocalLLMAPI", "Current model: $currentModelId")
        Log.d("LocalLLMAPI", "Target model: $modelId")
        
        if (currentModelId == modelId && llmInference != null) {
            Log.d("LocalLLMAPI", "Model $modelId is already loaded, skipping...")
            return
        }
        
        // Close existing model if different
        if (llmInference != null && currentModelId != modelId) {
            Log.d("LocalLLMAPI", "Unloading current model $currentModelId to load $modelId")
            close()
        }
        
        Log.d("LocalLLMAPI", "Setting current model to: $modelId")
        currentModelId = modelId
        Log.d("LocalLLMAPI", "Initializing model...")
        init()
        Log.d("LocalLLMAPI", "=== LOAD MODEL COMPLETED ===")
        Log.d("LocalLLMAPI", "Current model after load: $currentModelId")
    }

    suspend fun unloadModel() {
        close()
    }

    fun getCurrentModelId(): String? = currentModelId

    fun getCurrentModelInfo(): ModelInfo? {
        return currentModelId?.let { modelManager.getModelInfo(it) }
    }

    override fun generateResponse(prompt: String): Flow<String> =
        callbackFlow {
            try {
                Log.d("LocalLLMAPI", "=== GENERATE RESPONSE REQUESTED ===")
                Log.d("LocalLLMAPI", "Current model: $currentModelId")
                Log.d("LocalLLMAPI", "LLM Inference exists: ${llmInference != null}")
                
                if (llmInference == null) {
                    Log.e("LocalLLMAPI", "LLM not initialized!")
                    throw IllegalStateException("LLM not initialized. Call init() first.")
                }
                
                // Reset cancellation state for new generation
                isCancelled = false
                isGenerating = true
                
                Log.d("LocalLLMAPI", "Starting inference with model: ${currentModelId}")
                Log.d("LocalLLMAPI", "Prompt length: ${prompt.length}")

                // Generate response using the local model with proper error handling
                try {
                    llmInference!!.generateResponseAsync(
                        prompt,
                    ) { partialResult: String?, done: Boolean ->
                        try {
                            if (isCancelled) {
                                // Generation was cancelled
                                close()
                                return@generateResponseAsync
                            }
                            
                            partialResult?.let { 
                                if (!isClosedForSend) {
                                    trySend(it)
                                }
                            }
                            if (done) {
                                isGenerating = false
                                close()
                            }
                        } catch (e: Exception) {
                            Log.e("LocalLLMAPI", "Error in callback: ${e.message}", e)
                            isGenerating = false
                            close(e)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("LocalLLMAPI", "Error calling generateResponseAsync: ${e.message}", e)
                    isGenerating = false
                    close(e)
                }
            } catch (e: Exception) {
                Log.e("LocalLLMAPI", "Error generating response: ${e.message}", e)
                isGenerating = false
                close(e)
            }

            awaitClose {
                isGenerating = false
                Log.d("LocalLLMAPI", "Flow closed")
            }
        }

    override fun stopGeneration() {
        Log.d("LocalLLMAPI", "Stopping generation")
        isCancelled = true
        isGenerating = false
        
        // Don't close the LLM instance immediately to avoid crashes
        // Let the flow handle the cleanup properly
        Log.d("LocalLLMAPI", "Generation stop requested")
    }

    override fun close() {
        try {
            isGenerating = false
            isCancelled = true
            
            // Close LLM inference with proper error handling
            llmInference?.let { inference ->
                try {
                    inference.close()
                    Log.d("LocalLLMAPI", "LLM inference closed successfully for model ${currentModelId}")
                } catch (e: Exception) {
                    Log.e("LocalLLMAPI", "Error closing LLM inference: ${e.message}", e)
                }
            }
            llmInference = null
            currentModelId = null
            Log.d("LocalLLMAPI", "LLM closed successfully")
        } catch (e: Exception) {
            Log.e("LocalLLMAPI", "Error closing LLM: ${e.message}", e)
        }
    }
    
    // Add method to check if LLM is in a valid state
    fun isInitialized(): Boolean {
        return llmInference != null && currentModelId != null
    }
    
    // Add method to get memory usage info
    fun getMemoryUsageInfo(): String {
        return "Model: ${currentModelId ?: "None"}, Initialized: ${isInitialized()}"
    }
    
    fun getCurrentModelName(): String {
        return currentModelId?.let { modelManager.getModelInfo(it)?.name } ?: "None"
    }
} 
