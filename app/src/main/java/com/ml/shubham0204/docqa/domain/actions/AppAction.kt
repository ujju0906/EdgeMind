package com.ml.shubham0204.docqa.domain.actions

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import android.net.Uri
import android.os.PowerManager
import android.media.AudioManager
import android.app.ActivityManager
import android.content.pm.ApplicationInfo
import android.util.Log

data class AppAction(
    val id: String,
    val descriptions: List<String>,
    val action: (Context, String) -> String?,
    var embedding: FloatArray? = null,
    val showInChat: Boolean = false,
    val response: String = ""
)

data class InstalledApp(
    val name: String,
    val packageName: String,
    val resolveInfo: android.content.pm.ResolveInfo,
    val keywords: List<String> = emptyList()
)

class AppDiscoveryService(private val context: Context) {
    
    fun getAllInstalledApps(): List<InstalledApp> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        
        val launchableApps = pm.queryIntentActivities(mainIntent, 0)
        val installedApps = mutableListOf<InstalledApp>()
        
        launchableApps.forEach { resolveInfo ->
            try {
                val appName = resolveInfo.loadLabel(pm).toString()
                val packageName = resolveInfo.activityInfo.packageName
                
                // Generate keywords for better matching
                val keywords = generateAppKeywords(appName, packageName)
                
                installedApps.add(
                    InstalledApp(
                        name = appName,
                        packageName = packageName,
                        resolveInfo = resolveInfo,
                        keywords = keywords
                    )
                )
            } catch (e: Exception) {
                Log.w("AppDiscoveryService", "Error processing app: ${e.message}")
            }
        }
        
        return installedApps.sortedBy { it.name.lowercase() }
    }
    
    private fun generateAppKeywords(appName: String, packageName: String): List<String> {
        val keywords = mutableListOf<String>()
        
        // Add the app name itself
        keywords.add(appName.lowercase())
        
        // Add package name parts
        val packageParts = packageName.split(".")
        keywords.addAll(packageParts.filter { it.length > 2 })
        
        // Add common abbreviations and variations
        val commonMappings = mapOf(
            "instagram" to listOf("ig", "insta", "gram"),
            "whatsapp" to listOf("wa", "whats", "whatsapp"),
            "facebook" to listOf("fb", "meta", "facebook"),
            "youtube" to listOf("yt", "tube", "youtube"),
            "twitter" to listOf("x", "tweet", "twitter"),
            "tiktok" to listOf("tik", "tok", "tiktok"),
            "snapchat" to listOf("snap", "snapchat"),
            "telegram" to listOf("tg", "telegram"),
            "discord" to listOf("disc", "discord"),
            "spotify" to listOf("spot", "spotify"),
            "netflix" to listOf("flix", "netflix"),
            "amazon" to listOf("amzn", "amazon"),
            "uber" to listOf("ride", "uber"),
            "maps" to listOf("google maps", "gmap", "maps"),
            "gmail" to listOf("mail", "email", "gmail"),
            "chrome" to listOf("browser", "chrome"),
            "camera" to listOf("cam", "photo", "camera"),
            "gallery" to listOf("photos", "album", "gallery"),
            "calculator" to listOf("calc", "calculator"),
            "calendar" to listOf("cal", "calendar"),
            "clock" to listOf("time", "clock"),
            "notes" to listOf("note", "notes"),
            "files" to listOf("file manager", "files"),
            "settings" to listOf("setting", "settings")
        )
        
        // Add common mappings if app name matches
        commonMappings.forEach { (appKey, variations) ->
            if (appName.lowercase().contains(appKey) || packageName.lowercase().contains(appKey)) {
                keywords.addAll(variations)
            }
        }
        
        // Add word variations (remove spaces, common suffixes)
        val words = appName.split(" ")
        words.forEach { word ->
            if (word.length > 2) {
                keywords.add(word.lowercase())
                // Remove common suffixes
                keywords.add(word.lowercase().removeSuffix("app").removeSuffix("pro").removeSuffix("plus"))
            }
        }
        
        return keywords.distinct()
    }
}

