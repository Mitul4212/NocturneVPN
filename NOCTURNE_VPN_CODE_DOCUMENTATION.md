# NocturneVPN - Complete Code Documentation
## Android Development Interview Preparation Guide

---

## Table of Contents
1. [Project Overview](#project-overview)
2. [Application Architecture](#application-architecture)
3. [Core Components](#core-components)
4. [Activities](#activities)
5. [Fragments](#fragments)
6. [Managers](#managers)
7. [Utils & Helpers](#utils--helpers)
8. [Database Layer](#database-layer)
9. [Data Models](#data-models)
10. [Networking & API](#networking--api)
11. [Workers & Background Tasks](#workers--background-tasks)
12. [Android Concepts Used](#android-concepts-used)
13. [Kotlin Concepts Used](#kotlin-concepts-used)
14. [Java Concepts Used](#java-concepts-used)
15. [Interview Questions & Answers](#interview-questions--answers)

---

## Project Overview

**NocturneVPN** is an Android VPN application built with Kotlin and Java. It provides secure VPN connections using OpenVPN protocol, user authentication via Firebase, subscription management with Google Play Billing, and ad monetization.

### Key Features:
- VPN connection management
- User authentication (Email, Google, Facebook)
- Premium subscription system
- Server selection and management
- Connection history tracking
- Ad integration (Banner, Interstitial, Rewarded)
- Dark mode support
- Server ping calculation
- Premium server selection algorithm

---

## Application Architecture

### Architecture Pattern: **MVVM + Manager Pattern**

The app uses a combination of:
- **MVVM** (Model-View-ViewModel) for UI components
- **Manager Pattern** for business logic separation
- **Repository Pattern** for data access
- **Singleton Pattern** for shared resources

### Package Structure:
```
com.nocturnevpn/
├── view/
│   ├── activitys/          # Activities
│   ├── fragment/           # Fragments
│   ├── managers/           # Business logic managers
│   └── dialogs/            # Custom dialogs
├── utils/                  # Utility classes
├── db/                     # Database layer
├── model/                  # Data models
├── adapter/                # RecyclerView adapters
├── data/
│   ├── api/                # API interfaces
│   ├── model/              # Data models
│   └── repository/         # Repository classes
├── workers/                # Background workers
└── widget/                  # Custom views
```

---

## Core Components

### 1. NocturnVPNAppliction.kt
**Purpose**: Application class - Entry point of the app

**Key Responsibilities**:
- Initialize third-party SDKs (Facebook, Firebase, Google MobileAds)
- Setup periodic background workers
- Apply theme preferences
- Generate key hashes for debugging

**Key Functions**:
```kotlin
onCreate() // Initializes all SDKs and workers
setupPeriodicServerFetch() // Schedules server list updates every 15 minutes
scheduleDailySubscriptionVerification() // Verifies subscriptions daily
setupSubscriptionExpiryEnforcement() // Enforces subscription expiry
logAppInstallationSource() // Logs where app was installed from
```

**Android Concepts**:
- **Application Class**: Custom Application class for global initialization
- **WorkManager**: Background task scheduling
- **Singleton Pattern**: SDK initialization
- **BuildConfig**: Feature flags (DISABLE_THIRD_PARTY_SDKS)

---

### 2. SharedPreference.java
**Purpose**: Wrapper class for Android SharedPreferences

**Key Functions**:
```java
saveServer(Server server) // Saves selected VPN server
getServer() // Retrieves saved server
saveServerList(List<Server> serverList) // Saves server list as JSON
loadServerList() // Loads server list from JSON
isUserSignedIn() // Checks user sign-in status
setUserSignedIn(Boolean signedIn) // Sets sign-in status
setProtocolFilter(String filter) // Saves protocol filter (ALL/TCP/UDP)
setDarkModeEnabled(boolean enabled) // Saves dark mode preference
```

**Java Concepts**:
- **SharedPreferences**: Android's key-value storage
- **Gson**: JSON serialization/deserialization
- **TypeToken**: Generic type handling for Gson
- **Commit vs Apply**: Synchronous vs asynchronous saves

---

### 3. CheckInternetConnection.java
**Purpose**: Utility to check internet connectivity

**Key Function**:
```java
netCheck(Context context) // Returns true if internet is available
```

**Android Concepts**:
- **ConnectivityManager**: System service for network info
- **NetworkInfo**: Network state information
- **isConnectedOrConnecting()**: Checks active network state

---

## Activities

### 1. SplashActivity.kt
**Purpose**: First screen shown when app launches

**Key Responsibilities**:
- Show onboarding for first-time users
- Check authentication state
- Navigate to appropriate screen (Home/Auth)

**Key Functions**:
```kotlin
onCreate() // Sets up UI and checks first-time login
setupAuthStateListener() // Listens for Firebase auth state changes
checkAuthState() // Determines where to navigate
navigateToHome() // Navigates to HomeActivity
navigateToAuth() // Navigates to AppAuthActivity
```

**Android Concepts**:
- **WindowCompat**: Edge-to-edge display
- **WindowInsetsCompat**: System bar insets handling
- **FirebaseAuth.AuthStateListener**: Real-time auth state monitoring
- **Handler.postDelayed()**: Delayed execution
- **Intent**: Activity navigation

**Flow Logic**:
1. Check if first-time user → Show onboarding
2. If not first-time → Check auth state
3. If signed in → Navigate to Home
4. If not signed in → Navigate to Auth

---

### 2. HomeActivity.kt
**Purpose**: Main activity hosting navigation fragments

**Key Responsibilities**:
- Host bottom navigation
- Manage fragment navigation
- Handle sign-out
- Request notification permissions

**Key Functions**:
```kotlin
onCreate() // Sets up navigation and bottom bar
setupNavigation() // Configures bottom navigation behavior
signOut() // Signs out user and navigates to auth
checkAuthState() // Checks and updates UI based on auth state
askNotificationPermission() // Requests POST_NOTIFICATIONS permission
```

**Android Concepts**:
- **Navigation Component**: Fragment navigation
- **NavController**: Navigation controller
- **ActivityResultLauncher**: Modern permission request API
- **ViewBinding**: Type-safe view references
- **SmoothBottomBar**: Third-party bottom navigation

**Navigation Flow**:
- Home Fragment (index 1)
- Profile Fragment (index 0)
- Settings Fragment (index 2)

---

### 3. AppAuthActivity.kt
**Purpose**: Hosts authentication fragments (Sign In/Sign Up)

**Key Responsibilities**:
- Container for auth fragments
- Handle social auth results
- Mark login page as seen

**Key Functions**:
```kotlin
onCreate() // Initializes social auth helper
onActivityResult() // Handles Facebook login result
```

**Android Concepts**:
- **Fragment Container**: Hosts child fragments
- **SocialAuthHelper**: Wrapper for social authentication

---

### 4. ChangeServerActivity.java
**Purpose**: Server selection screen

**Key Responsibilities**:
- Display list of VPN servers
- Filter servers by protocol and search query
- Select and save server
- Fetch servers from API
- Calculate premium servers

**Key Functions**:
```java
onCreate() // Sets up RecyclerView and loads servers
filterServers(String query) // Filters by search and protocol
loadServerList(List<Server> serverList) // Groups and displays servers
populateServerList() // Fetches from API
groupServersByCountry(List<Server> servers) // Groups by country
selectBestPremiumServer(List<Server> serverList) // Calculates premium servers
triggerServerFetch() // Manually triggers server fetch
```

**Android Concepts**:
- **RecyclerView**: List display
- **ExpandableRecyclerView**: Expandable list items
- **SwipeRefreshLayout**: Pull-to-refresh
- **SearchView**: Search functionality
- **TextWatcher**: Real-time text input monitoring
- **BroadcastReceiver**: Receives server list updates
- **WorkManager**: Background server fetching
- **OkHttp**: HTTP client for API calls

**Premium Server Selection Algorithm**:
1. Deduplicate servers by IP+Port
2. Filter by protocol (TCP/UDP)
3. Filter by load (< 500 sessions)
4. Calculate ping (TCP ping or default)
5. Normalize metrics (ping, speed, sessions)
6. Score servers: `w_ping * score_ping + w_speed * score_speed + w_load * score_load`
7. Select top 20% or minimum 20 as premium

---

### 5. ReportIssueActivity.kt
**Purpose**: Multi-step issue reporting flow

**Key Responsibilities**:
- Step 1: Issue category selection
- Step 2: Quick fix suggestions
- Step 3: Report form submission

**Key Functions**:
```kotlin
onCreate() // Sets up toolbar and progress
showStep(step: Int) // Shows appropriate fragment for step
replaceFragment(fragment: Fragment) // Replaces fragment in container
onIssueCategorySelected(category: String) // Callback from step 1
onContactUsClicked() // Callback from step 2
```

**Android Concepts**:
- **Fragment Transactions**: Dynamic fragment replacement
- **Progress Indicator**: Step progress display
- **MaterialToolbar**: App bar with navigation

---

## Fragments

### 1. HomeFragment.kt
**Purpose**: Main VPN connection screen

**Key Responsibilities**:
- Display selected server
- Connect/disconnect VPN
- Show connection status and speeds
- Display globe visualization
- Handle premium server blocking
- Show banner ads

**Key Functions**:
```kotlin
onCreateView() // Inflates layout
onViewCreated() // Sets up UI and listeners
handleConnectButtonClick() // Handles connect/disconnect
proceedConnectionFlowAfterPremiumCheck() // Checks premium before connecting
updateGoProButtonState() // Updates premium button state
updateState() // Updates VPN status (VpnStatus.StateListener)
updateByteCount() // Updates speed data (VpnStatus.ByteCountListener)
initializeConsentPopup() // Shows GDPR consent
initializeBannerAd() // Loads banner ad
```

**Android Concepts**:
- **Fragment Lifecycle**: onCreate, onViewCreated, onResume, onPause, onDestroy
- **ViewBinding**: Type-safe view access
- **ActivityResultLauncher**: VPN permission request
- **VpnStatus.StateListener**: OpenVPN state monitoring
- **VpnStatus.ByteCountListener**: Network traffic monitoring
- **Coroutines**: Async operations (Dispatchers.IO, Dispatchers.Main)
- **Handler**: UI thread operations
- **Lottie Animations**: Button and loader animations
- **ConsentManager**: GDPR compliance
- **BannerAdManager**: Ad display

**VPN Connection Flow**:
1. User clicks connect button
2. Check if server selected → If not, open server selection
3. Check if server is premium → If yes, verify user subscription
4. Request notification permission (Android 13+)
5. Show interstitial ad (if not premium)
6. Request VPN permission
7. Start VPN connection
8. Update UI based on connection state

**Premium Check Logic**:
- Query Google Play purchases
- Verify with backend API
- Check Firebase subscription status
- Block premium server if user not premium

---

### 2. ProfileFragment.kt
**Purpose**: User profile display

**Key Responsibilities**:
- Show user information
- Display connection history
- Show premium status
- Handle guest vs signed-in states
- Copy device ID/IP to clipboard

**Key Functions**:
```kotlin
onViewCreated() // Sets up UI
setupProfileVisibility() // Shows guest or signed-in profile
loadRecentHistory() // Loads last 3 connections
updatePremiumStatus() // Updates premium badge
updateSelectedServerIp() // Shows selected server IP
updateDeviceUniqueId() // Shows device ID
copyDeviceIdToClipboard() // Copies to clipboard
```

**Android Concepts**:
- **RecyclerView**: History list display
- **ClipboardManager**: Copy to clipboard
- **SharedPreferences**: User data storage
- **Firebase Firestore**: User data retrieval
- **Fragment Result API**: Communication with child fragments

---

### 3. SignInFragment.kt
**Purpose**: User sign-in screen

**Key Responsibilities**:
- Email/password authentication
- Google Sign-In
- Facebook Sign-In
- Form validation
- Network connectivity check
- Email verification check

**Key Functions**:
```kotlin
signInUser() // Handles email/password sign-in
handleAuthResult() // Processes Firebase auth result
signInWithGoogle() // Google Sign-In flow
signInWithFacebook() // Facebook Sign-In flow
saveUserDataToLocalStorage() // Saves to SharedPreferences and AuthManager
uploadDeviceInfoToFirestore() // Uploads device info
isNetworkAvailable() // Checks internet connection
```

**Android Concepts**:
- **Firebase Authentication**: Email/password, Google, Facebook
- **ActivityResultLauncher**: Google Sign-In result
- **TextInputLayout**: Material Design input fields
- **Form Validation**: Input validation
- **Coroutines**: Async operations
- **Firebase Firestore**: User data storage

**Sign-In Flow**:
1. Validate email and password
2. Check network connectivity
3. Check Firebase configuration
4. Sign in with Firebase Auth
5. Check email verification
6. Save user data locally
7. Upload device info to Firestore
8. Navigate to HomeActivity

---

### 4. SignUpFragment.kt
**Purpose**: User registration screen

**Key Responsibilities**:
- Create new user account
- Validate form inputs
- Send email verification
- Social sign-up

**Key Functions**:
```kotlin
registerUser() // Creates new account
isValidPassword() // Validates password strength
isValidEmail() // Validates email format
getPasswordError() // Returns specific password error
saveUserDataToFirestore() // Saves to Firestore
uploadDeviceInfoToFirestore() // Uploads device info
sendEmailVerification() // Sends verification email
```

**Android Concepts**:
- **Password Validation**: Regex pattern matching
- **Email Validation**: Android Patterns.EMAIL_ADDRESS
- **Firebase Auth**: createUserWithEmailAndPassword
- **Email Verification**: sendEmailVerification()

**Password Requirements**:
- Minimum 8 characters
- At least 1 uppercase letter
- At least 1 lowercase letter
- At least 1 number
- At least 1 special character (@#$%^&+=!)

---

### 5. PremiumFragment.kt
**Purpose**: Subscription purchase screen

**Key Responsibilities**:
- Display subscription plans
- Handle Google Play Billing
- Verify subscriptions with backend
- Show loading dialog
- Navigate after purchase

**Key Functions**:
```kotlin
onCreateView() // Sets up UI
initBillingClient() // Initializes Google Play Billing
queryProductDetails() // Fetches subscription products
launchSubscription(monthly: Boolean) // Starts purchase flow
onPurchasesUpdated() // Handles purchase result
handlePurchase() // Processes purchase
grantEntitlement() // Verifies with backend and grants access
queryActiveSubscriptionsAndPersist() // Checks existing subscriptions
showLoadingDialog() // Shows verification dialog
hideLoadingDialog() // Hides dialog
```

**Android Concepts**:
- **Google Play Billing Library**: Subscription management
- **BillingClient**: Billing service connection
- **ProductDetails**: Subscription product info
- **Purchase**: Purchase object
- **AcknowledgePurchase**: Acknowledge purchase
- **Coroutines**: Async backend verification
- **AlertDialog**: Loading dialog
- **Navigation Component**: Fragment navigation

**Purchase Flow**:
1. User selects plan (monthly/yearly)
2. Launch billing flow
3. User completes purchase in Play Store
4. onPurchasesUpdated() called
5. Acknowledge purchase if needed
6. Verify with backend API
7. Save to Firebase
8. Update local preferences
9. Navigate to AfterPremiumFragment

---

### 6. settingFragment.kt
**Purpose**: App settings screen

**Key Responsibilities**:
- Display account name
- Sign out functionality
- Open notification settings
- Show rating dialog
- Navigate to other settings

**Key Functions**:
```kotlin
onViewCreated() // Sets up click listeners
updateAccountName() // Updates account name display
showSignOutConfirmationDialog() // Shows confirmation
performSignOut() // Signs out user
openNotificationSettings() // Opens system settings
showRatingDialog() // Shows Play Store rating dialog
```

**Android Concepts**:
- **Settings Intent**: ACTION_APP_NOTIFICATION_SETTINGS
- **Rating Dialog**: Custom rating UI
- **Sign Out**: Firebase Auth signOut()
- **SharedPreferences**: Clear user data

---

## Managers

### 1. VPNManager.kt
**Purpose**: Core VPN connection management

**Key Responsibilities**:
- Start/stop VPN connections
- Parse OpenVPN configuration
- Handle VPN state changes
- Manage broadcast receivers
- Track connection history
- Update notifications

**Key Functions**:
```kotlin
prepareVPN() // Requests VPN permission
startVpn() // Starts VPN connection
stopVPN() // Stops VPN connection
registerBroadcastReceiver() // Registers for status updates
unregisterBroadcastReceiver() // Unregisters receiver
updateVPNState() // Updates internal state
setStatus() // Handles connection state changes
queryActiveSubscriptionPurchases() // Queries Google Play subscriptions
```

**Android Concepts**:
- **VpnService**: VPN permission and service
- **OpenVPN Library**: de.blinkt.openvpn
- **ConfigParser**: OpenVPN config parsing
- **VPNLaunchHelper**: VPN startup
- **ProfileManager**: VPN profile management
- **BroadcastReceiver**: Status updates
- **LocalBroadcastManager**: Local broadcasts
- **Handler**: Periodic updates
- **BillingClient**: Subscription queries

**VPN Start Process**:
1. Check internet connection
2. Request VPN permission (if needed)
3. Get selected server from SharedPreferences
4. Parse .ovpn config string
5. Create VpnProfile
6. Validate profile
7. Set username/password (vpn/vpn)
8. Configure cipher compatibility
9. Set as temporary profile
10. Start VPN using VPNLaunchHelper
11. Track in history

**State Management**:
- CONNECTED: VPN is active
- DISCONNECTED: VPN stopped
- CONNECTING: Connection in progress
- RECONNECTING: Reconnecting after failure
- AUTH_FAILED: Authentication failed
- CONNECTION_FAILED: Connection failed

---

### 2. ConnectionStatusManager.kt
**Purpose**: Manages VPN connection status UI

**Key Responsibilities**:
- Update connection status text
- Update speed displays
- Animate button states
- Handle Lottie animations

**Key Functions**:
```kotlin
updateConnectionStatus() // Updates duration and speeds
updateConnectionSpeeds() // Updates speeds from bytes/second
updateVPNStatusText() // Updates status text
animateButtonClick() // Button click animation
updateConnectButtonUI() // Updates button based on state
animateConnecting() // Shows connecting animation
animateConnectionSuccess() // Shows connected state
animateDisconnection() // Shows disconnected state
```

**Android Concepts**:
- **Lottie Animations**: JSON-based animations
- **Animation**: Scale animations
- **Handler**: UI updates
- **Color Filter**: Dynamic color changes
- **View Visibility**: Show/hide views

**Animation States**:
- **Default**: Button animation (violet)
- **Connecting**: Button animation (orange) + Loader animation
- **Connected**: Button animation (violet) + White icon
- **Disconnected**: Connect button card visible

---

### 3. ServerManager.kt
**Purpose**: Manages server selection and display

**Key Responsibilities**:
- Load/save selected server
- Update server UI
- Display country flags
- Show ping indicators

**Key Functions**:
```kotlin
updateServer() // Updates selected server
loadSavedServer() // Loads from SharedPreferences
saveCurrentServer() // Saves current selection
updateServerUI() // Updates UI with server info
parsePing() // Parses ping value
getSignalResId() // Gets signal strength icon
```

**Android Concepts**:
- **FlagKit**: Country flag library
- **SharedPreferences**: Server storage
- **Image Resources**: Dynamic resource loading

---

### 4. AdManager.kt
**Purpose**: Centralized ad management

**Key Responsibilities**:
- Preload ads (Interstitial, Rewarded, Native)
- Show ads with consent awareness
- Handle ad lifecycle
- Retry failed ad loads
- Suppress ads for premium users

**Key Functions**:
```kotlin
initialize() // Initializes ad manager
preloadInterstitialAd() // Preloads interstitial
preloadRewardedAd() // Preloads rewarded
showInterstitialAd() // Shows interstitial
showRewardedAd() // Shows rewarded
createAdRequest() // Creates consent-aware ad request
isUserPremium() // Checks premium status
```

**Android Concepts**:
- **Google Mobile Ads SDK**: AdMob integration
- **InterstitialAd**: Full-screen ads
- **RewardedAd**: Rewarded video ads
- **NativeAd**: Native ad format
- **AdRequest**: Ad request builder
- **ConsentManager**: GDPR compliance
- **FullScreenContentCallback**: Ad lifecycle callbacks

**Ad Types**:
- **Banner**: Managed by BannerAdManager
- **Interstitial**: Shown before VPN connect/disconnect
- **Rewarded**: For earning premium time
- **Native**: (Not fully implemented)

**Consent Handling**:
- PERSONALIZED: Full ad targeting
- NON_PERSONALIZED: Limited targeting (npa=1)
- NOT_REQUIRED: No consent needed
- UNKNOWN: Default to personalized

---

### 5. NotificationManager.kt
**Purpose**: VPN notification management

**Key Responsibilities**:
- Create persistent VPN notification
- Update notification with speeds
- Remove notification on disconnect

**Android Concepts**:
- **NotificationCompat**: Notification builder
- **Foreground Service**: Persistent notification
- **Notification Channel**: Android 8+ channels

---

### 6. GlobeManager.kt
**Purpose**: 3D globe visualization

**Key Responsibilities**:
- Display world map
- Show user location
- Show VPN server location
- Update on connection state

**Android Concepts**:
- **WebView**: HTML/JavaScript globe
- **Location Services**: User location
- **JSON Assets**: Country coordinates

---

### 7. BannerAdManager.kt
**Purpose**: Banner ad management

**Key Responsibilities**:
- Initialize banner ads
- Load ads with consent
- Handle ad lifecycle (resume/pause/destroy)

**Android Concepts**:
- **AdView**: Banner ad view
- **AdRequest**: Ad loading
- **AdListener**: Ad events

---

## Utils & Helpers

### 1. AuthManager.kt
**Purpose**: Centralized authentication management

**Key Responsibilities**:
- Manage sign-in state
- Cache user data
- Sync with Firebase Auth
- Handle session restoration

**Key Functions**:
```kotlin
isUserSignedIn() // Checks sign-in status
saveAuthState() // Saves after sign-in
clearAuthState() // Clears on sign-out
getCurrentUserId() // Gets user ID
getCurrentUserEmail() // Gets email
getCurrentUserName() // Gets name
waitForAuthRestoration() // Waits for Firebase session restore
validateAndCleanAuthState() // Validates state consistency
```

**Android Concepts**:
- **Singleton Pattern**: Single instance
- **SharedPreferences**: Local caching
- **Firebase Auth**: Authentication
- **AuthStateListener**: Real-time auth monitoring
- **Handler**: Timeout handling

**State Management**:
- Caches user data in SharedPreferences for fast access
- Syncs with Firebase Auth for accuracy
- Handles session restoration on app start
- Validates state consistency

---

### 2. SubscriptionSyncManager.kt
**Purpose**: Subscription data synchronization

**Key Responsibilities**:
- Save subscription to Firebase
- Restore subscription from Firebase
- Handle pending subscriptions (before sign-in)
- Enforce local expiry
- Clear subscription on sign-out

**Key Functions**:
```kotlin
saveBackendVerifiedSubscription() // Saves verified subscription
restoreSubscriptionFromFirebase() // Restores from Firebase
flushPendingSubscriptionSnapshot() // Flushes pending data after sign-in
clearSubscriptionFromFirebase() // Clears on sign-out
isLocalPaidSubscriptionActive() // Checks local premium status
enforceLocalExpiryAndSync() // Enforces expiry
```

**Android Concepts**:
- **Firebase Firestore**: Cloud database
- **SetOptions.merge()**: Merge updates
- **SharedPreferences**: Local cache
- **Timestamp Comparison**: Expiry checking

**Subscription Flow**:
1. Purchase verified with backend
2. Save to Firebase (if signed in) or pending (if not)
3. Update local SharedPreferences
4. On app start, restore from Firebase
5. Check backend snapshot for authoritative status
6. Enforce expiry locally

---

### 3. AuthFlowManager.kt
**Purpose**: Manages authentication flow state

**Key Responsibilities**:
- Track first-time login
- Determine navigation destination
- Mark login as seen

**Key Functions**:
```kotlin
isFirstTimeLogin() // Checks if first time
markLoginSeen() // Marks login page as seen
markSuccessfulLogin() // Marks successful login
getDestinationClass() // Returns appropriate activity
resetFirstTimeLogin() // Resets on sign-out
```

**Android Concepts**:
- **SharedPreferences**: Flow state storage

---

### 4. ConsentManager.kt
**Purpose**: GDPR consent management

**Key Responsibilities**:
- Show consent dialog
- Save consent choice
- Determine ad consent status

**Android Concepts**:
- **GDPR Compliance**: EU privacy regulations
- **User Consent**: Explicit consent collection

---

### 5. HistoryManager.kt
**Purpose**: Connection history management

**Key Responsibilities**:
- Track connection start/stop
- Record connection duration
- Save to database
- Handle connection failures

**Key Functions**:
```kotlin
onConnectionStarted() // Records start
onConnectionEstablished() // Records successful connection
onConnectionDisconnected() // Records disconnect
onConnectionFailed() // Records failure
```

**Android Concepts**:
- **SQLite Database**: History storage
- **ContentProvider**: Database access

---

### 6. SocialAuthHelper.kt
**Purpose**: Social authentication wrapper

**Key Responsibilities**:
- Handle Google Sign-In
- Handle Facebook Sign-In
- Process auth results
- Save auth state

**Android Concepts**:
- **Google Sign-In API**: Google authentication
- **Facebook SDK**: Facebook authentication
- **ActivityResultLauncher**: Modern result handling

---

### 7. ThemeManager.kt
**Purpose**: Theme management

**Key Responsibilities**:
- Apply dark/light theme
- Save theme preference

**Android Concepts**:
- **AppCompatDelegate**: Theme switching
- **MODE_NIGHT_YES/NO**: Dark mode flags

---

### 8. RatingDialogManager.kt
**Purpose**: Play Store rating prompts

**Key Responsibilities**:
- Show rating dialog after first VPN use
- Limit to once per day
- Track if user has rated

**Android Concepts**:
- **SharedPreferences**: Rating state
- **Intent.ACTION_VIEW**: Play Store link

---

### 9. AnimatedBorderManager.kt
**Purpose**: Animated gradient border for premium button

**Key Responsibilities**:
- Start/stop animated border
- Handle navigation-based animation
- Manage animation state

**Android Concepts**:
- **Custom View**: AnimatedGradientBorderView
- **Animation**: Gradient animation

---

### 10. SpeedCalculator.kt
**Purpose**: Network speed calculations

**Key Responsibilities**:
- Parse formatted byte strings
- Calculate bytes per second
- Format speeds with units

**Key Functions**:
```kotlin
parseFormattedByteString() // Parses "1.5 MB" format
formatSpeedWithUnit() // Formats with B/KB/MB/GB
```

---

### 11. UserDataLoader.kt
**Purpose**: User data loading utility

**Key Responsibilities**:
- Load user name from multiple sources
- Load user email
- Handle fallbacks

---

### 12. CustomDialogManager.kt
**Purpose**: Custom dialog utilities

**Key Responsibilities**:
- Show custom dialogs
- Handle dialog lifecycle

---

### 13. EmailIntentHelper.kt
**Purpose**: Email intent creation

**Key Responsibilities**:
- Create email intents
- Attach files
- Pre-fill subject/body

---

### 14. FirebaseUtils.kt
**Purpose**: Firebase utility functions

**Key Responsibilities**:
- Check Firebase configuration
- Check user existence
- Get error messages

---

### 15. KeyHashGenerator.kt
**Purpose**: Generate key hashes for Facebook

**Key Responsibilities**:
- Generate SHA1 hash
- Generate SHA256 hash
- Log for debugging

**Android Concepts**:
- **PackageManager**: Package info
- **MessageDigest**: Hash generation

---

### 16. ViewUtils.kt
**Purpose**: View utility functions

**Key Responsibilities**:
- Extension functions for views
- Common view operations

---

### 17. Utils.java
**Purpose**: General utility functions

**Key Functions**:
```java
tcpPing() // TCP ping calculation
loadCountryCoordinates() // Loads country data from JSON
```

**Android Concepts**:
- **Socket**: TCP connection
- **JSON Parsing**: Gson library

---

### 18. CsvParser.java
**Purpose**: Parse VPN Gate CSV data

**Key Functions**:
```java
parse(Response response) // Parses CSV response to Server list
```

**Android Concepts**:
- **OkHttp Response**: HTTP response
- **CSV Parsing**: Manual parsing
- **String Manipulation**: Split, trim operations

---

### 19. PremiumServerUtils.java
**Purpose**: Premium server calculation

**Key Functions**:
```java
calculatePremiumServers() // Calculates which servers are premium
```

**Algorithm**:
- Deduplication
- Protocol filtering
- Load filtering
- Ping calculation
- Scoring algorithm
- Top 20% selection

---

### 20. OvpnUtils.java
**Purpose**: OpenVPN configuration utilities

**Key Functions**:
- OpenVPN config parsing helpers
- Config validation

---

## Database Layer

### 1. HistoryDatabase.java
**Purpose**: SQLite database schema for connection history

**Schema**:
```sql
CREATE TABLE history (
    _id INTEGER PRIMARY KEY AUTOINCREMENT,
    server_name TEXT,
    server_country TEXT,
    server_ip TEXT,
    connection_date INTEGER,
    duration INTEGER,
    status TEXT,
    data_used INTEGER
)
```

**Android Concepts**:
- **SQLite**: Local database
- **Contract Class**: Schema definition
- **CREATE TABLE**: Table creation

---

### 2. HistoryContract.java
**Purpose**: Database contract (table/column names)

**Key Constants**:
- TABLE_NAME
- Column names (_ID, COLUMN_NAME_SERVER_NAME, etc.)

---

### 3. HistoryHelper.java
**Purpose**: Database helper for history operations

**Key Functions**:
```java
getRecentHistory(int limit) // Gets last N connections
insertHistory() // Inserts new history entry
deleteHistory() // Deletes history
```

**Android Concepts**:
- **SQLiteDatabase**: Database operations
- **ContentValues**: Insert values
- **Cursor**: Query results

---

### 4. DbHelper.java
**Purpose**: Database helper for server storage

**Key Functions**:
```java
getAll() // Gets all servers
save(List<Server> servers) // Saves server list
```

---

### 5. ServerDatabase.java
**Purpose**: Server database schema

---

### 6. ServerContract.java
**Purpose**: Server database contract

---

## Data Models

### 1. Server.java
**Purpose**: VPN server data model

**Properties**:
- hostName, ipAddress, ping, speed
- countryLong, countryShort
- ovpnConfigData, port, protocol
- vpnSessions, uptime, totalUsers
- isStarred, isPremium

**Key Features**:
- **Parcelable**: Can be passed via Intent
- **Getters/Setters**: Standard Java pattern

**Android Concepts**:
- **Parcelable Interface**: Object serialization
- **writeToParcel()**: Serialization
- **CREATOR**: Deserialization

---

### 2. History.java
**Purpose**: Connection history data model

**Properties**:
- serverName, serverCountry, serverIp
- connectionDate, duration, status, dataUsed

---

### 3. CountryServerGroup.java
**Purpose**: Groups servers by country

**Properties**:
- countryName, countryCode
- List<Server> servers

---

### 4. CoinHistory.kt
**Purpose**: Reward coin history model

---

### 5. SubscriptionModels.kt
**Purpose**: Subscription data models

**Models**:
- SubscriptionStatus: status, expiryTimeMillis, autoRenewing
- SubscriptionRequest/Response: API models

---

### 6. ThemeViewModel.kt
**Purpose**: Theme state management

---

## Networking & API

### 1. SubscriptionService.kt
**Purpose**: Retrofit API interface for subscription verification

**Key Functions**:
```kotlin
@POST("verify-subscription")
suspend fun verifySubscription(@Body request: SubscriptionRequest): Response<SubscriptionResponse>
```

**Android Concepts**:
- **Retrofit**: HTTP client library
- **Suspend Functions**: Coroutine support
- **@POST Annotation**: HTTP method
- **@Body Annotation**: Request body

---

### 2. SubscriptionRepository.kt
**Purpose**: Repository for subscription operations

**Key Functions**:
```kotlin
checkSubscription() // Verifies subscription with backend
```

**Android Concepts**:
- **Repository Pattern**: Data access abstraction
- **Result Type**: Success/Failure handling
- **Coroutines**: Async operations

---

## Workers & Background Tasks

### 1. ServerFetchWorker.kt
**Purpose**: Periodic server list fetching

**Key Responsibilities**:
- Fetch server list from VPN Gate API
- Parse CSV response
- Calculate premium servers
- Save to database and cache
- Broadcast completion

**Key Functions**:
```kotlin
doWork() // Worker execution
```

**Android Concepts**:
- **CoroutineWorker**: Kotlin coroutine-based worker
- **WorkManager**: Background task scheduling
- **OkHttp**: HTTP client
- **LocalBroadcastManager**: Broadcast completion
- **Dispatchers.IO**: Background thread

**Work Flow**:
1. Make API request
2. Parse CSV response
3. Calculate premium servers
4. Save to SharedPreferences cache
5. Save to SQLite database
6. Broadcast completion
7. Return Result.success()

---

### 2. SubscriptionVerifyWorker.kt
**Purpose**: Daily subscription verification

**Key Responsibilities**:
- Verify subscriptions daily
- Update Firebase
- Enforce expiry

---

### 3. SubscriptionExpiryWorker.kt
**Purpose**: Enforce subscription expiry

**Key Responsibilities**:
- Check subscription expiry
- Clear expired subscriptions
- Update UI

---

## Android Concepts Used

### 1. Activity Lifecycle
- **onCreate()**: Initialization
- **onResume()**: Refresh data
- **onPause()**: Pause operations
- **onDestroy()**: Cleanup

### 2. Fragment Lifecycle
- **onAttach()**: Context available
- **onCreate()**: Fragment creation
- **onCreateView()**: Inflate layout
- **onViewCreated()**: View setup
- **onResume()**: Fragment visible
- **onPause()**: Fragment hidden
- **onDestroyView()**: View destroyed
- **onDestroy()**: Fragment destroyed

### 3. View Binding
- Type-safe view access
- Null safety
- Compile-time view resolution

### 4. Navigation Component
- **NavController**: Navigation control
- **NavGraph**: Navigation graph
- **Fragment Destinations**: Navigation targets
- **Safe Args**: Type-safe arguments

### 5. SharedPreferences
- Key-value storage
- **MODE_PRIVATE**: Private access
- **commit()**: Synchronous save
- **apply()**: Asynchronous save

### 6. BroadcastReceiver
- **LocalBroadcastManager**: Local broadcasts
- **IntentFilter**: Broadcast filtering
- **registerReceiver()**: Registration
- **unregisterReceiver()**: Unregistration

### 7. WorkManager
- **PeriodicWorkRequest**: Recurring tasks
- **OneTimeWorkRequest**: One-time tasks
- **Constraints**: Work constraints
- **WorkInfo**: Work status

### 8. Permissions
- **Runtime Permissions**: POST_NOTIFICATIONS
- **System Permissions**: VPN permission
- **ActivityResultLauncher**: Modern API
- **checkSelfPermission()**: Permission check

### 9. Services
- **VpnService**: VPN service
- **Foreground Service**: Persistent notification

### 10. Notifications
- **NotificationCompat**: Notification builder
- **NotificationChannel**: Android 8+ channels
- **PendingIntent**: Notification actions

### 11. Firebase
- **Firebase Auth**: Authentication
- **Firebase Firestore**: Cloud database
- **AuthStateListener**: Auth state monitoring

### 12. Google Play Billing
- **BillingClient**: Billing service
- **ProductDetails**: Product information
- **Purchase**: Purchase object
- **AcknowledgePurchase**: Purchase acknowledgment

### 13. RecyclerView
- **Adapter**: Data binding
- **ViewHolder**: View recycling
- **LayoutManager**: Layout management
- **ItemDecoration**: Item decoration

### 14. Coroutines
- **suspend functions**: Async operations
- **Dispatchers.IO**: Background thread
- **Dispatchers.Main**: Main thread
- **CoroutineScope**: Scope management

### 15. Retrofit
- **API Interface**: HTTP endpoints
- **@POST/@GET**: HTTP methods
- **@Body**: Request body
- **Response**: HTTP response

---

## Kotlin Concepts Used

### 1. Null Safety
- **Nullable Types**: `String?`
- **Safe Call**: `?.`
- **Elvis Operator**: `?:`
- **Non-null Assertion**: `!!`

### 2. Data Classes
- Automatic equals(), hashCode(), toString()
- Component functions

### 3. Extension Functions
- Add functions to existing classes
- `Context.toast()`

### 4. Higher-Order Functions
- Functions as parameters
- Lambda expressions

### 5. Coroutines
- **suspend**: Suspendable functions
- **launch**: Start coroutine
- **withContext**: Switch dispatcher
- **async/await**: Concurrent operations

### 6. Sealed Classes
- Restricted class hierarchies
- When expressions

### 7. Companion Objects
- Static-like members
- Singleton instance

### 8. Object Expressions
- Anonymous objects
- Interface implementations

### 9. Inline Functions
- Function inlining
- Performance optimization

### 10. Delegates
- **lazy**: Lazy initialization
- **by**: Delegate pattern

### 11. Scope Functions
- **let**: Null-safe operations
- **apply**: Object configuration
- **run**: Object operations
- **with**: Non-extension context

---

## Java Concepts Used

### 1. OOP Principles
- **Encapsulation**: Private fields, public methods
- **Inheritance**: Class extension
- **Polymorphism**: Interface implementation

### 2. Interfaces
- **Parcelable**: Object serialization
- **Callbacks**: Event handling

### 3. Collections
- **List**: Ordered collection
- **Map**: Key-value pairs
- **ArrayList**: Dynamic array
- **LinkedHashMap**: Ordered map

### 4. Exception Handling
- **try-catch**: Error handling
- **throws**: Exception declaration

### 5. Generics
- **Type Parameters**: `<T>`
- **TypeToken**: Gson generic types

### 6. Annotations
- **@Override**: Method override
- **@NonNull**: Non-null parameter

### 7. Static Members
- **static final**: Constants
- **static methods**: Utility functions

---

## Interview Questions & Answers

### Q1: Explain the VPN connection flow in this app.

**Answer**:
1. User clicks connect button in HomeFragment
2. Check if server is selected → If not, open ChangeServerActivity
3. Check if server is premium → If yes, verify user subscription with backend
4. Request POST_NOTIFICATIONS permission (Android 13+)
5. Show interstitial ad (if user not premium)
6. Request VPN permission via VpnService.prepare()
7. If permission granted, call VPNManager.startVpn()
8. Parse .ovpn config string using ConfigParser
9. Create VpnProfile and validate
10. Set username/password and cipher compatibility
11. Start VPN using VPNLaunchHelper.startOpenVpn()
12. Register BroadcastReceiver for status updates
13. Update UI based on connection state (CONNECTING → CONNECTED)
14. Track connection in HistoryManager
15. Update notification with connection status and speeds

---

### Q2: How does the premium server selection algorithm work?

**Answer**:
The algorithm in `ChangeServerActivity.selectBestPremiumServer()`:

1. **Deduplication**: Remove duplicate servers by IP+Port combination
2. **Protocol Filtering**: Keep only TCP or UDP servers
3. **Load Filtering**: Exclude servers with >500 active sessions
4. **Ping Calculation**: 
   - TCP: Actual TCP ping to server
   - UDP: Use reported ping or default value
5. **Normalization**: Find min/max for ping, speed, and sessions
6. **Scoring**: Calculate weighted score:
   - For TCP: `w_ping(0.5) * score_ping + w_speed(0.3) * score_speed + w_load(0.2) * score_load`
   - For UDP: Only speed and load (ping ignored)
7. **Selection**: Top 20% or minimum 20 servers marked as premium
8. **Caching**: Save updated list with premium flags to SharedPreferences

---

### Q3: Explain the authentication flow.

**Answer**:
1. **SplashActivity**: Checks if first-time user → Shows onboarding
2. **AppAuthActivity**: Hosts SignIn/SignUp fragments
3. **Sign-In Options**:
   - **Email/Password**: Firebase Auth → Email verification check → Save to AuthManager
   - **Google**: Google Sign-In API → Firebase Auth → Save to AuthManager
   - **Facebook**: Facebook SDK → Firebase Auth → Save to AuthManager
4. **AuthManager**: 
   - Saves to SharedPreferences (fast access)
   - Syncs with Firebase Auth (authoritative)
   - Handles session restoration
5. **After Sign-In**: 
   - Upload device info to Firestore
   - Restore subscription from Firebase
   - Navigate to HomeActivity with post-login splash flag

---

### Q4: How does subscription management work?

**Answer**:
1. **Purchase Flow** (PremiumFragment):
   - Initialize BillingClient
   - Query product details (monthly/yearly)
   - Launch billing flow
   - Handle purchase in onPurchasesUpdated()
   - Acknowledge purchase if needed
   
2. **Verification**:
   - Send purchase to backend API (SubscriptionRepository)
   - Backend verifies with Google Play
   - Returns subscription status (active/inactive, expiry)
   
3. **Storage**:
   - Save to Firebase Firestore (if user signed in)
   - Save to SharedPreferences (local cache)
   - Save backend snapshot (authoritative)
   
4. **Restoration** (SubscriptionSyncManager):
   - On app start, restore from Firebase
   - Check backend snapshot first (most recent)
   - Update local preferences
   - Enforce expiry locally
   
5. **Enforcement**:
   - SubscriptionExpiryWorker runs every 15 minutes
   - Checks expiry and clears if expired
   - SubscriptionVerifyWorker runs daily for verification

---

### Q5: Explain the ad integration architecture.

**Answer**:
1. **Consent Management** (ConsentManager):
   - Shows GDPR consent dialog on first launch
   - Saves user choice (Personalized/Non-personalized)
   
2. **Ad Preloading** (AdManager):
   - Preloads Interstitial and Rewarded ads on app start
   - Retries failed loads (max 3 attempts)
   - Caches loaded ads
   
3. **Ad Display**:
   - **Banner**: Managed by BannerAdManager, shown in fragments
   - **Interstitial**: Shown before VPN connect/disconnect (if not premium)
   - **Rewarded**: For earning premium time
   
4. **Consent-Aware Requests**:
   - Personalized: Full targeting
   - Non-personalized: Add "npa=1" bundle
   
5. **Premium Suppression**:
   - Ads suppressed for premium users
   - Checked via SubscriptionSyncManager

---

### Q6: How does the server list fetching work?

**Answer**:
1. **Periodic Fetch** (ServerFetchWorker):
   - Scheduled every 15 minutes via WorkManager
   - Runs in background
   
2. **Fetch Process**:
   - Make HTTP request to VPN Gate API (CSV format)
   - Parse CSV using CsvParser
   - Calculate premium servers using PremiumServerUtils
   - Save to SharedPreferences (JSON cache)
   - Save to SQLite database
   - Broadcast completion
   
3. **Manual Refresh**:
   - User pulls to refresh in ChangeServerActivity
   - Triggers OneTimeWorkRequest
   - Observes WorkInfo for completion
   
4. **Caching**:
   - Load from cache first (fast UX)
   - Update in background
   - Broadcast notifies UI to refresh

---

### Q7: Explain the connection history tracking.

**Answer**:
1. **HistoryManager** tracks:
   - Connection start (server, timestamp)
   - Connection established (success)
   - Connection disconnected (duration calculated)
   - Connection failed (error reason)
   
2. **Storage** (HistoryDatabase):
   - SQLite table: server_name, server_country, server_ip, connection_date, duration, status, data_used
   - HistoryHelper provides CRUD operations
   
3. **Display**:
   - ProfileFragment shows last 3 connections
   - HistoryFragment shows full history
   - SimpleHistoryAdapter displays list

---

### Q8: How does the app handle VPN state changes?

**Answer**:
1. **OpenVPN Library** (de.blinkt.openvpn):
   - VpnStatus.StateListener: Connection state changes
   - VpnStatus.ByteCountListener: Network traffic updates
   
2. **BroadcastReceiver** (VPNManager):
   - Receives "connectionState" broadcasts
   - Updates internal state
   - Notifies ConnectionStatusManager
   
3. **UI Updates** (ConnectionStatusManager):
   - Updates status text (Connected/Disconnected/Connecting)
   - Updates speed displays
   - Animates button states
   - Shows/hides loader animation
   
4. **Notification Updates** (NotificationManager):
   - Updates persistent notification
   - Shows current speeds
   - Removes on disconnect

---

### Q9: Explain the dark mode implementation.

**Answer**:
1. **Storage**: SharedPreference.setDarkModeEnabled()
2. **Application**: ThemeManager.applyTheme() on app start
3. **Switching**: AppCompatDelegate.setDefaultNightMode()
   - MODE_NIGHT_YES: Dark mode
   - MODE_NIGHT_NO: Light mode
4. **Resources**: values-night/ folder for dark theme resources

---

### Q10: How does the app handle offline scenarios?

**Answer**:
1. **Server List**: 
   - Cached in SharedPreferences (JSON)
   - Cached in SQLite database
   - Load from cache if API fails
   
2. **Authentication**:
   - Cached in SharedPreferences
   - Firebase Auth handles offline (restores on reconnect)
   
3. **Subscription**:
   - Cached locally
   - Backend snapshot for authoritative status
   - Enforced locally until next verification

---

## Key Takeaways for Interview

1. **Architecture**: MVVM + Manager Pattern + Repository Pattern
2. **VPN**: OpenVPN library integration, VpnService, state management
3. **Authentication**: Firebase Auth, social login, session management
4. **Billing**: Google Play Billing, backend verification, Firebase sync
5. **Ads**: AdMob integration, GDPR compliance, premium suppression
6. **Background Tasks**: WorkManager, CoroutineWorker, periodic updates
7. **Database**: SQLite, SharedPreferences, Firebase Firestore
8. **Networking**: OkHttp, Retrofit, CSV parsing
9. **UI**: Navigation Component, View Binding, Lottie animations
10. **Kotlin**: Coroutines, Extension functions, Null safety
11. **Java**: Parcelable, Collections, Exception handling

---

## Code Quality Practices Used

1. **Separation of Concerns**: Managers handle business logic
2. **Singleton Pattern**: Shared resources (AuthManager, AdManager)
3. **Repository Pattern**: Data access abstraction
4. **Error Handling**: Try-catch, Result types
5. **Logging**: Comprehensive logging for debugging
6. **Caching**: Multiple cache layers for performance
7. **State Management**: Centralized state management
8. **Lifecycle Awareness**: Proper lifecycle handling
9. **Memory Management**: Null binding cleanup, weak references
10. **Security**: Backend verification, encrypted storage

---

**End of Documentation**

