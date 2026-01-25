import com.project.starter.easylauncher.filter.ColorRibbonFilter
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.com.android.application)
    alias(libs.plugins.org.jetbrains.kotlin.android)
    alias(libs.plugins.com.google.devtools.ksp)
    alias(libs.plugins.com.google.dagger.hilt.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.google.firebase.crashlytics)
    alias(libs.plugins.easylauncher)
    alias(libs.plugins.androidx.room)
}

android {
    namespace = "com.msmobile.visitas"
    compileSdk = libs.versions.android.sdk.get().toInt()

    defaultConfig {
        applicationId = "com.msmobile.visitas"
        minSdk = libs.versions.android.min.sdk.get().toInt()
        targetSdk = libs.versions.android.sdk.get().toInt()
        versionCode = requireEnvVariable(EnvKeys.VERSION_CODE).toInt()
        versionName = requireVersionName()

        testInstrumentationRunner = "com.msmobile.visitas.HiltTestRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file(requireEnvVariable(EnvKeys.KEYSTORE_FILE))
            storePassword = requireEnvVariable(EnvKeys.KEYSTORE_PASSWORD)
            keyAlias = requireEnvVariable(EnvKeys.KEYSTORE_ALIAS)
            keyPassword = requireEnvVariable(EnvKeys.KEYSTORE_PASSWORD)
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isDefault = true
            buildConfigField(
                "String",
                EnvKeys.ENCRYPTION_PASSPHRASE,
                "\"${System.getenv(EnvKeys.ENCRYPTION_PASSPHRASE) ?: ""}\""
            )
        }
        release {
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            buildConfigField(
                "String",
                EnvKeys.ENCRYPTION_PASSPHRASE,
                "\"${requireEnvVariable(EnvKeys.ENCRYPTION_PASSPHRASE)}\""
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
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
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

easylauncher {
    buildTypes {
        create("debug") {
            filters(
                customRibbon(
                    gravity = ColorRibbonFilter.Gravity.BOTTOM,
                    label = "DEV"
                )
            )

        }
        create("release") {
            enable(false)
        }
    }
}

dependencies {

    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.compose)
    implementation(libs.activity.compose)
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.compose.destinations.core)
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.play.services.location)
    implementation(libs.play.appupdate)
    implementation(libs.play.appupdate.ktx)
    implementation(libs.material.icons)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.androidx.security.crypto)
    implementation(libs.firebase.crashlytics)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    ksp(libs.room.compiler)
    ksp(libs.moshi.kotlin.codegen)
    ksp(libs.compose.destinations.ksp)
    ksp(libs.dagger.compiler)
    ksp(libs.hilt.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Add these testing dependencies
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.android.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    kspAndroidTest(libs.hilt.android.compiler)

    debugImplementation(libs.ui.tooling)
}

private object EnvKeys {
    const val VERSION_CODE = "VERSION_CODE"
    const val KEYSTORE_FILE = "KEYSTORE_FILE"
    const val KEYSTORE_PASSWORD = "KEYSTORE_PASSWORD"
    const val KEYSTORE_ALIAS = "KEYSTORE_ALIAS"
    const val ENCRYPTION_PASSPHRASE = "ENCRYPTION_PASSPHRASE"
}

private fun requireEnvVariable(key: String): String {
    return System.getenv(key) ?: error("$key environment variable is required for release builds")
}

private fun requireVersionName(): String {
    val versionPropsFile = file("${rootProject.projectDir}/version.properties")

    if (!versionPropsFile.exists()) {
        error("version.properties file not found at ${versionPropsFile.absolutePath}")
    }

    val versionProps = Properties().apply {
        versionPropsFile.inputStream().use { load(it) }
    }
    val versionName = versionProps.getProperty("versionName")
        ?: error("versionName property not found in version.properties")

    return versionName
}

