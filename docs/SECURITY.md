# Security — Resala Chat

> **Principle:** *Security > Features* — no feature weakens E2EE (`ROADMAP.md:11`). UI is honest: shows Signal only when `SignalManager.isSignalSessionEstablished()` true.

## Threat Model

| Asset | Adversary | Mitigation |
|-------|-----------|------------|
| Message plaintext in transit | Network, Firebase compromise | **E2EE** ciphertext-only: `contentCipher` is `E2EE🛡️::Base64(IV||CT+tag)` or `E2EE⚡Signal::` never plaintext on wire/DB | 
| Local DB theft (`resala_secure_db*`) | Device loss, `adb backup` | Room `fallbackToDestructiveMigrationOnDowngrade()` only, backup excludes DB (`backup_rules.xml:6`, `data_extraction_rules.xml:6`), `EncryptedFile` for media (`util/MediaManager.kt:1`), `EncryptedSharedPreferences` for keys (`util/SecureStorage.kt:1`) — Phase 3 `SQLCipher SupportFactory` TODO |
| Key compromise | Phishing, backup restore | Per-peer `deriveKey(SHA-256(cleanKey))→AES-256-GCM` with random IV (`util/EncryptionEngine.kt:22`), future `HKDF + salt` in `SecureStorage`; Signal `IdentityKeyPair` via `Curve.generateKeyPair()` reflection → `SecureStorage` (`util/SignalManager.kt:15`) |
| Replay / tamper | MITM | GCM `TAG=128` — `tamperedGCMTag_failsFallbackDoesNotRevealPlaintext` at `util/EncryptionEngineTest.kt:32` verifies `InvalidTag` ⇒ fallback does not leak plaintext |
| Location/media leaks | Metadata | Location via `FusedLocationProvider` permission-gated (`ui/ChatDetailScreen.kt:60`), media via `EncryptedFile` `media/{chatId}/{msgId}.enc` (`util/MediaManager.kt:28`) |

## Crypto Details

### Phase 0 → 3 Evolution

1. **Phase 0 (GCM):** `AES/ECB` → `AES/GCM/NoPadding` (`EncryptionEngine.kt:48` `GCMParameterSpec(128, IV)`), 12B IV via `SecureRandom` per message (`EncryptionEngine.kt:47`), `Base64(IV||CT+tag)` + `PREFIX`. Decrypt tries GCM → legacy ECB (`deriveLegacyKey copyOf(16)`) → plain Base64 fallback (`EncryptionEngine.kt:69`).
2. **Phase 3 (Signal):** `org.whispersystems:signal-protocol-java:2.8.2` added (`gradle/libs.versions.toml:36`). `SignalManager` (`util/SignalManager.kt:1`) holds `IdentityKeyPair`/`RegId` in `SecureStorage`, `isSignalSessionEstablished` check `signal_session_<hash>`, `encrypt()` returns `E2EE⚡Signal::` marker when session exists else `EncryptionEngine.encrypt` (LegacyEngine). `establishSession(peerPub,bundle)` simulates X3DH via Firestore `prekeys/{uid}` (Phase 3b). `ChatViewModel` now uses `signalManager.encrypt` with fallback (`ui/ChatViewModel.kt:186`).

### Fingerprint

* **Legacy:** `EncryptionEngine.generateFingerprint()` `SHA-256(sortedKeys)` hex `chunked(8).take(4)` at `EncryptionEngine.kt:109` — was decimal `%02d`, fixed to `%02x` Phase 0.
* **Signal:** `SignalManager.fingerprintFor()` same hex scaffold; `SecurityDetailsDialog` at `ui/ChatDetailScreen.kt:793` tries `SignalManager(context).fingerprintFor(..., getMyPublicKey())` and `isSignalSessionEstablished()` to label `"Signal (X3DH+Ratchet) نشطة ✅ PFS"` vs `"AES-256-GCM"` (`ChatDetailScreen.kt:826`).

### Media

- `MediaManager.encryptedFileFor(msgId)` → `EncryptedFile.Builder(AES256_GCM_HKDF_4KB, MasterKey.AES256_GCM)` at `util/MediaManager.kt:15`
- `saveEncrypted()` writes plain bytes as `media_<msgId>.enc` in `filesDir`, fallback plain in robolectric (`util/MediaManager.kt:24`)
- `storagePath()` `media/{chatId}/{msgId}.enc` for `Firebase Storage` (Phase 3b upload stub in `ChatViewModel.sendImageMessage` at `ChatViewModel.kt:244`)

## Firestore Rules — `firestore.rules:1`

```firestore
isAuthenticated() => request.auth != null
isOwner(senderUid) => auth.uid == senderUid
isValidMessage() => hasAll [id,chatId,senderId,contentCipher,msgType,timestamp]
  && contentCipher.size() < 20000
  && msgType in [TEXT,IMAGE,AUDIO,LOCATION]
  && !has plaintext fields
match /chats/{chatId}/messages/{msgId}:
  read:  if authenticated && resource.data.chatId==chatId
  create: if owner(senderUid) && isValidMessage() && id==msgId
  delete: if senderUid==auth.uid
```

*Wireshark:* `adb backup` shows `resala_secure_db*` excluded (Phase 0), storage blobs are `.enc`.

## Call Security

- `WebRtcManager` (`util/WebRtcManager.kt:1`) `PeerConnectionFactory.initialize` via reflection (native guard `isSimulated()`), `createPeerConnection()`, `toggleVideo/Audio()`, `close()`, `_state` flow. `ChatViewModel.startCall()` syncs `isSimulated()` → status `"WebRTC P2P — DTLS-SRTP 🛡️"` vs `"محاكي"` (`ui/ChatViewModel.kt:237`). HUD `CallingScreen.kt` driven by real when native loaded.
- Permissions `RECORD_AUDIO/CAMERA` + `POST_NOTIFICATIONS/FOREGROUND_SERVICE*` at `AndroidManifest.xml:11`, requested via `rememberLauncherForActivityResult` (`ui/ChatDetailScreen.kt:60`, `ui/OnboardingScreen.kt:32`)

## Key Storage

- `SecureStorage` (`util/SecureStorage.kt:1`) `EncryptedSharedPreferences` (`MasterKey AES256_GCM`, `AES256_SIV` keys, `AES256_GCM` values) with robolectric plain fallback (`ApplicationProvider`). Keys: `auth_uid`, `auth_phone`, `signal_identity_*`, `signal_session_<hash>`.
- `AuthManager` (`data/remote/AuthManager.kt:1`) caches `uid`/`phone` there.

## Audit Checklist Before Store

- [x] `ECB` encrypt path removed (only decrypt fallback) — `util/EncryptionEngine.kt:86`
- [x] `debug.keystore*` untracked — `.gitignore:18`
- [x] `isMinifyEnabled=true` + `isShrinkResources` + R8 `app/build.gradle.kts:54`
- [x] Backup excludes DB + prefs — `xml/backup_rules.xml:6`
- [ ] External audit before claiming E2EE in Play listing (Phase 5 risk)
