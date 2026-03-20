plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

    // Apply Google Services plugin for Firebase
    id("com.google.gms.google-services") version "4.4.4"
    
    // KSP for Room - compatible with Kotlin 2.0.21
    id("com.google.devtools.ksp") version "2.0.21-1.0.28"

    id("kotlin-parcelize")
}

android {
    namespace = "com.apollo.socially"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.apollo.socially"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    android {
        buildFeatures {
            viewBinding = true
        }
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
    // Firebase Firestore (KTX)
    implementation("com.google.firebase:firebase-firestore-ktx:24.8.1")
    // Coroutines support for Tasks (helps awaiting Firebase tasks)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.6.4")
    // Lifecycle ViewModel KTX for viewModelScope and StateFlow interoperability
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    // Lifecycle Runtime KTX for lifecycleScope
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    // Coroutines Android dispatcher
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")

    // Google Sign-In (for Firebase Authentication via Google)
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    
    // CardView
    implementation("androidx.cardview:cardview:1.0.0")
    
    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    
    // Navigation Component
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.5")
    
    // Fragment KTX
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    
    // Lottie for animations
    implementation("com.airbnb.android:lottie:6.1.0")


    implementation("de.hdodenhof:circleimageview:3.1.0")  // circular avatar with border
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")


    // Glide — media thumbnail loading in the picker grid
    implementation("com.github.bumptech.glide:glide:4.16.0")
    ksp("com.github.bumptech.glide:compiler:4.16.0")

    // ViewPager2 — for multi-image post swiping
    implementation("androidx.viewpager2:viewpager2:1.0.0")

    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    implementation("com.github.yalantis:ucrop:2.2.8")


}