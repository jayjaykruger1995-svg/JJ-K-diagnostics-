plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }

android {
    namespace = "com.jjkdiagnostics"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.jjkdiagnostics"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }
}

kotlin { jvmToolchain(17) }
