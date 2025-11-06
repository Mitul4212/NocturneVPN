# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Glide Image Loading
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

# SearchView
-keep class androidx.appcompat.widget.SearchView { *; }

# Your App Specific Classes
-keep public class com.nocturnevpn.** { *; }
-keep public class com.nocturnevpn.view.** { *; }
-keep public class com.nocturnevpn.models.** { *; }

# VPN Connection Status Manager
-keep class com.nocturnevpn.view.managers.ConnectionStatusManager { *; }
-keep class com.nocturnevpn.view.managers.VPNManager { *; }
-keepclassmembers class com.nocturnevpn.view.managers.VPNManager { *; }

# Keep VPNManager methods that use new VPN API (ConfigParser, VPNLaunchHelper, ProfileManager)
-keepclassmembers class com.nocturnevpn.view.managers.VPNManager {
    public void startVpn();
    public void prepareVPN();
    public void stopVPN();
}

# Crashlytics
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# Fix R8 build issue
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Fix gson TypeToken crash
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken
-keep public class * implements java.lang.reflect.Type

# Retrofit does reflection on generic parameters. InnerClasses is required to use Signature and
# EnclosingMethod is required to use InnerClasses.
-keepattributes Signature, InnerClasses, EnclosingMethod

# Retrofit does reflection on method and parameter annotations.
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Keep annotation default values (e.g., retrofit2.http.Field.encoded).
-keepattributes AnnotationDefault

# Retain service method parameters when optimizing.
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Ignore annotation used for build tooling.
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# Ignore JSR 305 annotations for embedding nullability information.
-dontwarn javax.annotation.**

# Guarded by a NoClassDefFoundError try/catch and only used when on the classpath.
-dontwarn kotlin.Unit

# Top-level functions that can only be used by Kotlin.
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# With R8 full mode, it sees no subtypes of Retrofit interfaces since they are created with a Proxy
# and replaces all potential values with null. Explicitly keeping the interfaces prevents this.
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

# Keep inherited services.
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface * extends <1>

# With R8 full mode generic signatures are stripped for classes that are not
# kept. Suspend functions are wrapped in continuations where the type argument
# is used.
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# R8 full mode strips generic signatures from return types if not kept.
-if interface * { @retrofit2.http.* public *** *(...); }
-keep,allowoptimization,allowshrinking,allowobfuscation class <3>

# With R8 full mode generic signatures are stripped for classes that are not kept.
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Facebook SDK
-keep class com.facebook.** { *; }
-keep class com.facebook.android.** { *; }

# Lottie Animation
-keep class com.airbnb.lottie.** { *; }

# VPN Library - Critical for VPN functionality - Keep everything
-keep class de.blinkt.openvpn.** { *; }
-keep class de.blinkt.openvpn.core.** { *; }
-keep class de.blinkt.openvpn.api.** { *; }
-keep class de.blinkt.openvpn.utils.** { *; }

# Keep all classes and members in the entire de.blinkt.openvpn package
-keep class de.blinkt.openvpn.** { *; }
-keepclassmembers class de.blinkt.openvpn.** { *; }

# VPN Service - Must keep for VPN functionality
-keep class de.blinkt.openvpn.core.OpenVPNService { *; }
-keep class de.blinkt.openvpn.core.VpnStatus { *; }
-keep class de.blinkt.openvpn.core.VPNLaunchHelper { *; }
-keep class de.blinkt.openvpn.core.ProfileManager { *; }
-keep class de.blinkt.openvpn.core.ConfigParser { *; }
-keep class de.blinkt.openvpn.VpnProfile { *; }

# VPN Native Library Support
-keep class de.blinkt.openvpn.core.NativeUtils { *; }
-keep class de.blinkt.openvpn.core.ConnectionStatus { *; }
-keep class de.blinkt.openvpn.core.IOpenVPNServiceInternal { *; }

# Keep VPN callback interfaces
-keep interface de.blinkt.openvpn.core.VpnStatus$* { *; }
-keep interface de.blinkt.openvpn.core.IOpenVPNServiceInternal$* { *; }

# Keep VPN status enums
-keepclassmembers enum de.blinkt.openvpn.core.ConnectionStatus { *; }

# VPN API Service
-keep class de.blinkt.openvpn.api.ExternalOpenVPNService { *; }
-keep class de.blinkt.openvpn.api.IOpenVPNAPIService { *; }
-keep class de.blinkt.openvpn.api.APIVpnProfile { *; }

# Harden AIDL and internal interfaces (Play minify)
-keep class de.blinkt.openvpn.core.IOpenVPNServiceInternal { *; }
-keep class de.blinkt.openvpn.core.IOpenVPNServiceInternal$Stub { *; }
-keep class de.blinkt.openvpn.api.IOpenVPNAPIService { *; }
-keepattributes *Annotation*,InnerClasses,EnclosingMethod,Signature

# Keep all OpenVPN classes and methods - R8 is very aggressive
# Note: OpenVpnApi is deprecated - using ConfigParser, VPNLaunchHelper, ProfileManager instead
-keep class de.blinkt.openvpn.** { *; }
-keepclassmembers class de.blinkt.openvpn.** { *; }

# Keep new VPN API classes (ConfigParser, VPNLaunchHelper, ProfileManager)
-keep class de.blinkt.openvpn.core.ConfigParser { *; }
-keep class de.blinkt.openvpn.core.ConfigParser$ConfigParseError { *; }
-keep class de.blinkt.openvpn.core.VPNLaunchHelper { *; }
-keep class de.blinkt.openvpn.core.ProfileManager { *; }

# Keep VPN configuration parsing
-keep class de.blinkt.openvpn.core.ConfigParser$ConfigParseError { *; }
-keepclassmembers class de.blinkt.openvpn.core.ConfigParser { public *; }

# Keep VPN profile management
-keepclassmembers class de.blinkt.openvpn.VpnProfile { public *; }
-keepclassmembers class de.blinkt.openvpn.core.ProfileManager { public *; }

# Keep VPN service management
-keepclassmembers class de.blinkt.openvpn.core.OpenVPNService { public *; }
-keepclassmembers class de.blinkt.openvpn.core.VpnStatus { public *; }
-keepclassmembers class de.blinkt.openvpn.core.VPNLaunchHelper { public *; }

# Keep VPN utility classes
-keepclassmembers class de.blinkt.openvpn.utils.** { public *; }

# Keep VPN internal interfaces
-keepclassmembers class de.blinkt.openvpn.core.IOpenVPNServiceInternal$Stub { public *; }
-keepclassmembers class de.blinkt.openvpn.core.IOpenVPNServiceInternal { public *; }

# Keep VPN byte count and state listeners
-keepclassmembers class de.blinkt.openvpn.core.VpnStatus$ByteCountListener { public *; }
-keepclassmembers class de.blinkt.openvpn.core.VpnStatus$StateListener { public *; }

# --- Keep app model classes used with Gson (critical) ---
-keep class com.nocturnevpn.model.** { *; }
-keepclassmembers class com.nocturnevpn.model.** { *; }

# Keep server selection Activity and adapters
-keep class com.nocturnevpn.view.activitys.ChangeServerActivity { *; }
-keep class com.nocturnevpn.adapter.** { *; }