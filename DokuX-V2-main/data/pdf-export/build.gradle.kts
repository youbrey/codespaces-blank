plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.docapp.data.pdf"
    compileSdk = 34
    defaultConfig { minSdk = 24 }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:layout-engine"))
    implementation(project(":core:security"))
    implementation(project(":feature:export"))
}
