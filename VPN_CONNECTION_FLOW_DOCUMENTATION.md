# NocturneVPN - Complete VPN Connection Process Documentation

## Table of Contents
1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Complete Connection Flow](#complete-connection-flow)
4. [Module Integration Guide](#module-integration-guide)
5. [VPNLib Module Migration](#vpnlib-module-migration)
6. [Code Locations](#code-locations)

---

## Overview

NocturneVPN is an Android VPN application that uses OpenVPN protocol to establish secure connections. The application fetches server data from an external API, processes it, and uses a native VPN library (vpnLib) to establish connections.

**Key Technologies:**
- **API Source**: VPN Gate API (http://www.vpngate.net/api/iphone/)
- **VPN Protocol**: OpenVPN (via blinkt OpenVPN library)
- **Architecture**: Modular design with separate VPN library module
- **Data Storage**: SQLite Database + SharedPreferences

---

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                       APPLICATION LAYER                         │
├─────────────────────────────────────────────────────────────────┤
│  HomeFragment → VPNManager → OpenVpnApi → VPNLaunchHelper      │
│                ↓                                                │
│         ServerManager                                         │
│                ↓                                                │
│  ChangeServerActivity → CsvParser → Database                   │
└─────────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────────┐
│                     VPNLIB MODULE (Native)                      │
├─────────────────────────────────────────────────────────────────┤
│  OpenVPNService (VpnService) → OpenVPNThread                   │
│         ↓                                                       │
│  ConfigParser → VpnProfile → ProfileManager                    │
│         ↓                                                       │
│  Native OpenVPN Binary (libovpnexec.so)                        │
└─────────────────────────────────────────────────────────────────┘
```

### Module Structure

**app module** (`app/src/main/java/com/nocturnevpn/`):
- User interface components
- Business logic
- API integration
- Server management

**vpnLib module** (`vpnLib/src/main/java/de/blinkt/openvpn/`):
- OpenVPN protocol implementation
- VPNService management
- Native OpenVPN binary wrapper
- Configuration parsing

---

## Complete Connection Flow

### Step 1: Server Data Fetching from API

**File:** `app/src/main/java/com/nocturnevpn/workers/ServerFetchWorker.kt`

**Process:**
1. The `ServerFetchWorker` makes an HTTP GET request to VPN Gate API
2. API URL: `http://www.vpngate.net/api/iphone/`
3. Response format: CSV (Comma-Separated Values)

**Code Reference:**
```kotlin
// Line 40-46 in ServerFetchWorker.kt
val request = Request.Builder()
    .url(BuildConfig.VPN_GATE_API)
    .build()

val response = okHttpClient.newCall(request).execute()

if (response.isSuccessful) {
    val servers = CsvParser.parse(response)
    // Process and cache servers
}
```

**Also triggered in:** `app/src/main/java/com/nocturnevpn/view/activitys/ChangeServerActivity.java` (line 439-482)

---

### Step 2: CSV Parsing and Data Transformation

**File:** `app/src/main/java/com/nocturnevpn/utils/CsvParser.java`

**Process:**
1. Reads CSV response line by line
2. Each line represents one VPN server
3. Parses server metadata (IP, country, speed, ping, etc.)
4. **CRITICAL**: Decodes Base64-encoded OpenVPN configuration data

**CSV Column Structure:**
```
[0] HostName
[1] IP Address
[2] Score
[3] Ping
[4] Speed
[5] Country Long Name
[6] Country Short Code
[7] VPN Sessions
[8] Uptime
[9] Total Users
[10] Total Traffic
[11] Log Type
[12] Operator
[13] Message
[14] OVPN Config Data (Base64 encoded)
```

**Code Reference:**
```java
// Line 37-66 in CsvParser.java
public static Server stringToServer(String line) {
    String[] vpn = line.split(",");
    
    Server server = new Server();
    server.hostName = vpn[HOST_NAME];
    server.ipAddress = vpn[IP_ADDRESS];
    server.score = Integer.parseInt(vpn[SCORE]);
    // ... more fields ...
    
    // DECODE BASE64 OVPN CONFIG
    server.ovpnConfigData = new String(Base64.decode(
            vpn[OVPN_CONFIG_DATA], Base64.DEFAULT));
    
    // Extract port and protocol from OVPN config
    String[] lines = server.ovpnConfigData.split("[\\r\\n]+");
    server.port = getPort(lines);
    server.protocol = getProtocol(lines);
    
    return server;
}
```

**Key Files:**
- Parser: `CsvParser.java` (line 37-66)
- Server Model: `app/src/main/java/com/nocturnevpn/model/Server.java`
- Protocol extraction: `CsvParser.java` (line 98-128)

---

### Step 3: Server Data Storage and Caching

**Storage Locations:**

1. **SQLite Database** (`app/src/main/java/com/nocturnevpn/db/DbHelper.java`)
   - Stores raw server data
   - Provides fast local retrieval

2. **SharedPreferences** (`app/src/main/java/com/nocturnevpn/SharedPreference.java`)
   - Caches processed server list with ping/premium status
   - Stores selected server
   - Quick access for UI

**Code Reference:**
```java
// Saving servers in ServerFetchWorker.kt
sharedPref.saveServerList(updatedServers)  // SharedPreferences cache
dbHelper.save(updatedServers, context)     // SQLite database
```

---

### Step 4: User Clicks Connect Button

**File:** `app/src/main/java/com/nocturnevpn/view/fragment/HomeFragment.kt`

**Trigger:** User clicks the connect button in HomeFragment

**Flow:**
1. `handleConnectButtonClick()` called (line 291)
2. Validates premium server access if applicable
3. Checks notification permissions
4. Calls `vpnManager.prepareVPN()` (line 98-112 in VPNManager.kt)

**Code Reference:**
```kotlin
// Line 251-253 in HomeFragment.kt
binding?.connectButton?.setOnClickListener {
    handleConnectButtonClick()
}

// Line 291-366 in HomeFragment.kt
private fun handleConnectButtonClick() {
    connectionStatusManager.animateButtonClick()
    
    if (vpnManager.isVPNStarted()) {
        confirmDisconnect()
        return
    }
    
    // Premium check, notification permissions...
    proceedConnectionFlowAfterPremiumCheck(isPremium)
}
```

---

### Step 5: VPN Permission Request

**File:** `app/src/main/java/com/nocturnevpn/view/managers/VPNManager.kt`

**Process:**
1. `prepareVPN()` checks if `VpnService.prepare()` returns an Intent
2. If Intent is not null → User hasn't granted VPN permission → Launch permission dialog
3. If Intent is null → Permission already granted → Start VPN directly

**Code Reference:**
```kotlin
// Line 98-112 in VPNManager.kt
fun prepareVPN() {
    if (!vpnStart) {
        if (getInternetStatus()) {
            Log.d("OpenVPN", "Preparing VPN...")
            val intent = VpnService.prepare(context)
            if (intent != null) {
                vpnResultLauncher?.launch(intent)  // Request permission
            } else {
                Log.d("OpenVPN", "VPN already prepared, starting VPN")
                startVpn()  // Permission granted, start VPN
            }
        } else {
            context.toast("No Internet Connection")
        }
    }
}
```

**Permission Callback:** `vpnResult` launcher in HomeFragment.kt (line 104-111)

---

### Step 6: VPN Connection Initiation

**File:** `app/src/main/java/com/nocturnevpn/view/managers/VPNManager.kt`

**Process:**
1. Retrieves selected server from SharedPreferences
2. Gets OpenVPN configuration data from server object
3. Calls `OpenVpnApi.startVpn()` with configuration

**Code Reference:**
```kotlin
// Line 115-156 in VPNManager.kt
fun startVpn() {
    try {
        context.toast("Starting VPN...")
        val selectedServer = sharedPreference.getServer()
        
        if (selectedServer == null) {
            context.toast("No server selected.")
            return
        }
        
        // GET OVPN CONFIG DATA (already in .ovpn format)
        val conf = selectedServer.getOvpnConfigData()
        
        if (conf.isNullOrEmpty()) {
            context.toast("VPN configuration data is missing.")
            return
        }
        
        Log.d("VPN_START", "Starting VPN with server:")
        Log.d("VPN_START", "Country: ${selectedServer.getCountryLong()}")
        Log.d("VPN_START", "IP Address: ${selectedServer.getIpAddress()}")
        
        historyManager?.onConnectionStarted(selectedServer)
        
        // START VPN WITH OVPN CONFIG
        OpenVpnApi.startVpn(context, conf, selectedServer.getCountryShort(), "vpn", "vpn")
        vpnStart = true
    } catch (exception: IOException) {
        exception.printStackTrace()
        historyManager?.onConnectionFailed(selectedServer, "Configuration Error")
    }
}
```

**Key Points:**
- The `.ovpn` configuration is **already in the correct format** from the API
- No additional transformation is needed
- Configuration string is passed directly to OpenVPN library

---

### Step 7: OpenVPN API Processing

**File:** `vpnLib/src/main/java/de/blinkt/openvpn/OpenVpnApi.java`

**Process:**
1. Validates configuration is not empty
2. Creates `ConfigParser` to parse .ovpn configuration
3. Converts configuration to `VpnProfile` object
4. Validates profile
5. Saves profile as temporary profile via `ProfileManager`
6. Launches OpenVPN via `VPNLaunchHelper`

**Code Reference:**
```java
// Line 23-47 in OpenVpnApi.java
public static void startVpn(Context context, String inlineConfig, 
                           String sCountry, String userName, String pw) 
                           throws RemoteException {
    if (TextUtils.isEmpty(inlineConfig)) 
        throw new RemoteException("config is empty");
    startVpnInternal(context, inlineConfig, sCountry, userName, pw);
}

static void startVpnInternal(Context context, String inlineConfig, 
                            String sCountry, String userName, String pw) 
                            throws RemoteException {
    ConfigParser cp = new ConfigParser();
    try {
        // PARSE .OVPN CONFIGURATION
        cp.parseConfig(new StringReader(inlineConfig));
        VpnProfile vp = cp.convertProfile();  // Convert to profile object
        
        vp.mName = sCountry;
        
        // VALIDATE PROFILE
        if (vp.checkProfile(context) != R.string.no_error_found) {
            throw new RemoteException(context.getString(vp.checkProfile(context)));
        }
        
        vp.mProfileCreator = context.getPackageName();
        vp.mUsername = userName;
        vp.mPassword = pw;
        
        // SAVE TEMPORARY PROFILE
        ProfileManager.setTemporaryProfile(context, vp);
        
        // LAUNCH OPENVPN
        VPNLaunchHelper.startOpenVpn(vp, context);
    } catch (IOException | ConfigParser.ConfigParseError e) {
        throw new RemoteException(e.getMessage());
    }
}
```

**Key Files:**
- Entry point: `OpenVpnApi.java` (line 23-47)
- Parser: `vpnLib/src/main/java/de/blinkt/openvpn/core/ConfigParser.java`
- Profile: `vpnLib/src/main/java/de/blinkt/openvpn/VpnProfile.java`
- Launch helper: `VPNLaunchHelper.java`

---

### Step 8: Configuration Parsing

**File:** `vpnLib/src/main/java/de/blinkt/openvpn/core/ConfigParser.java`

**Process:**
1. Reads .ovpn configuration line by line
2. Parses standard OpenVPN directives (remote, proto, auth, cipher, etc.)
3. Extracts certificate/key data from inline blocks
4. Validates configuration options
5. Creates `VpnProfile` object with all settings

**Key Parsing Logic:**
```java
// ConfigParser.java
public void parseConfig(Reader reader) throws IOException, ConfigParseError {
    BufferedReader in = new BufferedReader(reader);
    String line;
    while ((line = in.readLine()) != null) {
        line = line.trim();
        if (line.isEmpty() || line.startsWith("#"))
            continue;
        
        // Parse different configuration options
        if (line.startsWith("remote")) {
            parseRemote(line);
        } else if (line.startsWith("proto")) {
            parseProto(line);
        } else if (line.startsWith("<ca>")) {
            parseCA(in);
        }
        // ... more options
    }
}
```

---

### Step 9: Profile Management

**File:** `vpnLib/src/main/java/de/blinkt/openvpn/core/ProfileManager.java`

**Process:**
1. `ProfileManager.setTemporaryProfile()` stores the VpnProfile
2. Profile is marked as "temporary" (not persisted to disk)
3. UUID is generated for the profile
4. Profile is added to in-memory profile list

**Code Reference:**
```java
// Line 106-167 in ProfileManager.java
public static void setTemporaryProfile(Context context, VpnProfile profile) {
    profile.mTemporaryProfile = true;
    profile.setUUID(UUID.randomUUID());
    tmpprofile = profile;
}
```

---

### Step 10: VPN Launch and Service Start

**File:** `vpnLib/src/main/java/de/blinkt/openvpn/core/VPNLaunchHelper.java`

**Process:**
1. Gets native OpenVPN binary path
2. Builds OpenVPN command-line arguments
3. Writes configuration to file
4. Creates Intent to start `OpenVPNService`
5. Service starts and spawns OpenVPN process

**Code Reference:**
```java
// Line 76-93 in VPNLaunchHelper.java
static String[] buildOpenvpnArgv(Context c) {
    Vector<String> args = new Vector<>();
    
    // Get native OpenVPN binary
    String binaryName = writeMiniVPN(c);
    if (binaryName == null) {
        VpnStatus.logError("Error writing minivpn binary");
        return null;
    }
    
    args.add(binaryName);  // e.g., "/data/app/libovpnexec.so"
    args.add("--config");
    args.add(getConfigFilePath(c));  // Write config to file
    
    return args.toArray(new String[args.size()]);
}
```

**Native Binary:**
- File: `libovpnexec.so` (loaded from `nativeLibraryDir`)
- Source: Pre-compiled OpenVPN binary for Android
- Location: `vpnLib/src/main/jniLibs/`

---

### Step 11: OpenVPN Service Execution

**File:** `vpnLib/src/main/java/de/blinkt/openvpn/core/OpenVPNService.java`

**Process:**
1. `OpenVPNService` extends `VpnService` (Android VPN Service)
2. Creates TUN (tunnel) interface via `VpnService.Builder()`
3. Spawns separate thread (`OpenVPNThread`) for OpenVPN process
4. OpenVPN process reads configuration file
5. Establishes TLS handshake with VPN server
6. Creates encrypted tunnel through TUN interface
7. Routes device traffic through tunnel

**Code Reference:**
```java
// OpenVPNService.java
public class OpenVPNService extends VpnService implements 
        StateListener, Callback, ByteCountListener {
    
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize service
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start VPN connection process
        startOpenVPNThread();
    }
    
    private void startOpenVPNThread() {
        // Create TUN interface
        Builder builder = new Builder();
        builder.setSession("OpenVPN");
        
        // Configure routes, DNS
        String[] routes = getRoutes();
        for (String route : routes) {
            builder.addRoute(route);
        }
        
        // Establish TUN interface
        ParcelFileDescriptor tunInterface = builder.establish();
        
        // Start OpenVPN thread with TUN interface
        OpenVPNThread.start(tunInterface);
    }
}
```

**Service Registration:**
```xml
<!-- AndroidManifest.xml -->
<service
    android:name="de.blinkt.openvpn.core.OpenVPNService"
    android:exported="false"
    android:permission="android.permission.BIND_VPN_SERVICE"
    android:foregroundServiceType="dataSync">
    <intent-filter>
        <action android:name="android.net.VpnService" />
    </intent-filter>
</service>
```

---

### Step 12: Connection Status Updates

**Files:** 
- `VPNManager.kt` (broadcast receiver)
- `ConnectionStatusManager.kt`
- `OpenVPNService.java` (status broadcaster)

**Process:**
1. `OpenVPNService` broadcasts status updates via `LocalBroadcastManager`
2. `VPNManager` registers broadcast receiver to listen for updates
3. `ConnectionStatusManager` updates UI based on status
4. `NotificationManager` updates persistent notification

**Status States:**
- `CONNECTING`: Initial connection attempt
- `WAIT`: Waiting for server response
- `AUTH`: Authenticating with server
- `CONNECTED`: Successfully connected
- `RECONNECTING`: Reconnection attempt
- `DISCONNECTED`: Connection terminated
- `NOPROCESS`: No VPN process running

**Code Reference:**
```kotlin
// VPNManager.kt line 192-244
fun registerBroadcastReceiver() {
    broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val state = intent.getStringExtra("state")
            val duration = intent.getStringExtra("duration")
            val byteIn = intent.getStringExtra("byteIn")
            val byteOut = intent.getStringExtra("byteOut")
            
            // Update status
            setStatus(state)
            
            // Update UI
            connectionStatusManager?.updateConnectionStatus(
                duration, lastPacketReceive, byteIn, byteOut
            )
            
            // Update notification
            if (state == "CONNECTED") {
                notificationManager?.updateVPNNotification(
                    serverCountry, byteIn, byteOut
                )
            }
        }
    }
    
    val intentFilter = IntentFilter("connectionState")
    LocalBroadcastManager.getInstance(context)
        .registerReceiver(broadcastReceiver!!, intentFilter)
}
```

---

### Step 13: Disconnection

**Trigger:** User clicks disconnect button in HomeFragment

**Process:**
1. `VPNManager.stopVPN()` called
2. `OpenVPNThread.stop()` sends stop signal to OpenVPN process
3. OpenVPN terminates connection gracefully
4. TUN interface is closed
5. Status broadcasts `DISCONNECTED` state
6. UI updates, notification removed

**Code Reference:**
```kotlin
// VPNManager.kt line 158-190
fun stopVPN(): Boolean {
    return try {
        Log.d("VPNManager", "Stopping VPN connection")
        
        historyManager?.onConnectionDisconnected()
        
        // STOP OPENVPN PROCESS
        OpenVPNThread.stop()
        vpnStart = false
        stopPeriodicNotificationUpdate()
        
        // Remove notification after delay
        notificationManager?.let { manager ->
            Handler(Looper.getMainLooper()).postDelayed({
                manager.removeVPNNotification()
            }, 1000)
        }
        
        true
    } catch (e: Exception) {
        Log.e("VPNManager", "Error stopping VPN", e)
        false
    }
}
```

---

## Module Integration Guide

### How to Integrate vpnLib Module into Your VPN Application

#### Step 1: Add vpnLib as a Project Dependency

**File:** `settings.gradle.kts`

```kotlin
rootProject.name = "NocturneVPN"
include(":app")
include(":vpnLib")  // Add vpnLib module
```

**File:** `app/build.gradle.kts`

```kotlin
dependencies {
    // ... other dependencies
    
    implementation(project(":vpnLib"))  // Add this line
}
```

---

#### Step 2: Add Required Permissions

**File:** `app/src/main/AndroidManifest.xml`

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    
    <!-- Internet permission for VPN -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    
    <!-- VPN service permission (declared in library's manifest) -->
    
    <!-- Foreground service for VPN -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
    
    <!-- Notifications -->
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    
    <!-- Allow binding to VPN service -->
    <uses-permission android:name="android.permission.BIND_VPN_SERVICE" />
    
    <application>
        <!-- Register VPN Service -->
        <service
            android:name="de.blinkt.openvpn.core.OpenVPNService"
            android:exported="false"
            android:permission="android.permission.BIND_VPN_SERVICE"
            android:foregroundServiceType="dataSync">
            <intent-filter>
                <action android:name="android.net.VpnService" />
            </intent-filter>
        </service>
    </application>
</manifest>
```

---

#### Step 3: Create VPN Manager Class

**File:** `app/src/main/java/your/package/view/managers/VPNManager.kt`

```kotlin
package your.package.view.managers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.Handler
import android.os.Looper
import android.os.RemoteException
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import de.blinkt.openvpn.OpenVpnApi
import de.blinkt.openvpn.core.OpenVPNService
import de.blinkt.openvpn.core.OpenVPNThread
import java.io.IOException

class VPNManager(
    private val context: Context,
    private val sharedPreference: YourSharedPreference
) {
    private var vpnStart = false
    private var vpnResultLauncher: ActivityResultLauncher<Intent>? = null
    private var broadcastReceiver: BroadcastReceiver? = null
    
    init {
        // Initialize VPN components
    }
    
    fun setVPNResultLauncher(launcher: ActivityResultLauncher<Intent>) {
        vpnResultLauncher = launcher
    }
    
    fun isVPNStarted(): Boolean = vpnStart
    
    fun prepareVPN() {
        if (!vpnStart) {
            val intent = VpnService.prepare(context)
            if (intent != null) {
                vpnResultLauncher?.launch(intent)
            } else {
                startVpn()
            }
        }
    }
    
    fun startVpn() {
        try {
            val selectedServer = sharedPreference.getServer()
            if (selectedServer == null) {
                context.toast("No server selected.")
                return
            }
            
            // Get OpenVPN configuration (.ovpn format)
            val conf = selectedServer.getOvpnConfigData()
            if (conf.isNullOrEmpty()) {
                context.toast("VPN configuration data is missing.")
                return
            }
            
            // Start VPN with configuration
            OpenVpnApi.startVpn(
                context,
                conf,                           // OpenVPN config string
                selectedServer.getCountryShort(), // Profile name
                "vpn",                          // Username (usually "vpn")
                "vpn"                           // Password (usually "vpn")
            )
            vpnStart = true
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }
    
    fun stopVPN(): Boolean {
        return try {
            OpenVPNThread.stop()
            vpnStart = false
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun registerBroadcastReceiver() {
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val state = intent.getStringExtra("state")
                val duration = intent.getStringExtra("duration") ?: "00:00:00"
                val byteIn = intent.getStringExtra("byteIn") ?: "0"
                val byteOut = intent.getStringExtra("byteOut") ?: "0"
                
                // Handle status updates
                setStatus(state)
            }
        }
        
        val intentFilter = IntentFilter("connectionState")
        LocalBroadcastManager.getInstance(context)
            .registerReceiver(broadcastReceiver!!, intentFilter)
    }
    
    private fun setStatus(connectionState: String?) {
        when (connectionState) {
            "CONNECTED" -> vpnStart = true
            "DISCONNECTED", "NOPROCESS" -> vpnStart = false
        }
    }
}
```

---

#### Step 4: Implement Server Model

**File:** `app/src/main/java/your/package/model/Server.kt`

```kotlin
package your.package.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Server(
    val hostName: String = "",
    val ipAddress: String = "",
    val countryLong: String = "",
    val countryShort: String = "",
    val ovpnConfigData: String = "",  // OpenVPN configuration in .ovpn format
    val port: Int = 0,
    val protocol: String = "",
    val speed: Long = 0,
    val ping: String = "",
    val score: Int = 0
) : Parcelable
```

---

#### Step 5: Request VPN Permission in Activity/Fragment

**File:** `app/src/main/java/your/package/view/fragment/HomeFragment.kt`

```kotlin
import android.net.VpnService
import androidx.activity.result.contract.ActivityResultContracts

class HomeFragment : Fragment() {
    private lateinit var vpnManager: VPNManager
    
    // Register VPN permission launcher
    private val vpnResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Permission granted, start VPN
            vpnManager.startVpn()
        } else {
            // Permission denied
            showToast("VPN permission required")
        }
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize VPN manager
        vpnManager = VPNManager(requireContext(), sharedPreference)
        vpnManager.setVPNResultLauncher(vpnResult)
        
        // Connect button click
        binding.connectButton.setOnClickListener {
            if (vpnManager.isVPNStarted()) {
                confirmDisconnect()
            } else {
                vpnManager.prepareVPN()
            }
        }
    }
}
```

---

## VPNLib Module Migration

### How to Migrate Your Existing VPNLib Module

#### Step 1: Understand vpnLib Structure

```
vpnLib/
├── build.gradle                      # Gradle configuration
├── src/main/
│   ├── AndroidManifest.xml          # Module manifest
│   ├── assets/
│   │   ├── pi*.arm64-v8a           # Native binaries
│   │   ├── pi*.armeabi-v7a
│   │   ├── pi*.x86
│   │   └── pi*.x86_64
│   ├── java/de/blinkt/openvpn/
│   │   ├── OpenVpnApi.java         # Main API entry point
│   │   ├── VpnProfile.java         # VPN profile model
│   │   ├── core/
│   │   │   ├── ConfigParser.java   # .ovpn parser
│   │   │   ├── VPNLaunchHelper.java # VPN launcher
│   │   │   ├── OpenVPNService.java  # Android VPN service
│   │   │   ├── OpenVPNThread.java   # OpenVPN thread
│   │   │   ├── ProfileManager.java  # Profile management
│   │   │   └── VpnStatus.java       # Status management
│   │   └── api/
│   │       └── ExternalOpenVPNService.java
│   └── res/
│       └── values/
│           └── strings.xml
└── proguard-rules.pro
```

---

#### Step 2: Copy vpnLib Module

1. **Copy entire vpnLib directory** to your project root
2. **Update package names** if different (optional)

**Commands:**
```bash
# Copy vpnLib to your project
cp -r /path/to/NocturneVPN/vpnLib /path/to/YourProject/vpnLib

# Update settings.gradle
echo 'include(":vpnLib")' >> settings.gradle.kts
```

---

#### Step 3: Configure Gradle Files

**File:** `vpnLib/build.gradle`

```groovy
apply plugin: 'com.android.library'

android {
    namespace 'de.blinkt.openvpn'
    compileSdkVersion 35
    
    defaultConfig {
        minSdkVersion 16
        targetSdkVersion 35
        
        testInstrumentationRunner 'androidx.test.runner.AndroidJUnitRunner'
        consumerProguardFiles 'proguard-rules.pro'
    }
    
    buildTypes {
        release {
            minifyEnabled false
        }
        debug {
            minifyEnabled false
        }
    }
    
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
    
    buildFeatures {
        aidl true
        buildConfig true
    }
}

dependencies {
    implementation fileTree(dir: 'libs', include: ['*.jar'])
    implementation 'androidx.localbroadcastmanager:localbroadcastmanager:1.0.0'
    implementation 'androidx.activity:activity-ktx:1.7.2'
    implementation 'androidx.appcompat:appcompat:1.1.0'
    
    testImplementation 'junit:junit:4.13'
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
}

// Force Kotlin stdlib version
configurations.all {
    resolutionStrategy {
        force "org.jetbrains.kotlin:kotlin-stdlib:1.8.10"
        force "org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.8.10"
        force "org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.10"
    }
}
```

---

#### Step 4: Add Native Libraries (Critical!)

The vpnLib module requires native OpenVPN binaries.

**Source:** Binary files are in `vpnLib/src/main/assets/`

**Files to copy:**
```
pi_openvpn.arm64-v8a
pi_openvpn.armeabi-v7a
pi_openvpn.x86
pi_openvpn.x86_64
```

**Alternative:** If you have pre-compiled `.so` files:

```
vpnLib/src/main/jniLibs/
├── arm64-v8a/
│   └── libovpnexec.so
├── armeabi-v7a/
│   └── libovpnexec.so
├── x86/
│   └── libovpnexec.so
└── x86_64/
    └── libovpnexec.so
```

**Important:** The native binaries are **essential**. Without them, the VPN will fail to start.

---

#### Step 5: Add AndroidManifest.xml

**File:** `vpnLib/src/main/AndroidManifest.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.BIND_VPN_SERVICE" />
    
    <application>
        <!-- VPN Service will be registered in app manifest -->
    </application>
</manifest>
```

---

#### Step 6: Add AIDL Files (if using external API)

**Files:** `vpnLib/src/main/aidl/de/blinkt/openvpn/api/*.aidl`

Create these AIDL interface files:
- `IOpenVPNService.aidl`
- `IOpenVPNServiceCallback.aidl`
- `IParcelFileDescriptor.aidl`
- `APIVpnProfile.aidl`

---

#### Step 7: Configure App Module

**File:** `app/build.gradle.kts`

```kotlin
android {
    // ... other config
    
    // IMPORTANT: Ensure native libs are extracted
    packagingOptions {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    // ... other dependencies
    
    implementation(project(":vpnLib"))
}
```

---

### Starting a VPN Session

#### Method 1: Using OpenVpnApi (Recommended)

```kotlin
import de.blinkt.openvpn.OpenVpnApi
import android.os.RemoteException

class VPNHelper(private val context: Context) {
    
    fun startVPN(ovpnConfigString: String, serverName: String) {
        try {
            // ovpnConfigString: Complete .ovpn configuration content
            // serverName: Display name for the connection
            // username: VPN username (typically "vpn")
            // password: VPN password (typically "vpn")
            
            OpenVpnApi.startVpn(
                context,
                ovpnConfigString,  // Full .ovpn config as string
                serverName,        // Profile name
                "vpn",            // Username
                "vpn"             // Password
            )
        } catch (e: RemoteException) {
            Log.e("VPN", "Failed to start VPN", e)
        }
    }
    
    fun stopVPN() {
        de.blinkt.openvpn.core.OpenVPNThread.stop()
    }
}
```

**Example Usage:**

```kotlin
// Read .ovpn file from assets or get from API
val ovpnConfig = """
    remote 123.45.67.89 1194 udp
    dev tun
    proto udp
    cipher AES-256-CBC
    auth SHA256
    comp-lzo
    <ca>
    -----BEGIN CERTIFICATE-----
    [Certificate content]
    -----END CERTIFICATE-----
    </ca>
""".trimIndent()

// Start VPN
vpnHelper.startVPN(ovpnConfig, "My VPN Server")
```

---

#### Method 2: Using VPNManager (With Permission Handling)

```kotlin
import android.net.VpnService

class VPNManager(private val context: Context) {
    
    private var vpnResultLauncher: ActivityResultLauncher<Intent>? = null
    
    fun setVPNResultLauncher(launcher: ActivityResultLauncher<Intent>) {
        vpnResultLauncher = launcher
    }
    
    fun prepareAndStart(ovpnConfig: String, serverName: String) {
        // Check if VPN permission is granted
        val intent = VpnService.prepare(context)
        if (intent != null) {
            // Request permission
            vpnResultLauncher?.launch(intent)
        } else {
            // Permission already granted, start VPN
            startVpn(ovpnConfig, serverName)
        }
    }
    
    private fun startVpn(ovpnConfig: String, serverName: String) {
        try {
            OpenVpnApi.startVpn(
                context,
                ovpnConfig,
                serverName,
                "vpn",
                "vpn"
            )
        } catch (e: RemoteException) {
            Log.e("VPN", "Failed to start VPN", e)
        }
    }
    
    fun stopVpn() {
        try {
            OpenVPNThread.stop()
        } catch (e: Exception) {
            Log.e("VPN", "Failed to stop VPN", e)
        }
    }
}
```

**Usage in Fragment:**

```kotlin
class HomeFragment : Fragment() {
    private lateinit var vpnManager: VPNManager
    
    private val vpnResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Permission granted, start VPN
            val config = getVPNConfig()
            vpnManager.startVpn(config, "My Server")
        }
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        vpnManager = VPNManager(requireContext())
        vpnManager.setVPNResultLauncher(vpnResult)
        
        connectButton.setOnClickListener {
            val config = getVPNConfig()
            vpnManager.prepareAndStart(config, "My Server")
        }
    }
}
```

---

### Configuration and Connection Callbacks

#### Method 1: Broadcast Receiver (Status Updates)

```kotlin
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import de.blinkt.openvpn.core.VpnStatus

class VPNStatusListener(private val context: Context) {
    private var receiver: BroadcastReceiver? = null
    
    fun startListening(onStatusUpdate: (status: String) -> Unit) {
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val state = intent.getStringExtra("state")
                val duration = intent.getStringExtra("duration") ?: "00:00:00"
                val byteIn = intent.getStringExtra("byteIn") ?: "0"
                val byteOut = intent.getStringExtra("byteOut") ?: "0"
                
                // Handle status update
                onStatusUpdate(state ?: "UNKNOWN")
                
                Log.d("VPN", "State: $state, Bytes: $byteIn / $byteOut")
            }
        }
        
        val filter = IntentFilter("connectionState")
        LocalBroadcastManager.getInstance(context)
            .registerReceiver(receiver!!, filter)
    }
    
    fun stopListening() {
        receiver?.let {
            LocalBroadcastManager.getInstance(context)
                .unregisterReceiver(it)
        }
    }
}

// Usage
val statusListener = VPNStatusListener(requireContext())
statusListener.startListening { status ->
    when (status) {
        "CONNECTED" -> {
            binding.statusText.text = "Connected"
        }
        "DISCONNECTED" -> {
            binding.statusText.text = "Disconnected"
        }
        "CONNECTING" -> {
            binding.statusText.text = "Connecting..."
        }
    }
}
```

---

#### Method 2: VpnStatus.StateListener (Direct Callbacks)

```kotlin
import de.blinkt.openvpn.core.VpnStatus
import android.content.Intent

class MyFragment : Fragment(), VpnStatus.StateListener {
    
    override fun onResume() {
        super.onResume()
        VpnStatus.addStateListener(this)
    }
    
    override fun onPause() {
        super.onPause()
        VpnStatus.removeStateListener(this)
    }
    
    override fun updateState(
        state: String?,
        logmessage: String?,
        localizedResId: Int,
        level: ConnectionStatus?,
        intent: Intent?
    ) {
        // Handle state update on UI thread
        requireActivity().runOnUiThread {
            when (state) {
                "CONNECTED" -> {
                    updateUIConnected()
                }
                "DISCONNECTED" -> {
                    updateUIDisconnected()
                }
                "CONNECTING" -> {
                    updateUIConnecting()
                }
            }
        }
    }
    
    override fun setConnectedVPN(uuid: String?) {
        // Store connected VPN UUID
    }
}

// Also register VpnStatus.ByteCountListener for speed updates
class MyFragment : Fragment(), VpnStatus.ByteCountListener {
    
    override fun onResume() {
        super.onResume()
        VpnStatus.addByteCountListener(this)
    }
    
    override fun onPause() {
        super.onPause()
        VpnStatus.removeByteCountListener(this)
    }
    
    override fun updateByteCount(
        inBytes: Long,
        outBytes: Long,
        diffInBytes: Long,
        diffOutBytes: Long
    ) {
        // Update speed/b yte count UI
        val speedIn = formatSpeed(diffInBytes)
        val speedOut = formatSpeed(diffOutBytes)
        
        binding.speedIn.text = speedIn
        binding.speedOut.text = speedOut
    }
    
    private fun formatSpeed(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format("%.2f MB/s", bytes / 1024.0 / 1024.0)
            bytes >= 1024 -> String.format("%.2f KB/s", bytes / 1024.0)
            else -> "$bytes B/s"
        }
    }
}
```

---

#### Method 3: Check Current Connection Status

```kotlin
import de.blinkt.openvpn.core.OpenVPNService

fun isVPNConnected(context: Context): Boolean {
    val state = OpenVPNService.getStatus()
    return state == "CONNECTED"
}

fun getConnectionStatus(context: Context): String? {
    return OpenVPNService.getStatus()
}

fun getConnectedServerInfo(context: Context): ConnectionInfo? {
    val vpnProfile = de.blinkt.openvpn.core.ProfileManager
        .getLastConnectedProfile(context)
    
    return if (vpnProfile != null) {
        ConnectionInfo(
            name = vpnProfile.mName,
            uuid = vpnProfile.getUUIDString(),
            state = OpenVPNService.getStatus()
        )
    } else {
        null
    }
}
```

---

## Code Locations

### API Integration
- **Server Fetching**: `app/src/main/java/com/nocturnevpn/workers/ServerFetchWorker.kt`
- **CSV Parsing**: `app/src/main/java/com/nocturnevpn/utils/CsvParser.java`
- **API URL Config**: `app/build.gradle.kts` (line 23)

### Server Management
- **Server Model**: `app/src/main/java/com/nocturnevpn/model/Server.java`
- **Database Helper**: `app/src/main/java/com/nocturnevpn/db/DbHelper.java`
- **SharedPreferences**: `app/src/main/java/com/nocturnevpn/SharedPreference.java`
- **Server Selection UI**: `app/src/main/java/com/nocturnevpn/view/activitys/ChangeServerActivity.java`

### VPN Connection
- **Home UI**: `app/src/main/java/com/nocturnevpn/view/fragment/HomeFragment.kt`
- **VPN Manager**: `app/src/main/java/com/nocturnevpn/view/managers/VPNManager.kt`
- **Connection Status**: `app/src/main/java/com/nocturnevpn/view/managers/ConnectionStatusManager.kt`

### VPN Library
- **API Entry**: `vpnLib/src/main/java/de/blinkt/openvpn/OpenVpnApi.java`
- **Config Parser**: `vpnLib/src/main/java/de/blinkt/openvpn/core/ConfigParser.java`
- **Profile**: `vpnLib/src/main/java/de/blinkt/openvpn/VpnProfile.java`
- **Launcher**: `vpnLib/src/main/java/de/blinkt/openvpn/core/VPNLaunchHelper.java`
- **Service**: `vpnLib/src/main/java/de/blinkt/openvpn/core/OpenVPNService.java`
- **Thread**: `vpnLib/src/main/java/de/blinkt/openvpn/core/OpenVPNThread.java`
- **Profile Manager**: `vpnLib/src/main/java/de/blinkt/openvpn/core/ProfileManager.java`
- **Status**: `vpnLib/src/main/java/de/blinkt/openvpn/core/VpnStatus.java`

### Native Binaries
- **ARM64**: `vpnLib/src/main/assets/pi_openvpn.arm64-v8a`
- **ARM**: `vpnLib/src/main/assets/pi_openvpn.armeabi-v7a`
- **x86**: `vpnLib/src/main/assets/pi_openvpn.x86`
- **x86_64**: `vpnLib/src/main/assets/pi_openvpn.x86_64`

### Manifest Configuration
- **App Manifest**: `app/src/main/AndroidManifest.xml` (line 257-265)
- **VPNLib Manifest**: `vpnLib/src/main/AndroidManifest.xml`

---

## Troubleshooting

### Common Issues

#### 1. "VPN configuration data is missing"
**Cause:** Server doesn't have valid `.ovpn` config
**Solution:** Check API response includes valid Base64 OVPN data

#### 2. "Error writing minivpn binary"
**Cause:** Native libraries missing or corrupted
**Solution:** Ensure all `.so` files are in `jniLibs/` or assets

#### 3. VPN permission denied
**Cause:** User didn't grant VPN permission
**Solution:** Ensure `VpnService.prepare()` is called before starting

#### 4. Connection fails immediately
**Cause:** Invalid .ovpn configuration or network issues
**Solution:** Check logs, validate OVPN config format

#### 5. "BillingClient not ready"
**Cause:** Google Play Billing not initialized
**Solution:** Wait for billing setup before querying purchases

---

## Summary

**Complete Flow Summary:**
1. **API Fetch** → CSV data from vpngate.net
2. **Parse** → Decode Base64 OVPN configs
3. **Store** → Cache in SharedPreferences + Database
4. **User Click** → Connect button in HomeFragment
5. **Permission** → Request VPN permission via VpnService
6. **Start** → Call OpenVpnApi.startVpn() with config
7. **Parse** → ConfigParser creates VpnProfile
8. **Launch** → VPNLaunchHelper starts OpenVPNService
9. **Connect** → Native OpenVPN binary establishes tunnel
10. **Status** → Broadcast updates to UI

**Key Integration Points:**
- Server data comes pre-formatted as .ovpn from API
- No transformation needed - pass config directly to library
- VPN permission required before starting
- Listen to broadcast receivers for status updates
- Native binaries are essential for operation

