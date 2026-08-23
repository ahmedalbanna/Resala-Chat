package com.example.ui

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.data.*
import com.example.data.remote.AuthManager
import com.example.data.remote.FirestoreSyncManager
import com.example.util.EncryptionEngine
import com.example.util.MediaManager
import com.example.util.SignalManager
import com.example.util.WebRtcManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val savedStateHandle: SavedStateHandle,
    private val authManager: AuthManager,
    private val syncManager: FirestoreSyncManager,
    private val signalManager: SignalManager,
    private val mediaManager: MediaManager,
    private val webRtcManager: WebRtcManager
) : ViewModel() {

    // Legacy constructor for previews/tests without Hilt (Phase 1 backward compat + Phase 2 local-only fallback)
    constructor(application: Application) : this(
        repository = ChatRepository(
            db = AppDatabase.getDatabase(application),
            userDao = AppDatabase.getDatabase(application).userDao(),
            chatDao = AppDatabase.getDatabase(application).chatDao(),
            messageDao = AppDatabase.getDatabase(application).messageDao()
        ),
        savedStateHandle = SavedStateHandle(),
        authManager = AuthManager(application),
        syncManager = FirestoreSyncManager(
            context = application,
            authManager = AuthManager(application),
            chatRepository = ChatRepository(
                db = AppDatabase.getDatabase(application),
                userDao = AppDatabase.getDatabase(application).userDao(),
                chatDao = AppDatabase.getDatabase(application).chatDao(),
                messageDao = AppDatabase.getDatabase(application).messageDao()
            )
        ),
        signalManager = SignalManager(application),
        mediaManager = MediaManager(application),
        webRtcManager = WebRtcManager(application)
    )

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

    // Simulated calling state (Phase 0 mock — real WebRTC in Phase 3)
    private val _activeCallState = MutableStateFlow<CallState?>(null)
    val activeCallState = _activeCallState.asStateFlow()

    // Phase 1: activeChatId mirrored to SavedStateHandle for process death + NavHost
    // Phase 2: Firestore remote observe job
    private var remoteObserveJob: Job? = null

    init {
        // Restore active chat from SavedStateHandle (NavHost deep link / rotation)
        savedStateHandle.get<String>("chatId")?.let { _activeChatId.value = it }
        // Keep handle in sync when _activeChatId changes
        viewModelScope.launch {
            _activeChatId.collect { id ->
                if (id != null) savedStateHandle["chatId"] = id else savedStateHandle.remove<String>("chatId")
            }
        }
        // Seeding now handled by AppDatabase seedCallback — no ViewModel race.
        // Phase 2: warm Firebase Auth + enable offline persistence (local-only fallback if not configured)
        try { syncManager.enableOfflinePersistence() } catch (_: Exception) {}
        viewModelScope.launch {
            try { authManager.signInAnonymously() } catch (_: Exception) {}
        }
        // Phase 3: init WebRTC + Signal identity
        try { webRtcManager.initialize() } catch (_: Exception) {}
        try { signalManager.getIdentityKeyPair() } catch (_: Exception) {}
        // Start remote observe for restored chat (deep link)
        _activeChatId.value?.let { startRemoteObserve(it) }
    }

    // Navigation and chat activation — also clears unread + updates SavedStateHandle + starts Firestore listener
    fun setActiveChat(chatId: String?) {
        _activeChatId.value = chatId
        remoteObserveJob?.cancel()
        if (chatId != null) {
            savedStateHandle["chatId"] = chatId
            viewModelScope.launch { repository.clearUnread(chatId) }
            startRemoteObserve(chatId)
        } else {
            savedStateHandle.remove<String>("chatId")
        }
    }

    private fun startRemoteObserve(chatId: String) {
        remoteObserveJob?.cancel()
        remoteObserveJob = viewModelScope.launch {
            try { syncManager.observeChat(chatId).collect {} } catch (_: Exception) {}
        }
    }

    /** Phase 1 fix: mark read on scroll-to-bottom (called from ChatDetail) */
    fun markRead(chatId: String) {
        viewModelScope.launch { repository.clearUnread(chatId) }
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

    // Get live messages flow (simple) + Paging 3 flow (Phase 1)
    fun getMessagesFlow(chatId: String): Flow<List<Message>> {
        return repository.getMessagesForChat(chatId)
    }

    fun getMessagesPager(chatId: String): Flow<PagingData<Message>> {
        return repository.getMessagesPager(chatId).cachedIn(viewModelScope)
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

    // Sending Actions — Phase 3: SignalManager (X3DH+Ratchet) with AES-GCM fallback + Firestore push
    fun sendTextMessage(chatId: String, text: String, peerPublicKey: String) {
        if (text.trim().isEmpty()) return
        viewModelScope.launch {
            val cipherText = try { signalManager.encrypt(text, peerPublicKey) } catch (_: Exception) { EncryptionEngine.encrypt(text, peerPublicKey) }
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
            try { syncManager.pushMessage(message) } catch (_: Exception) {}
            if (!authManager.isFirebaseConfigured()) {
                triggerMockPeerResponse(chatId, text, peerPublicKey)
            }
        }
    }

    fun sendLocationMessage(chatId: String, lat: Double, lng: Double, peerPublicKey: String) {
        viewModelScope.launch {
            val locationCaption = "📍 إحداثيات متبادلة مشفرة: ($lat, $lng)"
            val cipherText = try { signalManager.encrypt(locationCaption, peerPublicKey) } catch (_: Exception) { EncryptionEngine.encrypt(locationCaption, peerPublicKey) }
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
            try { syncManager.pushMessage(message) } catch (_: Exception) {}
            if (!authManager.isFirebaseConfigured()) {
                triggerMockPeerResponse(chatId, "[أرسل موقعاً جغرافياً]", peerPublicKey)
            }
        }
    }

    fun sendImageMessage(chatId: String, imageResName: String, peerPublicKey: String) {
        viewModelScope.launch {
            val caption = "🏞️ ملف وسائط مشفر وموثق"
            val cipherText = try { signalManager.encrypt(caption, peerPublicKey) } catch (_: Exception) { EncryptionEngine.encrypt(caption, peerPublicKey) }
            val msgId = UUID.randomUUID().toString()
            // Phase 3: save placeholder bytes via EncryptedFile (real camera will pass bytes)
            try { mediaManager.saveEncrypted(msgId, caption.toByteArray()) } catch (_: Exception) {}
            val message = Message(
                id = msgId,
                chatId = chatId,
                senderId = "me",
                contentCipher = cipherText,
                msgType = "IMAGE",
                localPath = imageResName,
                timestamp = System.currentTimeMillis()
            )
            repository.insertMessage(message)
            try { syncManager.pushMessage(message) } catch (_: Exception) {}
            // TODO Phase 3b: upload EncryptedFile to Firebase Storage media/{chatId}/{msgId}.enc
            if (!authManager.isFirebaseConfigured()) {
                triggerMockPeerResponse(chatId, "[أرسل صورة]", peerPublicKey)
            }
        }
    }

    /** Phase 3 overload: real image bytes from camera/gallery via MediaManager */
    fun sendImageMessage(chatId: String, imageBytes: ByteArray, peerPublicKey: String) {
        viewModelScope.launch {
            val msgId = UUID.randomUUID().toString()
            val path = try { mediaManager.saveEncrypted(msgId, imageBytes) } catch (_: Exception) { null }
            val caption = "🏞️ صورة مشفرة"
            val cipherText = try { signalManager.encrypt(caption, peerPublicKey) } catch (_: Exception) { EncryptionEngine.encrypt(caption, peerPublicKey) }
            val message = Message(id = msgId, chatId = chatId, senderId = "me", contentCipher = cipherText, msgType = "IMAGE", localPath = path, timestamp = System.currentTimeMillis())
            repository.insertMessage(message)
            try { syncManager.pushMessage(message) } catch (_: Exception) {}
            if (!authManager.isFirebaseConfigured()) triggerMockPeerResponse(chatId, "[أرسل صورة]", peerPublicKey)
        }
    }

    fun sendVoiceMessage(chatId: String, durationSecs: Int, peerPublicKey: String) {
        viewModelScope.launch {
            val caption = "🎙️ مقطع صوتي مرمز فائق الأمان"
            val cipherText = try { signalManager.encrypt(caption, peerPublicKey) } catch (_: Exception) { EncryptionEngine.encrypt(caption, peerPublicKey) }
            val msgId = UUID.randomUUID().toString()
            // Phase 3: MediaRecorder Opus bytes would be saved via MediaManager before send
            try { mediaManager.saveEncrypted(msgId, "opus_$durationSecs".toByteArray()) } catch (_: Exception) {}
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
            try { syncManager.pushMessage(message) } catch (_: Exception) {}
            if (!authManager.isFirebaseConfigured()) {
                triggerMockPeerResponse(chatId, "[أرسل تسجيلاً صوتياً]", peerPublicKey)
            }
        }
    }

    // Phase 1 fix: hold Job so while loop cancels on endCall (was leaking)
    // Phase 3: WebRtcManager drives real PeerConnection, fallback to simulated HUD if native not loaded
    private var callJob: Job? = null

    fun startCall(peerName: String, isVideo: Boolean) {
        callJob?.cancel()
        try { webRtcManager.createPeerConnection(isVideo) } catch (_: Exception) {}
        val webrtcState = webRtcManager.state.value
        val initialStatus = if (webrtcState.connectionState == "connected") "متصل WebRTC P2P 🛡️" else "جاري الاتصال (محاكاة) ... 🔒"
        _activeCallState.value = CallState(
            peerName = peerName,
            isVideo = isVideo,
            status = initialStatus,
            durationSecs = 0,
            activeMedia = true
        )
        callJob = viewModelScope.launch {
            delay(2000)
            // If WebRTC reported connected, show real status; else simulated
            val conn = webRtcManager.state.value.connectionState
            val connectedStatus = if (conn == "connected" && !webRtcManager.isSimulated()) "متصل WebRTC P2P — DTLS-SRTP 🛡️" else "متصل (محاكي) — تشفير محلي 🛡️"
            _activeCallState.value = _activeCallState.value?.copy(status = connectedStatus)
            while (_activeCallState.value != null) {
                delay(1000)
                _activeCallState.value = _activeCallState.value?.let {
                    it.copy(durationSecs = it.durationSecs + 1)
                }
            }
        }
    }

    fun toggleMuteCall() {
        val newMuted = !(_activeCallState.value?.isMuted ?: false)
        try { webRtcManager.toggleAudio(!newMuted) } catch (_: Exception) {}
        _activeCallState.value = _activeCallState.value?.let {
            it.copy(isMuted = newMuted)
        }
    }

    fun toggleCameraCall() {
        val newVideo = !(_activeCallState.value?.isVideo ?: false)
        try { webRtcManager.toggleVideo(newVideo) } catch (_: Exception) {}
        _activeCallState.value = _activeCallState.value?.let {
            it.copy(isVideo = newVideo)
        }
    }

    fun endCall() {
        callJob?.cancel()
        callJob = null
        try { webRtcManager.close() } catch (_: Exception) {}
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
                    "حالياً نستخدم تشفير محلي AES-256-GCM تجريبي لكل جهة اتصال (IV عشوائي لكل رسالة). بروتوكول Signal الحقيقي (Double Ratchet + Curve25519) قيد التطوير في Phase 3 وسيحل محله — لا نعتمد عليه أمنياً بعد. 🛡️"
                }
                userText.contains("مكالمة") || userText.contains("اتصال") || userText.contains("فيديو") -> {
                    "تلك الأيقونات تشغل حالياً محاكي مكالمات محلي فقط (بدون WebRTC حقيقي). سيتم ربط WebRTC P2P المشفر في Phase 3! 📞"
                }
                else -> {
                    "تم استلام رسالتك المشفرة بنجاح 🛡️ بموجب خوارزمية التشفير الحامية، تم استخدام مفتاح فريد لفك تشفيرها في واجهتي فقط. أخبرني المزيد!"
                }
            }

            val replyCipher = try { signalManager.encrypt(responseText, peerPublicKey) } catch (_: Exception) { EncryptionEngine.encrypt(responseText, peerPublicKey) }
            val replyMsg = Message(
                id = UUID.randomUUID().toString(),
                chatId = chatId,
                senderId = chatId.substringAfter("chat_"),
                contentCipher = replyCipher,
                timestamp = System.currentTimeMillis()
            )
            repository.insertMessage(replyMsg)
            if (_activeChatId.value != chatId) {
                repository.incrementUnread(chatId)
            }
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

// State representing simulated call (Phase 0) — real WebRTC in Phase 3
data class CallState(
    val peerName: String,
    val isVideo: Boolean,
    val status: String,
    val durationSecs: Int,
    val activeMedia: Boolean,
    val isMuted: Boolean = false
)
