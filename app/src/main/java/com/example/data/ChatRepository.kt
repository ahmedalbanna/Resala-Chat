package com.example.data

import kotlinx.coroutines.flow.Flow

class ChatRepository(
    private val userDao: UserDao,
    private val chatDao: ChatDao,
    private val messageDao: MessageDao
) {
    val allUsers: Flow<List<User>> = userDao.getAllUsers()
    val allChats: Flow<List<Chat>> = chatDao.getAllChats()

    fun getMessagesForChat(chatId: String): Flow<List<Message>> {
        return messageDao.getMessagesForChat(chatId)
    }

    fun getUserById(userId: String): Flow<User?> {
        return userDao.getUserById(userId)
    }

    suspend fun insertUser(user: User) {
        userDao.insertUser(user)
    }

    suspend fun insertUsers(users: List<User>) {
        userDao.insertUsers(users)
    }

    suspend fun insertChat(chat: Chat) {
        chatDao.insertChat(chat)
    }

    suspend fun insertMessage(message: Message) {
        messageDao.insertMessage(message)
        // Also update the chat's last message info
        val snippet = when (message.msgType) {
            "IMAGE" -> "🏞️ صورة مشفرة"
            "AUDIO" -> "🎙️ رسالة صوتية ملقنة"
            "LOCATION" -> "📍 موقع جغرافي آمن"
            else -> {
                // Return a decrypted prompt (or ciphertext helper)
                "رسالة مشفرة"
            }
        }
        chatDao.updateChatLastMessage(message.chatId, snippet, message.timestamp)
    }

    suspend fun updateChatLastMsgText(chatId: String, text: String, time: Long) {
        chatDao.updateChatLastMessage(chatId, text, time)
    }

    suspend fun clearUnread(chatId: String) {
        chatDao.clearUnreadCount(chatId)
    }

    suspend fun incrementUnread(chatId: String) {
        chatDao.incrementUnreadCount(chatId)
    }

    suspend fun deleteChat(chatId: String) {
        chatDao.deleteChatById(chatId)
    }
}
