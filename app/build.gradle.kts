plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
}

import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

val autoBuildNumber: Int by lazy {
    val versionPropsFile = file("version.properties")
    val versionProps = Properties()

    if (versionPropsFile.exists()) {
        FileInputStream(versionPropsFile).use { versionProps.load(it) }
    }

    val currentBuild = versionProps.getProperty("build_number", "5").toIntOrNull() ?: 5
    val isBuildingApp = gradle.startParameter.taskNames.any {
        it.contains("assemble", ignoreCase = true) || it.contains("bundle", ignoreCase = true) || it.contains("build", ignoreCase = true)
    }

    val nextBuild = if (isBuildingApp) currentBuild + 1 else currentBuild

    if (isBuildingApp) {
        versionProps.setProperty("build_number", nextBuild.toString())
        FileOutputStream(versionPropsFile).use { versionProps.store(it, "Auto-generated build number") }
    }

    nextBuild
}

android {
    namespace = "com.example.attendancetracker"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.example.attendancetracker"
        minSdk = 24
        targetSdk = 36
        versionCode = autoBuildNumber
        versionName = "1.0.4-alpha"
        buildConfigField("String", "RELEASE_TYPE", "\"Alpha\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      buildConfig = true
      aidl = false
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.extended)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation("org.json:json:20240303")

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // Room
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  ksp(libs.androidx.room.compiler)

  // WorkManager
  implementation("androidx.work:work-runtime-ktx:2.10.0")
}
