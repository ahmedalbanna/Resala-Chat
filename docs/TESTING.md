# Testing — Resala Chat

## Matrix (Phase 5.1)

| Layer | File | What | How |
|-------|------|------|-----|
| **Unit crypto** | `util/EncryptionEngineTest.kt:1` | 7 tests: round-trip Arabic/emoji, IV randomness, tamper GCM tag, legacy ECB fallback, fingerprint hex/symmetric, full 8 groups | Robolectric `@Config(sdk=36)` for `android.util.Base64`, `assertNotEquals` tamper, `assertEquals` fingerprint `4×8 hex` |
| **Room DAOs** | `data/DaosTest.kt:1` | 5 tests: `insertUser_and_retrieve`, `insertMessage_atomic` (snippet + time), `pagingSource_returnsMessages` (LoadResult.Page size 5), `getMessageById_found`, `incrementAndClearUnread` | `Room.inMemoryDatabaseBuilder` `allowMainThreadQueries`, `runTest`, `first()` flows, `PagingSource.LoadParams.Refresh` |
| **Migration** | `data/MigrationTest.kt:1` | `MIGRATION_1_2` start 1 → 2 no-op preserves data, `getAllUsers().first()` size 1 | `inMemoryDatabaseBuilder.addMigrations`, `runBlocking` |
| **ViewModel** | `ui/ChatViewModelTest.kt:1` | `searchQuery_filtersChats`, `setActiveChat_clearsUnread` | `ApplicationProvider`, `ChatViewModel(app)` legacy ctor, `delay(200)` for `combine`, `first()` |
| **UI Roborazzi** | `ResalaScreenshotTest.kt:1` + `GreetingScreenshotTest.kt:1` | Light LTR, dark typing, RTL `ar-rSA` `LayoutDirection.Rtl` | `MyApplicationTheme(darkTheme)`, `CompositionLocalProvider`, `captureRoboImage("src/test/screenshots/...")`, `RobolectricDeviceQualifiers.Pixel8` |
| **Instrumented** | `androidTest/FirestoreEmulatorTest.kt:1` | InMemory sanity, `useEmulator("10.0.2.2",8080)` scaffold, `packageName` | `AndroidJUnit4`, `runBlocking`, `first()` |
| **Manual** | — | `composeTestRule` send/typing/call via `testTag` `add_contact_fab`, `chat_search_input`, `send_msg_button`, `end_call_button`, `onboarding_*`, `empty_state_add_button` | `createComposeRule`, `onNodeWithTag` (future) |

## Deps

- `junit 4.13.2`, `androidx-junit 1.3.0`, `espresso-core 3.7.0`, `robolectric 4.16.1`, `roborazzi 1.59.0`, `kotlinx-coroutines-test 1.10.2`, `room-testing` (via `roomCompiler 2.7.0`), `arch-core-testing 2.2.0`, `test:core-ktx 1.6.1` — `gradle/libs.versions.toml:1`, `app/build.gradle.kts:126`

## Run

```bash
./gradlew :app:testDebugUnitTest            # all unit (incl. Robolectric)
./gradlew :app:connectedDebugAndroidTest    # needs emulator + firestore emulator for E2E
./gradlew :app:recordRoborazziDebug         # generate baseline
./gradlew :app:verifyRoborazziDebug         # diff <1% (Phase 4 KPI)
./gradlew :app:lintDebug                    # 0 error (Phase 4 KPI)
```

CI runs all four in `/.github/workflows/ci.yml:1` (assemble+test+roborazzi+lint).

## Notes

- `android.util.Base64` needs Robolectric — pure JUnit without `@RunWith(RobolectricTestRunner)` will NPE.
- `MigrationTestHelper` needs `Instrumentation`, so Phase 5 uses simple inMemory with `addMigrations` scaffold (real helper in instrumentation).
- `ChatViewModelTest` uses legacy `Application` ctor to avoid Hilt test setup; Hilt test (`@HiltAndroidTest`) planned Phase 5b.
- Firestore emulator: `firebase emulators:start --only firestore` then uncomment `useEmulator` in `FirestoreEmulatorTest`.
