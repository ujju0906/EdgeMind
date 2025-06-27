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
    
    private var currentLLM: LLMProvider? = null
    private var currentType: LLMType? = null
    
    suspend fun getLLM(type: LLMType = LLMType.LOCAL): LLMProvider {
        // If we already have the requested type, return it
        if (currentType == type && currentLLM != null) {
            return currentLLM!!
        }
        
        // Close existing LLM if different type
        currentLLM?.close()
        
        currentLLM = when (type) {
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
        
        currentType = type
        return currentLLM!!
    }
    
    fun isLocalModelAvailable(): Boolean {
        return modelManager.isModelDownloaded()
    }
    
    fun isRemoteModelAvailable(): Boolean {
        return geminiAPIKey.getAPIKey() != null
    }
    
    fun close() {
        currentLLM?.close()
        currentLLM = null
        currentType = null
    }
} 