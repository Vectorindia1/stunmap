package com.stunmap.parser

import com.stunmap.util.toIpString
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

data class ParsedIpPacket(
    val srcIp: String,
    val dstIp: String,
    val isUdp: Boolean,
    val payload: ByteArray?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ParsedIpPacket) return false
        return srcIp == other.srcIp && dstIp == other.dstIp &&
            isUdp == other.isUdp && payload.contentEquals(other.payload)
    }

    override fun hashCode(): Int {
        var result = srcIp.hashCode()
        result = 31 * result + dstIp.hashCode()
        result = 31 * result + isUdp.hashCode()
        result = 31 * result + (payload?.contentHashCode() ?: 0)
        return result
    }
}

private fun ByteArray?.contentEquals(other: ByteArray?): Boolean =
    if (this == null && other == null) true
    else if (this == null || other == null) false
    else this.contentEquals(other)

@Singleton
class IpPacketParser @Inject constructor() {

    fun parse(raw: ByteArray): ParsedIpPacket? {
        return try {
            if (raw.size < 20) return null

            val version = (raw[0].toInt() ushr 4) and 0xF
            if (version != 4) return null  // IPv4 only

            val ihl = (raw[0].toInt() and 0xF) * 4
            if (ihl < 20 || raw.size < ihl) return null

            val protocol = raw[9].toInt() and 0xFF
            val srcIp = raw.sliceArray(12..15).toIpString()
            val dstIp = raw.sliceArray(16..19).toIpString()

            val isUdp = protocol == 0x11
            val payload = if (raw.size > ihl) raw.sliceArray(ihl until raw.size) else null

            ParsedIpPacket(srcIp, dstIp, isUdp, payload)
        } catch (e: Exception) {
            Timber.w(e, "IP packet parse error")
            null
        }
    }
}

@Singleton
class UdpParser @Inject constructor() {

    fun getPayload(udpDatagram: ByteArray): ByteArray? {
        if (udpDatagram.size <= 8) return null
        return udpDatagram.sliceArray(8 until udpDatagram.size)
    }
}
