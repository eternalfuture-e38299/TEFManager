# 保留 aXML 库的所有类和方法
-keep class com.apk.axml.** { *; }
-keepclassmembers class com.apk.axml.** { *; }
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*

# 如果 aXML 库使用了泛型反射，还需要保留泛型信息
-keep class * implements java.lang.reflect.ParameterizedType
-keep class * extends java.lang.reflect.Type
-keep class eternal.future.tefmanager.MainHook { *; }
-keep class eternal.future.tefmanager.Platform { *; }

# ModPage 页面 JAR 按类名反射加载；Voyager 也依赖类名与签名
-keep class eternal.future.tefmanager.** { *; }
-keep interface eternal.future.tefmanager.** { *; }

# ============ 核心规则 ============
# 完全不优化指定包
-keep class bitter.jnibridge.** { *; }
-keepclassmembers class bitter.jnibridge.** { *; }

-keep class com.and.games505.TerrariaPaid.** { *; }
-keepclassmembers class com.and.games505.TerrariaPaid.** { *; }

-keep class com.google.androidgamesdk.** { *; }
-keepclassmembers class com.google.androidgamesdk.** { *; }

-keep class com.unity3d.player.** { *; }
-keepclassmembers class com.unity3d.player.** { *; }

-keep class org.fmod.** { *; }
-keepclassmembers class org.fmod.** { *; }

# ============ 忽略警告 ============
-dontwarn com.google.android.gms.tasks.**
-dontwarn com.google.android.play.core.assetpacks.**
-dontwarn com.google.androidgamesdk.**
-dontwarn com.unity3d.player.**


# ModPage 页面 JAR 由外部 Mod 编译，运行时会直接引用 Kotlin 运行时类。
# R8 会因为宿主力自身已内联这些调用而把它们裁掉，导致外部页面
# 报 "Failed resolution of: Lkotlin/jvm/internal/Intrinsics;"。
-keep class kotlin.** { *; }
-keep interface kotlin.** { *; }
