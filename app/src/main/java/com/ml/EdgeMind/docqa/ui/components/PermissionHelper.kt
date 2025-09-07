package com.ml.EdgeMind.docqa.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import java.io.File
import com.ml.EdgeMind.docqa.ui.components.NotificationPermissionHelper

object PermissionHelper {
    fun hasCallLogPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasSmsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasPhoneStatePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasAllRequiredPermissions(context: Context): Boolean {
        return hasCallLogPermission(context) && 
               hasSmsPermission(context) && 
               hasPhoneStatePermission(context)
    }
    
    fun hasStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For Android 11+ (API 30+), check if we can write to app's internal storage
            try {
                val testFile = File(context.filesDir, "test_write_permission")
                val canWrite = testFile.createNewFile()
                if (canWrite) {
                    testFile.delete()
                }
                canWrite
            } catch (e: Exception) {
                false
            }
        } else {
            // For older versions, check external storage permission
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    fun hasNotificationPermission(context: Context): Boolean {
        return NotificationPermissionHelper.hasNotificationPermission(context)
    }
    
    fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
}

@Composable
fun RequestCallLogPermission(
    onPermissionResult: (Boolean) -> Unit = {}
) {
    var hasPermission by remember {
        mutableStateOf(false)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        onPermissionResult(isGranted)
    }

    LaunchedEffect(Unit) {
        launcher.launch(Manifest.permission.READ_CALL_LOG)
    }
}

@Composable
fun RequestSmsPermission(
    onPermissionResult: (Boolean) -> Unit = {}
) {
    var hasPermission by remember {
        mutableStateOf(false)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        onPermissionResult(isGranted)
    }

    LaunchedEffect(Unit) {
        launcher.launch(Manifest.permission.READ_SMS)
    }
}

@Composable
fun RequestMultiplePermissions(
    permissions: Array<String>,
    onPermissionResult: (Map<String, Boolean>) -> Unit = {}
) {
    var permissionResults by remember {
        mutableStateOf<Map<String, Boolean>>(emptyMap())
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionResults = results
        onPermissionResult(results)
    }

    LaunchedEffect(Unit) {
        launcher.launch(permissions)
    }
}

@Composable
fun RequestStoragePermission(
    onPermissionResult: (Boolean) -> Unit = {}
) {
    var hasPermission by remember {
        mutableStateOf(false)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        onPermissionResult(isGranted)
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            launcher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            // For Android 11+, storage permission is not needed for app's internal storage
            onPermissionResult(true)
        }
    }
}

@Composable
fun RequestCameraPermission(
    onPermissionResult: (Boolean) -> Unit = {}
) {
    var hasPermission by remember {
        mutableStateOf(false)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        onPermissionResult(isGranted)
    }

    LaunchedEffect(Unit) {
        launcher.launch(Manifest.permission.CAMERA)
    }
} 