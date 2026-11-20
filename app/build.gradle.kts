import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

// Secrets live in git-ignored local.properties, never in source control.
// Public builds intentionally omit AI_GATEWAY_API_KEY so compiled APKs never leak credentials.
// Pass `-PincludeLocalSecrets=true` ONLY for private USB deploys to your personal device.
val localProps = Properties().apply {
  val f = rootProject.file("local.properties")
  if (f.exists()) f.inputStream().use { load(it) }
}
val includeLocalSecrets = providers.gradleProperty("includeLocalSecrets")
  .map { it.equals("true", ignoreCase = true) }
  .getOrElse(false)
fun propOrLocal(name: String, fallback: String = ""): String =
  (localProps.getProperty(name) ?: fallback).replace("\\", "\\\\").replace("\"", "\\\"")
fun privateSecret(name: String): String =
  if (includeLocalSecrets) propOrLocal(name, "") else ""

android {
  namespace = "com.timecure.app"
  compileSdk = 36
  defaultConfig {
    applicationId = "com.timecure.app"
    minSdk = 26
    targetSdk = 35
    versionCode = 2
    versionName = "2.0"

    buildConfigField("String", "AI_GATEWAY_API_KEY", "\"${privateSecret("AI_GATEWAY_API_KEY")}\"")
    buildConfigField(
      "String", "AI_GATEWAY_URL",
      "\"${propOrLocal("AI_GATEWAY_URL", "https://ai-gateway.vercel.sh/v1/chat/completions")}\""
    )
    buildConfigField("String", "AI_MODEL", "\"${propOrLocal("AI_MODEL", "minimax/minimax-m3.1-flash")}\"")
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    compose = true
    aidl = false
    buildConfig = true
    shaders = false
  }
  testOptions {
    unitTests.isReturnDefaultValues = true
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

  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.extended)

  implementation(libs.okhttp)

  debugImplementation(libs.androidx.compose.ui.tooling)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation("org.json:json:20240303")

  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)
}
