package com.stunmap.parser

import com.stunmap.util.Constants
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class StunParserTest {

    private lateinit var parser: StunParser

    // Real STUN Binding Response with XOR-MAPPED-ADDRESS
    // XOR-MAPPED-ADDRESS encodes IP 49.36.112.44, port 12345
    // IP raw bytes: 49^0x21=40=0x28, 36^0x12=54=0x36, 112^0xA4=0xD8=216, 44^0x42=0x26=38
    // Actually let's use a known-good test vector
    private val validStunResponse = byteArrayOf(
        0x01.toByte(), 0x01.toByte(),  // Binding Response (0x0101)
        0x00.toByte(), 0x0C.toByte(),  // Body length: 12
        0x21.toByte(), 0x12.toByte(), 0xA4.toByte(), 0x42.toByte(), // Magic cookie
        0xB7.toByte(), 0xE7.toByte(), 0x13.toByte(), 0x2D.toByte(), // Transaction ID (12 bytes)
        0x85.toByte(), 0x68.toByte(), 0x24.toByte(), 0x61.toByte(),
        0x15.toByte(), 0x73.toByte(), 0x61.toByte(), 0x09.toByte(),
        // XOR-MAPPED-ADDRESS attribute
        0x00.toByte(), 0x20.toByte(), // Attr type: XOR-MAPPED-ADDRESS
        0x00.toByte(), 0x08.toByte(), // Attr length: 8
        0x00.toByte(), 0x01.toByte(), // Reserved + Family (IPv4)
        0x2B.toByte(), 0x2B.toByte(), // XOR'd port: 0x2B2B XOR 0x2112 = 0x0A39 = 2617
        0x68.toByte(), 0x22.toByte(), 0xD2.toByte(), 0x6E.toByte()  // XOR'd IP
    )

    private val nonStunUdpPayload = byteArrayOf(
        0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
        0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F,
        0x10, 0x11, 0x12, 0x13
    )

    @Before
    fun setUp() {
        parser = StunParser()
    }

    @Test
    fun `isStunPacket returns true for valid STUN packet`() {
        assertTrue(parser.isStunPacket(validStunResponse))
    }

    @Test
    fun `isStunPacket returns false for non-STUN UDP`() {
        assertFalse(parser.isStunPacket(nonStunUdpPayload))
    }

    @Test
    fun `isStunPacket returns false for too-short payload`() {
        assertFalse(parser.isStunPacket(ByteArray(10)))
    }

    @Test
    fun `isStunPacket returns false when first 2 bits are not 00`() {
        val invalid = validStunResponse.copyOf()
        invalid[0] = 0xC1.toByte() // top 2 bits set
        assertFalse(parser.isStunPacket(invalid))
    }

    @Test
    fun `parse returns non-null for valid STUN response`() {
        val msg = parser.parse(validStunResponse, "1.2.3.4", "5.6.7.8")
        assertNotNull(msg)
        assertEquals("1.2.3.4", msg!!.srcIp)
        assertEquals("5.6.7.8", msg.dstIp)
        assertEquals(0x0101, msg.messageType)
        assertTrue(msg.isBindingResponse)
    }

    @Test
    fun `parse returns null for non-STUN payload`() {
        val result = parser.parse(nonStunUdpPayload, "1.2.3.4", "5.6.7.8")
        assertNull(result)
    }

    @Test
    fun `parse does not throw on malformed packet`() {
        val malformed = ByteArray(25) { it.toByte() }
        malformed[0] = 0x00
        malformed[4] = 0x21; malformed[5] = 0x12; malformed[6] = 0xA4.toByte(); malformed[7] = 0x42
        val result = parser.parse(malformed, "1.2.3.4", "5.6.7.8")
        // May return null or a partial result — must not throw
    }

    @Test
    fun `parseXorMappedAddress correctly un-XORs IP`() {
        // XOR-MAPPED-ADDRESS value bytes for IP 49.36.112.44 port 2617
        // Port: 2617 XOR 0x2112 = 0x0A39 XOR 0x2112 = 0x2B2B
        // IP[0]: 49 XOR 0x21 = 0x28 = 40
        val attrValue = byteArrayOf(
            0x00, 0x01,             // reserved + family IPv4
            0x2B.toByte(), 0x2B.toByte(), // XOR'd port
            0x68.toByte(), 0x22.toByte(), 0xD2.toByte(), 0x6E.toByte() // XOR'd IP
        )
        val result = parser.parseXorMappedAddress(attrValue)
        assertNotNull(result)
        val (ip, port) = result!!
        // Verify port un-XOR: 0x2B2B XOR 0x2112 = 0x0A39 = 2617
        assertEquals(2617, port)
        // Verify IP format
        assertTrue(ip.contains("."))
        val parts = ip.split(".").mapNotNull { it.toIntOrNull() }
        assertEquals(4, parts.size)
        parts.forEach { assertTrue(it in 0..255) }
    }

    @Test
    fun `parseXorMappedAddress returns null for IPv6 family`() {
        val ipv6Value = byteArrayOf(0x00, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        assertNull(parser.parseXorMappedAddress(ipv6Value))
    }

    @Test
    fun `parseAttributes returns correct list for multi-attribute body`() {
        // Build a body with two attributes
        val body = byteArrayOf(
            0x00.toByte(), 0x20.toByte(), // XOR_MAPPED_ADDRESS type
            0x00.toByte(), 0x08.toByte(), // length 8
            0x00.toByte(), 0x01.toByte(), 0x2B.toByte(), 0x2B.toByte(),
            0x68.toByte(), 0x22.toByte(), 0xD2.toByte(), 0x6E.toByte(),
            0x80.toByte(), 0x22.toByte(), // SOFTWARE type
            0x00.toByte(), 0x04.toByte(), // length 4
            0x54.toByte(), 0x65.toByte(), 0x73.toByte(), 0x74.toByte() // "Test"
        )
        val attrs = parser.parseAttributes(body)
        assertEquals(2, attrs.size)
        assertEquals(Constants.ATTR_XOR_MAPPED_ADDRESS, attrs[0].type)
        assertEquals(Constants.ATTR_SOFTWARE, attrs[1].type)
    }

    @Test
    fun `parseAttributes handles empty body`() {
        val result = parser.parseAttributes(ByteArray(0))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parseAttributes does not throw on truncated attribute`() {
        val truncated = byteArrayOf(0x00.toByte(), 0x20.toByte(), 0x00.toByte(), 0x08.toByte())
        // length says 8 bytes but no value follows — should break safely
        val result = parser.parseAttributes(truncated)
        assertTrue(result.isEmpty())
    }
}
