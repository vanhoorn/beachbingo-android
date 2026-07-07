package com.bestfriends.beachbingo.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bestfriends.beachbingo.ui.theme.BorderColor
import com.bestfriends.beachbingo.ui.theme.Danger
import com.bestfriends.beachbingo.ui.theme.OceanBlue
import com.bestfriends.beachbingo.ui.theme.Surface2Dark
import com.bestfriends.beachbingo.ui.theme.SurfaceDark
import com.bestfriends.beachbingo.ui.theme.TextPrimary

/**
 * Wiederverwendbare HUD-Leiste für alle Spiel-Screens.
 *
 * [content] liefert spielspezifische Infos (Score, Züge etc.) als Row-Inhalt.
 * Rechts stehen immer: Favorit-Stern | Pause | Abbruch.
 */
@Composable
fun GameHudBar(
    paused: Boolean,
    onPauseToggle: () -> Unit,
    onQuit: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        color = SurfaceDark,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Spielspezifische Info links
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                content()
            }

            // Pause / Play
            HudButton(
                label = if (paused) "▶" else "⏸",
                color = if (paused) OceanBlue.copy(0.25f) else Surface2Dark,
                borderColor = if (paused) OceanBlue else BorderColor,
                textColor = TextPrimary,
                onClick = onPauseToggle,
            )

            // Abbruch
            HudButton(
                label = "✕",
                color = Danger.copy(0.18f),
                borderColor = Danger.copy(0.5f),
                textColor = Danger,
                bold = true,
                onClick = onQuit,
            )
        }
    }
}

@Composable
private fun HudButton(
    label: String,
    color: androidx.compose.ui.graphics.Color,
    borderColor: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color,
    bold: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        color = color,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .size(36.dp)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp)),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize().then(
                Modifier.pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val e = awaitPointerEvent()
                            if (e.changes.any { it.pressed }) onClick()
                            e.changes.forEach { it.consume() }
                        }
                    }
                }
            )
        ) {
            Text(
                label,
                fontSize = 16.sp,
                color = textColor,
                fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}

/** Standard-Abbruch-Bestätigungsdialog */
@Composable
fun QuitConfirmDialog(
    title: String = "Spiel abbrechen?",
    message: String = "Dein Fortschritt geht verloren.",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🏳️ $title") },
        text = { Text(message) },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = onConfirm,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = Danger,
                ),
            ) { Text("Abbrechen") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Weiterspielen") }
        },
        containerColor = SurfaceDark,
        titleContentColor = TextPrimary,
        textContentColor = TextPrimary.copy(alpha = 0.8f),
    )
}
