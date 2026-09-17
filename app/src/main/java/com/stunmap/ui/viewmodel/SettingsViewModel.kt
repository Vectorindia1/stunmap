package com.stunmap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stunmap.classifier.MetaAsnFilter
import com.stunmap.repository.SessionRepository
import com.stunmap.repository.SettingsRepository
import com.stunmap.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import javax.inject.Inject

data class SettingsUiState(
    val apiKey: String = "",
    val showNonCandidates: Boolean = false,
    val darkMode: Boolean = true,
    val updateStatus: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val sessionRepository: SessionRepository,
    private val metaAsnFilter: MetaAsnFilter,
    private val httpClient: OkHttpClient
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.apiKeyFlow,
        settingsRepository.showNonCandidatesFlow,
        settingsRepository.darkModeFlow
    ) { apiKey, showNonCandidates, darkMode ->
        SettingsUiState(
            apiKey = apiKey ?: "",
            showNonCandidates = showNonCandidates,
            darkMode = darkMode
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun saveApiKey(key: String) = viewModelScope.launch {
        settingsRepository.setApiKey(key.trim())
    }

    fun setShowNonCandidates(show: Boolean) = viewModelScope.launch {
        settingsRepository.setShowNonCandidates(show)
    }

    fun clearAllHistory() = viewModelScope.launch {
        sessionRepository.deleteAllSessions()
    }

    fun updateMetaAsnList(context: android.content.Context) = viewModelScope.launch {
        try {
            val request = Request.Builder().url(Constants.META_ASN_UPDATE_URL).build()
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val content = response.body?.string() ?: return@launch
                val file = File(context.filesDir, Constants.META_ASN_ASSET_FILE)
                file.writeText(content)
                metaAsnFilter.reload()
                Timber.d("Meta ASN list updated successfully")
            }
        } catch (e: Exception) {
            Timber.e(e, "Meta ASN update failed")
        }
    }
}
