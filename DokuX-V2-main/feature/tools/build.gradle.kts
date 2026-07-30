plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.docapp.feature.tools"
    compileSdk = 34
    defaultConfig { minSdk = 24 }
}

dependencies {
    implementation(project(":core:security"))
    implementation(project(":core:gate"))
    implementation(project(":core:model"))
}
