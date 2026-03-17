plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

    // Apply Google Services plugin for Firebase
    id("com.google.gms.google-services") version "4.4.4"
}

android {
    namespace = "com.apollo.socially"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.apollo.socially"
        minSdk = 29
        targetSdk = 36
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Firebase
    // Using explicit versions to avoid BOM resolution issues in this setup
    implementation("com.google.firebase:firebase-analytics:21.4.0")
    // Firebase Authentication (KTX)
    implementation("com.google.firebase:firebase-auth-ktx:22.1.1")
    // Coroutines support for Tasks (helps awaiting Firebase tasks)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.6.4")
    // Lifecycle ViewModel KTX for viewModelScope and StateFlow interoperability
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    // Coroutines Android dispatcher
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")






}