package com.stunmap.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.stunmap.geo.GeoResult

@Entity(tableName = "geo_results")
data class GeoResultEntity(
    @PrimaryKey val ip: String,
    val city: String?,
    val region: String?,
    val country: String?,
    val postal: String?,
    val latitude: Double?,
    val longitude: Double?,
    val org: String?,
    val asn: String?,
    val isp: String?,
    val resolvedAt: Long,
    val source: String
) {
    fun toDomain() = GeoResult(
        ip = ip,
        city = city,
        region = region,
        country = country,
        postal = postal,
        latitude = latitude,
        longitude = longitude,
        org = org,
        asn = asn,
        isp = isp,
        resolvedAt = resolvedAt,
        source = source
    )
}

fun GeoResultEntity.toDomain() = GeoResult(
    ip = ip, city = city, region = region, country = country,
    postal = postal, latitude = latitude, longitude = longitude,
    org = org, asn = asn, isp = isp, resolvedAt = resolvedAt, source = source
)

fun GeoResult.toEntity() = GeoResultEntity(
    ip = ip,
    city = city,
    region = region,
    country = country,
    postal = postal,
    latitude = latitude,
    longitude = longitude,
    org = org,
    asn = asn,
    isp = isp,
    resolvedAt = resolvedAt,
    source = source
)
