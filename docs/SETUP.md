# Setup — Resala Chat

## Prerequisites

- **Android Studio** Hedgehog+ (AGP 9.1.1, Kotlin 2.2.10) — `gradle/libs.versions.toml:1`
- **JDK 17** (Temurin) — `../.github/workflows/ci.yml:12`
- Device/emulator **API 24+** (target `36`, `compileSdk 36` — `app/build.gradle.kts:12`)

## Quick Start (Local-Only, No Firebase)

```bash
git clone <repo> && cd Resala-Chat
# secrets are optional for local mode (Phase 0–1 mock)
cp .env.example .env   # GEMINI_API_KEY=MY_GEMINI_API_KEY is placeholder (GeminiConfig handles)
# No google-services.json needed — local-only fallback via AuthManager.local_*
./gradlew :app:assembleDebug
./gradlew :app:installDebug   # or Run in Android Studio
```

App seeds 4 users + 2 chats via `AppDatabase.seedCallback` (`data/AppDatabase.kt:35`) and works offline with mock bot (`ui/ChatViewModel.kt:321` only when `!isFirebaseConfigured`).

## Firebase Emulator (Phase 2 Real-Time)

1. Install `firebase-tools` (`npm i -g firebase-tools`), `firebase login`
2. Create Firebase project `resala-chat-placeholder`, download `google-services.json` → `app/google-services.json` (ignored, see `app/google-services.json.example`)
3. Enable **Auth** (Phone + Anonymous) and **Firestore** (Native mode)
4. Deploy rules: `firebase deploy --only firestore:rules --project <id> -f firestore.rules:1`
5. Emulator:
```bash
firebase emulators:start --only firestore,auth
# In app, uncomment in FirestoreEmulatorTest: Firestore.getInstance().useEmulator("10.0.2.2",8080)
./gradlew :app:assembleDebug
```
Two emulators on `10.0.2.2:8080` exchange ciphertext via `data/remote/FirestoreSyncManager.kt:1` (`pushMessage`/`observeChat`), offline persistence survives airplane toggle.

## Secrets & Signing

- **`.env`** — `GEMINI_API_KEY` reserved for Phase 3+ smart reply, handled by `util/GeminiConfig.kt:1` (null if placeholder). `secrets { propertiesFileName=.env default=.env.example }` at `app/build.gradle.kts:82`.
- **Signing** — `app/build.gradle.kts:24` `signingConfigs.release` uses `KEYSTORE_PATH/STORE_PASSWORD/KEY_ALIAS/KEY_PASSWORD` env if file exists, else unsigned (CI injects). `debug.keystore` auto-generated at `~/.android/` (not committed, `.gitignore:18`).
- **Decode debug keystore in CI** — `DEBUG_KEYSTORE_B64` secret → `base64 -d > debug.keystore` (`.github/workflows/ci.yml:22`)

## Build Variants

- `debug` — `isMinifyEnabled false`, `debugConfig` if `debug.keystore` exists else AGP default (`app/build.gradle.kts:61`)
- `release` — `isMinifyEnabled true` + `isShrinkResources true` + `proguard-android-optimize.txt` (`app/build.gradle.kts:54`)

```bash
./gradlew :app:assembleRelease   # needs KEYSTORE_PATH etc or unsigned
```

## Running Tests

```bash
./gradlew :app:testDebugUnitTest              # Robolectric: EncryptionEngine, Daos, ViewModel
./gradlew :app:recordRoborazziDebug           # generate screenshots to app/src/test/screenshots/
./gradlew :app:verifyRoborazziDebug           # diff <1% Phase 4 KPI
./gradlew :app:lintDebug                      # 0 error Phase 4 KPI
./gradlew :app:connectedAndroidTest           # FirestoreEmulatorTest needs emulator
```

See `TESTING.md`.

## Troubleshooting

- **Room `fallbackToDestructiveMigration` wipe** — removed Phase 0; use `MIGRATION_1_2` (`data/AppDatabase.kt:26`)
- **`debug.keystore` missing** — AGP generates `~/.android/debug.keystore`; our `debugConfig` is optional (`app/build.gradle.kts:39`)
- **Base64 in tests** — `android.util.Base64` needs Robolectric `@Config(sdk=[36])` (`util/EncryptionEngineTest.kt:9`)
- **No `google-services.json`** — app runs local-only (`AuthManager.isFirebaseConfigured()==false` → `FirestoreSyncManager` no-op) (`data/remote/AuthManager.kt:32`)
