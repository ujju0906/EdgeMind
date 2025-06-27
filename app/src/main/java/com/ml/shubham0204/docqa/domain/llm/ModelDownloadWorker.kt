package com.ml.shubham0204.docqa.domain.llm

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class ModelDownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_PROGRESS = "progress"
        const val TAG = "ModelDownloadWorker"
        const val MODEL_URL = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/main/Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.task?download=true"
        const val MODEL_FILENAME = "Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.task"
        const val MODEL_DIR_NAME = "llm"
    }

    override suspend fun doWork(): Result {
        return try {
            val modelDir = File(applicationContext.filesDir, MODEL_DIR_NAME)
            val modelFile = File(modelDir, MODEL_FILENAME)
            downloadModelWithProgress(modelFile.absolutePath)
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Model download failed", e)
            Result.failure()
        }
    }

    private suspend fun downloadModelWithProgress(modelPath: String) = withContext(Dispatchers.IO) {
        val finalModelFile = File(modelPath)
        val tempModelFile = File(modelPath + ".tmp")
        try {
            val modelDir = finalModelFile.parentFile
            if (modelDir != null && !modelDir.exists()) {
                modelDir.mkdirs()
            }

            if (tempModelFile.exists()) {
                tempModelFile.delete()
            }

            Log.d(TAG, "Starting model download with progress tracking...")
            val url = URL(MODEL_URL)
            val connection = url.openConnection()
            val contentLength = connection.contentLengthLong
            val inputStream = connection.getInputStream()
            val outputStream = FileOutputStream(tempModelFile)

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalBytesRead = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead

                if (contentLength > 0) {
                    val progress = (totalBytesRead * 100 / contentLength).toInt()
                    setProgress(workDataOf(KEY_PROGRESS to progress))
                }
            }

            outputStream.close()
            inputStream.close()
            tempModelFile.renameTo(finalModelFile)
            setProgress(workDataOf(KEY_PROGRESS to 100))
            Log.d(TAG, "Model download completed.")

        } catch (e: Exception) {
            Log.e(TAG, "Error downloading model: ${e.message}", e)
            if (tempModelFile.exists()) {
                tempModelFile.delete()
            }
            throw e
        }
    }
} 