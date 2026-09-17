package com.stunmap.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.stunmap.session.CaptureSession

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val startedAt: Long,
    val endedAt: Long?,
    val devicePublicIp: String,
    val stunHitCount: Int,
    val candidateCount: Int,
    val notes: String,
    val interrupted: Boolean = false
) {
    fun toDomain() = CaptureSession(
        id = id,
        startedAt = startedAt,
        endedAt = endedAt,
        devicePublicIp = devicePublicIp,
        stunHitCount = stunHitCount,
        candidateCount = candidateCount,
        notes = notes,
        interrupted = interrupted
    )
}

fun CaptureSession.toEntity() = SessionEntity(
    id = id,
    startedAt = startedAt,
    endedAt = endedAt,
    devicePublicIp = devicePublicIp,
    stunHitCount = stunHitCount,
    candidateCount = candidateCount,
    notes = notes,
    interrupted = interrupted
)
