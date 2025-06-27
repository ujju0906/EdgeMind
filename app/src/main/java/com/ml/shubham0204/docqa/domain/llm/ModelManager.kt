package com.ml.shubham0204.docqa.domain.llm

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class ModelManager(private val context: Context) {
    
    companion object {
        private const val MODEL_DIR_NAME = "llm"
        private const val MODEL_FILENAME = "Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.task"
        private const val MODEL_URL = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/main/Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.task?download=true"
        private const val TAG = "ModelManager"
    }

    private fun getModelDir(): File {
        return File(context.filesDir, MODEL_DIR_NAME)
    }

    fun isModelDownloaded(): Boolean {
        val modelDir = getModelDir()
        val modelFile = File(modelDir, MODEL_FILENAME)
        return modelFile.exists()
    }

    fun getModelPath(): String {
        val modelDir = getModelDir()
        val modelFile = File(modelDir, MODEL_FILENAME)
        return modelFile.absolutePath
    }

    fun getModelSize(): Long {
        val modelDir = getModelDir()
        val modelFile = File(modelDir, MODEL_FILENAME)
        return if (modelFile.exists()) modelFile.length() else 0L
    }

    fun getModelSizeInMB(): Float {
        return getModelSize() / (1024.0f * 1024.0f)
    }
} 