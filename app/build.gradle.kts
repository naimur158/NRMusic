plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.nrmusic.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nrmusic.app"
        minSdk = 26
        targetSdk = 35
        // versionCode/versionName can be overridden by CI: -PversionCode=42 -PversionName=1.0.42
        versionCode = (project.findProperty("versionCode") as String?)?.toInt() ?: 1
        versionName = (project.findProperty("versionName") as String?) ?: "1.0"
        // Repo for in-app update checks. CI injects it via -PgithubRepo=owner/name
        // (GitHub Actions passes ${{ github.repository }} automatically); blank locally.
        buildConfigField(
            "String",
            "GITHUB_REPO",
            "\"${project.findProperty("githubRepo") ?: ""}\""
        )
    }

    signingConfigs {
        create("release") {
            // Local builds use the committed keystore + default passwords; CI overrides
            // everything through environment variables (keystore restored from a secret).
            fun env(name: String) = System.getenv(name)?.takeIf { it.isNotBlank() }
            storeFile = rootProject.file(env("KEYSTORE_FILE") ?: "nrmusic-release.jks")
            storePassword = env("KEYSTORE_PASSWORD") ?: "nrmusic123"
            keyAlias = env("KEY_ALIAS") ?: "nrmusic"
            keyPassword = env("KEY_PASSWORD") ?: "nrmusic123"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-session:1.5.1")

    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("sh.calvin.reorderable:reorderable:3.1.0")
    implementation("androidx.palette:palette-ktx:1.0.0")
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("com.github.TeamNewPipe:NewPipeExtractor:v0.26.3")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")
}
