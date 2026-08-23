# Architecture — Resala Chat

## Module Graph (Hilt)

```
ResalaApplication (@HiltAndroidApp) — app/src/main/java/com/example/ResalaApplication.kt:1
  └─ di/AppModule (@Module @InstallIn SingletonComponent) — di/AppModule.kt:1
       ├─ AppDatabase (Room v2, MIGRATION_1_2, seedCallback) — data/AppDatabase.kt:1
       │    ├─ UserDao / ChatDao / MessageDao — data/Daos.kt:1
       │    └─ RoomDatabase.Callback seed 4 users + 2 chats + 2 messages (was ViewModel race)
       ├─ ChatRepository (db.withTransaction, Paging) — data/ChatRepository.kt:1
       │    └─ db: AppDatabase? (null fallback for tests)
       ├─ AuthManager / FirestoreSyncManager (@Singleton @Inject) — data/remote/
       ├─ SignalManager / MediaManager / WebRtcManager (@Singleton) — util/
       └─ CoroutineScope(SupervisorJob + IO)
  └─ ChatViewModel (@HiltViewModel) — ui/ChatViewModel.kt:1
       ├─ repository, SavedStateHandle["chatId"], authManager, syncManager,
       │  signalManager, mediaManager, webRtcManager
       ├─ StateFlow: users, chats, filteredChats (FTS), typingChats, showDecrypted, activeCallState
       └─ Legacy ctor Application for previews/tests
MainActivity (@AndroidEntryPoint) — MainActivity.kt:1
  └─ hiltViewModel<ChatViewModel>() + NavHost / wide split
```

Hilt `ksp` compilers: `hilt-compiler` + `room-compiler` at `app/build.gradle.kts:142`.

## Data Layer

### Entities — `data/`
- `User` (`data/User.kt:1`) — `id, fullName, phone, bio, avatarResId, publicKey, isFavorite, lastSeen`
- `Chat` (`data/Chat.kt:1`) — `id, peerId, peerName, lastMessageText/Time, unreadCount, isGroup`
- `Message` (`data/Message.kt:1`) — `id, chatId, senderId, contentCipher, msgType, localPath, lat/lng, timestamp, isE2ee, isRead, audioDuration`

### DAOs — `data/Daos.kt:1`
- `UserDao.getAllUsers(): Flow<List<User>> ORDER BY fullName`
- `ChatDao.getAllChats(): Flow<List<Chat>> ORDER BY lastMessageTime DESC`, `updateChatLastMessage`, `incrementUnread`
- `MessageDao.getMessagesForChat(): Flow`, `getMessagesForChatPaging(): PagingSource<Int,Message>`, `getMessageById()`, `getMessageCount()`

### Database — `data/AppDatabase.kt:1`
- `version 2`, `fallbackToDestructiveMigrationOnDowngrade()` only (no wipe on upgrade)
- `MIGRATION_1_2` no-op (Phase 0 → 3 placeholder for Signal columns)
- `seedCallback` onCreate inserts 4 seed users (bot, Mona, Khaled, Sulaiman) + 2 chats + 2 encrypted messages via `EncryptionEngine.encrypt`

### Repository — `data/ChatRepository.kt:1`
- `allUsers / allChats` flows
- `insertMessage()` → `db.withTransaction { insert + updateChatLastMessage(snippet) }` (atomic, Phase 1)
- `getMessagesPager(pageSize=30)` → `Pager(PagingConfig)` from `MessageDao.getMessagesForChatPaging`

## Domain / Sync

### Auth — `data/remote/AuthManager.kt:1`
- `getUid()` — Firebase `currentUser.uid` or `local_<8>` cached in `SecureStorage`
- `signInAnonymously()`, `signInWithPhone(phone,otp)` stub (stores phone, upgrades anonymous)
- `isFirebaseConfigured()` guard — local-only mode without `google-services.json`

