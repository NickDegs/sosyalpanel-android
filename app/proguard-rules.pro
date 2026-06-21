# Room
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keepclassmembers class * { @androidx.room.* <methods>; }
# Play Billing
-keep class com.android.billingclient.** { *; }
# Kotlin metadata
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
