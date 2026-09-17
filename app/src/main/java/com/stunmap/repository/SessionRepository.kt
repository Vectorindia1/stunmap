package com.stunmap.repository

import com.stunmap.db.GeoResultDao
import com.stunmap.db.SessionDao
import com.stunmap.db.StunHitDao
import com.stunmap.db.entities.toEntity
import com.stunmap.geo.GeoResult
import com.stunmap.session.CaptureSession
import com.stunmap.session.StunHit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val stunHitDao: StunHitDao,
    private val geoResultDao: GeoResultDao
) {
    fun getAllSessions(): Flow<List<CaptureSession>> =
        sessionDao.getAllSessions().map { list -> list.map { it.toDomain() } }

    suspend fun getSessionById(id: String): CaptureSession? =
        sessionDao.getById(id)?.toDomain()

    suspend fun insertSession(session: CaptureSession) =
        sessionDao.insert(session.toEntity())

    suspend fun updateSession(session: CaptureSession) =
        sessionDao.update(session.toEntity())

    suspend fun deleteSession(session: CaptureSession) {
        sessionDao.delete(session.toEntity())
    }

    fun getStunHitsForSession(sessionId: String): Flow<List<StunHit>> =
        stunHitDao.getBySessionId(sessionId).map { list -> list.map { it.toDomain() } }

    suspend fun getStunHitsForSessionOnce(sessionId: String): List<StunHit> =
        stunHitDao.getBySessionIdOnce(sessionId).map { it.toDomain() }

    suspend fun insertStunHit(hit: StunHit) =
        stunHitDao.insert(hit.toEntity())

    suspend fun insertGeoResult(result: GeoResult) =
        geoResultDao.insert(result.toEntity())

    suspend fun deleteAllSessions() = sessionDao.deleteAll()
}
