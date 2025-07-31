package com.ml.shubham0204.docqa.ui.screens.advanced_options

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ml.shubham0204.docqa.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AdvancedOptionsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {


    private val _maxTokens = MutableStateFlow(0)
    val maxTokens: StateFlow<Int> = _maxTokens

    private val _recentMessages = MutableStateFlow(0)
    val recentMessages: StateFlow<Int> = _recentMessages

    private val _recentCallLogs = MutableStateFlow(0)
    val recentCallLogs: StateFlow<Int> = _recentCallLogs

    private val _ragTopK = MutableStateFlow(0)
    val ragTopK: StateFlow<Int> = _ragTopK

    init {
        loadSettings()
    }

    private fun loadSettings() {
        _maxTokens.value = settingsRepository.getMaxTokens()
        _recentMessages.value = settingsRepository.getRecentMessages()
        _recentCallLogs.value = settingsRepository.getRecentCallLogs()
        _ragTopK.value = settingsRepository.getRagTopK()
    }

    fun saveMaxTokens(value: Int) {
        _maxTokens.value = value
        viewModelScope.launch { settingsRepository.saveMaxTokens(value) }
    }

    fun saveRecentMessages(value: Int) {
        _recentMessages.value = value
        viewModelScope.launch { settingsRepository.saveRecentMessages(value) }
    }

    fun saveRecentCallLogs(value: Int) {
        _recentCallLogs.value = value
        viewModelScope.launch { settingsRepository.saveRecentCallLogs(value) }
    }

    fun saveRagTopK(value: Int) {
        _ragTopK.value = value
        viewModelScope.launch { settingsRepository.saveRagTopK(value) }
    }
} 