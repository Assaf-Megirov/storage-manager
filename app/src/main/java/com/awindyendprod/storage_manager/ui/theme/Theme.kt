package com.awindyendprod.storage_manager.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.awindyendprod.storage_manager.model.Settings

@Composable
fun StorageManagerTheme(
    settings: Settings,
    content: @Composable () -> Unit
) {
    val colorScheme = lightColorScheme()
    val typography = Typography.withCustomSize(settings.fontSize)
    val view = LocalView.current
    
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
} 