plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.brightport.urbo"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.brightport.urbo"
        minSdk = 24
        targetSdk = 34
        val build = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 0
        versionCode = 1 + build
        versionName = "1.0 (build $build)"
    }

    // A fixed signing key, so each new build installs over the old one. Personal sideload use only.
    signingConfigs {
        create("sideload") {
            storeFile = file("urbo.keystore")
            storePassword = "urbo-app"
            keyAlias = "urbo"
            keyPassword = "urbo-app"
        }
    }

    buildTypes {
        getByName("debug") { signingConfig = signingConfigs.getByName("sideload") }
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("sideload")
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.webkit:webkit:1.12.1")
    implementation("androidx.core:core-ktx:1.13.1")
}
