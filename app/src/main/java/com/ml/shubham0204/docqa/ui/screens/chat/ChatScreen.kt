package com.ml.shubham0204.docqa.ui.screens.chat

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.ml.shubham0204.docqa.R
import com.ml.shubham0204.docqa.domain.llm.LLMInitializationState
import com.ml.shubham0204.docqa.ui.components.AppAlertDialog
import com.ml.shubham0204.docqa.ui.components.createAlertDialog
import com.ml.shubham0204.docqa.ui.theme.DocQATheme
import dev.jeziellago.compose.markdowntext.MarkdownText
import org.koin.androidx.compose.koinViewModel
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onOpenDocsClick: (() -> Unit),
    onEditAPIKeyClick: (() -> Unit),
    onModelDownloadClick: (() -> Unit) = {},
) {
    DocQATheme {
        val chatViewModel: ChatViewModel = koinViewModel()
        val llmState by chatViewModel.llmInitializationState.collectAsState()
        val context = LocalContext.current
        val isSmsContextEnabled by chatViewModel.isSmsContextEnabled.collectAsState()
        var showSmsSettingsDialog by remember { mutableStateOf(false) }

        val isCallLogContextEnabled by chatViewModel.isCallLogContextEnabled.collectAsState()
        var showCallLogSettingsDialog by remember { mutableStateOf(false) }

        val isDocumentContextEnabled by chatViewModel.isDocumentContextEnabled.collectAsState()
        val isActionExecutorEnabled by chatViewModel.isActionExecutorEnabled.collectAsState()

        if (showSmsSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showSmsSettingsDialog = false },
                title = { Text("Permission Required") },
                text = {
                    Text(
                        "You have permanently denied the SMS permission. To use this feature, you must enable it from the app settings."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showSmsSettingsDialog = false
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                    ) {
                        Text("Open Settings")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSmsSettingsDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showCallLogSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showCallLogSettingsDialog = false },
                title = { Text("Permission Required") },
                text = {
                    Text(
                        "You have permanently denied the Call Log permission. To use this feature, you must enable it from the app settings."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showCallLogSettingsDialog = false
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                    ) {
                        Text("Open Settings")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCallLogSettingsDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        val smsPermissionLauncher =
            rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    chatViewModel.toggleSmsContext()
                } else {
                    val activity = context as? Activity
                    if (activity != null && !activity.shouldShowRequestPermissionRationale(Manifest.permission.READ_SMS)) {
                        showSmsSettingsDialog = true
                    } else {
                        Toast.makeText(context, "Permission denied.", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        val callLogPermissionLauncher =
            rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    chatViewModel.toggleCallLogContext()
                } else {
                    val activity = context as? Activity
                    if (activity != null && !activity.shouldShowRequestPermissionRationale(Manifest.permission.READ_CALL_LOG)) {
                        showCallLogSettingsDialog = true
                    } else {
                        Toast.makeText(context, "Permission denied.", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text(text = "Chat", style = MaterialTheme.typography.headlineSmall) },
                    actions = {
                        val isLocalModelAvailable by
                        remember { mutableStateOf(chatViewModel.isLocalModelAvailable()) }
                        val isRemoteModelAvailable by
                        remember { mutableStateOf(chatViewModel.isRemoteModelAvailable()) }
                        val tooltipState = rememberTooltipState()

                        // SMS Context Toggle
                        IconButton(
                            onClick = {
                                if (ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.READ_SMS
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    chatViewModel.toggleSmsContext()
                                } else {
                                    smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Message,
                                contentDescription = "Toggle SMS Context",
                                tint = if (isSmsContextEnabled) Color.Green else Color.Gray
                            )
                        }

                        // Call Log Context Toggle
                        IconButton(
                            onClick = {
                                if (ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.READ_CALL_LOG
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    chatViewModel.toggleCallLogContext()
                                } else {
                                    callLogPermissionLauncher.launch(Manifest.permission.READ_CALL_LOG)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Toggle Call Log Context",
                                tint = if (isCallLogContextEnabled) Color.Green else Color.Gray
                            )
                        }

                        // Document Context Toggle
                        IconButton(
                            onClick = {
                                chatViewModel.toggleDocumentContext()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Toggle Document Context",
                                tint = if (isDocumentContextEnabled) Color.Green else Color.Gray
                            )
                        }

                        // Action Executor Toggle
                        IconButton(
                            onClick = {
                                chatViewModel.toggleActionExecutor()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Toggle Action Executor",
                                tint = if (isActionExecutorEnabled) Color.Green else Color.Gray
                            )
                        }

                        // LLM Status Indicator
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                            tooltip = {
                                Text(
                                    text =
                                    when (llmState) {
                                        is LLMInitializationState.Initialized ->
                                            if (isLocalModelAvailable) "Using Local LLM (Qwen2.5-1.5B)"
                                            else "Using Remote LLM (Gemini)"
                                        LLMInitializationState.Initializing -> "LLM is initializing..."
                                        is LLMInitializationState.Error -> "LLM failed to initialize"
                                        LLMInitializationState.NotInitialized -> "LLM not initialized"
                                    }
                                )
                            },
                            state = tooltipState) {
                                Icon(
                                    imageVector =
                                    when (llmState) {
                                        is LLMInitializationState.Initialized ->
                                            if (isLocalModelAvailable) Icons.Default.Computer
                                            else Icons.Default.Cloud
                                        LLMInitializationState.Initializing ->
                                            Icons.Default.Downloading
                                        is LLMInitializationState.Error -> Icons.Default.Error
                                        LLMInitializationState.NotInitialized ->
                                            Icons.Default.Key
                                    },
                                    contentDescription = "LLM Status",
                                    tint =
                                    when (llmState) {
                                        is LLMInitializationState.Initialized ->
                                            if (isLocalModelAvailable) Color.Green else Color.Blue
                                        LLMInitializationState.Initializing -> Color.Yellow
                                        is LLMInitializationState.Error -> Color.Red
                                        LLMInitializationState.NotInitialized -> Color.Gray
                                    },
                                    modifier = Modifier.size(24.dp))
                            }

                        // Model Download Button (only show if local model is not available)
                        if (!isLocalModelAvailable) {
                            IconButton(onClick = onModelDownloadClick) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = "Download Local Model",
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        IconButton(onClick = onOpenDocsClick) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = "Open Documents",
                            )
                        }
                    })
            }) {
                Column(modifier = Modifier.fillMaxSize().padding(it)) {
                    val question by chatViewModel.questionState.collectAsState()
                    val response by chatViewModel.responseState.collectAsState()
                    val isGeneratingResponse by chatViewModel.isGeneratingResponseState.collectAsState()
                    val retrievedContextList by chatViewModel.retrievedContextListState.collectAsState()

                    Column(
                        modifier = Modifier.fillMaxSize().weight(1f),
                    ) {
                        if (question.trim().isEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxSize().align(Alignment.CenterHorizontally),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Icon(
                                    modifier = Modifier.size(75.dp),
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color.LightGray,
                                )
                                Text(
                                    text = "Enter a query to see answers",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.LightGray,
                                )
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                item {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(
                                                text = question,
                                                style = MaterialTheme.typography.headlineSmall,
                                                modifier = Modifier.weight(1f))
                                            IconButton(
                                                onClick = {
                                                    val sendIntent: Intent =
                                                        Intent()
                                                            .apply {
                                                                action = Intent.ACTION_SEND
                                                                putExtra(Intent.EXTRA_TEXT, "$question\n\n$response")
                                                                type = "text/plain"
                                                            }
                                                    val shareIntent = Intent.createChooser(sendIntent, null)
                                                    context.startActivity(shareIntent)
                                                }) {
                                                Icon(
                                                    imageVector = Icons.Default.Share, contentDescription = "Share")
                                            }
                                        }

                                        if (isGeneratingResponse) {
                                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(text = "Answer", style = MaterialTheme.typography.titleMedium)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        MarkdownText(
                                            markdown = response,
                                            style =
                                            TextStyle(
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 16.sp,
                                            ),
                                            modifier = Modifier.fillMaxWidth())

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(text = "Retrieved Context", style = MaterialTheme.typography.titleMedium)
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                }
                                items(retrievedContextList) {
                                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(
                                                text = "Source: ${it.fileName}",
                                                style =
                                                TextStyle(
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontSize = 12.sp,
                                                    fontStyle = FontStyle.Italic),
                                                maxLines = 1)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = it.context,
                                                style =
                                                TextStyle(
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontSize = 14.sp,
                                                ),
                                                maxLines = 4)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    var questionText by remember { mutableStateOf("") }
                    val keyboardController = LocalSoftwareKeyboardController.current
                    val isLlmReady = llmState is LLMInitializationState.Initialized

                    Row(modifier = Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            value = questionText,
                            onValueChange = { questionText = it },
                            enabled = !isGeneratingResponse,
                            label = {
                                Text(
                                    text =
                                    when (llmState) {
                                        is LLMInitializationState.Initialized -> "Ask a question"
                                        LLMInitializationState.Initializing -> "LLM is initializing..."
                                        is LLMInitializationState.Error -> "LLM failed to initialize"
                                        LLMInitializationState.NotInitialized -> "LLM not initialized"
                                    })
                            })

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            modifier = Modifier.background(Color.Blue, CircleShape),
                            enabled = !isGeneratingResponse && isLlmReady,
                            onClick = {
                                keyboardController?.hide()
                                if (isDocumentContextEnabled && !chatViewModel.checkNumDocuments()) {
                                    Toast
                                        .makeText(context, "Add documents to execute queries when document context is enabled", Toast.LENGTH_LONG)
                                        .show()
                                    return@IconButton
                                }

                                // Check if any LLM is available (local or remote)
                                if (!chatViewModel.isLocalModelAvailable() && !chatViewModel.isRemoteModelAvailable()) {
                                    createAlertDialog(
                                        dialogTitle = "No LLM Available",
                                        dialogText = "Please download a local model or configure a Gemini API key to use the app.",
                                        dialogPositiveButtonText = "Download Local Model",
                                        onPositiveButtonClick = onModelDownloadClick,
                                        dialogNegativeButtonText = "Add API Key",
                                        onNegativeButtonClick = onEditAPIKeyClick,
                                    )
                                    return@IconButton
                                }

                                if (questionText.trim().isEmpty()) {
                                    Toast.makeText(context, "Enter a query to execute", Toast.LENGTH_LONG).show()
                                    return@IconButton
                                }
                                try {
                                    chatViewModel.getAnswer(
                                        questionText,
                                        context.getString(R.string.prompt_1),
                                    )
                                } catch (e: Exception) {
                                    Log.e("ChatScreen", "Failed to get answer", e)
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally {
                                    questionText = ""
                                }
                            }) {
                            if (isGeneratingResponse) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Send",
                                    tint = Color.White,
                                )
                            }
                        }
                    }
                }
            }
    }
}
