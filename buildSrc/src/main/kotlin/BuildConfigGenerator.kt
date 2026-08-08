/*******************************************************************************
 * TEFManager - BuildConfigGenerator
 * Copyright (C) 2026 eternalfuture-e38299
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Author: eternalfuture-e38299
 * GitHub: https://github.com/eternalfuture-e38299
 * Created: 2026/8/6
 *******************************************************************************/

import com.squareup.kotlinpoet.*
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.reflect.KClass

object BuildConfigGenerator {

    data class BuildConfig(
        val versionName: String,
        val versionCode: Int,
        val kernelVersion: String,
        val tefloaderVersion: String,
        val moduleVersions: Map<String, String>,
        val buildTime: LocalDateTime = LocalDateTime.now()
    )

    fun generate(
        config: BuildConfig,
        outputDir: File,
        packageName: String = "eternal.future.tefmanager",
        className: String = "BuildConfig"
    ) {
        // 清理并创建输出目录
        outputDir.deleteRecursively()
        outputDir.mkdirs()

        // 生成 BuildConfig 类
        val fileSpec = createBuildConfigFile(config, packageName, className)
        fileSpec.writeTo(outputDir)

        println("✅ Generated BuildConfig: $className in $packageName")
        println("   Version: ${config.versionName} (${config.versionCode})")
        println("   Kernel: ${config.kernelVersion}")
        println("   Modules: ${config.moduleVersions.size}")
        println("   Build Date: ${config.buildTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}")
    }

    private fun createBuildConfigFile(
        config: BuildConfig,
        packageName: String,
        className: String
    ): FileSpec {
        // 使用 object 而不是 class
        val objectBuilder = TypeSpec.objectBuilder(className)
            .addAnnotation(
                AnnotationSpec.builder(Suppress::class)
                    .addMember("\"unused\"")
                    .addMember("\"PropertyName\"")
                    .build()
            )

        // ===== 添加常量 =====

        // 版本信息 - 注意字符串需要用 %S 格式化，会自动添加引号
        addConstant(objectBuilder, "VERSION_NAME", config.versionName, String::class)
        addConstant(objectBuilder, "VERSION_CODE", config.versionCode, Int::class)

        // 内核版本
        addConstant(objectBuilder, "KERNEL_VERSION", config.kernelVersion, String::class)
        addConstant(objectBuilder, "TEFLOADER_VERSION", config.tefloaderVersion, String::class)

        // 编译日期
        val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.of("Asia/Shanghai"))
        addConstant(objectBuilder, "BUILD_DATE", config.buildTime.format(dateTimeFormatter) + " Asia/Shanghai (UTC+8)", String::class)

        // 编译时间戳（保留用于版本比较）
        addConstant(objectBuilder, "BUILD_TIMESTAMP", config.buildTime.toEpochSecond(java.time.ZoneOffset.UTC), Long::class)

        // 模块版本（直接在 object 中定义）
        config.moduleVersions.forEach { (module, version) ->
            val constantName = module.uppercase(Locale.ROOT).replace("-", "_").replace(".", "_")
            addConstant(objectBuilder, constantName, version, String::class)
        }

        // 添加获取模块版本的方法
        addModuleVersionFunction(objectBuilder, config)

        // 添加获取完整版本信息的方法
        addVersionInfoFunction(objectBuilder, config)

        // 添加构建信息字符串
        addBuildInfoString(objectBuilder)

        // 添加 toString 方法
        addToStringMethod(objectBuilder)

        // 创建文件
        return FileSpec.builder(packageName, className)
            .addType(objectBuilder.build())
            .build()
    }

    // ============================================
    // 辅助方法
    // ============================================

    private fun addConstant(
        builder: TypeSpec.Builder,
        name: String,
        value: Any,
        type: KClass<*>
    ) {
        builder.addProperty(
            PropertySpec.builder(name, type.asClassName())
                .addModifiers(KModifier.CONST)
                .initializer(formatInitializer(value))
                .build()
        )
    }

    private fun formatInitializer(value: Any): String {
        return when (value) {
            is String -> "\"$value\""
            is Long -> "${value}L"
            is Boolean -> value.toString()
            is Int -> value.toString()
            else -> value.toString()
        }
    }

    private fun addModuleVersionFunction(
        builder: TypeSpec.Builder,
        config: BuildConfig
    ) {
        val funBuilder = FunSpec.builder("getModuleVersion")
            .returns(String::class)
            .addParameter("moduleName", String::class)
            .beginControlFlow("return when (moduleName)")

        config.moduleVersions.forEach { (module, _) ->
            val constantName = module.uppercase(Locale.ROOT).replace("-", "_").replace(".", "_")
            funBuilder.addStatement("\"${module}\" -> $constantName")
        }

        funBuilder.addStatement("else -> \"unknown\"")
        funBuilder.endControlFlow()

        builder.addFunction(funBuilder.build())
    }

    private fun addVersionInfoFunction(
        builder: TypeSpec.Builder,
        config: BuildConfig
    ) {
        val moduleInfo = if (config.moduleVersions.isEmpty()) {
            "    (none)"
        } else {
            config.moduleVersions.entries.joinToString("\n    ") { "${it.key}: ${it.value}" }
        }

        val funBuilder = FunSpec.builder("getVersionInfo")
            .returns(String::class)
            .addCode(
                """
                return ${"\"\"\""}
                  Version: ${'$'}VERSION_NAME
                  Version Code: ${'$'}VERSION_CODE
                  Kernel: ${'$'}KERNEL_VERSION
                  Build Date: ${'$'}BUILD_DATE
                  Modules:
                    $moduleInfo
                ${"\"\"\""}.trimIndent()
                """.trimIndent()
            )
            .build()

        builder.addFunction(funBuilder)
    }

    private fun addBuildInfoString(builder: TypeSpec.Builder) {
        val funBuilder = FunSpec.builder("getBuildInfo")
            .returns(String::class)
            .addStatement(
                "return VERSION_NAME + \" (build \" + VERSION_CODE + \") | \" + KERNEL_VERSION + \" | \" + BUILD_DATE"
            )
            .build()

        builder.addFunction(funBuilder)
    }

    private fun addToStringMethod(builder: TypeSpec.Builder) {
        val funBuilder = FunSpec.builder("toString")
            .addModifiers(KModifier.OVERRIDE)
            .returns(String::class)
            .addStatement("return getBuildInfo()")
            .build()

        builder.addFunction(funBuilder)
    }
}