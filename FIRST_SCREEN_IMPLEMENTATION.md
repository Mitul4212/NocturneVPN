# First Screen Implementation Guide

## Overview
The app now shows a first screen UI when users install and open the app for the first time. This screen allows users to choose between "Continue as guest" or "Sign in" options.

## How It Works

### 1. First Time Detection
- Uses `AuthFlowManager` to track if this is the first time the app is opened
- Stores the state in SharedPreferences with key `is_first_time_login`

### 2. UI Flow
- **First time users**: See the first screen with two options
- **Returning users**: See the normal splash screen and proceed to appropriate activity

### 3. User Choices

#### Continue as Guest
- Marks that user has seen the login page
- Navigates directly to `HomeActivity`
- User can use the app without signing in

#### Sign In
- Marks that user has seen the login page  
- Navigates to `AppAuthActivity` for authentication
- User can sign in with email/password or social auth

### 4. Terms of Service
- Clickable "Terms of Service" link in the first screen
- Opens the terms of service webpage in browser

## Files Modified

### 1. `activity_splash.xml`
- Added `android:id="@+id/tv_terms_of_service"` to the Terms of Service TextView
- Two main layouts:
  - `splash_screen`: Normal splash screen (visible for returning users)
  - `firstScreen`: First time user screen (visible for new users)

### 2. `SplashActivity.kt`
- Added logic to check if it's first time login
- Shows appropriate screen based on first-time status
- Handles button clicks for guest profile and sign in
- Implements Terms of Service click functionality

### 3. `AuthFlowManager.kt`
- Added `clearAllPreferences()` method for testing
- Manages first-time login state

## Testing

### Test Activity
Created `TestFirstTimeActivity` to help test the functionality:

1. **Reset First Time Login**: Resets the first-time flag to test first screen again
2. **Clear All Preferences**: Clears all auth flow preferences
3. **Go to Splash Activity**: Tests the actual flow

### How to Test
1. Install the app fresh → Should show first screen
2. Select "Continue as guest" → Should go to HomeActivity
3. Select "Sign in" → Should go to AppAuthActivity
4. Close and reopen app → Should show splash screen (not first screen)
5. Use TestFirstTimeActivity to reset and test again

## Key Methods

### AuthFlowManager
- `isFirstTimeLogin()`: Check if first time opening app
- `markLoginSeen()`: Mark that user has seen login options
- `resetFirstTimeLogin()`: Reset for testing
- `clearAllPreferences()`: Clear all preferences

### SplashActivity
- `showFirstScreen()`: Display first time user UI
- `showSplashScreen()`: Display normal splash screen
- `handleGuestProfileSelection()`: Handle guest profile choice
- `handleSignInSelection()`: Handle sign in choice
- `openTermsOfService()`: Open terms of service webpage

## SharedPreferences Keys
- `is_first_time_login`: Boolean flag for first time detection
- `has_seen_login`: Boolean flag for login page seen status

## Notes
- The first screen is only shown once per app installation
- After user makes a choice, they won't see the first screen again
- Terms of Service link opens in external browser
- Guest users can use the app without authentication
- All state is managed through SharedPreferences for persistence

