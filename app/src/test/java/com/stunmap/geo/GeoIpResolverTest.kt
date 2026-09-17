package com.stunmap.geo

import com.stunmap.db.GeoResultDao
import com.stunmap.db.entities.GeoResultEntity
import com.stunmap.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GeoIpResolverTest {

    private lateinit var ipInfoClient: IpInfoClient
    private lateinit var maxMindResolver: MaxMindResolver
    private lateinit var geoResultDao: GeoResultDao
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var resolver: GeoIpResolver

    private val testIp = "49.36.112.44"
    private val cachedEntity = GeoResultEntity(
        ip = testIp, city = "Mumbai", region = "Maharashtra", country = "IN",
        postal = null, latitude = 19.07, longitude = 72.87,
        org = "AS18209 Reliance", asn = "AS18209", isp = "Reliance",
        resolvedAt = 1000L, source = GeoSource.IPINFO.name
    )
    private val onlineResult = GeoResult(
        ip = testIp, city = "Mumbai", country = "IN",
        resolvedAt = 2000L, source = GeoSource.IPINFO.name
    )
    private val offlineResult = GeoResult(
        ip = testIp, city = "Mumbai", country = "IN",
        resolvedAt = 3000L, source = GeoSource.MAXMIND_LOCAL.name
    )

    @Before
    fun setUp() {
        ipInfoClient = mockk(relaxed = true)
        maxMindResolver = mockk(relaxed = true)
        geoResultDao = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)
        resolver = GeoIpResolver(ipInfoClient, maxMindResolver, geoResultDao, settingsRepository)
    }

    @Test
    fun `cache hit returns entity without network calls`() = runTest {
        coEvery { geoResultDao.getByIp(testIp) } returns cachedEntity

        val result = resolver.resolve(testIp)

        assertNotNull(result)
        assertEquals(testIp, result!!.ip)
        assertEquals("Mumbai", result.city)
        coVerify(exactly = 0) { ipInfoClient.resolve(any(), any()) }
        coVerify(exactly = 0) { maxMindResolver.resolve(any()) }
    }

    @Test
    fun `cache miss with API key returns online result`() = runTest {
        coEvery { geoResultDao.getByIp(testIp) } returns null
        coEvery { settingsRepository.getApiKey() } returns "test-api-key"
        coEvery { ipInfoClient.resolve(testIp, "test-api-key") } returns onlineResult

        val result = resolver.resolve(testIp)

        assertNotNull(result)
        assertEquals(GeoSource.IPINFO.name, result!!.source)
        coVerify(exactly = 1) { ipInfoClient.resolve(testIp, "test-api-key") }
        coVerify(exactly = 1) { geoResultDao.insert(any()) }
        coVerify(exactly = 0) { maxMindResolver.resolve(any()) }
    }

    @Test
    fun `cache miss without API key falls back to MaxMind`() = runTest {
        coEvery { geoResultDao.getByIp(testIp) } returns null
        coEvery { settingsRepository.getApiKey() } returns null
        coEvery { maxMindResolver.resolve(testIp) } returns offlineResult

        val result = resolver.resolve(testIp)

        assertNotNull(result)
        assertEquals(GeoSource.MAXMIND_LOCAL.name, result!!.source)
        coVerify(exactly = 0) { ipInfoClient.resolve(any(), any()) }
        coVerify(exactly = 1) { maxMindResolver.resolve(testIp) }
        coVerify(exactly = 1) { geoResultDao.insert(any()) }
    }

    @Test
    fun `empty string API key falls back to MaxMind`() = runTest {
        coEvery { geoResultDao.getByIp(testIp) } returns null
        coEvery { settingsRepository.getApiKey() } returns ""
        coEvery { maxMindResolver.resolve(testIp) } returns offlineResult

        resolver.resolve(testIp)

        coVerify(exactly = 0) { ipInfoClient.resolve(any(), any()) }
        coVerify(exactly = 1) { maxMindResolver.resolve(testIp) }
    }

    @Test
    fun `online failure falls back to MaxMind`() = runTest {
        coEvery { geoResultDao.getByIp(testIp) } returns null
        coEvery { settingsRepository.getApiKey() } returns "key"
        coEvery { ipInfoClient.resolve(testIp, "key") } throws RuntimeException("network error")
        coEvery { maxMindResolver.resolve(testIp) } returns offlineResult

        val result = resolver.resolve(testIp)

        assertNotNull(result)
        assertEquals(GeoSource.MAXMIND_LOCAL.name, result!!.source)
        coVerify(exactly = 1) { maxMindResolver.resolve(testIp) }
    }

    @Test
    fun `both online and offline fail returns null`() = runTest {
        coEvery { geoResultDao.getByIp(testIp) } returns null
        coEvery { settingsRepository.getApiKey() } returns null
        coEvery { maxMindResolver.resolve(testIp) } throws RuntimeException("db not available")

        val result = resolver.resolve(testIp)

        assertNull(result)
    }

    @Test
    fun `online null result falls back to MaxMind`() = runTest {
        coEvery { geoResultDao.getByIp(testIp) } returns null
        coEvery { settingsRepository.getApiKey() } returns "key"
        coEvery { ipInfoClient.resolve(testIp, "key") } returns null
        coEvery { maxMindResolver.resolve(testIp) } returns offlineResult

        val result = resolver.resolve(testIp)

        assertNotNull(result)
        assertEquals(GeoSource.MAXMIND_LOCAL.name, result!!.source)
    }
}
