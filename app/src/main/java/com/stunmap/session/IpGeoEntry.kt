package com.stunmap.session

import com.stunmap.geo.GeoResult

data class IpGeoEntry(
    val ip: String,
    val geoResult: GeoResult? = null
)
