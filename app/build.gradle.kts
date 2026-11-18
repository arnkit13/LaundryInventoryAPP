import java.net.Socket

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.laundry.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.laundry.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.material)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    debugImplementation(libs.androidx.compose.ui.tooling)
}

tasks.register("startBackend") {
    group = "backend"
    description = "Starts the Spring Boot backend server if it is not already running."
    doFirst {
        val isPortInUse = try {
            Socket("127.0.0.1", 8080).use { true }
        } catch (e: Exception) {
            false
        }
        
        if (isPortInUse) {
            println("Backend is already running on port 8080. Skipping startup.")
        } else {
            println("Starting backend server...")
            val backendDir = File(project.rootDir.parentFile, "LaundryInventorySystem/backend")
            val pb = if (System.getProperty("os.name").lowercase().contains("windows")) {
                ProcessBuilder("cmd", "/c", "start", "/min", "cmd", "/c", "mvnw.cmd spring-boot:run")
            } else {
                ProcessBuilder("sh", "-c", "./mvnw spring-boot:run &")
            }
            pb.directory(backendDir)
            pb.start()
            println("Backend server started in background.")
        }
    }
}

// Commented out to prevent Android Studio build/run hangs or delays.
// You can still run the backend manually, or run `./gradlew startBackend` if needed.
// tasks.named("preBuild") {
//     dependsOn("startBackend")
// }