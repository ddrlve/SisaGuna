plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.sisaguna.android"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.sisaguna.android"
        minSdk = 26
        targetSdk = 34
        versionCode = 5
        versionName = "0.5.0"
        // APK file name: SisaGuna-v<versionName>-debug.apk instead of app-debug.apk.
        base.archivesName.set("SisaGuna-v$versionName")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Backend sisaguna-be (Go). Trailing slash is required by Retrofit.
        buildConfigField("String", "API_BASE_URL", "\"https://sisaguna.fadh.tech/api/v1/\"")
        buildConfigField("String", "WS_URL", "\"wss://sisaguna.fadh.tech/ws\"")
        // The backend still simulates non-cash payment and couriers (PARTNERS_MODE=dummy). While true the app
        // says so; set to false when a real gateway/courier is connected.
        buildConfigField("boolean", "PARTNERS_SIMULATED", "true")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.coil.compose)
    implementation(libs.osmdroid.android)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.security.crypto)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.okhttp.logging)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.kotlinx.serialization.json)
}

// Push (FCM) switches on by itself once Firebase's config file is dropped into app/ (see README). Without it
// the app builds and runs normally and simply skips push; the plugin would fail the build if applied blind.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}
