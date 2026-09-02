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

### 2. Closed testing, if the account is a post-2023 personal one

The Play Console account already exists and already ships `com.prava.trongio`
("მართვის მოწმობა - Prava"), so registration and production access are done.

The closed-test requirement is nonetheless **per app, not per account**: a personal
account created on or after 13 November 2023 must run a closed test with 12 testers
opted in continuously for 14 days before each *new* app reaches production. Updates to an
app that already has production access are exempt, which is why Prava ships freely.

Two exemptions decide whether this applies here, and only you can confirm which:

- the account was created **before** 13 November 2023, or
- it is an organization account registered to a legal business entity

If either holds, this app can go straight to production. If neither does, budget about
three weeks. The hard part is already solved: the same 12 testers may be reused across
apps, so whoever tested Prava can test this.

### 3. Local network permission is coming

The whole app is local network traffic. Android 16 made local network access an opt-in
runtime permission; it becomes **mandatory for apps targeting API 37**. Play's target-API
deadlines will force that target eventually, and on the day it does, discovery and control
stop working without the permission.

Nothing to change while targeting 36, but when moving to 37: declare
`android.permission.LOCAL_NETWORK_ACCESS`, request it at runtime, and handle refusal by
telling the user why the app cannot see their unit.

## One-time setup

An upload key already exists, the one the Prava app uses, with alias `app-key` at
`PHPNative-DrivingTest-app/credentials/app-release-key.jks`. Google permits reusing an
upload key across apps in the same account, so there is nothing to generate:

```sh
cp keystore.properties.example keystore.properties
# then fill in storePassword and keyPassword
```

Both `keystore.properties` and `*.jks` are gitignored. Without that file the release
build still compiles, just unsigned.

To use a separate key for this app instead:

```sh
keytool -genkeypair -v \
  -keystore ~/keys/gree-local-upload.jks \
  -keyalg RSA -keysize 4096 -validity 10000 \
  -alias upload
```

Either way the keystore must be backed up. Losing it means never being able to update the
listing again.

## Publishing from the command line

The account already has a Play service account, the one set up for the Prava app:

- key `~/.config/play/trongio-key.json`, service account
  `calude@prava-play-api.iam.gserviceaccount.com`, Cloud project `prava-play-api`
- verified working against both `androidpublisher` and `playdeveloperreporting`

**The API cannot create an app.** Confirmed by probing it: `com.prava.trongio` opens an
edit and returns 200, `ge.hackerman.gree` returns `404 Package not found`. So the first
step is console-only, and it is also the step that asks you to accept the developer
program policy and export declarations, which is yours to accept, not mine.

1. Play Console, **Create app**: name `Gree Local (unofficial)`, app, free.
2. Fill the store listing from `play/listing.md`, and upload the assets in `play/`.
3. Everything after that is scripted:

```sh
./gradlew bundleRelease
tools/play_upload.py app/build/outputs/bundle/release/app-release.aab \
    internal "0.1.0" completed
```

Bump `versionCode` (must increase every upload) and `versionName` in
`app/build.gradle.kts` first. Play takes the `.aab`, not the `.apk`.

Track names: `internal`, `alpha` (closed testing, where the 12-tester clock runs),
`beta` (open), `production`.

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
