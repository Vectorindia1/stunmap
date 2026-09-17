package com.stunmap.util

import org.junit.Assert.*
import org.junit.Test

class IpUtilsTest {

    // --- isPrivateIp ---

    @Test fun `10_0_0_1 is private`() { assertTrue("10.0.0.1".isPrivateIp()) }
    @Test fun `10_255_255_255 is private`() { assertTrue("10.255.255.255".isPrivateIp()) }
    @Test fun `172_16_0_1 is private`() { assertTrue("172.16.0.1".isPrivateIp()) }
    @Test fun `172_31_255_255 is private`() { assertTrue("172.31.255.255".isPrivateIp()) }
    @Test fun `172_15_0_1 is not private`() { assertFalse("172.15.0.1".isPrivateIp()) }
    @Test fun `172_32_0_1 is not private`() { assertFalse("172.32.0.1".isPrivateIp()) }
    @Test fun `192_168_0_1 is private`() { assertTrue("192.168.0.1".isPrivateIp()) }
    @Test fun `192_168_255_255 is private`() { assertTrue("192.168.255.255".isPrivateIp()) }
    @Test fun `192_169_0_1 is not private`() { assertFalse("192.169.0.1".isPrivateIp()) }
    @Test fun `127_0_0_1 loopback is private`() { assertTrue("127.0.0.1".isPrivateIp()) }
    @Test fun `127_255_255_255 is private`() { assertTrue("127.255.255.255".isPrivateIp()) }
    @Test fun `0_0_0_0 is private`() { assertTrue("0.0.0.0".isPrivateIp()) }
    @Test fun `169_254_1_1 link-local is private`() { assertTrue("169.254.1.1".isPrivateIp()) }

    @Test
    fun `public IPs are not private`() {
        assertFalse("8.8.8.8".isPrivateIp())
        assertFalse("1.1.1.1".isPrivateIp())
        assertFalse("49.36.112.44".isPrivateIp())
        assertFalse("157.240.22.35".isPrivateIp())
    }

    @Test
    fun `malformed IPs are treated as private (safe default)`() {
        assertTrue("not.an.ip".isPrivateIp()) // returns false on parse failure
        assertTrue("".isPrivateIp())
    }

    // --- isValidPublicIp ---

    @Test fun `0_0_0_0 is not valid public`() { assertFalse("0.0.0.0".isValidPublicIp()) }
    @Test fun `255_255_255_255 is not valid public`() { assertFalse("255.255.255.255".isValidPublicIp()) }
    @Test fun `private IPs are not valid public`() { assertFalse("192.168.1.1".isValidPublicIp()) }
    @Test fun `loopback is not valid public`() { assertFalse("127.0.0.1".isValidPublicIp()) }
    @Test fun `malformed is not valid public`() { assertFalse("not.an.ip".isValidPublicIp()) }
    @Test fun `too many octets is not valid public`() { assertFalse("1.2.3.4.5".isValidPublicIp()) }

    @Test
    fun `real public IPs are valid`() {
        assertTrue("49.36.112.44".isValidPublicIp())
        assertTrue("8.8.8.8".isValidPublicIp())
        assertTrue("1.1.1.1".isValidPublicIp())
        assertTrue("157.240.22.35".isValidPublicIp())
    }

    @Test
    fun `boundary octet 255 is valid when IP is otherwise public`() {
        // 203.0.113.255 is a documentation/test range but structurally valid
        assertTrue("203.0.113.255".isValidPublicIp())
    }

    // --- toIpBytes / ipBytesToString roundtrip ---

    @Test
    fun `toIpBytes and ipBytesToString roundtrip for public IP`() {
        val ip = "49.36.112.44"
        val bytes = ip.toIpBytes()
        assertNotNull(bytes)
        assertEquals(4, bytes!!.size)
        assertEquals(ip, bytes.ipBytesToString())
    }

    @Test
    fun `toIpBytes returns null for garbage string`() {
        assertNull("not-an-ip".toIpBytes())
    }

    @Test
    fun `ipBytesToString handles all 255 bytes`() {
        val bytes = byteArrayOf(255.toByte(), 255.toByte(), 255.toByte(), 255.toByte())
        assertEquals("255.255.255.255", bytes.ipBytesToString())
    }
}
