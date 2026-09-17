package com.stunmap.parser

data class StunMessage(
    val srcIp: String,
    val dstIp: String,
    val messageType: Int,
    val transactionId: String,
    val mappedIp: String?,
    val mappedPort: Int?,
    val timestamp: Long
) {
    val isBindingRequest: Boolean get() = messageType == 0x0001
    val isBindingResponse: Boolean get() = messageType == 0x0101
    val isBindingError: Boolean get() = messageType == 0x0111
}

enum class StunMessageType(val code: Int) {
    BINDING_REQUEST(0x0001),
    BINDING_RESPONSE(0x0101),
    BINDING_ERROR(0x0111),
    UNKNOWN(-1);

    companion object {
        fun fromCode(code: Int): StunMessageType =
            entries.find { it.code == code } ?: UNKNOWN
    }
}
