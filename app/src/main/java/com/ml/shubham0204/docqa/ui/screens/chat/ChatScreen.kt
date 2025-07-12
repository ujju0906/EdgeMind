package com.ml.shubham0204.docqa.ui.screens.chat

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.ml.shubham0204.docqa.R
import com.ml.shubham0204.docqa.data.ChatMessage
import com.ml.shubham0204.docqa.domain.llm.LLMInitializationState
import com.ml.shubham0204.docqa.ui.components.createAlertDialog
import com.ml.shubham0204.docqa.ui.theme.DocQATheme
import dev.jeziellago.compose.markdowntext.MarkdownText
import org.koin.androidx.compose.koinViewModel
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.CircularProgressIndicator
import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.provider.Settings
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Divider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.text.style.TextOverflow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

        // Chat history state
        val chatHistory by chatViewModel.chatHistoryState.collectAsState()
        val question by chatViewModel.questionState.collectAsState()
        val response by chatViewModel.responseState.collectAsState()
        val isGeneratingResponse by chatViewModel.isGeneratingResponseState.collectAsState()
        val retrievedContextList by chatViewModel.retrievedContextListState.collectAsState()
        
        // UI state
        var questionText by remember { mutableStateOf("") }
        var showChatMenu by remember { mutableStateOf(false) }
        var showSearchDialog by remember { mutableStateOf(false) }
        var searchQuery by remember { mutableStateOf("") }
        var showClearHistoryDialog by remember { mutableStateOf(false) }
        
        val listState = rememberLazyListState()
        val keyboardController = LocalSoftwareKeyboardController.current
        val isLlmReady = llmState is LLMInitializationState.Initialized
        val actionsEnabled by chatViewModel.actionsEnabled.collectAsState()

        // Auto-scroll to bottom when new messages arrive or response is streaming
        LaunchedEffect(chatHistory.size, response) {
            if (chatHistory.isNotEmpty() || response.isNotEmpty()) {
                listState.animateScrollToItem(
                    if (isGeneratingResponse) chatHistory.size else chatHistory.size - 1
                )
            }
        }

        // Permission dialogs
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

        // Search dialog
        if (showSearchDialog) {
            AlertDialog(
                onDismissRequest = { showSearchDialog = false },
                title = { Text("Search Chat History") },
                text = {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search query") },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (searchQuery.isNotEmpty()) {
                                chatViewModel.searchChatHistory(searchQuery)
                            } else {
                                chatViewModel.resetChatHistory()
                            }
                            showSearchDialog = false
                            searchQuery = ""
                        }
                    ) {
                        Text("Search")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            chatViewModel.resetChatHistory()
                            showSearchDialog = false
                            searchQuery = ""
                        }
                    ) {
                        Text("Clear Search")
                    }
                }
            )
        }

        // Clear history dialog
        if (showClearHistoryDialog) {
            AlertDialog(
                onDismissRequest = { showClearHistoryDialog = false },
                title = { Text("Clear Chat History") },
                text = { Text("Are you sure you want to clear all chat history? This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            chatViewModel.clearChatHistory()
                            showClearHistoryDialog = false
                        }
                    ) {
                        Text("Clear", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearHistoryDialog = false }) {
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
                    title = { 
                        Text(
                            text = "DocQA Chat", 
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold
                        ) 
                    },
                    colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
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
                                tint = if (isSmsContextEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
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
                                tint = if (isCallLogContextEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
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
                                tint = if (isDocumentContextEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
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
                                IconButton(onClick = onModelDownloadClick) {
                                    Icon(
                                        imageVector =
                                        when (llmState) {
                                            is LLMInitializationState.Initialized ->
                                                if (isLocalModelAvailable) Icons.Default.Computer
                                                else Icons.Default.Cloud
                                            LLMInitializationState.Initializing ->
                                                Icons.Default.Downloading
                                            is LLMInitializationState.Error -> Icons.Default.Error
                                            LLMInitializationState.NotInitialized -> Icons.Default.Key
                                        },
                                        contentDescription = "LLM Status",
                                        tint =
                                        when (llmState) {
                                            is LLMInitializationState.Initialized ->
                                                if (isLocalModelAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                                            LLMInitializationState.Initializing -> MaterialTheme.colorScheme.tertiary
                                            is LLMInitializationState.Error -> MaterialTheme.colorScheme.error
                                            LLMInitializationState.NotInitialized -> MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                        // Model Download Button (only show if local model is not available)
                        if (!isLocalModelAvailable) {
                            IconButton(onClick = onModelDownloadClick) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = "Download Local Model",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        IconButton(onClick = onOpenDocsClick) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = "Open Documents",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Actions toggle switch
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Actions",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Switch(
                                checked = actionsEnabled,
                                onCheckedChange = { chatViewModel.toggleActionsEnabled() },
                                modifier = Modifier.padding(start = 4.dp, end = 8.dp)
                            )
                        }

                        // Chat menu
                        Box {
                            IconButton(onClick = { showChatMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Chat Menu",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = showChatMenu,
                                onDismissRequest = { showChatMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Search History") },
                                    onClick = {
                                        showSearchDialog = true
                                        showChatMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Clear History") },
                                    onClick = {
                                        showClearHistoryDialog = true
                                        showChatMenu = false
                                    }
                                )
                            }
                        }
                    })
            }) {
                    Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(it)
                ) {
                    // Chat messages area
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        if (chatHistory.isEmpty() && !isGeneratingResponse) {
                            item {
                            Column(
                                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                            imageVector = Icons.Default.Message,
                                    contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text(
                                        text = "Start a Conversation",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                        text = "Ask questions about your documents or get help with tasks",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                }
                            }
                        } else {
                            // Display chat history
                            val filteredHistory = if (isGeneratingResponse && question.isNotEmpty()) {
                                // Only show up to the latest user message and its streaming response
                                val lastUserIndex = chatHistory.indexOfLast { it.isUserMessage && it.question == question }
                                if (lastUserIndex != -1) chatHistory.take(lastUserIndex + 1) else chatHistory
                            } else {
                                chatHistory
                            }
                            items(filteredHistory) { message ->
                                ChatMessageItem(
                                    message = message,
                                    onDelete = { chatViewModel.deleteMessage(message.messageId) },
                                    onShare = {
                                        val shareText = if (message.isUserMessage) {
                                            message.question
                                        } else {
                                            "${message.question}\n\n${message.response}"
                                        }
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                            type = "text/plain"
                                        }
                                        val shareIntent = Intent.createChooser(sendIntent, null)
                                        context.startActivity(shareIntent)
                                    }
                                )
                            }

                            // Show streaming response only if not already in chat history
                            val lastMessage = filteredHistory.lastOrNull()
                            val shouldShowStreaming = isGeneratingResponse && question.isNotEmpty() && (
                                lastMessage == null || lastMessage.isUserMessage || lastMessage.response.isEmpty()
                            )
                            if (shouldShowStreaming) {
                                item {
                                    StreamingResponseItem(
                                        question = question,
                                        response = response,
                                        onShare = {
                                            val shareText = "$question\n\n$response"
                                            val sendIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT, shareText)
                                                type = "text/plain"
                                            }
                                            val shareIntent = Intent.createChooser(sendIntent, null)
                                            context.startActivity(shareIntent)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Input area
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 4.dp
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                        OutlinedTextField(
                                modifier = Modifier.weight(1f),
                            value = questionText,
                            onValueChange = { questionText = it },
                            enabled = !isGeneratingResponse,
                            label = {
                                Text(
                                        text = when (llmState) {
                                            is LLMInitializationState.Initialized -> "Ask a question..."
                                        LLMInitializationState.Initializing -> "LLM is initializing..."
                                        is LLMInitializationState.Error -> "LLM failed to initialize"
                                        LLMInitializationState.NotInitialized -> "LLM not initialized"
                                        }
                                    )
                                },
                                maxLines = 3,
                                colors = androidx.compose.material3.TextFieldDefaults.outlinedTextFieldColors(
                                    containerColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedBorderColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            if (isGeneratingResponse) {
                                // Stop button when generating
                                IconButton(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            MaterialTheme.colorScheme.error,
                                            CircleShape
                                        ),
                                    onClick = { chatViewModel.stopGeneration() }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = "Stop Generation",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            } else {
                                // Send button when not generating
                        IconButton(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            if (isLlmReady) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            CircleShape
                                        ),
                                    enabled = isLlmReady,
                            onClick = {
                                keyboardController?.hide()
                                if (isDocumentContextEnabled && !chatViewModel.checkNumDocuments()) {
                                            Toast.makeText(
                                                context,
                                                "Add documents to execute queries when document context is enabled",
                                                Toast.LENGTH_LONG
                                            ).show()
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
                                    }
                                ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Send",
                                    tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                    }
                }
            }
    }
}

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(message.timestamp))
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (message.isUserMessage) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with timestamp and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (message.isUserMessage) "You" else "Assistant",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (message.isUserMessage) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Message content
            if (message.isUserMessage) {
                Text(
                    text = message.question,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Column {
                    MarkdownText(
                        markdown = message.response,
                        style = TextStyle(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 16.sp,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        linkColor = MaterialTheme.colorScheme.primary,
                        onLinkClicked = { url ->
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Log.e("ChatScreen", "Error opening link: $url", e)
                            }
                        }
                    )
                    
                    // Show context used if available
                    if (message.contextUsed.isNotEmpty() && message.contextUsed != "No context") {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Context: ${message.contextUsed}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontStyle = FontStyle.Italic,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StreamingResponseItem(
    question: String,
    response: String,
    onShare: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Assistant",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row {
                    Text(
                        text = "Now",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Streaming response
            if (response.isNotEmpty()) {
                MarkdownText(
                    markdown = response,
                    style = TextStyle(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 16.sp,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    linkColor = MaterialTheme.colorScheme.primary,
                    onLinkClicked = { url ->
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Log.e("ChatScreen", "Error opening link: $url", e)
                        }
                    }
                )
            }
            
            // Typing indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "AI is typing...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                }
            }
    }
}
