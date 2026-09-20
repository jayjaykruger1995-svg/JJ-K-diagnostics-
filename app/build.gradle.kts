plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }

android {
    namespace = "com.jjkdiagnostics"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.jjkdiagnostics"
        minSdk = 23
        targetSdk = 34
        versionCode = 2
        versionName = "0.1.1"
    }

    // Keep the first test build as a normal signed debug APK so Android can install it directly.
    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            isDebuggable = true
        }
    }
}

kotlin { jvmToolchain(17) }
