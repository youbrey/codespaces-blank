# Native bridge harus tetap bisa dipanggil JNI, tapi nama kelas boleh dipendekkan
-keepclassmembers class com.docapp.core.security.NativeBridge {
    native <methods>;
}

# Acak nama kelas/paket secara agresif
-repackageclasses ''
-allowaccessmodification
-overloadaggressively

# Room & Hilt — jangan obfuscate generated code
-keep class * extends androidx.room.RoomDatabase
-keep @dagger.hilt.android.HiltAndroidApp class *

# Model domain tidak perlu disembunyikan (bukan bagian sensitif)
-keep class com.docapp.core.model.** { *; }

# Hilangkan logging di release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}
