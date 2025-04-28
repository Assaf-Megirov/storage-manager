package com.awindyendprod.storage_manager.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.awindyendprod.storage_manager.model.Settings
import com.awindyendprod.storage_manager.model.Theme

@Composable
fun StorageManagerTheme(
    settings: Settings,
    content: @Composable () -> Unit
) {
    val useDark = when (settings.theme) {
        Theme.SYSTEM -> isSystemInDarkTheme()    // follow Android’s system setting
        Theme.DARK   -> true                     // always dark
        Theme.LIGHT  -> false                    // always light
    }
    val colorScheme = if (useDark) {
        darkColorScheme(
            primary = DarkMainAction,
            surface = DarkShelf,
            surfaceVariant = DarkSection,
            background = DarkBackground,
            onPrimary = DarkAppBarIcons,
            onBackground = DarkText,
            error = DarkError,
            secondary = DarkWarning,
            onError = DarkAppBarIcons
        )
    } else {
        lightColorScheme(
            primary = LightMainAction,
            surface = LightShelf,
            surfaceVariant = LightSection,
            background = LightBackground,
            onPrimary = LightAppBarIcons,
            onBackground = LightText,
            error = LightError,
            secondary = LightWarning,
            onError = LightAppBarIcons
        )
    }
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