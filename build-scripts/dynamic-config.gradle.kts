// Dynamic configuration script for Android builds
// This script shows how to make build properties dynamic using environment variables

import java.util.Properties
import java.io.FileInputStream

// Load properties from multiple sources
fun loadDynamicProperties(): Properties {
    val properties = Properties()
    
    // 1. Load from local.properties (if exists)
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        properties.load(FileInputStream(localPropertiesFile))
    }
    
    // 2. Load from gradle.properties (if exists)
    val gradlePropertiesFile = rootProject.file("gradle.properties")
    if (gradlePropertiesFile.exists()) {
        properties.load(FileInputStream(gradlePropertiesFile))
    }
    
    // 3. Override with environment variables
    val envVars = mapOf(
        "ANDROID_SDK_ROOT" to System.getenv("ANDROID_SDK_ROOT"),
        "ANDROID_KEYSTORE_PATH" to System.getenv("ANDROID_KEYSTORE_PATH"),
        "ANDROID_KEYSTORE_PASSWORD" to System.getenv("ANDROID_KEYSTORE_PASSWORD"),
        "ANDROID_KEY_ALIAS" to System.getenv("ANDROID_KEY_ALIAS"),
        "ANDROID_KEY_PASSWORD" to System.getenv("ANDROID_KEY_PASSWORD"),
        "GEMINI_API_KEY" to System.getenv("GEMINI_API_KEY"),
        "BUILD_TYPE" to System.getenv("BUILD_TYPE"),
        "BUILD_FLAVOR" to System.getenv("BUILD_FLAVOR"),
        "ENABLE_DEBUG_LOGGING" to System.getenv("ENABLE_DEBUG_LOGGING"),
        "ENABLE_CRASH_REPORTING" to System.getenv("ENABLE_CRASH_REPORTING")
    )
    
    envVars.forEach { (key, value) ->
        if (value != null) {
            properties.setProperty(key, value)
        }
    }
    
    return properties
}

// Extension function to get property with fallback
fun Properties.getPropertyWithFallback(key: String, fallback: String): String {
    return this.getProperty(key, fallback)
}

// Extension function to get boolean property
fun Properties.getBooleanProperty(key: String, fallback: Boolean = false): Boolean {
    return this.getProperty(key, fallback.toString()).toBoolean()
}

// Extension function to get integer property
fun Properties.getIntProperty(key: String, fallback: Int): Int {
    return this.getProperty(key, fallback.toString()).toIntOrNull() ?: fallback
}

// Make properties available to all projects
val dynamicProperties = loadDynamicProperties()
ext["dynamicProperties"] = dynamicProperties

// Print configuration summary (only in debug mode)
if (dynamicProperties.getBooleanProperty("ENABLE_DEBUG_LOGGING", false)) {
    println("=== Dynamic Configuration Summary ===")
    println("SDK Root: ${dynamicProperties.getPropertyWithFallback("ANDROID_SDK_ROOT", "Not set")}")
    println("Keystore Path: ${dynamicProperties.getPropertyWithFallback("ANDROID_KEYSTORE_PATH", "Not set")}")
    println("Build Type: ${dynamicProperties.getPropertyWithFallback("BUILD_TYPE", "debug")}")
    println("Build Flavor: ${dynamicProperties.getPropertyWithFallback("BUILD_FLAVOR", "development")}")
    println("Debug Logging: ${dynamicProperties.getBooleanProperty("ENABLE_DEBUG_LOGGING", false)}")
    println("Crash Reporting: ${dynamicProperties.getBooleanProperty("ENABLE_CRASH_REPORTING", false)}")
    println("=====================================")
} 