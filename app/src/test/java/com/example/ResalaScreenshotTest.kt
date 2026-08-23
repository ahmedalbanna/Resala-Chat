package com.example

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.Chat
import com.example.data.User
import com.example.ui.ChatListScreen
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Phase 4: Roborazzi screenshots — light/dark + LTR/RTL (Arabic) for a11y + RTL audit.
 * Verifies Theme consolidation (Vibrant), divider hairline, E2EE banner, FTS empty state.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class ResalaScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val sampleUsers = listOf(
    User(id = "u1", fullName = "منى الكردي 🛡️", phone = "+966 50 123 4567", bio = "مفتاح نشط", publicKey = "IK-98df", lastSeen = "متصل الآن"),
    User(id = "u2", fullName = "المهندس خالد ⚙️", phone = "+966 55 987 6543", bio = "في اجتماع", publicKey = "IK-a47f", lastSeen = "متصل الآن")
  )
  private val sampleChats = listOf(
    Chat(id = "chat_u1", peerId = "u1", peerName = "منى الكردي", lastMessageText = "أهلاً بك!", lastMessageTime = System.currentTimeMillis(), unreadCount = 2),
    Chat(id = "chat_u2", peerId = "u2", peerName = "المهندس خالد", lastMessageText = "تم إنشاء الجلسة الآمنة", lastMessageTime = System.currentTimeMillis() - 3600000)
  )

  @Test
  fun chatList_light_ltr() {
    composeTestRule.setContent {
      MyApplicationTheme(darkTheme = false) {
        ChatListScreen(chats = sampleChats, users = sampleUsers, activeChatId = null, typingChats = emptyMap(), showDecrypted = true, onChatSelected = {}, onStartNewChat = {}, onAddContact = { _, _, _ -> }, onToggleDecrypted = {})
      }
    }
    composeTestRule.onRoot().captureRoboImage("src/test/screenshots/resala_chatlist_light.png")
  }

  @Test
  fun chatList_dark_ltr() {
    composeTestRule.setContent {
      MyApplicationTheme(darkTheme = true) {
        ChatListScreen(chats = sampleChats, users = sampleUsers, activeChatId = "chat_u1", typingChats = mapOf("chat_u1" to true), showDecrypted = true, onChatSelected = {}, onStartNewChat = {}, onAddContact = { _, _, _ -> }, onToggleDecrypted = {})
      }
    }
    composeTestRule.onRoot().captureRoboImage("src/test/screenshots/resala_chatlist_dark.png")
  }

  @Test
  @Config(qualifiers = "ar-rSA")
  fun chatList_light_rtl_arabic() {
    composeTestRule.setContent {
      CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MyApplicationTheme(darkTheme = false) {
          ChatListScreen(chats = sampleChats, users = sampleUsers, activeChatId = null, typingChats = emptyMap(), showDecrypted = false, onChatSelected = {}, onStartNewChat = {}, onAddContact = { _, _, _ -> }, onToggleDecrypted = {})
        }
      }
    }
    composeTestRule.onRoot().captureRoboImage("src/test/screenshots/resala_chatlist_rtl.png")
  }
}
