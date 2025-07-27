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
import java.util.Locale

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
        
        // Enhanced common abbreviations and variations with more apps
        val commonMappings = mapOf(
            "instagram" to listOf("ig", "insta", "gram", "instagram"),
            "whatsapp" to listOf("wa", "whats", "whatsapp", "whats app"),
            "facebook" to listOf("fb", "meta", "facebook", "face book"),
            "youtube" to listOf("yt", "tube", "youtube", "you tube"),
            "twitter" to listOf("x", "tweet", "twitter", "x app"),
            "tiktok" to listOf("tik", "tok", "tiktok", "tik tok"),
            "snapchat" to listOf("snap", "snapchat", "snap chat"),
            "telegram" to listOf("tg", "telegram", "tele gram"),
            "discord" to listOf("disc", "discord", "dis cord"),
            "spotify" to listOf("spot", "spotify", "spot ify"),
            "netflix" to listOf("flix", "netflix", "net flix"),
            "amazon" to listOf("amzn", "amazon", "amazon app"),
            "uber" to listOf("ride", "uber", "uber app"),
            "maps" to listOf("google maps", "gmap", "maps", "google map", "navigation"),
            "gmail" to listOf("mail", "email", "gmail", "g mail", "google mail"),
            "chrome" to listOf("browser", "chrome", "google chrome", "web browser", "internet"),
            "firefox" to listOf("firefox", "fire fox", "mozilla", "browser"),
            "safari" to listOf("safari", "apple browser", "browser"),
            "camera" to listOf("cam", "photo", "camera", "photography"),
            "gallery" to listOf("photos", "album", "gallery", "photo gallery", "pictures"),
            "calculator" to listOf("calc", "calculator", "math", "calculate"),
            "calendar" to listOf("cal", "calendar", "schedule", "events"),
            "clock" to listOf("time", "clock", "alarm", "timer", "stopwatch"),
            "notes" to listOf("note", "notes", "notepad", "text editor", "memo"),
            "files" to listOf("file manager", "files", "file explorer", "storage"),
            "settings" to listOf("setting", "settings", "config", "preferences"),
            "phone" to listOf("phone", "dialer", "call", "contacts"),
            "messages" to listOf("sms", "text", "message", "messaging"),
            "contacts" to listOf("contact", "people", "address book"),
            "music" to listOf("music", "player", "audio", "songs"),
            "video" to listOf("video", "movies", "media player"),
            "games" to listOf("game", "gaming", "play"),
            "weather" to listOf("weather", "forecast", "climate"),
            "news" to listOf("news", "headlines", "current events"),
            "shopping" to listOf("shop", "buy", "purchase", "store"),
            "banking" to listOf("bank", "finance", "money", "account"),
            "health" to listOf("fitness", "workout", "exercise", "health"),
            "food" to listOf("restaurant", "delivery", "food", "dining"),
            "travel" to listOf("booking", "hotel", "flight", "travel"),
            "education" to listOf("learn", "study", "course", "education"),
            "social" to listOf("social media", "social", "connect"),
            "entertainment" to listOf("entertainment", "fun", "leisure"),
            "productivity" to listOf("work", "office", "productivity", "business"),
            "utilities" to listOf("tool", "utility", "helper", "assistant")
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
                keywords.add(word.lowercase().removeSuffix("app").removeSuffix("pro").removeSuffix("plus").removeSuffix("lite"))
            }
        }
        
        // Add category-based keywords
        val categoryKeywords = getCategoryKeywords(appName, packageName)
        keywords.addAll(categoryKeywords)
        
        return keywords.distinct()
    }
    
    private fun getCategoryKeywords(appName: String, packageName: String): List<String> {
        val appNameLower = appName.lowercase()
        val packageNameLower = packageName.lowercase()
        
        return when {
            // Social Media
            appNameLower.contains("instagram") || appNameLower.contains("facebook") || 
            appNameLower.contains("twitter") || appNameLower.contains("tiktok") ||
            appNameLower.contains("snapchat") || appNameLower.contains("telegram") ||
            appNameLower.contains("discord") || appNameLower.contains("whatsapp") ||
            appNameLower.contains("linkedin") || appNameLower.contains("reddit") ->
                listOf("social", "social media", "connect", "share", "post")
            
            // Communication
            appNameLower.contains("whatsapp") || appNameLower.contains("telegram") ||
            appNameLower.contains("discord") || appNameLower.contains("signal") ||
            appNameLower.contains("skype") || appNameLower.contains("zoom") ->
                listOf("chat", "message", "call", "communication", "talk")
            
            // Entertainment
            appNameLower.contains("youtube") || appNameLower.contains("netflix") ||
            appNameLower.contains("spotify") || appNameLower.contains("tiktok") ||
            appNameLower.contains("twitch") || appNameLower.contains("disney") ->
                listOf("entertainment", "fun", "watch", "listen", "stream")
            
            // Productivity
            appNameLower.contains("chrome") || appNameLower.contains("firefox") ||
            appNameLower.contains("gmail") || appNameLower.contains("drive") ||
            appNameLower.contains("docs") || appNameLower.contains("sheets") ||
            appNameLower.contains("slides") || appNameLower.contains("keep") ->
                listOf("work", "productivity", "office", "business", "tool")
            
            // Navigation
            appNameLower.contains("maps") || appNameLower.contains("waze") ||
            appNameLower.contains("uber") || appNameLower.contains("lyft") ->
                listOf("navigation", "travel", "location", "directions", "transport")
            
            // Shopping
            appNameLower.contains("amazon") || appNameLower.contains("ebay") ||
            appNameLower.contains("shop") || appNameLower.contains("store") ->
                listOf("shopping", "buy", "purchase", "shop", "store")
            
            // Banking/Finance
            appNameLower.contains("bank") || appNameLower.contains("pay") ||
            appNameLower.contains("money") || appNameLower.contains("finance") ->
                listOf("banking", "finance", "money", "payment", "account")
            
            // Health/Fitness
            appNameLower.contains("fitness") || appNameLower.contains("health") ||
            appNameLower.contains("workout") || appNameLower.contains("exercise") ->
                listOf("health", "fitness", "workout", "exercise", "wellness")
            
            // Food/Dining
            appNameLower.contains("food") || appNameLower.contains("restaurant") ||
            appNameLower.contains("delivery") || appNameLower.contains("dining") ->
                listOf("food", "restaurant", "delivery", "dining", "eat")
            
            // Games
            appNameLower.contains("game") || packageNameLower.contains("game") ->
                listOf("game", "gaming", "play", "entertainment")
            
            // Utilities
            appNameLower.contains("camera") || appNameLower.contains("gallery") ||
            appNameLower.contains("calculator") || appNameLower.contains("clock") ||
            appNameLower.contains("notes") || appNameLower.contains("files") ->
                listOf("utility", "tool", "helper", "system")
            
            else -> emptyList()
        }
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
                    try {
                        // Check camera permission
                        if (ctx.checkSelfPermission(android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            return@AppAction "PERMISSION_REQUEST:CAMERA"
                        }
                        
                        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        ctx.startActivity(intent)
                        "📸 Opening camera"
                    } catch (e: Exception) {
                        "📸 Failed to open camera: ${e.message}"
                    }
            },
            showInChat = true
        )
    )
    
    actions.add(
        AppAction(
            id = "open_gallery",
            descriptions = listOf("Open gallery", "View photos", "Open photos", "Photo gallery", "Pictures", "Images"),
            action = { ctx, _ ->
                try {
                    // Check media permissions for Android 15+
                    val hasMediaPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        // Android 13+ uses READ_MEDIA_IMAGES
                        ctx.checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    } else {
                        // Android 12 and below use READ_EXTERNAL_STORAGE
                        ctx.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    }
                    
                    if (!hasMediaPermission) {
                        return@AppAction "PERMISSION_REQUEST:MEDIA"
                    }
                    
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        type = "image/*"
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    ctx.startActivity(intent)
                    "🖼️ Opening photo gallery"
                } catch (e: Exception) {
                    "🖼️ Gallery app not found"
                }
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
            descriptions = listOf("Open WiFi settings", "Connect to WiFi", "Show network settings", "WiFi", "Wireless", "Internet settings"),
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
            id = "open_app_permissions",
            descriptions = listOf("Open app permissions", "App permissions", "Permissions", "Grant permissions", "Camera permission", "Allow camera"),
            action = { ctx, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", ctx.packageName, null)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    ctx.startActivity(intent)
                    "📱 Opening app permissions. Please enable Camera permission for flashlight to work."
                } catch (e: Exception) {
                    "📱 Go to Settings > Apps > DocQA > Permissions > Camera > Allow"
                }
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

    // ===== BASIC ANDROID APPS =====
    // Note: SMS and Phone call actions removed to prevent interference with RAG functionality
    
    // Note: Contacts action removed to prevent interference with RAG functionality
    
    actions.add(
        AppAction(
            id = "open_music",
            descriptions = listOf("Open music", "Music player", "Play music", "Songs", "Audio", "Music app", "Listen to music"),
            action = { ctx, _ ->
                try {
                    // Try to open music app by package name
                    val musicPackages = listOf("com.android.music", "com.google.android.music", "com.samsung.android.music", "com.spotify.music")
                    var opened = false
                    
                    for (packageName in musicPackages) {
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
                    
                    if (opened) "🎵 Opening music player" else "Music app not found"
                } catch (e: Exception) {
                    "Music app not found"
                }
            },
            showInChat = true
        )
    )
    
    actions.add(
        AppAction(
            id = "open_video",
            descriptions = listOf("Open video", "Video player", "Play video", "Movies", "Media player", "Watch video"),
            action = { ctx, _ ->
                try {
                    // Try to open video app by package name
                    val videoPackages = listOf("com.android.video", "com.google.android.videos", "com.samsung.android.video", "com.netflix.mediaclient")
                    var opened = false
                    
                    for (packageName in videoPackages) {
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
                    
                    if (opened) "🎬 Opening video player" else "Video app not found"
                } catch (e: Exception) {
                    "Video app not found"
                }
            },
            showInChat = true
        )
    )
    
    actions.add(
        AppAction(
            id = "open_weather",
            descriptions = listOf("Open weather", "Weather app", "Check weather", "Forecast", "Weather forecast", "Temperature"),
            action = { ctx, _ ->
                try {
                    // Try to open weather app by package name
                    val weatherPackages = listOf("com.android.weather", "com.google.android.weather", "com.samsung.android.weather", "com.accuweather.android")
                    var opened = false
                    
                    for (packageName in weatherPackages) {
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
                    
                    if (opened) "🌤️ Opening weather app" else "Weather app not found"
                } catch (e: Exception) {
                    "Weather app not found"
                }
            },
            showInChat = true
        )
    )
    
    actions.add(
        AppAction(
            id = "open_play_store",
            descriptions = listOf("Open play store", "Play store", "Google play", "App store", "Download apps", "Install apps"),
            action = { ctx, _ ->
                try {
                    // Try to open Play Store
                    val playStoreIntent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("market://")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    ctx.startActivity(playStoreIntent)
                    "🛒 Opening Google Play Store"
                } catch (e: Exception) {
                    "Play Store not found"
                }
            },
            showInChat = true
        )
    )

    // ===== DEVICE CONTROLS =====
    actions.add(
        AppAction(
            id = "turn_on_flashlight",
            descriptions = listOf("Turn on flashlight", "Enable torch", "Light up", "Flashlight on", "Torch on", "Lumos", "Lumos Maxima", "Illuminate", "Cast Lumos", "Lumos spell", "Light my wand"),
            action = { ctx, _ ->
                try {
                    // Check camera permission
                    if (ctx.checkSelfPermission(android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        return@AppAction "PERMISSION_REQUEST:CAMERA"
                    }
                    
                    val cameraManager = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                    
                    // Check if camera list is available
                    if (cameraManager.cameraIdList.isEmpty()) {
                        return@AppAction "🪄 No cameras found on this device"
                    }
                    
                    val rearCameraId = cameraManager.cameraIdList.find { cameraId ->
                        try {
                            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                            facing == CameraCharacteristics.LENS_FACING_BACK
                        } catch (e: Exception) {
                            false
                        }
                    }
                    
                    if (rearCameraId != null) {
                        // Check if flash is available
                        val characteristics = cameraManager.getCameraCharacteristics(rearCameraId)
                        val flashAvailable = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)
                        
                        if (flashAvailable == true) {
                            cameraManager.setTorchMode(rearCameraId, true)
                            "🪄 Lumos! The flashlight is now lit like a wand!"
                        } else {
                            "🪄 This device doesn't have a flash for Lumos spell"
                        }
                    } else {
                        "🪄 No rear camera found for Lumos spell"
                    }
                } catch (e: Exception) {
                    "🪄 Failed to cast Lumos spell: ${e.message}"
                }
            },
            showInChat = true
        )
    )
    
    actions.add(
        AppAction(
            id = "turn_off_flashlight",
            descriptions = listOf("Turn off flashlight", "Disable torch", "Turn off light", "Flashlight off", "Torch off", "Nox", "Extinguish", "Darkness", "Cast Nox", "Nox spell", "Extinguish my wand"),
            action = { ctx, _ ->
                try {
                    // Check camera permission
                    if (ctx.checkSelfPermission(android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        return@AppAction "PERMISSION_REQUEST:CAMERA"
                    }
                    
                    val cameraManager = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                    
                    // Check if camera list is available
                    if (cameraManager.cameraIdList.isEmpty()) {
                        return@AppAction "🪄 No cameras found on this device"
                    }
                    
                    val rearCameraId = cameraManager.cameraIdList.find { cameraId ->
                        try {
                            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                            facing == CameraCharacteristics.LENS_FACING_BACK
                        } catch (e: Exception) {
                            false
                        }
                    }
                    
                    if (rearCameraId != null) {
                        // Check if flash is available
                        val characteristics = cameraManager.getCameraCharacteristics(rearCameraId)
                        val flashAvailable = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)
                        
                        if (flashAvailable == true) {
                            cameraManager.setTorchMode(rearCameraId, false)
                            "🪄 Nox! The light has been extinguished."
                        } else {
                            "🪄 This device doesn't have a flash for Nox spell"
                        }
                    } else {
                        "🪄 No rear camera found for Nox spell"
                    }
                } catch (e: Exception) {
                    "🪄 Failed to cast Nox spell: ${e.message}"
                }
            },
            showInChat = true
        )
    )
    
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
    
    // ===== FUN & MAGICAL ACTIONS =====
    actions.add(
        AppAction(
            id = "magical_actions",
            descriptions = listOf("Wingardium Leviosa", "Expecto Patronum", "Alohomora", "Accio", "Expelliarmus", "Stupefy", "Protego", "Incendio", "Aguamenti", "Reparo", "Finite Incantatem", "Magical spells"),
            action = { _, query ->
                val spell = query.lowercase().trim()
                when {
                    spell.contains("wingardium leviosa") -> "🪄 Wingardium Leviosa! Objects are now floating around you!"
                    spell.contains("expecto patronum") -> "🦌 Expecto Patronum! Your patronus is protecting you!"
                    spell.contains("alohomora") -> "🔓 Alohomora! All locks have been magically opened!"
                    spell.contains("accio") -> "📱 Accio! Your phone is now summoned to your hand!"
                    spell.contains("expelliarmus") -> "⚡ Expelliarmus! All weapons have been disarmed!"
                    spell.contains("stupefy") -> "💫 Stupefy! Everything around you is stunned!"
                    spell.contains("protego") -> "🛡️ Protego! A magical shield is protecting you!"
                    spell.contains("incendio") -> "🔥 Incendio! Fire is blazing around you!"
                    spell.contains("aguamenti") -> "💧 Aguamenti! Water is flowing everywhere!"
                    spell.contains("reparo") -> "🔧 Reparo! Everything broken is now fixed!"
                    spell.contains("finite incantatem") -> "✨ Finite Incantatem! All spells have been cancelled!"
                    else -> "🪄 That's not a spell I recognize. Try: Lumos (flashlight on), Nox (flashlight off), Wingardium Leviosa, or Expecto Patronum!"
                }
            },
            showInChat = true
        )
    )
    
    actions.add(
        AppAction(
            id = "fun_commands",
            descriptions = listOf("Make it rain", "Dance mode", "Party time", "Celebration", "Confetti", "Fireworks", "Magic", "Abracadabra", "Hocus Pocus", "Bibbidi Bobbidi Boo"),
            action = { _, query ->
                val command = query.lowercase().trim()
                when {
                    command.contains("make it rain") -> "🌧️ Make it rain! Virtual money is falling from the sky!"
                    command.contains("dance mode") -> "💃 Dance mode activated! Time to bust some moves!"
                    command.contains("party time") -> "🎉 Party time! Let's celebrate!"
                    command.contains("celebration") -> "🎊 Celebration mode! Everything is festive!"
                    command.contains("confetti") -> "🎊 Confetti explosion! Colorful paper everywhere!"
                    command.contains("fireworks") -> "🎆 Fireworks display! The sky is lit up!"
                    command.contains("magic") -> "✨ Magic is happening all around you!"
                    command.contains("abracadabra") -> "🔮 Abracadabra! Your wish has been granted!"
                    command.contains("hocus pocus") -> "🧙‍♀️ Hocus Pocus! Something magical is happening!"
                    command.contains("bibbidi bobbidi boo") -> "👑 Bibbidi Bobbidi Boo! You're now a princess/prince!"
                    else -> "🎭 Fun command activated! Magic is in the air!"
                }
            },
            showInChat = true
        )
    )

    // ===== UTILITY ACTIONS =====
    actions.add(
        AppAction(
            id = "open_calculator",
            descriptions = listOf("Open calculator", "Calculator", "Calc", "Math", "Calculate", "Math calculator"),
            action = { ctx, _ ->
                try {
                    // Try to open calculator app by package name
                    val calcPackages = listOf("com.android.calculator2", "com.google.android.calculator", "com.sec.android.app.popupcalculator", "com.samsung.android.calculator")
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
                    
                    if (opened) "🧮 Opening calculator" else "Calculator not found"
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
            descriptions = listOf("Open calendar", "Calendar", "Schedule", "Events", "Date", "Planner", "Agenda"),
            action = { ctx, _ ->
                try {
                    // Try to open calendar app by package name
                    val calendarPackages = listOf("com.google.android.calendar", "com.android.calendar", "com.samsung.android.calendar", "com.android.providers.calendar")
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
                    
                    if (opened) "📅 Opening calendar" else "Calendar not found"
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
            id = "set_alarm",
            descriptions = listOf("Set alarm", "Wake me up", "Alarm for", "Set timer", "Remind me", "Wake up", "Set reminder"),
            action = { ctx, query ->
                try {
                    // Enhanced time parsing with more patterns
                    val timePatterns = listOf(
                        Regex("""(\d{1,2})(?::(\d{2}))?\s*(am|pm|AM|PM)?"""), // 5:30 PM, 5 PM, 17:30
                        Regex("""(\d{1,2})\s*(am|pm|AM|PM)"""), // 5 AM, 5 PM
                        Regex("""(\d{1,2}):(\d{2})"""), // 17:30, 5:30
                        Regex("""at\s+(\d{1,2})(?::(\d{2}))?\s*(am|pm|AM|PM)?"""), // at 5 PM, at 5:30 AM
                        Regex("""for\s+(\d{1,2})(?::(\d{2}))?\s*(am|pm|AM|PM)?""") // for 5 PM, for 5:30 AM
                    )
                    
                    var hour = -1
                    var minute = 0
                    var amPm = ""
                    
                    // Try each pattern
                    for (pattern in timePatterns) {
                        val match = pattern.find(query)
                        if (match != null) {
                            hour = match.groupValues[1].toInt()
                            minute = match.groupValues[2].toIntOrNull() ?: 0
                            amPm = match.groupValues[3].lowercase()
                            break
                        }
                    }
                    
                    if (hour != -1) {
                        // Convert to 24-hour format
                        if (amPm == "pm" && hour != 12) hour += 12
                        if (amPm == "am" && hour == 12) hour = 0
                        
                        // Validate time
                        if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                            return@AppAction "⏰ Invalid time format. Please use format like '5 PM' or '17:30'"
                        }
                        
                        // Try multiple alarm setting methods
                        var alarmSet = false
                        
                        // Method 1: Try Google Clock with specific intent
                        try {
                            val googleClockIntent = Intent(Intent.ACTION_MAIN).apply {
                                addCategory(Intent.CATEGORY_DEFAULT)
                                setClassName("com.google.android.deskclock", "com.android.deskclock.alarmclock.AlarmClockActivity")
                                putExtra("android.intent.extra.ALARM_HOUR", hour)
                                putExtra("android.intent.extra.ALARM_MINUTES", minute)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            ctx.startActivity(googleClockIntent)
                            alarmSet = true
                        } catch (e: Exception) {
                            // Continue to next method
                        }
                        
                        // Method 2: Try AOSP Clock with specific intent
                        if (!alarmSet) {
                            try {
                                val aospClockIntent = Intent(Intent.ACTION_MAIN).apply {
                                    addCategory(Intent.CATEGORY_DEFAULT)
                                    setClassName("com.android.deskclock", "com.android.deskclock.alarmclock.AlarmClockActivity")
                                    putExtra("android.intent.extra.ALARM_HOUR", hour)
                                    putExtra("android.intent.extra.ALARM_MINUTES", minute)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                ctx.startActivity(aospClockIntent)
                                alarmSet = true
                            } catch (e: Exception) {
                                // Continue to next method
                            }
                        }
                        
                        // Method 3: Try Samsung Clock
                        if (!alarmSet) {
                            try {
                                val samsungClockIntent = Intent(Intent.ACTION_MAIN).apply {
                                    addCategory(Intent.CATEGORY_DEFAULT)
                                    setClassName("com.samsung.android.clock", "com.samsung.android.clock.alarmclock.AlarmClockActivity")
                                    putExtra("android.intent.extra.ALARM_HOUR", hour)
                                    putExtra("android.intent.extra.ALARM_MINUTES", minute)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                ctx.startActivity(samsungClockIntent)
                                alarmSet = true
                            } catch (e: Exception) {
                                // Continue to next method
                            }
                        }
                        
                        // Method 4: Try generic alarm intent
                        if (!alarmSet) {
                            try {
                                val genericAlarmIntent = Intent(Intent.ACTION_MAIN).apply {
                                    addCategory(Intent.CATEGORY_DEFAULT)
                                    putExtra("android.intent.extra.ALARM_HOUR", hour)
                                    putExtra("android.intent.extra.ALARM_MINUTES", minute)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                ctx.startActivity(genericAlarmIntent)
                                alarmSet = true
                            } catch (e: Exception) {
                                // Continue to fallback
                            }
                        }
                        
                        // Method 5: Fallback - Open clock app with time info
                        if (!alarmSet) {
                            val clockPackages = listOf(
                                "com.google.android.deskclock",
                                "com.android.deskclock", 
                                "com.samsung.android.clock",
                                "com.sec.android.app.clock"
                            )
                            
                            var clockOpened = false
                            for (packageName in clockPackages) {
                                try {
                                    val clockIntent = ctx.packageManager.getLaunchIntentForPackage(packageName)
                                    if (clockIntent != null) {
                                        clockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        ctx.startActivity(clockIntent)
                                        clockOpened = true
                                        break
                                    }
                                } catch (e: Exception) {
                                    continue
                                }
                            }
                            
                            if (clockOpened) {
                                val timeStr = String.format("%02d:%02d", hour, minute)
                                return@AppAction "⏰ Opening clock app. Please set alarm for $timeStr manually."
                            } else {
                                return@AppAction "⏰ No clock app found. Please install a clock app to set alarms."
                            }
                        }
                        
                        val timeStr = String.format("%02d:%02d", hour, minute)
                        "⏰ Alarm set for $timeStr! The clock app should open with the alarm configured."
                        
                    } else {
                        // No time found, open clock app
                        val clockPackages = listOf(
                            "com.google.android.deskclock",
                            "com.android.deskclock", 
                            "com.samsung.android.clock",
                            "com.sec.android.app.clock"
                        )
                        
                        var clockOpened = false
                        for (packageName in clockPackages) {
                            try {
                                val clockIntent = ctx.packageManager.getLaunchIntentForPackage(packageName)
                                if (clockIntent != null) {
                                    clockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    ctx.startActivity(clockIntent)
                                    clockOpened = true
                                    break
                                }
                            } catch (e: Exception) {
                                continue
                            }
                        }
                        
                        if (clockOpened) {
                            "⏰ Opening clock app. Please specify a time like 'Set alarm for 5 PM' or 'Wake me up at 7:30 AM'"
                        } else {
                            "⏰ No clock app found. Please install a clock app to set alarms."
                        }
                    }
                } catch (e: Exception) {
                    "⏰ Failed to set alarm: ${e.message}"
                }
            },
            showInChat = true
        )
    )
    
    actions.add(
        AppAction(
            id = "open_notes",
            descriptions = listOf("Open notes", "Notes", "Notepad", "Text editor", "Write", "Memo", "Sticky notes"),
            action = { ctx, _ ->
                try {
                    // Try to open notes app by package name
                    val notesPackages = listOf("com.google.android.keep", "com.samsung.android.app.notes", "com.sec.android.app.memo", "com.android.notes")
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
                    
                    if (opened) "📝 Opening notes" else "Notes app not found"
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
            descriptions = listOf("Open maps", "Maps", "Navigation", "GPS", "Location", "Directions", "Google maps"),
            action = { ctx, _ ->
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("geo:0,0?q=")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    ctx.startActivity(intent)
                    "🗺️ Opening maps"
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
            descriptions = listOf("Open browser", "Browser", "Web browser", "Internet", "Web", "Chrome", "Firefox", "Safari"),
            action = { ctx, _ ->
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://www.google.com")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    ctx.startActivity(intent)
                    "🌐 Opening browser"
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
            descriptions = listOf("Open email", "Email", "Gmail", "Mail", "Inbox", "Check email", "Send email"),
            action = { ctx, _ ->
                try {
                    // Try to open email app by package name
                    val emailPackages = listOf("com.google.android.gm", "com.android.email", "com.samsung.android.email.provider", "com.android.mail")
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
                    
                    if (opened) "📧 Opening email" else "Email app not found"
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
                            
                            // Provide additional suggestions for similar apps
                            val similarApps = getSimilarApps(app, installedApps, 3)
                            val response = if (similarApps.isNotEmpty()) {
                                "Opening ${app.name}. Similar apps you might like: ${similarApps.joinToString(", ")}"
                            } else {
                                "Opening ${app.name}"
                            }
                            response
                        } catch (e: Exception) {
                            "Failed to open ${app.name}: ${e.message}"
                        }
                    } else {
                        "Cannot open ${app.name} (no launch intent available)."
                    }
                } else {
                    // If no good match found, suggest similar apps and categories
                    val suggestions = appCandidates
                        .take(5)
                        .map { it.first.name }
                        .joinToString(", ")
                    
                    val categorySuggestions = getCategorySuggestions(sanitizedQuery, installedApps)
                    
                    val response = buildString {
                        append("I couldn't find an exact match for '${query.trim()}'.")
                        if (suggestions.isNotEmpty()) {
                            append(" Did you mean: $suggestions?")
                        }
                        if (categorySuggestions.isNotEmpty()) {
                            append(" Or try these categories: $categorySuggestions")
                        }
                        if (suggestions.isEmpty() && categorySuggestions.isEmpty()) {
                            append(" Please check the app name and try again.")
                        }
                    }
                    response
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
    
    actions.add(
        AppAction(
            id = "find_apps_by_category",
            descriptions = listOf("Find social apps", "Show entertainment apps", "List productivity apps", "Find games", "Show communication apps", "Find shopping apps", "List health apps", "Show navigation apps", "Find banking apps", "List utility apps"),
            action = { ctx, query ->
                val appDiscovery = AppDiscoveryService(ctx)
                val installedApps = appDiscovery.getAllInstalledApps()
                
                if (installedApps.isEmpty()) {
                    return@AppAction "No apps found on this device."
                }
                
                val queryLower = query.lowercase()
                val categoryKeywords = mapOf(
                    "social" to listOf("social", "connect", "share", "post", "follow", "instagram", "facebook", "twitter", "tiktok"),
                    "communication" to listOf("chat", "message", "call", "talk", "video call", "whatsapp", "telegram", "discord"),
                    "entertainment" to listOf("watch", "listen", "stream", "play", "fun", "youtube", "netflix", "spotify"),
                    "productivity" to listOf("work", "office", "business", "tool", "productivity", "chrome", "gmail", "drive"),
                    "navigation" to listOf("map", "location", "directions", "travel", "transport", "maps", "uber"),
                    "shopping" to listOf("shop", "buy", "purchase", "store", "market", "amazon", "ebay"),
                    "banking" to listOf("bank", "finance", "money", "payment", "account"),
                    "health" to listOf("fitness", "health", "workout", "exercise", "wellness"),
                    "food" to listOf("food", "restaurant", "delivery", "dining", "eat"),
                    "games" to listOf("game", "gaming", "play", "entertainment"),
                    "utilities" to listOf("tool", "utility", "helper", "system", "assistant", "camera", "calculator")
                )
                
                val matchingCategory = categoryKeywords.entries.find { (category, keywords) ->
                    keywords.any { keyword -> queryLower.contains(keyword) }
                }
                
                if (matchingCategory != null) {
                    val (category, keywords) = matchingCategory
                    val categoryApps = installedApps.filter { app ->
                        app.keywords.any { appKeyword ->
                            keywords.any { keyword -> appKeyword.contains(keyword) }
                        }
                    }.take(10)
                    
                    if (categoryApps.isNotEmpty()) {
                        val appList = categoryApps.joinToString(", ") { it.name }
                        "Here are your ${category.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(
                                Locale.getDefault()
                            ) else it.toString()
                        }} apps: $appList"
                    } else {
                        "No ${category} apps found on your device."
                    }
                } else {
                    "I couldn't identify a specific category. Try saying 'Find social apps', 'Show entertainment apps', or 'List productivity apps'."
                }
            },
            showInChat = true
        )
    )

    return actions
}

// Helper functions for app discovery
private fun getSimilarApps(targetApp: InstalledApp, allApps: List<InstalledApp>, maxCount: Int): List<String> {
    val targetKeywords = targetApp.keywords.toSet()
    val similarApps = mutableListOf<Pair<InstalledApp, Int>>()
    
    allApps.forEach { app ->
        if (app.packageName != targetApp.packageName) {
            val commonKeywords = targetKeywords.intersect(app.keywords.toSet())
            if (commonKeywords.isNotEmpty()) {
                similarApps.add(Pair(app, commonKeywords.size))
            }
        }
    }
    
    return similarApps
        .sortedByDescending { pair -> pair.second }
        .take(maxCount)
        .map { pair -> pair.first.name }
}

private fun getCategorySuggestions(query: String, allApps: List<InstalledApp>): List<String> {
    val queryLower = query.lowercase()
    val categories = mutableMapOf<String, Int>()
    
    // Define category keywords
    val categoryMappings = mapOf(
        "social" to listOf("social", "connect", "share", "post", "follow"),
        "communication" to listOf("chat", "message", "call", "talk", "video call"),
        "entertainment" to listOf("watch", "listen", "stream", "play", "fun"),
        "productivity" to listOf("work", "office", "business", "tool", "productivity"),
        "navigation" to listOf("map", "location", "directions", "travel", "transport"),
        "shopping" to listOf("shop", "buy", "purchase", "store", "market"),
        "banking" to listOf("bank", "finance", "money", "payment", "account"),
        "health" to listOf("fitness", "health", "workout", "exercise", "wellness"),
        "food" to listOf("food", "restaurant", "delivery", "dining", "eat"),
        "games" to listOf("game", "gaming", "play", "entertainment"),
        "utilities" to listOf("tool", "utility", "helper", "system", "assistant")
    )
    
    // Count apps in each category that might match the query
    categoryMappings.forEach { (category, keywords) ->
        var count = 0
        keywords.forEach { keyword ->
            if (queryLower.contains(keyword)) {
                count += allApps.count { app ->
                    app.keywords.any { it.contains(keyword) }
                }
            }
        }
        if (count > 0) {
            categories[category] = count
        }
    }
    
    return categories
        .entries
        .sortedByDescending { entry -> entry.value }
        .take(3)
        .map { entry -> entry.key.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } }
} 