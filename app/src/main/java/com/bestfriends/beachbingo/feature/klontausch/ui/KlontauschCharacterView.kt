package com.bestfriends.beachbingo.feature.klontausch.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.bestfriends.beachbingo.feature.klontausch.KlonPart

private fun partSuffix(part: KlonPart): String = when (part) {
    KlonPart.KOPF    -> "head"
    KlonPart.KOERPER -> "body"
    KlonPart.BEINE   -> "legs"
}

@Composable
fun KlontauschCharacterView(
    characterId: String,
    part: KlonPart,
    modifier: Modifier = Modifier,
) {
    val context   = LocalContext.current
    val assetName = "klontausch/${characterId}_${partSuffix(part)}.png"
    val bitmap = remember(assetName) {
        runCatching {
            BitmapFactory.decodeStream(context.assets.open(assetName))?.asImageBitmap()
        }.getOrNull()
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier,
        )
    } else {
        KlontauschSilhouette(part = part, modifier = modifier)
    }
}

@Composable
fun KlontauschSilhouette(
    part: KlonPart,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(Color(0xFF1E2D45))
            .border(1.dp, Color(0xFF3A5070)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "?",
            color = Color(0xFF4A6080),
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}
