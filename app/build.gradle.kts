plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }

android {
    namespace = "com.jjkdiagnostics"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.jjkdiagnostics.test"
        minSdk = 23
        targetSdk = 28
        versionCode = 3
        versionName = "0.1.2"
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            isDebuggable = true
        }
    }
}

kotlin { jvmToolchain(17) }
