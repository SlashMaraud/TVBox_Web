plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "dev.deads.webapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "dev.deads.webapp"
        minSdk = 21 // GeckoView requiere mínimo 21, perfecto para Chromecast
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    
    // Esta es la versión "comodín" que Mozilla sirve por defecto.
    // Si esta no la encuentra, es que hay un problema de conexión con su servidor.
    implementation("org.mozilla.geckoview:geckoview-omni:115.0.20230710155057")
}
