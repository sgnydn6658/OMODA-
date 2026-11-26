plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.aracekran.video"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.aracekran.video"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Kendi imza anahtarınız yoksa debug anahtarıyla imzalanır (kurulabilir APK üretir).
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Google'ın resmi Android for Cars kütüphanesi (telefonda çalışır, araca bir şey yüklemez)
    implementation("androidx.car.app:app:1.4.0")
    // Android Auto (telefon yansıtma) desteği + araç hızı okuma
    implementation("androidx.car.app:app-projected:1.4.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-common:2.6.2")
}
