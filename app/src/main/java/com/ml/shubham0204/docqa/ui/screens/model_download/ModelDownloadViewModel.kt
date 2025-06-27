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
    
    init {
        checkModelStatus()
    }
    
    fun checkModelStatus() {
        _isModelDownloaded.value = modelManager.isModelDownloaded()
        _modelSize.value = modelManager.getModelSizeInMB()
    }
    
    fun downloadModel() {
        if (_downloadState.value is DownloadState.Downloading) return
        
        val workRequest = OneTimeWorkRequestBuilder<ModelDownloadWorker>().build()
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
                        checkModelStatus()
                    }
                    WorkInfo.State.FAILED -> {
                        _downloadState.value = DownloadState.Error("Download failed")
                    }
                    else -> {
                        // Other states like ENQUEUED, BLOCKED, CANCELLED
                    }
                }
            }
        }
    }
    
    fun resetDownloadState() {
        _downloadState.value = DownloadState.Idle
        _downloadProgress.value = 0f
    }
    
    sealed class DownloadState {
        object Idle : DownloadState()
        object Downloading : DownloadState()
        object Success : DownloadState()
        data class Error(val message: String) : DownloadState()
    }
} 