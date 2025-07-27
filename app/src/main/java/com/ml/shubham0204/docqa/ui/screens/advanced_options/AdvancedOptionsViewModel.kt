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

    private val _topP = MutableStateFlow(0f)
    val topP: StateFlow<Float> = _topP

    private val _topK = MutableStateFlow(0)
    val topK: StateFlow<Int> = _topK

    private val _temperature = MutableStateFlow(0f)
    val temperature: StateFlow<Float> = _temperature

    private val _maxTokens = MutableStateFlow(0)
    val maxTokens: StateFlow<Int> = _maxTokens

    private val _recentMessages = MutableStateFlow(0)
    val recentMessages: StateFlow<Int> = _recentMessages

    private val _recentCallLogs = MutableStateFlow(0)
    val recentCallLogs: StateFlow<Int> = _recentCallLogs

    init {
        loadSettings()
    }

    private fun loadSettings() {
        _topP.value = settingsRepository.getTopP()
        _topK.value = settingsRepository.getTopK()
        _temperature.value = settingsRepository.getTemperature()
        _maxTokens.value = settingsRepository.getMaxTokens()
        _recentMessages.value = settingsRepository.getRecentMessages()
        _recentCallLogs.value = settingsRepository.getRecentCallLogs()
    }

    fun saveTopP(value: Float) {
        _topP.value = value
        viewModelScope.launch { settingsRepository.saveTopP(value) }
    }

    fun saveTopK(value: Int) {
        _topK.value = value
        viewModelScope.launch { settingsRepository.saveTopK(value) }
    }

    fun saveTemperature(value: Float) {
        _temperature.value = value
        viewModelScope.launch { settingsRepository.saveTemperature(value) }
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
} 