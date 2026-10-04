#############################################
# GENERAL SAFE RULES
#############################################

# Keep your app (start broad, then reduce later)
-keep class pynith.apps.** { *; }

# Keep annotations (very important)
-keepattributes *Annotation*

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }

#############################################
# OKHTTP
#############################################
-keepattributes Signature
-keepattributes Exceptions
-dontwarn okhttp3.**
-dontwarn okio.**

#############################################
# GSON (if used anywhere indirectly)
#############################################
-keep class com.google.gson.** { *; }
-keep class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

#############################################
# UCROP (image library)
#############################################
-keep class com.yalantis.ucrop.** { *; }

#############################################
# RXJAVA
#############################################
-dontwarn io.reactivex.**

#############################################
# ANDROIDX (safe keep)
#############################################
-dontwarn androidx.**


# --- existing rules above ---

#############################################
# WORKMANAGER + ROOM (CRITICAL FIX)
#############################################
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**

-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

-keep class * extends androidx.room.RoomDatabase { *; }
-keep class **_Impl { *; }

-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.Entity class * { *; }

-keepclassmembers class * {
    @androidx.room.* <fields>;
}

-keepattributes *Annotation*