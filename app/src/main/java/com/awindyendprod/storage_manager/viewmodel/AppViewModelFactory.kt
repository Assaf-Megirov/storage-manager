package com.awindyendprod.storage_manager.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.awindyendprod.storage_manager.services.ProfilePersistenceService
import com.awindyendprod.storage_manager.services.ProfileSettingsStore
import com.awindyendprod.storage_manager.services.StorageTrackerPersistenceService

/**
 * Single factory so [ProfileViewModel] and [SettingsViewModel] share one [ProfileSettingsStore]
 * and settings switching is always wired.
 */
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

    private val storageTrackerViewModel: StorageTrackerViewModel by lazy {
        StorageTrackerViewModel(appContext, persistenceService)
    }

    private val settingsViewModel: SettingsViewModel by lazy {
        SettingsViewModel(appContext, persistenceService, storageTrackerViewModel, profileSettingsStore)
    }

    val profileSettingsStoreInstance: ProfileSettingsStore
        get() = profileSettingsStore

    val profilePersistenceServiceInstance: ProfilePersistenceService
        get() = profilePersistenceService

    val persistenceServiceInstance: StorageTrackerPersistenceService
        get() = persistenceService

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(StorageTrackerViewModel::class.java) -> storageTrackerViewModel as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> settingsViewModel as T
            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> ProfileViewModel(
                appContext,
                profilePersistenceService,
                profileSettingsStore,
                persistenceService,
                storageTrackerViewModel,
                settingsViewModel
            ) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
