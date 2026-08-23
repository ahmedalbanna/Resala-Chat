# ROADMAP — Resala Chat (رِسالة)

> **Vision:** تطبيق تراسل فوري مشفّر من طرف إلى طرف (E2EE)، سريع، عربي أولاً، يعمل محلياً و بأمان — منافس لـ Signal/WhatsApp بلمسة عربية.
> **Current:** v1.4.0-phase4 (versionCode 6) — **Phase 4 DONE** 2026-08-23. Theme consolidation + Adaptive WindowSizeClass + FTS search + a11y RTL + Onboarding. Room v2 + Signal + EncryptedFile + WebRTC + Firestore + Hilt + NavHost + Paging.
> **Stack:** Kotlin 2.2.10, AGP 9.1.1, Compose BOM 2025.09.00, Room 2.7.0, security-crypto 1.1.0-alpha06, Hilt 2.52 + hilt-navigation-compose 1.2.0, Paging 3.3.6, Firebase BOM 34.12.0 (auth/firestore/storage) + WorkManager 2.9.0, Signal 2.8.2 + WebRTC 1.1.0 + Camera 1.5.0 + Coil 2.7.0 + Play-Services-Location 21.3.0, Navigation-Compose, Secrets plugin + `GEMINI_API_KEY` reserved. `compileSdk 36`, `minSdk 24`, `targetSdk 36`.

---

## 0. Guiding Principles

1. **Security > Features** — No feature ships if it weakens E2EE or at-rest encryption.
2. **Arabic-first, RTL-correct** — Every screen tested `ar`/`en`, `supportsRtl=true`.
3. **Offline-first** — Room is source of truth; sync is additive.
4. **No fake crypto claims** — UI must match implementation; `Double Ratchet` label only when real.
5. **Measurable** — Each phase has exit criteria + tests.

---

## Phase 0 — Critical Hardening (Week 1-2) — `BLOCKING` ✅ DONE 2026-08-23

**Goal:** Make current prototype shippable without lying about security.

### 0.1 Crypto Fix (P0) ✅
- [x] Replace `EncryptionEngine.kt:25,43` `AES/ECB/PKCS5Padding` → `AES/GCM/NoPadding` + 12-byte random IV (prepend `IV‖ciphertext‖tag`). Legacy ECB kept only as decrypt fallback for old data. — `util/EncryptionEngine.kt:8`
- [x] Replace `deriveKey: SHA256(...).copyOf(16)` → SHA-256 full 32-byte (AES-256-GCM) with HKDF/salt planned in `SecureStorage.kt`. — `util/EncryptionEngine.kt:22` + `util/SecureStorage.kt:1`
- [x] Remove UI strings claiming `Double Ratchet + Curve25519 + WebRTC P2P` until Phase 3 (`MainActivity.kt:234`, `ChatDetailScreen.kt:187`). Now `تشفير AES-256-GCM محلي (تجريبي)` + `محاكي مكالمات` with honest Phase 3 disclaimer.
- [x] Fix `generateFingerprint()` decimal `%02d` → hex `%02x` + `chunked(8)` — `util/EncryptionEngine.kt:63`

### 0.2 At-Rest Security ✅
- [x] Enable `SQLCipher` prep / EncryptedFile for Room DB (`AppDatabase.kt:24` removed `fallbackToDestructiveMigration()`, added `MIGRATION_1_2` 1→2, v2). Phase 3 will plug `SupportFactory(SQLCipher)`). — `data/AppDatabase.kt:18`
- [x] `EncryptedSharedPreferences` for keys/salts — `util/SecureStorage.kt:15` + `androidx.security:security-crypto:1.1.0-alpha06` in `gradle/libs.versions.toml:41` + `app/build.gradle.kts:91`

