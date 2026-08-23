# Resala Chat — Documentation

> **رِسالة** — تطبيق تراسل فوري مشفّر، عربي أولاً، محلي أولاً (offline-first).

This `docs/` folder is the source of truth after `ROADMAP.md`. Start here, then dive into specifics.

## Quick Links

| Doc | Purpose |
|-----|---------|
| [`ARCHITECTURE.md`](ARCHITECTURE.md) | Hilt + Room + Navigation + Firestore + Signal/WebRTC layering, module graph |
| [`SECURITY.md`](SECURITY.md) | Threat model, crypto (AES-GCM → Signal), at-rest, Firestore rules |
| [`SETUP.md`](SETUP.md) | Local build, env, Firebase emulator, `google-services.json` |
| [`FEATURES.md`](FEATURES.md) | User-facing features per phase |
| [`TESTING.md`](TESTING.md) | Unit / Room / UI / E2E matrix, Roborazzi |
| [`DEPLOYMENT.md`](DEPLOYMENT.md) | CI, signing, Play Console, store assets |
| [`../ROADMAP.md`](../ROADMAP.md) | 5-phase plan with checked tasks |
| [`store/README.md`](store/README.md) | Play Store screenshots & checklist |

## Current Status

- **Version:** `1.5.0` (`versionCode 7`) — Phase 5 DONE — `app/build.gradle.kts:18`
- **Package:** `namespace com.example`, `applicationId com.aistudio.resalachat.vjqkzp` (migration to `com.resala.chat` planned 1.6)
- **SDK:** `compileSdk 36`, `minSdk 24`, `targetSdk 36` — `app/build.gradle.kts:11`
- **Stack:** Kotlin 2.2.10, AGP 9.1.1, Compose BOM 2025.09.00, Room 2.7.0, Hilt 2.52, Paging 3.3.6, Firebase BOM 34.12.0, Signal 2.8.2, WebRTC 1.1.0 — `gradle/libs.versions.toml:1`

## One-Paragraph Architecture

Offline-first Room (`resala_secure_db` v2) is source of truth. Hilt provides `AppDatabase`/`ChatRepository` (`di/AppModule.kt:1`). `ChatViewModel` (`ui/ChatViewModel.kt:1`) exposes `Flow`s + `SavedStateHandle` chat routing. UI is Compose Material3 with `NavHost` (`MainActivity.kt:128`) + wide `360dp` split. Firestore (`data/remote/FirestoreSyncManager.kt:1`) pushes `E2EE⚡Signal::` ciphertext and `observeChat` inserts remote — WorkManager (`workers/SyncWorker.kt:1`) retries. Signal (`util/SignalManager.kt:1`) wraps libsignal with AES-GCM fallback. Media via `EncryptedFile` (`util/MediaManager.kt:1`), calls via `WebRtcManager` (`util/WebRtcManager.kt:1`).

## How to Run (TL;DR)

```bash
# Android Studio Hedgehog+ (AGP 9.1.1)
git clone <repo> && cd Resala-Chat
cp .env.example .env   # only if Gemini needed (Phase 3+ smart reply)
# add app/google-services.json from Firebase console (or run local-only)
./gradlew :app:assembleDebug
# optional emulator backend:
# firebase emulators:start --only firestore
```

See [`SETUP.md`](SETUP.md) for full steps.

## Feature Demo (Phase 0–5)

- **Phase 0:** AES-256-GCM + `EncryptedSharedPreferences` — `util/EncryptionEngine.kt:22`
- **Phase 1:** Hilt + `NavHost` + `withTransaction` + Paging — `di/AppModule.kt:1`, `data/ChatRepository.kt:38`
- **Phase 2:** Firestore ciphertext sync + `AuthManager` phone/anonymous — `data/remote/`
- **Phase 3:** Signal + `EncryptedFile` + WebRTC + `FusedLocationProvider` — `util/`
- **Phase 4:** Vibrant dark, `WindowSizeClass` adaptive, FTS, `OnboardingScreen.kt:1`
- **Phase 5:** 7+3+2 tests + CI `/.github/workflows/ci.yml:1` + `1.5.0` release

## Conventions

- **Security > Features** — no feature weakens E2EE (`ROADMAP.md:11`)
- **Arabic-first** — `supportsRtl=true` (`AndroidManifest.xml:19`), `ar` screenshots (`ResalaScreenshotTest.kt:42`)
- **No fake crypto** — UI shows Signal only when `SignalManager.isSignalSessionEstablished()` true (`ui/ChatDetailScreen.kt:800`)
