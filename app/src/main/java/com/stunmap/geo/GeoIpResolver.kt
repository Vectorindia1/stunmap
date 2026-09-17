package com.stunmap.geo

import com.stunmap.db.GeoResultDao
import com.stunmap.db.entities.toDomain
import com.stunmap.db.entities.toEntity
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeoIpResolver @Inject constructor(
    private val ipInfoClient: IpInfoClient,
    private val maxMindResolver: MaxMindResolver,
    private val geoResultDao: GeoResultDao,
    private val settingsRepository: com.stunmap.repository.SettingsRepository
) {
    suspend fun resolve(ip: String): GeoResult? {
        // 1. Check Room cache first
        geoResultDao.getByIp(ip)?.let { entity -> return entity.toDomain() }

        val apiKey = settingsRepository.getApiKey()

        // 2. Try online if API key configured
        if (!apiKey.isNullOrEmpty()) {
            try {
                val result = ipInfoClient.resolve(ip, apiKey)
                if (result != null) {
                    geoResultDao.insert(result.toEntity())
                    return result
                }
            } catch (e: Exception) {
                Timber.w(e, "IPInfo failed for $ip, falling back to offline")
            }
        }

        // 3. Fallback to MaxMind local DB
        return try {
            maxMindResolver.resolve(ip)?.also {
                geoResultDao.insert(it.toEntity())
            }
        } catch (e: Exception) {
            Timber.e(e, "MaxMind also failed for $ip")
            null
        }
    }
}