fun getPredefinedActions(context: Context): List<AppAction> {
    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    val rearCameraId =
        cameraManager.cameraIdList.find {
            cameraManager.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING) ==
                CameraCharacteristics.LENS_FACING_BACK
        }

    val actions = mutableListOf<AppAction>()
    
    // ===== SYSTEM CONTROLS =====
    actions.add(
            AppAction(
                id = "open_camera",
            descriptions = listOf("Open camera", "Take a picture", "Launch camera", "Start camera", "Camera", "Take photo", "Take selfie"),
                action = { ctx, _ ->
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    ctx.startActivity(intent)
                "Opening camera"
            },
            showInChat = true
        )
    )
    
    actions.add(
        AppAction(
            id = "open_gallery",
            descriptions = listOf("Open gallery", "View photos", "Open photos", "Photo gallery", "Pictures", "Images"),
            action = { ctx, _ ->
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    type = "image/*"
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                ctx.startActivity(intent)
                "Opening photo gallery"
            },
            showInChat = true
        )
    )
    
    actions.add(
            AppAction(
            id = "open_settings",
            descriptions = listOf("Open settings", "Go to settings", "System settings", "Device settings", "Settings menu"),
                action = { ctx, _ ->
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    ctx.startActivity(intent)
                "Opening settings"
            },
            showInChat = true
        )
    )
    
    actions.add(
            AppAction(
                id = "open_wifi_settings",
            descriptions = listOf("Open Wi-Fi settings", "Connect to Wi-Fi", "Show network settings", "Wi-Fi", "Wireless", "Internet settings"),
                action = { ctx, _ ->
                val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    ctx.startActivity(intent)
                "Opening Wi-Fi settings"
            },
            showInChat = true
        )
    )
    
    actions.add(
        AppAction(
            id = "open_bluetooth_settings",
            descriptions = listOf("Open Bluetooth settings", "Connect a Bluetooth device", "Show Bluetooth devices", "Bluetooth", "BT settings"),
            action = { ctx, _ ->
                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                ctx.startActivity(intent)
                "Opening Bluetooth settings"
            },
            showInChat = true
        )
    )
    
    actions.add(
            AppAction(
            id = "open_display_settings",
            descriptions = listOf("Open display settings", "Change screen brightness", "Adjust display", "Screen settings", "Brightness"),
                action = { ctx, _ ->
                val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    ctx.startActivity(intent)
                "Opening display settings"
            },
            showInChat = true
        )
    )
    
    actions.add(
        AppAction(
            id = "open_sound_settings",
            descriptions = listOf("Open sound settings", "Volume settings", "Audio settings", "Sound", "Volume", "Audio"),
            action = { ctx, _ ->
                val intent = Intent(Settings.ACTION_SOUND_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                ctx.startActivity(intent)
                "Opening sound settings"
            },
            showInChat = true
        )
    )
    
    actions.add(
            AppAction(
            id = "open_battery_settings",
            descriptions = listOf("Open battery settings", "Battery info", "Power settings", "Battery", "Power"),
                action = { ctx, _ ->
                val intent = Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    ctx.startActivity(intent)
                "Opening battery settings"
            },
            showInChat = true
        )
    )
    
    actions.add(
        AppAction(
            id = "open_storage_settings",
            descriptions = listOf("Open storage settings", "Storage info", "Storage", "Memory", "Space"),
            action = { ctx, _ ->
                val intent = Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                ctx.startActivity(intent)
                "Opening storage settings"
            },
            showInChat = true
        )
    )
    
    actions.add(
            AppAction(
            id = "open_apps_settings",
            descriptions = listOf("Open apps settings", "Application settings", "Manage apps", "Apps", "Applications"),
            action = { ctx, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                ctx.startActivity(intent)
                "Opening apps settings"
            },
            showInChat = true
        )
    )
    
    actions.add(
        AppAction(
            id = "open_security_settings",
            descriptions = listOf("Open security settings", "Security", "Lock screen", "Fingerprint", "Face unlock"),
            action = { ctx, _ ->
                val intent = Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                ctx.startActivity(intent)
                "Opening security settings"
            },
            showInChat = true
        )
    )
    
    actions.add(
        AppAction(
            id = "open_developer_settings",
            descriptions = listOf("Open developer settings", "Go to developer options", "Turn on USB debugging", "Developer options", "USB debugging"),
            action = { ctx, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                ctx.startActivity(intent)
                "Opening developer settings"
                },
                showInChat = true
            )
        )

    // ===== DEVICE CONTROLS =====
    if (rearCameraId != null) {
        actions.add(
            AppAction(
                id = "turn_on_flashlight",
                descriptions = listOf("Turn on flashlight", "Enable torch", "Light up", "Flashlight on", "Torch on"),
                action = { _, _ ->
                    try {
                        cameraManager.setTorchMode(rearCameraId, true)
                        "Turning on the flashlight"
                    } catch (e: Exception) {
                        "Failed to turn on flashlight"
                    }
                },
                showInChat = true
            )
        )
        actions.add(
            AppAction(
                id = "turn_off_flashlight",
                descriptions = listOf("Turn off flashlight", "Disable torch", "Turn off light", "Flashlight off", "Torch off"),
                action = { _, _ ->
                    try {
                        cameraManager.setTorchMode(rearCameraId, false)
                        "Turning off the flashlight"
                    } catch (e: Exception) {
                        "Failed to turn off flashlight"
                    }
                },
                showInChat = true
            )
        )
    }
    
    actions.add(
        AppAction(
            id = "increase_volume",
            descriptions = listOf("Increase volume", "Turn up volume", "Volume up", "Louder", "Raise volume"),
            action = { ctx, _ ->
                val audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND)
                "Increasing volume"
            },
            showInChat = true
        )
    )
    
    actions.add(
        AppAction(
            id = "decrease_volume",
            descriptions = listOf("Decrease volume", "Turn down volume", "Volume down", "Quieter", "Lower volume"),
            action = { ctx, _ ->
                val audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND)
                "Decreasing volume"
            },
            showInChat = true
        )
    )
    
    actions.add(
        AppAction(
            id = "mute_volume",
            descriptions = listOf("Mute volume", "Silence", "Mute", "Turn off sound"),
            action = { ctx, _ ->
                val audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audioManager.adjustVolume(AudioManager.ADJUST_MUTE, AudioManager.FLAG_PLAY_SOUND)
                "Muting volume"
            },
            showInChat = true
        )
    )
    
    actions.add(
        AppAction(
            id = "vibrate_mode",
            descriptions = listOf("Vibrate mode", "Vibration", "Vibrate", "Silent mode"),
            action = { ctx, _ ->
                val audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                "Switching to vibrate mode"
            },
            showInChat = true
        )
    )
    
    actions.add(
        AppAction(
            id = "normal_mode",
            descriptions = listOf("Normal mode", "Sound on", "Ring mode", "Normal ring"),
            action = { ctx, _ ->
                val audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                "Switching to normal mode"
            },
            showInChat = true
        )
    )

    // ===== UTILITY ACTIONS =====
    actions.add(
        AppAction(
            id = "open_calculator",
            descriptions = listOf("Open calculator", "Calculator", "Calc", "Math", "Calculate"),
            action = { ctx, _ ->
                try {
                    // Try to open calculator app by package name
                    val calcPackages = listOf("com.android.calculator2", "com.google.android.calculator", "com.sec.android.app.popupcalculator")
                    var opened = false
                    
                    for (packageName in calcPackages) {
                        try {
                            val intent = ctx.packageManager.getLaunchIntentForPackage(packageName)
                            if (intent != null) {
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                ctx.startActivity(intent)
                                opened = true
                                break
                            }
                        } catch (e: Exception) {
                            continue
                        }
                    }
                    
                    if (opened) "Opening calculator" else "Calculator not found"
                } catch (e: Exception) {
                    "Calculator not found"
                }
            },
            showInChat = true
        )
    )
    
    actions.add(
        AppAction(
            id = "open_calendar",
            descriptions = listOf("Open calendar", "Calendar", "Schedule", "Events", "Date"),
            action = { ctx, _ ->
                try {
                    // Try to open calendar app by package name
                    val calendarPackages = listOf("com.google.android.calendar", "com.android.calendar", "com.samsung.android.calendar")
                    var opened = false
                    
                    for (packageName in calendarPackages) {
                        try {
                            val intent = ctx.packageManager.getLaunchIntentForPackage(packageName)
                            if (intent != null) {
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                ctx.startActivity(intent)
                                opened = true
                                break
                            }
                        } catch (e: Exception) {
                            continue
                        }
                    }
                    
                    if (opened) "Opening calendar" else "Calendar not found"
                } catch (e: Exception) {
                    "Calendar not found"
                }
            },
            showInChat = true
        )
    )
    
    actions.add(
        AppAction(
            id = "open_clock",
            descriptions = listOf("Open clock", "Clock", "Time", "Alarm", "Timer"),
            action = { ctx, _ ->
                try {
                    // Try to open clock app by package name
                    val clockPackages = listOf("com.android.deskclock", "com.google.android.deskclock")
                    var opened = false
                    
                    for (packageName in clockPackages) {
                        try {
                            val intent = ctx.packageManager.getLaunchIntentForPackage(packageName)
                            if (intent != null) {
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                ctx.startActivity(intent)
                                opened = true
                                break
                            }
                        } catch (e: Exception) {
                            continue
                        }
                    }
                    
                    if (opened) "Opening clock" else "Clock app not found"
                } catch (e: Exception) {
                    "Clock app not found"
                }
            },
            showInChat = true
        )
    )
    
    actions.add(
        AppAction(
            id = "open_notes",
            descriptions = listOf("Open notes", "Notes", "Notepad", "Text editor", "Write"),
            action = { ctx, _ ->
                try {
                    // Try to open notes app by package name
                    val notesPackages = listOf("com.google.android.keep", "com.samsung.android.app.notes", "com.sec.android.app.memo")
                    var opened = false
                    
                    for (packageName in notesPackages) {
                        try {
                            val intent = ctx.packageManager.getLaunchIntentForPackage(packageName)
                            if (intent != null) {
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                ctx.startActivity(intent)
                                opened = true
                                break
                            }
                        } catch (e: Exception) {
                            continue
                        }
                    }
                    
                    if (opened) "Opening notes" else "Notes app not found"
                } catch (e: Exception) {
                    "Notes app not found"
                }
            },
            showInChat = true
        )
    )
    
    actions.add(
        AppAction(
            id = "open_maps",
            descriptions = listOf("Open maps", "Maps", "Navigation", "GPS", "Location", "Directions"),
            action = { ctx, _ ->
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("geo:0,0?q=")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    ctx.startActivity(intent)
                    "Opening maps"
                } catch (e: Exception) {
                    "Maps app not found"
                }
            },
            showInChat = true
        )
    )
    
    actions.add(
        AppAction(
            id = "open_browser",
            descriptions = listOf("Open browser", "Browser", "Web browser", "Internet", "Web", "Chrome", "Firefox"),
            action = { ctx, _ ->
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://www.google.com")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    ctx.startActivity(intent)
                    "Opening browser"
                } catch (e: Exception) {
                    "Browser not found"
                }
            },
            showInChat = true
        )
    )
    
    actions.add(
        AppAction(
            id = "open_email",
            descriptions = listOf("Open email", "Email", "Gmail", "Mail", "Inbox"),
            action = { ctx, _ ->
                try {
                    // Try to open email app by package name
                    val emailPackages = listOf("com.google.android.gm", "com.android.email", "com.samsung.android.email.provider")
                    var opened = false
                    
                    for (packageName in emailPackages) {
                        try {
                            val intent = ctx.packageManager.getLaunchIntentForPackage(packageName)
                            if (intent != null) {
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                ctx.startActivity(intent)
                                opened = true
                                break
                            }
                        } catch (e: Exception) {
                            continue
                        }
                    }
                    
                    if (opened) "Opening email" else "Email app not found"
                } catch (e: Exception) {
                    "Email app not found"
                }
                },
            showInChat = true
        )
    )

    // ===== SMART APP DISCOVERY =====
    actions.add(
        AppAction(
            id = "open_application",
            descriptions = listOf("Open app", "Launch app", "Start app", "Open", "Launch", "Start", "Run app", "Open application"),
            action = { ctx, query ->
                val appDiscovery = AppDiscoveryService(ctx)
                val installedApps = appDiscovery.getAllInstalledApps()
                
                if (installedApps.isEmpty()) {
                    return@AppAction "No apps found on this device."
                }
                
                // Clean and normalize the query
                val sanitizedQuery = query.lowercase()
                    .replace(Regex("['\".,!?]"), "")
                    .replace(Regex("\\s+"), " ")
                    .trim()
                
                // Extract potential app name from query
                val queryKeywords = sanitizedQuery.split(" ")
                    .filter { it.length > 2 }
                
                // Score each app
                val appCandidates = mutableListOf<Triple<InstalledApp, Float, List<String>>>()
                
                installedApps.forEach { app ->
                    var score = 0f
                    val matchedKeywords = mutableListOf<String>()
                    
                    // Exact name match gets highest score
                    if (sanitizedQuery.contains(app.name.lowercase())) {
                        score += 100f
                        matchedKeywords.add(app.name)
                    }
                    
                    // Check each keyword against app keywords
                    queryKeywords.forEach { queryKeyword ->
                        app.keywords.forEach { appKeyword ->
                            if (appKeyword.contains(queryKeyword) || queryKeyword.contains(appKeyword)) {
                                score += 15f
                                if (!matchedKeywords.contains(appKeyword)) {
                                    matchedKeywords.add(appKeyword)
                                }
                            }
                        }
                    }
                    
                    // Package name matching
                    if (app.packageName.lowercase().contains(sanitizedQuery)) {
                        score += 20f
                    }
                    
                    // Length bonus for longer matches
                    if (app.name.length > 3) {
                        score += app.name.length * 0.1f
                    }
                    
                    if (score > 0) {
                        appCandidates.add(Triple(app, score, matchedKeywords))
                    }
                }
                
                // Sort by score and get the best match
                val bestMatch = appCandidates.maxByOrNull { it.second }
                
                if (bestMatch != null && bestMatch.second >= 10f) {
                    val (app, score, matchedKeywords) = bestMatch
                    val pm = ctx.packageManager
                    val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
                    
                    if (launchIntent != null) {
                        try {
                            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            ctx.startActivity(launchIntent)
                            "Opening ${app.name}"
                        } catch (e: Exception) {
                            "Failed to open ${app.name}: ${e.message}"
                        }
                    } else {
                        "Cannot open ${app.name} (no launch intent available)."
                    }
                } else {
                    // If no good match found, suggest similar apps
                    val suggestions = appCandidates
                        .take(5)
                        .map { it.first.name }
                        .joinToString(", ")
                    
                    if (suggestions.isNotEmpty()) {
                        "I couldn't find an exact match for '${query.trim()}'. Did you mean: $suggestions?"
                    } else {
                        "I couldn't find any apps matching '${query.trim()}'. Please check the app name and try again."
                    }
                }
            },
            showInChat = true
        )
    )
    
    actions.add(
        AppAction(
            id = "list_apps",
            descriptions = listOf("List apps", "Show apps", "What apps do I have", "Installed apps", "App list"),
            action = { ctx, _ ->
                val appDiscovery = AppDiscoveryService(ctx)
                val installedApps = appDiscovery.getAllInstalledApps()
                
                if (installedApps.isEmpty()) {
                    return@AppAction "No apps found on this device."
                }
                
                val appList = installedApps.take(20).joinToString(", ") { it.name }
                "Here are some of your installed apps: $appList"
            },
            showInChat = true
        )
    )

    return actions
} 