plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.saavi.saavi"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.saavi.saavi"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val apiKey: String? = project.findProperty("GEMINI_API_KEY") as String?
        buildConfigField("String", "GEMINI_API_KEY", "\"$apiKey\"")


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

        // ✅ Fix for "META-INF/INDEX.LIST" error
        packaging {
            resources {
                excludes += "META-INF/INDEX.LIST"
                excludes += "META-INF/DEPENDENCIES"
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

        // ✅ Add CameraX dependencies
        implementation("androidx.camera:camera-core:1.4.1")
        implementation("androidx.camera:camera-camera2:1.4.1")
        implementation("androidx.camera:camera-lifecycle:1.4.1")
        implementation("androidx.camera:camera-view:1.4.1")

        // ✅ AI Model API (Google Gemini)
        implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

        // ✅ Networking (for API calls)
        implementation("com.squareup.okhttp3:okhttp:4.12.0")

        // ✅ Text-to-Speech (TTS) & Speech Recognition
        implementation("androidx.core:core:1.15.0")
        implementation("com.google.cloud:google-cloud-speech:2.2.1")

        // ✅ Material Icons for UI Enhancements
        implementation("androidx.compose.material:material-icons-core:1.7.8")
        implementation("androidx.compose.material:material-icons-extended:1.7.8")

        // to store and retrieve the selected language.
        implementation("androidx.datastore:datastore-preferences:1.1.3")

        // Retrofit for API calls
        implementation("com.squareup.retrofit2:retrofit:2.9.0")
        implementation("com.squareup.retrofit2:converter-gson:2.9.0")

// OkHttp for networking
        implementation("com.squareup.okhttp3:okhttp:4.9.3")



        testImplementation(libs.junit)
        androidTestImplementation(libs.androidx.junit)
        androidTestImplementation(libs.androidx.espresso.core)
        androidTestImplementation(platform(libs.androidx.compose.bom))
        androidTestImplementation(libs.androidx.ui.test.junit4)
        debugImplementation(libs.androidx.ui.tooling)
        debugImplementation(libs.androidx.ui.test.manifest)
    }
}
