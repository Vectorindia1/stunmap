# STUNMAP — Cloud & Backend Reference
**For Claude Code: all external services, API keys, and data sources used by this project.**

---

## 1. GeoIP — IPinfo.io (Online, Primary)

**URL:** `https://ipinfo.io/{ip}/json`
**Authentication:** Bearer token (optional for free tier, recommended for higher limits)
**Free tier:** 50,000 requests/month
**Rate limit:** ~1,000 requests/day without token; increase with free account

### Request
```
GET https://ipinfo.io/49.36.112.44/json
Authorization: Bearer {IPINFO_API_KEY}
```

### Response
```json
{
  "ip": "49.36.112.44",
  "city": "Mumbai",
  "region": "Maharashtra",
  "country": "IN",
  "loc": "19.0760,72.8777",
  "org": "AS55836 Reliance Jio Infocomm Limited",
  "postal": "400070",
  "timezone": "Asia/Kolkata"
}
```

### Parsing notes
- `loc` field: split on comma → `[latitude, longitude]`
- `org` field: split on first space → `ASN = "AS55836"`, `ISP = "Reliance Jio Infocomm Limited"`
- All fields can be missing/null — handle gracefully

### API Key Storage
- Store in Android `DataStore<Preferences>` (not hardcoded)
- Key: `"ipinfo_api_key"` (see SettingsRepository)
- User enters this in Settings screen
- If empty, skip online and go straight to MaxMind offline

### Getting an API key
1. Register free at https://ipinfo.io/signup
2. Copy token from dashboard
3. Enter in app Settings

---

## 2. GeoIP — MaxMind GeoLite2 (Offline, Fallback)

**File:** `GeoLite2-City.mmdb`
**Location in project:** `app/src/main/assets/GeoLite2-City.mmdb`
**License:** Creative Commons Attribution-ShareAlike 4.0 (free for any use, requires attribution)
**Update frequency:** Updated by MaxMind monthly — re-download periodically

### How to download
1. Register free at https://www.maxmind.com/en/geolite2/signup
2. Go to: Account → Download Files → GeoLite2 City → Download GZIP
3. Extract: `GeoLite2-City.mmdb`
4. Place at: `app/src/main/assets/GeoLite2-City.mmdb`

### Important
- File is ~60MB — add to `.gitignore` (do not commit to git)
- Add `GeoLite2-City.mmdb` to `.gitignore`
- Document download step in project README

### Java SDK
```kotlin
// Maven/Gradle: com.maxmind.geoip2:geoip2:4.2.0
val reader = DatabaseReader.Builder(mmdbFile).build()
val response = reader.city(InetAddress.getByName(ip))
val city = response.city.name
val region = response.mostSpecificSubdivision.name
val country = response.country.isoCode
val lat = response.location.latitude
val lng = response.location.longitude
val asn = response.traits.autonomousSystemNumber
val org = response.traits.autonomousSystemOrganization
```

### How MaxMindResolver should open the file
The .mmdb file is in `assets/` — cannot be accessed as a `File` directly. Workaround: copy to internal storage on first launch.

```kotlin
@Singleton
class MaxMindResolver @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val reader: DatabaseReader by lazy { initReader() }

    private fun initReader(): DatabaseReader {
        val mmdbFile = File(context.filesDir, "GeoLite2-City.mmdb")
        if (!mmdbFile.exists()) {
            context.assets.open("GeoLite2-City.mmdb").use { input ->
                FileOutputStream(mmdbFile).use { output -> input.copyTo(output) }
            }
        }
        return DatabaseReader.Builder(mmdbFile).build()
    }
}
```

---

## 3. Self IP Detection

**URL:** `https://ipinfo.io/ip`
**Method:** GET (no auth needed)
**Response:** Plain text IP string, e.g. `103.21.58.12`
**Timeout:** 5 seconds
**Used by:** `SelfIpDetector.kt` at session start

---

## 4. Meta ASN CIDR List

### Source (authoritative)
Meta/Facebook announces IP blocks through BGP. Authoritative sources:
- ARIN: https://search.arin.net/rdap/#q=facebook
- RIPE: https://apps.db.ripe.net/db-web-ui/query?searchtext=AS32934
- BGP.he.net: https://bgp.he.net/AS32934#_prefixes (human-readable)
- Raw prefix data: https://ipinfo.io/AS32934 (requires IPinfo account)

### Meta's Key ASNs
| ASN | Description |
|---|---|
| AS32934 | Facebook main infrastructure |
| AS63293 | WhatsApp Inc |
| AS54115 | Facebook CDN |
| AS44907 | Facebook |
| AS35995 | Meta Platforms |

