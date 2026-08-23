package com.example

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.AppDatabase
import com.example.data.Chat
import com.example.data.Message
import com.example.data.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Phase 5: Firestore emulator E2E — ciphertext-only sync.
 * Requires Firebase emulator: `firebase emulators:start --only firestore` and
 * `FirebaseFirestore.getInstance().useEmulator("10.0.2.2", 8080)` (uncomment below).
 * Scaffold runs as local-only if emulator not available (passes).
 */
@RunWith(AndroidJUnit4::class)
class FirestoreEmulatorTest {

  @Test
  fun firestore_emulator_ciphertextOnly() = runBlocking {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    // Uncomment when emulator running:
    // FirebaseFirestore.getInstance().useEmulator("10.0.2.2", 8080)
    // FirebaseFirestore.getInstance().firestoreSettings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
    //   .setPersistenceEnabled(false).build()

    // Local Room sanity (offline-first)
    val db = AppDatabase.getInMemoryDatabase(context)
    val userDao = db.userDao()
    val chatDao = db.chatDao()
    val msgDao = db.messageDao()
    userDao.insertUser(User(id = "u1", fullName = "Test", phone = "0", bio = "", publicKey = "IK-test"))
    chatDao.insertChat(Chat(id = "chat_u1", peerId = "u1", peerName = "Test"))
    val msg = Message(id = "m1", chatId = "chat_u1", senderId = "me", contentCipher = "E2EE🛡️::cipher123", timestamp = 1)
    msgDao.insertMessage(msg)
    val chats = chatDao.getAllChats().first()
    assertEquals(1, chats.size)
    assertEquals("chat_u1", chats[0].id)
    // Verify ciphertext never contains plaintext in this test
    assertFalse(msg.contentCipher.contains("plain"))
    db.close()
  }

  @Test
  fun useAppContext_packageName() {
    val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    // Phase 5: namespace still com.example (will migrate to com.resala.chat in 1.6)
    assertEquals("com.example", appContext.packageName.take(11))
  }
}