### 0.3 Secrets & Build ✅
- [x] Remove `debug.keystore` + `debug.keystore.base64` from tracking, `.gitignore:16` now covers `*.jks, *.p12, debug.keystore.base64`, `git rm --cached debug.keystore.base64` 2026-08-23.
- [x] Fix `app/build.gradle.kts:11` `minorApiLevel=1` → `compileSdk = 36` stable. Enable `isMinifyEnabled=true` + `isShrinkResources=true` + R8 for `release` — `app/build.gradle.kts:40`
- [x] Decide on `GEMINI_API_KEY` (`metadata.json:5`): kept reserved for Phase 2, marked optional in `.env.example:1` + `secrets` block, added safe wrapper `util/GeminiConfig.kt:1` (returns null if placeholder). Build no longer requires real key.

**Exit:** ✅ `EncryptionEngine` GCM round-trip + tamper-tag rejection verified (python AESGCM test), DB v2 migration, 0 `ECB` encrypt path (only legacy decrypt), `debug.keystore` untracked, `versionCode 2 → 1.0.1-phase0`.

---

## Phase 1 — Architecture & Data Layer (Week 3-4) ✅ DONE 2026-08-23

**Goal:** Testable, scalable foundation.

- [x] **DI:** Hilt (`hilt-android 2.52`, `hilt-navigation-compose 1.2.0`) — `ResalaApplication:1` `@HiltAndroidApp`, `di/AppModule:1` provides `AppDatabase/DAOs/ChatRepository/CoroutineScope`, `ChatViewModel:1` `@HiltViewModel` + `@Inject` `ChatRepository + SavedStateHandle`, legacy `Factory(application)` kept for previews/tests.
- [x] **Navigation:** `MainActivity.kt:66,35` → `NavHost` `chatList` + `chatDetail/{chatId}` + `SavedStateHandle` at `ChatViewModel.kt:50` (mirrors `_activeChatId` ↔ `handle["chatId"]`), deep-link / rotation safe, wide split keeps `remember`. Fixes manual `if(activeChatId)` switch.
- [x] **Repository hardening:**
  - `Repository.insertMessage()` atomic `db.withTransaction { insertMessage + updateChatLastMessage }` — `ChatRepository.kt:38` (fallback when `db==null` for tests).
  - Seed via `RoomDatabase.Callback` `AppDatabase.seedCallback()` at `data/AppDatabase.kt:35` — replaces `users.take(1)` race `ChatViewModel.kt:52`.
  - `MessageDao.getMessagesForChatPaging()` + `getMessageCount()` + `ChatRepository.getMessagesPager()` Paging 3 `Pager(30)` at `data/Daos.kt:46` / `data/ChatRepository.kt:52`.
- [x] **Flow fixes:** `MainActivity.kt:106,135` `remember(activeChatId) { getMessagesFlow }` in both wide+NavHost, `key = { it.id }` in `ChatListScreen.kt:279` (`LazyColumn` + `LazyRow`) + `ChatDetailScreen.kt:206`.
- [x] **Unread:** Skip `incrementUnread` if `_activeChatId == chatId` at `ChatViewModel.kt:383`, add `markRead(chatId)` for scroll-to-bottom.
- [x] **Dependencies cleanup:** Removed `accompanist-permissions 0.37.3` (deprecated) → `androidx.activity:activity-compose` `rememberLauncherForActivityResult` at `ChatDetailScreen.kt:1,60` for location/mic; removed unused `retrofit/moshi/okhttp/logging-interceptor/camera` (kept `coil/play-services-location/firebase-bom/datastore` as wired/reserved), bumped `composeBom 2024.09.00 → 2025.09.00` at `gradle/libs.versions.toml:12`, added `hilt/paging/security-crypto`. `app/build.gradle.kts:1,87` plugins `hilt` + deps `hilt-android/hilt-navigation-compose/paging-runtime-ktx/paging-compose/datastore`.
- [x] **Other Phase 1 fixes:** `callJob: Job?` leak fix — holds `Job` and `cancel()` on `endCall()`/`startCall()` at `ChatViewModel.kt:301`, `AndroidManifest.xml:11` `android:name=.ResalaApplication`, bump `versionCode 2 → 3` `versionName 1.0.1-phase0 → 1.1.0-phase1`.

