import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "kr.bluevisor.applens"
    compileSdk = 36
    flavorDimensions += "distribution"

    val admobTestAppId = "ca-app-pub-3940256099942544~3347511713"
    val admobTestBannerId = "ca-app-pub-3940256099942544/6300978111"

    val properties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { properties.load(it) }
    }

    val keystoreProperties = Properties()
    val keystorePropertiesFile = rootProject.file("keystore.properties")
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
    }

    fun resolveAdMobConfig(key: String, fallback: String): String {
        return providers.environmentVariable(key).orNull
            ?: providers.gradleProperty(key).orNull
            ?: properties.getProperty(key)
            ?: fallback
    }

    val admobReleaseAppId = resolveAdMobConfig(
        key = "ADMOB_APP_ID",
        fallback = admobTestAppId
    )
    val admobReleaseBannerId = resolveAdMobConfig(
        key = "ADMOB_BANNER_ID",
        fallback = admobTestBannerId
    )

    fun resolveSigningConfig(key: String): String? {
        return providers.environmentVariable(key).orNull
            ?: providers.gradleProperty(key).orNull
            ?: keystoreProperties.getProperty(key)
    }

    val releaseStoreFile = resolveSigningConfig("storeFile")
    val releaseStorePassword = resolveSigningConfig("storePassword")
    val releaseKeyAlias = resolveSigningConfig("keyAlias")
    val releaseKeyPassword = resolveSigningConfig("keyPassword")

    defaultConfig {
        applicationId = "kr.bluevisor.applens"
        minSdk = 24
        targetSdk = 36
        versionCode = 5
        versionName = "1.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    productFlavors {
        create("internal") {
            dimension = "distribution"
            buildConfigField("boolean", "IS_PACKAGE_VISIBILITY_LIMITED", "false")
        }

        create("play") {
            dimension = "distribution"
            buildConfigField("boolean", "IS_PACKAGE_VISIBILITY_LIMITED", "true")
        }
    }

    signingConfigs {
        create("release") {
            if (!releaseStoreFile.isNullOrBlank()) {
                storeFile = file(releaseStoreFile)
            }
            storePassword = releaseStorePassword
            keyAlias = releaseKeyAlias
            keyPassword = releaseKeyPassword
        }
    }

    buildTypes {
        debug {
            manifestPlaceholders["admobAppId"] = admobTestAppId
            buildConfigField("String", "ADMOB_BANNER_ID", "\"$admobTestBannerId\"")
        }

        release {
            isMinifyEnabled = false
            manifestPlaceholders["admobAppId"] = admobReleaseAppId
            buildConfigField("String", "ADMOB_BANNER_ID", "\"$admobReleaseBannerId\"")
            signingConfig = signingConfigs.getByName("release")
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
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    
    // Google AdMob
    implementation(libs.play.services.ads)
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
