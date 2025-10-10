# NocturneVPN

Modern Android VPN client built with Kotlin and Jetpack. It offers fast, reliable connections, intuitive server selection, and a polished UI with dark/light themes.

This app uses a dedicated VPN module built on Android `VpnService` and ships with sensible defaults for modern devices while remaining compatible with older Android versions via the `vpnLib` library.

## Features

- Fast and stable VPN connection using open standards
- Server selection by country and latency
- 3D globe visualization (CesiumJS) of the connected IP location
- Clean, responsive UI with Material 3 and dynamic theming
- Session status, bandwidth, and connection duration indicators
- Basic analytics and crash reporting (optional)

## Architecture & Tech Stack

- Language: Kotlin
- UI: Jetpack (Fragments/Views), Material 3, Navigation
- Background: WorkManager
- Networking/VPN: Android `VpnService` (via `vpnLib` module)
- HTTP: Retrofit2, OkHttp (with logging)
- Images/Animations: Glide, Lottie
- Data/Async: Kotlin Coroutines/Flow, LiveData (where applicable)
- Build: Gradle Kotlin DSL (AGP 8.5.x), Kotlin 2.0.x
- Services: Firebase (Auth, Analytics, Firestore), Google Sign‑In, Facebook Login
- Monetization/Compliance: Google Play Billing, Google Mobile Ads (AdMob), UMP (Consent)

### Modules

- `app`: Android application module (UI, navigation, app wiring)
- `vpnLib`: Core VPN implementation and utilities (AIDL enabled, consumer ProGuard, ndkVersion pinned)

## Project Structure

```
NocturneVPN/
  app/
    src/main/...            # UI, fragments, view models, resources
  vpnLib/
    src/main/...            # VPN service, networking, helpers
```

## Requirements

- Android Studio (Giraffe/Iguana or newer)
- JDK 17
- Android SDK Platform 35 (compileSdk/targetSdk 35)
- Min SDK: 24 for `app` (library supports down to 16)
- Gradle (use included wrapper)
- NDK installed (library uses `ndkVersion 29.0.13113456`)

## Getting Started

1) Clone the repository
```
git clone https://github.com/Mitul4212/NocturneVPN.git
cd NocturneVPN
```

2) Open in Android Studio
- Open the root project directory.
- Let Gradle sync complete.

3) Configure optional services
- Firebase: ensure `app/google-services.json` exists if you plan to use Auth/Analytics/Firestore. Otherwise remove the Google Services plugin and Firebase deps.
- Social Auth: configure Firebase Auth providers (Google/Facebook) or remove dependencies.
- Ads & Consent: if using AdMob, integrate UMP (Consent) and set up ad units in the console.

4) Build & run
```
./gradlew assembleDebug
./gradlew installDebug
```
- Or use the Android Studio Run button to launch on a device/emulator.

On Windows PowerShell, use:
```
./gradlew.bat assembleDebug
```

## App Signing (Release)

1) Create or use an existing keystore
2) Configure signing in `gradle.properties` or Android Studio Build Variants
3) Build
```
./gradlew assembleRelease
```
Artifacts are available under `app/release/` and `app/build/outputs/`.

## Configuration

- Environment/Secrets: keep API keys and service configs out of VCS. Use local `gradle.properties` or CI secrets.
- Permissions: required permissions are declared in `AndroidManifest.xml` (e.g., `BIND_VPN_SERVICE`, `INTERNET`, `ACCESS_NETWORK_STATE`, `FOREGROUND_SERVICE`). Review and adjust as needed.
- Application ID & namespace: `com.nocturnevpn` (see `app/build.gradle.kts`).
- BuildConfig fields (in `app`):
  - `BuildConfig.VPN_GATE_API` → defaults to `http://www.vpngate.net/api/iphone/`
  - `BuildConfig.DISABLE_THIRD_PARTY_SDKS` → set to `true` to skip initializing third‑party SDKs during troubleshooting
- Bundling: language/density/ABI splits are disabled for a single base APK.
- Packaging: `jniLibs.useLegacyPackaging = true` to ensure native libs are extractable for OpenVPN executables.
- View/Data Binding: both enabled.
- MultiDex: enabled.

## Notable Dependencies

- Core: `androidx.core:core-ktx`, `appcompat`, `material`, `constraintlayout`
- Navigation: `androidx.navigation:navigation-fragment-ktx`, `navigation-ui-ktx`
- Background: `work-runtime-ktx`
- Networking: `retrofit2`, `converter-gson`, `okhttp`, `logging-interceptor`
- Images/Animations: `glide`, `lottie`
- Firebase (via BoM): `firebase-auth`, `firebase-analytics`, `firebase-firestore-ktx`
- Auth Providers: `play-services-auth` (Google), `facebook-login`
- Monetization/Consent: `billing-ktx`, `play-services-ads`, `user-messaging-platform`
- UI helpers: `SmoothBottomBar`, `flagkit-android`, `facebook shimmer`
- Web: `webkit`, Cronet via `play-services-cronet`

## Screenshots / Demo

> Add screenshots or GIFs here to showcase onboarding, server list, connection screen, and the 3D globe view.

## Badges (optional)

> Add CI/build, code quality, or release badges here.

## Troubleshooting

- Gradle sync issues: use the bundled Gradle wrapper and match Android Gradle Plugin version.
- VPN connection fails: verify device supports `VpnService` and that network policies don’t block VPNs.
- Firebase errors: confirm `google-services.json` and plugin setup or remove Firebase dependencies.

## Contributing

Contributions are welcome! Please:

1) Fork the repo and create a feature branch
2) Follow Kotlin/Android best practices and project code style
3) Keep commits focused and descriptive
4) Open a Pull Request with a clear description and screenshots when relevant

## Privacy & Terms

See `PRIVACY_POLICY.txt` and `TERMS_OF_SERVICE.txt` in the project root.

## License

This project’s licensing terms will be defined in `LICENSE`. Until then, all rights reserved.

