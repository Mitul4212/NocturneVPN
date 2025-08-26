# Social Authentication Setup Guide

This guide will help you configure Google and Facebook authentication for your NocturneVPN app.

## Prerequisites

1. Firebase project with Authentication enabled
2. Google Cloud Console project
3. Facebook Developer account

## Firebase Setup

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Go to Authentication > Sign-in method
4. Enable Google and Facebook providers

## Google Sign-In Setup

### 1. Firebase Console Configuration
1. In Firebase Console, go to Authentication > Sign-in method
2. Enable Google provider
3. Add your support email

### 2. Google Cloud Console Configuration
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Select your project
3. Go to APIs & Services > Credentials
4. Create OAuth 2.0 Client ID for Android
5. Add your app's SHA-1 fingerprint
6. Copy the Web Client ID

### 3. Update Code
Replace `YOUR_WEB_CLIENT_ID` in `SocialAuthHelper.kt` with your actual Web Client ID:

```kotlin
.requestIdToken("YOUR_ACTUAL_WEB_CLIENT_ID_HERE")
```

## Facebook Sign-In Setup

### 1. Facebook Developer Console
1. Go to [Facebook Developers](https://developers.facebook.com/)
2. Create a new app or use existing one
3. Add Facebook Login product
4. Configure Android platform

### 2. Android Configuration
1. Add your app's package name
2. Add your app's key hashes (SHA-1 and SHA-256)
3. Copy the App ID and Client Token

### 3. Update Configuration Files

#### Update `app/src/main/res/values/facebook_app_id.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="facebook_app_id">YOUR_FACEBOOK_APP_ID</string>
    <string name="fb_login_protocol_scheme">fbYOUR_FACEBOOK_APP_ID</string>
    <string name="facebook_client_token">YOUR_FACEBOOK_CLIENT_TOKEN</string>
</resources>
```

### 4. Generate Key Hashes

Run these commands to get your key hashes:

#### For Debug:
```bash
keytool -exportcert -alias androiddebugkey -keystore ~/.android/debug.keystore | openssl sha1 -binary | openssl base64
```

#### For Release:
```bash
keytool -exportcert -alias YOUR_RELEASE_ALIAS -keystore YOUR_RELEASE_KEYSTORE | openssl sha1 -binary | openssl base64
```

## Testing

1. Build and run the app
2. Try signing in with Google and Facebook
3. Check Firebase Console to see if users are created
4. Verify user data is saved in Firestore

## Troubleshooting

### Common Issues:

1. **Google Sign-In fails**: 
   - Verify Web Client ID is correct
   - Check SHA-1 fingerprint in Google Cloud Console
   - Ensure Google Sign-In API is enabled

2. **Facebook Sign-In fails**:
   - Verify App ID and Client Token
   - Check key hashes in Facebook Developer Console
   - Ensure app is not in development mode (for production)

3. **Firebase Authentication fails**:
   - Verify Google and Facebook providers are enabled in Firebase
   - Check Firebase project configuration

## Security Notes

1. Never commit real API keys to version control
2. Use different keys for debug and release builds
3. Keep your keystore files secure
4. Regularly rotate your API keys

## Additional Resources

- [Firebase Authentication Documentation](https://firebase.google.com/docs/auth)
- [Google Sign-In for Android](https://developers.google.com/identity/sign-in/android)
- [Facebook Login for Android](https://developers.facebook.com/docs/facebook-login/android) 