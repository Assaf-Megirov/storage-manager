package com.example.storage_manager.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.storage_manager.services.StorageTrackerPersistenceService

class StorageTrackerViewModelFactory(
    private val context: Context,
    private val persistenceService: StorageTrackerPersistenceService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StorageTrackerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StorageTrackerViewModel(context.applicationContext, persistenceService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

    companion object {
        fun provide(context: Context, persistenceService: StorageTrackerPersistenceService): StorageTrackerViewModelFactory {
            return StorageTrackerViewModelFactory(context, persistenceService)
        }
    }
}