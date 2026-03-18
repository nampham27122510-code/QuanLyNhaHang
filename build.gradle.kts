// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // Plugin hỗ trợ dịch vụ Google (Firebase)
        classpath("com.google.gms:google-services:4.4.1")
    }
}

plugins {
    // Phiên bản Android Gradle Plugin (AGP) 8.8.0 yêu cầu Java 17
    id("com.android.application") version "8.8.0" apply false
    id("com.android.library") version "8.8.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
}