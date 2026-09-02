import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.uccd3223.group13.foodhero"
    compileSdk = 36

    defaultConfig {
        manifestPlaceholders += mapOf()
        applicationId = "com.uccd3223.group13.foodhero"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Load secrets from gitignored secrets.properties or local.properties
        val secretsFile = rootProject.file("secrets.properties")
        val localPropsFile = rootProject.file("local.properties")
        val secrets = Properties()

        if (secretsFile.exists()) {
            FileInputStream(secretsFile).use { secrets.load(it) }
        } else if (localPropsFile.exists()) {
            FileInputStream(localPropsFile).use { secrets.load(it) }
        }

        val supabaseUrl = secrets.getProperty("SUPABASE_URL")
            ?: (project.findProperty("SUPABASE_URL") as? String)
            ?: "https://your-project-id.supabase.co"
        val supabaseAnonKey = secrets.getProperty("SUPABASE_ANON_KEY")
            ?: (project.findProperty("SUPABASE_ANON_KEY") as? String)
            ?: "dummy-supabase-anon-key"
        val mapsApiKey = secrets.getProperty("MAPS_API_KEY")
            ?: (project.findProperty("MAPS_API_KEY") as? String)
            ?: "dummy-google-maps-api-key"
        val googleWebClientId = secrets.getProperty("GOOGLE_WEB_CLIENT_ID")
            ?: (project.findProperty("GOOGLE_WEB_CLIENT_ID") as? String)
            ?: "dummy-google-web-client-id"

        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
        buildConfigField("String", "GOOGLE_MAPS_KEY", "\"$mapsApiKey\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
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
