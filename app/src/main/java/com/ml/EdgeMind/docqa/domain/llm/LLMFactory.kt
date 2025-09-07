package com.ml.EdgeMind.docqa.domain.llm

import android.content.Context
import com.ml.EdgeMind.docqa.data.GeminiAPIKey
import com.ml.EdgeMind.docqa.data.SettingsRepository

class LLMFactory(
    private val context: Context,
    private val geminiAPIKey: GeminiAPIKey,
    private val modelManager: ModelManager,
    private val settingsRepository: SettingsRepository
) {
    
    enum class LLMType {
        LOCAL,
        REMOTE
    }
    
    fun create(type: LLMType, modelId: String? = null): LLMProvider {
        return when (type) {
            LLMType.LOCAL -> {
                if (!modelManager.isModelDownloaded()) {
                    throw IllegalStateException("No models are available locally")
                }
                LocalLLMAPI(context, modelManager, modelId, settingsRepository)
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
    
    fun getAvailableLocalModels(): List<ModelInfo> {
        return modelManager.getAvailableModels()
    }
    
    fun getDownloadedModels(): List<ModelInfo> {
        return modelManager.getDownloadedModels()
    }
    
    fun isModelDownloaded(modelId: String): Boolean {
        return modelManager.isModelDownloaded(modelId)
    }
    
    fun getModelInfo(modelId: String): ModelInfo? {
        return modelManager.getModelInfo(modelId)
    }
} 