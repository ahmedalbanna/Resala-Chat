# ROADMAP — Resala Chat (رِسالة)

> **Vision:** تطبيق تراسل فوري مشفّر من طرف إلى طرف (E2EE)، سريع، عربي أولاً، يعمل محلياً و بأمان — منافس لـ Signal/WhatsApp بلمسة عربية.
> **Current:** v1.0.1-phase0 (versionCode 2) — **Phase 0 DONE** 2026-08-23. Room v2 + AES-256-GCM (IV random) + EncryptedSharedPreferences + honest UI + hardened build. Prototype local-first.
> **Stack:** Kotlin 2.2.10, AGP 9.1.1, Compose BOM 2024.09.00, Room 2.7.0, security-crypto 1.1.0-alpha06, Navigation-Compose/Retrofit/Moshi declared (Phase 2), Secrets plugin + `GEMINI_API_KEY` reserved Phase 2. `compileSdk 36`, `minSdk 24`, `targetSdk 36`.

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

## Phase 1 — Architecture & Data Layer (Week 3-4)

**Goal:** Testable, scalable foundation.

- [ ] **DI:** Introduce Hilt (`hilt-android`, `hilt-navigation-compose`). Move `AppDatabase.getDatabase()` + `ChatRepository` to modules. Remove `ChatViewModel(application: Application)` manual factory `ChatViewModel.kt:371`.
- [ ] **Navigation:** Replace `activeChatId: String?` state (`MainActivity.kt:66`, `ChatViewModel.kt:35`) with `Navigation-Compose` `NavHost` (`chatList`, `chatDetail/{chatId}`, `call/{chatId}`) + `SavedStateHandle`. Fixes rotation/backstack, deep links.
- [ ] **Repository hardening:**
  - `Repository.insertMessage()` wrap in `@Transaction` (`updateChatLastMessage` + `insertMessage` atomic) — `ChatRepository.kt:33`.
  - Fix seed via `RoomDatabase.Callback` instead of `users.take(1)` race `ChatViewModel.kt:52`.
  - Add `MessageDao.getPagedMessages()` with Paging 3 for large histories.
- [ ] **Flow fixes:** `MainActivity.kt:106,135` `remember(activeChatId) { viewModel.getMessagesFlow(id) }`, add `key = { it.id }` to `LazyColumn` `ChatListScreen.kt:279` / `ChatDetailScreen.kt:206`.
- [ ] **Unread:** Skip `incrementUnread` if `activeChatId == chatId` `ChatViewModel.kt:367`; add `markRead` on scroll-to-bottom.
- [ ] **Dependencies cleanup:** Purge unused `coil/retrofit/moshi/okhttp/firebase-bom/camera/datastore` from `libs.versions.toml:21-39` or wire them. Update `composeBom` to 2025.x, remove deprecated `accompanist-permissions` → `androidx.activity:activity-compose` permissions.

**Exit:** `ChatRepositoryTest` + `ChatViewModelTest` (Turbine + `kotlinx-coroutines-test`) green; `HiltTest` passes.

---

## Phase 2 — Real-Time Backend (Week 5-7)

**Pick one backend — Firebase is fastest for MVP:**

**Option A (Recommended): Firebase**
- [ ] `firebase-bom:34.12.0` already declared — enable `firebase-auth` (phone), `firebase-firestore` (chats/messages) or `realtime-database`, `firebase-storage`.
- [ ] Auth: Phone OTP + anonymous upgrade; map `User.id` → `uid`.
- [ ] Sync: `Firestore` `messages/{chatId}/msgs` with `contentCipher` + `msgType`, `FieldValue.serverTimestamp()`. Offline persistence on.
- [ ] Security Rules: `request.auth != null && request.resource.data.senderId == request.auth.uid`.
- [ ] `WorkManager` for retry queue when offline.

**Option B: Custom WebSocket (Ktor + Postgres)**
- [ ] `retrofit + okhttp + moshi` (already in catalog) → WebSocket + `DataStore` token.

- [ ] Call timer fix: hold `Job` in `ChatViewModel.kt:295`, `job.cancel()` on `endCall()`.

**Exit:** Two physical devices exchange real ciphertext via Firestore, survive airplane-mode toggle.

---

## Phase 3 — Real E2EE & Media (Week 8-11)

**Goal:** Honest E2EE you can market.

- [ ] **Signal Protocol:** Integrate `libsignal-protocol-java` (X3DH + Double Ratchet). Store `IdentityKey`, `PreKeys`, `SignedPreKey` in encrypted DB. Remove `EncryptionEngine` sim or keep as fallback `LegacyEngine`.
- [ ] **Key verification:** QR + `SecurityDetailsDialog` `ChatDetailScreen.kt:710` compares real fingerprints.
- [ ] **Media pipeline:**
  - Enable `coil-compose:2.7.0` (already declared) + `camera*` deps (`app/build.gradle.kts:75-77` uncomment) for real picker/capture.
  - `firebase-storage` encrypted blobs; `localPath` → real `uri` with `EncryptedFile`.
  - Real audio: `MediaRecorder` (Opus) + waveform via `Visualizer`; fix `ChatDetailScreen.kt:539` mock timer.
  - Location: use `play-services-location:21.3.0` (declared) + runtime permission + `FusedLocationProviderClient`; replace `Math.random()` `ChatDetailScreen.kt:316`.
- [ ] **WebRTC real:** Integrate `org.webrtc:google-webrtc` for 1-1 voice/video; keep `CallingScreen.kt` HUD but wire real `PeerConnection`. Add `RECORD_AUDIO/CAMERA` runtime flow + `POST_NOTIFICATIONS` (Android 13+).

**Exit:** Wireshark shows only ciphertext; second device decrypts; `adb backup` yields encrypted DB.

---

## Phase 4 — UX, Polishing & Arabic Quality (Week 12-13)

- [ ] **Design system:** Consolidate `theme/Color.kt` / `Theme.kt` Vibrant palette, support dynamic dark (current `darkColorScheme` at `Theme.kt:14` is ad-hoc). Extract `ChatRowItem`, `MessageBubbleItem` reuse.
- [ ] **Adaptive:** Refine `isWideScreen >600dp` `MainActivity.kt:72` (use `WindowSizeClass`). Fix divider `0.5.dp` hairline on low-dpi.
- [ ] **Search & Contacts:** FTS for `messages.contentCipher` decrypted index (in-memory); `ChatListScreen.kt:50` filter currently scans `lastMessageText` only.
- [ ] **A11y + RTL:** Content descriptions audit (some already done `ChatListScreen.kt:92`), test with TalkBack, `android:layoutDirection` screenshots via `Roborazzi` (`GreetingScreenshotTest.kt`).
- [ ] **Onboarding:** Add phone-auth screen, permissions rationale, empty-state CTA.

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
