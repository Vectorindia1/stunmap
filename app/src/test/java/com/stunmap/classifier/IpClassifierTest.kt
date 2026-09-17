package com.stunmap.classifier

import com.stunmap.util.CidrMatcher
import org.junit.Assert.*
import org.junit.Test

class IpClassifierTest {

    @Test
    fun `private IP 192_168 classified correctly`() {
        assertTrue("192.168.1.1".isInPrivateRange())
    }

    @Test
    fun `private IP 10_x classified correctly`() {
        assertTrue("10.0.0.1".isInPrivateRange())
    }

    @Test
    fun `private IP 172_16_x classified correctly`() {
        assertTrue("172.16.0.1".isInPrivateRange())
        assertTrue("172.31.255.255".isInPrivateRange())
    }

    @Test
    fun `172_32 is not private`() {
        assertFalse("172.32.0.1".isInPrivateRange())
    }

    @Test
    fun `loopback is private`() {
        assertTrue("127.0.0.1".isInPrivateRange())
    }

    @Test
    fun `public IP is not private`() {
        assertFalse("8.8.8.8".isInPrivateRange())
        assertFalse("49.36.112.44".isInPrivateRange())
    }

    @Test
    fun `cidr matcher detects IP in range`() {
        val matcher = CidrMatcher("157.240.0.0/17")
        assertTrue(matcher.contains("157.240.22.35"))
        assertTrue(matcher.contains("157.240.0.1"))
    }

    @Test
    fun `cidr matcher rejects IP outside range`() {
        val matcher = CidrMatcher("157.240.0.0/17")
        assertFalse(matcher.contains("157.241.0.1"))
        assertFalse(matcher.contains("8.8.8.8"))
    }

    @Test
    fun `cidr 32 matches only that exact IP`() {
        val matcher = CidrMatcher("1.2.3.4/32")
        assertTrue(matcher.contains("1.2.3.4"))
        assertFalse(matcher.contains("1.2.3.5"))
    }

    @Test
    fun `cidr 0 matches everything`() {
        val matcher = CidrMatcher("0.0.0.0/0")
        assertTrue(matcher.contains("1.2.3.4"))
        assertTrue(matcher.contains("255.255.255.255"))
    }

    @Test
    fun `cidr matcher handles boundary IPs`() {
        val matcher = CidrMatcher("69.63.176.0/20")
        // First IP in range
        assertTrue(matcher.contains("69.63.176.0"))
        // Last IP in range: /20 = 4096 addresses, last = 69.63.191.255
        assertTrue(matcher.contains("69.63.191.255"))
        // Just outside
        assertFalse(matcher.contains("69.63.192.0"))
        assertFalse(matcher.contains("69.63.175.255"))
    }

    @Test
    fun `invalid cidr returns null from parse`() {
        assertNull(CidrMatcher.parse("not_a_cidr"))
        assertNull(CidrMatcher.parse("300.0.0.0/8"))
    }
}

private fun String.isInPrivateRange(): Boolean {
    val parts = split(".").mapNotNull { it.toIntOrNull() }
    if (parts.size != 4) return false
    return when {
        parts[0] == 10 -> true
        parts[0] == 172 && parts[1] in 16..31 -> true
        parts[0] == 192 && parts[1] == 168 -> true
        parts[0] == 127 -> true
        parts[0] == 0 -> true
        parts[0] == 169 && parts[1] == 254 -> true
        else -> false
    }
}
