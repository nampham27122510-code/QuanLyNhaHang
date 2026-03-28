// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // Plugin hỗ trợ dịch vụ Google (Firebase)
        classpath("com.google.gms:google-services:4.4.2")
    }
}

plugins {
    // Hạ cấp về 8.8.0 để khớp với phiên bản Android Studio hiện tại của Nam
    id("com.android.application") version "8.8.0" apply false
    id("com.android.library") version "8.8.0" apply false

    // Giữ Kotlin ở bản 1.9.22 hoặc 2.0.0 đều được, 1.9.22 sẽ ổn định hơn với AGP 8.8.0
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
}