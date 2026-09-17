package com.stunmap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stunmap.repository.SessionRepository
import com.stunmap.session.CaptureSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val sessionRepository: SessionRepository
) : ViewModel() {

    val sessions: StateFlow<List<CaptureSession>> =
        sessionRepository.getAllSessions()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteSession(session: CaptureSession) {
        viewModelScope.launch {
            sessionRepository.deleteSession(session)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            sessionRepository.deleteAllSessions()
        }
    }
}
