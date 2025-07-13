package com.ml.shubham0204.docqa.domain.llm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
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
            val hasNotificationPermission =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(
                        applicationContext,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }

            if (hasNotificationPermission) {
                // Create notification channel for Android O and above
                createNotificationChannel()

                // Start foreground service with notification
                setForeground(createForegroundInfo(0))
            }
            
            val modelDir = File(applicationContext.filesDir, MODEL_DIR_NAME)
            val modelFile = File(modelDir, MODEL_FILENAME)
            downloadModelWithProgress(modelFile.absolutePath, hasNotificationPermission)
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Model download failed", e)
            Result.failure()
        }
    }

    private suspend fun downloadModelWithProgress(modelPath: String, hasNotificationPermission: Boolean) = withContext(Dispatchers.IO) {
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
            val connection = url.openConnection() as HttpURLConnection
            
            // Optimize connection settings for faster downloads
            connection.connectTimeout = CONNECT_TIMEOUT
            connection.readTimeout = READ_TIMEOUT
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android) DocQA-App")
            connection.setRequestProperty("Accept", "*/*")
            connection.setRequestProperty("Accept-Encoding", "gzip, deflate")
            connection.setRequestProperty("Connection", "keep-alive")
            
            val contentLength = connection.contentLengthLong
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
                        if (hasNotificationPermission) {
                            // Update foreground notification with progress
                            setForeground(createForegroundInfo(progress))
                        }
                        lastProgressUpdate = progress.toLong()
                    }
                }
            }

            outputStream.close()
            inputStream.close()
            connection.disconnect()

            // Finalize the download
            Log.d(TAG, "Finalizing download...")
            if (finalModelFile.exists()) {
                Log.d(TAG, "Deleting existing model file.")
                finalModelFile.delete()
            }
            if (tempModelFile.renameTo(finalModelFile)) {
                Log.d(TAG, "Model file renamed successfully.")
                setProgress(workDataOf(KEY_PROGRESS to 100))
                if (hasNotificationPermission) {
                    setForeground(createForegroundInfo(100))
                }
                Log.d(TAG, "Model download completed.")
            } else {
                Log.e(TAG, "Failed to rename temp file to final model file.")
                throw Exception("Failed to finalize model download.")
            }

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
            .build()

        return ForegroundInfo(NOTIFICATION_ID, notification)
    }
} 