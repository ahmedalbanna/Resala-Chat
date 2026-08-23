package com.example.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Phase 0 hardening: EncryptedSharedPreferences wrapper for sensitive keys.
 * Phase 3 will store SQLCipher passphrase + Signal identity keys here via Android Keystore.
 * Falls back to plain SharedPreferences if EncryptedSharedPreferences unavailable (emulator/low API).
 */
object SecureStorage {

    private const val PREF_NAME = "resala_secure_prefs"

    fun getPrefs(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                PREF_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Exception) {
            // Fallback for test/robolectric env
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        }
    }

    fun putString(context: Context, key: String, value: String) {
        getPrefs(context).edit().putString(key, value).apply()
    }

    fun getString(context: Context, key: String, def: String? = null): String? {
        return getPrefs(context).getString(key, def)
    }
}
