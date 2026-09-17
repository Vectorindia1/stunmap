package com.stunmap.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stunmap.service.StunCaptureService
import com.stunmap.session.IpGeoEntry
import com.stunmap.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class CaptureUiState(
    val isCapturing: Boolean = false,
    val durationMs: Long = 0L,
    val stunHitCount: Int = 0,
    val candidateIps: List<IpGeoEntry> = emptyList(),
    val devicePublicIp: String = ""
)

@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val sessionManager: SessionManager
) : ViewModel() {

    val captureState: StateFlow<CaptureUiState> = combine(
        sessionManager.currentSession,
        sessionManager.candidateIps,
        sessionManager.stunHitCount
    ) { session, candidates, hitCount ->
        CaptureUiState(
            isCapturing = session != null,
            durationMs = session?.durationMs ?: 0L,
            stunHitCount = hitCount,
            candidateIps = candidates,
            devicePublicIp = session?.devicePublicIp ?: ""
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CaptureUiState())

    fun onStartCapture(context: Context) {
        StunCaptureService.start(context)
    }

    fun onStopCapture(context: Context) {
        StunCaptureService.stop(context)
    }
}
