package com.awindyendprod.storage_manager.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.awindyendprod.storage_manager.services.DriveSyncService
import com.awindyendprod.storage_manager.services.GoogleAuthService
import com.awindyendprod.storage_manager.services.ProfilePersistenceService
import com.awindyendprod.storage_manager.services.ProfileSettingsStore
import com.awindyendprod.storage_manager.services.StorageTrackerPersistenceService
import com.awindyendprod.storage_manager.services.SyncManager
import com.awindyendprod.storage_manager.services.SyncPreferencesStore
import com.awindyendprod.storage_manager.services.TombstoneStore

class AppViewModelFactory(
    private val context: Context,
) : ViewModelProvider.Factory {

    private val appContext = context.applicationContext

    private val persistenceService: StorageTrackerPersistenceService by lazy {
        StorageTrackerPersistenceService(appContext)
    }

    private val profilePersistenceService: ProfilePersistenceService by lazy {
        ProfilePersistenceService(appContext)
    }

    private val profileSettingsStore: ProfileSettingsStore by lazy {
        ProfileSettingsStore(appContext)
    }

    private val tombstoneStore: TombstoneStore by lazy {
        TombstoneStore(appContext)
    }

    private val syncPreferencesStore: SyncPreferencesStore by lazy {
        SyncPreferencesStore(appContext)
    }

    private val googleAuthService: GoogleAuthService by lazy {
        GoogleAuthService(appContext)
    }

    private val driveSyncService: DriveSyncService by lazy {
        DriveSyncService()
    }

    private val syncManager: SyncManager by lazy {
        SyncManager(
            appContext,
            persistenceService,
            profilePersistenceService,
            profileSettingsStore,
            tombstoneStore,
            googleAuthService,
            driveSyncService,
            syncPreferencesStore
        )
    }

    private val storageTrackerViewModel: StorageTrackerViewModel by lazy {
        StorageTrackerViewModel(appContext, persistenceService, tombstoneStore)
    }

    private val settingsViewModel: SettingsViewModel by lazy {
        SettingsViewModel(appContext, persistenceService, storageTrackerViewModel, profileSettingsStore)
    }

    private val profileViewModel: ProfileViewModel by lazy {
        ProfileViewModel(
            appContext,
            profilePersistenceService,
            profileSettingsStore,
            persistenceService,
            tombstoneStore,
            storageTrackerViewModel,
            settingsViewModel
        )
    }

    private val syncViewModel: SyncViewModel by lazy {
        SyncViewModel(syncManager, googleAuthService, syncPreferencesStore, profileViewModel).also { vm ->
            val trigger: () -> Unit = { vm.scheduleDebouncedSync() }
            storageTrackerViewModel.onDataChanged = trigger
            profileViewModel.onDataChanged = trigger
        }
    }

    val profileSettingsStoreInstance: ProfileSettingsStore
        get() = profileSettingsStore

    val profilePersistenceServiceInstance: ProfilePersistenceService
        get() = profilePersistenceService

    val persistenceServiceInstance: StorageTrackerPersistenceService
        get() = persistenceService

    val syncViewModelInstance: SyncViewModel
        get() = syncViewModel

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(StorageTrackerViewModel::class.java) -> storageTrackerViewModel as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> settingsViewModel as T
            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> profileViewModel as T
            modelClass.isAssignableFrom(SyncViewModel::class.java) -> syncViewModel as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
