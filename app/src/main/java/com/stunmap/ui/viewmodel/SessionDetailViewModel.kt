package com.stunmap.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stunmap.classifier.IpClassification
import com.stunmap.db.GeoResultDao
import com.stunmap.db.entities.toDomain
import com.stunmap.geo.GeoResult
import com.stunmap.repository.SessionRepository
import com.stunmap.session.CaptureSession
import com.stunmap.session.StunHit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionDetailUiState(
    val session: CaptureSession? = null,
    val hits: List<StunHit> = emptyList(),
    val geoResults: Map<String, GeoResult> = emptyMap(),
    val isLoading: Boolean = true
)

@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val geoResultDao: GeoResultDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: String = checkNotNull(savedStateHandle["sessionId"])

    private val _uiState = MutableStateFlow(SessionDetailUiState())
    val uiState: StateFlow<SessionDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val session = sessionRepository.getSessionById(sessionId)
            val hits = sessionRepository.getStunHitsForSessionOnce(sessionId)
            val candidateIps = hits
                .filter { it.classification == IpClassification.CANDIDATE }
                .mapNotNull { it.mappedIp }
                .distinct()
            val geoEntities = geoResultDao.getByIps(candidateIps)
            val geoMap = geoEntities.associate { it.ip to it.toDomain() }

            _uiState.value = SessionDetailUiState(
                session = session,
                hits = hits,
                geoResults = geoMap,
                isLoading = false
            )
        }
    }
}
