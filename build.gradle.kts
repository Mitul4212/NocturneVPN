// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        google()
        jcenter()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
//        classpath("com.android.tools.build:gradle:8.1.0") // or latest stable
//        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.0") // or latest stable
//        classpath ("com.google.gms:google-services:4.4.0")
//        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.7.7")

        classpath("com.android.tools.build:gradle:8.5.2")
//        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.24")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.21")

        classpath ("com.google.gms:google-services:4.4.2")
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.8.9")
    }
}

allprojects {
    repositories {
        google()
        maven(url = "https://jitpack.io")
        mavenCentral()
    }

    configurations.all {
        resolutionStrategy {
            force("org.jetbrains.kotlin:kotlin-stdlib:1.8.10")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.8.10")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.10")
        }
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}





//// Top-level build file where you can add configuration options common to all sub-projects/modules.
//plugins {
//    id("com.android.application") version "8.1.2" apply false
//    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
//}