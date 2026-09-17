package com.stunmap.geo

import com.stunmap.util.Constants
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IpInfoClient @Inject constructor(
    private val httpClient: OkHttpClient
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun resolve(ip: String, apiKey: String?): GeoResult? {
        return try {
            val url = "${Constants.IPINFO_BASE_URL}/$ip/json"
            val requestBuilder = Request.Builder().url(url)
            if (!apiKey.isNullOrEmpty()) {
                requestBuilder.header("Authorization", "Bearer $apiKey")
            }

            val client = httpClient.newBuilder()
                .connectTimeout(Constants.GEOIP_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .readTimeout(Constants.GEOIP_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .build()

            val response = client.newCall(requestBuilder.build()).execute()

            if (response.code == 429) {
                Timber.w("IPInfo rate limited for $ip")
                return null
            }
            if (!response.isSuccessful) {
                Timber.w("IPInfo returned ${response.code} for $ip")
                return null
            }

            val body = response.body?.string() ?: return null
            parseResponse(ip, body)
        } catch (e: Exception) {
            Timber.w(e, "IPInfo request failed for $ip")
            null
        }
    }

    private fun parseResponse(ip: String, body: String): GeoResult? {
        return try {
            val obj = json.parseToJsonElement(body).jsonObject

            val loc = obj["loc"]?.jsonPrimitive?.content
            val (lat, lng) = if (loc != null && loc.contains(",")) {
                val parts = loc.split(",")
                Pair(parts[0].toDoubleOrNull(), parts[1].toDoubleOrNull())
            } else {
                Pair(null, null)
            }

            val orgField = obj["org"]?.jsonPrimitive?.content
            val (asn, isp) = parseOrg(orgField)

            GeoResult(
                ip = ip,
                city = obj["city"]?.jsonPrimitive?.content?.takeIf { it.isNotEmpty() },
                region = obj["region"]?.jsonPrimitive?.content?.takeIf { it.isNotEmpty() },
                country = obj["country"]?.jsonPrimitive?.content?.takeIf { it.isNotEmpty() },
                postal = obj["postal"]?.jsonPrimitive?.content?.takeIf { it.isNotEmpty() },
                latitude = lat,
                longitude = lng,
                org = orgField,
                asn = asn,
                isp = isp,
                resolvedAt = System.currentTimeMillis(),
                source = GeoSource.IPINFO.name
            )
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse IPInfo response for $ip")
            null
        }
    }

    private fun parseOrg(org: String?): Pair<String?, String?> {
        if (org.isNullOrEmpty()) return Pair(null, null)
        val spaceIdx = org.indexOf(' ')
        return if (spaceIdx > 0) {
            Pair(org.substring(0, spaceIdx), org.substring(spaceIdx + 1))
        } else {
            Pair(org, null)
        }
    }
}
