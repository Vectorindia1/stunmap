# STUNMAP — Project Memory
**Read this at the start of every Claude Code session before doing any work.**

---

## What This Project Is

STUNMAP is an Android OSINT tool built for a cybersecurity investigator (Veenu, final-year B.Tech CSE / Cyber Security, OSINT + offensive security focus). It captures network traffic during WhatsApp calls, identifies STUN protocol exchanges, extracts peer IP addresses from `XOR-MAPPED-ADDRESS` attributes, and geolocates them. It is a personal investigative utility — sideloaded, not Play Store distributed.

---

## Key Technical Decisions Made

| Decision | What was chosen | Why |
|---|---|---|
| Capture method | `VpnService` TUN interface | Only rootless option on Android |
| STUN detection | Magic cookie `0x2112A442` | RFC 5389 standard — reliable, no false positives |
| IP address extraction | `XOR-MAPPED-ADDRESS` (0x0020), fallback `MAPPED-ADDRESS` (0x0001) | XOR variant is the modern standard |
| GeoIP online | IPinfo.io API | Simple REST, good free tier |
| GeoIP offline | MaxMind GeoLite2 City .mmdb bundled in assets | Works without internet |
| Meta IP filter | Bundled CIDR list in `assets/meta_asn_cidr.txt` | Eliminates false positives from WhatsApp servers |
| Architecture | MVVM + Clean (Repository) + Hilt DI | Maintainable, testable |
| UI framework | Jetpack Compose + Material3 | Modern, declarative |
| Language | Kotlin 100% | No Java interop needed |
| DB | Room | Standard Android persistence |
| Map | MapLibre | No API key, OpenStreetMap |
| Font | System Monospace (JetBrains Mono TTFs optional) | Avoids bundling 3MB font if not available |

---

## Project State

**Status: Phase 0-5 COMPLETE (Phases 6-8 partial)**

### Completed
- Full Android project structure (`stunmap/` subdirectory)
- Phase 0: Project setup, Gradle config, all dependencies
- Phase 1: All parser layer (StunParser, IpPacketParser, UdpParser, ByteUtils, CidrMatcher)
- Phase 2: Classification layer (MetaAsnFilter, SelfIpDetector, IpClassifier) + GeoIP (IpInfoClient, MaxMindResolver, GeoIpResolver)
- Phase 3: Room database (AppDatabase, all DAOs, all entities, repositories)
- Phase 4: VPN capture service (StunCaptureService, ServiceNotification, SessionManager)
- Phase 5: Full UI layer (all screens, all ViewModels, navigation, theme, components)
- Phase 6 (partial): Export (JSON + text), Settings persistence (DataStore), Meta ASN update

### Remaining
- Phase 6: FileProvider manifest registration ✅ done; Interrupted session handling ✅ done
- Phase 7: Testing hardening (unit tests written; integration/device tests not yet)
- Phase 8: Build signing config (see CLOUD.md)
- JetBrains Mono TTF fonts — must be downloaded and placed in `app/src/main/res/font/` as `.ttf` files; current fallback is system monospace
- `GeoLite2-City.mmdb` — must be downloaded separately (see CLOUD.md)

---

## File Locations That Matter

| File | Path | Notes |
|---|---|---|
| PRD | `stunmap/PRD.md` | Full product requirements |
| Architecture | `stunmap/ARCHITECTURE.md` | Class-level implementation details + byte parsing |
| Cloud | `stunmap/CLOUD.md` | GeoIP API keys, ASN list update, any backend |
| Meta ASN list | `stunmap/app/src/main/assets/meta_asn_cidr.txt` | Update from CLOUD.md source |
| MaxMind DB | `stunmap/app/src/main/assets/GeoLite2-City.mmdb` | Developer must download separately (see CLOUD.md) |
| Main Service | `stunmap/app/src/main/java/com/stunmap/service/StunCaptureService.kt` | VpnService — most critical file |
| STUN Parser | `stunmap/app/src/main/java/com/stunmap/parser/StunParser.kt` | Core parsing logic |
| IP Classifier | `stunmap/app/src/main/java/com/stunmap/classifier/IpClassifier.kt` | SELF/META/CANDIDATE logic |
| Session Manager | `stunmap/app/src/main/java/com/stunmap/session/SessionManager.kt` | Business logic hub |

---

## Package Name

```
com.stunmap
```

---

## Critical Constraints Claude Code Must Never Violate

1. **No root required** — VpnService only, no su/su exec calls
2. **Foreground service mandatory** — Service MUST run as foreground with notification or Android kills it on API 26+
3. **VPN permission dialog** — Must call `VpnService.prepare(context)` before starting service; if it returns a non-null Intent, launch that Intent to get user permission
4. **Packet loop MUST forward traffic** — Write every packet back to the TUN fd output stream, not just STUN ones. Otherwise the device loses internet during capture.
5. **Never crash the packet loop** — Every parsing operation is wrapped in try/catch. A malformed packet is logged and skipped, never crashes the service.
6. **Single VPN constraint** — Only one `VpnService` can run at a time.
7. **MaxMind GeoLite2 license** — The .mmdb file requires a free MaxMind account to download. It is NOT bundled in git. Add it to `.gitignore`.
8. **IPinfo.io API key is optional** — if absent or empty, fall back to MaxMind offline immediately.

---

## Known Issues / Watch Out For

- `ByteArray.getShort()` helper must mask with `0xFF` before shifting — Kotlin Byte is signed (-128 to 127).
- MapLibre v11: use `org.maplibre.android.MapLibre` (not `com.mapbox`).
- VpnService `setBlocking(true)` simplifies the read loop — runs in `Dispatchers.IO`.
- Android 12+ requires `FOREGROUND_SERVICE_SPECIAL_USE` permission.
- `VpnService.Builder.establish()` returns null if user hasn't granted permission.
- MapLibre annotation plugin (`maplibre-plugins-annotation`) may need separate dependency if used.
- ExportUtils uses FileProvider — `android:authorities="${applicationId}.provider"` must match manifest.