### Seed CIDR List for `assets/meta_asn_cidr.txt`
```
# Meta Platforms / Facebook / WhatsApp Infrastructure
# Last updated: 2024
# Source: ARIN, RIPE, BGP routing tables
# Format: CIDR
# Lines starting with # are comments

# AS32934 - Facebook main
31.13.24.0/21
31.13.64.0/18
45.64.40.0/22
66.220.144.0/20
69.63.176.0/20
69.171.224.0/19
74.119.76.0/22
102.132.96.0/20
103.4.96.0/22
129.134.0.0/17
157.240.0.0/17
163.70.128.0/17
163.177.0.0/16
173.252.64.0/18
179.60.192.0/22
185.60.216.0/22
204.15.20.0/22

# AS63293 - WhatsApp
157.240.0.0/17
```

### Updating the list
1. Go to https://bgp.he.net/AS32934#_prefixes
2. Download or copy the prefix list
3. Do the same for AS63293, AS54115
4. Deduplicate and replace `assets/meta_asn_cidr.txt`
5. In app Settings → "Update Meta ASN List" button fetches from a URL you host

### Hosted update URL (optional)
Host the updated `meta_asn_cidr.txt` at a URL you control (GitHub raw, or a simple CDN).
The app fetches this URL when user taps "Update ASN List" in Settings.

Example:
```
https://raw.githubusercontent.com/veenu-infosec/stunmap-assets/main/meta_asn_cidr.txt
```

Store this URL in `Constants.kt`:
```kotlin
const val META_ASN_UPDATE_URL = "https://raw.githubusercontent.com/YOUR_USERNAME/stunmap-assets/main/meta_asn_cidr.txt"
```

When fetched, write to `context.filesDir/meta_asn_cidr.txt` (internal storage), and reload `MetaAsnFilter`.

**Priority on load:** Check internal storage first (user-updated), fall back to `assets/` (bundled seed).

---

## 5. MapLibre (No API Key Required)

MapLibre uses OpenStreetMap tiles. No API key or account required.

```kotlin
// In Application.onCreate():
MapLibre.getInstance(context)
```

Default tile server (public, free):
```
https://demotiles.maplibre.org/style.json
```

For better offline capability, consider using a self-hosted tile server or a MapTiler free account.

---

## 6. No Backend Server Required

STUNMAP v1.0 is fully client-side:
- All data stored locally in Room (SQLite)
- GeoIP resolved via IPinfo API (user's own API key) or local mmdb
- No server-side processing
- No data sent to any server except the GeoIP lookup for the target IP

**Privacy note:** The GeoIP API lookup sends the target's IP to IPinfo.io's servers. This is inherent to any IP geolocation service. For maximum privacy, use MaxMind offline only (no online lookups).

---

## 7. Build & Signing (Developer Notes)

### Debug build (sideload testing)
```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk

adb install app/build/outputs/apk/debug/app-debug.apk
```

### Release build
```bash
./gradlew assembleRelease
# Requires signing config in build.gradle.kts
```

### Signing config in build.gradle.kts
```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_PATH") ?: "stunmap.keystore")
            storePassword = System.getenv("KEYSTORE_PASS") ?: ""
            keyAlias = System.getenv("KEY_ALIAS") ?: "stunmap"
            keyPassword = System.getenv("KEY_PASS") ?: ""
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}
```

### ProGuard rules needed (`proguard-rules.pro`)
```
# MaxMind GeoIP2
-keep class com.maxmind.** { *; }
-dontwarn com.maxmind.**

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
```

---

## 8. Testing Without a Real WhatsApp Call

To test the STUN parser without making a live call, use these approaches:

### Option A: Pre-recorded STUN bytes (unit test)
Hardcode known STUN packet bytes in test. Example XOR-MAPPED-ADDRESS packet:
```kotlin
// Real STUN Binding Response with XOR-MAPPED-ADDRESS = 49.36.112.44:12345
val sampleStunBytes = byteArrayOf(
    0x01, 0x01,       // Message type: Binding Response (0x0101)
    0x00, 0x0C,       // Body length: 12 bytes
    0x21, 0x12.toByte(), 0xA4.toByte(), 0x42,  // Magic cookie
    0xB7.toByte(), 0xE7.toByte(), 0x13, 0x2D,  // Transaction ID (12 bytes)
    0x85.toByte(), 0x68, 0x24, 0x61,
    0x15, 0x73, 0x61, 0x09,
    // XOR-MAPPED-ADDRESS attribute
    0x00, 0x20,       // Attr type: XOR-MAPPED-ADDRESS
    0x00, 0x08,       // Attr length: 8
    0x00, 0x01,       // Reserved + Family (IPv4)
    0x30, 0x39,       // XOR'd port: 0x3039 XOR 0x2112 = 0x112b = 4395 (example)
    0x68, 0x22.toByte(), 0xD2.toByte(), 0x6E  // XOR'd IP
)
```

### Option B: Local STUN server
Run a local STUN server (e.g., `coturn`) and make a test WebRTC connection from another device or browser.

### Option C: tcpdump on test call
Make a real WhatsApp call to a test number, capture with `tcpdump` on a Linux box running the same WiFi network, save as PCAP, extract STUN bytes from Wireshark, use as test vector.
