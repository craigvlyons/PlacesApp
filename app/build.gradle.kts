
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.secrets.gradle)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val releaseSigningPropertiesFile = rootProject.file("release-signing.properties")
val releaseSigningProperties = Properties().apply {
    if (releaseSigningPropertiesFile.isFile) {
        releaseSigningPropertiesFile.inputStream().use(::load)
    }
}

fun requireReleaseSigningProperty(name: String): String =
    releaseSigningProperties.getProperty(name)?.takeIf(String::isNotBlank)
        ?: throw GradleException(
            "Missing '$name' in ${releaseSigningPropertiesFile.name}. " +
                "Copy release-signing.properties.example and keep the completed file out of Git."
        )

android {
    namespace = "com.example.favoriteplaces"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        // The legacy app must remain installed during verified data migration. Its
        // signing key was lost, so the replacement needs a distinct permanent ID.
        applicationId = "com.personal.favoriteplaces"
        minSdk = 24
        targetSdk = 37
        versionCode = 13
        versionName = "3.2.8"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        if (releaseSigningPropertiesFile.isFile) {
            create("release") {
                storeFile = rootProject.file(requireReleaseSigningProperty("storeFile"))
                storePassword = requireReleaseSigningProperty("storePassword")
                keyAlias = requireReleaseSigningProperty("keyAlias")
                keyPassword = requireReleaseSigningProperty("keyPassword")
            }
        }
    }
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    sourceSets {
        getByName("androidTest").assets.directories.add("$projectDir/schemas")
    }
}

dependencies {
    implementation(libs.gson)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    // Google Maps and Places
    implementation(libs.maps.compose)
    implementation(libs.places)


    // Compose dependencies
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.androidx.hilt.navigation.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(platform(libs.kotlinx.serialization.bom))

    //Dagger - Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Room
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    // Kotlin Extensions and Coroutines support for Room
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.datastore.preferences)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}

secrets{
    defaultPropertiesFileName = "local.defaults.properties"
}
