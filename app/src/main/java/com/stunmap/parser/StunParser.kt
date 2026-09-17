package com.stunmap.parser

import com.stunmap.util.Constants
import com.stunmap.util.getLong4
import com.stunmap.util.getShort
import com.stunmap.util.toHexString
import com.stunmap.util.toIpString
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StunParser @Inject constructor() {

    fun isStunPacket(udpPayload: ByteArray): Boolean {
        if (udpPayload.size < Constants.STUN_HEADER_SIZE) return false
        if ((udpPayload[0].toInt() and 0xC0) != 0) return false
        val cookie = udpPayload.getLong4(4)
        return cookie == Constants.STUN_MAGIC_COOKIE
    }

    fun parse(udpPayload: ByteArray, srcIp: String, dstIp: String): StunMessage? {
        if (!isStunPacket(udpPayload)) return null

        return try {
            val messageType = udpPayload.getShort(0)
            val bodyLength = udpPayload.getShort(2)
            val transactionId = udpPayload.sliceArray(8..19).toHexString()

            if (udpPayload.size < Constants.STUN_HEADER_SIZE + bodyLength) return null
            val body = udpPayload.sliceArray(
                Constants.STUN_HEADER_SIZE until Constants.STUN_HEADER_SIZE + bodyLength
            )

            val attributes = parseAttributes(body)

            var xorMappedIp: String? = null
            var xorMappedPort: Int? = null
            var mappedIp: String? = null
            var mappedPort: Int? = null

            for (attr in attributes) {
                when (attr.type) {
                    Constants.ATTR_XOR_MAPPED_ADDRESS -> {
                        parseXorMappedAddress(attr.value)?.let { (ip, port) ->
                            xorMappedIp = ip
                            xorMappedPort = port
                        }
                    }
                    Constants.ATTR_MAPPED_ADDRESS -> {
                        parseMappedAddress(attr.value)?.let { (ip, port) ->
                            mappedIp = ip
                            mappedPort = port
                        }
                    }
                }
            }

            val resolvedIp = xorMappedIp ?: mappedIp
            val resolvedPort = xorMappedPort ?: mappedPort

            StunMessage(
                srcIp = srcIp,
                dstIp = dstIp,
                messageType = messageType,
                transactionId = transactionId,
                mappedIp = resolvedIp,
                mappedPort = resolvedPort,
                timestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Timber.w(e, "STUN parse error — skipping malformed packet")
            null
        }
    }

    fun parseXorMappedAddress(value: ByteArray): Pair<String, Int>? {
        if (value.size < 8) return null
        val family = value[1].toInt() and 0xFF
        if (family != 0x01) return null  // IPv4 only in v1

        val rawPort = value.getShort(2)
        val port = rawPort xor 0x2112

        val magicBytes = byteArrayOf(0x21, 0x12, 0xA4.toByte(), 0x42)
        val ipBytes = ByteArray(4) { i ->
            (value[4 + i].toInt() xor (magicBytes[i].toInt() and 0xFF)).toByte()
        }

        return Pair(ipBytes.toIpString(), port)
    }

    fun parseMappedAddress(value: ByteArray): Pair<String, Int>? {
        if (value.size < 8) return null
        val family = value[1].toInt() and 0xFF
        if (family != 0x01) return null
        val port = value.getShort(2)
        val ip = value.sliceArray(4..7).toIpString()
        return Pair(ip, port)
    }

    fun parseAttributes(body: ByteArray): List<RawStunAttribute> {
        val attrs = mutableListOf<RawStunAttribute>()
        var offset = 0
        while (offset + 4 <= body.size) {
            val type = body.getShort(offset)
            val length = body.getShort(offset + 2)
            val padded = (length + 3) and 3.inv()
            if (offset + 4 + length > body.size) break
            val value = body.sliceArray(offset + 4 until offset + 4 + length)
            attrs.add(RawStunAttribute(type, length, value))
            offset += 4 + padded
        }
        return attrs
    }
}
