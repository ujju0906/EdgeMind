package com.ml.shubham0204.docqa.ui.screens.advanced_options

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedOptionsScreen(
    viewModel: AdvancedOptionsViewModel = koinViewModel(),
    onBackClick: () -> Unit
) {
    val topP by viewModel.topP.collectAsState()
    val topK by viewModel.topK.collectAsState()
    val temperature by viewModel.temperature.collectAsState()
    val maxTokens by viewModel.maxTokens.collectAsState()
    val recentMessages by viewModel.recentMessages.collectAsState()
    val recentCallLogs by viewModel.recentCallLogs.collectAsState()

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Advanced Options") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        )
    }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HyperparameterSlider(
                label = "Top P",
                value = topP,
                onValueChange = { viewModel.saveTopP(it) },
                valueRange = 0f..1f,
                steps = 100
            )
            HyperparameterInputField(
                label = "Top K",
                value = topK,
                onValueChange = { viewModel.saveTopK(it) }
            )
            HyperparameterSlider(
                label = "Temperature",
                value = temperature,
                onValueChange = { viewModel.saveTemperature(it) },
                valueRange = 0f..2f,
                steps = 20
            )
            HyperparameterInputField(
                label = "Max Tokens",
                value = maxTokens,
                onValueChange = { viewModel.saveMaxTokens(it) }
            )
            HyperparameterSlider(
                label = "Recent Messages",
                value = recentMessages.toFloat(),
                onValueChange = { viewModel.saveRecentMessages(it.toInt()) },
                valueRange = 1f..10f,
                steps = 9
            )
            HyperparameterSlider(
                label = "Recent Call Logs",
                value = recentCallLogs.toFloat(),
                onValueChange = { viewModel.saveRecentCallLogs(it.toInt()) },
                valueRange = 1f..10f,
                steps = 9
            )
        }
    }
}

@Composable
fun HyperparameterSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(text = "$label: ${String.format("%.2f", value)}")
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps
        )
    }
}

@Composable
fun HyperparameterInputField(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(text = label)
        OutlinedTextField(
            value = value.toString(),
            onValueChange = { onValueChange(it.toIntOrNull() ?: 0) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
    }
} 