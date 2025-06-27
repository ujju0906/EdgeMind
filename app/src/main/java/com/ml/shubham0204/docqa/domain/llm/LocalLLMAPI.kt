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

    private fun ensureLLMInitialized() {
        if (llmInference != null) {
            return
        }
        try {
            val modelPath = modelManager.getModelPath()
            if (!modelManager.isModelDownloaded()) {
                throw IllegalStateException("Model is not downloaded, cannot initialize LocalLLMAPI")
            }
            // Set the configuration options for the LLM Inference task
            val taskOptions = LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(1024)
                .setMaxTopK(64)
                .build()

            // Create an instance of the LLM Inference task
            llmInference = LlmInference.createFromOptions(context, taskOptions)
            Log.d("LocalLLMAPI", "LLM initialized successfully with model: $modelPath")
        } catch (e: Exception) {
            Log.e("LocalLLMAPI", "Failed to initialize LLM: ${e.message}", e)
            throw e
        }
    }

    override fun generateResponse(prompt: String): Flow<String> =
        callbackFlow {
            try {
                ensureLLMInitialized()
                Log.d("LocalLLMAPI", "Generating response for prompt: $prompt")

                val llm = llmInference ?: throw IllegalStateException("LLM not initialized")

                // Generate response using the local model
                llm.generateResponseAsync(
                    prompt,
                    { partialResult, done ->
                        trySend(partialResult)
                        if (done) {
                            channel.close()
                        }
                    },
                )
            } catch (e: Exception) {
                Log.e("LocalLLMAPI", "Error generating response: ${e.message}", e)
                channel.close(e)
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