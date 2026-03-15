# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keep class com.moneytracker.app.data.local.database.entities.** { *; }
-keep class com.moneytracker.app.domain.model.** { *; }

# Cloud backup (Google Sign-In + Drive REST) uses reflection and @Key field mapping.
# Keep these classes/member names in release to avoid runtime crashes/parse issues.
-keepattributes Signature,RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations,EnclosingMethod,InnerClasses

-keep class com.google.android.gms.auth.api.signin.** { *; }
-keep class com.google.api.client.googleapis.extensions.android.gms.auth.** { *; }
-keep class com.google.api.client.googleapis.auth.oauth2.** { *; }
-keep class com.google.api.client.http.** { *; }
-keep class com.google.api.client.json.** { *; }
-keep class com.google.api.client.util.** { *; }
-keep class com.google.api.services.drive.** { *; }
-keep class com.google.gson.** { *; }

-keepclassmembers class * {
	@com.google.api.client.util.Key <fields>;
}

-dontwarn org.apache.http.**
