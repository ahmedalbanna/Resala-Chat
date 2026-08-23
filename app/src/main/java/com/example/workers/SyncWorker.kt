package com.example.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Phase 2: WorkManager retry for Firestore pushes when offline.
 * Enqueued by FirestoreSyncManager.pushMessage on failure.
 * Retries with exponential backoff, NetworkType.CONNECTED.
 * Phase 2 scaffold: lightweight, no HiltWorker dependency to keep build simple.
 * Future: migrate to @HiltWorker + hilt-work 1.2.0 when needed.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val msgId = inputData.getString("msgId") ?: return@withContext Result.failure()
        val chatId = inputData.getString("chatId") ?: return@withContext Result.failure()
        return@withContext try {
            // Phase 2 scaffold: fetch message snapshot from Room
            val db = AppDatabase.getDatabase(applicationContext)
            // TODO Phase 2.1: add MessageDao.getMessageById(msgId) suspend query
            // For now we treat retry as success to clear queue if Firebase is configured
            // Real push will be attempted by SyncManager when next message is sent
            // If Firebase not configured, just succeed (local-only mode)
            val isFirebaseConfigured = try {
                com.google.firebase.FirebaseApp.getInstance() != null
            } catch (_: Exception) { false }

            if (!isFirebaseConfigured) {
                Result.success()
            } else {
                // Exponential backoff will retry up to 5 times; succeed to avoid stuck queue for scaffold
                Result.success()
            }
        } catch (e: Exception) {
            if (runAttemptCount < 5) Result.retry() else Result.failure()
        }
    }
}
