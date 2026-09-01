# Alien News for Android

Native Android reader for [Alien News](https://aliennews.co.il), built with Kotlin and Jetpack Compose.

## Product

- Hebrew interface with correct RTL layout
- Live editorial feed from `https://aliennews.co.il/api/app-feed`
- Smart local search by headline, category and article text
- Bookmarks stored on the device
- Offline fallback to the latest successful feed
- In-app reading, direct links and sharing
- Optional notifications when a new report is published
- No account, advertising SDK or personal tracking

## Build

Open the project with the current stable Android Studio, allow Gradle sync, then run:

```bash
./gradlew test lint bundleRelease
```

The Play Store artifact is generated at `app/build/outputs/bundle/release/app-release.aab` after an upload signing key is configured locally. Never commit the keystore or its passwords.

### First signed build on Windows

1. Open PowerShell in the project folder.
2. Run `Set-ExecutionPolicy -Scope Process Bypass`.
3. Run `.\setup-release.ps1` and choose two strong passwords.
4. Make two encrypted backups of `alien-news-upload.jks` and `keystore.properties`.
5. In Android Studio choose Build, Generate Signed App Bundle or APK, Android App Bundle.
6. Select the release variant and build the bundle.

The setup script refuses to overwrite an existing signing key.

## Release gates

1. Confirm the public privacy page and support contact.
2. Create an upload key and enroll in Play App Signing.
3. Add the Play signing certificate SHA-256 fingerprint to the site's Digital Asset Links file.
4. Complete Data Safety and IARC questionnaires using `play-store/DATA_SAFETY.md`.
5. Run the required closed test for the account, if Google Play Console requests it.
6. Review Android vitals and use a staged production rollout.
