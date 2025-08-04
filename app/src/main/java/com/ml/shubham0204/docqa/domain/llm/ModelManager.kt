package com.ml.shubham0204.docqa.domain.llm

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

data class ModelInfo(
    val id: String,
    val name: String,
    val description: String,
    val url: String,
    val filename: String,
    val sizeInGB: Float,
    val parameters: String,
    val quantization: String,
    val isDownloaded: Boolean = false,
    val fileSize: Long = 0L
)

class ModelManager(private val context: Context) {
    
    companion object {
        private const val MODEL_DIR_NAME = "llm"
        private const val TAG = "ModelManager"
        
        // Available models
        val AVAILABLE_MODELS = listOf(
            ModelInfo(
                id = "qwen2.5-1.5b",
                name = "Qwen2.5-1.5B-Instruct",
                description = "Fast and efficient 1.5B parameter model",
                url = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/main/Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.task?download=true",
                filename = "Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.task",
                sizeInGB = 1.8f,
                parameters = "1.5B",
                quantization = "8-bit"
            ),
            ModelInfo(
                id = "phi-4-mini",
                name = "Phi-4-Mini-Instruct",
                description = "Microsoft's efficient Phi-4 mini model",
                url = "https://huggingface.co/litert-community/Phi-4-mini-instruct/resolve/main/Phi-4-mini-instruct_multi-prefill-seq_q8_ekv1280.task?download=true",
                filename = "Phi-4-mini-instruct_multi-prefill-seq_q8_ekv1280.task",
                sizeInGB = 3.7f,
                parameters = "1.3B",
                quantization = "8-bit"
            ),

            ModelInfo(
                id = "qwen2.5-3b",
                name = "Qwen2.5-3B-Instruct",
                description = "Larger 3B parameter model for better quality",
                url = "https://huggingface.co/litert-community/Qwen2.5-3B-Instruct/resolve/main/Qwen2.5-3B-Instruct_multi-prefill-seq_q8_ekv1280.task?download=true",
                filename = "Qwen2.5-3B-Instruct_multi-prefill-seq_q8_ekv1280.task",
                sizeInGB = 3f,
                parameters = "3B",
                quantization = "8-bit"
            )
        )
    }

    private fun getModelDir(): File {
        return File(context.filesDir, MODEL_DIR_NAME)
    }

    fun getAvailableModels(): List<ModelInfo> {
        return AVAILABLE_MODELS.map { model ->
            val modelFile = File(getModelDir(), model.filename)
            model.copy(
                isDownloaded = modelFile.exists(),
                fileSize = if (modelFile.exists()) modelFile.length() else 0L
            )
        }
    }

    fun getModelInfo(modelId: String): ModelInfo? {
        return AVAILABLE_MODELS.find { it.id == modelId }?.let { model ->
            val modelFile = File(getModelDir(), model.filename)
            model.copy(
                isDownloaded = modelFile.exists(),
                fileSize = if (modelFile.exists()) modelFile.length() else 0L
            )
        }
    }

    fun isModelDownloaded(modelId: String): Boolean {
        val modelInfo = AVAILABLE_MODELS.find { it.id == modelId } ?: return false
        val modelFile = File(getModelDir(), modelInfo.filename)
        return modelFile.exists()
    }

    fun getModelPath(modelId: String): String? {
        val modelInfo = AVAILABLE_MODELS.find { it.id == modelId } ?: return null
        val modelFile = File(getModelDir(), modelInfo.filename)
        return if (modelFile.exists()) modelFile.absolutePath else null
    }

    fun getModelSize(modelId: String): Long {
        val modelInfo = AVAILABLE_MODELS.find { it.id == modelId } ?: return 0L
        val modelFile = File(getModelDir(), modelInfo.filename)
        return if (modelFile.exists()) modelFile.length() else 0L
    }

    fun getModelSizeInMB(modelId: String): Float {
        return getModelSize(modelId) / (1024.0f * 1024.0f)
    }

    fun getTotalDownloadedSize(): Long {
        return AVAILABLE_MODELS.sumOf { getModelSize(it.id) }
    }

    fun getTotalDownloadedSizeInMB(): Float {
        return getTotalDownloadedSize() / (1024.0f * 1024.0f)
    }

    fun getDownloadedModels(): List<ModelInfo> {
        return getAvailableModels().filter { it.isDownloaded }
    }

    suspend fun deleteModel(modelId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val modelInfo = AVAILABLE_MODELS.find { it.id == modelId } ?: return@withContext false
                val modelDir = getModelDir()
                val modelFile = File(modelDir, modelInfo.filename)
                
                if (modelFile.exists()) {
                    val deleted = modelFile.delete()
                    if (deleted) {
                        Log.d(TAG, "Model file deleted successfully: ${modelInfo.name}")
                        
                        // Also delete the model directory if it's empty
                        if (modelDir.exists() && modelDir.listFiles()?.isEmpty() == true) {
                            modelDir.delete()
                            Log.d(TAG, "Model directory deleted")
                        }
                        
                        true
                    } else {
                        Log.e(TAG, "Failed to delete model file: ${modelInfo.name}")
                        false
                    }
                } else {
                    Log.d(TAG, "Model file does not exist: ${modelInfo.name}")
                    true // Consider it successful if file doesn't exist
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting model: ${e.message}", e)
                false
            }
        }
    }

    suspend fun deleteAllModels(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                var allDeleted = true
                for (model in AVAILABLE_MODELS) {
                    if (!deleteModel(model.id)) {
                        allDeleted = false
                    }
                }
                allDeleted
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting all models: ${e.message}", e)
                false
            }
        }
    }

    // Legacy methods for backward compatibility
    fun isModelDownloaded(): Boolean {
        // Check if any model is downloaded
        return getDownloadedModels().isNotEmpty()
    }

    fun getModelPath(): String {
        // Return the first downloaded model path, or the default model path
        val downloadedModels = getDownloadedModels()
        return if (downloadedModels.isNotEmpty()) {
            getModelPath(downloadedModels.first().id) ?: ""
        } else {
            val defaultModel = AVAILABLE_MODELS.first()
            File(getModelDir(), defaultModel.filename).absolutePath
        }
    }

    fun getModelSize(): Long {
        return getTotalDownloadedSize()
    }

    fun getModelSizeInMB(): Float {
        return getTotalDownloadedSizeInMB()
    }

    suspend fun deleteModel(): Boolean {
        // Delete all models for backward compatibility
        return deleteAllModels()
    }
} 