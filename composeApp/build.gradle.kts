import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    jvm()
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.android)
            implementation(fileTree(mapOf("dir" to "libs/android", "include" to listOf("*.jar", "*.aar"))))
        }
        commonMain {
            kotlin {
                srcDir("build/generated/strings")
            }
            dependencies {
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.compose.ui)
                implementation(libs.compose.components.resources)
                implementation(libs.compose.uiToolingPreview)
                implementation(libs.androidx.lifecycle.viewmodelCompose)
                implementation(libs.androidx.lifecycle.runtimeCompose)
                implementation(libs.voyager.navigator)
                implementation(libs.voyager.transitions)
                implementation(libs.material.icons.extended)
                implementation(libs.material.kolor)
                implementation(libs.kermit)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)
                implementation(libs.colorpicker.compose)
                implementation(libs.kotlinx.serialization.protobuf)
                implementation(libs.okio.fakefilesystem)
                implementation(libs.okio)
                implementation(libs.ktor.client.core)
            }
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.ktor.client.cio)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

tasks.register("generateStrings") {
    doLast {
        StringsGenerator.generateForModule(
            moduleDir = projectDir,
            packageName = "eternal.future.tefmanager.strings.generated"
        )
    }
}

// 修复：为所有 Kotlin 编译任务添加依赖
tasks.matching { it.name.startsWith("compileKotlin") }.configureEach {
    dependsOn("generateStrings")
}

// 或者更精确的方式（推荐）：
afterEvaluate {
    listOf(
        "compileKotlinCommon",
        "compileKotlinAndroid",
        "compileKotlinJvm",
        "compileKotlinIosX64",
        "compileKotlinIosArm64",
        "compileKotlinIosSimulatorArm64"
    ).forEach { taskName ->
        tasks.findByName(taskName)?.dependsOn("generateStrings")
    }
}

android {
    namespace = "eternal.future.tefmanager"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "eternal.future.tefmanager"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("tefmanager.p12")
            storePassword = "EternalFuture@2026"
            keyAlias = "TEFManager"
            keyPassword = "EternalFuture@2026"
            storeType = "PKCS12"
        }

        getByName("debug") {
            storeFile = file("tefmanager.p12")
            storePassword = "EternalFuture@2026"
            keyAlias = "TEFManager"
            keyPassword = "EternalFuture@2026"
            storeType = "PKCS12"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "eternal.future.tefmanager.MainKt"

        nativeDistributions {
            targetFormats(
                TargetFormat.Dmg,    // macOS
                TargetFormat.Msi,    // Windows
                // TargetFormat.Deb,    // Linux Debian
                // TargetFormat.Rpm,    // Linux RPM
                TargetFormat.AppImage // Linux AppImage
            )

            packageName = "TEFManager"
            packageVersion = "1.0.0"

            // macOS 配置
            macOS {
                bundleID = "eternal.future.tefmanager"
                iconFile.set(File("src/jvmMain/resources/icon.icns"))
            }

            // Windows 配置
            windows {
                iconFile.set(File("src/jvmMain/resources/icon.ico"))
                menuGroup = "TEFManager"
            }

            // Linux 配置
            linux {
                iconFile.set(File("src/jvmMain/resources/icon.png"))
                menuGroup = "TEFManager"
            }
        }
    }
}
