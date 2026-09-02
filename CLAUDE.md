# Gree Local

Native Android app (Kotlin + Jetpack Compose) that controls Gree air conditioners over
the LAN with no cloud account. Public repo: `trongio/gree-local`. Published on Google
Play as `ge.hackerman.gree`.

Read `STATUS.md` first for where the project actually stands. `README.md` is the public
face, `RELEASING.md` is everything about Play.

## Build

The environment needs both variables set explicitly; the shell does not export them.

```sh
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export ANDROID_HOME=~/Android/Sdk

./gradlew assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest      # 20 tests, protocol layer, runs on the JVM
./gradlew bundleRelease          # .aab for Play, needs keystore.properties
```

Phone is a Galaxy S22 Ultra, `adb -s R5CW61C9K4F install -r <apk>`.

Gradle 8.11.2 / Kotlin 2.2.20 / compileSdk 36 / minSdk 26. Versions live in
`gradle/libs.versions.toml`; use the version catalog, not inline coordinates.

## Layout

```
protocol/   GreeCrypto, GreeClient, GreeProtocol   pure JVM, no android.* imports
data/       DeviceStore, StateCache, LanScanner, GreeDevice
ui/         ControlScreen, HomeScreen, TemperatureBar, SwingCard, Symbols, theme/
widget/     GreeWidget (AppWidgetProvider), WidgetConfigActivity, WidgetTargets
```

`protocol/` deliberately uses `java.util.Base64` rather than `android.util.Base64` so
the whole layer runs under plain JUnit with no instrumentation. Keep it that way: if
you add an `android.*` import there, the tests stop covering it.

## Rules that came from real bugs

- **Never read a `MutableStateFlow.value` inside a composable.** That creates no snapshot
  subscription, so the UI silently never recomposes. This shipped once and looked like a
  network bug. The screens collect `GreeViewModel.deviceUis` / `selectedUi`, which are
  `combine(...).stateIn(...)`. Add new derived state the same way.
- **Compose `Canvas` does not clip to its bounds.** Anything drawn past the edge paints
  over siblings. `Modifier.clipToBounds()` where it matters.
- **Play Console fields arrive pre-populated.** Typing into one appends and the save
  fails validation with no useful message. `ctrl+a` first, every time. This burned three
  attempts on the contact email and two on release notes.
- **A drag gesture must commit once on release**, not per pointer move. The unit gets one
  datagram per commit and a single drag would otherwise send a dozen.

## Protocol facts

Port 7000 UDP, JSON envelope with a base64 `pack` field, AES-128-ECB/PKCS7. Generic key
`a3K8Bx%2r8Y7#xDh` is used only for discovery and bind; the unit then issues a per-device
key. **The unit stores exactly one key**, so re-binding from anywhere invalidates the
previous one and the vendor app re-pairs on its next launch.

`TemSen` carries a `+40` bias, and `0` means the unit has no sensor, which must surface
as null rather than -40 C. This firmware also reports 3 degrees low in heat mode; that is
the AC, not the app, and it is deliberately not corrected. Evidence table is in the
README.

Test unit: `192.168.0.199`, firmware V1.2.1, protocol v1. It does **not** answer UDP
broadcast to `.255` from the wired desktop, only unicast, which is why discovery sweeps
the whole /24.

AES-GCM firmware (roughly 2022 onward) is not implemented.

## Design system

Fixed palette, not Material You, because dynamic colour washes out the teal. Read colours
through `Gree.colors` (`bg`, `card`, `ink`, `ink2`, `line`, `accent`, plus `warm` for heat
and offline). Type is Albert Sans (variable, weights via `FontVariation.Settings`), IBM
Plex Mono for numerals, and Material Symbols Rounded subset with `pyftsubset`. If you add
an icon glyph, re-subset the font or it renders as tofu.

## Do not

- Read, print, echo, or type the keystore passwords. `keystore.properties` and `*.jks`
  are gitignored; if the user needs them moved, hand them a shell one-liner to run.
- Print the contents of `~/.config/play/trongio-key.json`.
- Commit a real device MAC or per-device key.
- Change the Play title away from `Gree Local (unofficial)` or drop the non-affiliation
  line from the description. Both are the trademark mitigation, not filler.
- Use the em dash character anywhere.
