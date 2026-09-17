package com.stunmap.session

import com.stunmap.classifier.IpClassification
import com.stunmap.classifier.IpClassifier
import com.stunmap.classifier.SelfIpDetector
import com.stunmap.geo.GeoIpResolver
import com.stunmap.geo.GeoResult
import com.stunmap.geo.GeoSource
import com.stunmap.parser.StunMessage
import com.stunmap.repository.SessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionManagerTest {

    private lateinit var sessionRepository: SessionRepository
    private lateinit var ipClassifier: IpClassifier
    private lateinit var selfIpDetector: SelfIpDetector
    private lateinit var geoIpResolver: GeoIpResolver
    private lateinit var testScope: TestScope
    private lateinit var sessionManager: SessionManager

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        sessionRepository = mockk(relaxed = true)
        ipClassifier = mockk(relaxed = true)
        selfIpDetector = mockk(relaxed = true)
        geoIpResolver = mockk(relaxed = true)
        testScope = TestScope(testDispatcher)

        sessionManager = SessionManager(
            sessionRepository = sessionRepository,
            ipClassifier = ipClassifier,
            selfIpDetector = selfIpDetector,
            geoIpResolver = geoIpResolver,
            appScope = testScope
        )
    }

    @Test
    fun `startSession creates session and persists it`() = testScope.runTest {
        coEvery { selfIpDetector.detect() } returns "1.2.3.4"

        sessionManager.startSession()

        val session = sessionManager.currentSession.value
        assertNotNull(session)
        assertEquals("1.2.3.4", session!!.devicePublicIp)
        assertEquals(0, session.stunHitCount)
        assertEquals(0, session.candidateCount)
        assertTrue(session.isActive)
        coVerify(exactly = 1) { sessionRepository.insertSession(any()) }
    }

    @Test
    fun `startSession with null self IP uses unknown`() = testScope.runTest {
        coEvery { selfIpDetector.detect() } returns null

        sessionManager.startSession()

        assertEquals("unknown", sessionManager.currentSession.value?.devicePublicIp)
    }

    @Test
    fun `startSession resets prior state`() = testScope.runTest {
        coEvery { selfIpDetector.detect() } returns "1.2.3.4"
        coEvery { ipClassifier.classify(any()) } returns IpClassification.CANDIDATE
        coEvery { geoIpResolver.resolve(any()) } returns null

        sessionManager.startSession()
        sessionManager.recordStunHit(makeMessage("49.36.0.1"))
        advanceUntilIdle()

        assertEquals(1, sessionManager.candidateIps.value.size)

        // Start a new session — state should reset
        sessionManager.startSession()
        assertEquals(0, sessionManager.candidateIps.value.size)
        assertEquals(0, sessionManager.stunHitCount.value)
    }

    @Test
    fun `recordStunHit with no active session is ignored`() = testScope.runTest {
        // No startSession called
        sessionManager.recordStunHit(makeMessage("49.36.0.1"))

        coVerify(exactly = 0) { sessionRepository.insertStunHit(any()) }
    }

    @Test
    fun `recordStunHit with private IP is ignored`() = testScope.runTest {
        coEvery { selfIpDetector.detect() } returns "1.2.3.4"
        sessionManager.startSession()

        // 192.168.1.1 — isValidPublicIp() returns false
        sessionManager.recordStunHit(makeMessage("192.168.1.1"))

        coVerify(exactly = 0) { sessionRepository.insertStunHit(any()) }
        assertEquals(0, sessionManager.stunHitCount.value)
    }

    @Test
    fun `recordStunHit with null mappedIp is ignored`() = testScope.runTest {
        coEvery { selfIpDetector.detect() } returns "1.2.3.4"
        sessionManager.startSession()

        val noIpMsg = makeMessage(ip = null)
        sessionManager.recordStunHit(noIpMsg)

        coVerify(exactly = 0) { sessionRepository.insertStunHit(any()) }
    }

    @Test
    fun `recordStunHit CANDIDATE adds to candidateIps and triggers geo resolve`() = testScope.runTest {
        coEvery { selfIpDetector.detect() } returns "1.2.3.4"
        coEvery { ipClassifier.classify("49.36.0.1") } returns IpClassification.CANDIDATE
        coEvery { geoIpResolver.resolve("49.36.0.1") } returns GeoResult(
            ip = "49.36.0.1", city = "Pune", country = "IN", source = GeoSource.IPINFO.name
        )

        sessionManager.startSession()
        sessionManager.recordStunHit(makeMessage("49.36.0.1"))
        advanceUntilIdle()

        val candidates = sessionManager.candidateIps.value
        assertEquals(1, candidates.size)
        assertEquals("49.36.0.1", candidates[0].ip)
        assertEquals("Pune", candidates[0].geoResult?.city)
        assertEquals(1, sessionManager.stunHitCount.value)
    }

    @Test
    fun `recordStunHit META_INFRA records hit but does not add to candidateIps`() = testScope.runTest {
        coEvery { selfIpDetector.detect() } returns "1.2.3.4"
        coEvery { ipClassifier.classify("157.240.22.35") } returns IpClassification.META_INFRA

        sessionManager.startSession()
        sessionManager.recordStunHit(makeMessage("157.240.22.35"))

        assertEquals(0, sessionManager.candidateIps.value.size)
        assertEquals(1, sessionManager.stunHitCount.value)
        coVerify(exactly = 1) { sessionRepository.insertStunHit(any()) }
    }

    @Test
    fun `duplicate CANDIDATE IP is not added twice to candidateIps`() = testScope.runTest {
        coEvery { selfIpDetector.detect() } returns "1.2.3.4"
        coEvery { ipClassifier.classify("49.36.0.1") } returns IpClassification.CANDIDATE
        coEvery { geoIpResolver.resolve("49.36.0.1") } returns null

        sessionManager.startSession()
        sessionManager.recordStunHit(makeMessage("49.36.0.1"))
        sessionManager.recordStunHit(makeMessage("49.36.0.1"))
        advanceUntilIdle()

        assertEquals(1, sessionManager.candidateIps.value.size)
        assertEquals(2, sessionManager.stunHitCount.value)
    }

    @Test
    fun `endSession sets endedAt and clears current session`() = testScope.runTest {
        coEvery { selfIpDetector.detect() } returns "1.2.3.4"

        sessionManager.startSession()
        assertNotNull(sessionManager.currentSession.value)

        sessionManager.endSession()

        assertNull(sessionManager.currentSession.value)
        coVerify(exactly = 1) { sessionRepository.updateSession(match { it.endedAt != null }) }
    }

    @Test
    fun `markSessionInterrupted sets interrupted flag`() = testScope.runTest {
        coEvery { selfIpDetector.detect() } returns "1.2.3.4"

        sessionManager.startSession()
        sessionManager.markSessionInterrupted()

        assertNull(sessionManager.currentSession.value)
        coVerify(exactly = 1) { sessionRepository.updateSession(match { it.interrupted }) }
    }

    // --- helpers ---

    private fun makeMessage(ip: String?): StunMessage = StunMessage(
        srcIp = "10.0.0.2",
        dstIp = "8.8.8.8",
        messageType = 0x0101,
        transactionId = "aabbccddeeff00112233445566778899".take(24),
        mappedIp = ip,
        mappedPort = 12345,
        timestamp = System.currentTimeMillis()
    )
}
