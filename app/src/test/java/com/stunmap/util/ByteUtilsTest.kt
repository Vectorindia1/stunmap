package com.stunmap.util

import org.junit.Assert.*
import org.junit.Test

class ByteUtilsTest {

    @Test
    fun `getShort reads unsigned big-endian short`() {
        val bytes = byteArrayOf(0x00, 0x00, 0x01, 0x02)
        assertEquals(0x0102, bytes.getShort(2))
    }

    @Test
    fun `getShort handles signed bytes without sign-extension`() {
        // 0xFF 0xFE must produce 0xFFFE = 65534, not -2
        val bytes = byteArrayOf(0xFF.toByte(), 0xFE.toByte())
        assertEquals(0xFFFE, bytes.getShort(0))
    }

    @Test
    fun `getShort extracts STUN magic cookie high word`() {
        val cookie = byteArrayOf(0x21.toByte(), 0x12.toByte(), 0xA4.toByte(), 0x42.toByte())
        assertEquals(0x2112, cookie.getShort(0))
        assertEquals(0xA442, cookie.getShort(2))
    }

    @Test
    fun `getShort with zero bytes`() {
        val bytes = byteArrayOf(0x00, 0x00)
        assertEquals(0, bytes.getShort(0))
    }

    @Test
    fun `getLong4 reads STUN magic cookie as 0x2112A442`() {
        val cookie = byteArrayOf(0x21.toByte(), 0x12.toByte(), 0xA4.toByte(), 0x42.toByte())
        assertEquals(0x2112A442L, cookie.getLong4(0))
    }

    @Test
    fun `getLong4 handles all 0xFF bytes`() {
        val bytes = byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
        assertEquals(0xFFFFFFFFL, bytes.getLong4(0))
    }

    @Test
    fun `getLong4 handles all zero bytes`() {
        val bytes = ByteArray(4)
        assertEquals(0L, bytes.getLong4(0))
    }

    @Test
    fun `toIpString converts 4 bytes to dotted decimal`() {
        val bytes = byteArrayOf(8, 8, 8, 8)
        assertEquals("8.8.8.8", bytes.toIpString())
    }

    @Test
    fun `toIpString handles high-value bytes correctly`() {
        val bytes = byteArrayOf(192.toByte(), 168.toByte(), 1, 100)
        assertEquals("192.168.1.100", bytes.toIpString())
    }

    @Test
    fun `toIpString handles 255 octets`() {
        val bytes = byteArrayOf(255.toByte(), 255.toByte(), 255.toByte(), 255.toByte())
        assertEquals("255.255.255.255", bytes.toIpString())
    }

    @Test
    fun `toHexString produces lowercase hex with padding`() {
        val bytes = byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte())
        assertEquals("deadbeef", bytes.toHexString())
    }

    @Test
    fun `toHexString pads single-digit bytes`() {
        val bytes = byteArrayOf(0x00, 0x0A, 0x0F)
        assertEquals("000a0f", bytes.toHexString())
    }

    @Test
    fun `toHexString on empty array returns empty string`() {
        assertEquals("", ByteArray(0).toHexString())
    }

    @Test
    fun `getInt reads signed big-endian int`() {
        val bytes = byteArrayOf(0x7F.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
        assertEquals(Int.MAX_VALUE, bytes.getInt(0))
    }

    @Test
    fun `getInt reads zero`() {
        val bytes = ByteArray(4)
        assertEquals(0, bytes.getInt(0))
    }

    @Test
    fun `getInt reads with offset`() {
        val bytes = byteArrayOf(0x00, 0x00, 0x00, 0x01, 0x02, 0x03, 0x04)
        assertEquals(0x01020304, bytes.getInt(3))
    }
}
