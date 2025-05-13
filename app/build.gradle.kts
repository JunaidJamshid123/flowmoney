// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
    id("org.jetbrains.kotlin.kapt") // ✅ Directly applied without alias
    id ("kotlin-parcelize")



}

android {
    namespace = "com.example.flowmoney"
    compileSdk = 35



    buildFeatures {
        viewBinding = true
        compose = false // DISABLE compose here
    }

    defaultConfig {
        applicationId = "com.example.flowmoney"
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
// Configure the Compose compiler version




dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // Added Google Sign-In dependency
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    // Third-party libraries
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    kapt("com.github.bumptech.glide:compiler:4.16.0")
    implementation(libs.firebase.storage)

    // Room Database components for offline support
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion") // Kotlin Extensions for Room
    
    // Gson for JSON serialization/deserialization
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Using Android standard notifications instead of OneSignal
    
    // WorkManager for background synchronization
    implementation("androidx.work:work-runtime-ktx:2.8.1")
    
    // SwipeRefreshLayout - needed for pull-to-refresh functionality
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    
    // Lifecycle components - needed for viewModelScope and LiveData
    val lifecycleVersion = "2.6.2"
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion") // viewModelScope
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion") // LiveData + asLiveData()
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion") // lifecycleScope
    
    // OneSignal push notification
    // implementation("com.onesignal:OneSignal:[5.0.0, 5.99.99]")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}