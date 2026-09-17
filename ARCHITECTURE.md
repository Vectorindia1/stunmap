# STUNMAP — Technical Architecture
**For Claude Code (senior developer reference)**

---

## 1. High-Level Architecture

STUNMAP follows Clean Architecture with MVVM on the presentation layer.

```
┌────────────────────────────────────────────────────────────┐
│  Presentation Layer (Jetpack Compose + ViewModels)         │
│  HomeScreen | LiveCaptureScreen | HistoryScreen | Detail   │
└──────────────────┬─────────────────────────────────────────┘
                   │ observes Flow<T>
┌──────────────────▼─────────────────────────────────────────┐
│  Domain Layer (Use Cases / Session Manager)                │
│  SessionManager | IpClassifier | StunParser                │
└──────────────────┬─────────────────────────────────────────┘
                   │
┌──────────────────▼─────────────────────────────────────────┐
│  Data Layer (Repositories)                                 │
│  SessionRepository | GeoRepository                        │
│  ┌──────────────────┐  ┌──────────────────────────────┐   │
│  │ Room (local DB)  │  │ GeoIpResolver                │   │
│  │ SessionDao       │  │  ├── IpInfoClient (OkHttp)   │   │
│  │ StunHitDao       │  │  └── MaxMindResolver (mmdb)  │   │
│  │ GeoResultDao     │  │                              │   │
│  └──────────────────┘  └──────────────────────────────┘   │
└────────────────────────────────────────────────────────────┘
                   ▲
                   │ StunHit events via SharedFlow
┌──────────────────┴─────────────────────────────────────────┐
│  System Layer (Android VpnService)                         │
│  StunCaptureService → PacketForwarder → IpPacketParser     │
│                                       → UdpParser          │
│                                       → StunParser         │
└────────────────────────────────────────────────────────────┘
```

---

## 2. Threading Model

Everything is async. No blocking operations on the main thread.

```
Main Thread (UI):
  - Compose rendering
  - ViewModel.uiState collection

IO Dispatcher (Dispatchers.IO):
  - VpnService TUN read loop
  - OkHttp GeoIP API calls
  - Room database reads/writes
  - MMDB file reads

Default Dispatcher (Dispatchers.Default):
  - Packet parsing (CPU-bound)
  - CIDR matching
  - ByteArray XOR operations

Service Scope:
  - StunCaptureService has its own CoroutineScope (SupervisorJob + Dispatchers.IO)
  - Cancelled on service destroy
```

---

## 3. VpnService Detailed Implementation

### StunCaptureService.kt

```kotlin
@AndroidEntryPoint
class StunCaptureService : VpnService() {

    @Inject lateinit var sessionManager: SessionManager
    @Inject lateinit var ipClassifier: IpClassifier
    @Inject lateinit var stunParser: StunParser
    @Inject lateinit var ipPacketParser: IpPacketParser

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var vpnInterface: ParcelFileDescriptor? = null
    private var captureJob: Job? = null

    companion object {
        const val ACTION_START = "com.stunmap.START_CAPTURE"
        const val ACTION_STOP  = "com.stunmap.STOP_CAPTURE"

        fun start(context: Context) =
            context.startForegroundService(Intent(context, StunCaptureService::class.java).apply {
                action = ACTION_START
            })

        fun stop(context: Context) =
            context.startService(Intent(context, StunCaptureService::class.java).apply {
                action = ACTION_STOP
            })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startCapture()
            ACTION_STOP  -> stopCapture()
        }
        return START_STICKY
    }

    private fun startCapture() {
        startForeground(NOTIF_ID, ServiceNotification.build(this))

        val builder = Builder()
            .setSession("STUNMAP Capture")
            .addAddress("10.0.0.2", 32)       // TUN interface IP (arbitrary)
            .addRoute("0.0.0.0", 0)            // Route ALL traffic through TUN
            .addDnsServer("8.8.8.8")
            .setMtu(1500)
            .setBlocking(true)                 // Use blocking reads (simpler)

        vpnInterface = builder.establish()
            ?: throw IllegalStateException("VPN permission not granted")

        captureJob = serviceScope.launch {
            sessionManager.startSession()
            readPacketLoop()
        }
    }

    private suspend fun readPacketLoop() {
        val fd = vpnInterface!!.fileDescriptor
        val inputStream = FileInputStream(fd)
        val outputStream = FileOutputStream(fd)
        val buffer = ByteArray(32767)  // Max IP packet size

        while (isActive) {
            val length = withContext(Dispatchers.IO) { inputStream.read(buffer) }
            if (length <= 0) continue

            val packet = buffer.copyOf(length)

            // Forward packet immediately so device stays connected
            withContext(Dispatchers.IO) { outputStream.write(packet) }

            // Parse on Default dispatcher (CPU work)
            withContext(Dispatchers.Default) {
                processPacket(packet)
            }
        }
    }

    private suspend fun processPacket(packet: ByteArray) {
        val ipPacket = ipPacketParser.parse(packet) ?: return
        if (!ipPacket.isUdp) return

        val udpPayload = ipPacket.payload ?: return
        if (!stunParser.isStunPacket(udpPayload)) return

        val stunMessage = stunParser.parse(udpPayload, ipPacket.srcIp, ipPacket.dstIp) ?: return
        sessionManager.recordStunHit(stunMessage)
    }

    private fun stopCapture() {
        captureJob?.cancel()
        vpnInterface?.close()
        vpnInterface = null
        sessionManager.endSession()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onRevoke() {
        // Called by Android if user revokes VPN permission while running
        stopCapture()
    }
}
```

