package com.stunmap

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stunmap.db.AppDatabase
import com.stunmap.db.GeoResultDao
import com.stunmap.db.SessionDao
import com.stunmap.db.StunHitDao
import com.stunmap.db.entities.GeoResultEntity
import com.stunmap.db.entities.SessionEntity
import com.stunmap.db.entities.StunHitEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var sessionDao: SessionDao
    private lateinit var stunHitDao: StunHitDao
    private lateinit var geoResultDao: GeoResultDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        sessionDao = db.sessionDao()
        stunHitDao = db.stunHitDao()
        geoResultDao = db.geoResultDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndQuerySession() = runTest {
        val entity = makeSession("s1", 1000L)
        sessionDao.insert(entity)

        val result = sessionDao.getById("s1")
        assertNotNull(result)
        assertEquals("s1", result!!.id)
        assertEquals("1.2.3.4", result.devicePublicIp)
    }

    @Test
    fun updateSession() = runTest {
        val entity = makeSession("s1", 1000L)
        sessionDao.insert(entity)

        val updated = entity.copy(stunHitCount = 42, endedAt = 9000L)
        sessionDao.update(updated)

        val result = sessionDao.getById("s1")
        assertEquals(42, result!!.stunHitCount)
        assertEquals(9000L, result.endedAt)
    }

    @Test
    fun getAllSessionsReturnedNewestFirst() = runTest {
        sessionDao.insert(makeSession("s1", startedAt = 1000L))
        sessionDao.insert(makeSession("s2", startedAt = 3000L))
        sessionDao.insert(makeSession("s3", startedAt = 2000L))

        val sessions = sessionDao.getAllSessions().first()
        assertEquals(3, sessions.size)
        assertEquals("s2", sessions[0].id)
        assertEquals("s3", sessions[1].id)
        assertEquals("s1", sessions[2].id)
    }

    @Test
    fun insertStunHitAndQueryBySession() = runTest {
        sessionDao.insert(makeSession("s1", 1000L))

        val hit = makeHit("h1", "s1")
        stunHitDao.insert(hit)

        val hits = stunHitDao.getBySessionIdOnce("s1")
        assertEquals(1, hits.size)
        assertEquals("h1", hits[0].id)
        assertEquals("49.36.112.44", hits[0].mappedIp)
    }

    @Test
    fun stunHitsCascadeDeleteOnSessionDelete() = runTest {
        sessionDao.insert(makeSession("s1", 1000L))
        stunHitDao.insert(makeHit("h1", "s1"))
        stunHitDao.insert(makeHit("h2", "s1"))

        sessionDao.delete(makeSession("s1", 1000L))

        val hits = stunHitDao.getBySessionIdOnce("s1")
        assertTrue(hits.isEmpty())
    }

    @Test
    fun stunHitsIsolatedBetweenSessions() = runTest {
        sessionDao.insert(makeSession("s1", 1000L))
        sessionDao.insert(makeSession("s2", 2000L))
        stunHitDao.insert(makeHit("h1", "s1"))
        stunHitDao.insert(makeHit("h2", "s2"))

        assertEquals(1, stunHitDao.getBySessionIdOnce("s1").size)
        assertEquals(1, stunHitDao.getBySessionIdOnce("s2").size)
    }

    @Test
    fun geoResultInsertAndQuery() = runTest {
        val geo = GeoResultEntity(
            ip = "49.36.112.44", city = "Mumbai", region = "Maharashtra",
            country = "IN", postal = null, latitude = 19.07, longitude = 72.87,
            org = "AS18209 Reliance", asn = "AS18209", isp = "Reliance",
            resolvedAt = 1000L, source = "IPINFO"
        )
        geoResultDao.insert(geo)

        val result = geoResultDao.getByIp("49.36.112.44")
        assertNotNull(result)
        assertEquals("Mumbai", result!!.city)
        assertEquals("IN", result.country)
    }

    @Test
    fun geoResultUpsertOverwritesExisting() = runTest {
        val geo = GeoResultEntity(
            ip = "1.1.1.1", city = "Sydney", region = null, country = "AU",
            postal = null, latitude = -33.86, longitude = 151.20,
            org = null, asn = null, isp = null, resolvedAt = 1000L, source = "MAXMIND_LOCAL"
        )
        geoResultDao.insert(geo)

        val updated = geo.copy(city = "Melbourne", resolvedAt = 9999L)
        geoResultDao.insert(updated)

        val result = geoResultDao.getByIp("1.1.1.1")
        assertEquals("Melbourne", result!!.city)
    }

    @Test
    fun deleteAllSessionsRemovesEverything() = runTest {
        sessionDao.insert(makeSession("s1", 1000L))
        sessionDao.insert(makeSession("s2", 2000L))

        sessionDao.deleteAll()

        val sessions = sessionDao.getAllSessions().first()
        assertTrue(sessions.isEmpty())
    }

    // --- helpers ---

    private fun makeSession(id: String, startedAt: Long) = SessionEntity(
        id = id, startedAt = startedAt, endedAt = null,
        devicePublicIp = "1.2.3.4", stunHitCount = 0, candidateCount = 0,
        notes = "", interrupted = false
    )

    private fun makeHit(id: String, sessionId: String) = StunHitEntity(
        id = id, sessionId = sessionId, timestamp = System.currentTimeMillis(),
        srcIp = "10.0.0.2", dstIp = "8.8.8.8",
        mappedIp = "49.36.112.44", mappedPort = 12345,
        messageType = "BINDING_RESPONSE", transactionId = "aabbccddeeff001122334455",
        classification = "CANDIDATE"
    )
}
