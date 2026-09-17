package com.stunmap.session

import kotlinx.serialization.Serializable

@Serializable
data class CaptureSession(
    val id: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val devicePublicIp: String,
    val stunHitCount: Int = 0,
    val candidateCount: Int = 0,
    val notes: String = "",
    val interrupted: Boolean = false
) {
    val durationMs: Long
        get() = (endedAt ?: System.currentTimeMillis()) - startedAt

    val isActive: Boolean
        get() = endedAt == null
}
