# Screen Wakelock Detector — release ProGuard rules

# Room (KSP-generated implementations)
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-dontwarn androidx.room.paging.**

# Hilt / Dagger
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keepclasseswithmembers class * {
    @dagger.* <methods>;
    @javax.inject.* <fields>;
}

# Glance app widget
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }

# App domain models and DB layer (reflection / serialization)
-keep class com.screenwakelock.detector.data.db.** { *; }
-keep class com.screenwakelock.detector.domain.model.** { *; }
