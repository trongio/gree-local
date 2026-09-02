# Status

Last updated 2 September 2026.

A running record of what exists, what state it is in, and what is left. `README.md` is
the public description, `RELEASING.md` holds the Play detail, `CLAUDE.md` holds the
working rules. This file is the one to read first after a break.

## Where the project stands

The app is **built, verified on real hardware, and submitted to Google Play for review**.
Nothing is blocked on code. The remaining work is waiting on Google, then running the
14-day closed test.

| Thing | State |
|---|---|
| Protocol layer | Working, 20 unit tests passing on the JVM |
| App | Feature complete for v0.1.0, verified on a Galaxy S22 Ultra (Android 16) |
| Home screen widget | Working |
| Repo | Public, `github.com/trongio/gree-local`, CI green |
| GitHub release | `v0.1.0` tagged, APK attached |
| Privacy policy | Live at `https://trongio.github.io/gree-local/` |
| Play app | Created, setup 11/11, submitted for review 2 September 2026 |
| Production | Not yet. Needs review approval, then 12+ testers for 14 continuous days |

## What was built

Started from the question of whether the AC could be driven without the vendor cloud. It
can: Gree Wi-Fi modules speak a plain UDP protocol on port 7000, and the cloud signup is
only in the way. Confirmed against the unit at `192.168.0.199` (firmware V1.2.1, protocol
v1, AES-128-ECB) before writing any app code.

Native Kotlin was chosen over React Native so the socket is a raw `DatagramSocket` with
no JS bridge in the path. Cold start is about 0.9 s and the release APK is 1.4 MB.

Shipped in v0.1.0:

- subnet discovery (unicast sweep plus broadcast), local bind, per-device key storage
- power, mode, target temperature, fan speed including a separate Auto control
- vertical swing positions, drawn as ray diagrams on a Canvas
- Turbo, Quiet, Sleep, Display light, X-Fan, Health
- live room temperature from the unit's internal sensor
- multiple units, rename, add-by-IP fallback for networks that block sweeps
- home screen widget rendering from a cached state so it draws without waiting on the unit

The UI is an implementation of a design that was imported and then iterated on: the
temperature hero is slider variant 2b, with a hatched band showing the gap between room
and target, layered under the fill when cooling and inside it when heating.

## Play Console state

Personal account `trongio` (`8007123209084828794`), which already ships
`com.prava.trongio`.

- app `ge.hackerman.gree`, app id `4974487171322038217`
- title `Gree Local (unofficial)`, category House & Home, contact
  `giokakabadze50@gmail.com`
- versionCode 1 signed with the existing Prava upload key (`CN=trongio`, SHA-256
  `f026e244...10e3f1`)
- internal track live, closed track `Alpha` in review, 177 countries
- account-level `TESTERS` list (18 people) attached, shared with Prava
- all eight policy declarations answered, recorded in `RELEASING.md`
- 14 changes submitted 2 September 2026, Google quotes up to 7 days

One non-blocking warning on the bundle: no debug symbols uploaded for the native
libraries. It only makes native crash traces harder to read. Worth adding to a later
release, not worth holding this one.

## Open items

1. **`giorgi.kakabadze@casatrade.ge` is not in the TESTERS list.** Two automated attempts
   failed with "Your changes couldn't be saved" while other edits in the same session
   succeeded. Needs adding by hand. The list is account-level, so this also changes
   Prava's testers.
2. **Tablet screenshots** are marked required on the listing but did not block saving.
   They may be asked for during review. Either render them on a tablet emulator or
   declare the app phone-only under Reach and devices.
3. **Debug symbols** for the release bundle, see above.

## Next actions

Once review clears, the app leaves Draft and the testers can opt in:

1. get the 18 testers to actually opt in through the closed-test link
2. hold 12 or more opted in continuously for 14 days
3. apply for production access

Shipping an update after that:

```sh
# bump versionCode and versionName in app/build.gradle.kts first
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ANDROID_HOME=~/Android/Sdk
./gradlew bundleRelease
tools/play_upload.py app/build/outputs/bundle/release/app-release.aab \
    internal "0.1.1" completed
```

`versionCode` must increase on every upload and Play takes the `.aab`, not the `.apk`.

## Known future work

- AES-GCM (protocol v2) firmware is not supported, only v1 ECB.
- `LOCAL_NETWORK_ACCESS` becomes mandatory at target API 37. The entire app is local
  network traffic, so discovery and control stop working on the day the target moves
  without it. Declare it, request it at runtime, and handle refusal with a real
  explanation.
- Quick Settings tile is the obvious next surface after the widget.

## Unresolved

The unit was observed going `Pow:0` to `Pow:1` and back with nothing touching it. Never
reproduced deliberately and never explained. It may be that this firmware powers on for
any parameter command, in which case the plus and minus buttons can start the AC
unintentionally. Worth resolving, because it would be a real behavioural bug.
