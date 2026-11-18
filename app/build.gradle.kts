plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "motakashi.androidapp.pomodorotimer"
    compileSdk = 35

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        applicationId = "motakashi.androidapp.pomodorotimer"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
