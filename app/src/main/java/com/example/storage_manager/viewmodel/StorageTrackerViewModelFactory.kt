package com.example.storage_manager.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

object StorageTrackerViewModelFactory {
    fun provide(context: Context): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            StorageTrackerViewModel(context)
        }
    }
}