package com.example.storage_manager

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import com.example.storage_manager.model.AppLanguage
import com.example.storage_manager.ui.screens.StorageManagerApp
import com.example.storage_manager.ui.screens.StorageManagerMainScreen
import com.example.storage_manager.viewmodel.SettingsViewModel
import com.example.storage_manager.viewmodel.StorageTrackerViewModel
import com.example.storage_manager.viewmodel.StorageTrackerViewModelFactory
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.Locale
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.storage_manager.ui.theme.StorageManagerTheme
import android.view.View
import androidx.core.os.ConfigurationCompat
import android.os.Build
import com.example.storage_manager.services.StorageTrackerPersistenceService

class MainActivity : ComponentActivity() {
    private val persistenceService by lazy {
        StorageTrackerPersistenceService(this)
    }

    private val storageTrackerViewModel: StorageTrackerViewModel by viewModels {
        StorageTrackerViewModelFactory.provide(this, persistenceService)
    }

    private val settingsViewModel: SettingsViewModel by viewModels {
        SettingsViewModel.Factory(this, persistenceService, storageTrackerViewModel)
    }
    
    override fun attachBaseContext(newBase: Context) {
        // Use temporary ViewModels just for initial configuration
        val tempPersistenceService = StorageTrackerPersistenceService(newBase)
        val tempStorageTrackerViewModel = StorageTrackerViewModel(newBase, tempPersistenceService)
        val tempSettingsViewModel = SettingsViewModel(
            newBase, 
            tempPersistenceService,
            tempStorageTrackerViewModel
        )
        val settings = tempSettingsViewModel.settings.value
        val locale = when (settings.language) {
            AppLanguage.SYSTEM -> Resources.getSystem().configuration.locales[0]
            AppLanguage.ENGLISH -> Locale("en")
            AppLanguage.HEBREW -> Locale("iw")
            AppLanguage.RUSSIAN -> Locale("ru")
        }
        
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        
        // Create a wrapped context with the new configuration
        val context = newBase.createConfigurationContext(config)
        
        // Force RTL for Hebrew after context creation
        if (settings.language == AppLanguage.HEBREW) {
            context.resources.configuration.setLayoutDirection(locale)
        }
        
        super.attachBaseContext(context)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Observe recreation events
        lifecycleScope.launch {
            settingsViewModel.recreateActivity.collect { shouldRecreate ->
                if (shouldRecreate) {
                    settingsViewModel.onActivityRecreated()
                    recreate()
                }
            }
        }

        setContent {
            val settings by settingsViewModel.settings.collectAsState()
            StorageManagerTheme(settings = settings) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    StorageManagerApp(
                        viewModel = storageTrackerViewModel,
                        settingsViewModel = settingsViewModel
                    )
                }
            }
        }
    }
}