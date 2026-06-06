package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey val id: String,
    val chatId: String,
    val senderId: String,
    val contentCipher: String, // The encrypted ciphertext stored directly in SQL Database
    val msgType: String = "TEXT", // TEXT, IMAGE, AUDIO, LOCATION
    val localPath: String? = null, // Mock local file path for image/audio
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val isE2ee: Boolean = true,
    val isRead: Boolean = false,
    val isSent: Boolean = true,
    val audioDuration: Int = 0 // Duration of audio message in seconds
)
