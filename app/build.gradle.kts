plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}
apply(plugin = "androidx.navigation.safeargs.kotlin")


android {
    namespace = "com.nocturnevpn"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nocturnevpn"
        minSdk = 24
        targetSdk = 35
        versionCode = 5
        versionName = "1.0.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        multiDexEnabled = true
        buildConfigField("String", "VPN_GATE_API", "\"http://www.vpngate.net/api/iphone/\"")
        // Toggle to disable third-party SDK initializations during troubleshooting
        buildConfigField("boolean", "DISABLE_THIRD_PARTY_SDKS", "true")

    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures{
        dataBinding = true
        viewBinding = true
        buildConfig = true
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    // Deliver a single base APK (disable language/density/ABI splits)
    bundle {
        language { enableSplit = false }
        density { enableSplit = false }
        abi { enableSplit = false }
    }

    // Ensure native libs are extracted to app lib/ directory so OpenVPN can exec from nativeLibraryDir
    packagingOptions {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {

    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))


    implementation("androidx.core:core-ktx:1.15.0")
    implementation ("androidx.activity:activity-ktx:1.7.2")

    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
//    implementation("org.chromium.net:cronet-embedded:119.6045.31")
    implementation("com.google.android.gms:play-services-cronet:18.0.1")
    implementation("androidx.webkit:webkit:1.13.0")
    implementation ("androidx.work:work-runtime-ktx:2.9.0")

    implementation("androidx.navigation:navigation-fragment-ktx:2.8.9")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.9")
    implementation("androidx.gridlayout:gridlayout:1.0.0")
    implementation("com.google.firebase:firebase-firestore-ktx:25.1.4")


    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    implementation(project(":vpnLib"))


    implementation ("com.github.ibrahimsn98:SmoothBottomBar:1.7.9")

    implementation ("androidx.cardview:cardview:1.0.0")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.github.bumptech.glide:glide:4.12.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.12.0")

    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.badoo.mobile:android-weak-handler:1.1")
//    implementation("com.android.support:multidex:1.0.3")

    implementation("androidx.multidex:multidex:2.0.1")

    implementation ("com.github.murgupluoglu:flagkit-android:1.0.5")
    implementation ("com.facebook.shimmer:shimmer:0.5.0")
    implementation ("com.google.android.material:material:1.12.0") // or latest
    implementation ("com.airbnb.android:lottie:6.3.0")

    // Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:33.16.0"))

    // Firebase modules
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-analytics")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    
    // Facebook Sign-In
    implementation("com.facebook.android:facebook-login:16.3.0")
    
    implementation("com.google.code.gson:gson:2.10.1")

    // Google UMP SDK for GDPR/CCPA compliance
    implementation("com.google.android.ump:user-messaging-platform:2.1.0")

    // Google Ads SDK for banner ads
    implementation("com.google.android.gms:play-services-ads:22.6.0")

}