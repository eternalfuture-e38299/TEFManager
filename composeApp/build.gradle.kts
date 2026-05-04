import com.android.build.api.dsl.ApplicationExtension
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlin.serialization)
}

val generateStrings by tasks.registering {
    description = "generated strings code"
    val out = layout.buildDirectory.dir("generated/strings")
    outputs.dir(out)

    StringsGenerator.generateForModule(
        moduleDir = layout.projectDirectory.asFile,
        packageName = "eternal.future.tefmanager.strings.generated"
    )
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    jvm {
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

    sourceSets {
        androidMain {
            dependencies {
                implementation(libs.compose.uiToolingPreview)
                implementation(libs.androidx.activity.compose)
                implementation(libs.ktor.client.android)
                implementation(libs.apkzlib)
                implementation(libs.apksig)
                implementation(libs.bcprov.jdk18on)
                implementation(libs.bcpkix.jdk18on)
                implementation(libs.material)
                compileOnly(libs.xposed.api)
                // implementation(fileTree(mapOf("dir" to "libs/android", "include" to listOf("*.jar", "*.aar"))))
                implementation(project(":composeApp:libs:android:aXML"))
                implementation(files("libs/android/ManifestEditor-2.0.jar"))
            }
        }
        commonMain {
            kotlin {
                srcDir(generateStrings.map { it.outputs })
            }
            dependencies {
                implementation(libs.kamel.image.default)
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
                implementation(libs.filekit.core)
                implementation(libs.filekit.dialogs.compose)
                implementation(libs.filekit.coil)
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

val androidApp = extensions.getByType(ApplicationExtension::class)
androidApp.apply {
    namespace = "eternal.future.tefmanager"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "eternal.future.tefmanager"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                file("proguard-rules.pro")
            )
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