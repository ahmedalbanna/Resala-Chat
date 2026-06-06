package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val fullName: String,
    val phone: String,
    val bio: String,
    val avatarResId: Int = 0, // Placeholder avatar icon ID
    val publicKey: String,    // Public Key for End-to-End Encryption visualization
    val isFavorite: Boolean = false,
    val lastSeen: String = "نشط الآن"
)
