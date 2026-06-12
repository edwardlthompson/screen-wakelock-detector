import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
}

fun releaseKeystoreFile(): java.io.File? {
    val path = keystoreProperties.getProperty("storeFile") ?: System.getenv("RELEASE_STORE_FILE")
    return path?.let { file(it).takeIf { f -> f.exists() } }
}

android {
    namespace = "com.screenwakelock.detector"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.screenwakelock.detector"
        minSdk = 29
        targetSdk = 35
        versionCode = 1002010
        versionName = "1.2.10"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        create("release") {
            val store = releaseKeystoreFile() ?: return@create
            storeFile = store
            storePassword = keystoreProperties.getProperty("storePassword")
                ?: System.getenv("RELEASE_STORE_PASSWORD")
            keyAlias = keystoreProperties.getProperty("keyAlias")
                ?: System.getenv("RELEASE_KEY_ALIAS")
            keyPassword = keystoreProperties.getProperty("keyPassword")
                ?: System.getenv("RELEASE_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            releaseKeystoreFile()?.let {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-service:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

    implementation("com.google.dagger:hilt-android:2.52")
    ksp("com.google.dagger:hilt-android-compiler:2.52")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    implementation("com.github.topjohnwu.libsu:core:6.0.0")

    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.glance:glance-material3:1.1.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

val copyReleaseNotes by tasks.registering(Copy::class) {
    val versionCode = android.defaultConfig.versionCode!!
    from(rootProject.file("fastlane/metadata/android/en-US/changelogs/$versionCode.txt"))
    into(layout.projectDirectory.dir("src/main/res/raw"))
    rename { "changelog_$versionCode.txt" }
}

val copyNamedReleaseApk by tasks.registering(Copy::class) {
    dependsOn("assembleRelease")
    val versionName = android.defaultConfig.versionName!!
    from(layout.buildDirectory.dir("outputs/apk/release"))
    include("app-release.apk")
    into(rootProject.layout.projectDirectory.dir("dist"))
    rename { "Screen-Wakelock-Detector-${versionName}.apk" }
    doFirst {
        val signedApk = layout.buildDirectory.file("outputs/apk/release/app-release.apk").get().asFile
        if (!signedApk.exists()) {
            error(
                "Signed app-release.apk not found — configure keystore.properties or RELEASE_* env " +
                    "(unsigned app-release-unsigned.apk cannot be renamed for distribution)",
            )
        }
    }
}

tasks.named("preBuild") { dependsOn(copyReleaseNotes) }

tasks.register("printVersionName") {
    doLast {
        println(android.defaultConfig.versionName)
    }
}
