# Mizu ProGuard Rules

# Hilt
-keepclassmembers,allowobfuscation class * {
    @dagger.hilt.android.lifecycle.HiltViewModel <init>(...);
}
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Room
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# Media3
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# NextLib
-keep class io.github.anilbeesetti.** { *; }
-dontwarn io.github.anilbeesetti.**

# LibVLC
-keep class org.videolan.** { *; }
-dontwarn org.videolan.**

# Coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Coil
-keep class coil.** { *; }

# DataStore
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite { *; }

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable
-keep class * implements android.os.Parcelable { *; }
-keep class * implements java.io.Serializable { *; }

# Native methods / JNI
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}
