package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.util.EncryptionEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = ChatRepository(db.userDao(), db.chatDao(), db.messageDao())

    // UI States
    val users: StateFlow<List<User>> = repository.allUsers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val chats: StateFlow<List<Chat>> = repository.allChats.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Active screen states
    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatIdDetails = _activeChatId.asStateFlow()

    private val _showDecrypted = MutableStateFlow(true)
    val showDecrypted = _showDecrypted.asStateFlow()

    // Tracking active typing indicator per chat (chatId -> isTyping)
    private val _typingChats = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val typingChats = _typingChats.asStateFlow()

    // Live WebRTC calling simulator state
    private val _activeCallState = MutableStateFlow<CallState?>(null)
    val activeCallState = _activeCallState.asStateFlow()

    init {
        // Prepopulate users and chats if database is empty
        viewModelScope.launch {
            users.take(1).collect { existingUsers ->
                if (existingUsers.isEmpty()) {
                    seedDatabase()
                }
            }
        }
    }

    private suspend fun seedDatabase() {
        val seedUsers = listOf(
            User(
                id = "user_bot",
                fullName = "المساعد الذكي المشفر 🤖",
                phone = "+966 54 000 0000",
                bio = "روبوت ذكي للرد التلقائي وإثبات التشفير الفوري",
                publicKey = "IK-robo-secure-bot-key-83ad",
                isFavorite = true,
                lastSeen = "متصل الآن"
            ),
            User(
                id = "user_mona",
                fullName = "منى الكردي 🛡️",
                phone = "+966 50 123 4567",
                bio = "متوفرة | مفتاح الهوية المعتمد نشط",
                publicKey = "IK-98df-892c-bef4-c321",
                isFavorite = true,
                lastSeen = "نشط منذ ٤ د"
            ),
            User(
                id = "user_khaled",
                fullName = "المهندس خالد ⚙️",
                phone = "+966 55 987 6543",
                bio = "في اجتماع آمن مغلق 🔒 المراسلة مشفرة",
                publicKey = "IK-a47f-8be3-91cd-fb92",
                isFavorite = false,
                lastSeen = "متصل الآن"
            ),
            User(
                id = "user_sulaiman",
                fullName = "سليمان الفهيد 🏡",
                phone = "+966 53 456 7890",
                bio = "تراسل معي لحماية خصوصيتك",
                publicKey = "IK-38bc-9fdb-4328-da62",
                isFavorite = false,
                lastSeen = "نشط أمس"
            )
        )
        repository.insertUsers(seedUsers)

        // Seed initial conversations (chats)
        val chatBot = Chat(
            id = "chat_user_bot",
            peerId = "user_bot",
            peerName = "المساعد الذكي المشفر 🤖",
            lastMessageText = "دردشة مشفرة تامة (End-to-End)",
            lastMessageTime = System.currentTimeMillis() - 7200000,
            unreadCount = 1
        )
        val chatMona = Chat(
            id = "chat_user_mona",
            peerId = "user_mona",
            peerName = "منى الكردي 🛡️",
            lastMessageText = "أهلاً بك! تم إنشاء الجلسة الآمنة بيننا بنجاح.",
            lastMessageTime = System.currentTimeMillis() - 3600000,
            unreadCount = 0
        )

        repository.insertChat(chatBot)
        repository.insertChat(chatMona)

        // Seed some initial encrypted messages to bot conversation
        val msg1 = Message(
            id = UUID.randomUUID().toString(),
            chatId = "chat_user_bot",
            senderId = "user_bot",
            contentCipher = EncryptionEngine.encrypt("أهلاً بك في رِسالة! 👋 هذا التطبيق مشفر بالكامل ببروتوكول يحمي بياناتك محلياً وفي السقيفة السحابية. جرّب إرسال نصوص، رسائل صوتية، أو مواقع جغرافية، أو تجربة المكالمات الآمنة!", "IK-robo-secure-bot-key-83ad"),
            timestamp = System.currentTimeMillis() - 7200000
        )
        val msg2 = Message(
            id = UUID.randomUUID().toString(),
            chatId = "chat_user_mona",
            senderId = "user_mona",
            contentCipher = EncryptionEngine.encrypt("أهلاً بك! تم إنشاء الجلسة الآمنة بيننا بنجاح.", "IK-98df-892c-bef4-c321"),
            timestamp = System.currentTimeMillis() - 3600000
        )

        db.messageDao().insertMessage(msg1)
        db.messageDao().insertMessage(msg2)
    }

    // Navigation and chat activation
    fun setActiveChat(chatId: String?) {
        _activeChatId.value = chatId
        if (chatId != null) {
            viewModelScope.launch {
                repository.clearUnread(chatId)
            }
        }
    }

    fun toggleDecryptedView() {
        _showDecrypted.value = !_showDecrypted.value
    }

    // Start a message session with an existing contact
    fun startChatWithUser(user: User): String {
        val chatId = "chat_${user.id}"
        viewModelScope.launch {
            val exist = chats.value.any { it.id == chatId }
            if (!exist) {
                val newChat = Chat(
                    id = chatId,
                    peerId = user.id,
                    peerName = fullNameWithoutIcons(user.fullName),
                    lastMessageText = "بدأت محادثة آمنة جديدة 🛡️",
                    lastMessageTime = System.currentTimeMillis()
                )
                repository.insertChat(newChat)
            }
            setActiveChat(chatId)
        }
        return chatId
    }

    // Helper to strip emoji for chat list peer names if needed
    private fun fullNameWithoutIcons(name: String): String {
        return name.replace(Regex("[\uD83C-\uDBFF\uDC00-\uDFFF]"), "").trim()
    }

    // Get live messages flow
    fun getMessagesFlow(chatId: String): Flow<List<Message>> {
        return repository.getMessagesForChat(chatId)
    }

    // Add a custom user/contact
    fun createContactAndStartChat(name: String, phone: String, bio: String) {
        viewModelScope.launch {
            val randomId = "user_custom_${UUID.randomUUID().toString().take(6)}"
            val randomKey = "IK-${UUID.randomUUID().toString().take(12)}"
            val newUser = User(
                id = randomId,
                fullName = name,
                phone = phone,
                bio = bio.ifEmpty { "متوافر في رِسالة الآمن" },
                publicKey = randomKey,
                isFavorite = false,
                lastSeen = "نشط الآن"
            )
            repository.insertUser(newUser)
            startChatWithUser(newUser)
        }
    }

    // Sending Actions
    fun sendTextMessage(chatId: String, text: String, peerPublicKey: String) {
        if (text.trim().isEmpty()) return
        viewModelScope.launch {
            val cipherText = EncryptionEngine.encrypt(text, peerPublicKey)
            val msgId = UUID.randomUUID().toString()
            val message = Message(
                id = msgId,
                chatId = chatId,
                senderId = "me",
                contentCipher = cipherText,
                msgType = "TEXT",
                timestamp = System.currentTimeMillis()
            )
            repository.insertMessage(message)

            // Trigger simulated secure bot/peer reply
            triggerMockPeerResponse(chatId, text, peerPublicKey)
        }
    }

    fun sendLocationMessage(chatId: String, lat: Double, lng: Double, peerPublicKey: String) {
        viewModelScope.launch {
            val locationCaption = "📍 إحداثيات متبادلة مشفرة: ($lat, $lng)"
            val cipherText = EncryptionEngine.encrypt(locationCaption, peerPublicKey)
            val msgId = UUID.randomUUID().toString()
            val message = Message(
                id = msgId,
                chatId = chatId,
                senderId = "me",
                contentCipher = cipherText,
                msgType = "LOCATION",
                latitude = lat,
                longitude = lng,
                timestamp = System.currentTimeMillis()
            )
            repository.insertMessage(message)
            triggerMockPeerResponse(chatId, "[أرسل موقعاً جغرافياً]", peerPublicKey)
        }
    }

    fun sendImageMessage(chatId: String, imageResName: String, peerPublicKey: String) {
        viewModelScope.launch {
            val caption = "🏞️ ملف وسائط مشفر وموثق"
            val cipherText = EncryptionEngine.encrypt(caption, peerPublicKey)
            val msgId = UUID.randomUUID().toString()
            val message = Message(
                id = msgId,
                chatId = chatId,
                senderId = "me",
                contentCipher = cipherText,
                msgType = "IMAGE",
                localPath = imageResName, // pre-set resource name reference
                timestamp = System.currentTimeMillis()
            )
            repository.insertMessage(message)
            triggerMockPeerResponse(chatId, "[أرسل صورة]", peerPublicKey)
        }
    }

    fun sendVoiceMessage(chatId: String, durationSecs: Int, peerPublicKey: String) {
        viewModelScope.launch {
            val caption = "🎙️ مقطع صوتي مرمز فائق الأمان"
            val cipherText = EncryptionEngine.encrypt(caption, peerPublicKey)
            val msgId = UUID.randomUUID().toString()
            val message = Message(
                id = msgId,
                chatId = chatId,
                senderId = "me",
                contentCipher = cipherText,
                msgType = "AUDIO",
                localPath = "recorded_audio_sample",
                audioDuration = durationSecs,
                timestamp = System.currentTimeMillis()
            )
            repository.insertMessage(message)
            triggerMockPeerResponse(chatId, "[أرسل تسجيلاً صوتياً]", peerPublicKey)
        }
    }

    // Call Simulator Controls
    fun startCall(peerName: String, isVideo: Boolean) {
        _activeCallState.value = CallState(
            peerName = peerName,
            isVideo = isVideo,
            status = "جاري الاتصال الآمن WebRTC... 🔒",
            durationSecs = 0,
            activeMedia = true
        )
        // Simulate peer answering after 2 seconds, counting duration
        viewModelScope.launch {
            delay(2000)
            _activeCallState.value = _activeCallState.value?.copy(status = "متصل - تشفير مباشر P2P 🛡️")
            while (_activeCallState.value != null) {
                delay(1000)
                _activeCallState.value = _activeCallState.value?.let {
                    it.copy(durationSecs = it.durationSecs + 1)
                }
            }
        }
    }

    fun toggleMuteCall() {
        _activeCallState.value = _activeCallState.value?.let {
            it.copy(isMuted = !it.isMuted)
        }
    }

    fun toggleCameraCall() {
        _activeCallState.value = _activeCallState.value?.let {
            it.copy(isVideo = !it.isVideo)
        }
    }

    fun endCall() {
        _activeCallState.value = null
    }

    // Private simulated network bot responder
    private fun triggerMockPeerResponse(chatId: String, userText: String, peerPublicKey: String) {
        viewModelScope.launch {
            // Only respond if the active chat matches
            delay(1000)
            _typingChats.update { it + (chatId to true) }
            repository.updateChatLastMsgText(chatId, "جاري الكتابة الآن...", System.currentTimeMillis())

            delay(2200) // typing wait for natural response
            _typingChats.update { it + (chatId to false) }

            val responseText = when {
                userText.contains("مرحبا") || userText.contains("اهلين") || userText.contains("أهلاً") || userText.contains("السلام") -> {
                    "أهلاً بك يا صديقي العزيز! 💬 أنا متصل الآن عبر خادم WebSocket مشفر بخصوصية تامة. كيف تجد وضوح الصوت واستقرار المنصة؟"
                }
                userText == "[أرسل موقعاً جغرافياً]" -> {
                    "أوه! تم فك تشفير موقعك الآمن بنجاح 📍 أرى إحداثياتك بوضوح على الخريطة التفاعلية الفورية."
                }
                userText == "[أرسل تسجيلاً صوتياً]" -> {
                    "استلمت رسالتك الصوتية المشفرة 🎙️ تم تنزيلها وفك ضغط ترميز الصوت Opus بنجاح آمن."
                }
                userText == "[أرسل صورة]" -> {
                    "يا لها من لقطة مشفرة رائعة! 🏞️ تم فك تشفير وسائطك المخزنة محلياً بالكامل."
                }
                userText.contains("تشفير") || userText.contains("أمان") || userText.contains("امن") -> {
                    "نعم! نستخدم بروتوكول Signal (Double Ratchet + Curve25519) لتحديث مفاتيح الرسائل باستمرار. حتى لو تم التقاط حزمة، لن يقرأها أحد! 🛡️"
                }
                userText.contains("مكالمة") || userText.contains("اتصال") || userText.contains("فيديو") -> {
                    "جرب استخدام أيقونات الهاتف 📞 والكاميرا 📹 في شريط العنوان بالأعلى لتجربة محاكي مكالمات WebRTC الصوتية والمرئية المشفرة!"
                }
                else -> {
                    "تم استلام رسالتك المشفرة بنجاح 🛡️ بموجب خوارزمية التشفير الحامية، تم استخدام مفتاح فريد لفك تشفيرها في واجهتي فقط. أخبرني المزيد!"
                }
            }

            val replyCipher = EncryptionEngine.encrypt(responseText, peerPublicKey)
            val replyMsg = Message(
                id = UUID.randomUUID().toString(),
                chatId = chatId,
                senderId = chatId.substringAfter("chat_"),
                contentCipher = replyCipher,
                timestamp = System.currentTimeMillis()
            )
            repository.insertMessage(replyMsg)
            repository.incrementUnread(chatId)
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ChatViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

// State representing WebRTC simulated call
data class CallState(
    val peerName: String,
    val isVideo: Boolean,
    val status: String,
    val durationSecs: Int,
    val activeMedia: Boolean,
    val isMuted: Boolean = false
)
