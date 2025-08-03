package com.ml.shubham0204.docqa.ui.screens.chat

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTooltipState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.ml.shubham0204.docqa.R
import com.ml.shubham0204.docqa.data.*
import com.ml.shubham0204.docqa.domain.llm.AppLLMProvider
import com.ml.shubham0204.docqa.domain.llm.LLMInitializationState
import com.ml.shubham0204.docqa.ui.theme.*
import com.ml.shubham0204.docqa.ui.screens.chat.ChatViewModel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import dev.jeziellago.compose.markdowntext.MarkdownText
import org.koin.androidx.compose.koinViewModel

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onOpenDocsClick: (() -> Unit),
    onEditAPIKeyClick: (() -> Unit),
    onModelDownloadClick: (() -> Unit) = {},
    onAdvancedOptionsClick: (() -> Unit)
) {
            DocQATheme {
            val chatViewModel: ChatViewModel = koinViewModel()
            val llmState by chatViewModel.llmInitializationState.collectAsState()
            val context = LocalContext.current
            val isSmsContextEnabled by chatViewModel.isSmsContextEnabled.collectAsState()
            var showSmsSettingsDialog by remember { mutableStateOf(false) }
            var showSmsPermissionDialog by remember { mutableStateOf(false) }

            val isCallLogContextEnabled by chatViewModel.isCallLogContextEnabled.collectAsState()
            var showCallLogSettingsDialog by remember { mutableStateOf(false) }
            var showCallLogPermissionDialog by remember { mutableStateOf(false) }

            val isDocumentContextEnabled by chatViewModel.isDocumentContextEnabled.collectAsState()
            
            // Camera permission states
            var showCameraSettingsDialog by remember { mutableStateOf(false) }
            var showCameraPermissionDialog by remember { mutableStateOf(false) }
            var cameraPermissionRequested by remember { mutableStateOf(false) }

        // Chat history state
        val chatHistory by chatViewModel.chatHistoryState.collectAsState()
        val question by chatViewModel.questionState.collectAsState()
        val response by chatViewModel.responseState.collectAsState()
        val isGeneratingResponse by chatViewModel.isGeneratingResponseState.collectAsState()
       // val retrievedContextList by chatViewModel.retrievedContextListState.collectAsState()
        
        // Debug chat history state
        LaunchedEffect(chatHistory) {
            Log.d("ChatScreen", "Chat history updated: ${chatHistory.size} messages")
            chatHistory.forEach { message: ChatMessage ->
                Log.d("ChatScreen", "Message: ${message.messageId} - ${if (message.isUserMessage) "USER" else "ASSISTANT"} - ${message.question}")
            }
        }
        
        // UI state
        var questionText by remember { mutableStateOf("") }
        var showChatMenu by remember { mutableStateOf(false) }
        var showSearchDialog by remember { mutableStateOf(false) }
        var searchQuery by remember { mutableStateOf("") }
        var showClearHistoryDialog by remember { mutableStateOf(false) }
        var showMediaSettingsDialog by remember { mutableStateOf(false) }
        
        val listState = rememberLazyListState()
        val keyboardController = LocalSoftwareKeyboardController.current
        val isLlmReady = llmState is LLMInitializationState.Initialized
        val actionsEnabled by chatViewModel.actionsEnabled.collectAsState()
        
        // Responsive design
        val configuration = LocalConfiguration.current
        val isTablet = configuration.screenWidthDp.dp >= 600.dp
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        
        // Responsive spacing and sizing
        val horizontalPadding = if (isTablet) 24.dp else 16.dp
        val verticalPadding = if (isTablet) 20.dp else 16.dp
        val cardSpacing = if (isTablet) 16.dp else 12.dp
        val maxContentWidth = if (isTablet) 600.dp else configuration.screenWidthDp.dp - (horizontalPadding * 2)

        // Auto-scroll to bottom when new messages arrive or response is streaming
        LaunchedEffect(chatHistory.size, response) {
            if (chatHistory.isNotEmpty() || response.isNotEmpty()) {
                listState.animateScrollToItem(
                    if (isGeneratingResponse) chatHistory.size else chatHistory.size - 1
                )
            }
        }

        // Permission launchers
        val smsPermissionLauncher =
            rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    chatViewModel.toggleSmsContext()
                    Toast.makeText(context, "SMS permission granted! Context enabled.", Toast.LENGTH_SHORT).show()
                } else {
                    val activity = context as? Activity
                    if (activity != null && !activity.shouldShowRequestPermissionRationale(Manifest.permission.READ_SMS)) {
                        showSmsSettingsDialog = true
                    } else {
                        Toast.makeText(context, "SMS permission denied. Context disabled.", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        val callLogPermissionLauncher =
            rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    chatViewModel.toggleCallLogContext()
                    Toast.makeText(context, "Call Log permission granted! Context enabled.", Toast.LENGTH_SHORT).show()
                } else {
                    val activity = context as? Activity
                    if (activity != null && !activity.shouldShowRequestPermissionRationale(Manifest.permission.READ_CALL_LOG)) {
                        showCallLogSettingsDialog = true
                    } else {
                        Toast.makeText(context, "Call Log permission denied. Context disabled.", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        val cameraPermissionLauncher =
            rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    // Camera permission granted, can now use flashlight
                    Toast.makeText(context, "🪄 Camera permission granted! You can now cast Lumos and Nox spells!", Toast.LENGTH_LONG).show()
                    cameraPermissionRequested = true
                } else {
                    val activity = context as? Activity
                    if (activity != null && !activity.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                        showCameraSettingsDialog = true
                    } else {
                        Toast.makeText(context, "Camera permission denied. Flashlight spells won't work.", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        val mediaPermissionLauncher =
            rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    // Media permission granted
                    Toast.makeText(context, "🖼️ Media permission granted! You can now access photos and files.", Toast.LENGTH_LONG).show()
                } else {
                    val activity = context as? Activity
                    if (activity != null && !activity.shouldShowRequestPermissionRationale(Manifest.permission.READ_MEDIA_IMAGES)) {
                        showMediaSettingsDialog = true
                    } else {
                        Toast.makeText(context, "Media permission denied. Gallery access won't work.", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        // SMS Permission Dialog
        if (showSmsPermissionDialog) {
            AlertDialog(
                onDismissRequest = { showSmsPermissionDialog = false },
                title = { Text("SMS Permission Required") },
                text = {
                    Text(
                        "This app needs SMS permission to provide context from your messages. This helps improve the quality of responses by understanding your communication patterns."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showSmsPermissionDialog = false
                            smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
                        }
                    ) {
                        Text("Grant Permission")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSmsPermissionDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // SMS Settings Dialog
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

        // Call Log Permission Dialog
        if (showCallLogPermissionDialog) {
            AlertDialog(
                onDismissRequest = { showCallLogPermissionDialog = false },
                title = { Text("Call Log Permission Required") },
                text = {
                    Text(
                        "This app needs Call Log permission to provide context from your call history. This helps improve the quality of responses by understanding your communication patterns."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showCallLogPermissionDialog = false
                            callLogPermissionLauncher.launch(Manifest.permission.READ_CALL_LOG)
                        }
                    ) {
                        Text("Grant Permission")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCallLogPermissionDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Call Log Settings Dialog
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

        // Camera Permission Dialog
        if (showCameraPermissionDialog) {
            AlertDialog(
                onDismissRequest = { showCameraPermissionDialog = false },
                title = { Text("Camera Permission Required") },
                text = {
                    Text(
                        "This app needs Camera permission to control the flashlight for Lumos and Nox spells. The camera is used to access the flashlight hardware."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showCameraPermissionDialog = false
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    ) {
                        Text("Grant Permission")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCameraPermissionDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Camera Settings Dialog
        if (showCameraSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showCameraSettingsDialog = false },
                title = { Text("Camera Permission Required") },
                text = {
                    Text(
                        "You have permanently denied the Camera permission. To use the flashlight (Lumos spell), you must enable it from the app settings."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showCameraSettingsDialog = false
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
                    TextButton(onClick = { showCameraSettingsDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showMediaSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showMediaSettingsDialog = false },
                title = { Text("Media Permission Required") },
                text = {
                    Text(
                        "You have permanently denied the Media permission. To access photos and files, you must enable it from the app settings."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showMediaSettingsDialog = false
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
                    TextButton(onClick = { showMediaSettingsDialog = false }) {
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

        // Set up permission request callbacks
        LaunchedEffect(Unit) {
            chatViewModel.setCameraPermissionRequestCallback {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    // Permission already granted, proceed with action
                    Toast.makeText(context, "Camera permission already granted!", Toast.LENGTH_SHORT).show()
                } else {
                    showCameraPermissionDialog = true
                }
            }
            chatViewModel.setSmsPermissionRequestCallback {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_SMS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    // Permission already granted, proceed with action
                    Toast.makeText(context, "SMS permission already granted!", Toast.LENGTH_SHORT).show()
                } else {
                    showSmsPermissionDialog = true
                }
            }
            chatViewModel.setCallLogPermissionRequestCallback {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_CALL_LOG
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    // Permission already granted, proceed with action
                    Toast.makeText(context, "Call Log permission already granted!", Toast.LENGTH_SHORT).show()
                } else {
                    showCallLogPermissionDialog = true
                }
            }
            chatViewModel.setMediaPermissionRequestCallback {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Android 13+ uses READ_MEDIA_IMAGES
                    mediaPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                } else {
                    // Android 12 and below use READ_EXTERNAL_STORAGE
                    mediaPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            text = "", 
                            style = if (isTablet) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold
                        ) 
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
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
                                    showSmsPermissionDialog = true
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
                                    showCallLogPermissionDialog = true
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

                        // Model Selection Dropdown
                        var showModelDropdown by remember { mutableStateOf(false) }
                        val currentModelInfo = AppLLMProvider.getCurrentModelInfo()
                        
                        // Log current model for debugging
                        LaunchedEffect(currentModelInfo) {
                            Log.d("ChatScreen", "Current model: ${currentModelInfo?.name ?: "None"}")
                        }
                        
                        Box {
                            IconButton(onClick = { showModelDropdown = true }) {
                                Icon(
                                    imageVector = when (llmState) {
                                        is LLMInitializationState.Initialized ->
                                            if (isLocalModelAvailable) Icons.Default.Computer
                                            else Icons.Default.Cloud
                                        LLMInitializationState.Initializing ->
                                            Icons.Default.Downloading
                                        is LLMInitializationState.Error -> Icons.Default.Error
                                        LLMInitializationState.NotInitialized -> Icons.Default.Key
                                    },
                                    contentDescription = "Model Selection",
                                    tint = when (llmState) {
                                        is LLMInitializationState.Initialized ->
                                            if (isLocalModelAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                                        LLMInitializationState.Initializing -> MaterialTheme.colorScheme.tertiary
                                        is LLMInitializationState.Error -> MaterialTheme.colorScheme.error
                                        LLMInitializationState.NotInitialized -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            DropdownMenu(
                                expanded = showModelDropdown,
                                onDismissRequest = { showModelDropdown = false }
                            ) {
                                // Current model info
                                currentModelInfo?.let { model ->
                                    DropdownMenuItem(
                                        text = { 
                                            Text(
                                                text = "Current: ${model.name}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        },
                                        onClick = { showModelDropdown = false }
                                    )
                                }
                                
                                // Available models
                                val downloadedModels = chatViewModel.getDownloadedModels()
                                downloadedModels.forEach { model ->
                                    DropdownMenuItem(
                                        text = { 
                                            Text(
                                                text = model.name,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        },
                                        onClick = {
                                            Log.d("ChatScreen", "User selected model: ${model.name} (${model.id})")
                                            chatViewModel.switchModel(model.id)
                                            showModelDropdown = false
                                        }
                                    )
                                }
                                
                                // Download new model option
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            text = "Download New Model...",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    },
                                    onClick = {
                                        showModelDropdown = false
                                        onModelDownloadClick()
                                    }
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = "Actions",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = actionsEnabled,
                                onCheckedChange = { chatViewModel.toggleActionsEnabled() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurface,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.outline
                                )
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
                                DropdownMenuItem(
                                    text = { Text("Debug Context Status") },
                                    onClick = {
                                        chatViewModel.debugContextStatus()
                                        showChatMenu = false
                                        Toast.makeText(context, "Context status logged to console", Toast.LENGTH_SHORT).show()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Advanced Options") },
                                    onClick = {
                                        showChatMenu = false
                                        onAdvancedOptionsClick()
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .then(
                                if (isTablet) {
                                    Modifier.widthIn(max = maxContentWidth)
                                } else {
                                    Modifier
                                }
                            ),
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(cardSpacing),
                        contentPadding = PaddingValues(
                            horizontal = horizontalPadding, 
                            vertical = verticalPadding
                        )
                    ) {
                        Log.d("ChatScreen", "Chat history size: ${chatHistory.size}, isGeneratingResponse: $isGeneratingResponse")
                        // Always show chat history section for debugging
                        if (false && chatHistory.isEmpty() && !isGeneratingResponse) {
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
                                        textAlign = TextAlign.Center
                                )
                                }
                            }
                        } else {
                            // Display chat history
                            val filteredHistory = if (isGeneratingResponse && question.isNotEmpty()) {
                                // Show all chat history but don't show the assistant message that's being streamed
                                chatHistory.filter { !(it.isUserMessage == false && it.response.isEmpty()) }
                            } else {
                                chatHistory
                            }
                            Log.d("ChatScreen", "Displaying ${filteredHistory.size} messages from ${chatHistory.size} total messages")
                            
                            // Debug item to show current state
                            if (filteredHistory.isEmpty()) {
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text(
                                                text = "DEBUG: No messages to display. Chat history size: ${chatHistory.size}",
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            TextButton(
                                                onClick = { 
                                                    chatViewModel.debugChatHistory()
                                                    chatViewModel.resetChatHistory()
                                                }
                                            ) {
                                                Text("Reload Chat History", color = MaterialTheme.colorScheme.onErrorContainer)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            TextButton(
                                                onClick = { 
                                                    chatViewModel.resetDatabase()
                                                }
                                            ) {
                                                Text("Reset Database", color = MaterialTheme.colorScheme.onErrorContainer)
                                            }
                                        }
                                    }
                                }
                            }
                            
                            items(filteredHistory) { message: ChatMessage ->
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
                                    val currentContext by chatViewModel.currentContextState.collectAsState()
                                    StreamingResponseItem(
                                        question = question,
                                        response = response,
                                        context = currentContext,
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
                                .padding(if (isTablet) 8.dp else 4.dp),
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
                                        is LLMInitializationState.Error -> "Type a message or try actions like 'open camera'"
                                        LLMInitializationState.NotInitialized -> "Type a message or try actions like 'open camera'"
                                        },
                                        style = if (isTablet) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium
                                    )
                                },
                                maxLines = if (isTablet) 4 else 3,
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    containerColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedBorderColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(if (isTablet) 24.dp else 20.dp),
                                textStyle = if (isTablet) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium
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
                                            MaterialTheme.colorScheme.primary,
                                            CircleShape
                                        ),
                                    enabled = true, // Always enabled - ChatViewModel handles LLM availability
                            onClick = {
                                keyboardController?.hide()
                                // Document context check removed - ChatViewModel handles this gracefully

                                // Check if any LLM is available (local or remote) only for non-action queries
                                // Actions can work without LLM, so we'll let the ChatViewModel handle this
                                // The ChatViewModel will check for actions first, and only require LLM for non-action queries

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
                                val intent = Intent(Intent.ACTION_VIEW, url.toUri())
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
                    
                    // Show detailed context dropdown if available
                    if (message.detailedContext.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ContextDropdown(contextText = message.detailedContext)
                    }
                }
            }
        }
    }
}

@Composable
fun ContextDropdown(contextText: String) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Context Used",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Hide context" else "Show context",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = contextText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun StreamingResponseItem(
    question: String,
    response: String,
    context: String,
    onShare: () -> Unit
) {
    val localContext = LocalContext.current
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
                            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                            localContext.startActivity(intent)
                        } catch (e: Exception) {
                            Log.e("ChatScreen", "Error opening link: $url", e)
                        }
                    }
                )
                
                // Context dropdown
                if (context.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    ContextDropdown(contextText = context)
                }
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
