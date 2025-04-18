package com.awindyendprod.storage_manager

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
import com.awindyendprod.storage_manager.model.AppLanguage
import com.awindyendprod.storage_manager.ui.screens.StorageManagerApp
import com.awindyendprod.storage_manager.viewmodel.SettingsViewModel
import com.awindyendprod.storage_manager.viewmodel.StorageTrackerViewModel
import com.awindyendprod.storage_manager.viewmodel.StorageTrackerViewModelFactory
import kotlinx.coroutines.launch
import java.util.Locale
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.awindyendprod.storage_manager.ui.theme.StorageManagerTheme
import com.awindyendprod.storage_manager.services.StorageTrackerPersistenceService
import android.content.Intent

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

        val context = newBase.createConfigurationContext(config)

        if (settings.language == AppLanguage.HEBREW) {
            context.resources.configuration.setLayoutDirection(locale)
        }
        
        super.attachBaseContext(context)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            settingsViewModel.recreateActivity.collect { shouldRecreate ->
                if (shouldRecreate) {
                    settingsViewModel.onActivityRecreated()
                    recreate()
                }
            }
        }

        val importUri = if (savedInstanceState == null) {
            when (intent?.action) {
                Intent.ACTION_VIEW -> intent?.data
                else -> null
            }
        } else null

        setContent {
            val settings by settingsViewModel.settings.collectAsState()
            StorageManagerTheme(settings = settings) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    StorageManagerApp(
                        viewModel = storageTrackerViewModel,
                        settingsViewModel = settingsViewModel,
                        importUri = importUri
                    )
                }
            }
        }
    }
}