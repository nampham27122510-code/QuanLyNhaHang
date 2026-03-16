plugins {
    // Chỉ giữ lại alias cho các plugin chuẩn
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // Thêm plugin Google Services để kết nối Firebase
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.quanlynhahang"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.quanlynhahang"
        minSdk = 24
        targetSdk = 35
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
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // Ép các thư viện cơ bản về phiên bản ổn định cho SDK 35/34
    implementation("androidx.core:core-ktx:1.13.0") // Hạ từ 1.18.0 xuống 1.13.0
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity:1.9.3") // Hạ từ 1.13.0 xuống 1.9.3
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Firebase BoM giữ nguyên vì bạn đã để bản 32.8.0 rất ổn định
    implementation(platform("com.google.firebase:firebase-bom:32.8.0"))
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")

    // Các thư viện test
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}