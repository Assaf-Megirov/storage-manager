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
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.awindyendprod.storage_manager.model.AppLanguage
import com.awindyendprod.storage_manager.ui.screens.StorageManagerApp
import com.awindyendprod.storage_manager.viewmodel.SettingsViewModel
import com.awindyendprod.storage_manager.viewmodel.StorageTrackerViewModel
import com.awindyendprod.storage_manager.viewmodel.StorageTrackerViewModelFactory
import com.awindyendprod.storage_manager.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch
import java.util.Locale
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import com.awindyendprod.storage_manager.ui.theme.StorageManagerTheme
import com.awindyendprod.storage_manager.services.StorageTrackerPersistenceService
import com.awindyendprod.storage_manager.services.ProfileMigrationService
import com.awindyendprod.storage_manager.services.ProfilePersistenceService
import com.awindyendprod.storage_manager.ui.components.DueItemsAlertDialog
import android.content.Intent
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.awindyendprod.storage_manager.services.DailyDueItemCheckWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val persistenceService by lazy {
        StorageTrackerPersistenceService(this)
    }

    private val profilePersistenceService by lazy {
        ProfilePersistenceService(this)
    }

    private val profileMigrationService by lazy {
        ProfileMigrationService(this, profilePersistenceService, persistenceService)
    }

    private val storageTrackerViewModel: StorageTrackerViewModel by viewModels {
        StorageTrackerViewModelFactory.provide(this, persistenceService)
    }

    private val settingsViewModel: SettingsViewModel by viewModels {
        SettingsViewModel.Factory(this, persistenceService, storageTrackerViewModel)
    }

    private val profileViewModel: ProfileViewModel by viewModels {
        ProfileViewModel.Factory(this, profilePersistenceService, storageTrackerViewModel)
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
        
        // Ensure traditional layout where content doesn't go behind system bars
        WindowCompat.setDecorFitsSystemWindows(window, true)

        // Migrate existing data to profiles if needed
        lifecycleScope.launch {
            val currentProfileId = profileMigrationService.migrateExistingDataIfNeeded()
            profileViewModel.loadProfiles()
            if (currentProfileId != null) {
                storageTrackerViewModel.reloadDataForProfile(currentProfileId)
            }
        }

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
            var showDueItemsAlert by remember { mutableStateOf(false) }
            var alertItemCount by remember { mutableStateOf(0) }
            var alertDaysBefore by remember { mutableStateOf(0) }

            // Check for pending alerts when the app starts
            LaunchedEffect(Unit) {
                checkForPendingAlert { count, days ->
                    alertItemCount = count
                    alertDaysBefore = days
                    showDueItemsAlert = true
                }
                
                // Schedule daily checks
                scheduleDailyChecks()
            }

            StorageManagerTheme(settings = settings) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    StorageManagerApp(
                        viewModel = storageTrackerViewModel,
                        settingsViewModel = settingsViewModel,
                        profileViewModel = profileViewModel,
                        importUri = importUri
                    )

                    // Show alert dialog if needed
                    if (showDueItemsAlert) {
                        DueItemsAlertDialog(
                            itemCount = alertItemCount,
                            daysBefore = alertDaysBefore,
                            onDismiss = {
                                showDueItemsAlert = false
                                clearPendingAlert()
                            },
                            onViewItems = {
                                showDueItemsAlert = false
                                clearPendingAlert()
                                // TODO: Navigate to AllDue screen with specific date
                            }
                        )
                    }
                }
            }
        }
    }

    private fun checkForPendingAlert(onAlert: (Int, Int) -> Unit) {
        val prefs = getSharedPreferences("daily_alerts", Context.MODE_PRIVATE)
        val count = prefs.getInt("pending_alert_count", 0)
        val days = prefs.getInt("pending_alert_days", 0)
        val timestamp = prefs.getLong("alert_timestamp", 0)
        
        // Check if alert is recent (within last 24 hours) and valid
        val isRecent = System.currentTimeMillis() - timestamp < TimeUnit.HOURS.toMillis(24)
        if (count > 0 && isRecent) {
            onAlert(count, days)
        }
    }

    private fun clearPendingAlert() {
        val prefs = getSharedPreferences("daily_alerts", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    private fun scheduleDailyChecks() {
        val workManager = WorkManager.getInstance(this)
        
        // Cancel existing work
        workManager.cancelUniqueWork(DailyDueItemCheckWorker.WORK_NAME)
        
        // Schedule new daily work
        val dailyWork = PeriodicWorkRequest.Builder(
            DailyDueItemCheckWorker::class.java,
            1, TimeUnit.DAYS
        ).build()
        
        workManager.enqueueUniquePeriodicWork(
            DailyDueItemCheckWorker.WORK_NAME,
            androidx.work.ExistingPeriodicWorkPolicy.REPLACE,
            dailyWork
        )
    }
}