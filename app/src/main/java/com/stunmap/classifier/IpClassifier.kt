package com.stunmap.classifier

import com.stunmap.util.CidrMatcher
import com.stunmap.util.isPrivateIp
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IpClassifier @Inject constructor(
    private val metaAsnFilter: MetaAsnFilter,
    private val selfIpDetector: SelfIpDetector
) {
    private val knownStunServers = listOf(
        CidrMatcher("74.125.0.0/16"),   // Google STUN
        CidrMatcher("172.217.0.0/16"),  // Google
        CidrMatcher("64.233.160.0/19"), // Google
        CidrMatcher("104.16.0.0/12"),   // Cloudflare
        CidrMatcher("1.1.1.0/24"),      // Cloudflare
        CidrMatcher("8.8.8.0/24"),      // Google DNS / STUN
    )

    fun classify(ip: String): IpClassification {
        return try {
            if (ip.isPrivateIp()) return IpClassification.PRIVATE

            val selfIp = selfIpDetector.getCached()
            if (selfIp != null && ip == selfIp) return IpClassification.SELF

            if (metaAsnFilter.contains(ip)) return IpClassification.META_INFRA

            if (knownStunServers.any { it.contains(ip) }) return IpClassification.KNOWN_STUN

            IpClassification.CANDIDATE
        } catch (e: Exception) {
            Timber.w(e, "Classification error for IP $ip")
            IpClassification.UNKNOWN
        }
    }
}
