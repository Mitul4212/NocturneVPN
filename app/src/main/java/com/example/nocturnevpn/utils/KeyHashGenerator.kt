package com.example.nocturnevpn.utils

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.util.Base64
import android.util.Log
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * Utility class to generate key hashes for Facebook authentication
 * This is useful for debugging and getting the correct key hashes
 */
object KeyHashGenerator {
    
    private const val TAG = "KeyHashGenerator"
    
    /**
     * Generate and log the key hash for the current app
     * This should be called in your MainActivity or Application class for debugging
     */
    fun generateKeyHash(context: Context) {
        try {
            val info: PackageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES
            )
            
            info.signatures?.forEach { signature ->
                val md: MessageDigest = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                
                val keyHash = Base64.encodeToString(md.digest(), Base64.DEFAULT)
                Log.d(TAG, "KeyHash: $keyHash")
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Package name not found", e)
        } catch (e: NoSuchAlgorithmException) {
            Log.e(TAG, "No such algorithm", e)
        }
    }
    
    /**
     * Generate SHA-1 hash for the current app
     */
    fun generateSHA1Hash(context: Context): String? {
        return try {
            val info: PackageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES
            )
            
            val signatures = info.signatures
            if (signatures != null && signatures.isNotEmpty()) {
                val signature = signatures[0]
                val md: MessageDigest = MessageDigest.getInstance("SHA1")
                md.update(signature.toByteArray())
                
                val sha1Hash = Base64.encodeToString(md.digest(), Base64.DEFAULT)
                Log.d(TAG, "SHA1 Hash: $sha1Hash")
                sha1Hash
            } else {
                Log.e(TAG, "No signatures found")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating SHA1 hash", e)
            null
        }
    }
    
    /**
     * Generate SHA-256 hash for the current app
     */
    fun generateSHA256Hash(context: Context): String? {
        return try {
            val info: PackageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES
            )
            
            val signatures = info.signatures
            if (signatures != null && signatures.isNotEmpty()) {
                val signature = signatures[0]
                val md: MessageDigest = MessageDigest.getInstance("SHA256")
                md.update(signature.toByteArray())
                
                val sha256Hash = Base64.encodeToString(md.digest(), Base64.DEFAULT)
                Log.d(TAG, "SHA256 Hash: $sha256Hash")
                sha256Hash
            } else {
                Log.e(TAG, "No signatures found")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating SHA256 hash", e)
            null
        }
    }
} 