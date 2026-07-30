plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.docapp.data.filesystem"
    compileSdk = 34
    defaultConfig { minSdk = 24 }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":data:docx-engine"))
    implementation(project(":data:local-db"))
    implementation(libs.coroutines.core)
}
