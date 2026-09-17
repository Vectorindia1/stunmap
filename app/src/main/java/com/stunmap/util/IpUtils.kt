package com.stunmap.util

import java.net.InetAddress

fun String.toIpBytes(): ByteArray? = try {
    InetAddress.getByName(this).address
} catch (e: Exception) {
    null
}

fun ByteArray.ipBytesToString(): String {
    if (size != 4) throw IllegalArgumentException("Expected 4 bytes for IPv4")
    return "${this[0].toInt() and 0xFF}.${this[1].toInt() and 0xFF}" +
        ".${this[2].toInt() and 0xFF}.${this[3].toInt() and 0xFF}"
}

fun String.isPrivateIp(): Boolean {
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

fun String.isValidPublicIp(): Boolean {
    if (this == "0.0.0.0" || this == "255.255.255.255") return false
    if (isPrivateIp()) return false
    val parts = split(".").mapNotNull { it.toIntOrNull() }
    if (parts.size != 4) return false
    return parts.all { it in 0..255 }
}
