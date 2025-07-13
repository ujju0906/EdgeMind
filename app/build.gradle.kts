plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.compose.compiler)
    id("com.google.devtools.ksp")
    id("io.objectbox")
}

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
            storeFile = file("C:\\Users\\girid\\Desktop\\passcode\\raghavcode.jks")
            storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("RELEASE_KEYSTORE_ALIAS")
            keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
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
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.17.0")

    // iTextPDF - for parsing PDFs
    implementation(libs.itextpdf)

    // ObjectBox - vector database
    implementation(libs.objectbox.android)
    // The object browser dependency is disabled as it causes duplicate class errors.
    // debugImplementation(libs.objectbox.android.objectbrowser)

    // Gemini SDK - LLM
    implementation(libs.generativeai)

    // MediaPipe LLM Inference - Local LLM
    implementation("com.google.mediapipe:tasks-genai:0.10.24")

    // compose-markdown
    // https://github.com/jeziellago/compose-markdown
    implementation(libs.compose.markdown)

    // Koin dependency injection
    implementation(libs.koin.android)
    implementation(libs.koin.annotations)
    implementation(libs.koin.androidx.compose)
    ksp(libs.koin.ksp.compiler)

    // For secured/encrypted shared preferences
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Koin
    implementation(platform("io.insert-koin:koin-bom:3.5.6"))
    implementation("io.insert-koin:koin-android")
    implementation("io.insert-koin:koin-androidx-compose")

    // WorkManager for background tasks
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
