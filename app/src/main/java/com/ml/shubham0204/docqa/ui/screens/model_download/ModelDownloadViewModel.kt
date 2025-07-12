package com.ml.shubham0204.docqa.ui.screens.model_download

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.ml.shubham0204.docqa.domain.llm.ModelDownloadWorker
import com.ml.shubham0204.docqa.domain.llm.ModelManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject
import java.util.UUID

class ModelDownloadViewModel(
    private val modelManager: ModelManager,
    private val context: Context
) : ViewModel() {
    
    private val workManager = WorkManager.getInstance(context)
    
    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState
    
    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress
    
    private val _isModelDownloaded = MutableStateFlow(false)
    val isModelDownloaded: StateFlow<Boolean> = _isModelDownloaded
    
    private val _modelSize = MutableStateFlow(0f)
    val modelSize: StateFlow<Float> = _modelSize
    
    private val _deleteState = MutableStateFlow<DeleteState>(DeleteState.Idle)
    val deleteState: StateFlow<DeleteState> = _deleteState
    
    private var currentWorkId: UUID? = null
    
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
                    _downloadState.value = DownloadState.Downloading
                    val progress = activeWork.progress.getInt(ModelDownloadWorker.KEY_PROGRESS, 0)
                    _downloadProgress.value = progress / 100f
                }
            }
        }
    }
    
    fun checkModelStatus() {
        _isModelDownloaded.value = modelManager.isModelDownloaded()
        _modelSize.value = modelManager.getModelSizeInMB()
    }
    
    fun downloadModel() {
        if (_downloadState.value is DownloadState.Downloading) return
        
        val workRequest = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .addTag("model_download")
            .build()
        
        currentWorkId = workRequest.id
        workManager.enqueue(workRequest)
        
        viewModelScope.launch {
            workManager.getWorkInfoByIdFlow(workRequest.id).collect { workInfo ->
                when (workInfo.state) {
                    WorkInfo.State.RUNNING -> {
                        _downloadState.value = DownloadState.Downloading
                        val progress = workInfo.progress.getInt(ModelDownloadWorker.KEY_PROGRESS, 0)
                        _downloadProgress.value = progress / 100f
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        _downloadState.value = DownloadState.Success
                        _downloadProgress.value = 1f
                        currentWorkId = null
                        checkModelStatus()
                    }
                    WorkInfo.State.FAILED -> {
                        _downloadState.value = DownloadState.Error("Download failed")
                        currentWorkId = null
                    }
                    WorkInfo.State.CANCELLED -> {
                        _downloadState.value = DownloadState.Cancelled
                        currentWorkId = null
                    }
                    else -> {
                        // Other states like ENQUEUED, BLOCKED
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
        }
    }
    
    fun resetDownloadState() {
        _downloadState.value = DownloadState.Idle
        _downloadProgress.value = 0f
        currentWorkId = null
    }
    
    fun deleteModel() {
        if (_deleteState.value is DeleteState.Deleting) return
        
        _deleteState.value = DeleteState.Deleting
        
        viewModelScope.launch {
            try {
                val success = modelManager.deleteModel()
                if (success) {
                    _deleteState.value = DeleteState.Success
                    checkModelStatus() // Update the model status
                } else {
                    _deleteState.value = DeleteState.Error("Failed to delete model")
                }
            } catch (e: Exception) {
                _deleteState.value = DeleteState.Error("Error deleting model: ${e.message}")
            }
        }
    }
    
    fun resetDeleteState() {
        _deleteState.value = DeleteState.Idle
    }
    
    sealed class DownloadState {
        object Idle : DownloadState()
        object Downloading : DownloadState()
        object Success : DownloadState()
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