package com.stunmap.geo

import android.content.Context
import com.maxmind.geoip2.DatabaseReader
import com.maxmind.geoip2.exception.AddressNotFoundException
import com.stunmap.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.net.InetAddress
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MaxMindResolver @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val reader: DatabaseReader? by lazy { initReader() }

    private fun initReader(): DatabaseReader? {
        return try {
            val mmdbFile = File(context.filesDir, Constants.GEOIP_DB_ASSET_FILE)
            if (!mmdbFile.exists()) {
                // Copy from assets
                context.assets.open(Constants.GEOIP_DB_ASSET_FILE).use { input ->
                    FileOutputStream(mmdbFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            DatabaseReader.Builder(mmdbFile).build()
        } catch (e: Exception) {
            Timber.w(e, "MaxMind DB not available — offline geo will be unavailable")
            null
        }
    }

    fun resolve(ip: String): GeoResult? {
        val db = reader ?: return null
        return try {
            val addr = InetAddress.getByName(ip)
            val response = db.city(addr)

            val lat = response.location.latitude
            val lng = response.location.longitude
            val asn = response.traits.autonomousSystemNumber?.toString()?.let { "AS$it" }
            val org = response.traits.autonomousSystemOrganization

            GeoResult(
                ip = ip,
                city = response.city.name,
                region = response.mostSpecificSubdivision.name,
                country = response.country.isoCode,
                postal = response.postal.code,
                latitude = lat,
                longitude = lng,
                org = if (asn != null && org != null) "$asn $org" else org,
                asn = asn,
                isp = org,
                resolvedAt = System.currentTimeMillis(),
                source = GeoSource.MAXMIND_LOCAL.name
            )
        } catch (e: AddressNotFoundException) {
            Timber.d("MaxMind: no data for $ip")
            null
        } catch (e: Exception) {
            Timber.w(e, "MaxMind resolve failed for $ip")
            null
        }
    }
}
