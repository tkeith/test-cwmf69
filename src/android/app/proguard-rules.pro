# Delayed Messaging System ProGuard Rules
# Version: 1.0.0

#-------------------
# General Settings
#-------------------
-optimizationpasses 5
-dontusemixedcaseclassnames
-keepattributes SourceFile,LineNumberTable
-printmapping mapping.txt
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*

#-------------------
# Kotlin
#-------------------
-keepclassmembers class **.*$Companion { *; }
-keepclassmembers class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }
-dontwarn kotlin.reflect.**

#-------------------
# Message System
#-------------------
# Preserve message models and core functionality
-keep class com.delayedmessaging.android.domain.model.** { *; }
-keep class com.delayedmessaging.android.queue.** { *; }
-keep class com.delayedmessaging.android.timing.** { *; }
# Prevent optimization of critical timing code
-dontoptimize class com.delayedmessaging.android.timing.**

#-------------------
# WebSocket
#-------------------
-keep class com.delayedmessaging.android.websocket.** { *; }
-keepclassmembers class * implements javax.websocket.Endpoint { *; }
-keepclassmembers class * implements org.java_websocket.client.WebSocketClient { *; }
-dontwarn org.java_websocket.**

#-------------------
# Network
#-------------------
-keepattributes Signature
-keepclasseswithmembers class * { @retrofit2.http.* <methods>; }
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

#-------------------
# Security
#-------------------
-keep class com.delayedmessaging.android.security.** { *; }
-keepclassmembers class * extends javax.crypto.SecretKey { *; }
-keepattributes *Annotation*
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes Signature
-keepattributes EnclosingMethod

#-------------------
# Firebase Cloud Messaging
#-------------------
-keep class com.google.firebase.messaging.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

#-------------------
# JSON Serialization
#-------------------
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

#-------------------
# Database
#-------------------
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room.Dao { *; }
-keep @androidx.room.Entity class *
-keepclassmembers @androidx.room.Entity class * { *; }

#-------------------
# Crash Reporting
#-------------------
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
-keep class com.google.firebase.crashlytics.** { *; }

#-------------------
# View Binding
#-------------------
-keep class * implements androidx.viewbinding.ViewBinding {
    public static ** bind(android.view.View);
    public static ** inflate(android.view.LayoutInflater);
}

#-------------------
# Coroutines
#-------------------
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

#-------------------
# Debugging Support
#-------------------
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable
-printusage usage.txt
-printseeds seeds.txt