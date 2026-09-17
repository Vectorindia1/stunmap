package com.stunmap.parser

data class RawStunAttribute(
    val type: Int,
    val length: Int,
    val value: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RawStunAttribute) return false
        return type == other.type && length == other.length && value.contentEquals(other.value)
    }

    override fun hashCode(): Int {
        var result = type
        result = 31 * result + length
        result = 31 * result + value.contentHashCode()
        return result
    }
}

sealed class StunAttribute {
    data class XorMappedAddress(val ip: String, val port: Int) : StunAttribute()
    data class MappedAddress(val ip: String, val port: Int) : StunAttribute()
    data class Software(val name: String) : StunAttribute()
    data class Unknown(val type: Int, val rawAttr: RawStunAttribute) : StunAttribute()
}
