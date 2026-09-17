package com.stunmap.session

import com.stunmap.classifier.IpClassification
import com.stunmap.classifier.IpClassifier
import com.stunmap.classifier.SelfIpDetector
import com.stunmap.db.entities.toEntity
import com.stunmap.geo.GeoIpResolver
import com.stunmap.geo.GeoResult
import com.stunmap.parser.StunMessage
import com.stunmap.parser.StunMessageType
import com.stunmap.repository.SessionRepository
import com.stunmap.util.isValidPublicIp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val ipClassifier: IpClassifier,
    private val selfIpDetector: SelfIpDetector,
    private val geoIpResolver: GeoIpResolver,
    private val appScope: CoroutineScope
) {
    private val _currentSession = MutableStateFlow<CaptureSession?>(null)
    val currentSession: StateFlow<CaptureSession?> = _currentSession.asStateFlow()

    private val _stunHits = MutableSharedFlow<StunHit>(replay = 0, extraBufferCapacity = 128)
    val stunHits: SharedFlow<StunHit> = _stunHits.asSharedFlow()

    private val _candidateIps = MutableStateFlow<List<IpGeoEntry>>(emptyList())
    val candidateIps: StateFlow<List<IpGeoEntry>> = _candidateIps.asStateFlow()

    private val _stunHitCount = MutableStateFlow(0)
    val stunHitCount: StateFlow<Int> = _stunHitCount.asStateFlow()

    suspend fun startSession() {
        selfIpDetector.invalidate()
        val selfIp = selfIpDetector.detect()

        val session = CaptureSession(
            id = UUID.randomUUID().toString(),
            startedAt = System.currentTimeMillis(),
            devicePublicIp = selfIp ?: "unknown"
        )
        _currentSession.value = session
        _candidateIps.value = emptyList()
        _stunHitCount.value = 0
        sessionRepository.insertSession(session)
        Timber.d("Session started: ${session.id}, selfIp=$selfIp")
    }

    suspend fun recordStunHit(message: StunMessage) {
        val mappedIp = message.mappedIp ?: return
        if (!mappedIp.isValidPublicIp()) return

        val classification = ipClassifier.classify(mappedIp)
        val current = _currentSession.value ?: return

        val hit = StunHit(
            id = UUID.randomUUID().toString(),
            sessionId = current.id,
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
        _stunHitCount.update { it + 1 }

        // Update session hit count in DB
        val updatedSession = current.copy(stunHitCount = _stunHitCount.value)
        _currentSession.value = updatedSession
        sessionRepository.updateSession(updatedSession)

        if (classification == IpClassification.CANDIDATE) {
            resolveAndAddCandidate(mappedIp)
        }
    }

    private fun resolveAndAddCandidate(ip: String) {
        if (_candidateIps.value.any { it.ip == ip }) return

        val entry = IpGeoEntry(ip = ip, geoResult = null)
        _candidateIps.update { it + entry }

        val current = _currentSession.value ?: return
        val updatedSession = current.copy(candidateCount = _candidateIps.value.size)
        _currentSession.value = updatedSession

        appScope.launch {
            try {
                val geo = geoIpResolver.resolve(ip)
                _candidateIps.update { list ->
                    list.map { if (it.ip == ip) it.copy(geoResult = geo) else it }
                }
                if (geo != null) {
                    sessionRepository.insertGeoResult(geo)
                }
                Timber.d("Geo resolved for $ip: ${geo?.city}, ${geo?.country}")
            } catch (e: Exception) {
                Timber.w(e, "Geo resolution failed for $ip")
            }
        }
    }

    suspend fun endSession() {
        val session = _currentSession.value ?: return
        val updated = session.copy(
            endedAt = System.currentTimeMillis(),
            candidateCount = _candidateIps.value.size,
            stunHitCount = _stunHitCount.value
        )
        sessionRepository.updateSession(updated)
        _currentSession.value = null
        Timber.d("Session ended: ${session.id}")
    }

    suspend fun markSessionInterrupted() {
        val session = _currentSession.value ?: return
        val updated = session.copy(
            endedAt = System.currentTimeMillis(),
            interrupted = true,
            candidateCount = _candidateIps.value.size
        )
        sessionRepository.updateSession(updated)
        _currentSession.value = null
    }
}