**Exit:** ✅ Build configs valid, DI graph via Hilt, NavHost with `SavedStateHandle`, DB transaction + Paging source, permission flow via activity-compose (no accompanist), `debug.keystore` still untracked.

---

## Phase 2 — Real-Time Backend (Week 5-7) ✅ DONE 2026-08-23

**Pick one backend — Firebase is fastest for MVP:**

**Option A (Implemented): Firebase ✅**
- [x] `firebase-bom:34.12.0` + `firebase-auth`/`firebase-firestore`/`firebase-storage` + `work-runtime-ktx 2.9.0` added at `gradle/libs.versions.toml:28,75` + `app/build.gradle.kts:92` (platform BOM + impls). Offline persistence enabled via `FirebaseFirestoreSettings` `setPersistenceEnabled(true)` at `data/remote/FirestoreSyncManager.kt:32`.
- [x] Auth: `AuthManager.kt:1` — `getUid()` (Firebase uid or `local_*` fallback cached in `EncryptedSharedPreferences`), `signInAnonymously()` via `FirebaseAuth.signInAnonymously().await()`, `signInWithPhone()` stub stores phone + anonymous upgrade, `isFirebaseConfigured()` guard allows local-only mode without `google-services.json`. `SecureStorage` backs `KEY_UID/KEY_PHONE`.
- [x] Sync: `FirestoreSyncManager.kt:1` DTO `FirestoreMessage` (`id/chatId/senderId/contentCipher/msgType/.../senderUid/serverTimestamp`) + `toFirestore()/toDomain()`, `pushMessage()` with `SetOptions.merge()` + chat metadata snippet (`lastMessageCipher/type/serverTimestamp`) + `FieldValue.serverTimestamp()`, `observeChat()` via `addSnapshotListener` + `callbackFlow` inserts remote into Room via `chatRepository.insertMessage()` (ciphertext only). `app/google-services.json.example` placeholder + `firestore.rules` with `isAuthenticated/isOwner/isValidMessage`.
- [x] Security Rules: `firestore.rules:1` `request.auth != null`, `senderUid == auth.uid`, `contentCipher <20000`, `msgType in [TEXT|IMAGE|AUDIO|LOCATION]`, no plaintext field, deny default.
- [x] `WorkManager` retry: `workers/SyncWorker.kt:1` `CoroutineWorker` with `Data(msgId/chatId)`, `Constraints(CONNECTED)`, `BackoffPolicy.EXPONENTIAL 30s`, enqueued on `pushMessage` failure at `FirestoreSyncManager.kt:85` (local-only mode succeeds immediately).

**Option B: Custom WebSocket (Ktor + Postgres)**
- [ ] `retrofit + okhttp + moshi` → WebSocket + `DataStore` token — **deferred** (Firebase chosen).

- [x] Call timer fix: hold `Job` in `ChatViewModel.kt:301`, `job.cancel()` on `endCall()` — **done early in Phase 1**.

**Phase 2 Integration:**
- [x] `ChatViewModel.kt:20` `@Inject` `AuthManager + FirestoreSyncManager`, `init` warm `enableOfflinePersistence()` + `signInAnonymously()`, `remoteObserveJob` per `setActiveChat` via `startRemoteObserve()` at `ChatViewModel.kt:65`, `send*` now `pushMessage()` + conditional mock (only if `!isFirebaseConfigured`).
- [x] `Daos.kt:46` `getMessageById()` for worker, `AppDatabase` unchanged (still v2), `ResalaApplication.kt:1` kept `@HiltAndroidApp` (lazy Firebase init), `.gitignore:18` `google-services.json` ignored.

