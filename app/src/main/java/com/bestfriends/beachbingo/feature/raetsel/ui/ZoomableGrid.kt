package com.bestfriends.beachbingo.feature.raetsel.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Wraps grid content with pinch-to-zoom (1x–4x) and two-finger pan.
 * Tap coordinates passed to [onTap] and [onLongPress] are already inverse-transformed
 * into the grid's own coordinate space, so callers can map them to cells directly.
 * Double-tap resets to zoom=1 / pan=zero.
 */
@Composable
fun ZoomableGrid(
    modifier: Modifier = Modifier,
    onTap: ((x: Float, y: Float) -> Unit)? = null,
    onLongPress: ((x: Float, y: Float) -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    var zoom by remember { mutableStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    // rememberUpdatedState ensures the coroutines below always call the latest lambda,
    // even though pointerInput(Unit) coroutines are not restarted on recomposition.
    val onTapRef = rememberUpdatedState(onTap)
    val onLongPressRef = rememberUpdatedState(onLongPress)

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTransformGestures { _, panDelta, zoomDelta, _ ->
                    val newZoom = (zoom * zoomDelta).coerceIn(1f, 4f)
                    val maxPanX = size.width * (1f - newZoom)
                    val maxPanY = size.height * (1f - newZoom)
                    zoom = newZoom
                    pan = Offset(
                        (pan.x + panDelta.x).coerceIn(maxPanX, 0f),
                        (pan.y + panDelta.y).coerceIn(maxPanY, 0f),
                    )
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { zoom = 1f; pan = Offset.Zero },
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
            },
            content = content,
        )
    }
}
