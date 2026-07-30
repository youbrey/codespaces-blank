plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.docapp.core.security"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
    }
}

dependencies {
    implementation(project(":core:gate"))
    implementation(libs.security.crypto)
    implementation(libs.play.integrity)
}
