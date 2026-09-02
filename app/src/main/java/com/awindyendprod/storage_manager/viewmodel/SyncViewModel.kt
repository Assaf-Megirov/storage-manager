package com.awindyendprod.storage_manager.viewmodel

import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awindyendprod.storage_manager.services.GoogleAuthService
import com.awindyendprod.storage_manager.services.SyncManager
import com.awindyendprod.storage_manager.services.SyncOutcome
import com.awindyendprod.storage_manager.services.SyncPreferencesStore
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

enum class MainDeviceStatus {
    UNSET, THIS_DEVICE, OTHER_DEVICE
}

data class SyncUiState(
    val syncEnabled: Boolean = false,
    val signedInAccountEmail: String? = null,
    val syncInProgress: Boolean = false,
    val lastSyncedAtMillis: Long? = null,
    val mainDeviceStatus: MainDeviceStatus = MainDeviceStatus.UNSET,
    val reauthRequired: Boolean = false,
)

enum class SyncResultUi {
    Success, AuthRequired, NetworkUnavailable, Failed, SignInFailed, MainClaimRejected
}

class SyncViewModel(
    private val syncManager: SyncManager,
    private val googleAuthService: GoogleAuthService,
    private val syncPreferencesStore: SyncPreferencesStore,
    private val profileViewModel: ProfileViewModel,
) : ViewModel() {

    private val _uiState = MutableStateFlow(buildInitialUiState())
    val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()

    private val _syncResult = MutableStateFlow<SyncResultUi?>(null)
    val syncResult: StateFlow<SyncResultUi?> = _syncResult.asStateFlow()

    private val _showAdoptConfirmation = MutableStateFlow(false)
    val showAdoptConfirmation: StateFlow<Boolean> = _showAdoptConfirmation.asStateFlow()
    private var adoptConfirmationDeferred: CompletableDeferred<Boolean>? = null

    private val _resetInProgress = MutableStateFlow(false)
    val resetInProgress: StateFlow<Boolean> = _resetInProgress.asStateFlow()

    private var periodicSyncJob: Job? = null
    private var debouncedSyncJob: Job? = null

    fun scheduleDebouncedSync() {
        if (!_uiState.value.syncEnabled) return
        debouncedSyncJob?.cancel()
        debouncedSyncJob = viewModelScope.launch {
            delay(DATA_CHANGE_SYNC_DEBOUNCE_MS)
            syncNow(interactive = false)
        }
    }

    fun flushPendingSync() {
        val pending = debouncedSyncJob
        if (pending == null || !pending.isActive) return
        pending.cancel()
        debouncedSyncJob = null
        syncNow(interactive = false)
    }

    private fun buildInitialUiState(): SyncUiState = SyncUiState(
        syncEnabled = syncPreferencesStore.isSyncEnabled(),
        signedInAccountEmail = googleAuthService.getSignedInAccount()?.email,
        lastSyncedAtMillis = syncPreferencesStore.getLastSyncedAtMillis(),
        mainDeviceStatus = computeMainDeviceStatus()
    )

    private fun computeMainDeviceStatus(): MainDeviceStatus {
        val cached = syncPreferencesStore.getCachedMainDeviceId() ?: return MainDeviceStatus.UNSET
        return if (cached == syncPreferencesStore.getOrCreateDeviceId()) {
            MainDeviceStatus.THIS_DEVICE
        } else {
            MainDeviceStatus.OTHER_DEVICE
        }
    }

    fun clearSyncResult() {
        _syncResult.value = null
    }

    fun buildSignInIntent(): Intent = googleAuthService.buildSignInClient().signInIntent

    fun handleSignInResult(data: Intent?) {
        Log.d(TAG, "handleSignInResult called, data=$data")
        val account = try {
            GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException::class.java)
        } catch (e: ApiException) {
            Log.e(TAG, "Google sign-in failed: ${GoogleSignInStatusCodes.getStatusCodeString(e.statusCode)}", e)
            null
        }
        _uiState.value = _uiState.value.copy(
            signedInAccountEmail = account?.email,
            reauthRequired = false
        )
        if (account != null && _uiState.value.syncEnabled) {
            syncNow(interactive = true)
        } else if (account == null) {
            _syncResult.value = SyncResultUi.SignInFailed
        }
    }

    fun signOut() {
        googleAuthService.signOut {
            _uiState.value = _uiState.value.copy(
                signedInAccountEmail = null,
                reauthRequired = false
            )
        }
    }

    fun resetAllData(onComplete: (remoteDeleted: Boolean) -> Unit) {
        if (_resetInProgress.value) return
        stopPeriodicSync()
        debouncedSyncJob?.cancel()
        _resetInProgress.value = true
        viewModelScope.launch {
            val remoteDeleted = syncManager.resetAllData()
            if (!remoteDeleted) delay(1500)
            _resetInProgress.value = false
            onComplete(remoteDeleted)
        }
    }

    fun setSyncEnabled(enabled: Boolean) {
        syncPreferencesStore.setSyncEnabled(enabled)
        _uiState.value = _uiState.value.copy(syncEnabled = enabled)
        if (enabled) {
            syncNow(interactive = true)
        }
    }

    fun setMainDevice(markAsMain: Boolean) {
        syncPreferencesStore.setMarkedAsMainLocally(markAsMain)
        _uiState.value = _uiState.value.copy(mainDeviceStatus = computeMainDeviceStatus())
        syncNow(interactive = true)
    }

    fun confirmAdopt(accepted: Boolean) {
        _showAdoptConfirmation.value = false
        adoptConfirmationDeferred?.complete(accepted)
        adoptConfirmationDeferred = null
    }

    private suspend fun awaitAdoptConfirmation(): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        adoptConfirmationDeferred = deferred
        _showAdoptConfirmation.value = true
        return deferred.await()
    }

    fun syncNow(interactive: Boolean = false) {
        if (_uiState.value.syncInProgress) return
        _uiState.value = _uiState.value.copy(syncInProgress = true)
        viewModelScope.launch {
            val confirmWholesaleAdopt: suspend () -> Boolean = if (interactive) {
                { awaitAdoptConfirmation() }
            } else {
                { false }
            }
            val outcome = syncManager.performSync(confirmWholesaleAdopt = confirmWholesaleAdopt)
            Log.d(TAG, "performSync outcome: $outcome")
            if (outcome is SyncOutcome.Success) {
                profileViewModel.reloadAfterSync()
            }
            _uiState.value = _uiState.value.copy(
                syncInProgress = false,
                lastSyncedAtMillis = syncPreferencesStore.getLastSyncedAtMillis(),
                mainDeviceStatus = computeMainDeviceStatus(),
                reauthRequired = outcome is SyncOutcome.AuthRequired
            )
            outcome.toUiResult()?.let { _syncResult.value = it }
        }
    }

    fun onAppForegrounded() {
        val lastSync = syncPreferencesStore.getLastSyncedAtMillis()
        val debounceElapsed = lastSync == null || System.currentTimeMillis() - lastSync >= FOREGROUND_DEBOUNCE_MS
        if (debounceElapsed) {
            syncNow(interactive = false)
        }
    }

    fun startPeriodicSync() {
        periodicSyncJob?.cancel()
        periodicSyncJob = viewModelScope.launch {
            while (isActive) {
                delay(PERIODIC_SYNC_INTERVAL_MS)
                if (_uiState.value.syncEnabled) {
                    syncNow(interactive = false)
                }
            }
        }
    }

    fun stopPeriodicSync() {
        periodicSyncJob?.cancel()
        periodicSyncJob = null
    }

    private fun SyncOutcome.toUiResult(): SyncResultUi? = when (this) {
        is SyncOutcome.Success -> SyncResultUi.Success
        is SyncOutcome.AuthRequired -> SyncResultUi.AuthRequired
        is SyncOutcome.NetworkUnavailable -> SyncResultUi.NetworkUnavailable
        is SyncOutcome.Failure -> SyncResultUi.Failed
        is SyncOutcome.MainClaimRejected -> SyncResultUi.MainClaimRejected
        is SyncOutcome.NothingToDo -> null
    }

    companion object {
        private const val TAG = "SyncViewModel"
        private const val FOREGROUND_DEBOUNCE_MS = 60_000L
        private const val DATA_CHANGE_SYNC_DEBOUNCE_MS = 3_000L
        private val PERIODIC_SYNC_INTERVAL_MS = TimeUnit.MINUTES.toMillis(5)
    }
}
