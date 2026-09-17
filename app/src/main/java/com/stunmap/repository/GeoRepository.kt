package com.stunmap.repository

import com.stunmap.db.GeoResultDao
import com.stunmap.db.entities.toDomain
import com.stunmap.geo.GeoIpResolver
import com.stunmap.geo.GeoResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeoRepository @Inject constructor(
    private val geoResultDao: GeoResultDao,
    private val geoIpResolver: GeoIpResolver
) {
    suspend fun resolve(ip: String): GeoResult? = geoIpResolver.resolve(ip)

    suspend fun getCached(ip: String): GeoResult? = geoResultDao.getByIp(ip)?.toDomain()
}
