package com.awindyendprod.storage_manager.ui.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun DraggableFloatingActionButton(
    onClick: () -> Unit,
    isDragEnabled: Boolean,
    initialPosition: Offset? = null,
    onPositionChanged: (Offset) -> Unit = {},
    content: @Composable () -> Unit
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val fabSize = 56.dp
    
    // Calculate default position (bottom-right with margin)
    val defaultPosition = with(density) {
        Offset(
            x = (screenWidth - fabSize - 16.dp).toPx(),
            y = (screenHeight - fabSize - 16.dp).toPx()
        )
    }
    
    var position by remember { mutableStateOf(initialPosition ?: defaultPosition) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(initialPosition, defaultPosition) {
        if (!isDragging) {
            position = initialPosition ?: defaultPosition
        }
    }
    
    FloatingActionButton(
        onClick = {
            if (!isDragging) {
                onClick()
            }
        },
        modifier = Modifier
            .size(fabSize)
            .offset {
                IntOffset(
                    x = position.x.roundToInt(),
                    y = position.y.roundToInt()
                )
            }
            .pointerInput(isDragEnabled) {
                if (isDragEnabled) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = { 
                            isDragging = false
                            onPositionChanged(position)
                        }
                    ) { _, dragAmount ->
                        val newPosition = position + dragAmount

                        val maxX = with(density) { (screenWidth - fabSize).toPx() }
                        val maxY = with(density) { (screenHeight - fabSize).toPx() }

                        position = Offset(
                            x = newPosition.x.coerceIn(0f, maxX),
                            y = newPosition.y.coerceIn(0f, maxY)
                        )
                    }
                }
            }
    ) {
        content()
    }
}
