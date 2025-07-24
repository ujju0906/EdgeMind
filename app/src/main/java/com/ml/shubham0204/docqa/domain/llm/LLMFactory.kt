package com.ml.shubham0204.docqa.domain.llm

import android.content.Context
import com.ml.shubham0204.docqa.data.GeminiAPIKey

class LLMFactory(
    private val context: Context,
    private val geminiAPIKey: GeminiAPIKey,
    private val modelManager: ModelManager
) {
    
    enum class LLMType {
        LOCAL,
        REMOTE
    }
    
    fun create(type: LLMType): LLMProvider {
        return when (type) {
            LLMType.LOCAL -> {
                if (!modelManager.isModelDownloaded()) {
                    throw IllegalStateException("Model not available locally")
                }
                LocalLLMAPI(context, modelManager)
            }
            LLMType.REMOTE -> {
                val apiKey = geminiAPIKey.getAPIKey() 
                    ?: throw IllegalStateException("Gemini API key not available")
                GeminiRemoteAPI(apiKey)
            }
        }
    }
    
    fun isLocalModelAvailable(): Boolean {
        return modelManager.isModelDownloaded()
    }
    
    fun isRemoteModelAvailable(): Boolean {
        return geminiAPIKey.getAPIKey() != null
    }
} 