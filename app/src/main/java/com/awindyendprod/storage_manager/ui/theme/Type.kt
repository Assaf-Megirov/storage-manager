package com.awindyendprod.storage_manager.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.awindyendprod.storage_manager.model.FontSize

// Set of Material typography styles to start with
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )
)

fun Typography.withCustomSize(fontSize: FontSize): Typography {
    val scaleFactor = when (fontSize) {
        FontSize.SMALL -> 0.8f
        FontSize.MEDIUM -> 1f
        FontSize.LARGE -> 1.2f
    }
    
    return this.copy(
        bodyLarge = bodyLarge.copy(fontSize = bodyLarge.fontSize * scaleFactor),
        bodyMedium = bodyMedium.copy(fontSize = bodyMedium.fontSize * scaleFactor),
        bodySmall = bodySmall.copy(fontSize = bodySmall.fontSize * scaleFactor),
        titleLarge = titleLarge.copy(fontSize = titleLarge.fontSize * scaleFactor),
        titleMedium = titleMedium.copy(fontSize = titleMedium.fontSize * scaleFactor),
        titleSmall = titleSmall.copy(fontSize = titleSmall.fontSize * scaleFactor)
    )
} 