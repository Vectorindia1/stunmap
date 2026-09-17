# STUNMAP — Product Requirements Document
**Codename:** STUNMAP
**Platform:** Android (primary), Windows (future)
**Type:** Personal investigative / OSINT utility — sideloaded, not Play Store distributed
**Author:** Veenu Krishna Shah
**For:** Claude Code (senior developer reference)
**Version:** 1.0.0

---

## 1. Problem Statement

During OSINT and investigative engagements involving WhatsApp calls, manually capturing and analyzing network traffic in Wireshark is slow, error-prone, and not viable on a mobile device mid-call. The investigator needs an always-available Android tool that can:

1. Passively capture network traffic during an active WhatsApp call
2. Parse STUN/ICE protocol exchanges automatically
3. Extract the peer's reflexive public IP from `XOR-MAPPED-ADDRESS` attributes
4. Filter out WhatsApp/Meta infrastructure IPs so no false positives appear
5. Geolocate the remaining candidate IP to city/state/ISP level
6. Display results cleanly in real-time with historical session logging

---

## 2. Goals

- **G1:** Fully functional on Android without root — VpnService-based capture only
- **G2:** Real-time STUN parsing during an active call with live UI updates
- **G3:** Intelligent IP classification (self / Meta infra / peer candidate)
- **G4:** GeoIP resolution with city, state, country, ISP, ASN output
- **G5:** Session history — every capture stored locally with full IP hit log
- **G6:** Export capability — share session report as JSON or PDF
- **G7:** Clean, dark-themed UI optimized for one-hand use during a live call
- **G8:** Offline-capable — Meta ASN filter list bundled locally; GeoIP works offline via MaxMind GeoLite2 local DB

## 3. Non-Goals (v1.0)

- iOS support
- Automatic detection of "call started" without user tap (v2 roadmap)
- PCAP file import (v2 roadmap — but architecture must allow it)
- Root-based deep packet inspection
- Any MitM or traffic manipulation
- Play Store distribution

---

## 4. Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin (100%) |
| Min SDK | API 26 (Android 8.0 Oreo) |
| Target SDK | API 34 (Android 14) |
| UI | Jetpack Compose + Material3 |
| Architecture | MVVM + Clean Architecture (Repository pattern) |
| DI | Hilt |
| Local DB | Room |
| Async | Kotlin Coroutines + Flow |
| Networking | OkHttp3 (GeoIP API calls) |
| Serialization | Kotlin Serialization (kotlinx.serialization) |
| GeoIP (online) | IPinfo.io API |
| GeoIP (offline) | MaxMind GeoLite2 City — mmdb bundled in assets |
| MMDB reader | MaxMind Java SDK (`geoip2`) |
| Maps | MapLibre (open-source, no key required) |
| Build | Gradle (Kotlin DSL) |
| Logging | Timber |

---

## 5. Complete Feature List

### 5.1 Core Features

**F1 — VPN Packet Capture Service**
- Subclass of `VpnService`
- Creates TUN interface that intercepts all device IP traffic
- Runs as foreground service with persistent notification (required by Android OS)
- Forwards non-STUN traffic transparently so phone stays fully connected during capture
- Graceful start/stop without device restart

**F2 — STUN Protocol Parser**
- Parses all UDP packets for STUN magic cookie (`0x2112A442`)
- Extracts message type, transaction ID, length
- Walks TLV attribute list looking for:
  - `XOR-MAPPED-ADDRESS` (0x0020) — un-XOR'd to get real IP
  - `MAPPED-ADDRESS` (0x0001) — legacy fallback
  - `SOFTWARE` (0x8022) — log for debug
- Matches request/response pairs by transaction ID
- Emits `StunHit` events to session manager

