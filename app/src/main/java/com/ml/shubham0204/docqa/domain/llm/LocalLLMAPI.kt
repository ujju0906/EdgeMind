package com.ml.shubham0204.docqa.domain.llm

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class LocalLLMAPI(
    private val context: Context,
    private val modelManager: ModelManager
) : LLMProvider {
    private var llmInference: LlmInference? = null

    override suspend fun init() {
        if (llmInference != null) {
            return
        }
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
                    .setMaxTokens(1024)
                    .build()

            // Create an instance of the LLM Inference task
            llmInference = LlmInference.createFromOptions(context, taskOptions)
            val endTime = System.currentTimeMillis()
            Log.d("AppLifecycle", "LocalLLMAPI: Model loading finished in ${endTime - startTime}ms")
        } catch (e: Exception) {
            Log.e("LocalLLMAPI", "Failed to initialize LLM: ${e.message}", e)
            throw e
        }
    }

    override fun generateResponse(prompt: String): Flow<String> =
        callbackFlow {
            try {
                if (llmInference == null) {
                    throw IllegalStateException("LLM not initialized. Call init() first.")
                }
                Log.d("AppPerformance", "Starting: Local LLM Inference")
                Log.d("LocalLLMAPI", "Generating response for prompt: $prompt")

                // Generate response using the local model
                llmInference!!.generateResponseAsync(
                    prompt,
                ) { partialResult: String?, done: Boolean ->
                    partialResult?.let { trySend(it) }
                    if (done) {
                        close()
                    }
                }
            } catch (e: Exception) {
                Log.e("LocalLLMAPI", "Error generating response: ${e.message}", e)
                close(e)
            }

            awaitClose {}
        }

    override fun close() {
        try {
            llmInference?.close()
            llmInference = null
            Log.d("LocalLLMAPI", "LLM closed successfully")
        } catch (e: Exception) {
            Log.e("LocalLLMAPI", "Error closing LLM: ${e.message}", e)
        }
    }
} 