plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.icstmgsfbstud"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.icstmgsfbstud"
        minSdk = 24
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

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.database)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.messaging)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("com.google.firebase:firebase-analytics:22.1.2")

    implementation("androidx.core:core-ktx:1.6.0")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.github.bumptech.glide:glide:4.12.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.12.0")
    implementation("com.jaredrummler:material-spinner:1.3.1")
    implementation("com.github.fornewid:neumorphism:0.3.2")
    implementation("com.squareup.picasso:picasso:2.8")

    implementation("de.hdodenhof:circleimageview:3.1.0")

    androidTestImplementation ("com.android.support.test.espresso:espresso-core:3.0.2")
    implementation("pl.droidsonroids.gif:android-gif-drawable:1.2.29")

    implementation("com.google.zxing:core:3.3.3")

    implementation("com.github.yuriy-budiyev:code-scanner:2.3.0")

    implementation("com.github.GrenderG:Toasty:1.5.2")

}