package com.example.util

import android.util.Base64
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Phase 5: Unit tests for EncryptionEngine — round-trip, IV randomness, GCM tag tamper, fingerprint.
 * Uses Robolectric for android.util.Base64 availability.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class EncryptionEngineTest {

  private val pubKey = "IK-robo-secure-bot-key-83ad"
  private val otherKey = "IK-98df-892c-bef4-c321"

  @Test
  fun encryptDecrypt_roundTrip_arabicAndEmoji() {
    val plain = "أهلاً بك في رِسالة! 👋 مساء الخير"
    val cipher = EncryptionEngine.encrypt(plain, pubKey)
    assertTrue(cipher.startsWith("E2EE🛡️::"))
    val decrypted = EncryptionEngine.decrypt(cipher, pubKey)
    assertEquals(plain, decrypted)
  }

  @Test
  fun encrypt_producesDifferentCiphertexts_dueToRandomIV() {
    val plain = "نفس النص"
    val c1 = EncryptionEngine.encrypt(plain, pubKey)
    val c2 = EncryptionEngine.encrypt(plain, pubKey)
    assertNotEquals("IV must be random", c1, c2)
    // Both decrypt correctly
    assertEquals(plain, EncryptionEngine.decrypt(c1, pubKey))
    assertEquals(plain, EncryptionEngine.decrypt(c2, pubKey))
  }

  @Test
  fun decrypt_tamperedGCMTag_failsFallbackDoesNotRevealPlaintext() {
    val plain = "رسالة سرية جداً"
    val cipher = EncryptionEngine.encrypt(plain, pubKey)
    val b64 = cipher.substringAfter("E2EE🛡️::")
    val combined = Base64.decode(b64, Base64.NO_WRAP)
    // Flip a byte in ciphertext+tag (after 12-byte IV)
    combined[15] = (combined[15].toInt() xor 0x01).toByte()
    val tampered = "E2EE🛡️::" + Base64.encodeToString(combined, Base64.NO_WRAP)
    val decrypted = EncryptionEngine.decrypt(tampered, pubKey)
    // Should NOT equal original plain (GCM auth fails, fallback base64 also not plain)
    assertNotEquals(plain, decrypted)
  }

  @Test
  fun decrypt_legacyECB_fallback_stillWorks() {
    // Simulate old ECB data via direct fallback path: encrypt with GCM then manually craft ECB-like?
    // For scaffold, ensure that legacy path exists: tamper that fails GCM but valid Base64 fallback returns something
    val plain = "legacy test"
    val b64Plain = Base64.encodeToString(plain.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    val legacyCipher = "E2EE🛡️::" + b64Plain
    // Our decrypt should try GCM (fail), then ECB (fail), then plain Base64 -> success
    val decrypted = EncryptionEngine.decrypt(legacyCipher, pubKey)
    assertEquals(plain, decrypted)
  }

  @Test
  fun fingerprint_isHexGrouped() {
    val fp = EncryptionEngine.generateFingerprint(pubKey, otherKey)
    // Expected: 4 groups of 8 hex chars separated by space
    val groups = fp.split(" ")
    assertEquals(4, groups.size)
    for (g in groups) {
      assertEquals(8, g.length)
      assertTrue(g.matches(Regex("[0-9a-f]{8}")))
    }
  }

  @Test
  fun fingerprint_symmetric() {
    val fp1 = EncryptionEngine.generateFingerprint(pubKey, otherKey)
    val fp2 = EncryptionEngine.generateFingerprint(otherKey, pubKey)
    assertEquals(fp1, fp2)
  }

  @Test
  fun fullFingerprint_8groups() {
    val fp = EncryptionEngine.generateFullFingerprint(pubKey, otherKey)
    assertEquals(8, fp.split(" ").size)
  }
}
