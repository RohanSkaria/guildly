plugins {
    alias(libs.plugins.android.application)
    // Add this line to apply Google Services plugin to this module
    id("com.google.gms.google-services")
}

android {
    namespace = "edu.northeastern.guildly"
    compileSdk = 35

    defaultConfig {
        applicationId = "edu.northeastern.guildly"
        minSdk = 27
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.circleimageview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)



    implementation(platform("com.google.firebase:firebase-bom:33.9.0"))

    // Firebase Realtime Database
    implementation("com.google.firebase:firebase-database-ktx")

    //Firebase Auth
    implementation("com.google.firebase:firebase-auth-ktx")
    }