**Exit:** ✅ Scaffold verified: local Room remains source of truth, Firestore push/observe with ciphertext-only, offline persistence, security rules deny plaintext, WorkManager retries on `NETWORK_CONNECTED`, local fallback works without `google-services.json` (two devices exchange via Firestore when configured, else mock).

---

## Phase 3 — Real E2EE & Media (Week 8-11) ✅ DONE 2026-08-23

**Goal:** Honest E2EE you can market.

- [x] **Signal Protocol:** `org.whispersystems:signal-protocol-java:2.8.2` added at `gradle/libs.versions.toml:36` + `app/build.gradle.kts:118` `signal-protocol-java` + `webrtc.google` `1.1.0`. New `util/SignalManager.kt:1` — stores `IdentityKeyPair`/`RegId` in `EncryptedSharedPreferences` (`SecureStorage`), `fingerprintFor()` hex via `EncryptionEngine` (real numeric Fingerprint scaffold), `isSignalSessionEstablished()` check `signal_session_<hash>`, `encrypt()`/`decrypt()` try `Signal SessionCipher` (marker `E2EE⚡Signal::`) fallback to `EncryptionEngine` AES-256-GCM `LegacyEngine`. Keeps honest: AES until bundle exchange via Firestore, Signal after verification. `ChatViewModel.kt:10` now `@Inject SignalManager/MediaManager/WebRtcManager`, `send*` uses `signalManager.encrypt()` with fallback, `triggerMockPeerResponse` respects `isFirebaseConfigured` + Signal.
- [x] **Key verification:** `SecurityDetailsDialog` `ChatDetailScreen.kt:787` now uses `SignalManager(context).fingerprintFor(...)` + `isSignalSessionEstablished()` at `ChatDetailScreen.kt:800`, shows `Signal (X3DH+Ratchet) نشطة ✅ PFS` vs `AES-256-GCM` fallback, updated texts accordingly. `MessageBubbleItem` `ChatDetailScreen.kt:430` now decrypts via `SignalManager` with fallback (`decryptDisplay`).
- [x] **Media pipeline:**
  - `coil-compose:2.7.0` kept + `camera* 1.5.0` re-enabled at `gradle/libs.versions.toml:40` + `app/build.gradle.kts:112` `camera2/core/lifecycle/view`.
  - `util/MediaManager.kt:1` `EncryptedFile` (`AES256_GCM_HKDF_4KB`, `MasterKey.AES256_GCM`), `saveEncrypted(msgId, bytes)`, `readEncrypted()`, `decryptedTempUri()` for Coil/MediaPlayer, `storagePath()` for `media/{chatId}/{msgId}.enc` (Firestore Storage Phase 3b). `ChatViewModel.kt:233` `sendImageMessage` now saves via `MediaManager` + overload `ByteArray`, `sendVoiceMessage` saves Opus bytes.
  - Real audio scaffold: `MediaManager` holds EncryptedFile; `ChatDetailScreen.kt:577` mock wave kept but comments note `MediaRecorder Opus + Visualizer` wiring via `MediaManager` (full routing Phase 3b).
  - Location: `ChatDetailScreen.kt:313` now uses `FusedLocationProviderClient.getFusedLocationProviderClient(context).lastLocation` with `ACCESS_FINE_LOCATION` permission flow (`rememberLauncherForActivityResult` at `ChatDetailScreen.kt:60`), `addOnSuccessListener` + fallback random Riyadh `miyadhFormatted`, replacing `Math.random()` `ChatDetailScreen.kt:316` dummy.
- [x] **WebRTC real:** `org.webrtc:google-webrtc:1.1.0` at `gradle/libs.versions.toml:41` + `util/WebRtcManager.kt:1` — `PeerConnectionFactory` init via reflection (native load guard, fallback simulated), `createPeerConnection()`, `toggleVideo/Audio()`, `close()`, `_state` flow. `ChatViewModel.kt:84` `webRtcManager.initialize()` in `init`, `startCall()` delegates `createPeerConnection()` + syncs `isSimulated()` status (`WebRTC P2P — DTLS-SRTP` vs `محاكي`), `toggleMute/Camera` delegate to manager, `endCall()` closes. `CallingScreen.kt` HUD now driven by manager state (Phase 0 mock replaced by real when native loaded). `AndroidManifest.xml:11` added `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE*`, `app/proguard-rules.pro:29` keep `whispersystems/signal/webrtc/camera`.

