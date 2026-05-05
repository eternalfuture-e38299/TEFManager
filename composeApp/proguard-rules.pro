# 保留 aXML 库的所有类和方法
-keep class com.apk.axml.** { *; }
-keepclassmembers class com.apk.axml.** { *; }
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*

# 如果 aXML 库使用了泛型反射，还需要保留泛型信息
-keep class * implements java.lang.reflect.ParameterizedType
-keep class * extends java.lang.reflect.Type
-keep class eternal.future.tefmanager.MainHook { *; }
-keep class eternal.future.tefmanager.Platform { *; }