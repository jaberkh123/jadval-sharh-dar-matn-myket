import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
}

android {
  namespace = "com.aistudio.sharhdarmatn"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.aistudio.sharhdarmatn"
    minSdk = 24
    targetSdk = 36
    versionCode = 15
    versionName = "2.4"
  }

  signingConfigs {
    create("release") {
      // اختیاری: اگر local.properties شامل مسیر keystore باشد، بیلد release امضا می‌شود.
      val localProps = Properties().apply {
        val f = rootProject.file("local.properties")
        if (f.exists()) f.reader(Charsets.UTF_8).use { load(it) }
      }
      fun secret(envKey: String, propKey: String): String =
        System.getenv(envKey) ?: localProps.getProperty(propKey) ?: ""

      val storePath = secret("KEYSTORE_PATH", "KEYSTORE_PATH")
      if (storePath.isNotBlank()) {
        storeFile = file(storePath)
        storePassword = secret("KEYSTORE_PASSWORD", "KEYSTORE_PASSWORD")
        keyAlias = secret("KEY_ALIAS", "KEY_ALIAS")
        keyPassword = secret("KEYSTORE_PASSWORD", "KEYSTORE_PASSWORD")
      }
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      // فقط در صورت وجود keystore در local.properties امضا می‌شود
      val localProps = Properties().apply {
        val f = rootProject.file("local.properties")
        if (f.exists()) f.reader(Charsets.UTF_8).use { load(it) }
      }
      if (localProps.getProperty("KEYSTORE_PATH") != null) {
        signingConfig = signingConfigs.getByName("release")
      }
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  buildFeatures {
    compose = true
  }
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  debugImplementation(libs.androidx.compose.ui.tooling)
  ksp(libs.androidx.room.compiler)
  // v2.1: تبلیغات ادیوری (همسان / میان‌صفحه‌ای / بازگشت به برنامه)
  implementation(libs.adivery)
  // v2.2: تبلیغات خودمان — شبکهٔ «تبلیغ» (روش تبلیغ): coil برای AsyncImage + okhttp برای fetch
  implementation(libs.coil.compose)
  implementation(libs.okhttp)
}
