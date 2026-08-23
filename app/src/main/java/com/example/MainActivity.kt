package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.Chat
import com.example.data.User
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Initialize modern Chat ViewModel with constructor injection
                val viewModel: ChatViewModel = viewModel(
                    factory = ChatViewModel.Factory(application)
                )

                val callState by viewModel.activeCallState.collectAsState()

                if (callState != null) {
                    // Full screen calling overlay if call is active
                    CallingScreen(
                        callState = callState!!,
                        onMuteToggle = { viewModel.toggleMuteCall() },
                        onCameraToggle = { viewModel.toggleCameraCall() },
                        onEndCall = { viewModel.endCall() }
                    )
                } else {
                    // Main layout controller
                    MainAppLayout(viewModel)
                }
            }
        }
    }
}

@Composable
fun MainAppLayout(viewModel: ChatViewModel) {
    val chats by viewModel.chats.collectAsState()
    val users by viewModel.users.collectAsState()
    val activeChatId by viewModel.activeChatIdDetails.collectAsState()
    val typingChats by viewModel.typingChats.collectAsState()
    val showDecrypted by viewModel.showDecrypted.collectAsState()

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val isWideScreen = screenWidthDp > 600

    if (isWideScreen) {
        // Expanded layout: List-Detail Side-By-Side canonical split screen
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight()
            ) {
                ChatListScreen(
                    chats = chats,
                    users = users,
                    activeChatId = activeChatId,
                    typingChats = typingChats,
                    showDecrypted = showDecrypted,
                    onChatSelected = { viewModel.setActiveChat(it) },
                    onStartNewChat = { viewModel.startChatWithUser(it) },
                    onAddContact = { name, phone, bio -> viewModel.createContactAndStartChat(name, phone, bio) },
                    onToggleDecrypted = { viewModel.toggleDecryptedView() }
                )
            }

            // Vertical line divider
            VerticalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.5.dp)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                if (activeChatId != null) {
                    val activeChat = chats.find { it.id == activeChatId }
                    val activeUser = users.find { it.id == activeChat?.peerId }
                    val messagesFlow = viewModel.getMessagesFlow(activeChatId!!)
                    val messages by messagesFlow.collectAsState(initial = emptyList())

                    if (activeUser != null && activeChat != null) {
                        ChatDetailScreen(
                            user = activeUser,
                            messages = messages,
                            isTyping = typingChats[activeChatId!!] ?: false,
                            showDecrypted = showDecrypted,
                            onSendMessage = { viewModel.sendTextMessage(activeChatId!!, it, activeUser.publicKey) },
                            onSendLocation = { lat, lng -> viewModel.sendLocationMessage(activeChatId!!, lat, lng, activeUser.publicKey) },
                            onSendImage = { resName -> viewModel.sendImageMessage(activeChatId!!, resName, activeUser.publicKey) },
                            onSendVoice = { duration -> viewModel.sendVoiceMessage(activeChatId!!, duration, activeUser.publicKey) },
                            onStartCall = { isVid -> viewModel.startCall(activeUser.fullName, isVid) },
                            onBack = { viewModel.setActiveChat(null) },
                            onToggleDecrypted = { viewModel.toggleDecryptedView() }
                        )
                    }
                } else {
                    // Split screen static placeholder
                    WideScreenWelcomeSplash()
                }
            }
        }
    } else {
        // Compact layout (Phone default view): Switch pages single view layout
        if (activeChatId != null) {
            val activeChat = chats.find { it.id == activeChatId }
            val activeUser = users.find { it.id == activeChat?.peerId }
            val messagesFlow = viewModel.getMessagesFlow(activeChatId!!)
            val messages by messagesFlow.collectAsState(initial = emptyList())

            if (activeUser != null && activeChat != null) {
                // Back navigation gesture controller
                BackHandler {
                    viewModel.setActiveChat(null)
                }

                ChatDetailScreen(
                    user = activeUser,
                    messages = messages,
                    isTyping = typingChats[activeChatId!!] ?: false,
                    showDecrypted = showDecrypted,
                    onSendMessage = { viewModel.sendTextMessage(activeChatId!!, it, activeUser.publicKey) },
                    onSendLocation = { lat, lng -> viewModel.sendLocationMessage(activeChatId!!, lat, lng, activeUser.publicKey) },
                    onSendImage = { resName -> viewModel.sendImageMessage(activeChatId!!, resName, activeUser.publicKey) },
                    onSendVoice = { duration -> viewModel.sendVoiceMessage(activeChatId!!, duration, activeUser.publicKey) },
                    onStartCall = { isVid -> viewModel.startCall(activeUser.fullName, isVid) },
                    onBack = { viewModel.setActiveChat(null) },
                    onToggleDecrypted = { viewModel.toggleDecryptedView() }
                )
            }
        } else {
            ChatListScreen(
                chats = chats,
                users = users,
                activeChatId = activeChatId,
                typingChats = typingChats,
                showDecrypted = showDecrypted,
                onChatSelected = { viewModel.setActiveChat(it) },
                onStartNewChat = { viewModel.startChatWithUser(it) },
                onAddContact = { name, phone, bio -> viewModel.createContactAndStartChat(name, phone, bio) },
                onToggleDecrypted = { viewModel.toggleDecryptedView() }
            )
        }
    }
}

@Composable
fun WideScreenWelcomeSplash() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "قفل أمان",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
            }
            
            Text(
                text = "رِسالة للتراسل الآمن",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "اختر جهة اتصال أو دردشة قائمة من لوحة اليسار لبدء تبادل الرسائل المشفرة نهاية لنهاية (E2EE) الموثقة بالكامل.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 380.dp)
            )
            
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "بروتوكول تشغير تام",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "تشفير AES-256-GCM محلي (تجريبي) + محاكي مكالمات",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
