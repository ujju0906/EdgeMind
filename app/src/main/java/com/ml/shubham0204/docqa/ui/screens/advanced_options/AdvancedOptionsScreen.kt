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
    val maxTokens by viewModel.maxTokens.collectAsState()
    val recentMessages by viewModel.recentMessages.collectAsState()
    val recentCallLogs by viewModel.recentCallLogs.collectAsState()
    val ragTopK by viewModel.ragTopK.collectAsState()

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
            HyperparameterInputField(
                label = "Max Tokens",
                value = maxTokens,
                onValueChange = { viewModel.saveMaxTokens(it) }
            )
            HyperparameterIntSlider(
                label = "Recent Messages",
                value = recentMessages,
                onValueChange = { viewModel.saveRecentMessages(it) },
                valueRange = 1..10
            )
            HyperparameterIntSlider(
                label = "Recent Call Logs",
                value = recentCallLogs,
                onValueChange = { viewModel.saveRecentCallLogs(it) },
                valueRange = 1..10
            )
            HyperparameterIntSlider(
                label = "Document Chunks (Top K)",
                value = ragTopK,
                onValueChange = { viewModel.saveRagTopK(it) },
                valueRange = 1..10
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

@Composable
fun HyperparameterIntSlider(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    valueRange: IntRange
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(text = "$label: $value")
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
            steps = valueRange.last - valueRange.first - 1
        )
    }
} 