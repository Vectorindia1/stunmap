package com.stunmap.session

import com.stunmap.classifier.IpClassification
import com.stunmap.parser.StunMessageType
import kotlinx.serialization.Serializable

@Serializable
data class StunHit(
    val id: String,
    val sessionId: String,
    val timestamp: Long,
    val srcIp: String,
    val dstIp: String,
    val mappedIp: String?,
    val mappedPort: Int?,
    val messageType: StunMessageType,
    val transactionId: String,
    val classification: IpClassification
)
