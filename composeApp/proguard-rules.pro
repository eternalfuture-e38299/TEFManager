# 保留 aXML 库的所有类和方法
-keep class com.apk.axml.** { *; }
-keepclassmembers class com.apk.axml.** { *; }
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*

# 如果 aXML 库使用了泛型反射，还需要保留泛型信息
-keep class * implements java.lang.reflect.ParameterizedType
-keep class * extends java.lang.reflect.Type
-keep class eternal.future.tefmanager.MainHook { *; }
-keep class eternal.future.tefmanager.Platform { *; }

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