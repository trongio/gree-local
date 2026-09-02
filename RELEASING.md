# Releasing to Google Play

## Blockers to settle first

### 1. The name uses someone else's trademark

"Gree" is a registered trademark of Gree Electric Appliances. Google Play's impersonation
policy prohibits a listing that suggests an affiliation that does not exist, and a title
beginning with another company's brand is the usual way apps trip it. A takedown here
costs the listing, not just a review round.

Safer options, roughly in order:

- rename to something that does not lead with the brand, and describe compatibility in
  the description instead ("works with Gree and compatible air conditioners")
- if the brand stays in the title, put "unofficial" in it, and keep the
  "not affiliated with" line in the description

Changing the display name is cheap. Changing `applicationId` is not: it is permanent once
published, so decide before the first upload.

### 2. Personal accounts need 12 testers for 14 days

Personal Play Console accounts created after 13 November 2023 must run a closed test with
at least 12 testers opted in continuously for 14 days before applying for production. It
applies per app. Organization accounts registered to a legal business entity are exempt.

Budget three weeks from first upload to public availability, and line up 12 real people
with Google accounts.

### 3. Local network permission is coming

The whole app is local network traffic. Android 16 made local network access an opt-in
runtime permission; it becomes **mandatory for apps targeting API 37**. Play's target-API
deadlines will force that target eventually, and on the day it does, discovery and control
stop working without the permission.

Nothing to change while targeting 36, but when moving to 37: declare
`android.permission.LOCAL_NETWORK_ACCESS`, request it at runtime, and handle refusal by
telling the user why the app cannot see their unit.

## One-time setup

Create an upload key. Keep the passwords in your password manager and the keystore
somewhere backed up: losing it means you can never update the listing again.

```sh
keytool -genkeypair -v \
  -keystore ~/keys/gree-local-upload.jks \
  -keyalg RSA -keysize 4096 -validity 10000 \
  -alias upload
```

Then point the build at it, from a file git already ignores:

```sh
cat > keystore.properties <<'PROPS'
storeFile=/home/azael/keys/gree-local-upload.jks
storePassword=...
keyAlias=upload
keyPassword=...
PROPS
```

Without that file the release build still compiles, just unsigned.

## Each release

```sh
./gradlew bundleRelease
# app/build/outputs/bundle/release/app-release.aab
```

Bump `versionCode` (must increase every upload) and `versionName` in
`app/build.gradle.kts` first. Play takes the `.aab`, not the `.apk`.

## Listing assets

Everything Play asks for is in `play/`:

| Asset | File |
|---|---|
| App icon, 512x512 | `play/icon-512.png` |
| Feature graphic, 1024x500 | `play/feature-graphic-1024x500.png` |
| Phone screenshots (2 minimum) | `play/screenshots/` |
| Store listing text | `play/listing.md` |
| Data safety answers | `play/listing.md` |

The privacy policy is `PRIVACY.md`. Play needs it at a public URL: enabling GitHub Pages
on this repo, or a gist, is enough.
