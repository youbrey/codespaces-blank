plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.docapp.data.billing"
    compileSdk = 34
    defaultConfig { minSdk = 24 }
}

dependencies {
    implementation(project(":core:security"))
    implementation(project(":core:gate"))
    implementation(libs.billing.ktx)
    implementation(libs.play.integrity)
    implementation(libs.coroutines.core)
}
