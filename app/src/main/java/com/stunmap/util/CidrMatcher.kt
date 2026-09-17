package com.stunmap.util

class CidrMatcher(cidr: String) {
    private val networkAddress: Int
    private val mask: Int

    init {
        val parts = cidr.trim().split("/")
        require(parts.size == 2) { "Invalid CIDR: $cidr" }
        val ipParts = parts[0].split(".").map { it.toInt() }
        require(ipParts.size == 4) { "Invalid IP in CIDR: $cidr" }
        val prefixLen = parts[1].toInt()
        require(prefixLen in 0..32) { "Invalid prefix length: $prefixLen" }

        var addr = 0
        for (part in ipParts) {
            addr = (addr shl 8) or (part and 0xFF)
        }
        mask = if (prefixLen == 0) 0 else (-1 shl (32 - prefixLen))
        networkAddress = addr and mask
    }

    fun contains(ip: String): Boolean {
        return try {
            val parts = ip.split(".").map { it.toInt() }
            if (parts.size != 4) return false
            var addr = 0
            for (part in parts) {
                addr = (addr shl 8) or (part and 0xFF)
            }
            (addr and mask) == networkAddress
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        fun parse(cidr: String): CidrMatcher? = try {
            CidrMatcher(cidr)
        } catch (e: Exception) {
            null
        }
    }
}
