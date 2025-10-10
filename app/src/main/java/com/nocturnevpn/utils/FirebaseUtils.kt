package com.nocturnevpn.utils

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FirebaseUtils {
    
    private const val TAG = "FirebaseUtils"
    
    /**
     * Check if Firebase is properly configured and accessible
     */
    suspend fun isFirebaseAccessible(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val db = FirebaseFirestore.getInstance()
            // Try a simple operation to test connectivity
            db.collection("_test_connection").limit(1).get().await()
            Log.d(TAG, "Firebase is accessible")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Firebase is not accessible: ${e.message}")
            false
        }
    }
    
    /**
     * Check if user exists in Firestore with better error handling
     */
    suspend fun checkUserExists(email: String): Result<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            val db = FirebaseFirestore.getInstance()
            Log.d(TAG, "Checking if user exists: $email")
            val documents = db.collection("users").whereEqualTo("email", email).get().await()
            val exists = !documents.isEmpty
            Log.d(TAG, "User exists check result: $exists")
            Result.success(exists)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking user existence: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get detailed error message for Firebase operations
     */
    fun getErrorMessage(exception: Throwable): String {
        return when {
            exception.message?.contains("network", ignoreCase = true) == true -> 
                "Network error. Please check your internet connection."
            exception.message?.contains("permission", ignoreCase = true) == true -> 
                "Access denied. Please contact support."
            exception.message?.contains("quota", ignoreCase = true) == true -> 
                "Service temporarily unavailable. Please try again later."
            exception.message?.contains("unavailable", ignoreCase = true) == true -> 
                "Service unavailable. Please try again later."
            exception.message?.contains("not found", ignoreCase = true) == true -> 
                "Service not found. Please check your configuration."
            exception.message?.contains("timeout", ignoreCase = true) == true -> 
                "Request timeout. Please try again."
            exception.message?.contains("cancelled", ignoreCase = true) == true -> 
                "Request was cancelled."
            else -> "Unable to complete operation. Please try again."
        }
    }
    
    /**
     * Check if Firebase Auth is properly configured
     */
    fun isFirebaseAuthConfigured(): Boolean {
        return try {
            val auth = FirebaseAuth.getInstance()
            auth.app != null
        } catch (e: Exception) {
            Log.e(TAG, "Firebase Auth not configured: ${e.message}")
            false
        }
    }
    
    /**
     * Check if Firestore is properly configured
     */
    fun isFirestoreConfigured(): Boolean {
        return try {
            val db = FirebaseFirestore.getInstance()
            db.app != null
        } catch (e: Exception) {
            Log.e(TAG, "Firestore not configured: ${e.message}")
            false
        }
    }
} 