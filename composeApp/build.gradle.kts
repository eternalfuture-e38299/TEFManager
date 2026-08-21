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

object AppConfig {
    const val VERSION_NAME : String = "1.1.0"
    const val VERSION_CODE : Int = 2026082100
    const val KERNEL_VERSION = "1.0.1"
    const val TEFLOADER_VERSION = "1.0.1"
    val MODULE_VERSIONS = mapOf(
        "LanguagePackExtension" to "1.0.2",
        "TexturePackExtension" to "1.0.2",
        "FontPackExtension" to "1.0.0"
    )
    const val IS_INLINE_GAME = false
    const val INLINE_GAME_VERSION = "1.4.5.6.4"
    const val INLINE_GAME_VERSION_CODE = 301543
}

val buildConfig = BuildConfigGenerator.BuildConfig(
    AppConfig.VERSION_NAME,
    AppConfig.VERSION_CODE,
    AppConfig.KERNEL_VERSION,
    AppConfig.TEFLOADER_VERSION,
    AppConfig.MODULE_VERSIONS,
    isInlineGame = AppConfig.IS_INLINE_GAME,
    inlineGameVersion = AppConfig.INLINE_GAME_VERSION,
    inlineGameVersionCode = AppConfig.INLINE_GAME_VERSION_CODE
)

val generateCode by tasks.registering {
    description = "Generated strings and build config code"

    // 设置输出目录
    val stringsOutputDir = layout.buildDirectory.dir("generated/strings")
    val buildConfigOutputDir = layout.buildDirectory.dir("generated/buildconfig")

    outputs.dir(stringsOutputDir)
    outputs.dir(buildConfigOutputDir)

    // 执行生成任务

    // 生成字符串代码
    StringsGenerator.generate(
        moduleDir = layout.projectDirectory.asFile,
        packageName = "eternal.future.tefmanager.strings.generated"
    )

    // 生成 BuildConfig
    BuildConfigGenerator.generate(
        config = buildConfig,
        outputDir = buildConfigOutputDir.get().asFile,
        packageName = "eternal.future.tefmanager"
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

                if (AppConfig.IS_INLINE_GAME) {
                    implementation(files("libs/android/terraria.jar"))
                }
            }
        }
        commonMain {
            kotlin {
                srcDir(generateCode.map { it.outputs })
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
                implementation(libs.kmp.zip)
                implementation(libs.kmp.zip.okio)
                implementation(libs.kmp.zip.kotlinx)
            }
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(libs.desktop.jvm.linux.x64)
            implementation(libs.desktop.jvm.macos.x64)
            implementation(libs.desktop.jvm.windows.x64)
            implementation(libs.desktop.jvm.macos.arm64)

            // implementation(compose.desktop.currentOs)
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
    compileSdk = 37

    defaultConfig {
        applicationId = "eternal.future.tefmanager"
        minSdk = 24
        targetSdk = 37
        versionCode = AppConfig.VERSION_CODE
        versionName = AppConfig.VERSION_NAME

        if (AppConfig.IS_INLINE_GAME) {
            ndk {
                abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a"))
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                file("proguard-rules.pro")
            )
        }
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("tefmanager.p12")
            keyAlias = "TEFManager"
            storePassword = "EternalFuture@2026"
            keyPassword = "EternalFuture@2026"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }

    packaging {
        if (!AppConfig.IS_INLINE_GAME) {
            jniLibs {
                excludes += listOf(
                    "**/libil2cpp.so",
                    "**/libmain.so",
                    "**/libunity.so",
                    "**/libc++_shared.so"
                )
            }

            resources.excludes += "assets/bin/Data/**"
        }

        resources {
            // 排除冲突的 LICENSE 文件
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/NOTICE.md"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/*.md"
            excludes += "META-INF/DEPENDENCIES"
            // 如果某些文件需要保留，可以使用 pickFirsts
            // pickFirsts += "META-INF/LICENSE.md"
        }
    }


    if (AppConfig.IS_INLINE_GAME) {
        androidResources {
            noCompress += mutableListOf(
                "assets/bin/Data/data.unity3d",
                "assets/bin/Data/resources.resource",
                "assets/bin/Data/unity default resources"
            )
        }
    }
}


dependencies {
    debugImplementation(libs.compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "eternal.future.tefmanager.MainKt"

        buildTypes.release.proguard {
            // 启用 ProGuard
            isEnabled.set(true)
            obfuscate.set(true)
            optimize.set(true)
            configurationFiles.from(project.file("proguard-rules-jvm.pro"))
        }

        nativeDistributions {
            targetFormats(
                TargetFormat.Dmg,    // macOS
                TargetFormat.Msi,    // Windows
                TargetFormat.AppImage // Linux AppImage
            )

            packageName = "TEFManager"
            packageVersion = AppConfig.VERSION_NAME

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
                iconFile.set(File("src/jvmMain/resources/icon.webp"))
                menuGroup = "TEFManager"
            }
        }
    }
}
