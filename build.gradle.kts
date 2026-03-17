// Top-level build file
buildscript {
    dependencies {
        classpath("com.google.gms:google-services:4.4.1") // Cần dòng này để nhận plugin gms
    }
}

plugins {
    id("com.android.application") version "8.8.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
}