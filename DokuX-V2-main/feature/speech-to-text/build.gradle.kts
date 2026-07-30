plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.docapp.feature.speech"
    compileSdk = 34
    defaultConfig { minSdk = 24 }
}

dependencies {
    implementation(project(":core:gate"))
    implementation(project(":core:security"))
    implementation(libs.coroutines.core)
    implementation("com.alphacephei:vosk-android:0.3.47")
    implementation("net.java.dev.jna:jna:5.13.0@aar") // dependency Vosk untuk native binding
}