---

## 4. STUN Parser Detailed Implementation

### StunParser.kt

```kotlin
class StunParser @Inject constructor() {

    companion object {
        const val MAGIC_COOKIE = 0x2112A442L
        const val ATTR_XOR_MAPPED_ADDRESS = 0x0020
        const val ATTR_MAPPED_ADDRESS     = 0x0001
        const val ATTR_SOFTWARE           = 0x8022

        const val MSG_BINDING_REQUEST  = 0x0001
        const val MSG_BINDING_RESPONSE = 0x0101
        const val MSG_BINDING_ERROR    = 0x0111
    }

    fun isStunPacket(udpPayload: ByteArray): Boolean {
        if (udpPayload.size < 20) return false
        if ((udpPayload[0].toInt() and 0xC0) != 0) return false
        val cookie = udpPayload.getLong4(4)
        return cookie == MAGIC_COOKIE
    }

    fun parse(udpPayload: ByteArray, srcIp: String, dstIp: String): StunMessage? {
        if (!isStunPacket(udpPayload)) return null

        val messageType = udpPayload.getShort(0)
        val bodyLength  = udpPayload.getShort(2)
        val transactionId = udpPayload.sliceArray(8..19).toHexString()

        if (udpPayload.size < 20 + bodyLength) return null
        val body = udpPayload.sliceArray(20 until 20 + bodyLength)

        val attributes = parseAttributes(body)

        var xorMappedIp: String? = null
        var xorMappedPort: Int? = null
        var mappedIp: String? = null
        var mappedPort: Int? = null

        for (attr in attributes) {
            when (attr.type) {
                ATTR_XOR_MAPPED_ADDRESS -> {
                    parseXorMappedAddress(attr.value)?.let { (ip, port) ->
                        xorMappedIp = ip; xorMappedPort = port
                    }
                }
                ATTR_MAPPED_ADDRESS -> {
                    parseMappedAddress(attr.value)?.let { (ip, port) ->
                        mappedIp = ip; mappedPort = port
                    }
                }
            }
        }

        // Prefer XOR-MAPPED-ADDRESS over legacy MAPPED-ADDRESS
        val resolvedIp   = xorMappedIp ?: mappedIp
        val resolvedPort = xorMappedPort ?: mappedPort

        return StunMessage(
            srcIp = srcIp,
            dstIp = dstIp,
            messageType = messageType,
            transactionId = transactionId,
            mappedIp = resolvedIp,
            mappedPort = resolvedPort,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun parseXorMappedAddress(value: ByteArray): Pair<String, Int>? {
        if (value.size < 8) return null
        val family = value[1].toInt() and 0xFF
        if (family != 0x01) return null  // IPv4 only in v1

        val rawPort = value.getShort(2)
        val port = rawPort xor 0x2112

        val magicBytes = byteArrayOf(0x21, 0x12, 0xA4.toByte(), 0x42)
        val ipBytes = ByteArray(4) { i ->
            (value[4 + i].toInt() xor (magicBytes[i].toInt() and 0xFF)).toByte()
        }

        return Pair(ipBytes.toIpString(), port)
    }

    private fun parseMappedAddress(value: ByteArray): Pair<String, Int>? {
        if (value.size < 8) return null
        val family = value[1].toInt() and 0xFF
        if (family != 0x01) return null
        val port = value.getShort(2)
        val ip = value.sliceArray(4..7).toIpString()
        return Pair(ip, port)
    }

    private fun parseAttributes(body: ByteArray): List<RawStunAttribute> {
        val attrs = mutableListOf<RawStunAttribute>()
        var offset = 0
        while (offset + 4 <= body.size) {
            val type   = body.getShort(offset)
            val length = body.getShort(offset + 2)
            val padded = (length + 3) and 3.inv()
            if (offset + 4 + length > body.size) break
            val value = body.sliceArray(offset + 4 until offset + 4 + length)
            attrs.add(RawStunAttribute(type, length, value))
            offset += 4 + padded
        }
        return attrs
    }
}

// Extension functions (in ByteUtils.kt)
fun ByteArray.getShort(offset: Int): Int =
    ((this[offset].toInt() and 0xFF) shl 8) or (this[offset + 1].toInt() and 0xFF)

fun ByteArray.getLong4(offset: Int): Long =
    ((this[offset].toLong()   and 0xFF) shl 24) or
    ((this[offset+1].toLong() and 0xFF) shl 16) or
    ((this[offset+2].toLong() and 0xFF) shl 8)  or
    (this[offset+3].toLong()  and 0xFF)

fun ByteArray.toIpString(): String =
    "${this[0].toInt() and 0xFF}.${this[1].toInt() and 0xFF}" +
    ".${this[2].toInt() and 0xFF}.${this[3].toInt() and 0xFF}"

fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }
```

