package com.stunmap.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.stunmap.classifier.IpClassification
import com.stunmap.parser.StunMessageType
import com.stunmap.session.StunHit

@Entity(
    tableName = "stun_hits",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class StunHitEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val timestamp: Long,
    val srcIp: String,
    val dstIp: String,
    val mappedIp: String?,
    val mappedPort: Int?,
    val messageType: String,
    val transactionId: String,
    val classification: String
) {
    fun toDomain() = StunHit(
        id = id,
        sessionId = sessionId,
        timestamp = timestamp,
        srcIp = srcIp,
        dstIp = dstIp,
        mappedIp = mappedIp,
        mappedPort = mappedPort,
        messageType = StunMessageType.entries.find { it.name == messageType } ?: StunMessageType.UNKNOWN,
        transactionId = transactionId,
        classification = IpClassification.entries.find { it.name == classification }
            ?: IpClassification.UNKNOWN
    )
}

fun StunHit.toEntity() = StunHitEntity(
    id = id,
    sessionId = sessionId,
    timestamp = timestamp,
    srcIp = srcIp,
    dstIp = dstIp,
    mappedIp = mappedIp,
    mappedPort = mappedPort,
    messageType = messageType.name,
    transactionId = transactionId,
    classification = classification.name
)
