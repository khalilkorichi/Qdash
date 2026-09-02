# Project Rules for FinTrack-DZ (Qdash)

## 🚨 Critical Release Checklist — ALWAYS follow when bumping app version

When updating the app version in `app/build.gradle.kts`, you **MUST** also perform ALL of the following steps in the same commit or release:

1. **Update `app/build.gradle.kts`**: Bump `versionCode`, `versionName`, and `UPDATE_IDENTITY`.
2. **Build the APK**: Run `.\gradlew assembleRelease` to produce the minified, optimized APK (with R8 and resource shrinking).
3. **Copy APK to `.build-outputs/`**: Copy the APK file from `app/build/outputs/apk/release/app-release.apk` to `.build-outputs/app-release.apk`.
4. **Update `update.json`**: Update ALL fields to match the new release:
   - `versionCode` — must match the new versionCode in build.gradle.kts
   - `versionName` — must match the new versionName in build.gradle.kts
   - `updateIdentity` — must match the new UPDATE_IDENTITY in build.gradle.kts
   - `publishedAt` — set to current UTC timestamp
   - `apkUrl` — set to `"https://raw.githubusercontent.com/khalilkorichi/Qdash/main/.build-outputs/app-release.apk?v=<versionCode>"`
   - `apkSize` — get the exact byte size of the new APK using `(Get-Item "path").Length`
   - `apkSha256` — recalculate using `(Get-FileHash "path" -Algorithm SHA256).Hash.ToLower()`
   - `releaseNotes` — describe what changed in this version
5. **Stage ALL changed files**: `git add app/build.gradle.kts update.json .build-outputs/app-release.apk` plus any source changes.
6. **Commit and push**: Push everything in one atomic push.
7. **Create/Update GitHub Release with Raw Plain SHA-256**:
   When creating or editing a GitHub release (`gh release create` / `gh release edit`), the release notes body **MUST** contain a plain, unformatted line with the hash:
   ```text
   SHA-256: <64-char-hex-hash>
   sha256 = <64-char-hex-hash>
   ```
   ⚠️ **CRITICAL**: Do **NOT** wrap the keyword in Markdown bolding (e.g. `**SHA-256**:`) or code backticks (e.g. `` `hash` ``) on that line, because older installed client versions use `Regex("(?i)sha-?256\\s*[:=]\\s*([a-f0-9]{64})")` to parse the hash when falling back to GitHub API releases. Any markdown asterisks or backticks cause the legacy regex to fail with *"لا يحتوي اصدار github على sha-256 صالح لملف apk"*.

### Why this matters
The app on user devices fetches `update.json` from GitHub raw CDN to detect new versions, and falls back to GitHub Releases API if CDN fails. If `update.json` or the GitHub Release is missing or poorly formatted, users cannot verify and install the update.

## Google Play Protect on Emulators
To prevent Play Protect from scanning/blocking debug APK installations on emulators, run:
```bash
adb shell settings put global package_verifier_user_consent -1
adb shell settings put global verifier_verify_adb_installs 0
```
