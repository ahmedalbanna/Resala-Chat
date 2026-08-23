package com.example.data.remote

import android.content.Context
import com.example.util.SecureStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

/**
 * Phase 2: Firebase Auth wrapper — phone OTP + anonymous upgrade.
 * Falls back to local deviceId if Firebase not configured (no google-services.json).
 * Stores uid in EncryptedSharedPreferences for offline use.
 */
@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val KEY_UID = "auth_uid"
        private const val KEY_PHONE = "auth_phone"
    }

    /** Returns current uid: Firebase uid if signed in, else cached local id. */
    fun getUid(): String {
        // Try Firebase if available
        try {
            val firebaseAuth = com.google.firebase.auth.FirebaseAuth.getInstance()
            firebaseAuth.currentUser?.uid?.let { uid ->
                SecureStorage.putString(context, KEY_UID, uid)
                return uid
            }
        } catch (_: Exception) {
            // Firebase not configured (no google-services.json) — use local fallback
        }
        // Fallback: cached or new device-local id
        val cached = SecureStorage.getString(context, KEY_UID)
        if (cached != null) return cached
        val newId = "local_${UUID.randomUUID().toString().take(8)}"
        SecureStorage.putString(context, KEY_UID, newId)
        return newId
    }

    fun getPhone(): String? = SecureStorage.getString(context, KEY_PHONE)

    /** Anonymous sign-in — used on first launch if no phone yet. */
    suspend fun signInAnonymously(): Result<String> {
        return try {
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            val result = auth.signInAnonymously().await()
            val uid = result.user?.uid ?: getUid()
            SecureStorage.putString(context, KEY_UID, uid)
            Result.success(uid)
        } catch (e: Exception) {
            // Local fallback
            Result.success(getUid())
        }
    }

    /**
     * Phone OTP stub — Phase 2 will wire Firebase PhoneAuthProvider.
     * For now stores phone locally and ensures anonymous uid exists.
     */
    suspend fun signInWithPhone(phone: String, otp: String? = null): Result<String> {
        SecureStorage.putString(context, KEY_PHONE, phone)
        // TODO Phase 2b: Firebase PhoneAuthProvider.verifyPhoneNumber + credential signIn
        // For scaffold, just ensure anonymous auth and return local uid
        return signInAnonymously()
    }

    fun isFirebaseConfigured(): Boolean {
        return try {
            com.google.firebase.FirebaseApp.getInstance() != null &&
                com.google.firebase.auth.FirebaseAuth.getInstance().app.options.projectId.isNotBlank()
        } catch (_: Exception) {
            false
        }
    }
}
