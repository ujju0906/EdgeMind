package com.ml.EdgeMind.docqa.domain.embeddings

import android.content.Context
import android.util.Log
import com.ml.shubham0204.sentence_embeddings.SentenceEmbedding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.koin.core.annotation.Single
import java.io.File

@Single
class SentenceEmbeddingProvider(
    private val context: Context,
) {
    private val sentenceEmbedding = SentenceEmbedding()
    private var isInitialized = false

    init {
        try {
            Log.d("SentenceEmbeddingProvider", "Starting initialization...")
            val onnxLocalFile = copyToLocalStorage("all-MiniLM-L6-V2.onnx")
            val tokenizerLocalFile = copyToLocalStorage("tokenizer.json")
            val tokenizerBytes = tokenizerLocalFile.readBytes()
            
            runBlocking(Dispatchers.IO) {
                sentenceEmbedding.init(
                    onnxLocalFile.absolutePath,
                    tokenizerBytes,
                    useTokenTypeIds = false,
                    outputTensorName = "last_hidden_state",
                    normalizeEmbeddings = false,
                )
            }
            isInitialized = true
            Log.d("SentenceEmbeddingProvider", "Initialization successful")
        } catch (e: Exception) {
            Log.e("SentenceEmbeddingProvider", "Failed to initialize sentence encoder: ${e.message}", e)
            isInitialized = false
        }
    }

    fun encodeText(text: String): FloatArray {
        if (!isInitialized) {
            Log.w("SentenceEmbeddingProvider", "Sentence encoder not initialized, returning empty embedding")
            return FloatArray(384) { 0f } // Return zero embedding if not initialized
        }
        
        return try {
            runBlocking(Dispatchers.Default) {
                sentenceEmbedding.encode(text)
            }
        } catch (e: Exception) {
            Log.e("SentenceEmbeddingProvider", "Failed to encode text: ${e.message}", e)
            FloatArray(384) { 0f } // Return zero embedding on error
        }
    }
    
    fun isReady(): Boolean = isInitialized

    // Copies the file from the assets folder to the app's internal
    // storage. Files stored in the assets folder are not accessible with
    // a `File` object that makes handling difficult.
    private fun copyToLocalStorage(filename: String): File {
        val storageFile = File(context.filesDir, filename)
        if (!storageFile.exists()) {
            val tokenizerBytes = context.assets.open(filename).readBytes()
            if (!storageFile.exists()) {
                storageFile.writeBytes(tokenizerBytes)
            }
            return storageFile
        } else {
            return storageFile
        }
    }
}
