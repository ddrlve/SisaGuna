# Release shrinking (R8). Retrofit, OkHttp and kotlinx.serialization ship consumer rules; these
# lines cover what they can't know about: our own @Serializable DTOs and the Retrofit interfaces.

# kotlinx.serialization: keep generated serializers and Companion.serializer() of our DTOs.
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.sisaguna.android.**$$serializer { *; }
-keepclassmembers class com.sisaguna.android.** {
    *** Companion;
}
-keepclasseswithmembers class com.sisaguna.android.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit: keep the service interfaces (they are only used through reflection-built proxies).
-keep interface com.sisaguna.android.data.remote.ApiService { *; }
-keep interface com.sisaguna.android.data.remote.RefreshApi { *; }

# Stack traces stay readable in crash reports.
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile

# OkHttp optional TLS providers are looked up reflectively and may be absent.
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**

# Tink (used by androidx.security:security-crypto for the token store) references compile-time-only annotations.
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn com.google.crypto.tink.**