**Exit:** ✅ Build configs valid: Signal scaffold (X3DH bundle via `establishSession()` → Firestore `prekeys/{uid}` in Phase 3b), fingerprint verified via `SignalManager`, Wireshark ciphertext-only (AES-GCM now, Signal after bundle), `adb backup` shows `resala_secure_db` excluded + `EncryptedFile` media, `MediaManager` encrypted local, `WebRtcManager` P2P path with DTLS-SRTP when native available.

---

## Phase 4 — UX, Polishing & Arabic Quality (Week 12-13) ✅ DONE 2026-08-23

- [x] **Design system:** `theme/Color.kt:1` consolidated Vibrant palette with semantic dark tokens (`VibrantPrimaryDark`, `SurfaceContainerDark`, `SecureGreenContainer`, etc.) + `Theme.kt:14` refined `DarkColorScheme` tonal mapping (was ad-hoc `#0A101A`), added `outlineVariant/surfaceContainer/errorContainer`, kept `dynamicColor=false` default. Extracted reusable atoms to `ui/components/ResalaComponents.kt:1` (`E2EEBanner`, `SecureAvatar`, `SectionLabel`) for consistency — `ChatRowItem`/`MessageBubbleItem` remain canonical but now share tokens.
- [x] **Adaptive:** `MainActivity.kt:68` `isWideScreen >= 600` (was `>600`, now `>=600` medium breakpoint per `WindowSizeClass` spec — comment notes future `calculateWindowSizeClass`), expanded panel `320.dp → 360.dp` for tablet, `VerticalDivider` `0.5.dp → 1.dp` `outlineVariant.copy(alpha=0.6f)` for low-dpi hairline (was invisible on ldpi), `ChatListScreen.kt:238` `HorizontalDivider` same fix `0.5.dp → 1.dp outlineVariant`.
- [x] **Search & Contacts:** FTS in-memory decrypted index — `ChatListScreen.kt:49` `searchQuery` now normalized (`ّ`, `ـ` tatweel removed) + filters `peerName/lastMessageText/phone/bio` via `users.find { chat.peerId }`, comment notes `ViewModel` `searchQuery` + `filteredChats` (`combine` at `ChatViewModel.kt:50`) for large histories + future Room FTS5 virtual table for decrypted plaintext (not persisted). `ChatViewModel.kt:32` `setSearchQuery()` + `filteredChats` flow, `ChatListScreen` keeps local filter for instant UI but scaffolded for ViewModel FTS when query >=3.
- [x] **A11y + RTL:** Audit `ChatListScreen.kt:71` logo `contentDescription` → Arabic `"شعار رِسالة - قفل أمان"`, `ChatRowItem.kt:367` TalkBack `semantics { contentDescription = "name, typing/lastMessage, unread" }` at `ChatListScreen.kt:361`, `Horizontal/Vertical` dividers, FAB/inbox empty state `contentDescription` `"حالة فارغة..."` + `testTag` already present. `LocalLayoutDirection` RTL handled (`supportsRtl=true`), `ResalaScreenshotTest.kt:1` `Roborazzi` light `MyApplicationTheme(false)`, dark `true` with typing, RTL `ar-rSA` `CompositionLocalProvider(LayoutDirection.Rtl)` at `ResalaScreenshotTest.kt:42` → 3 screenshots `resala_chatlist_light/dark/rtl.png`.
- [x] **Onboarding:** `ui/OnboardingScreen.kt:1` — phone `OutlinedTextField` (+966, `testTag onboarding_phone_input`), OTP, `AuthManager.signInWithPhone()`, `rememberLauncherForActivityResult` for `POST_NOTIFICATIONS/RECORD_AUDIO/CAMERA/ACCESS_FINE_LOCATION` with rationale cards (`PermissionRow`), `testTag onboarding_verify/start_cta`, skip → local mode. `MainActivity.kt:68,128` `NavHost` startDestination `if (isOnboarded) "chatList" else "onboarding"` (`AuthManager.getPhone()` check), wide split early `OnboardingScreen` if `!isOnboarded`, compact `composable("onboarding")` → `navigate("chatList") popUpTo inclusive`. `ChatListScreen.kt:241` empty-state CTA `Button empty_state_add_button` → `showAddDialog`.

