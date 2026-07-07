// ─── App-level build.gradle.kts ───────────────────────────────────────────────

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.navigation.safeargs)
}

android {
    namespace   = "com.example.bloodbank"
    compileSdk  = 35

    defaultConfig {
        applicationId  = "com.example.blood"
        minSdk         = 26       // Android 8.0 — covers ~95% of devices
        targetSdk      = 35
        versionCode    = 1
        versionName    = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Hilt test runner (needed when you add Hilt tests later)
        // testInstrumentationRunner = "com.example.bloodbank.HiltTestRunner"
    }

    buildTypes {
        debug {
            isDebuggable   = true
            isMinifyEnabled = false
            buildConfigField("String", "BUILD_TYPE_LABEL", "\"Debug\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "BUILD_TYPE_LABEL", "\"Release\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview"
        )
    }

    buildFeatures {
        viewBinding  = true   // ViewBinding instead of synthetic / findViewId
        buildConfig  = true   // Enables BuildConfig class
    }

    // Split output APKs by ABI for smaller downloads (optional, good practice)
    splits {
        abi {
            isEnable = false   // Enable on release if needed
        }
    }
}

// ─── Dependencies ─────────────────────────────────────────────────────────────
dependencies {

    // ── Core & AppCompat ───────────────────────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.splashscreen)

    // ── Lifecycle (ViewModel + LiveData + Flow) ────────────────────────────────
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)

    // ── Navigation Component ───────────────────────────────────────────────────
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // ── Room (Local cache / offline support) ──────────────────────────────────
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)           // ← KSP (not kapt)

    // ── DataStore (user preferences) ──────────────────────────────────────────
    implementation(libs.androidx.datastore.preferences)

    // ── UI / Material ──────────────────────────────────────────────────────────
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.glide)
    implementation(libs.lottie)
    implementation(libs.shimmer)

    // ── Dependency Injection — Hilt ────────────────────────────────────────────
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)                    // ← KSP (not kapt)
    implementation(libs.hilt.navigation.fragment)
    
    // Hilt Worker (For WorkManager)
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // ── WorkManager ────────────────────────────────────────────────────────────
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // ── Firebase (BOM keeps all versions in sync) ──────────────────────────────
    implementation(platform(libs.firebase.bom))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-database-ktx")
    
    implementation(libs.firebase.storage.ktx)
    implementation(libs.firebase.messaging.ktx)
    implementation(libs.firebase.analytics.ktx)

    // ── Social Login ───────────────────────────────────────────────────────────
    implementation("com.google.android.gms:play-services-auth:21.0.0")
    implementation("com.facebook.android:facebook-login:16.3.0")

    // ── Kotlin Coroutines ──────────────────────────────────────────────────────
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)   // .await() on Firebase Tasks

    // ── Location Services (GPS) ────────────────────────────────────────────────
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // ── QR Code Generator (offline, no API cost) ───────────────────────────────
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    // ── Network (Retrofit for PSGC API) ────────────────────────────────────────
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)

    // ── Unit Tests ─────────────────────────────────────────────────────────────
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)

    // ── Android Instrumentation Tests ─────────────────────────────────────────
    androidTestImplementation(libs.androidx.junit.ext)
    androidTestImplementation(libs.androidx.espresso.core)
}
