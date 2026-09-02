plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.uccd3223.group13.mad_group13_foodhero"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.uccd3223.group13.mad_group13_foodhero"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Build config fields for Supabase & Google Maps
        buildConfigField("String", "SUPABASE_URL", "\"https://mfflnhpukfegotlqejne.supabase.co\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1mZmxuaHB1a2ZlZ290bHFlam5lIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDAzNjY3MTYsImV4cCI6MjA1NTk0MjcxNn0.Vz56F2Qz7559-0Z6mB1n5J-eR5B_X8lF1GzUj6qQZ-0\"")
        buildConfigField("String", "GOOGLE_MAPS_KEY", "\"AIzaSyD-dummy-maps-key-for-campus-preview\"")
        manifestPlaceholders["MAPS_API_KEY"] = "AIzaSyD-dummy-maps-key-for-campus-preview"
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
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    // Android UI & Components
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.cardview)
    implementation(libs.viewpager2)
    implementation(libs.swiperefreshlayout)

    // Room Database
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    // Retrofit & OkHttp & Gson
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    // Glide
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)

    // Security Crypto
    implementation(libs.security.crypto)

    // WorkManager
    implementation(libs.work.runtime)

    // ZXing Barcode / QR Code
    implementation(libs.zxing.core)
    implementation(libs.zxing.embedded)

    // Google Play Services Maps & Location
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    implementation(libs.maps.utils)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}