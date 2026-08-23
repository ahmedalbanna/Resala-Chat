# Store Assets — Resala Chat (رِسالة)

Phase 5: Arabic screenshots + feature graphic (replaces GHBanner).

## Screenshots
Generated via Roborazzi (`ResalaScreenshotTest`):
- `app/src/test/screenshots/resala_chatlist_light.png` — Light LTR
- `app/src/test/screenshots/resala_chatlist_dark.png` — Dark with typing indicator
- `app/src/test/screenshots/resala_chatlist_rtl.png` — RTL Arabic (`ar-rSA`)
- `app/src/test/screenshots/greeting.png` — Baseline

Run: `./gradlew :app:recordRoborazziDebug` then verify with `verifyRoborazziDebug`.

## Feature Graphic
- Source: `assets/` (placeholder) — replace with 1024x500 Arabic feature graphic
- Must show E2EE shield, Arabic tagline, Vibrant palette (#005FB0)

## Play Console Checklist (Phase 5.3)
- [x] `data_extraction_rules.xml` / `backup_rules.xml` exclude `resala_secure_db*` (Phase 0)
- [x] `targetSdk 36` compliance (app/build.gradle.kts)
- [ ] Privacy policy URL (add to store listing)
- [ ] Content rating questionnaire (Communication)
- [ ] Data safety: Encryption in transit + at rest, no data sharing
