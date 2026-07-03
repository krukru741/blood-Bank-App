# Add project specific ProGuard rules here.

# ── Firebase ──────────────────────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# ── Firestore model classes — prevent field name obfuscation ──────────────────
# Add your DTO packages here once you create them in Step 2:
# -keep class com.example.bloodbank.data.remote.model.** { *; }

# ── Hilt ──────────────────────────────────────────────────────────────────────
-keepnames @dagger.hilt.android.lifecycle.HiltViewModel class * extends androidx.lifecycle.ViewModel

# ── Parcelable ────────────────────────────────────────────────────────────────
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# ── Kotlin Coroutines ─────────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ── Glide ─────────────────────────────────────────────────────────────────────
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { *; }

# ── Retrofit / OkHttp (uncomment if you add REST API later) ──────────────────
# -dontwarn okhttp3.**
# -keep class retrofit2.** { *; }
