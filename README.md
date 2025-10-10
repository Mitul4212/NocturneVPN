# NocturneVPN

**NocturneVPN** is a modern Android VPN client built with Kotlin and Jetpack.  
It offers fast, reliable connections, intuitive server selection, and a polished UI with dark/light themes.

This app uses a dedicated VPN module built on **Android VpnService** and integrates the **ICS OpenVPN** (`de.blinkt.openvpn`) library for core VPN functionality.

---

## ✨ Features

- Fast and stable VPN connection using open standards (OpenVPN protocol)
- Server selection by country and latency
- 3D globe visualization (CesiumJS) of the connected IP location
- Clean, responsive UI with Material 3 and dynamic theming
- Session status, bandwidth, and connection duration indicators
- Optional analytics and crash reporting

---

## 🏗️ Architecture & Tech Stack

| Layer | Tech |
|-------|------|
| **Language** | Kotlin |
| **UI** | Jetpack (Fragments/Views), Material 3, Navigation |
| **Background** | WorkManager |
| **VPN Core** | Android VpnService (via `vpnLib`, ICS OpenVPN native integration) |
| **Networking** | Retrofit2, OkHttp (with logging) |
| **Images/Animations** | Glide, Lottie |
| **Async/Data** | Kotlin Coroutines, Flow, LiveData |
| **Build** | Gradle Kotlin DSL (AGP 8.5.x), Kotlin 2.0.x |
| **Services** | Firebase (Auth, Analytics, Firestore), Google Sign-In, Facebook Login |
| **Monetization** | AdMob, Google Play Billing, UMP (Consent) |

---

## 🧩 Modules

- `app`: Main Android app (UI, navigation, user interaction)
- `vpnLib`: Core VPN module (JNI + OpenVPN integration via NDK)

---

## 📁 Project Structure

```
NocturneVPN/
  app/
    src/main/...  # UI, fragments, view models, resources
  vpnLib/
    src/main/...  # VPN service, networking, helpers
```

---

## ⚙️ Requirements

- Android Studio **Iguana or newer**
- **JDK 17**
- **Android SDK Platform 35**
- **Min SDK 24** (VPN library supports down to 16)
- **NDK** (ndkVersion = 29.0.13113456)

---

## 🚀 Getting Started

### 1. Clone the Repository
```bash
git clone https://github.com/Mitul4212/NocturneVPN.git
cd NocturneVPN
```

### 2. Open in Android Studio

Open the root project and let Gradle sync complete.

### 3. Configure Optional Services

- Firebase: Add `google-services.json` if using Auth/Analytics/Firestore
- Social Auth: Configure Google/Facebook sign-in
- Ads: Set up AdMob + Consent SDK (UMP)

### 4. Build & Run
```bash
./gradlew assembleDebug
./gradlew installDebug
```

On Windows PowerShell:
```bash
./gradlew.bat assembleDebug
```

---

## 🔑 App Signing (Release)

- Create or use an existing keystore
- Configure signing in `gradle.properties` or Android Studio
- Build:
```bash
./gradlew assembleRelease
```

Artifacts: `app/build/outputs/apk/release/`

---

## ⚙️ Configuration

- Permissions: Declared in `AndroidManifest.xml`
- App ID: `com.nocturnevpn`
- BuildConfig Fields:
  - `VPN_GATE_API` → `http://www.vpngate.net/api/iphone/`
  - `DISABLE_THIRD_PARTY_SDKS` → `true` for debugging
- Packaging: `jniLibs.useLegacyPackaging = true`
- View/Data Binding: Enabled
- MultiDex: Enabled

---

## 🧰 Notable Dependencies

- Core: `androidx.core-ktx`, `appcompat`, `material`, `constraintlayout`
- Navigation: `androidx.navigation:navigation-fragment-ktx`
- Background: `work-runtime-ktx`
- Networking: `retrofit2`, `okhttp`, `gson`
- Firebase: `firebase-auth`, `firebase-analytics`, `firebase-firestore-ktx`
- Auth Providers: `play-services-auth`, `facebook-login`
- Monetization: `billing-ktx`, `play-services-ads`, `user-messaging-platform`
- UI Helpers: `SmoothBottomBar`, `flagkit-android`, `facebook shimmer`
- Web: `webkit`, `play-services-cronet`

---

## 🧠 Troubleshooting

- Gradle Sync Issues: Use included Gradle wrapper and correct AGP version
- VPN Fails to Connect: Verify device supports `VpnService`
- Firebase Errors: Check `google-services.json` setup
- Native Library (NDK) Build Issues: Ensure correct NDK version and ABI filters

---

## 📸 Screenshots / Demo

Add screenshots or GIFs here showing the server list, connection UI, and globe visualization.

---

## 👥 Contributing

Contributions are welcome!

- Fork the repo
- Create a feature branch
- Follow Kotlin & Android best practices
- Submit a pull request with details and screenshots

---

## 🔒 Privacy & Terms

See `PRIVACY_POLICY.txt` and `TERMS_OF_SERVICE.txt` for details.

---

## 🧾 License

This project includes and modifies code from the
ICS OpenVPN project
by Arne Schwabe, licensed under the GNU General Public License v2 (GPLv2).

Therefore, NocturneVPN is distributed under the same license:

NocturneVPN - Android VPN Client
Copyright (C) 2025 Mitul Chovatiya

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License version 2,
as published by the Free Software Foundation.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.
If not, see <https://www.gnu.org/licenses/old-licenses/gpl-2.0.txt>.

Full license text available in the `LICENSE` file.

---

## 🧑‍💻 Author

Mitul Chovatiya  
Email: your-email@example.com  
GitHub: https://github.com/Mitul4212

---

✅ **Next Step:**  
You must also add a `LICENSE` file at your project root — this project should use **GPL-2.0** (the same as ICS-OpenVPN). I will add it now.

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

