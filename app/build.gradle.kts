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
        versionCode = 3
        versionName = "3.0"

    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
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
