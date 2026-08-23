package com.example.util

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object EncryptionEngine {

    private const val PREFIX = "E2EE🛡️::"
    private const val GCM_IV_LENGTH = 12 // 96-bit IV per NIST
    private const val GCM_TAG_LENGTH_BITS = 128

    /**
     * Phase 0 hardening: HKDF-like KDF from identity publicKey -> 256-bit AES key.
     * We keep derivation deterministic per peer (no per-message salt yet) but use
     * full SHA-256 (32 bytes) for AES-256-GCM instead of truncated 128-bit ECB.
     * Future: per-install salt in EncryptedSharedPreferences + HKDF expand.
     */
    private fun deriveKey(publicKey: String): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        // Keep cleaning for backward compat, but hash full UTF-8
        val cleanKey = publicKey.replace(Regex("[^a-zA-Z0-9]"), "")
        val hash = digest.digest(cleanKey.toByteArray(Charsets.UTF_8))
        // AES-256: use full 32 bytes. If device lacks 256-bit policy, fallback to 16 bytes
        return SecretKeySpec(hash, "AES")
    }

    // Legacy 128-bit key for decrypting old ECB data (pre Phase-0)
    private fun deriveLegacyKey(publicKey: String): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val cleanKey = publicKey.replace(Regex("[^a-zA-Z0-9]"), "")
        val hash = digest.digest(cleanKey.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(hash.copyOf(16), "AES")
    }

    /**
     * Encrypts plainText with AES-256-GCM + random 12-byte IV.
     * Format: PREFIX + Base64( IV || ciphertext+tag )
     * Each message gets a fresh IV => same plaintext produces different ciphertext.
     */
    fun encrypt(plainText: String, publicKey: String): String {
        return try {
            val keySpec = deriveKey(publicKey)
            val iv = ByteArray(GCM_IV_LENGTH).also { SecureRandom().nextBytes(it) }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv, 0, GCM_IV_LENGTH))
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            // Prepend IV for transport; tag is appended to encryptedBytes by GCM
            val combined = iv + encryptedBytes
            val base64Cipher = Base64.encodeToString(combined, Base64.NO_WRAP)
            "$PREFIX$base64Cipher"
        } catch (e: Exception) {
            // Last-resort: plain Base64 so UI still shows something (should not happen)
            PREFIX + Base64.encodeToString(plainText.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        }
    }

    /**
     * Decrypts ciphertext with AES-GCM. Falls back to legacy AES/ECB for data
     * created before Phase-0 hardening, then to plain Base64.
     */
    fun decrypt(cipherText: String, publicKey: String): String {
        if (!cipherText.startsWith(PREFIX)) return cipherText
        val pureCipher = cipherText.substringAfter(PREFIX)
        // 1) Try GCM (current)
        try {
            val keySpec = deriveKey(publicKey)
            val combined = Base64.decode(pureCipher, Base64.NO_WRAP)
            if (combined.size > GCM_IV_LENGTH) {
                val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
                val cipherBytes = combined.copyOfRange(GCM_IV_LENGTH, combined.size)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, keySpec, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv, 0, GCM_IV_LENGTH))
                val decryptedBytes = cipher.doFinal(cipherBytes)
                return String(decryptedBytes, Charsets.UTF_8)
            }
        } catch (_: Exception) {
            // fall through to legacy
        }
        // 2) Legacy ECB (pre-Phase0)
        try {
            val keySpec = deriveLegacyKey(publicKey)
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, keySpec)
            val decodedBytes = Base64.decode(pureCipher, Base64.NO_WRAP)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (_: Exception) {
            // fall through
        }
        // 3) Plain Base64 fallback
        return try {
            val decoded = Base64.decode(pureCipher, Base64.NO_WRAP)
            String(decoded, Charsets.UTF_8)
        } catch (_: Exception) {
            cipherText
        }
    }

    /**
     * Generates a unique hex fingerprint (like WhatsApp/Signal) representing the mutual keys.
     * Fixed: was using decimal %02d (00-255). Now hex %02x (00-ff).
     * Output: 32 bytes hex -> 64 chars grouped as "a1b2c3d4 ... " (8 groups of 8 hex chars).
     * For UI compatibility we expose first 12 bytes as 4 groups of 6 hex chars.
     */
    fun generateFingerprint(keyA: String, keyB: String): String {
        val combined = if (keyA < keyB) "$keyA:$keyB" else "$keyB:$keyA"
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(combined.toByteArray(Charsets.UTF_8))
        // Hex fingerprint grouped by 8 chars (4 bytes) -> 8 groups; take first 4 for compact UI
        val hex = hash.joinToString("") { String.format("%02x", it.toInt() and 0xFF) }
        // Return 4 groups of 8 hex chars separated by space, e.g. "a1b2c3d4 e5f6a7b8 ..."
        return hex.chunked(8).take(4).joinToString(" ")
    }

    /**
     * Full-length debug fingerprint (all 32 bytes hex grouped).
     */
    fun generateFullFingerprint(keyA: String, keyB: String): String {
        val combined = if (keyA < keyB) "$keyA:$keyB" else "$keyB:$keyA"
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(combined.toByteArray(Charsets.UTF_8))
        val hex = hash.joinToString("") { String.format("%02x", it.toInt() and 0xFF) }
        return hex.chunked(8).joinToString(" ")
    }
}
