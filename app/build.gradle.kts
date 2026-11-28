import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// 版本號只從根目錄 version.properties 讀。以前 versionCode/versionName 寫死在這裡，
// 每次改版都得記著動 build 檔；多一個 -test.N 之後更不可能靠人腦同步。
val versionProps = Properties().apply {
    rootProject.file("version.properties").inputStream().use { load(it) }
}
val appVersionCode = versionProps.getProperty("versionCode")?.trim()?.toIntOrNull()
    ?: error("version.properties 缺少合法的 versionCode")
val appVersionName = versionProps.getProperty("versionName")?.trim().orEmpty()

// 「檢查更新」用 AppVersion 解析這個字串；解析不了就永遠回「版本資訊無法辨識」。
// 在設定階段擋下來，比等使用者回報「檢查更新壞了」便宜。
require(Regex("""^\d+(\.\d+){1,3}(-[0-9A-Za-z.\-]+)?$""").matches(appVersionName)) {
    "versionName \"$appVersionName\" 無法被 data/update/AppVersion.kt 解析"
}

android {
    namespace = "com.yuzlyn.mdeditor"
    // M3 Expressive alpha26 / Compose 1.12 declare minCompileSdk 37;
    // targetSdk stays at 36 as required by the platform doctrine.
    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }

    defaultConfig {
        applicationId = "com.yuzlyn.mdeditor"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // 這個專案一直用 debug 簽章發佈（v1.0.0 起的 APK 都是同一張憑證）。
            // 沿用同一張：已經裝過舊版的人可以直接覆蓋升級，換簽章就得先卸載。
            // 要改用私人 keystore 的話，這裡換成自己的 signingConfig 即可。
            signingConfig = signingConfigs.getByName("debug")
            // 開 R8：release 從 36 MB 降到十幾 MB，同時把類名、方法名改掉，
            // 反編譯後看不到可讀的結構。風險集中在反射點與 WebView 橋接，
            // keep 規則見 proguard-rules.pro；真機冒煙由使用者驗收。
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    sourceSets {
        getByName("androidTest") {
            // 測試樣本只留一份：androidTest 直接吃 test 的資源目錄。
            // 兩份副本以前靠手工同步，實測三支樣本 md5 一直相同，等於白養一份。
            // 路徑前綴必須保留 books/，ReaderRenderingTest 是按 assets.open("books/…") 讀的。
            assets.srcDir("src/test/resources")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.material.kolor)
    implementation(libs.androidx.documentfile)
    implementation(libs.pdfbox.android)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}