# Features — Resala Chat (رِسالة)

Arabic-first, offline-first, E2EE messaging. Feature completeness by phase.

## Phase 0 — Hardening (1.0.1-phase0)

- **AES-256-GCM** per peer (`IV 12B` random, `TAG 128`) — `util/EncryptionEngine.kt:44`, fingerprint hex `util/EncryptionEngine.kt:109`
- **EncryptedSharedPreferences** for keys — `util/SecureStorage.kt:15`
- **Honest UI** — `MainActivity.kt:234` `AES-256-GCM` badge, `ChatDetailScreen.kt:187` banner, `CallingScreen.kt:62` `محاكي`
- **Hardened build** — `compileSdk 36`, R8, no `debug.keystore` in repo

## Phase 1 — Architecture

- **Hilt DI** — `ResalaApplication`, `di/AppModule`, `@HiltViewModel` (`ui/ChatViewModel.kt:20`)
- **NavHost** — `chatList` / `chatDetail/{chatId}` + `SavedStateHandle["chatId"]` (`MainActivity.kt:128`, `ChatViewModel.kt:50`), wide `360dp` split
- **Transactions & Paging** — `insertMessage() withTransaction` (`data/ChatRepository.kt:46`), `getMessagesPager(30)` (`data/Daos.kt:46`)
- **Flow fixes** — `remember(chatId){getMessagesFlow}` + `key={id}` (`MainActivity.kt:104`, `ChatListScreen.kt:279`)
- **Unread & call leak** — skip `increment` if active (`ChatViewModel.kt:321`), `callJob:Job?` (`ChatViewModel.kt:234`)
- **Permissions** — `activity.compose` `rememberLauncherForActivityResult` replaces `accompanist` (`ui/ChatDetailScreen.kt:60`)

## Phase 2 — Real-Time (1.2.0-phase2)

- **Auth** — `AuthManager` phone `+966` / anonymous (`data/remote/AuthManager.kt:1`) cached in `SecureStorage`, `isFirebaseConfigured` guard
- **Firestore** — `FirestoreSyncManager` `FirestoreMessage` DTO ciphertext-only, `pushMessage` `serverTimestamp` + `observeChat` `callbackFlow` (`data/remote/FirestoreSyncManager.kt:1`), `firestore.rules:1` `senderUid==auth.uid`
- **WorkManager** — `SyncWorker` `CONNECTED` exponential 30s (`workers/SyncWorker.kt:1`)
- **Offline-first** — `enableOfflinePersistence()` `CACHE_SIZE_UNLIMITED`, local fallback without `google-services.json` (`AuthManager.kt:32`)

## Phase 3 — Real E2EE & Media (1.3.0-phase3)

- **Signal** — `SignalManager` `IdentityKeyPair` via `Curve.generateKeyPair()` reflection → `SecureStorage`, `fingerprintFor`, `isSignalSessionEstablished`, `encrypt` `E2EE⚡Signal::` fallback `EncryptionEngine` (`util/SignalManager.kt:1`), `ChatViewModel` now `signalManager.encrypt`
- **Media** — `MediaManager` `EncryptedFile` `AES256_GCM_HKDF_4KB` (`util/MediaManager.kt:15`), `saveEncrypted/readEncrypted/decryptedTempUri/storagePath media/{chatId}/{msgId}.enc`, `ChatViewModel.sendImageMessage(ByteArray)` + voice Opus scaffold
- **Location** — `FusedLocationProviderClient.lastLocation` with permission flow → fallback random Riyadh (`ui/ChatDetailScreen.kt:313`)
- **Camera** — `GetContent` `imagePickLauncher` → `onSendImageBytes` (`ui/ChatDetailScreen.kt:313`), `camera* 1.5.0` re-enabled (`gradle/libs.versions.toml:40`), Coil 2.7.0 display via decrypted `Uri`
- **WebRTC** — `WebRtcManager` `PeerConnectionFactory` reflection guard (`util/WebRtcManager.kt:1`), `ChatViewModel.startCall` `createPeerConnection` → status `DTLS-SRTP 🛡️` vs `محاكي` (`ui/ChatViewModel.kt:237`), `CallingScreen` HUD, `POST_NOTIFICATIONS`/`FOREGROUND_SERVICE*` (`AndroidManifest.xml:11`)

## Phase 4 — UX Polish (1.4.0-phase4)

- **Design** — Vibrant consolidated (`Color.kt:1` `VibrantPrimaryDark` etc), `Theme.kt:14` dark tonal mapping, `ui/components/ResalaComponents.kt:1` `E2EEBanner/SecureAvatar/SectionLabel`
- **Adaptive** — `isWideScreen >=600` WindowSizeClass medium, `VerticalDivider 1.dp outlineVariant` hairline (`MainActivity.kt:93`) + `HorizontalDivider` (`ChatListScreen.kt:238`)
- **Search FTS** — `searchQuery` normalized (`ّ` `ـ`) filters `peerName/lastMessage/phone/bio` (`ChatListScreen.kt:49`), `ChatViewModel` `searchQuery` + `filteredChats` `combine` (`ChatViewModel.kt:32`) for future FTS5
- **A11y + RTL** — logo `شعار رِسالة`, `ChatRowItem` TalkBack `semantics` (`ChatListScreen.kt:361`), empty `حالة فارغة` + CTA `empty_state_add_button` (`ChatListScreen.kt:241`), `ResalaScreenshotTest` light/dark/RTL (`LayoutDirection.Rtl`)
- **Onboarding** — `ui/OnboardingScreen.kt:1` phone/OTP `AuthManager`, 4 permission cards `POST_NOTIFICATIONS/RECORD_AUDIO/CAMERA/LOCATION`, `NavHost` `onboarding` startDestination if `!isOnboarded` (`MainActivity.kt:68,128`)

## Phase 5 — Quality (1.5.0)

- Tests, CI, release — see `TESTING.md`, `DEPLOYMENT.md`, `store/README.md`
- **Backlog:** Groups (`Chat.isGroup`), disappearing messages, Compose Multiplatform, Gemini smart reply (opt-in, never plaintext server), FCM `data` ciphertext push (`ROADMAP.md:131`)
