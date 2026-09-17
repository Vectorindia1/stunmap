package com.stunmap.classifier

import com.stunmap.util.Constants
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SelfIpDetector @Inject constructor(
    private val httpClient: OkHttpClient
) {
    @Volatile
    private var cachedIp: String? = null

    suspend fun detect(): String? {
        cachedIp?.let { return it }
        return try {
            val client = httpClient.newBuilder()
                .connectTimeout(Constants.SELF_IP_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .readTimeout(Constants.SELF_IP_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .build()
            val request = Request.Builder()
                .url(Constants.SELF_IP_URL)
                .build()
            val response = client.newCall(request).execute()
            val ip = response.body?.string()?.trim()
            if (!ip.isNullOrEmpty()) {
                cachedIp = ip
                Timber.d("Self IP detected: $ip")
            }
            ip
        } catch (e: Exception) {
            Timber.w(e, "Could not detect self IP")
            null
        }
    }

    fun getCached(): String? = cachedIp

    fun invalidate() {
        cachedIp = null
    }
}
