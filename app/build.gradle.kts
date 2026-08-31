import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.devtoolsKsp)
}

val localProperties = Properties().apply {
    rootProject.file("local.properties")
        .takeIf { it.isFile }
        ?.inputStream()
        ?.use(::load)
}
val weatherApiKey = System.getenv("WEATHERAPI_API_KEY")
    ?.takeIf { it.isNotBlank() }
    ?: localProperties.getProperty("WEATHERAPI_API_KEY").orEmpty()
val weatherApiProxyUrl = System.getenv("WEATHERAPI_PROXY_URL")
    ?.takeIf { it.isNotBlank() }
    ?: localProperties.getProperty("WEATHERAPI_PROXY_URL")
        ?.takeIf { it.isNotBlank() }
    ?: "https://gardenspa-weather-api.pukpukyc96.chatgpt.site/api/weather"
fun String.asBuildConfigString(): String =
    "\"${trim().replace("\\", "\\\\").replace("\"", "\\\"")}\""

System.getenv("BOOKEEPER_BUILD_DIR")
    ?.takeIf { it.isNotBlank() }
    ?.let { layout.buildDirectory.set(file(it)) }

android {
    namespace = "ru.samates.gardenspa"
    compileSdk = 36

    defaultConfig {
        applicationId = "ru.samates.gardenspa"
        minSdk = 26
        targetSdk = 34
        versionCode = 40
        versionName = "1.0.39"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField(
            "String",
            "WEATHERAPI_API_KEY",
            "".asBuildConfigString()
        )
        buildConfigField(
            "String",
            "WEATHERAPI_PROXY_URL",
            weatherApiProxyUrl.asBuildConfigString()
        )

    }

    buildTypes {
        debug {
            buildConfigField(
                "String",
                "WEATHERAPI_API_KEY",
                weatherApiKey.asBuildConfigString()
            )
        }
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
    buildFeatures {
        compose = true
        buildConfig = true
    }

}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("androidx.datastore:datastore-preferences-core:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.4")
    implementation("androidx.compose.ui:ui:1.5.4")
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation("androidx.security:security-crypto:1.1.0")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("io.github.boguszpawlowski.composecalendar:composecalendar:1.4.0")
    implementation("io.github.boguszpawlowski.composecalendar:kotlinx-datetime:1.4.0")
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.runtime.livedata)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.ui)
    implementation(libs.androidx.datastore.core)
    implementation(libs.androidx.datastore.preferences.core)
    annotationProcessor(libs.androidx.room.room.compiler)
    ksp(libs.androidx.room.room.compiler)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.navigation:navigation-compose:2.9.5")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.ui.graphics)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
