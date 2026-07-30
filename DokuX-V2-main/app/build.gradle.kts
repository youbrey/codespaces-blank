plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.docapp.editor"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.docapp.editor"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        create("debugConfig") {
            storeFile = file("${rootDir}/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debugConfig")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:command"))
    implementation(project(":core:layout-engine"))
    implementation(project(":core:gate"))
    implementation(project(":core:security"))
    implementation(project(":data:docx-engine"))
    implementation(project(":data:pdf-export"))
    implementation(project(":data:local-db"))
    implementation(project(":data:billing"))
    implementation(project(":data:filesystem"))
    implementation(project(":feature:editor"))
    implementation(project(":feature:export"))
    implementation(project(":feature:template"))
    implementation(project(":feature:tools"))
    implementation(project(":feature:speech-to-text"))
    implementation(project(":feature:file-browser"))
    implementation(project(":feature:ai-generate"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.runtime)
    implementation(libs.activity.compose)
    implementation(libs.coroutines.core)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
