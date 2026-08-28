package com.bestfriends.beachbingo.feature.raetsel.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Wraps grid content with pinch-to-zoom ([initialZoom]–4x) and two-finger pan.
 * Tap coordinates passed to [onTap] and [onLongPress] are already inverse-transformed
 * into the grid's own coordinate space, so callers can map them to cells directly.
 * Double-tap resets to [initialZoom] / pan=zero.
 */
@Composable
fun ZoomableGrid(
    modifier: Modifier = Modifier,
    initialZoom: Float = 1f,
    initialPan: Offset = Offset.Zero,
    onTap: ((x: Float, y: Float) -> Unit)? = null,
    onLongPress: ((x: Float, y: Float) -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    var zoom by remember { mutableStateOf(initialZoom) }
    var pan by remember { mutableStateOf(initialPan) }
    // rememberUpdatedState ensures the coroutines below always call the latest lambda,
    // even though pointerInput(Unit) coroutines are not restarted on recomposition.
    val onTapRef = rememberUpdatedState(onTap)
    val onLongPressRef = rememberUpdatedState(onLongPress)

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTransformGestures { centroid, panDelta, zoomDelta, _ ->
                    val prevZoom = zoom
                    val newZoom = (zoom * zoomDelta).coerceIn(initialZoom, 4f)
                    // Adjust pan so the pinch centroid stays fixed on screen
                    val ratio = newZoom / prevZoom
                    val newPanX = centroid.x + (pan.x - centroid.x) * ratio + panDelta.x
                    val newPanY = centroid.y + (pan.y - centroid.y) * ratio + panDelta.y
                    val maxPanX = size.width * (1f - newZoom)
                    val maxPanY = size.height * (1f - newZoom)
                    zoom = newZoom
                    pan = Offset(
                        newPanX.coerceIn(minOf(maxPanX, 0f), maxOf(maxPanX, 0f)),
                        newPanY.coerceIn(minOf(maxPanY, 0f), maxOf(maxPanY, 0f)),
                    )
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { zoom = initialZoom; pan = initialPan },
                    onTap = { raw ->
                        val gx = (raw.x - pan.x) / zoom
                        val gy = (raw.y - pan.y) / zoom
                        onTapRef.value?.invoke(gx, gy)
                    },
                    onLongPress = { raw ->
                        val gx = (raw.x - pan.x) / zoom
                        val gy = (raw.y - pan.y) / zoom
                        onLongPressRef.value?.invoke(gx, gy)
                    },
                )
            },
    ) {
        Box(
            modifier = Modifier.graphicsLayer {
                scaleX = zoom
                scaleY = zoom
                translationX = pan.x
                translationY = pan.y
                transformOrigin = TransformOrigin(0f, 0f)
            },
            content = content,
        )
    }
}
