import com.otaliastudios.tools.publisher.common.License
import com.otaliastudios.tools.publisher.common.Release

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("jacoco")
}

android {
    setCompileSdkVersion(29)
    defaultConfig {
        setMinSdkVersion(15)
        setTargetSdkVersion(29)
        versionCode = 1
        versionName = "2.6.3"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArgument("filter", "" +
                "com.otaliastudios.cameraview.tools.SdkExcludeFilter," +
                "com.otaliastudios.cameraview.tools.SdkIncludeFilter")
    }
    buildTypes["debug"].isTestCoverageEnabled = true
    buildTypes["release"].isMinifyEnabled = false
}

dependencies {
    testImplementation("junit:junit:4.12")
    testImplementation("org.mockito:mockito-inline:2.28.2")

    androidTestImplementation("androidx.test:runner:1.2.0")
    androidTestImplementation("androidx.test:rules:1.2.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.1")
    androidTestImplementation("org.mockito:mockito-android:2.28.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.2.0")

    api("androidx.exifinterface:exifinterface:1.1.0")
    api("androidx.lifecycle:lifecycle-common:2.1.0")
    api("com.google.android.gms:play-services-tasks:17.0.0")
    implementation("androidx.annotation:annotation:1.1.0")
    implementation("com.otaliastudios.opengl:egloo:0.5.2")
}

// Publishing