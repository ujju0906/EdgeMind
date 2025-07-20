package com.ml.shubham0204.docqa.domain.llm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.net.HttpURLConnection

class ModelDownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_PROGRESS = "progress"
        const val TAG = "ModelDownloadWorker"
//        const val MODEL_URL = "https://huggingface.co/litert-community/TinyLlama-1.1B-Chat-v1.0/resolve/main/TinyLlama-1.1B-Chat-v1.0_multi-prefill-seq_q8_ekv1280.task?download=true"
//        const val MODEL_FILENAME = "TinyLlama-1.1B-Chat-v1.0_multi-prefill-seq_q8_ekv1280.task"
//
//        const val MODEL_URL = "https://huggingface.co/litert-community/Phi-4-mini-instruct/resolve/main/Phi-4-mini-instruct_multi-prefill-seq_q8_ekv1280.task?download=true"
//        const val MODEL_FILENAME = "Phi-4-mini-instruct_multi-prefill-seq_q8_ekv1280.task"
        const val MODEL_URL = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/main/Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.task?download=true"
        const val MODEL_FILENAME = "Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.task"
        const val MODEL_DIR_NAME = "llm"
        // Increased buffer size for better performance
        private const val BUFFER_SIZE = 64 * 1024 // 64KB buffer
        private const val CONNECT_TIMEOUT = 30000 // 30 seconds
        private const val READ_TIMEOUT = 60000 // 60 seconds
        
        // Notification constants
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "model_download_channel"
        private const val CHANNEL_NAME = "Model Download"
    }

    override suspend fun doWork(): Result {
        return try {
            // Create notification channel for Android O and above
            createNotificationChannel()
            
            // For Android 15+, avoid foreground service to prevent crashes
            val shouldUseForeground = Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE // Android 15
            if (shouldUseForeground) {
                try {
                    setForeground(createForegroundInfo(0))
                    Log.d(TAG, "Foreground service started successfully")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to start foreground service: ${e.message}")
                }
            } else {
                Log.d(TAG, "Skipping foreground service for Android 15+ to avoid crashes")
            }
            
            // Check and create model directory with proper permissions
            val modelDir = File(applicationContext.filesDir, MODEL_DIR_NAME)
            if (!modelDir.exists()) {
                val created = modelDir.mkdirs()
                if (!created) {
                    Log.e(TAG, "Failed to create model directory: ${modelDir.absolutePath}")
                    return Result.failure()
                }
                Log.d(TAG, "Created model directory: ${modelDir.absolutePath}")
            }
            
            // Check if directory is writable
            if (!modelDir.canWrite()) {
                Log.e(TAG, "Model directory is not writable: ${modelDir.absolutePath}")
                return Result.failure()
            }
            
            val modelFile = File(modelDir, MODEL_FILENAME)
            downloadModelWithProgress(modelFile.absolutePath, shouldUseForeground)
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Model download failed", e)
            Result.failure()
        }
    }

    private suspend fun downloadModelWithProgress(modelPath: String, useForeground: Boolean = true) = withContext(Dispatchers.IO) {
        val finalModelFile = File(modelPath)
        val tempModelFile = File("$modelPath.tmp")
        try {
            val modelDir = finalModelFile.parentFile
            if (modelDir != null && !modelDir.exists()) {
                val created = modelDir.mkdirs()
                if (!created) {
                    throw Exception("Failed to create model directory: ${modelDir.absolutePath}")
                }
            }

            // Check if we can write to the directory
            if (!modelDir!!.canWrite()) {
                throw Exception("Cannot write to model directory: ${modelDir.absolutePath}")
            }

            // Clean up any existing temp file
            if (tempModelFile.exists()) {
                val deleted = tempModelFile.delete()
                if (!deleted) {
                    Log.w(TAG, "Failed to delete existing temp file: ${tempModelFile.absolutePath}")
                }
            }

            Log.d(TAG, "Starting model download with progress tracking...")
            Log.d(TAG, "Download URL: $MODEL_URL")
            Log.d(TAG, "Target file: ${finalModelFile.absolutePath}")
            
            val url = URL(MODEL_URL)
            val connection = url.openConnection() as HttpURLConnection
            
            // Optimize connection settings for faster downloads
            connection.connectTimeout = CONNECT_TIMEOUT
            connection.readTimeout = READ_TIMEOUT
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android) DocQA-App")
            connection.setRequestProperty("Accept", "*/*")
            connection.setRequestProperty("Accept-Encoding", "gzip, deflate")
            connection.setRequestProperty("Connection", "keep-alive")
            
            // Add additional headers for better compatibility
            connection.setRequestProperty("Cache-Control", "no-cache")
            connection.setRequestProperty("Pragma", "no-cache")
            
            val contentLength = connection.contentLengthLong
            Log.d(TAG, "Content length: $contentLength bytes")
            
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("HTTP error: ${connection.responseCode} - ${connection.responseMessage}")
            }
            
            val inputStream = connection.getInputStream()
            val outputStream = FileOutputStream(tempModelFile)

            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int
            var totalBytesRead = 0L
            var lastProgressUpdate = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead

                if (contentLength > 0) {
                    val progress = (totalBytesRead * 100 / contentLength).toInt()
                    // Update progress less frequently to reduce overhead
                    if (progress > lastProgressUpdate || totalBytesRead == contentLength) {
                        setProgress(workDataOf(KEY_PROGRESS to progress))
                        // Update foreground notification only if enabled and supported
                        if (useForeground) {
                            try {
                                setForeground(createForegroundInfo(progress))
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to update foreground notification: ${e.message}")
                                // Continue without foreground notification update
                            }
                        }
                        lastProgressUpdate = progress.toLong()
                    }
                }
            }

            outputStream.close()
            inputStream.close()
            connection.disconnect()
            
            // Verify the temp file was created and has content
            if (!tempModelFile.exists() || tempModelFile.length() == 0L) {
                throw Exception("Downloaded file is empty or missing: ${tempModelFile.absolutePath}")
            }
            
            Log.d(TAG, "Temp file size: ${tempModelFile.length()} bytes")
            
            // Rename temp file to final file
            val renamed = tempModelFile.renameTo(finalModelFile)
            if (!renamed) {
                throw Exception("Failed to rename temp file to final file")
            }
            
            // Verify final file
            if (!finalModelFile.exists() || finalModelFile.length() == 0L) {
                throw Exception("Final model file is empty or missing: ${finalModelFile.absolutePath}")
            }
            
            setProgress(workDataOf(KEY_PROGRESS to 100))
            // Update final foreground notification only if enabled
            if (useForeground) {
                try {
                    setForeground(createForegroundInfo(100))
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to update final foreground notification: ${e.message}")
                    // Continue without foreground notification update
                }
            }
            Log.d(TAG, "Model download completed successfully. File size: ${finalModelFile.length()} bytes")

        } catch (e: Exception) {
            Log.e(TAG, "Error downloading model: ${e.message}", e)
            if (tempModelFile.exists()) {
                tempModelFile.delete()
            }
            throw e
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress for model downloads"
                setShowBadge(false)
            }

            val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createForegroundInfo(progress: Int): ForegroundInfo {
        val title = "Downloading AI Model"
        val message = if (progress == 100) "Download completed!" else "Downloading... $progress%"
        
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setProgress(100, progress, false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setSilent(true) // Reduce notification noise
            .build()

        return ForegroundInfo(NOTIFICATION_ID, notification)
    }
} 