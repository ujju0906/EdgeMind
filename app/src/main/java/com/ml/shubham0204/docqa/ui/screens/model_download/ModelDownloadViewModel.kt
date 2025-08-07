package com.ml.shubham0204.docqa.ui.screens.model_download

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.ml.shubham0204.docqa.domain.llm.ModelDownloadWorker
import com.ml.shubham0204.docqa.domain.llm.ModelManager
import com.ml.shubham0204.docqa.domain.llm.AppLLMProvider
import com.ml.shubham0204.docqa.domain.llm.LLMFactory
import com.ml.shubham0204.docqa.domain.llm.ModelInfo
import com.ml.shubham0204.docqa.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject
import java.util.UUID

class ModelDownloadViewModel(
    private val modelManager: ModelManager,
    private val context: Context,
    private val llmFactory: LLMFactory,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    private val workManager = WorkManager.getInstance(context)
    
    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState
    
    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress
    
    private val _availableModels = MutableStateFlow<List<ModelInfo>>(emptyList())
    val availableModels: StateFlow<List<ModelInfo>> = _availableModels
    
    private val _downloadedModels = MutableStateFlow<List<ModelInfo>>(emptyList())
    val downloadedModels: StateFlow<List<ModelInfo>> = _downloadedModels
    
    private val _totalDownloadedSize = MutableStateFlow(0f)
    val totalDownloadedSize: StateFlow<Float> = _totalDownloadedSize
    
    private val _deleteState = MutableStateFlow<DeleteState>(DeleteState.Idle)
    val deleteState: StateFlow<DeleteState> = _deleteState
    
    private var currentWorkId: UUID? = null
    private var currentDownloadingModelId: String? = null
    
    init {
        checkModelStatus()
        observeExistingDownloads()
    }
    
    private fun observeExistingDownloads() {
        viewModelScope.launch {
            workManager.getWorkInfosByTagFlow("model_download").collect { workInfos ->
                val activeWork = workInfos.find { it.state == WorkInfo.State.RUNNING }
                if (activeWork != null) {
                    currentWorkId = activeWork.id
                    currentDownloadingModelId = activeWork.progress.getString(ModelDownloadWorker.KEY_MODEL_ID)
                    
                    val isPaused = activeWork.progress.getBoolean(ModelDownloadWorker.KEY_IS_PAUSED, false)
                    if (isPaused) {
                        val pauseReason = activeWork.progress.getString(ModelDownloadWorker.KEY_PAUSE_REASON) ?: "Unknown"
                        _downloadState.value = DownloadState.Paused(currentDownloadingModelId ?: "", pauseReason)
                    } else {
                        _downloadState.value = DownloadState.Downloading(currentDownloadingModelId ?: "")
                    }
                    
                    val progress = activeWork.progress.getInt(ModelDownloadWorker.KEY_PROGRESS, 0)
                    _downloadProgress.value = progress / 100f
                }
            }
        }
    }
    
    fun checkModelStatus() {
        _availableModels.value = modelManager.getAvailableModels()
        _downloadedModels.value = modelManager.getDownloadedModels()
        _totalDownloadedSize.value = modelManager.getTotalDownloadedSizeInMB()
    }
    
    fun downloadModel(modelId: String) {
        if (_downloadState.value is DownloadState.Downloading) return
        
        val modelInfo = modelManager.getModelInfo(modelId)
        if (modelInfo == null) {
            _downloadState.value = DownloadState.Error("Model not found: $modelId")
            return
        }
        
        if (modelInfo.isDownloaded) {
            _downloadState.value = DownloadState.Error("Model already downloaded: ${modelInfo.name}")
            return
        }

        val workRequestBuilder = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .addTag("model_download")

        if (modelInfo.requiresAuth) {
            val token = settingsRepository.getHuggingFaceToken()
            if (token.isNullOrEmpty()) {
                _downloadState.value = DownloadState.Error("Hugging Face token not set")
                return
            }
            workRequestBuilder.setInputData(
                androidx.work.workDataOf(
                    ModelDownloadWorker.KEY_MODEL_ID to modelId,
                    "access_token" to token
                )
            )
        } else {
            workRequestBuilder.setInputData(
                androidx.work.workDataOf(ModelDownloadWorker.KEY_MODEL_ID to modelId)
            )
        }

        val workRequest = workRequestBuilder.build()

        currentWorkId = workRequest.id
        currentDownloadingModelId = modelId
        workManager.enqueue(workRequest)
        
        viewModelScope.launch {
            workManager.getWorkInfoByIdFlow(workRequest.id).collect { workInfo ->
                workInfo?.let { info ->
                    when (info.state) {
                        WorkInfo.State.RUNNING -> {
                            val isPaused = info.progress.getBoolean(ModelDownloadWorker.KEY_IS_PAUSED, false)
                            if (isPaused) {
                                val pauseReason = info.progress.getString(ModelDownloadWorker.KEY_PAUSE_REASON) ?: "Unknown"
                                _downloadState.value = DownloadState.Paused(modelId, pauseReason)
                            } else {
                                _downloadState.value = DownloadState.Downloading(modelId)
                            }
                            val progress = info.progress.getInt(ModelDownloadWorker.KEY_PROGRESS, 0)
                            _downloadProgress.value = progress / 100f
                        }
                        WorkInfo.State.SUCCEEDED -> {
                            _downloadState.value = DownloadState.Success(modelId)
                            _downloadProgress.value = 1f
                            currentWorkId = null
                            currentDownloadingModelId = null
                            checkModelStatus()
                            
                            // Reinitialize LLM with the newly downloaded model
                            try {
                                AppLLMProvider.forceReinitialize(llmFactory, modelId)
                            } catch (e: Exception) {
                                // Log error but don't fail the download
                                android.util.Log.e("ModelDownloadViewModel", "Failed to reinitialize LLM after download: ${e.message}", e)
                            }
                        }
                        WorkInfo.State.FAILED -> {
                            _downloadState.value = DownloadState.Error("Download failed for model: $modelId")
                            currentWorkId = null
                            currentDownloadingModelId = null
                        }
                        WorkInfo.State.CANCELLED -> {
                            _downloadState.value = DownloadState.Cancelled
                            currentWorkId = null
                            currentDownloadingModelId = null
                        }
                        else -> {
                            // Other states like ENQUEUED, BLOCKED
                        }
                    }
                }
            }
        }
    }
    
    fun cancelDownload() {
        currentWorkId?.let { workId ->
            workManager.cancelWorkById(workId)
            _downloadState.value = DownloadState.Cancelled
            _downloadProgress.value = 0f
            currentWorkId = null
            currentDownloadingModelId = null
        }
    }
    
    fun resetDownloadState() {
        _downloadState.value = DownloadState.Idle
        _downloadProgress.value = 0f
        currentWorkId = null
        currentDownloadingModelId = null
    }
    
    fun deleteModel(modelId: String) {
        if (_deleteState.value is DeleteState.Deleting) return
        
        _deleteState.value = DeleteState.Deleting
        
        viewModelScope.launch {
            try {
                val success = modelManager.deleteModel(modelId)
                if (success) {
                    _deleteState.value = DeleteState.Success
                    checkModelStatus() // Update the model status
                    
                    // Reinitialize LLM after model deletion (will fall back to remote if available)
                    try {
                        AppLLMProvider.forceReinitialize(llmFactory)
                    } catch (e: Exception) {
                        // Log error but don't fail the deletion
                        android.util.Log.e("ModelDownloadViewModel", "Failed to reinitialize LLM after deletion: ${e.message}", e)
                    }
                } else {
                    _deleteState.value = DeleteState.Error("Failed to delete model: $modelId")
                }
            } catch (e: Exception) {
                _deleteState.value = DeleteState.Error("Error deleting model: ${e.message}")
            }
        }
    }
    
    fun deleteAllModels() {
        if (_deleteState.value is DeleteState.Deleting) return
        
        _deleteState.value = DeleteState.Deleting
        
        viewModelScope.launch {
            try {
                val success = modelManager.deleteAllModels()
                if (success) {
                    _deleteState.value = DeleteState.Success
                    checkModelStatus() // Update the model status
                    
                    // Reinitialize LLM after model deletion (will fall back to remote if available)
                    try {
                        AppLLMProvider.forceReinitialize(llmFactory)
                    } catch (e: Exception) {
                        // Log error but don't fail the deletion
                        android.util.Log.e("ModelDownloadViewModel", "Failed to reinitialize LLM after deletion: ${e.message}", e)
                    }
                } else {
                    _deleteState.value = DeleteState.Error("Failed to delete all models")
                }
            } catch (e: Exception) {
                _deleteState.value = DeleteState.Error("Error deleting all models: ${e.message}")
            }
        }
    }
    
    fun resetDeleteState() {
        _deleteState.value = DeleteState.Idle
    }
    
    fun getCurrentDownloadingModel(): ModelInfo? {
        return currentDownloadingModelId?.let { modelManager.getModelInfo(it) }
    }
    
    fun loadModel(modelId: String) {
        android.util.Log.d("ModelDownloadViewModel", "=== LOAD MODEL REQUESTED FROM UI ===")
        android.util.Log.d("ModelDownloadViewModel", "Target model: $modelId")
        android.util.Log.d("ModelDownloadViewModel", "Current model: ${AppLLMProvider.getCurrentModelName()}")
        
        viewModelScope.launch {
            try {
                AppLLMProvider.switchModel(llmFactory, modelId)
                android.util.Log.d("ModelDownloadViewModel", "=== LOAD MODEL COMPLETED ===")
                android.util.Log.d("ModelDownloadViewModel", "New model: ${AppLLMProvider.getCurrentModelName()}")
            } catch (e: Exception) {
                android.util.Log.e("ModelDownloadViewModel", "Failed to load model: ${e.message}", e)
            }
        }
    }
    
    sealed class DownloadState {
        object Idle : DownloadState()
        data class Downloading(val modelId: String) : DownloadState()
        data class Paused(val modelId: String, val reason: String) : DownloadState()
        data class Success(val modelId: String) : DownloadState()
        object Cancelled : DownloadState()
        data class Error(val message: String) : DownloadState()
    }
    
    sealed class DeleteState {
        object Idle : DeleteState()
        object Deleting : DeleteState()
        object Success : DeleteState()
        data class Error(val message: String) : DeleteState()
    }
} 