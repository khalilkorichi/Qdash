# Project Rules for FinTrack-DZ (Qdash)

## 🚨 Critical Release Checklist — ALWAYS follow when bumping app version

When updating the app version in `app/build.gradle.kts`, you **MUST** also perform ALL of the following steps in the same commit or release:

1. **Update `app/build.gradle.kts`**: Bump `versionCode`, `versionName`, and `UPDATE_IDENTITY`.
2. **Build the APK**: Run `.\gradlew assembleDebug` to produce the new APK.
3. **Copy APK to `.build-outputs/`**: Replace the APK file at `.build-outputs/New folder (2)/app-debug.apk` with the newly built one.
4. **Update `update.json`**: Update ALL fields to match the new release:
   - `versionCode` — must match the new versionCode in build.gradle.kts
   - `versionName` — must match the new versionName in build.gradle.kts
   - `updateIdentity` — must match the new UPDATE_IDENTITY in build.gradle.kts
   - `publishedAt` — set to current UTC timestamp
   - `apkSize` — get the exact byte size of the new APK using `(Get-Item "path").Length`
   - `apkSha256` — recalculate using `(Get-FileHash "path" -Algorithm SHA256).Hash.ToLower()`
   - `releaseNotes` — describe what changed in this version
5. **Stage ALL changed files**: `git add app/build.gradle.kts update.json ".build-outputs/New folder (2)/app-debug.apk"` plus any source changes.
6. **Commit and push**: Push everything in one atomic push.

### Why this matters
The app on user devices fetches `update.json` from GitHub raw CDN to detect new versions. If `update.json` is not updated, **no user will ever see the update notification**, even if the code changes are pushed. The `update.json` is the **single source of truth** for the in-app update system.

## Google Play Protect on Emulators
To prevent Play Protect from scanning/blocking debug APK installations on emulators, run:
```bash
adb shell settings put global package_verifier_user_consent -1
adb shell settings put global verifier_verify_adb_installs 0
```
