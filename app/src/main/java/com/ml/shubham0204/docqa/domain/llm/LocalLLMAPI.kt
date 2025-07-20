package com.ml.shubham0204.docqa.domain.llm

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalLLMAPI(
    private val context: Context,
    private val modelManager: ModelManager
) : LLMProvider {
    private var llmInference: LlmInference? = null
    private var isGenerating = false
    private var isCancelled = false

    override suspend fun init() {
        if (llmInference != null) {
            return
        }
        
        return withContext(Dispatchers.IO) {
            try {
                Log.d("AppLifecycle", "LocalLLMAPI: Model loading started.")
                val startTime = System.currentTimeMillis()
                val modelPath = modelManager.getModelPath()
                if (!modelManager.isModelDownloaded()) {
                    throw IllegalStateException("Model is not downloaded, cannot initialize LocalLLMAPI")
                }
                
                // Set the configuration options for the LLM Inference task
                val taskOptions =
                    LlmInferenceOptions.builder()
                        .setModelPath(modelPath)
                        .setMaxTokens(2048) // Increased from 1024 to handle longer inputs
                        .build()

                // Create an instance of the LLM Inference task
                llmInference = LlmInference.createFromOptions(context, taskOptions)
                val endTime = System.currentTimeMillis()
                Log.d("AppLifecycle", "LocalLLMAPI: Model loading finished in ${endTime - startTime}ms")
            } catch (e: Exception) {
                Log.e("LocalLLMAPI", "Failed to initialize LLM: ${e.message}", e)
                // Clean up on failure
                llmInference?.close()
                llmInference = null
                throw e
            }
        }
    }

    override fun generateResponse(prompt: String): Flow<String> =
        callbackFlow {
            try {
                if (llmInference == null) {
                    throw IllegalStateException("LLM not initialized. Call init() first.")
                }
                
                // Reset cancellation state for new generation
                isCancelled = false
                isGenerating = true
                
                Log.d("AppPerformance", "Starting: Local LLM Inference")
                Log.d("LocalLLMAPI", "Generating response for prompt length: ${prompt.length}")

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
                    Log.d("LocalLLMAPI", "LLM inference closed successfully")
                } catch (e: Exception) {
                    Log.e("LocalLLMAPI", "Error closing LLM inference: ${e.message}", e)
                }
            }
            llmInference = null
            Log.d("LocalLLMAPI", "LLM closed successfully")
        } catch (e: Exception) {
            Log.e("LocalLLMAPI", "Error closing LLM: ${e.message}", e)
        }
    }
    
    // Add method to check if LLM is in a valid state
    fun isInitialized(): Boolean {
        return llmInference != null
    }
    
    // Add method to get memory usage info
    fun getMemoryInfo(): String {
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        val maxMemory = runtime.maxMemory() / 1024 / 1024
        return "Used: ${usedMemory}MB, Max: ${maxMemory}MB"
    }
} 