---

## 5. Session Manager & Flow Architecture

```kotlin
@Singleton
class SessionManager @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val ipClassifier: IpClassifier,
    private val geoIpResolver: GeoIpResolver,
    private val scope: CoroutineScope  // Application-scoped via Hilt
) {
    // Exposed to UI
    private val _currentSession = MutableStateFlow<CaptureSession?>(null)
    val currentSession: StateFlow<CaptureSession?> = _currentSession.asStateFlow()

    private val _stunHits = MutableSharedFlow<StunHit>(replay = 0, extraBufferCapacity = 64)
    val stunHits: SharedFlow<StunHit> = _stunHits.asSharedFlow()

    private val _candidateIps = MutableStateFlow<List<IpGeoEntry>>(emptyList())
    val candidateIps: StateFlow<List<IpGeoEntry>> = _candidateIps.asStateFlow()

    suspend fun startSession() {
        val selfIp = SelfIpDetector.detect()  // HTTP call to ipinfo.io/ip
        val session = CaptureSession(
            id = UUID.randomUUID().toString(),
            startedAt = System.currentTimeMillis(),
            devicePublicIp = selfIp ?: "unknown"
        )
        _currentSession.value = session
        sessionRepository.insertSession(session)
        _candidateIps.value = emptyList()
    }

    suspend fun recordStunHit(message: StunMessage) {
        val mappedIp = message.mappedIp ?: return
        val classification = ipClassifier.classify(mappedIp)
        val currentSession = _currentSession.value ?: return

        val hit = StunHit(
            id = UUID.randomUUID().toString(),
            sessionId = currentSession.id,
            timestamp = message.timestamp,
            srcIp = message.srcIp,
            dstIp = message.dstIp,
            mappedIp = mappedIp,
            mappedPort = message.mappedPort,
            messageType = StunMessageType.fromCode(message.messageType),
            transactionId = message.transactionId,
            classification = classification
        )

        sessionRepository.insertStunHit(hit)
        _stunHits.emit(hit)

        if (classification == IpClassification.CANDIDATE) {
            resolveAndAddCandidate(mappedIp)
        }
    }

    private suspend fun resolveAndAddCandidate(ip: String) {
        // Avoid duplicate resolution
        if (_candidateIps.value.any { it.ip == ip }) return

        // Add placeholder immediately
        val entry = IpGeoEntry(ip = ip, geoResult = null)
        _candidateIps.update { it + entry }

        // Resolve asynchronously
        scope.launch {
            val geo = geoIpResolver.resolve(ip)
            _candidateIps.update { list ->
                list.map { if (it.ip == ip) it.copy(geoResult = geo) else it }
            }
            geo?.let { sessionRepository.insertGeoResult(it) }
        }
    }

    suspend fun endSession() {
        val session = _currentSession.value ?: return
        val updated = session.copy(
            endedAt = System.currentTimeMillis(),
            candidateCount = _candidateIps.value.size
        )
        sessionRepository.updateSession(updated)
        _currentSession.value = null
    }
}
```

