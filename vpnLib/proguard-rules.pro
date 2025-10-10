# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the Android SDK tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# CRITICAL: Keep ALL OpenVPN classes - R8 is very aggressive
-keep class de.blinkt.openvpn.** { *; }
-keepclassmembers class de.blinkt.openvpn.** { *; }

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# OpenVPN Core Classes - CRITICAL for VPN functionality
-keep class de.blinkt.openvpn.VpnProfile
-keepclassmembers class de.blinkt.openvpn.VpnProfile { public *;}

# OpenVPN Core Package - Keep all core classes
-keep class de.blinkt.openvpn.core.**
-keepclassmembers class de.blinkt.openvpn.core.** { public *;}

# Specific OpenVPN Core Classes
-keep class de.blinkt.openvpn.core.ConfigParser$ConfigParseError
-keep class de.blinkt.openvpn.core.ConfigParser
-keepclassmembers class de.blinkt.openvpn.core.ConfigParser {public *;}

-keep class de.blinkt.openvpn.core.ConnectionStatus
-keepclassmembers class de.blinkt.openvpn.core.ConnectionStatus {public *;}

-keep class de.blinkt.openvpn.core.IOpenVPNServiceInternal$Stub
-keepclassmembers class de.blinkt.openvpn.core.IOpenVPNServiceInternal$Stub {public *;}

-keep class de.blinkt.openvpn.core.IOpenVPNServiceInternal
-keepclassmembers class de.blinkt.openvpn.core.IOpenVPNServiceInternal {public *;}

-keep class de.blinkt.openvpn.core.OpenVPNService
-keepclassmembers class de.blinkt.openvpn.core.OpenVPNService {public *;}

-keep class de.blinkt.openvpn.core.ProfileManager
-keepclassmembers class de.blinkt.openvpn.core.ProfileManager {public *;}

-keep class de.blinkt.openvpn.core.VPNLaunchHelper
-keepclassmembers class de.blinkt.openvpn.core.VPNLaunchHelper {public *;}

-keep class de.blinkt.openvpn.core.VpnStatus$ByteCountListener
-keep class de.blinkt.openvpn.core.VpnStatus$StateListener
-keep class de.blinkt.openvpn.core.VpnStatus
-keepclassmembers class de.blinkt.openvpn.core.VpnStatus {public *;}

# OpenVPN Utils Package
-keep class de.blinkt.openvpn.utils.**
-keepclassmembers class de.blinkt.openvpn.utils.** { public *; }

# OpenVPN API Package
-keep class de.blinkt.openvpn.api.**
-keepclassmembers class de.blinkt.openvpn.api.** { public *; }

# Harden AIDL and internal interfaces (Play minify parity with app module)
-keep class de.blinkt.openvpn.core.IOpenVPNServiceInternal { *; }
-keep class de.blinkt.openvpn.core.IOpenVPNServiceInternal$Stub { *; }
-keep class de.blinkt.openvpn.api.IOpenVPNAPIService { *; }
-keepattributes *Annotation*,InnerClasses,EnclosingMethod,Signature

# CRITICAL: Keep OpenVpnApi class - This is the main entry point
-keep class de.blinkt.openvpn.OpenVpnApi { *; }
-keepclassmembers class de.blinkt.openvpn.OpenVpnApi { *; }
-keepclassmembers class de.blinkt.openvpn.OpenVpnApi { public static *; }

# Native Library Support
-keep class de.blinkt.openvpn.core.NativeUtils { *; }

# VPN Status Enums
-keepclassmembers enum de.blinkt.openvpn.core.ConnectionStatus {*;}

# Keep VPN callback interfaces
-keep interface de.blinkt.openvpn.core.VpnStatus$* { *; }
-keep interface de.blinkt.openvpn.core.IOpenVPNServiceInternal$* { *; }

# Keep VPN configuration parsing
-keep class de.blinkt.openvpn.core.ConfigParser$ConfigParseError { *; }

# Keep VPN profile management
-keepclassmembers class de.blinkt.openvpn.core.ProfileManager { public *; }

# Keep VPN service management
-keepclassmembers class de.blinkt.openvpn.core.OpenVPNService { public *; }
-keepclassmembers class de.blinkt.openvpn.core.VpnStatus { public *; }
-keepclassmembers class de.blinkt.openvpn.core.VPNLaunchHelper { public *; }

# Keep VPN internal interfaces
-keepclassmembers class de.blinkt.openvpn.core.IOpenVPNServiceInternal$Stub { public *; }
-keepclassmembers class de.blinkt.openvpn.core.IOpenVPNServiceInternal { public *; }

# Keep VPN byte count and state listeners
-keepclassmembers class de.blinkt.openvpn.core.VpnStatus$ByteCountListener { public *; }
-keepclassmembers class de.blinkt.openvpn.core.VpnStatus$StateListener { public *; }