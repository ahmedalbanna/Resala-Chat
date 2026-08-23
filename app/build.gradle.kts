plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.hilt)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}

android {
  namespace = "com.example"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.aistudio.resalachat.vjqkzp"
    minSdk = 24
    targetSdk = 36
    versionCode = 3
    versionName = "1.1.0-phase1"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      // Only configure storeFile if file actually exists to avoid config-time crash when key not present (CI)
      val ksFile = file(keystorePath)
      if (ksFile.exists()) {
        storeFile = ksFile
        storePassword = System.getenv("STORE_PASSWORD")
        keyAlias = System.getenv("KEY_ALIAS") ?: "upload"
        keyPassword = System.getenv("KEY_PASSWORD")
      }
    }
    // Debug keystore is intentionally NOT committed (see .gitignore). AGP will auto-generate
    // a debug keystore at $HOME/.android/debug.keystore if missing. Keep custom debugConfig
    // only when local debug.keystore file exists (legacy AI Studio import).
    create("debugConfig") {
      val dbg = file("${rootDir}/debug.keystore")
      if (dbg.exists()) {
        storeFile = dbg
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
      }
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      // Phase 0 hardening: enable R8 for release (was false). Keep optimize rules.
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      // Use release signing only if configured; otherwise unsigned (CI will inject via env)
      val releaseCfg = signingConfigs.getByName("release")
      signingConfig = if (releaseCfg.storeFile?.exists() == true) releaseCfg else null
    }
    debug {
      isMinifyEnabled = false
      // Use custom debugConfig only if file exists, else AGP default debug keystore
      val dbgCfg = signingConfigs.getByName("debugConfig")
      signingConfig = if (dbgCfg.storeFile?.exists() == true) dbgCfg else null
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Secrets Gradle Plugin: .env / .env.example
// Phase 0: GEMINI_API_KEY is unused (mock E2EE only). Kept for Phase 2 (Firebase AI) so builds
// don't fail when .env missing — defaultPropertiesFileName provides placeholder.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Phase 1 cleanup: removed deprecated accompanist-permissions (→ androidx.activity),
// retired unused Retrofit/Moshi/OkHttp (reserved for Phase 2 custom WS if needed — re-add then),
// kept coil/play-services-location/firebase-bom as they are wired or Phase 2 reserved.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.security.crypto)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.hilt.navigation.compose)
  implementation(libs.androidx.paging.runtime.ktx)
  implementation(libs.androidx.paging.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.hilt.android)
  // implementation(libs.firebase.ai) // Phase 2
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.play.services.location)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.hilt.compiler)
}
