# Hush — Geofenced Auto-Silent (Android APK)

A premium dark/aurora-styled Android app that **automatically silences your phone
(ringer + media + notification + system volumes) the moment you enter any location
you selected**, and restores everything when you leave.

- **APK:** `Hush-v1.1.0.apk` (workspace root & `release/`, signed v2+v3, minSdk 26 / targetSdk 34)
- **Package:** `com.aurora.hush` · version 1.1.0 (crash-hardened)
- **Source:** `hush/` (pure framework Android — no third-party libraries)
- **Build script:** `build.sh` (aapt2 → javac → d8 → zipalign → apksigner, no Gradle)

## Features
- Aurora pastel header with wave divider, live clock, battery readout (reference-style UI)
- Circular progress rings per zone showing live proximity
- Add zones by **current GPS fix** or **tap-on-map** (dark Leaflet/CARTO map)
- Adjustable silence radius (50 m – 1.55 km) per zone
- Silent or Vibrate mode; restores ringer + all stream volumes on exit
- Foreground location service (works in background), boot auto-start, master switch
- Per-zone enable/disable (tap a card) and delete

## Install
1. Copy `Hush-v1.1.0.apk` to your phone and tap it (allow *Install unknown apps*).
2. Open **Hush** and grant:
   - **Location → Allow all the time** (background detection)
   - **Notifications** (status notification)
   - **Do Not Disturb access** (Android requires this to change ringer mode) —
     a warning card on the home screen deep-links straight to it.
3. Tap **+**, name the zone (Home/Office…), pick location, set radius, save.
4. Keep battery optimization off for Hush (Settings → Apps → Hush → Battery → Unrestricted)
   for most reliable background detection.

## How silencing works
A foreground service listens to GPS/network/passive location updates (plus a 20 s
fallback poll). When your position falls inside a zone radius it saves your current
ringer mode + stream volumes, sets ringer to Silent/Vibrate and zeroes
ring/notification/media/system/DTMF volumes. On exit (or *Restore sound now* in
Settings, or service stop) everything is restored exactly.

## Rebuild
```bash
./build.sh      # outputs out/Hush-v1.0.0.apk
```
Requires the SDK pieces already fetched to `/home/user/sdk` (build-tools 34, platform-34).

## v1.1.0 changelog (crash & glitch fixes)
- FIX launch crash: `registerReceiver` now passes `RECEIVER_NOT_EXPORTED` (required on targetSdk 34)
- FIX first-launch crash: location-type foreground service never starts before location permission is granted (Android 14 SecurityException); service also self-stops safely if started without permission
- FIX boot crash: BootReceiver checks permission + wraps start in try/catch
- FIX "Stop protection" no longer restarts on next app open (master flag synced everywhere)
- FIX notification is now tappable (opens app) and no longer re-alerts on every update
- FIX zone list no longer rebuilds (flicker) unless data actually changed
- FIX map screen shows a friendly toast if tiles can't load offline
- Hardened all receiver/service/broadcast calls with try/catch; explicit package-scoped broadcasts
- Tests: 27/27 JVM logic tests + 41-group static reference/manifest check pass

## License

Copyright (c) 2026 grdcreate

The original Hush project code in this repository is offered under a **dual
license**: you may use, modify and redistribute it under **either** of the
following, at your option:

- the **MIT License** — see [LICENSE-MIT](LICENSE-MIT), or
- the **Apache License, Version 2.0** — see [LICENSE-APACHE](LICENSE-APACHE).

Whichever license you select, you must comply with all of the terms of that
license for your use of the dual-licensed code.

**Third-party components are not part of this dual license.** Dependencies,
runtime services, assets and other components that are owned by others keep
their own separate licenses, which remain applicable to those components.
In particular:

- [Leaflet](https://leafletjs.com/) 1.9.4 (BSD-2-Clause) is loaded by the
  in-app map screen at runtime from a CDN; it is not bundled in this
  repository or in the APK.
- Map tiles served at runtime: © Esri (World Dark Gray Base keyless basemap)
  with © OpenStreetMap contributors as automatic fallback provider.
- Build-time tooling (Android SDK components: aapt2, d8, zipalign,
  apksigner) is used only to produce the APK and is not redistributed here.

All remaining source code, resources, icons and design assets in this
repository are original work of the copyright holder and are covered by the
dual license above. The complete terms of each offered license are available
in [LICENSE-MIT](LICENSE-MIT) and [LICENSE-APACHE](LICENSE-APACHE).
