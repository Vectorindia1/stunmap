# STUNMAP

[![Build APK](https://github.com/Vectorindia1/stunmap/actions/workflows/build-apk.yml/badge.svg)](https://github.com/Vectorindia1/stunmap/actions/workflows/build-apk.yml)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

Android OSINT tool that captures and maps STUN/ICE traffic in real time — no root required.

STUNMAP uses Android's `VpnService` API to create a local TUN interface, intercepts UDP packets, and identifies STUN/ICE candidates. Every discovered peer IP is geolocated and plotted on an interactive map.

---

## Download

Grab the latest debug APK straight from the [**Releases page**](https://github.com/Vectorindia1/stunmap/releases/tag/latest-debug).

> The `latest-debug` release is rebuilt automatically on every push to `main`.

---

## Features

- **Rootless packet capture** — runs entirely through `VpnService`, no `su` required
- **STUN/ICE detection** — parses STUN binding requests/responses and ICE candidate attributes
- **Live map** — plots peer IPs on a MapLibre dark-theme map as they appear
- **IP geolocation** — online lookup via IPinfo.io with offline MaxMind GeoLite2 fallback
- **Session history** — browse, search, and swipe-to-delete past capture sessions
- **Export** — share session data as JSON
- **Single-tap VPN** — foreground service with persistent notification; one tap starts/stops capture

---

## Build from source

### Prerequisites

| Tool | Version |
|------|---------|
| Android Studio | Hedgehog or newer |
| JDK | 17 |
| Android SDK | API 34 |
| Gradle | 8.7 (wrapper included) |

### Steps

```bash
git clone https://github.com/Vectorindia1/stunmap.git
cd stunmap
./gradlew assembleDebug
# APK → app/build/outputs/apk/debug/app-debug.apk
```

### Optional: offline IP geolocation

The app falls back to an online lookup when the local database is absent. For fully offline geo:

1. Sign up for a free MaxMind account at <https://www.maxmind.com>
2. Download the **GeoLite2-City** database (`.mmdb`)
3. Copy it to `app/src/main/assets/GeoLite2-City.mmdb`

See [CLOUD.md](CLOUD.md) for IPinfo.io API key setup.

---

## Architecture

See [ARCHITECTURE.md](ARCHITECTURE.md) for a full breakdown of the packet-capture pipeline, Hilt dependency graph, and screen/ViewModel structure.

---

## Contributing

1. Fork the repo and create a branch (`git checkout -b feature/your-idea`)
2. Make your changes — keep commits focused
3. Run `./gradlew test` and make sure all tests pass
4. Open a pull request against `main`

Please follow the existing code style (Kotlin, Jetpack Compose, Hilt). No root calls, no background-only services (foreground + notification is mandatory per Android policy).

---

## License

[Apache 2.0](LICENSE) — © 2024 Vectorindia1
