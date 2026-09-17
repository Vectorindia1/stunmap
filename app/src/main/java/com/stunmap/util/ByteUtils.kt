package com.stunmap.util

fun ByteArray.getShort(offset: Int): Int =
    ((this[offset].toInt() and 0xFF) shl 8) or (this[offset + 1].toInt() and 0xFF)

fun ByteArray.getLong4(offset: Int): Long =
    ((this[offset].toLong() and 0xFF) shl 24) or
    ((this[offset + 1].toLong() and 0xFF) shl 16) or
    ((this[offset + 2].toLong() and 0xFF) shl 8) or
    (this[offset + 3].toLong() and 0xFF)

fun ByteArray.toIpString(): String =
    "${this[0].toInt() and 0xFF}.${this[1].toInt() and 0xFF}" +
    ".${this[2].toInt() and 0xFF}.${this[3].toInt() and 0xFF}"

fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }

fun ByteArray.getInt(offset: Int): Int =
    ((this[offset].toInt() and 0xFF) shl 24) or
    ((this[offset + 1].toInt() and 0xFF) shl 16) or
    ((this[offset + 2].toInt() and 0xFF) shl 8) or
    (this[offset + 3].toInt() and 0xFF)