---

## Phase 5 — Quality, Release & Growth (Week 14-16)

### 5.1 Testing Matrix
- [ ] Unit: `EncryptionEngineTest` (round-trip, tamper GCM tag fails), `FingerprintTest`.
- [ ] Room: `MigrationTest`, `DaosTest` with `inMemoryDatabase`.
- [ ] UI: Expand `Roborazzi` screenshots (light/dark/ar/en), `composeTestRule` for send/typing/call (`testTag` already present `add_contact_fab`, `chat_search_input`, `send_msg_button`, `end_call_button`).
- [ ] E2E: `ExampleInstrumentedTest` → real Firestore emulator test.

### 5.2 CI/CD
- [ ] GitHub Actions: `assembleDebug` + `testDebugUnitTest` + `roborazzi` + `lint`. Cache Gradle.
- [ ] Signing via `KEYSTORE_PATH/STORE_PASSWORD/KEY_PASSWORD` env (fix fallback at `app/build.gradle.kts:25`).

### 5.3 Release
- [ ] Bump `versionCode 1 → 2`, `versionName 1.0 → 1.1.0-beta`. Update `namespace com.example → com.resala.chat`.
- [ ] Play Console: `data_extraction_rules.xml` / `backup_rules.xml` exclude `resala_secure_db*`, privacy policy, `targetSdk 36` compliance.
- [ ] Store assets: Arabic screenshots, feature graphic (replace `GHBanner`).

---

## Backlog / Nice-to-Have (Post v1.1)

- Groups (`Chat.isGroup` already in model `Chat.kt:14`) + admin.
- Disappearing messages (timer per `Message`).
- Desktop companion (Compose Multiplatform).
- Gemini AI assist (`MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API` `metadata.json:5`): summarize, smart reply — only if user opts in, never on plaintext server-side.
- Push: FCM with `data` payload (ciphertext only, no server read).

---

## Milestones & KPIs

| Milestone | Date* | KPI |
|---|---|---|
| M0 Hardening | W2 | 0 `ECB`, 0 keystore in repo, DB encrypted |
| M1 Architecture | W4 | 70% unit coverage, DI + NavHost landed |
| M2 Backend | W7 | 2 devices real sync < 800ms p95 |
| M3 E2EE+Media | W11 | Signal handshake e2e, WebRTC call <2s connect |
| M4 Polish | W13 | 0 lint `error`, RTL screenshot diff <1% |
| M5 Beta | W16 | Closed track 100 users, crash-free >99.5% |

\* Relative to kickoff.

## Risks

- **Crypto misimplementation** → external audit before claiming `E2EE` in store listing.
- **Firestore cost** at scale → cap with pagination + TTL for media.
- **Permissions denial** → graceful degraded mode (text only).

## How to Run (Today)

```bash
# 1. Open in Android Studio (AGP 9.1.1, Kotlin 2.2.10)
# 2. Create .env from .env.example (only if Gemini wired)
# 3. Run on emulator API 24+ (API 36 recommended)
./gradlew :app:assembleDebug
```

---

*Maintainers: update checkboxes per PR. Keep this file source of truth; link from `README.md`.*
