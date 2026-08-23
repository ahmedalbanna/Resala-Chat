package com.example.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class ChatRepository(
    private val db: AppDatabase? = null,
    private val userDao: UserDao,
    private val chatDao: ChatDao,
    private val messageDao: MessageDao
) {
    // Legacy 3-arg constructor for tests / pre-Hilt callers (Phase 1 backward compat)
    constructor(userDao: UserDao, chatDao: ChatDao, messageDao: MessageDao) : this(
        db = null,
        userDao = userDao,
        chatDao = chatDao,
        messageDao = messageDao
    )
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

    /** Phase 1: atomic insert + lastMessage update via Room transaction (or sequential fallback for tests) */
    suspend fun insertMessage(message: Message) {
        val snippet = when (message.msgType) {
            "IMAGE" -> "🏞️ صورة مشفرة"
            "AUDIO" -> "🎙️ رسالة صوتية ملقنة"
            "LOCATION" -> "📍 موقع جغرافي آمن"
            else -> "رسالة مشفرة"
        }
        if (db != null) {
            db.withTransaction {
                messageDao.insertMessage(message)
                chatDao.updateChatLastMessage(message.chatId, snippet, message.timestamp)
            }
        } else {
            // Fallback for legacy callers / tests without DB handle
            messageDao.insertMessage(message)
            chatDao.updateChatLastMessage(message.chatId, snippet, message.timestamp)
        }
    }

    /** Paging 3: large history support without loading all messages */
    fun getMessagesPager(chatId: String, pageSize: Int = 30): Flow<PagingData<Message>> {
        return Pager(
            config = PagingConfig(pageSize = pageSize, enablePlaceholders = false),
            pagingSourceFactory = { messageDao.getMessagesForChatPaging(chatId) }
        ).flow
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
