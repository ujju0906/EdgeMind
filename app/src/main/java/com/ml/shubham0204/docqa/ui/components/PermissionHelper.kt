package com.ml.shubham0204.docqa.ui.components

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