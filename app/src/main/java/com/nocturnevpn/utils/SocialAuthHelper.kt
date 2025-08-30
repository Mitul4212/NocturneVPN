package com.nocturnevpn.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class SocialAuthHelper(private val context: Context) {
    
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val callbackManager: CallbackManager = CallbackManager.Factory.create()
    
    // Google Sign-In
    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("124157274076-efoq830erv6li653ll3jn0vmossdj9co.apps.googleusercontent.com") // Replace with your web client ID from Firebase console
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context as Activity, gso)
    }
    
    interface AuthCallback {
        fun onSuccess(userId: String, email: String, name: String)
        fun onFailure(errorMessage: String)
    }
    
    fun signInWithGoogle(activity: Activity, callback: AuthCallback) {
        Log.d("SocialAuthHelper", "signInWithGoogle called")
        
        // Check if user is already signed in
        val account = GoogleSignIn.getLastSignedInAccount(activity)
        if (account != null) {
            Log.d("SocialAuthHelper", "User already signed in: ${account.email}")
            firebaseAuthWithGoogle(account.idToken!!, callback)
            return
        }
        
        val signInIntent = googleSignInClient.signInIntent
        Log.d("SocialAuthHelper", "Starting Google Sign-In activity")
        activity.startActivityForResult(signInIntent, RC_SIGN_IN)
        
        // Store callback for later use
        currentCallback = callback
    }

    // Modern Activity Result API version
    fun signInWithGoogleModern(launcher: androidx.activity.result.ActivityResultLauncher<Intent>, callback: AuthCallback) {
        Log.d("SocialAuthHelper", "signInWithGoogleModern called")
        
        // Store callback first
        currentCallback = callback
        
        // Check if user is already signed in
        val account = GoogleSignIn.getLastSignedInAccount(context as Activity)
        if (account != null) {
            Log.d("SocialAuthHelper", "User already signed in: ${account.email}")
            firebaseAuthWithGoogle(account.idToken!!, callback)
            return
        }
        
        // Test if Google Sign-In is properly configured
        try {
            val signInIntent = googleSignInClient.signInIntent
            Log.d("SocialAuthHelper", "Starting Google Sign-In activity with modern launcher")
            launcher.launch(signInIntent)
        } catch (e: Exception) {
            Log.e("SocialAuthHelper", "Error launching Google Sign-In: ${e.message}")
            callback.onFailure("Google Sign-In configuration error: ${e.message}")
        }
    }
    
    fun signInWithFacebook(callback: AuthCallback) {
        Log.d("SocialAuthHelper", "signInWithFacebook called")
        
        try {
            // Store callback first
            currentCallback = callback
            
            // Always logout first to force fresh login (for testing)
            LoginManager.getInstance().logOut()
            Log.d("SocialAuthHelper", "Logged out from Facebook to force fresh login")
            
            // Register callback before starting login
            LoginManager.getInstance().registerCallback(callbackManager,
                object : FacebookCallback<LoginResult> {
                    override fun onSuccess(result: LoginResult) {
                        Log.d("SocialAuthHelper", "Facebook login successful with token: ${result.accessToken.token.take(10)}...")
                        handleFacebookAccessToken(result.accessToken, callback)
                    }
                    
                    override fun onCancel() {
                        Log.d("SocialAuthHelper", "Facebook sign-in was cancelled")
                        callback.onFailure("Facebook sign-in was cancelled")
                    }
                    
                    override fun onError(error: FacebookException) {
                        Log.e("SocialAuthHelper", "Facebook sign-in failed: ${error.message}")
                        callback.onFailure("Facebook sign-in failed: ${error.message}")
                    }
                })
            
            // Start login after registering callback
            // Using only basic permissions that don't require verification
            LoginManager.getInstance().logInWithReadPermissions(
                context as Activity,
                listOf("public_profile") // Remove email temporarily
            )
            
        } catch (e: Exception) {
            Log.e("SocialAuthHelper", "Error starting Facebook login: ${e.message}")
            e.printStackTrace()
            callback.onFailure("Facebook configuration error: ${e.message}")
        }
    }
    
    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d("SocialAuthHelper", "handleActivityResult called, requestCode=$requestCode, resultCode=$resultCode")
        
        // Handle Facebook login result first
        val facebookHandled = callbackManager.onActivityResult(requestCode, resultCode, data)
        Log.d("SocialAuthHelper", "Facebook callback manager handled result: $facebookHandled")
        
        // If Facebook login was handled and we have a current callback, check for access token
        if (facebookHandled && currentCallback != null) {
            // Add a small delay to allow Facebook SDK to process the result
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                val accessToken = AccessToken.getCurrentAccessToken()
                if (accessToken != null && !accessToken.isExpired) {
                    Log.d("SocialAuthHelper", "Found valid access token after Facebook login: ${accessToken.token.take(10)}...")
                    handleFacebookAccessToken(accessToken, currentCallback!!)
                } else {
                    Log.d("SocialAuthHelper", "No valid access token found after Facebook login")
                }
            }, 1000) // 1 second delay
        }
        
        // Handle Google Sign-In result
        if (requestCode == RC_SIGN_IN) {
            Log.d("SocialAuthHelper", "Processing Google Sign-In result")
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                Log.d("SocialAuthHelper", "Google account selected: ${account.email}")
                
                if (currentCallback != null) {
                    firebaseAuthWithGoogle(account.idToken!!, currentCallback!!)
                } else {
                    Log.e("SocialAuthHelper", "No callback found for Google Sign-In")
                }
            } catch (e: ApiException) {
                Log.e("SocialAuthHelper", "Google sign-in failed: ${e.message}")
                currentCallback?.onFailure("Google sign-in failed: ${e.message}")
                // Clear the callback after failed authentication
                currentCallback = null
            }
        }
    }
    
    private fun firebaseAuthWithGoogle(idToken: String, callback: AuthCallback) {
        Log.d("SocialAuthHelper", "firebaseAuthWithGoogle called with idToken length: ${idToken.length}")
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Log.d("SocialAuthHelper", "Firebase sign-in successful: ${user?.email}")
                    user?.let {
                        saveUserDataToFirestore(it.uid, it.email ?: "", it.displayName ?: "", true, "google")
                        uploadDeviceInfoToFirestore(it.uid)
                        
                        // Save authentication state
                        val authManager = AuthManager.getInstance(context)
                        authManager.saveAuthState(it.uid, it.email ?: "", it.displayName ?: "", "google")
                        
                        Log.d("SocialAuthHelper", "Google auth state saved, calling callback")
                        callback.onSuccess(it.uid, it.email ?: "", it.displayName ?: "")
                        // Clear the callback after successful authentication
                        currentCallback = null
                    }
                } else {
                    Log.e("SocialAuthHelper", "Firebase sign-in failed: ${task.exception?.message}")
                    callback.onFailure("Authentication failed: ${task.exception?.message}")
                    // Clear the callback after failed authentication
                    currentCallback = null
                }
            }
    }
    
    private fun handleFacebookAccessToken(token: AccessToken, callback: AuthCallback) {
        Log.d("SocialAuthHelper", "handleFacebookAccessToken called with token: ${token.token.take(10)}...")
        Log.d("SocialAuthHelper", "Token permissions: ${token.permissions}")
        Log.d("SocialAuthHelper", "Token is expired: ${token.isExpired}")
        
        val credential = FacebookAuthProvider.getCredential(token.token)
        
        Log.d("SocialAuthHelper", "Starting Firebase authentication with Facebook credential")
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Log.d("SocialAuthHelper", "Firebase Facebook sign-in successful: ${user?.email}")
                    user?.let {
                        // Generate a fallback email if none is provided
                        val userEmail = it.email ?: "user_${it.uid}@facebook.com"
                        val userName = it.displayName ?: "Facebook User"
                        
                        Log.d("SocialAuthHelper", "Saving user data to Firestore with email: $userEmail")
                        saveUserDataToFirestore(it.uid, userEmail, userName, true, "facebook")
                        uploadDeviceInfoToFirestore(it.uid)
                        
                        // Save authentication state
                        val authManager = AuthManager.getInstance(context)
                        authManager.saveAuthState(it.uid, userEmail, userName, "facebook")
                        
                        Log.d("SocialAuthHelper", "Facebook auth state saved, calling callback")
                        callback.onSuccess(it.uid, userEmail, userName)
                        // Clear the callback after successful authentication
                        currentCallback = null
                    }
                } else {
                    Log.e("SocialAuthHelper", "Firebase Facebook sign-in failed: ${task.exception?.message}")
                    task.exception?.printStackTrace()
                    callback.onFailure("Authentication failed: ${task.exception?.message}")
                    // Clear the callback after failed authentication
                    currentCallback = null
                }
            }
    }
    
    private fun saveUserDataToFirestore(userId: String, email: String, name: String, agreedToTerms: Boolean, authProvider: String) {
        val user = hashMapOf(
            "name" to name,
            "email" to email,
            "agreedToTerms" to agreedToTerms,
            "isVerified" to true, // Social auth users are pre-verified
            "authProvider" to authProvider,
            "createdAt" to System.currentTimeMillis()
        )
        db.collection("users").document(userId).set(user)
    }
    
    private fun uploadDeviceInfoToFirestore(userId: String) {
        val context = context
        val userFriendlyId = context.getUserFriendlyDeviceId()
        val androidId = android.provider.Settings.Secure.getString(
            context.contentResolver, 
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: "UNKNOWN"
        val model = android.os.Build.MODEL ?: "MODEL"
        val manufacturer = android.os.Build.MANUFACTURER ?: "MANUFACTURER"
        val osVersion = android.os.Build.VERSION.RELEASE ?: "RELEASE"
        val now = System.currentTimeMillis()
        val readableNow = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(now))
        
        val deviceInfo = hashMapOf(
            "device_id" to userFriendlyId,
            "android_id" to androidId,
            "model" to model,
            "manufacturer" to manufacturer,
            "os_version" to osVersion,
            "first_login" to now,
            "first_login_readable" to readableNow,
            "last_login" to now,
            "last_login_readable" to readableNow
        )
        
        val userDoc = db.collection("users").document(userId)
        val devicesCol = userDoc.collection("devices")
        
        devicesCol.whereEqualTo("device_id", userFriendlyId).get().addOnSuccessListener { querySnapshot ->
            if (querySnapshot.isEmpty) {
                devicesCol.add(deviceInfo)
            } else {
                for (doc in querySnapshot.documents) {
                    doc.reference.update(
                        mapOf(
                            "last_login" to now,
                            "last_login_readable" to readableNow
                        )
                    )
                }
            }
        }
    }
    
    companion object {
        private const val RC_SIGN_IN = 9001
        private var currentCallback: AuthCallback? = null
    }
    
    fun signOutFromGoogle() {
        googleSignInClient.signOut()
        Log.d("SocialAuthHelper", "Signed out from Google")
    }
} 