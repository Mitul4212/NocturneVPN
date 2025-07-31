# Firebase Authentication Troubleshooting Guide

## Problem: "Error checking user registration" in Sign-In Page

This error occurs when the app cannot properly connect to Firebase Firestore to verify if a user is registered.

## Root Causes and Solutions

### 1. Network Connectivity Issues

**Symptoms:**
- Error message: "Network error. Please check your internet connection."
- App cannot reach Firebase services

**Solutions:**
- Check your internet connection (WiFi or mobile data)
- Try switching between WiFi and mobile data
- Check if you're behind a firewall or VPN that blocks Firebase
- Restart your router/modem

### 2. Firebase Configuration Problems

**Symptoms:**
- Error message: "Authentication service not configured. Please contact support."
- App crashes when trying to access Firebase

**Solutions:**
- Ensure `google-services.json` is properly placed in the `app/` directory
- Verify Firebase project is properly set up in Firebase Console
- Check that the package name in `google-services.json` matches your app's package name
- Ensure Firebase Authentication and Firestore are enabled in Firebase Console

### 3. Firebase Project Issues

**Symptoms:**
- Error message: "Service unavailable. Please try again later."
- Error message: "Service temporarily unavailable. Please try again later."

**Solutions:**
- Check if your Firebase project is active (not suspended)
- Verify billing is set up correctly in Firebase Console
- Check if you've exceeded Firebase quotas
- Ensure your Firebase project is in the correct region

### 4. Firestore Security Rules

**Symptoms:**
- Error message: "Access denied. Please contact support."
- Users cannot read/write to Firestore

**Solutions:**
- Check Firestore security rules in Firebase Console
- Ensure rules allow read access to the `users` collection
- Example rule for testing:
  ```
  rules_version = '2';
  service cloud.firestore {
    match /databases/{database}/documents {
      match /users/{userId} {
        allow read, write: if true; // For testing only
      }
    }
  }
  ```

### 5. App Configuration Issues

**Symptoms:**
- Error message: "Unable to verify user registration. Please try again."
- Inconsistent behavior across different devices

**Solutions:**
- Clear app data and cache
- Uninstall and reinstall the app
- Check if the app has proper permissions (Internet, Network State)
- Verify the app is using the latest version

## Testing Firebase Connection

### Using the Firebase Test Activity

1. Navigate to the Firebase Test Activity in the app
2. Click "Test Full Firebase Connection" to check all components
3. Review the results to identify specific issues

### Manual Testing Steps

1. **Test Network Connectivity:**
   - Ensure device has internet access
   - Try accessing other online services

2. **Test Firebase Auth:**
   - Check if Firebase Auth is properly initialized
   - Verify `google-services.json` configuration

3. **Test Firestore:**
   - Check if Firestore is accessible
   - Verify security rules allow read operations

## Debug Information

### Logs to Check

Look for these log tags in Android Studio Logcat:
- `SignInFragment` - Sign-in process logs
- `FirebaseUtils` - Firebase utility logs
- `FirebaseTest` - Test activity logs
- `AuthManager` - Authentication manager logs

### Common Error Messages

| Error Message | Likely Cause | Solution |
|---------------|--------------|----------|
| "Network error. Please check your internet connection." | No internet access | Check network connectivity |
| "Authentication service not configured." | Firebase config missing | Verify `google-services.json` |
| "Access denied. Please contact support." | Firestore security rules | Check Firestore rules |
| "Service unavailable." | Firebase project issues | Check Firebase Console |
| "User is not registered." | User doesn't exist | User needs to sign up first |

## Prevention Measures

### For Developers

1. **Implement Proper Error Handling:**
   - Use the improved `SignInFragment` with better error messages
   - Add network connectivity checks
   - Implement retry mechanisms

2. **Add Firebase Health Checks:**
   - Use `FirebaseUtils` for connection testing
   - Implement fallback mechanisms
   - Add proper logging for debugging

3. **Configure Firebase Properly:**
   - Set up proper security rules
   - Enable necessary Firebase services
   - Monitor Firebase usage and quotas

### For Users

1. **Ensure Stable Internet Connection:**
   - Use reliable WiFi or mobile data
   - Avoid VPNs that might block Firebase

2. **Keep App Updated:**
   - Install latest app version
   - Clear cache regularly

3. **Contact Support:**
   - If issues persist, contact app support
   - Provide error messages and device information

## Emergency Solutions

### If Firebase is Completely Down

1. **Implement Offline Mode:**
   - Cache user data locally
   - Allow basic app functionality without authentication

2. **Use Alternative Authentication:**
   - Implement local authentication as fallback
   - Sync data when Firebase is back online

3. **User Communication:**
   - Show clear error messages
   - Provide estimated resolution time
   - Offer alternative contact methods

## Support Contact

If you continue to experience issues:

1. **For Users:**
   - Contact app support through the app
   - Provide device model, Android version, and error messages
   - Include screenshots if possible

2. **For Developers:**
   - Check Firebase Console for project status
   - Review Firebase documentation
   - Contact Firebase support if needed

---

**Last Updated:** December 2024
**Version:** 1.0 