---

## 6. GeoIP Resolver (Online + Offline Fallback)

```kotlin
@Singleton
class GeoIpResolver @Inject constructor(
    private val ipInfoClient: IpInfoClient,
    private val maxMindResolver: MaxMindResolver,
    private val geoResultDao: GeoResultDao
) {
    suspend fun resolve(ip: String): GeoResult? {
        // 1. Check Room cache first
        geoResultDao.getByIp(ip)?.let { return it }

        // 2. Try online
        try {
            val result = ipInfoClient.resolve(ip)
            if (result != null) {
                geoResultDao.insert(result)
                return result
            }
        } catch (e: Exception) {
            Timber.w(e, "IPInfo API failed for $ip, falling back to offline")
        }

        // 3. Fallback to MaxMind local DB
        return try {
            maxMindResolver.resolve(ip)?.also { geoResultDao.insert(it) }
        } catch (e: Exception) {
            Timber.e(e, "MaxMind resolver also failed for $ip")
            null
        }
    }
}
```

---

## 7. CaptureViewModel (UI ↔ Domain bridge)

```kotlin
@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val sessionManager: SessionManager
) : ViewModel() {

    val captureState: StateFlow<CaptureUiState> = combine(
        sessionManager.currentSession,
        sessionManager.candidateIps,
        sessionManager.stunHits.scan(0) { acc, _ -> acc + 1 }  // hit counter
    ) { session, candidates, hitCount ->
        CaptureUiState(
            isCapturing = session != null,
            duration = session?.let { System.currentTimeMillis() - it.startedAt } ?: 0L,
            stunHitCount = hitCount,
            candidateIps = candidates
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CaptureUiState())

    fun onStartCapture(context: Context) {
        StunCaptureService.start(context)
    }

    fun onStopCapture(context: Context) {
        StunCaptureService.stop(context)
    }
}

data class CaptureUiState(
    val isCapturing: Boolean = false,
    val duration: Long = 0L,
    val stunHitCount: Int = 0,
    val candidateIps: List<IpGeoEntry> = emptyList()
)
```

---

## 8. Room Database Schema

```kotlin
@Database(
    entities = [
        SessionEntity::class,
        StunHitEntity::class,
        GeoResultEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun stunHitDao(): StunHitDao
    abstract fun geoResultDao(): GeoResultDao

    companion object {
        const val DB_NAME = "stunmap.db"
    }
}
```

Key relations:
- `StunHitEntity.sessionId` → FK to `SessionEntity.id`
- `GeoResultEntity.ip` → lookup key, referenced by `StunHitEntity.mappedIp`

---

## 9. Hilt Dependency Graph

```
@ApplicationComponent
├── AppDatabase (singleton)
│   ├── SessionDao
│   ├── StunHitDao
│   └── GeoResultDao
├── OkHttpClient (singleton)
├── IpInfoClient (singleton) → OkHttpClient
├── MaxMindResolver (singleton) → assets/GeoLite2-City.mmdb
├── GeoIpResolver (singleton) → IpInfoClient + MaxMindResolver + GeoResultDao
├── MetaAsnFilter (singleton) → assets/meta_asn_cidr.txt
├── IpClassifier (singleton) → MetaAsnFilter + SelfIpDetector
├── StunParser (singleton)
├── IpPacketParser (singleton)
├── SessionRepository (singleton) → SessionDao + StunHitDao + GeoResultDao
├── SessionManager (singleton) → SessionRepository + IpClassifier + GeoIpResolver
└── ApplicationScope (CoroutineScope, singleton)
```

