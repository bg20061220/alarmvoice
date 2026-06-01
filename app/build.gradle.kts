plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.voicesnooze"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.voicesnooze"
        minSdk = 26 // Android 8.0 (Oreo). SpeechRecognizer available since API 8, but we use API 26 for stability.
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    // Enable Jetpack Compose
    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
}

dependencies {
    // Kotlin & Coroutines: async code without threads
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")

    // AndroidX: modern Android framework
    implementation("androidx.core:core:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2") // lifecycleScope for coroutines

    // Jetpack Compose: declarative UI framework
    // BOM (Bill of Materials) pins all Compose versions to be compatible
    val composeBom = platform("androidx.compose:compose-bom:2023.10.00")
    implementation(composeBom)

    // Compose core
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3") // Material Design 3

    // Compose activity integration
    implementation("androidx.activity:activity-compose:1.8.0")

    // Material Design 3 icons (optional, for better UI)
    implementation("androidx.compose.material:material-icons-extended")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Compose testing (optional, for UI testing)
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
