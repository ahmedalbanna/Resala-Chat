package com.example.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Phase 5: Daos + Repository transaction tests with inMemoryDatabase (Robolectric).
 * Covers insert, paging source, @Transaction atomicity.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DaosTest {

  private lateinit var db: AppDatabase
  private lateinit var repository: ChatRepository

  @Before
  fun setup() {
    db = Room.inMemoryDatabaseBuilder(
      ApplicationProvider.getApplicationContext(),
      AppDatabase::class.java
    ).allowMainThreadQueries().build()
    repository = ChatRepository(db, db.userDao(), db.chatDao(), db.messageDao())
  }

  @After
  fun teardown() {
    db.close()
  }

  @Test
  fun insertUser_and_retrieve() = runTest {
    val user = User(id = "u1", fullName = "Test User", phone = "+966500000000", bio = "bio", publicKey = "IK-test")
    repository.insertUser(user)
    val users = repository.allUsers.first()
    assertEquals(1, users.size)
    assertEquals("u1", users[0].id)
  }

  @Test
  fun insertMessage_updatesChatLastMessage_atomically() = runTest {
    val user = User(id = "u2", fullName = "منى", phone = "+966501", bio = "", publicKey = "IK-a")
    val chat = Chat(id = "chat_u2", peerId = "u2", peerName = "منى", lastMessageText = "", lastMessageTime = 0)
    repository.insertUser(user)
    repository.insertChat(chat)
    val msg = Message(id = "m1", chatId = "chat_u2", senderId = "me", contentCipher = "E2EE🛡️::abc", msgType = "TEXT", timestamp = 12345)
    repository.insertMessage(msg)
    val chats = repository.allChats.first()
    assertEquals(1, chats.size)
    assertEquals("رسالة مشفرة", chats[0].lastMessageText)
    assertEquals(12345, chats[0].lastMessageTime)
  }

  @Test
  fun pagingSource_returnsMessages() = runTest {
    val chatId = "chat_paging"
    repository.insertUser(User(id = "u3", fullName = "خالد", phone = "1", bio = "", publicKey = "IK-p"))
    repository.insertChat(Chat(id = chatId, peerId = "u3", peerName = "خالد", lastMessageText = ""))
    repeat(5) { i ->
      repository.insertMessage(Message(id = "m$i", chatId = chatId, senderId = "me", contentCipher = "E2EE🛡️::${i}", timestamp = i.toLong()))
    }
    // Paging source should load
    val pagingSource = db.messageDao().getMessagesForChatPaging(chatId)
    val result = pagingSource.load(
      androidx.paging.PagingSource.LoadParams.Refresh(key = null, loadSize = 10, placeholdersEnabled = false)
    )
    assertTrue(result is androidx.paging.PagingSource.LoadResult.Page)
    val page = result as androidx.paging.PagingSource.LoadResult.Page
    assertEquals(5, page.data.size)
  }

  @Test
  fun getMessageById_found() = runTest {
    val msg = Message(id = "mid123", chatId = "c1", senderId = "me", contentCipher = "E2EE🛡️::x")
    db.messageDao().insertMessage(msg)
    val found = db.messageDao().getMessageById("mid123")
    assertNotNull(found)
    assertEquals("mid123", found!!.id)
  }

  @Test
  fun incrementAndClearUnread() = runTest {
    val chat = Chat(id = "chat_unread", peerId = "u4", peerName = "سليمان", unreadCount = 0)
    repository.insertChat(chat)
    repository.incrementUnread("chat_unread")
    repository.incrementUnread("chat_unread")
    var chats = repository.allChats.first()
    assertEquals(2, chats.find { it.id == "chat_unread" }!!.unreadCount)
    repository.clearUnread("chat_unread")
    chats = repository.allChats.first()
    assertEquals(0, chats.find { it.id == "chat_unread" }!!.unreadCount)
  }
}
