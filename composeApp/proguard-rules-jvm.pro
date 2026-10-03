# 保留 Kotlin 序列化
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

# 保留 Compose 运行时
-keep class org.jetbrains.skiko.** { *; }

# 保留 Okio 和 Ktor 相关
-keep class okio.** { *; }
-keep class io.ktor.** { *; }

# 保留泛型签名（库可能需要的）
-keepattributes Signature, InnerClasses, EnclosingMethod

# 保留所有 Kotlin 协程类（防止被混淆）
-keep class kotlinx.coroutines.** {
    *;
}
-keep class kotlin.coroutines.** {
    *;
}

# 保留协程内部状态机类（关键！）
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ===== DBus 相关（关键！解决你的错误） =====
# 保留 DBus 核心
-keep class org.freedesktop.dbus.** { *; }
-keep class org.freedesktop.DBus.** { *; }

# 保留传输层实现
-keep class org.freedesktop.dbus.transport.** { *; }

# 保留 JNA（DBus 依赖）
-keep class com.sun.jna.** { *; }
-keep class com.sun.jna.platform.** { *; }

# 保留 ServiceLoader 服务
-keep class services.** { *; }

# FileKit
-keep class io.github.vinceglb.filekit.** { *; }

# 忽略不必要的警告
-dontwarn kotlinx.datetime.**
-dontwarn org.slf4j.**
-dontwarn com.sun.jna.**
-dontwarn org.freedesktop.dbus.**