package com.ml.shubham0204.docqa.domain.actions

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast

data class AppAction(
    val id: String,
    val descriptions: List<String>,
    val action: (Context, String) -> String?,
    var embedding: FloatArray? = null,
    val showInChat: Boolean = false,
    val response: String = ""
)

fun getPredefinedActions(context: Context): List<AppAction> {
    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    val rearCameraId = try {
        cameraManager.cameraIdList.find {
            cameraManager.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING) ==
                CameraCharacteristics.LENS_FACING_BACK
        }
    } catch (e: Exception) {
        null // Handle case where camera access fails
    }

    val actions =
        mutableListOf(
            AppAction(
                id = "open_camera",
                descriptions = listOf("Open camera", "Take a picture", "Launch camera"),
                action = { ctx, _ ->
                    try {
                        // Check if camera permission is granted
                        if (ctx.checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                            // Open app settings to let user grant camera permission
                            try {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = android.net.Uri.fromParts("package", ctx.packageName, null)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                ctx.startActivity(intent)
                                Toast.makeText(ctx, "Please enable Camera permission and try again", Toast.LENGTH_LONG).show()
                                return@AppAction "Opening app settings to enable camera permission"
                            } catch (e: Exception) {
                                Toast.makeText(ctx, "Camera permission is required. Please enable it in Settings → Apps → DocQA → Permissions", Toast.LENGTH_LONG).show()
                                return@AppAction "Camera permission is required. Please enable it in Settings → Apps → DocQA → Permissions"
                            }
                        }
                        
                        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        
                        // Check if there's a camera app that can handle this intent
                        if (intent.resolveActivity(ctx.packageManager) != null) {
                            ctx.startActivity(intent)
                            null
                        } else {
                            Toast.makeText(ctx, "No camera app available", Toast.LENGTH_SHORT).show()
                            "No camera app available"
                        }
                    } catch (e: Exception) {
                        Toast.makeText(ctx, "Failed to open camera: ${e.message}", Toast.LENGTH_SHORT).show()
                        "Failed to open camera: ${e.message}"
                    }
                },
                showInChat = true,
                response = "Opening camera"
            ),
            AppAction(
                id = "open_developer_settings",
                descriptions =
                listOf("Open developer settings", "Go to developer options", "Turn on USB debugging"),
                action = { ctx, _ ->
                    val intent =
                        Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    ctx.startActivity(intent)
                    null
                }
            ),
            AppAction(
                id = "open_wifi_settings",
                descriptions = listOf("Open Wi-Fi settings", "Connect to Wi-Fi", "Show network settings"),
                action = { ctx, _ ->
                    val intent =
                        Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    ctx.startActivity(intent)
                    null
                }
            ),
            AppAction(
                id = "open_bluetooth_settings",
                descriptions =
                listOf("Open Bluetooth settings", "Connect a Bluetooth device", "Show Bluetooth devices"),
                action = { ctx, _ ->
                    val intent =
                        Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    ctx.startActivity(intent)
                    null
                }
            ),
            AppAction(
                id = "open_display_settings",
                descriptions = listOf("Open display settings", "Change screen brightness", "Adjust display"),
                action = { ctx, _ ->
                    val intent =
                        Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    ctx.startActivity(intent)
                    null
                }
            ),
            AppAction(
                id = "open_application",
                descriptions = listOf("Open app", "Launch app", "Start app"),
                action = { ctx, query ->
                    val pm = ctx.packageManager
                    val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                        addCategory(Intent.CATEGORY_LAUNCHER)
                    }
                    val launchableApps = pm.queryIntentActivities(mainIntent, 0)
                    val sanitizedQuery = query.replace(Regex("['\".,]"), "")

                    val targetApp =
                        launchableApps
                            .map { resolveInfo ->
                                val appName = resolveInfo.loadLabel(pm).toString()
                                Pair(appName, resolveInfo)
                            }
                            .filter { (appName, _) ->
                                sanitizedQuery.contains(appName, ignoreCase = true)
                            }
                            .maxByOrNull { (appName, _) -> appName.length }

                    if (targetApp != null) {
                        val (appName, resolveInfo) = targetApp
                        val appInfo = resolveInfo.activityInfo.applicationInfo
                        val launchIntent = pm.getLaunchIntentForPackage(appInfo.packageName)
                        if (launchIntent != null) {
                            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            ctx.startActivity(launchIntent)
                            "Opening $appName"
                        } else {
                            "Cannot open $appName."
                        }
                    } else {
                        "I couldn't find an app to open in your request."
                    }
                },
                showInChat = true
            )
        )

    if (rearCameraId != null) {
        actions.add(
            AppAction(
                id = "turn_on_flashlight",
                descriptions = listOf("Turn on flashlight", "Enable torch", "Light up"),
                action = { _, _ ->
                    try {
                        cameraManager.setTorchMode(rearCameraId, true)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to turn on flashlight", Toast.LENGTH_SHORT)
                            .show()
                    }
                    null
                },
                showInChat = true,
                response = "Turning on the flashlight"
            )
        )
        actions.add(
            AppAction(
                id = "turn_off_flashlight",
                descriptions = listOf("Turn off flashlight", "Disable torch", "Turn off light"),
                action = { _, _ ->
                    try {
                        cameraManager.setTorchMode(rearCameraId, false)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to turn off flashlight", Toast.LENGTH_SHORT)
                            .show()
                    }
                    null
                },
                showInChat = true,
                response = "Turning off the flashlight"
            )
        )
    }

    return actions
} 