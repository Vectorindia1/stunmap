package com.stunmap.geo

import kotlinx.serialization.Serializable

enum class GeoSource {
    IPINFO,
    MAXMIND_LOCAL
}

@Serializable
data class GeoResult(
    val ip: String,
    val city: String? = null,
    val region: String? = null,
    val country: String? = null,
    val postal: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val org: String? = null,
    val asn: String? = null,
    val isp: String? = null,
    val resolvedAt: Long = System.currentTimeMillis(),
    val source: String = GeoSource.IPINFO.name
)
