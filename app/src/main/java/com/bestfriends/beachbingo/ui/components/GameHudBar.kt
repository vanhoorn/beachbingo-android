package com.bestfriends.beachbingo.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.bestfriends.beachbingo.ui.theme.BorderColor
import com.bestfriends.beachbingo.ui.theme.Danger
import com.bestfriends.beachbingo.ui.theme.EmojiLarge
import com.bestfriends.beachbingo.ui.theme.OceanBlue
import com.bestfriends.beachbingo.ui.theme.Surface2Dark
import com.bestfriends.beachbingo.ui.theme.SurfaceDark
import com.bestfriends.beachbingo.ui.theme.TextPrimary
import com.bestfriends.beachbingo.ui.theme.TextSub

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
    showPause: Boolean = true,
    onShowRules: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        color = SurfaceDark,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
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

            // Regeln
            if (onShowRules != null) {
                HudButton(
                    icon = Icons.Filled.Info,
                    contentDescription = "Regeln",
                    color = Surface2Dark,
                    borderColor = BorderColor,
                    tint = TextSub,
                    onClick = onShowRules,
                )
            }

            // Pause / Play
            if (showPause) {
                HudButton(
                    icon = if (paused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                    contentDescription = if (paused) "Weiterspielen" else "Pause",
                    color = if (paused) OceanBlue.copy(0.25f) else Surface2Dark,
                    borderColor = if (paused) OceanBlue else BorderColor,
                    tint = TextPrimary,
                    onClick = onPauseToggle,
                )
            }

            // Abbruch
            HudButton(
                icon = Icons.Filled.Close,
                contentDescription = "Beenden",
                color = Danger.copy(0.18f),
                borderColor = Danger.copy(0.5f),
                tint = Danger,
                onClick = onQuit,
            )
        }
    }
}

@Composable
private fun HudButton(
    icon: ImageVector,
    contentDescription: String,
    color: androidx.compose.ui.graphics.Color,
    borderColor: androidx.compose.ui.graphics.Color,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = color,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .size(48.dp)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp)),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/** 3-Option-Dialog für speicherbare Spiele */
@Composable
fun GameSaveQuitDialog(
    emoji: String = "🏳️",
    message: String = "",
    hideSave: Boolean = false,
    onContinue: () -> Unit,
    onSaveAndQuit: () -> Unit,
    onQuitWithoutSave: () -> Unit,
) {
    Dialog(onDismissRequest = onContinue) {
        Surface(shape = RoundedCornerShape(20.dp), color = SurfaceDark) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(emoji, fontSize = EmojiLarge)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Spiel beenden?",
                    fontSize = MaterialTheme.typography.titleMedium.fontSize,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                )
                Spacer(Modifier.height(4.dp))
                if (message.isNotEmpty()) {
                    Text(
                        message,
                        fontSize = MaterialTheme.typography.labelMedium.fontSize,
                        color = TextSub,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(20.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedButton(
                        onClick = onContinue,
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, TextSub.copy(alpha = 0.4f)),
                    ) { Text("Weiterspielen", color = TextSub, fontWeight = FontWeight.Bold) }
                    if (!hideSave) {
                        Button(
                            onClick = onSaveAndQuit,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
                        ) { Text("💾 Speichern & Beenden") }
                    }
                    OutlinedButton(
                        onClick = onQuitWithoutSave,
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Danger.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger),
                    ) { Text("✕ Beenden ohne Speichern", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

/** Standard-Abbruch-Bestätigungsdialog (ohne Speichern-Option) */
@Composable
fun QuitConfirmDialog(
    emoji: String = "🏳️",
    message: String = "Dein Fortschritt geht verloren.",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = SurfaceDark) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(emoji, fontSize = EmojiLarge)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Spiel beenden?",
                    fontSize = MaterialTheme.typography.titleMedium.fontSize,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    message,
                    fontSize = MaterialTheme.typography.labelMedium.fontSize,
                    color = TextSub,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, TextSub.copy(alpha = 0.4f)),
                    ) { Text("Weiterspielen", color = TextSub, fontWeight = FontWeight.Bold) }
                    OutlinedButton(
                        onClick = onConfirm,
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Danger.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger),
                    ) { Text("✕ Beenden", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
