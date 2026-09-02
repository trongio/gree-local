# Releasing to Google Play

## Blockers to settle first

### 1. The name uses someone else's trademark

"Gree" is a registered trademark of Gree Electric Appliances. Google Play's impersonation
policy prohibits a listing that suggests an affiliation that does not exist, and a title
beginning with another company's brand is the usual way apps trip it. A takedown here
costs the listing, not just a review round.

**Decision taken:** keep the name, qualify it. The Play title is
`Gree Local (unofficial)` and the description opens with the non-affiliation notice.
That is the mitigation, so do not quietly trim either one to save characters.

Residual risk is real but reduced: the title still leads with the brand. If Google ever
objects, the fallback is a rename that does not lead with it, describing compatibility in
the description instead ("works with Gree and compatible air conditioners"). The launcher
label stays the short "Gree Local" so it fits under the icon.

`applicationId` is `ge.hackerman.gree` and is permanent once published.

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

Privacy policy URL for the Play listing, already live:

    https://trongio.github.io/gree-local/

Served by GitHub Pages from `main` `/docs`. `PRIVACY.md` is the same text in the repo;
`docs/index.html` is the copy Play links to, so edit both together.
