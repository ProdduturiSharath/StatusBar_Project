plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.sharath.statusbartotal"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.sharath.statusbartotal"
        minSdk = 33
        targetSdk = 35
        versionCode = 6
        versionName = "4.0"

    }

    buildTypes {
        release {
            // Same key as the debug builds already on the phone, so updates keep the data.
            signingConfig = signingConfigs.getByName("debug")
            optimization {
                enable = true
                // Whole-app optimization. A package-limited scope (androidx/kotlin only)
                // crashed on start: IllegalAccessError between optimized and unoptimized classes.
            }
        }
    }
    buildFeatures {
        compose = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.icons.core)
    implementation(libs.androidx.activity.compose)
    testImplementation(libs.junit)
}
