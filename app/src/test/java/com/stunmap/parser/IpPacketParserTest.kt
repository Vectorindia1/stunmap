package com.stunmap.parser

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class IpPacketParserTest {

    private lateinit var parser: IpPacketParser

    // Minimal valid IPv4/UDP packet header (no payload)
    private val validUdpPacket = buildIpv4UdpHeader(
        srcIp = byteArrayOf(192.toByte(), 168.toByte(), 1, 1),
        dstIp = byteArrayOf(8, 8, 8, 8),
        udpPayload = byteArrayOf(0x01, 0x02, 0x03, 0x04)
    )

    @Before
    fun setUp() {
        parser = IpPacketParser()
    }

    @Test
    fun `parse returns null for too-short packet`() {
        assertNull(parser.parse(ByteArray(10)))
        assertNull(parser.parse(ByteArray(0)))
    }

    @Test
    fun `parse returns null for non-IPv4 packet`() {
        val ipv6Header = ByteArray(40)
        ipv6Header[0] = 0x60.toByte() // version 6
        assertNull(parser.parse(ipv6Header))
    }

    @Test
    fun `parse identifies UDP correctly`() {
        val result = parser.parse(validUdpPacket)
        assertNotNull(result)
        assertTrue(result!!.isUdp)
    }

    @Test
    fun `parse identifies TCP as non-UDP`() {
        val tcpPacket = validUdpPacket.copyOf()
        tcpPacket[9] = 0x06.toByte() // TCP protocol
        val result = parser.parse(tcpPacket)
        assertNotNull(result)
        assertFalse(result!!.isUdp)
    }

    @Test
    fun `parse extracts correct src and dst IPs`() {
        val result = parser.parse(validUdpPacket)
        assertNotNull(result)
        assertEquals("192.168.1.1", result!!.srcIp)
        assertEquals("8.8.8.8", result.dstIp)
    }

    @Test
    fun `parse extracts UDP payload`() {
        val result = parser.parse(validUdpPacket)
        assertNotNull(result)
        assertNotNull(result!!.payload)
        // Payload is the entire UDP datagram (header + data)
        assertTrue(result.payload!!.isNotEmpty())
    }

    @Test
    fun `parse handles variable IHL correctly`() {
        // IHL = 6 (24 bytes header, with options)
        val packetWithOptions = validUdpPacket.copyOf(validUdpPacket.size + 4)
        packetWithOptions[0] = 0x46.toByte() // version 4, IHL 6
        val result = parser.parse(packetWithOptions)
        assertNotNull(result)
    }

    private fun buildIpv4UdpHeader(
        srcIp: ByteArray,
        dstIp: ByteArray,
        udpPayload: ByteArray
    ): ByteArray {
        val ipHeaderLen = 20
        val udpHeaderLen = 8
        val total = ipHeaderLen + udpHeaderLen + udpPayload.size
        val packet = ByteArray(total)

        // IP header
        packet[0] = 0x45.toByte()  // version 4, IHL 5
        packet[1] = 0x00
        packet[2] = (total shr 8).toByte()
        packet[3] = total.toByte()
        packet[8] = 64            // TTL
        packet[9] = 0x11           // Protocol UDP
        // src IP
        srcIp.copyInto(packet, 12)
        // dst IP
        dstIp.copyInto(packet, 16)

        // UDP header
        packet[20] = 0x10; packet[21] = 0x92.toByte() // src port 4242
        packet[22] = 0x23; packet[23] = 0x28 // dst port 9000
        val udpLen = udpHeaderLen + udpPayload.size
        packet[24] = (udpLen shr 8).toByte()
        packet[25] = udpLen.toByte()

        // Payload
        udpPayload.copyInto(packet, 28)

        return packet
    }
}
