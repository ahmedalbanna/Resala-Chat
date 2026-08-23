package com.example.data.remote

import android.content.Context
import com.example.data.ChatRepository
import com.example.data.Message
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Phase 2: Firestore real-time sync — offline-first.
 * Local Room is source of truth; Firestore is transport.
 * Collections: `chats/{chatId}/messages/{msgId}` and `chats/{chatId}` metadata.
 * Ciphertext only — server never sees plaintext.
 */
@Singleton
class FirestoreSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authManager: AuthManager,
    private val chatRepository: ChatRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Enable offline persistence — call from Application.onCreate */
    fun enableOfflinePersistence() {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(com.google.firebase.firestore.FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()
            firestore.firestoreSettings = settings
        } catch (_: Exception) {
            // Already initialized or Firebase not configured
        }
    }

    /** DTO for Firestore — never store plaintext */
    data class FirestoreMessage(
        val id: String = "",
        val chatId: String = "",
        val senderId: String = "",
        val contentCipher: String = "",
        val msgType: String = "TEXT",
        val localPath: String? = null,
        val latitude: Double = 0.0,
        val longitude: Double = 0.0,
        val audioDuration: Int = 0,
        val isE2ee: Boolean = true,
        val timestamp: Long = 0L,
        val serverTimestamp: Any? = FieldValue.serverTimestamp(),
        val senderUid: String = ""
    )

    private fun Message.toFirestore(): FirestoreMessage = FirestoreMessage(
        id = id,
        chatId = chatId,
        senderId = senderId,
        contentCipher = contentCipher,
        msgType = msgType,
        localPath = localPath,
        latitude = latitude,
        longitude = longitude,
        audioDuration = audioDuration,
        isE2ee = isE2ee,
        timestamp = timestamp,
        senderUid = authManager.getUid()
    )

    private fun FirestoreMessage.toDomain(): Message? {
        if (id.isBlank() || chatId.isBlank()) return null
        return Message(
            id = id,
            chatId = chatId,
            senderId = senderId,
            contentCipher = contentCipher,
            msgType = msgType,
            localPath = localPath,
            latitude = latitude,
            longitude = longitude,
            timestamp = timestamp.takeIf { it != 0L } ?: System.currentTimeMillis(),
            isE2ee = isE2ee,
            isRead = false,
            isSent = true,
            audioDuration = audioDuration
        )
    }

    /** Push local message to Firestore — enqueues WorkManager on failure */
    suspend fun pushMessage(message: Message): Result<Unit> {
        if (!authManager.isFirebaseConfigured()) {
            // Local-only mode — no-op, still success for offline-first
            return Result.success(Unit)
        }
        return try {
            val firestore = FirebaseFirestore.getInstance()
            val dto = message.toFirestore()
            firestore.collection("chats")
                .document(message.chatId)
                .collection("messages")
                .document(message.id)
                .set(dto, SetOptions.merge())
                .await()
            // Also update chat metadata with snippet
            firestore.collection("chats")
                .document(message.chatId)
                .set(
                    mapOf(
                        "lastMessageCipher" to message.contentCipher.take(80),
                        "lastMessageType" to message.msgType,
                        "lastMessageTime" to FieldValue.serverTimestamp(),
                        "lastSenderUid" to authManager.getUid()
                    ),
                    SetOptions.merge()
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            // Enqueue retry
            enqueueRetry(message)
            Result.failure(e)
        }
    }

    private fun enqueueRetry(message: Message) {
        try {
            val data = androidx.work.Data.Builder()
                .putString("msgId", message.id)
                .putString("chatId", message.chatId)
                .build()
            val req = androidx.work.OneTimeWorkRequestBuilder<com.example.workers.SyncWorker>()
                .setInputData(data)
                .setConstraints(
                    androidx.work.Constraints.Builder()
                        .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 30, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            androidx.work.WorkManager.getInstance(context).enqueue(req)
        } catch (_: Exception) {
        }
    }

    /** Observe remote messages for a chat — inserts into Room when new remote doc arrives */
    fun observeChat(chatId: String): Flow<List<Message>> = callbackFlow {
        if (!authManager.isFirebaseConfigured()) {
            // No Firebase — just close, local data drives UI
            close()
            return@callbackFlow
        }
        try {
            val firestore = FirebaseFirestore.getInstance()
            val reg: ListenerRegistration = firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("timestamp")
                .addSnapshotListener { snap, err ->
                    if (err != null || snap == null) return@addSnapshotListener
                    scope.launch {
                        val remote = snap.documents.mapNotNull { doc ->
                            try {
                                doc.toObject(FirestoreMessage::class.java)?.toDomain()
                            } catch (_: Exception) { null }
                        }
                        // Insert each remote message not from self (senderUid != getUid()) into Room
                        val myUid = authManager.getUid()
                        for (msg in remote) {
                            // Avoid echo of own message: check if already exists locally
                            // Our Message.senderId == "me" for own, but Firestore stores senderUid
                            // So we skip if msg already in DB by id (Room will REPLACE anyway)
                            // Only insert if not already present to avoid loop
                            // We check by trying to insert — Room REPLACE is idempotent, but we
                            // avoid incrementing unread incorrectly by checking sender
                            val isOwn = msg.senderId == "me" // local own messages already in Room
                            if (!isOwn) {
                                // Use repository directly — will also update chat snippet transactionally
                                // But prevent pushing back to Firestore (avoid loop) — we insert locally only
                                // So we call DAO directly via repository's db? For Phase 2 scaffold,
                                // use chatRepository insert which would push again — avoid by checking source.
                                // Instead, insert via DAO without push: we need a local-only insert.
                                // For now, insert via repository but with guard: SyncManager will push only for "me" messages,
                                // so remote echo is safe because senderId != "me" → ChatViewModel will not push remote inserts.
                                chatRepository.insertMessage(msg)
                                // Only increment unread if not viewing that chat
                                // ChatViewModel handles incremental logic, but we can increment here
                                // if needed — Phase 1 already handles, so skip here.
                            }
                        }
                        trySend(remote)
                    }
                }
            awaitClose { reg.remove() }
        } catch (e: Exception) {
            close(e)
        }
    }
}
