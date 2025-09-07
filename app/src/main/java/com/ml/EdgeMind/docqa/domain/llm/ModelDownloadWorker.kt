package com.ml.EdgeMind.docqa.domain.llm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.net.URL
import java.net.HttpURLConnection

class ModelDownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_PROGRESS = "progress"
        const val KEY_MODEL_ID = "model_id"
        const val KEY_DOWNLOADED_BYTES = "downloaded_bytes"
        const val KEY_IS_PAUSED = "is_paused"
        const val KEY_PAUSE_REASON = "pause_reason"
        const val TAG = "ModelDownloadWorker"
        const val MODEL_DIR_NAME = "llm"
        // Increased buffer size for better performance
        private const val BUFFER_SIZE = 64 * 1024 // 64KB buffer
        private const val CONNECT_TIMEOUT = 30000 // 30 seconds
        private const val READ_TIMEOUT = 60000 // 60 seconds
        private const val NETWORK_CHECK_INTERVAL = 5000L // 5 seconds
        private const val MAX_RETRY_ATTEMPTS = 3
        
        // Notification constants
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "model_download_channel"
        private const val CHANNEL_NAME = "Model Download"
    }

    private var isNetworkAvailable = true
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private fun getHuggingFaceToken(): String? {
        return try {
            val properties = java.util.Properties()
            applicationContext.assets.open("local.properties").use {
                properties.load(it)
                properties.getProperty("hf.token")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Could not read hf.token from local.properties", e)
            null
        }
    }

    override suspend fun doWork(): Result {
        return try {
            // Get model ID from input data
            val modelId = inputData.getString(KEY_MODEL_ID) ?: "qwen2.5-1.5b"
            val modelInfo = ModelManager.AVAILABLE_MODELS.find { it.id == modelId }
                ?: return Result.failure()
            
            // Setup network monitoring
            setupNetworkMonitoring()
            
            // Create notification channel for Android O and above
            createNotificationChannel()
            
            // For Android 15+, avoid foreground service to prevent crashes
            val shouldUseForeground = Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE // Android 15
            if (shouldUseForeground) {
                try {
                    setForeground(createForegroundInfo(0, modelInfo.name, "Starting download..."))
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
            
            val modelFile = File(modelDir, modelInfo.filename)
            downloadModelWithProgress(modelFile.absolutePath, modelInfo, modelId, shouldUseForeground)
            
            // Cleanup network monitoring
            cleanupNetworkMonitoring()
            
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Model download failed", e)
            cleanupNetworkMonitoring()
            Result.failure()
        }
    }

    private fun setupNetworkMonitoring() {
        connectivityManager = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG, "Network became available")
                isNetworkAvailable = true
            }

            override fun onLost(network: Network) {
                Log.d(TAG, "Network connection lost")
                isNetworkAvailable = false
            }
        }

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager?.registerNetworkCallback(networkRequest, networkCallback!!)
    }

    private fun cleanupNetworkMonitoring() {
        networkCallback?.let { callback ->
            connectivityManager?.unregisterNetworkCallback(callback)
        }
        networkCallback = null
        connectivityManager = null
    }

    private fun isNetworkConnected(): Boolean {
        val cm = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetwork ?: return false
        val networkCapabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private suspend fun downloadModelWithProgress(
        modelPath: String, 
        modelInfo: ModelInfo,
        modelId: String,
        useForeground: Boolean = true
    ) = withContext(Dispatchers.IO) {
        val finalModelFile = File(modelPath)
        val tempModelFile = File("$modelPath.tmp")
        var downloadedBytes = 0L
        var retryAttempts = 0
        
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

            // Check if we have a partial download to resume
            if (tempModelFile.exists()) {
                downloadedBytes = tempModelFile.length()
                Log.d(TAG, "Found partial download: ${downloadedBytes} bytes")
                setProgress(workDataOf(
                    KEY_PROGRESS to 0,
                    KEY_MODEL_ID to modelId,
                    KEY_DOWNLOADED_BYTES to downloadedBytes,
                    KEY_IS_PAUSED to false
                ))
            }

            Log.d(TAG, "Starting model download with progress tracking...")
            Log.d(TAG, "Model: ${modelInfo.name}")
            Log.d(TAG, "Download URL: ${modelInfo.url}")
            Log.d(TAG, "Target file: ${finalModelFile.absolutePath}")
            Log.d(TAG, "Resuming from: ${downloadedBytes} bytes")
            
            var downloadSuccessful = false
            while (retryAttempts < MAX_RETRY_ATTEMPTS && !downloadSuccessful) {
                try {
                    val url = URL(modelInfo.url)
                    val connection = url.openConnection() as HttpURLConnection

                    if (modelInfo.requiresAuth) {
                        val token = getHuggingFaceToken()
                        if (token != null) {
                            connection.setRequestProperty("Authorization", "Bearer $token")
                        }
                    }

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
                    
                    // Add range header for resume support
                    if (downloadedBytes > 0) {
                        connection.setRequestProperty("Range", "bytes=$downloadedBytes-")
                        Log.d(TAG, "Requesting range: bytes=$downloadedBytes-")
                    }
                    
                    val contentLength = connection.contentLengthLong
                    val totalSize = if (downloadedBytes > 0) downloadedBytes + contentLength else contentLength
                    Log.d(TAG, "Content length: $contentLength bytes, Total size: $totalSize bytes")
                    
                    val responseCode = connection.responseCode
                    Log.d(TAG, "Response code: $responseCode")
                    
                    if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_PARTIAL) {
                        throw Exception("HTTP error: $responseCode - ${connection.responseMessage}")
                    }
                    
                    val inputStream = connection.getInputStream()
                    val randomAccessFile = if (downloadedBytes > 0) {
                        RandomAccessFile(tempModelFile, "rw").apply { seek(downloadedBytes) }
                    } else {
                        null
                    }
                    val outputStream = if (downloadedBytes > 0) {
                        FileOutputStream(randomAccessFile!!.fd)
                    } else {
                        FileOutputStream(tempModelFile)
                    }

                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int
                    var lastProgressUpdate = 0L
                    var lastNetworkCheck = System.currentTimeMillis()

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        // Check network connectivity periodically
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastNetworkCheck > NETWORK_CHECK_INTERVAL) {
                            if (!isNetworkConnected()) {
                                Log.d(TAG, "Network disconnected, pausing download...")
                                setProgress(workDataOf(
                                    KEY_PROGRESS to (downloadedBytes * 100 / totalSize).toInt(),
                                    KEY_MODEL_ID to modelId,
                                    KEY_DOWNLOADED_BYTES to downloadedBytes,
                                    KEY_IS_PAUSED to true,
                                    KEY_PAUSE_REASON to "Network disconnected"
                                ))
                                
                                if (useForeground) {
                                    try {
                                        setForeground(createForegroundInfo(
                                            (downloadedBytes * 100 / totalSize).toInt(),
                                            modelInfo.name,
                                            "Paused - Waiting for network..."
                                        ))
                                    } catch (e: Exception) {
                                        Log.w(TAG, "Failed to update foreground notification: ${e.message}")
                                    }
                                }
                                
                                // Wait for network to come back
                                while (!isNetworkConnected()) {
                                    try {
                                        Thread.sleep(1000)
                                    } catch (e: InterruptedException) {
                                        Log.d(TAG, "Network wait interrupted")
                                        return@withContext
                                    }
                                    if (isStopped) {
                                        Log.d(TAG, "Download stopped while waiting for network")
                                        return@withContext
                                    }
                                }
                                
                                Log.d(TAG, "Network restored, resuming download...")
                                setProgress(workDataOf(
                                    KEY_PROGRESS to (downloadedBytes * 100 / totalSize).toInt(),
                                    KEY_MODEL_ID to modelId,
                                    KEY_DOWNLOADED_BYTES to downloadedBytes,
                                    KEY_IS_PAUSED to false
                                ))
                                
                                if (useForeground) {
                                    try {
                                        setForeground(createForegroundInfo(
                                            (downloadedBytes * 100 / totalSize).toInt(),
                                            modelInfo.name,
                                            "Resuming download..."
                                        ))
                                    } catch (e: Exception) {
                                        Log.w(TAG, "Failed to update foreground notification: ${e.message}")
                                    }
                                }
                                
                                lastNetworkCheck = currentTime
                            }
                        }
                        
                        outputStream.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        if (totalSize > 0) {
                            val progress = (downloadedBytes * 100 / totalSize).toInt()
                            // Update progress less frequently to reduce overhead
                            if (progress > lastProgressUpdate || downloadedBytes == totalSize) {
                                setProgress(workDataOf(
                                    KEY_PROGRESS to progress,
                                    KEY_MODEL_ID to modelId,
                                    KEY_DOWNLOADED_BYTES to downloadedBytes,
                                    KEY_IS_PAUSED to false
                                ))
                                // Update foreground notification only if enabled and supported
                                if (useForeground) {
                                    try {
                                        setForeground(createForegroundInfo(progress, modelInfo.name))
                                    } catch (e: Exception) {
                                        Log.w(TAG, "Failed to update foreground notification: ${e.message}")
                                        // Continue without foreground notification update
                                    }
                                }
                                lastProgressUpdate = progress.toLong()
                            }
                        }
                        
                        // Check if work is stopped
                        if (isStopped) {
                            Log.d(TAG, "Download stopped by user")
                            return@withContext
                        }
                    }

                    outputStream.close()
                    randomAccessFile?.close()
                    inputStream.close()
                    connection.disconnect()
                    
                    // Download completed successfully
                    downloadSuccessful = true
                    
                } catch (e: Exception) {
                    retryAttempts++
                    Log.e(TAG, "Download attempt $retryAttempts failed: ${e.message}")
                    
                    if (retryAttempts >= MAX_RETRY_ATTEMPTS) {
                        throw e
                    }
                    
                    // Wait before retrying
                    try {
                        Thread.sleep((2000 * retryAttempts).toLong()) // Exponential backoff
                    } catch (e: InterruptedException) {
                        Log.d(TAG, "Retry wait interrupted")
                        return@withContext
                    }
                    
                    if (isStopped) {
                        Log.d(TAG, "Download stopped during retry")
                        return@withContext
                    }
                }
            }
            
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
            
            setProgress(workDataOf(
                KEY_PROGRESS to 100,
                KEY_MODEL_ID to modelId,
                KEY_DOWNLOADED_BYTES to finalModelFile.length(),
                KEY_IS_PAUSED to false
            ))
            // Update final foreground notification only if enabled
            if (useForeground) {
                try {
                    setForeground(createForegroundInfo(100, modelInfo.name, "Download completed!"))
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to update final foreground notification: ${e.message}")
                    // Continue without foreground notification update
                }
            }
            Log.d(TAG, "Model download completed successfully. File size: ${finalModelFile.length()} bytes")

        } catch (e: Exception) {
            Log.e(TAG, "Error downloading model: ${e.message}", e)
            // Don't delete temp file on error - it can be resumed later
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

    private fun createForegroundInfo(progress: Int, modelName: String, status: String = ""): ForegroundInfo {
        val title = "Downloading $modelName"
        val message = when {
            progress == 100 -> "Download completed!"
            status.isNotEmpty() -> status
            else -> "Downloading... $progress%"
        }
        
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