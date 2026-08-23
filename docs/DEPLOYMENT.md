# Deployment — Resala Chat

## CI — `.github/workflows/ci.yml:1`

```yaml
on: push [main], pull_request [main]
jobs:
  build-and-test:
    runs-on: ubuntu-latest
    steps:
      checkout → setup-java 17 (cache gradle) → setup-android
      cache: gradle/libs.versions.toml + app/build.gradle.kts
      decode debug keystore: DEBUG_KEYSTORE_B64 → debug.keystore
      chmod +x gradlew || gradle
      assembleDebug (env KEYSTORE_PATH/STORE_PASSWORD/KEY_ALIAS/KEY_PASSWORD with exists() guard app/build.gradle.kts:25)
      testDebugUnitTest
      record/verifyRoborazziDebug (continue-on-error)
      lintDebug → upload test-reports + debug-apk artifacts
```

- **Trigger:** push/PR `main`
- **Java:** `temurin 17`, `cache: gradle`
- **Android SDK:** `android-actions/setup-android@v3`
- **Cache key:** `hashFiles('gradle/libs.versions.toml','app/build.gradle.kts')`
- **Artifacts:** `app/build/reports/tests/`, `lint-results-*.html`, `screenshots/`, `apk/debug/*.apk`

## Signing

- **Debug:** auto `~/.android/debug.keystore` (not committed, `.gitignore:18`), `debugConfig` optional if file exists (`app/build.gradle.kts:39`)
- **Release:** `signingConfigs.release` at `app/build.gradle.kts:24`:
  ```kotlin
  val ksFile = file(System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks")
  if (ksFile.exists()) { storeFile=ksFile; storePassword=System.getenv("STORE_PASSWORD"); keyAlias=System.getenv("KEY_ALIAS")?: "upload" }
  ```
  CI injects `KEYSTORE_PATH/STORE_PASSWORD/KEY_ALIAS/KEY_PASSWORD` via `secrets`; local unsigned if missing (guard `storeFile?.exists()==true` at `app/build.gradle.kts:58`)

- **R8:** `isMinifyEnabled true` + `isShrinkResources true` + `proguard-android-optimize.txt` (`app/build.gradle.kts:54`), keep `room/security/hilt/firebase/signal/webrtc/camera` at `app/proguard-rules.pro:23`

## Versioning

- `versionCode 7`, `versionName 1.5.0` — `app/build.gradle.kts:18` (was `1.4.0-phase4`)
- `compileSdk 36`, `minSdk 24`, `targetSdk 36`
- **Namespace kept `com.example`** for 1.5.0 to avoid `R` breakage; migration `com.example → com.resala.chat` + `applicationId com.aistudio.resalachat.vjqkzp → com.resala.chat` planned 1.6 with signing key rotation & user migration guide (Phase 5.3 note in `ROADMAP.md:126`)

## Play Console — `docs/store/README.md`

- `backup_rules.xml:6` / `data_extraction_rules.xml:6` exclude `resala_secure_db*` + prefs
- `targetSdk 36` compliance
- **Privacy policy:** placeholder (add URL)
- **Content rating:** Communication
- **Data safety:** Encryption in transit (TLS) + at rest (Room `EncryptedFile`/`SecureStorage`), **no data sharing**, `contentCipher` ciphertext-only on Firestore (`firestore.rules:1`)

## Store Assets

- **Screenshots** Roborazzi `app/src/test/screenshots/resala_chatlist_light/dark/rtl.png` (`ResalaScreenshotTest.kt:1`) + `greeting.png` baseline
- **Feature graphic** 1024×500 Vibrant `#005FB0` shield + Arabic tagline (replace `GHBanner` at `README.md:2`)
- Generate: `./gradlew :app:recordRoborazziDebug` then copy to `docs/store/`

## How to Release

```bash
# 1. Bump versionCode/versionName at app/build.gradle.kts:18
# 2. ./gradlew :app:assembleRelease   # needs KEYSTORE_PATH etc
# 3. ./gradlew :app:bundleRelease
# 4. Upload AAB to Play Console closed track (100 users, crash-free >99.5% M5 KPI)
# 5. Promote to production after Wireshark ciphertext-only + 2-device 800ms p95 verified
```

## Secrets

- `.env` → `GEMINI_API_KEY` placeholder, `GeminiConfig` handles null (`util/GeminiConfig.kt:1`)
- `google-services.json` ignored (`.gitignore:18`), example at `app/google-services.json.example`
- `debug.keystore*` / `*.jks` / `*.p12` ignored
