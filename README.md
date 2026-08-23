<div align="center">
<img width="1200" height="475" alt="Resala Chat Banner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# رِسالة — Resala Chat

> تطبيق تراسل فوري مشفّر، عربي أولاً، محلي أولاً — `1.5.0` Phase 5 DONE.

[![CI](https://github.com/anomalyco/opencode/actions/workflows/ci.yml/badge.svg)](.github/workflows/ci.yml) [![Kotlin 2.2.10](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF)]() [![Compose BOM 2025.09.00](https://img.shields.io/badge/Compose%20BOM-2025.09.00-4285F4)]()

View in AI Studio: https://ai.studio/apps/ffc5bd5c-dbd7-4e46-bdea-2f16f55d1529

## Documentation

| Doc | Description |
|-----|-------------|
| [`docs/README.md`](docs/README.md) | Overview & quick start |
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | Hilt/Room/NavHost/Firestore/Signal layering |
| [`docs/SECURITY.md`](docs/SECURITY.md) | Threat model, GCM→Signal, Firestore rules |
| [`docs/SETUP.md`](docs/SETUP.md) | Local, Firebase emulator, secrets |
| [`docs/FEATURES.md`](docs/FEATURES.md) | Feature matrix by phase |
| [`docs/TESTING.md`](docs/TESTING.md) | Matrix (7+5+2+3 tests), Roborazzi |
| [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md) | CI, signing, Play Console |
| [`ROADMAP.md`](ROADMAP.md) | 5-phase checked plan |
| [`docs/store/README.md`](docs/store/README.md) | Screenshots & store assets |

## Run Locally (30s)

**Prereqs:** Android Studio Hedgehog+ (AGP 9.1.1), JDK 17.

```bash
git clone <repo> && cd Resala-Chat
cp .env.example .env   # placeholder OK (Gemini reserved)
# no google-services.json needed for local-only (Firestore fallback)
./gradlew :app:assembleDebug
./gradlew :app:installDebug # or Run in IDE
```

Emulator API 24+ (36 recommended). Seeds 4 contacts via `AppDatabase.seedCallback` (`data/AppDatabase.kt:35`).

**With Firebase (real-time):** see `docs/SETUP.md` — enable Auth/Firestore, `app/google-services.json`, `firestore.rules:1`.

## Features (Phases 0–5)

- **E2EE:** AES-256-GCM (`E2EE🛡️::`) → Signal `E2EE⚡Signal::` (`util/SignalManager.kt:1`) + `EncryptedFile` (`util/MediaManager.kt:1`)
- **Realtime:** Firestore ciphertext sync + `WorkManager` retry (`data/remote/FirestoreSyncManager.kt:1`)
- **Calls:** WebRTC `PeerConnection` via `WebRtcManager` (`util/WebRtcManager.kt:1`) — DTLS-SRTP or simulated
- **UX:** Hilt + `NavHost` (`MainActivity.kt:128`), Paging 3 (`data/Daos.kt:46`), Vibrant dark, `WindowSizeClass` adaptive, FTS search, `OnboardingScreen` (`ui/OnboardingScreen.kt:1`), RTL screenshots (`ResalaScreenshotTest.kt:1`)

Full matrix: [`docs/FEATURES.md`](docs/FEATURES.md).

## Tests

```bash
./gradlew :app:testDebugUnitTest       # EncryptionEngine 7, Daos 5, ViewModel 2, etc.
./gradlew :app:verifyRoborazziDebug    # light/dark/RTL diff <1%
./gradlew :app:lintDebug               # 0 error
```

See [`docs/TESTING.md`](docs/TESTING.md).

## CI & Release

- **CI:** `.github/workflows/ci.yml:1` — `assembleDebug` + `test` + `roborazzi` + `lint` + artifacts (`KEYSTORE_*` env guard)
- **Release:** `versionCode 7` `versionName 1.5.0` (`app/build.gradle.kts:18`), R8, Play Console backup excludes + `targetSdk 36` — [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md)

## Project Structure

```
app/src/main/java/com/example/
  data/ (User, Chat, Message, Daos, AppDatabase, ChatRepository)
  data/remote/ (AuthManager, FirestoreSyncManager)
  workers/ (SyncWorker)
  util/ (EncryptionEngine, SecureStorage, GeminiConfig, SignalManager, MediaManager, WebRtcManager)
  ui/ (MainActivity, ChatViewModel, ChatListScreen, ChatDetailScreen, CallingScreen, OnboardingScreen, theme/)
  ui/components/ (ResalaComponents)
  ResalaApplication (Hilt)
```

## Security

Ciphertext-only on wire & Firestore, `EncryptedSharedPreferences` + `EncryptedFile`, Firestore rules `senderUid==auth.uid`, `POST_NOTIFICATIONS`/`CAMERA` permission rationale — [`docs/SECURITY.md`](docs/SECURITY.md).

---

*Built across 5 phases (0 hardening → 5 release) — see `ROADMAP.md` for checked tasks.*
