plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.tayebi7.fennecplayer"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.tayebi7.fennecplayer"
        minSdk = 21 // لدعم كافة شاشات Android TV وأجهزة TV Box القديمة والحديثة
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // واجهات أندرويد ودعم شاشات التلفزيون (Leanback)
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.leanback:leanback:1.0.0")

    // مشغل الفيديو Media3 (ExoPlayer) لدعم HLS (m3u8), TS, RTSP, Dash
    val media3Version = "1.2.1"
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-exoplayer-hls:$media3Version")
    implementation("androidx.media3:media3-exoplayer-dash:$media3Version")
    implementation("androidx.media3:media3-exoplayer-rtsp:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")

    // الاتصال بسيرفرات Xtream Codes وقراءة ملفات JSON / M3U
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // تحميل شعارات القنوات وبوسترات الأفلام (Glide)
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // البرمجة غير المتزامنة (Coroutines) لمنع تشنج الواجهة
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
}
