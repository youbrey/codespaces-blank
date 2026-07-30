plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.docapp.feature.editor"
    compileSdk = 34
    defaultConfig { minSdk = 24 }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:command"))
    implementation(project(":core:layout-engine"))
    implementation(project(":core:gate"))
    implementation(project(":core:security"))
    implementation(project(":data:filesystem"))
    implementation(project(":data:local-db"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.coroutines.core)
    implementation(libs.work.runtime)
}