### Firestore — `data/remote/FirestoreSyncManager.kt:1`
- `FirestoreMessage` DTO (ciphertext-only, `senderUid`, `serverTimestamp`)
- `enableOfflinePersistence()` → `FirebaseFirestoreSettings(persistence=true, cache=UNLIMITED)` at `FirestoreSyncManager.kt:32`
- `pushMessage()` → `chats/{chatId}/messages/{msgId}` `set(merge)` + `chats/{chatId}` metadata, `FieldValue.serverTimestamp()`, enqueues `SyncWorker` on failure
- `observeChat()` → `callbackFlow` + `addSnapshotListener` orderBy `timestamp` → `repository.insertMessage()` (no echo for `senderId=="me"`)

### Worker — `workers/SyncWorker.kt:1`
- `CoroutineWorker`, `Data(msgId,chatId)`, `Constraints(CONNECTED)`, `Backoff EXPONENTIAL 30s`

## Crypto & Media

### Legacy Engine — `util/EncryptionEngine.kt:1`
- `PREFIX = "E2EE🛡️::"`, `GCM_IV_LENGTH=12`, `TAG=128`
- `deriveKey()` → `SHA-256(cleanKey)` → `SecretKeySpec(hash, AES)` (32B AES-256)
- `deriveLegacyKey()` → `copyOf(16)` AES-128 for pre-Phase0 ECB fallback
- `encrypt()` → random IV + `AES/GCM/NoPadding` → `Base64(IV||ct+tag)` + prefix
- `decrypt()` tries GCM → legacy ECB → plain Base64
- `generateFingerprint()` → `SHA-256(sortedKeys)` hex `chunked(8).take(4)`, `generateFullFingerprint()` 8 groups

### Signal — `util/SignalManager.kt:1`
- Stores `IdentityKeyPair` via `Curve.generateKeyPair()` reflection → `SecureStorage` (`signal_identity_*`), `RegId`
- `fingerprintFor()` delegates to `EncryptionEngine` scaffold (real `Fingerprint` in 3b)
- `isSignalSessionEstablished(peerPub)` → `SecureStorage["signal_session_<hash>"] != null`
- `encrypt()` → if session → `E2EE⚡Signal::` marker (future `SessionCipher`), else `EncryptionEngine.encrypt`
- `establishSession(peerPub, bundleJson)` / `clearSession()`

### Media — `util/MediaManager.kt:1`
- `MasterKey(AES256_GCM)` → `EncryptedFile(AES256_GCM_HKDF_4KB)` per `media_<msgId>.enc` in `filesDir`
- `saveEncrypted() / readEncrypted() / decryptedTempUri()` (`cacheDir/tmp_`), `storagePath()` `media/{chatId}/{msgId}.enc`, `clearCache()`

### WebRTC — `util/WebRtcManager.kt:1`
- `PeerConnectionFactory.initialize` via reflection (native guard), `isSimulated()` fallback
- `_state: StateFlow<WebRtcState>(isInitialized, isVideo, isAudio, connectionState, stats)`
- `createPeerConnection(isVideo)`, `toggleVideo/Audio()`, `close()`

### Secure Storage — `util/SecureStorage.kt:1`, `util/GeminiConfig.kt:1`
- `EncryptedSharedPreferences` (`MasterKey AES256_GCM`, `AES256_SIV/GCM` keys, `AES256_GCM` values) with robolectric fallback
- `GeminiConfig` safe `BuildConfig.GEMINI_API_KEY` wrapper (null if placeholder)

## UI Layer

### Navigation — `MainActivity.kt:1`
- `ResalaApplication @HiltAndroidApp` → `MainActivity @AndroidEntryPoint`
- `isWideScreen = screenWidthDp >=600` (Phase 4 WindowSizeClass medium, was `>600`, panel `320→360dp`) — `MainActivity.kt:68`
- Wide: `Row { Box(360dp) ChatListScreen | VerticalDivider 1.dp outlineVariant | Box(weight1) ChatDetail+remember }`
- Compact: `NavHost` `rememberNavController`, `LaunchedEffect(activeChatId)` sync, routes `chatList` / `chatDetail/{chatId}` + `onboarding` (Phase 4 if `!isOnboarded`), `composable("onboarding")` → `popUpTo inclusive`