**F3 — IP Classifier**
- Four classification tiers:
  1. **SELF** — matches device's current public IP (fetched on session start)
  2. **META_INFRA** — matches bundled Meta/WhatsApp ASN CIDR list
  3. **KNOWN_STUN_SERVER** — Google STUN, Cloudflare STUN, etc.
  4. **CANDIDATE** — everything else (the target's peer IP)
- Meta ASN list bundled as `assets/meta_asn_cidr.txt`, updateable from cloud
- Classification result attached to every `StunHit`

**F4 — GeoIP Resolution**
- Online path: IPinfo.io API (`https://ipinfo.io/{ip}/json`)
- Offline path: MaxMind GeoLite2 City `.mmdb` bundled in `assets/`
- Resolves: city, region/state, country, postal code, latitude, longitude, ISP/org, ASN
- Results cached in Room to avoid re-fetching same IP twice
- Falls back to offline DB automatically if no internet

**F5 — Session Manager**
- Each capture = one `CaptureSession` with unique ID, timestamp, duration
- Stores all `StunHit` objects linked to session
- Stores all `GeoResult` objects linked to IPs
- Exposes session as `Flow<CaptureSession>` for live UI updates

**F6 — Live Capture Screen**
- Start/stop button (large, tap-able one-handed mid-call)
- Live counter: "X STUN packets seen, Y candidate IPs found"
- Running list of IP cards as they appear — colour coded by classification
- Candidate IPs pinned to top with geo data auto-loading beneath them
- Duration timer
- No automatic call detection in v1 — user taps Start when call begins

**F7 — Session History**
- List of all past capture sessions with date, duration, candidate count
- Tap to open full session detail
- Swipe to delete

**F8 — Session Detail View**
- Full STUN hit log (filterable by classification)
- For each CANDIDATE IP: city, state, country, ISP, ASN, lat/lng
- Map pin showing approximate geolocation (MapLibre)
- Export button (JSON / plain text summary)

**F9 — Settings**
- IPinfo.io API key entry (falls back to offline if empty)
- Toggle: show/hide SELF and META_INFRA IPs in live view
- Toggle: dark mode (default on)
- Button: update Meta ASN list from cloud URL
- Button: clear all session history

**F10 — Export**
- JSON export: full session object with all hits and geo results
- Text export: human-readable summary (IP, city/state/country, ISP)
- Share sheet integration (share to Telegram, save to files, etc.)

### 5.2 Future Features (v2 Roadmap — architect for, don't build now)

- **PCAP file import** — parse `.pcap`/`.pcapng` files from storage for offline analysis
- **Accessibility Service hook** — detect WhatsApp call screen to auto-start capture
- **Windows client** — Npcap-based capture with same STUN parser core (shared Kotlin/JVM module)
- **Dark web cross-reference** — pipe candidate IP to H4$CR4CK / Dark Gappa APIs
- **WebRTC call support** — extend parser to other apps using WebRTC (Signal, etc.)

---

## 6. Android Manifest Requirements

```xml
<!-- Required permissions -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- VpnService declaration — MANDATORY -->
<service
    android:name=".service.StunCaptureService"
    android:permission="android.permission.BIND_VPN_SERVICE"
    android:exported="false">
    <intent-filter>
        <action android:name="android.net.VpnService" />
    </intent-filter>
</service>
```

**Important:** VpnService requires user to grant the VPN permission via system dialog on first run. Handle this with `VpnService.prepare()` and an `ActivityResultLauncher`.

---

## 7. Project Structure

```
stunmap/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/stunmap/
│   │   │   │   ├── StunMapApp.kt               # Application class, Hilt init
│   │   │   │   │
│   │   │   │   ├── service/
│   │   │   │   │   ├── StunCaptureService.kt   # VpnService subclass — CORE
│   │   │   │   │   ├── PacketForwarder.kt      # Forwards non-STUN traffic
│   │   │   │   │   └── ServiceNotification.kt  # Foreground notification builder
│   │   │   │   │
│   │   │   │   ├── parser/
│   │   │   │   │   ├── IpPacketParser.kt       # Raw IP header parsing
│   │   │   │   │   ├── UdpParser.kt            # UDP header parsing
│   │   │   │   │   ├── StunParser.kt           # STUN magic cookie + TLV parser
│   │   │   │   │   ├── StunMessage.kt          # Data class for parsed STUN
│   │   │   │   │   └── StunAttribute.kt        # Sealed class for STUN attrs
│   │   │   │   │
│   │   │   │   ├── classifier/
│   │   │   │   │   ├── IpClassifier.kt         # Classification orchestrator
│   │   │   │   │   ├── MetaAsnFilter.kt        # Loads + checks meta_asn_cidr.txt
│   │   │   │   │   ├── SelfIpDetector.kt       # Fetches device public IP on start
│   │   │   │   │   └── IpClassification.kt     # Enum: SELF, META_INFRA, KNOWN_STUN, CANDIDATE
│   │   │   │   │
│   │   │   │   ├── geo/
│   │   │   │   │   ├── GeoIpResolver.kt        # Online + offline resolution logic
│   │   │   │   │   ├── IpInfoClient.kt         # OkHttp3 client for ipinfo.io
│   │   │   │   │   ├── MaxMindResolver.kt      # mmdb lookup via geoip2 SDK
│   │   │   │   │   └── GeoResult.kt            # Data class: city, state, country, ISP, lat, lng
│   │   │   │   │
│   │   │   │   ├── session/
│   │   │   │   │   ├── SessionManager.kt       # Business logic, emits Flows
│   │   │   │   │   ├── CaptureSession.kt       # Session data class
│   │   │   │   │   └── StunHit.kt              # Individual STUN event data class
│   │   │   │   │
│   │   │   │   ├── db/
│   │   │   │   │   ├── AppDatabase.kt          # Room database
│   │   │   │   │   ├── SessionDao.kt
│   │   │   │   │   ├── StunHitDao.kt
│   │   │   │   │   ├── GeoResultDao.kt
│   │   │   │   │   └── entities/               # Room @Entity classes
│   │   │   │   │
│   │   │   │   ├── repository/
│   │   │   │   │   ├── SessionRepository.kt
│   │   │   │   │   └── GeoRepository.kt
│   │   │   │   │
│   │   │   │   ├── ui/
│   │   │   │   │   ├── MainActivity.kt
│   │   │   │   │   ├── navigation/
│   │   │   │   │   │   └── AppNavGraph.kt      # Compose NavHost
│   │   │   │   │   ├── screens/
│   │   │   │   │   │   ├── HomeScreen.kt       # Entry point — Start/stop + quick stats
│   │   │   │   │   │   ├── LiveCaptureScreen.kt# Real-time IP cards during capture
│   │   │   │   │   │   ├── HistoryScreen.kt    # Past sessions list
│   │   │   │   │   │   ├── SessionDetailScreen.kt # Full session view + map
│   │   │   │   │   │   └── SettingsScreen.kt
│   │   │   │   │   ├── viewmodel/
│   │   │   │   │   │   ├── CaptureViewModel.kt
│   │   │   │   │   │   ├── HistoryViewModel.kt
│   │   │   │   │   │   └── SessionDetailViewModel.kt
│   │   │   │   │   ├── components/
│   │   │   │   │   │   ├── IpCard.kt           # Reusable IP display card
│   │   │   │   │   │   ├── GeoMapView.kt       # MapLibre composable wrapper
│   │   │   │   │   │   ├── ClassificationBadge.kt
│   │   │   │   │   │   └── CaptureButton.kt    # Big start/stop button
│   │   │   │   │   └── theme/
│   │   │   │   │       ├── Theme.kt            # Dark OSINT theme
│   │   │   │   │       ├── Color.kt
│   │   │   │   │       └── Type.kt
│   │   │   │   │
│   │   │   │   ├── di/
│   │   │   │   │   ├── AppModule.kt
│   │   │   │   │   ├── DatabaseModule.kt
│   │   │   │   │   └── NetworkModule.kt
│   │   │   │   │
│   │   │   │   └── util/
│   │   │   │       ├── ByteUtils.kt            # ByteArray helpers, XOR ops
│   │   │   │       ├── IpUtils.kt              # IP string <-> byte[] conversion
│   │   │   │       ├── CidrMatcher.kt          # CIDR range matching
│   │   │   │       ├── ExportUtils.kt          # JSON/text export logic
│   │   │   │       └── Constants.kt            # STUN magic cookie, port defs
│   │   │   │
│   │   │   ├── assets/
│   │   │   │   ├── meta_asn_cidr.txt           # Meta/WhatsApp IP ranges (see CLOUD.md)
│   │   │   │   └── GeoLite2-City.mmdb          # MaxMind offline DB (download separately)
│   │   │   │
│   │   │   └── AndroidManifest.xml
│   │   │
│   │   └── test/ + androidTest/
│   │       ├── parser/StunParserTest.kt
│   │       ├── classifier/IpClassifierTest.kt
│   │       └── geo/GeoIpResolverTest.kt
│   │
│   └── build.gradle.kts
│
├── build.gradle.kts
├── settings.gradle.kts
├── PRD.md
├── ARCHITECTURE.md
├── MEMORY.md
├── PROGRESS.md
└── CLOUD.md
```

---

## 8. Data Models

### CaptureSession
```kotlin
data class CaptureSession(
    val id: String,              // UUID
    val startedAt: Long,         // epoch ms
    val endedAt: Long?,          // null if still running
    val devicePublicIp: String,  // fetched at session start
    val stunHitCount: Int,
    val candidateCount: Int,
    val notes: String = ""
)
```

### StunHit
```kotlin
data class StunHit(
    val id: String,              // UUID
    val sessionId: String,
    val timestamp: Long,
    val srcIp: String,           // IP that sent the packet
    val dstIp: String,           // IP that received the packet
    val mappedIp: String?,       // XOR-MAPPED-ADDRESS result (null if not present)
    val mappedPort: Int?,
    val messageType: StunMessageType,
    val transactionId: String,   // hex string
    val classification: IpClassification  // of mappedIp or srcIp
)
```

### GeoResult
```kotlin
data class GeoResult(
    val ip: String,              // the resolved IP
    val city: String?,
    val region: String?,         // state/province
    val country: String?,
    val postal: String?,
    val latitude: Double?,
    val longitude: Double?,
    val org: String?,            // "AS12345 Some ISP"
    val asn: String?,
    val isp: String?,
    val resolvedAt: Long,
    val source: GeoSource        // IPINFO or MAXMIND_LOCAL
)
```

### IpClassification (enum)
```kotlin
enum class IpClassification {
    SELF,           // device's own public IP
    META_INFRA,     // WhatsApp/Meta servers
    KNOWN_STUN,     // Public STUN servers (Google, Cloudflare)
    CANDIDATE,      // Likely peer's IP
    UNKNOWN         // Not yet classified
}
```

---

## 9. Critical Implementation: STUN Parser Byte Layout

**STUN Header (20 bytes total):**

```
Byte 0-1:   Message Type (first 2 bits MUST be 00)
Byte 2-3:   Message Length (body length, not including header)
Byte 4-7:   Magic Cookie = 0x2112A442 (fixed — use this to identify STUN)
Byte 8-19:  Transaction ID (96 bits / 12 bytes)
```

**Magic cookie detection:**
```kotlin
const val STUN_MAGIC_COOKIE = 0x2112A442L

fun isStunPacket(udpPayload: ByteArray): Boolean {
    if (udpPayload.size < 20) return false
    // First 2 bits must be 00
    if ((udpPayload[0].toInt() and 0xC0) != 0) return false
    // Magic cookie at bytes 4-7
    val cookie = ((udpPayload[4].toLong() and 0xFF) shl 24) or
                 ((udpPayload[5].toLong() and 0xFF) shl 16) or
                 ((udpPayload[6].toLong() and 0xFF) shl 8)  or
                 (udpPayload[7].toLong() and 0xFF)
    return cookie == STUN_MAGIC_COOKIE
}
```

**XOR-MAPPED-ADDRESS parsing (attribute type 0x0020):**
```kotlin
fun parseXorMappedAddress(attrValue: ByteArray): Pair<String, Int>? {
    if (attrValue.size < 8) return null
    // byte 0: reserved, byte 1: family
    val family = attrValue[1].toInt() and 0xFF
    if (family != 0x01) return null  // IPv4 only in v1

    // Port: XOR with top 16 bits of magic cookie (0x2112)
    val rawPort = ((attrValue[2].toInt() and 0xFF) shl 8) or (attrValue[3].toInt() and 0xFF)
    val port = rawPort xor 0x2112

    // IP: XOR with magic cookie (0x2112A442)
    val magicBytes = byteArrayOf(0x21, 0x12, 0xA4.toByte(), 0x42)
    val ipBytes = ByteArray(4) { i -> (attrValue[4 + i].toInt() xor magicBytes[i].toInt()).toByte() }
    val ip = "${ipBytes[0].toInt() and 0xFF}.${ipBytes[1].toInt() and 0xFF}" +
             ".${ipBytes[2].toInt() and 0xFF}.${ipBytes[3].toInt() and 0xFF}"

    return Pair(ip, port)
}
```

**Attribute TLV walk:**
```kotlin
fun parseAttributes(body: ByteArray): List<StunAttribute> {
    val attrs = mutableListOf<StunAttribute>()
    var offset = 0
    while (offset + 4 <= body.size) {
        val type = ((body[offset].toInt() and 0xFF) shl 8) or (body[offset+1].toInt() and 0xFF)
        val length = ((body[offset+2].toInt() and 0xFF) shl 8) or (body[offset+3].toInt() and 0xFF)
        val paddedLength = (length + 3) and 3.inv()  // STUN attrs are 4-byte aligned
        if (offset + 4 + length > body.size) break
        val value = body.sliceArray(offset + 4 until offset + 4 + length)
        attrs.add(StunAttribute(type, length, value))
        offset += 4 + paddedLength
    }
    return attrs
}
```

---

## 10. IP Packet Parsing Pipeline

VpnService gives raw IP packets off the TUN fd. Pipeline:

```
TUN FileDescriptor
  → read() → ByteArray (raw IP packet)
  → IpPacketParser.parse()
      → check IP version (byte 0 >> 4 == 4 for IPv4)
      → check protocol field (byte 9 == 0x11 = UDP)
      → extract src IP (bytes 12-15), dst IP (bytes 16-19)
      → extract header length (byte 0 & 0x0F) * 4
  → UdpParser.parse(ipPayload)
      → extract src port (bytes 0-1), dst port (bytes 2-3)
      → extract UDP payload (byte 8 onwards)
  → StunParser.parse(udpPayload)
      → isStunPacket() check
      → parse header
      → parseAttributes()
      → extract XOR-MAPPED-ADDRESS
  → IpClassifier.classify(mappedIp)
  → GeoIpResolver.resolve(mappedIp) [if CANDIDATE]
  → SessionManager.recordHit(stunHit)
  → emit to UI via StateFlow
```

---

## 11. Meta ASN Filter List

File: `assets/meta_asn_cidr.txt`

Seed list (update regularly from ARIN/RIPE):
```
# Meta Platforms / WhatsApp Infrastructure
# Format: CIDR  # Comment
69.63.176.0/20
69.171.224.0/19
74.119.76.0/22
103.4.96.0/22
129.134.0.0/16
157.240.0.0/17
163.70.128.0/17
179.60.192.0/22
185.89.216.0/22
204.15.20.0/22
# WhatsApp TURN/STUN servers
31.13.64.0/18
31.13.24.0/21
```

`MetaAsnFilter` loads this file at app start, parses into a list of `InetAddress` range objects, and provides a `contains(ip: String): Boolean` method using `CidrMatcher`.

---

## 12. UI Design Spec

**Theme:** Dark / "hacker terminal" aesthetic — fitting for an OSINT tool
- Background: `#0A0E1A` (near-black with blue tint)
- Surface: `#111827`
- Primary accent: `#00E5FF` (cyan — security/terminal feel)
- CANDIDATE badge: `#00FF88` (green — "found it")
- META_INFRA badge: `#FF4444` (red — discard)
- SELF badge: `#FFAA00` (amber — it's you)
- Font: JetBrains Mono (monospace — matches terminal aesthetic)
- All text: left-aligned, no rounded cards everywhere

**Home Screen layout:**
```
┌─────────────────────────────┐
│  STUNMAP          [⚙ Settings]│
│                              │
│  ┌──────────────────────┐   │
│  │  ▶  START CAPTURE    │   │
│  └──────────────────────┘   │
│                              │
│  Last session: 2 candidates  │
│  [View History →]            │
└─────────────────────────────┘
```

**Live Capture Screen layout:**
```
┌─────────────────────────────┐
│  ● LIVE    00:02:34    [■ STOP]│
│                              │
│  STUN pkts: 47    Candidates: 2│
│                              │
│  ┌── CANDIDATE ─────────────┐│
│  │ 49.36.112.44             ││
│  │ Mumbai, Maharashtra, IN  ││
│  │ Jio (AS55836)            ││
│  │ [13.2°N, 77.5°E] [MAP]  ││
│  └──────────────────────────┘│
│                              │
│  ┌── META_INFRA ─────────┐  │
│  │ 157.240.22.35  [✕]    │  │
│  └───────────────────────┘  │
└─────────────────────────────┘
```

---

## 13. Dependency Configuration (build.gradle.kts)

```kotlin
dependencies {
    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.10.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.0")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Hilt DI
    implementation("com.google.dagger:hilt-android:2.51")
    kapt("com.google.dagger:hilt-compiler:2.51")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // MaxMind GeoIP2
    implementation("com.maxmind.geoip2:geoip2:4.2.0")

    // MapLibre (OpenStreetMap based, no API key)
    implementation("org.maplibre.gl:android-sdk:11.0.0")

    // Logging
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
```

---

## 14. Known Limitations & Mitigations

| Limitation | Impact | Mitigation |
|---|---|---|
| TURN fallback (no P2P) | No peer IP visible | Log TURN relay IP for reference; surface clearly in UI as "relay only" |
| CGNAT on mobile carriers | IP maps to carrier not individual | Show ISP + approximate region, label accuracy as "carrier-level" |
| VPN conflict | Only one VPN active at once | Detect active VPN on start, warn user clearly |
| Android battery optimization | Service killed mid-call | Implement proper foreground service; prompt user to disable battery optimization for app |
| WhatsApp updates | May change STUN behaviour | Architecture is protocol-based (RFC 5389) — not WhatsApp-specific. Future-proof. |
| IPv6 | Not handled in v1 | Log IPv6 STUN hits, surface as "IPv6 (unsupported in v1)" |

---

## 15. Testing Checklist

- [ ] StunParser correctly detects magic cookie
- [ ] XOR-MAPPED-ADDRESS un-XOR produces correct IP and port
- [ ] Attribute TLV walker handles malformed packets without crashing
- [ ] MetaAsnFilter correctly blocks all IPs in seed list
- [ ] CidrMatcher edge cases (boundary IPs, /32, /0)
- [ ] GeoIpResolver falls back to offline when no network
- [ ] VpnService starts/stops cleanly
- [ ] Foreground service survives 10+ minutes of background
- [ ] Session correctly persists across app restart
- [ ] Export produces valid JSON
- [ ] UI renders correctly on API 26 and API 34

---

*See ARCHITECTURE.md for detailed class interactions and threading model.*
*See MEMORY.md for project state and decisions log.*
*See PROGRESS.md for build task checklist.*
*See CLOUD.md for backend and ASN list update strategy.*
