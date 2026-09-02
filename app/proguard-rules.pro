# Project specific ProGuard rules

# Preserve line numbers and attributes for debugging and reflection
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,Exceptions

# ── Moshi ───────────────────────────────────────────────────────────────────
-dontwarn com.squareup.moshi.**
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-keep class * extends com.squareup.moshi.JsonAdapter { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclasseswithmembers class * {
    @com.squareup.moshi.JsonClass <fields>;
}

# ── Retrofit & OkHttp ───────────────────────────────────────────────────────
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-dontwarn okio.**

# ── Room Database ───────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public abstract *;
}

# ── WorkManager ─────────────────────────────────────────────────────────────
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ── Google APIs & Drive & GSON ──────────────────────────────────────────────
-keep class com.google.api.services.drive.** { *; }
-keep class com.google.api.client.** { *; }
-keep class com.google.gson.** { *; }
-dontwarn com.google.api.client.**
-dontwarn com.google.common.**
-dontwarn javax.annotation.**
-dontwarn org.apache.http.**

# ── Jsoup ───────────────────────────────────────────────────────────────────
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**

# ── Project Domain & Update Models ──────────────────────────────────────────
-keep class com.qdash.domain.model.** { *; }
-keep class com.qdash.data.update.** { *; }