### Screens — `ui/`
- `ChatListScreen.kt:1` — `searchQuery` normalized (`ّ` tatweel), filters `peerName/lastMessage/phone/bio` + FTS comment, `E2EEBanner` (via `components/ResalaComponents.kt`), `LazyRow users key=id`, `LazyColumn filteredChats key=id`, `ChatRowItem` a11y `semantics`, empty CTA `empty_state_add_button`
- `ChatDetailScreen.kt:1` — `FusedLocationProviderClient.lastLocation` (`ACCESS_FINE_LOCATION` launcher), `GetContent` `imagePickLauncher` → `onSendImageBytes`, `MediaManager`/`SignalManager` decrypt via `decryptDisplay`, `SecurityDetailsDialog` uses `SignalManager.fingerprintFor` + `isSignalSessionEstablished` (`ChatDetailScreen.kt:800`), wave `Canvas`, `MessageBubbleItem` key=id
- `CallingScreen.kt:1` — HUD `connected` vs `connecting`, driven by `WebRtcManager` when native, fallback simulated, `end_call_button` testTag
- `OnboardingScreen.kt:1` — phone `+966` `onboarding_phone_input` + OTP, `AuthManager.signInWithPhone`, 4 permission cards `POST_NOTIFICATIONS/RECORD_AUDIO/CAMERA/LOCATION` via `rememberLauncherForActivityResult`, `onboarding_verify/start_cta` `testTag`, skip→local
- Components `ui/components/ResalaComponents.kt:1` — `E2EEBanner`, `SecureAvatar`, `SectionLabel` (Phase 4 consolidation)
- Theme `ui/theme/Color.kt:1` Vibrant + dark tonal mapping (`VibrantPrimaryDark` etc), `Theme.kt:14` `DarkColorScheme` (`surfaceContainer`, `outlineVariant`), `Typography.kt`

### ViewModel — `ui/ChatViewModel.kt:1`
- `@HiltViewModel @Inject(repository, SavedStateHandle, authManager, syncManager, signalManager, mediaManager, webRtcManager)`
- Flows `users, chats, filteredChats (combine), typingChats, showDecrypted, activeCallState, searchQuery`
- `init` restores `handle["chatId"]`, syncs `_activeChatId ↔ handle`, warms `auth`, `webRtc`, `signal` identity, `startRemoteObserve`
- `setActiveChat()` clears unread + `handle`, `remoteObserveJob` cancel + `observeChat.collect`, `markRead()`
- `send*` → `signalManager.encrypt()` (fallback `EncryptionEngine`) → `repository.insertMessage` → `syncManager.pushMessage` → mock if `!isFirebaseConfigured`
- `callJob: Job?` leak fix — `startCall()` `webRtcManager.createPeerConnection()`, `toggleMute/Camera` delegate, `endCall()` `close()`

## Build

- `gradle/libs.versions.toml:1` — `agp 9.1.1`, `kotlin 2.2.10`, `composeBom 2025.09.00`, `room 2.7.0`, `hilt 2.52`, `paging 3.3.6`, `firebase 34.12.0`, `work 2.9.0`, `signal 2.8.2`, `webrtc 1.1.0`, `camera 1.5.0`, `coil 2.7.0`, `robolectric 4.16.1`, `roborazzi 1.59.0`
- `app/build.gradle.kts:1` — `hilt` plugin + `ksp` room/hilt, `firebase-bom` + `auth/firestore/storage`, `work-runtime-ktx`, `signal/webrtc/camera/coil`, `room-testing`/`arch-core-testing` for Phase 5
- `AndroidManifest.xml:11` — `ResalaApplication`, permissions `INTERNET, RECORD_AUDIO, CAMERA, FINE/COARSE_LOCATION, POST_NOTIFICATIONS, FOREGROUND_SERVICE*`
