plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.compose.compiler)
    id("com.google.devtools.ksp")
    id("io.objectbox")
}

import com.example.build.loadProperties

val localProperties = loadProperties(rootProject.file("local.properties"))

android {
    namespace = "com.ml.shubham0204.docqa"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ml.shubham0204.docqa"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "0.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        proguardFiles("proguard-rules.pro")
    }
    signingConfigs {
        create("release") {
            storeFile = file(localProperties.getProperty("release.storeFile", "DEFAULT_PATH_TO_YOUR_KEYSTORE"))
            storePassword = localProperties.getProperty("release.storePassword", "DEFAULT_STORE_PASSWORD")
            keyAlias = localProperties.getProperty("release.keyAlias", "DEFAULT_KEY_ALIAS")
            keyPassword = localProperties.getProperty("release.keyPassword", "DEFAULT_KEY_PASSWORD")
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "META-INF/DEPENDENCIES"
            pickFirsts += "META-INF/gradle/incremental.annotation.processors"
        }
    }
    buildToolsVersion = "35.0.0"
    ndkVersion = "27.2.12479018"
}

ksp {
    arg("KOIN_CONFIG_CHECK", "true")
}

configurations {
    all {
        exclude(group = "io.objectbox", module = "objectbox-android-objectbrowser")
    }
}

// Configuration for dependency resolution
configurations.all {
    resolutionStrategy {
        eachDependency {
            if (requested.group == "org.jetbrains.kotlin") {
                useVersion("2.0.0")
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.compose.material3.icons.extended)
    implementation(libs.navigation.compose)

    // Apache POI
    implementation(libs.apache.poi)
    implementation(libs.apache.poi.ooxml)

    // Sentence Embeddings
    // https://github.com/shubham0204/Sentence-Embeddings-Android
    implementation(files("libs/sentence_embeddings.aar"))
    implementation(libs.onnxruntime.android)

    // iTextPDF - for parsing PDFs
    implementation(libs.itextpdf)

    // ObjectBox - vector database
    implementation(libs.objectbox.android)
    // The object browser dependency is disabled as it causes duplicate class errors.
    // debugImplementation(libs.objectbox.android.objectbrowser)

    // Gemini SDK - LLM
    implementation(libs.generativeai)

    // MediaPipe LLM Inference - Local LLM (Updated for Android 15 compatibility)
    implementation(libs.tasks.genai)

    // compose-markdown
    // https://github.com/jeziellago/compose-markdown
    implementation(libs.compose.markdown)

    // Koin dependency injection
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.annotations)
    implementation(libs.koin.androidx.compose)
    ksp(libs.koin.ksp.compiler)

    // For secured/encrypted shared preferences
    implementation(libs.androidx.security.crypto)

    // WorkManager for background tasks
    implementation(libs.androidx.work.runtime.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
