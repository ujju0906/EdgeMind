# ProGuard rules for DocQA Android App
# This file contains rules to prevent crashes during code obfuscation

# Keep line number information for debugging stack traces
-keepattributes SourceFile,LineNumberTable

# Hide the original source file name
-renamesourcefileattribute SourceFile

# ============================================================================
# ANDROID & KOTLIN RULES
# ============================================================================

# Keep Android components
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.preference.Preference
-keep public class * extends android.view.View
-keep public class * extends android.app.Fragment

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings {
    <fields>;
}

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ============================================================================
# COMPOSE RULES
# ============================================================================

# Keep Compose components
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** { *; }

# Keep Compose Material3
-keep class androidx.compose.material3.** { *; }
-keepclassmembers class androidx.compose.material3.** { *; }

# Keep Compose Navigation
-keep class androidx.navigation.** { *; }
-keepclassmembers class androidx.navigation.** { *; }

# ============================================================================
# OBJECTBOX RULES
# ============================================================================

# Keep ObjectBox classes
-keep class io.objectbox.** { *; }
-keepclassmembers class io.objectbox.** { *; }

# Keep ObjectBox generated classes
-keep class * extends io.objectbox.annotation.Entity { *; }
-keep class * extends io.objectbox.annotation.Id { *; }
-keep class * extends io.objectbox.annotation.Index { *; }
-keep class * extends io.objectbox.annotation.NameInDb { *; }
-keep class * extends io.objectbox.annotation.Property { *; }
-keep class * extends io.objectbox.annotation.Relation { *; }
-keep class * extends io.objectbox.annotation.Transient { *; }
-keep class * extends io.objectbox.annotation.Unique { *; }

# Keep ObjectBox generated classes with specific patterns
-keep class * extends io.objectbox.Box { *; }
-keep class * extends io.objectbox.BoxStore { *; }
-keep class * extends io.objectbox.Transaction { *; }

# ============================================================================
# ONNX RUNTIME RULES
# ============================================================================

# Keep ONNX Runtime classes
-keep class com.microsoft.onnxruntime.** { *; }
-keepclassmembers class com.microsoft.onnxruntime.** { *; }

# Keep ONNX model files
-keep class **.onnx { *; }

# ============================================================================
# APACHE POI RULES
# ============================================================================

# Keep Apache POI classes
-keep class org.apache.poi.** { *; }
-keepclassmembers class org.apache.poi.** { *; }

# Keep POI OOXML classes
-keep class org.apache.poi.xssf.** { *; }
-keep class org.apache.poi.xwpf.** { *; }
-keepclassmembers class org.apache.poi.xssf.** { *; }
-keepclassmembers class org.apache.poi.xwpf.** { *; }

# ============================================================================
# ITEXTPDF RULES
# ============================================================================

# Keep iTextPDF classes
-keep class com.itextpdf.** { *; }
-keepclassmembers class com.itextpdf.** { *; }

# ============================================================================
# GEMINI AI RULES
# ============================================================================

# Keep Gemini AI SDK classes
-keep class com.google.ai.client.generativeai.** { *; }
-keepclassmembers class com.google.ai.client.generativeai.** { *; }

# Keep Gemini model classes
-keep class com.google.ai.client.generativeai.type.** { *; }
-keepclassmembers class com.google.ai.client.generativeai.type.** { *; }

# ============================================================================
# MEDIAPIPE RULES
# ============================================================================

# Keep MediaPipe classes
-keep class com.google.mediapipe.** { *; }
-keepclassmembers class com.google.mediapipe.** { *; }

# Keep MediaPipe LLM classes
-keep class com.google.mediapipe.tasks.genai.** { *; }
-keepclassmembers class com.google.mediapipe.tasks.genai.** { *; }

# ============================================================================
# KOIN RULES
# ============================================================================

# Keep Koin classes
-keep class io.insert-koin.** { *; }
-keepclassmembers class io.insert-koin.** { *; }

# Keep Koin annotations
-keep @io.insert-koin.annotation.KoinApiMarker class * { *; }
-keepclassmembers @io.insert-koin.annotation.KoinApiMarker class * { *; }

# Keep Koin generated classes
-keep class * extends io.insert-koin.core.component.Component { *; }
-keep class * extends io.insert-koin.core.component.KoinComponent { *; }

# ============================================================================
# COMPOSE MARKDOWN RULES
# ============================================================================

# Keep Compose Markdown classes
-keep class com.github.jeziellago.compose_markdown.** { *; }
-keepclassmembers class com.github.jeziellago.compose_markdown.** { *; }

# ============================================================================
# CUSTOM AAR RULES
# ============================================================================

# Keep custom sentence embeddings AAR classes
-keep class com.github.shubham0204.** { *; }
-keepclassmembers class com.github.shubham0204.** { *; }

# Keep any classes in the custom AAR
-keep class sentence_embeddings.** { *; }
-keepclassmembers class sentence_embeddings.** { *; }

# ============================================================================
# WORKMANAGER RULES
# ============================================================================

# Keep WorkManager classes
-keep class androidx.work.** { *; }
-keepclassmembers class androidx.work.** { *; }

# Keep WorkManager workers
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }

# ============================================================================
# SECURITY CRYPTO RULES
# ============================================================================

# Keep Security Crypto classes
-keep class androidx.security.** { *; }
-keepclassmembers class androidx.security.** { *; }

# ============================================================================
# APP-SPECIFIC RULES
# ============================================================================

# Keep all classes in the app package
-keep class com.ml.shubham0204.docqa.** { *; }
-keepclassmembers class com.ml.shubham0204.docqa.** { *; }

# Keep data models
-keep class com.ml.shubham0204.docqa.data.** { *; }
-keepclassmembers class com.ml.shubham0204.docqa.data.** { *; }

# Keep domain classes
-keep class com.ml.shubham0204.docqa.domain.** { *; }
-keepclassmembers class com.ml.shubham0204.docqa.domain.** { *; }

# Keep UI classes
-keep class com.ml.shubham0204.docqa.ui.** { *; }
-keepclassmembers class com.ml.shubham0204.docqa.ui.** { *; }

# Keep ViewModels
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel { *; }

# Keep Compose ViewModels
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel { *; }

# ============================================================================
# SERIALIZATION RULES
# ============================================================================

# Keep serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ============================================================================
# REFLECTION RULES
# ============================================================================

# Keep classes that might be used with reflection
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ============================================================================
# NATIVE LIBRARY RULES
# ============================================================================

# Keep native method names
-keepclasseswithmembernames class * {
    native <methods>;
}

# ============================================================================
# WEBVIEW RULES (if needed in future)
# ============================================================================

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# ============================================================================
# OPTIMIZATION RULES
# ============================================================================

# Don't warn about unused classes in libraries
-dontwarn org.apache.poi.**
-dontwarn com.itextpdf.**
-dontwarn com.microsoft.onnxruntime.**
-dontwarn com.google.ai.client.generativeai.**
-dontwarn com.google.mediapipe.**
-dontwarn io.insert-koin.**
-dontwarn com.github.jeziellago.**
-dontwarn com.github.shubham0204.**

# Don't warn about missing classes
-dontwarn android.support.**
-dontwarn androidx.**
-dontwarn org.jetbrains.**
-dontwarn kotlin.**
-dontwarn kotlinx.**

# ============================================================================
# ASSET PROTECTION
# ============================================================================

# Keep asset files
-keep class **.onnx { *; }
-keep class **.json { *; }
-keep class **.txt { *; }
-keep class **.bin { *; }