---

## 10. Foreground Service Notification Channel

```kotlin
object ServiceNotification {
    const val CHANNEL_ID = "stunmap_capture"
    const val NOTIF_ID = 1001

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "STUNMAP Capture",
            NotificationManager.IMPORTANCE_LOW  // Silent, no sound
        ).apply {
            description = "Active network capture session"
        }
        context.getSystemService<NotificationManager>()?.createNotificationChannel(channel)
    }

    fun build(context: Context): Notification {
        val stopIntent = PendingIntent.getService(
            context, 0,
            Intent(context, StunCaptureService::class.java).apply {
                action = StunCaptureService.ACTION_STOP
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("STUNMAP Active")
            .setContentText("Capturing STUN traffic...")
            .setSmallIcon(R.drawable.ic_radar)
            .setOngoing(true)
            .addAction(R.drawable.ic_stop, "Stop", stopIntent)
            .setSilent(true)
            .build()
    }
}
```

---

## 11. IP Parsing — IPv4 Header Reference

```
Byte 0:     Version (top 4 bits) + IHL (bottom 4 bits)
            IHL * 4 = header length in bytes (minimum 20, max 60)
Byte 1:     DSCP + ECN
Byte 2-3:   Total Length
Byte 4-5:   Identification
Byte 6-7:   Flags + Fragment Offset
Byte 8:     TTL
Byte 9:     Protocol (0x11 = UDP, 0x06 = TCP)
Byte 10-11: Header Checksum
Byte 12-15: Source IP Address
Byte 16-19: Destination IP Address
Byte 20+:   Options (if IHL > 5) + Payload
```

```kotlin
data class ParsedIpPacket(
    val srcIp: String,
    val dstIp: String,
    val isUdp: Boolean,
    val payload: ByteArray?
)

class IpPacketParser @Inject constructor() {
    fun parse(raw: ByteArray): ParsedIpPacket? {
        if (raw.size < 20) return null
        val version = (raw[0].toInt() ushr 4) and 0xF
        if (version != 4) return null  // IPv4 only

        val ihl = (raw[0].toInt() and 0xF) * 4  // header length
        val protocol = raw[9].toInt() and 0xFF
        val srcIp = raw.sliceArray(12..15).toIpString()
        val dstIp = raw.sliceArray(16..19).toIpString()

        val isUdp = protocol == 0x11
        val payload = if (raw.size > ihl) raw.sliceArray(ihl until raw.size) else null

        return ParsedIpPacket(srcIp, dstIp, isUdp, payload)
    }
}
```

**UDP Header (8 bytes):**
```
Byte 0-1: Source Port
Byte 2-3: Destination Port
Byte 4-5: Length
Byte 6-7: Checksum
Byte 8+:  Payload
```

```kotlin
class UdpParser @Inject constructor() {
    fun getPayload(udpDatagram: ByteArray): ByteArray? {
        if (udpDatagram.size <= 8) return null
        return udpDatagram.sliceArray(8 until udpDatagram.size)
    }
}
```

---

## 12. Edge Cases to Handle

1. **Multiple STUN exchanges per call** — WhatsApp may do ICE restarts (network change, WiFi→mobile handoff). Same call → multiple STUN exchanges. Transaction ID ensures you don't double-count.

2. **Fragmented UDP** — Very rare for STUN (packets are small). Log a warning and skip if IP fragment flag is set.

3. **Multiple peer candidates** — On rare network configs, multiple CANDIDATE IPs appear. Show all of them ranked by first-seen time.

4. **Service restart** — If Android kills the service, the active session should be marked as "interrupted" on next DB load. Implement `onTaskRemoved()` in service.

5. **Malformed STUN** — Any parsing exception must be caught and silently logged. Never crash the packet loop.

6. **IP `0.0.0.0`** — Invalid address. Filter out immediately after XOR un-mask.

7. **Loopback (127.x.x.x) and private ranges** — Filter: `10.x.x.x`, `172.16-31.x.x`, `192.168.x.x` — these are never valid peer IPs from an internet call.
