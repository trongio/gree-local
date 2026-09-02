<p align="center">
  <img src="art/banner-1600x400.png" alt="Gree Local" width="820">
</p>

A native Android app for controlling Gree air conditioners **entirely over your LAN**.
No cloud account, no vendor servers, no internet connection required.

> **Unofficial.** Not affiliated with, endorsed by, or connected to Gree Electric
> Appliances. Gree is a trademark of its respective owner. Protocol details come from
> public reverse engineering work by the community.

Gree units with a Wi-Fi module speak a simple UDP protocol on port 7000. The vendor
apps use it too, but only after routing you through a cloud signup. This app skips that
part: it discovers units on your subnet, binds directly, and talks to them over the wire.

## Status

Verified end to end on a Galaxy S22 Ultra (Android 16) against a Gree unit running
firmware `V1.2.1` (protocol v1, AES-128-ECB): subnet discovery, local bind, live status
polling and command writes all confirmed, with writes checked independently against the
unit from a second client.

## Features

- Discovers units by sweeping the local subnet (unicast plus broadcast)
- Binds locally and stores the per-device key on the phone
- Power, mode (auto / cool / dry / fan / heat), target temperature, fan speed
- Vertical swing positions
- Turbo, Quiet, Sleep, Display light, X-Fan, Health
- Live room temperature readout from the unit's internal sensor
- Multiple units, add-by-IP fallback for networks that block sweeps

## Why native

Raw `DatagramSocket` and `javax.crypto` with no JS bridge or bundled runtime. Cold start
is instant, the APK is small, and there is a clean path to a home screen widget and a
Quick Settings tile later.

## Privacy

Nothing is collected, sent, or stored off the phone.
[Full policy](https://trongio.github.io/gree-local/).

## Build

Requires the Android SDK (compileSdk 36) and JDK 17 or newer.

```sh
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Protocol notes

Discovery is a plain `{"t":"scan"}` datagram. Everything after that is a JSON envelope
whose `pack` field is AES-128-ECB base64. Binding uses a generic key shared by every
Gree unit; the unit replies with a per-device key used for all later traffic.

The unit stores **one** key at a time, so binding here may make the vendor app re-bind
on its next launch. Nothing breaks, it just re-pairs silently.

Newer firmware (roughly 2022 onward) uses AES-GCM instead of ECB. That variant is not
implemented yet.

### Room temperature drops by 3 degrees in heat mode

`TemSen` carries the internal sensor with a `+40` bias, so `65` means 25 C. On this unit
it also **reports 3 degrees lower whenever the mode is heat**, and snaps back on leaving
it.

That is the air conditioner, not this app. It reproduces with the unit **powered off**,
where there is no fan, no coil and nothing physical that could move a sensor:

| Mode (unit powered off) | `TemSen` | Reported |
|---|---|---|
| Cool | 65 | 25 C |
| Heat | 62 | 22 C |
| Cool | 65 | 25 C |
| Heat | 62 | 22 C |
| Auto / Dry / Fan | 65 | 25 C |

Heat is the only mode that shifts it, always by exactly 3, instantly and repeatably. So
the firmware subtracts a constant from the reported value in heat mode. The likely reason
is the usual one for a wall-mounted split: the unit sits high where the air is warmest, so
in heating it biases the reading toward what the occupied zone feels rather than what the
sensor sees. The `+40` encoding is well documented; this heat-mode constant is not, so
that explanation is inference from the measurement above.

The app deliberately does **not** correct for it. The constant probably varies by model,
and compensating would make the app disagree with the unit's own display and remote.

