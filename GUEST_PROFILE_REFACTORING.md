# Guest Profile Refactoring

## Overview
The guest profile functionality has been separated from the main profile fragment to reduce code complexity and improve maintainability.

## Changes Made

### 1. New Files Created

#### `fragment_guest_profile.xml`
- **Location**: `app/src/main/res/layout/fragment_guest_profile.xml`
- **Purpose**: Contains the UI layout for the guest profile section
- **Features**:
  - Sign in button
  - User ID display with copy functionality
  - Upgrade button
  - Restore option

#### `GuestProfileFragment.kt`
- **Location**: `app/src/main/java/com/example/nocturnevpn/view/fragment/GuestProfileFragment.kt`
- **Purpose**: Handles the backend logic for guest profile functionality
- **Features**:
  - Sign in navigation
  - Upgrade navigation
  - Copy user ID to clipboard
  - Device unique ID management

### 2. Modified Files

#### `fragment_profile.xml`
- **Change**: Replaced the embedded guest profile layout with an `<include>` tag
- **Benefit**: Cleaner main layout file, easier to maintain

#### `profileFragment.kt`
- **Changes**:
  - Removed guest profile specific click listeners
  - Added `setupGuestProfile()` method to handle guest profile visibility
  - Added `setupGuestProfileClickListeners()` method for guest profile interactions
  - Added `updateGuestDeviceUniqueId()` method
  - Added `copyUserIdToClipboard()` method
  - Updated `onResume()` to refresh guest profile visibility

#### `SharedPreference.java`
- **Changes**:
  - Added `USER_SIGNED_IN` constant
  - Added `isUserSignedIn()` method
  - Added `setUserSignedIn(Boolean signedIn)` method

## Usage

### In Main Profile Fragment
The main profile fragment now uses the guest profile as an included layout:

```xml
<include
    android:id="@+id/guestProfile"
    layout="@layout/fragment_guest_profile"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:visibility="gone"
    app:layout_constraintBottom_toBottomOf="parent"
    app:layout_constraintEnd_toEndOf="parent"
    app:layout_constraintStart_toStartOf="parent"
    app:layout_constraintTop_toTopOf="parent" />
```

### Guest Profile Visibility Control
The guest profile visibility is controlled based on user sign-in status:

```kotlin
private fun setupGuestProfile() {
    val isUserSignedIn = sharedPreference?.isUserSignedIn() ?: false
    
    if (isUserSignedIn) {
        binding.guestProfile.visibility = View.GONE
        binding.profilePageAfterSignin.visibility = View.VISIBLE
    } else {
        binding.guestProfile.visibility = View.VISIBLE
        binding.profilePageAfterSignin.visibility = View.GONE
        setupGuestProfileClickListeners()
    }
}
```

## Benefits

1. **Code Separation**: Guest profile logic is now separate from main profile logic
2. **Maintainability**: Easier to maintain and modify guest profile functionality
3. **Reusability**: Guest profile can be reused in other parts of the app if needed
4. **Cleaner Code**: Main profile fragment is now more focused and less cluttered
5. **Better Organization**: UI and backend code are properly separated

## Future Enhancements

1. **Fragment Communication**: Can implement proper fragment communication using interfaces
2. **State Management**: Can add better state management for user authentication
3. **Animation**: Can add smooth transitions between guest and signed-in states
4. **Testing**: Easier to write unit tests for guest profile functionality

## Notes

- The guest profile fragment is currently used as an included layout rather than a separate fragment for simplicity
- All guest profile interactions are handled through the main profile fragment
- The SharedPreference class now includes user authentication state management
- The refactoring maintains all existing functionality while improving code organization
- **Touch Event Solution**: Due to FragmentContainerView touch event interception, individual button click listeners are used instead of root view touch listeners
- **Working Approach**: The `setupIndividualButtonListeners()` method successfully handles all button interactions

## Final Implementation

The guest profile functionality is now properly separated and working:

1. **Layout Separation**: Guest profile UI is in `fragment_guest_profile.xml`
2. **Logic Separation**: Guest profile logic is handled in `setupGuestProfile()` and `setupIndividualButtonListeners()`
3. **Working Buttons**: All buttons (Sign In, Upgrade, Copy) are functional
4. **Clean Code**: Main profile fragment is much cleaner and more maintainable 