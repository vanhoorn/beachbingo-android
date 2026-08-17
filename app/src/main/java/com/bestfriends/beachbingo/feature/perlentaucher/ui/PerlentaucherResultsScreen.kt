package com.bestfriends.beachbingo.feature.perlentaucher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bestfriends.beachbingo.ui.theme.*

private fun starCount(movesLeft: Int): Int = when {
    movesLeft >= 6 -> 3
    movesLeft >= 2 -> 2
    else           -> 1
}

@Composable
fun PerlentaucherResultsScreen(
    level: Int,
    score: Int,
    movesLeft: Int,
    bestScore: Int,
    newBestScore: Boolean,
    onNextLevel: () -> Unit,
    onSaveAndQuit: () -> Unit,
    onNavigateToLobby: () -> Unit,
) {
    val stars = starCount(movesLeft)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(SurfaceDark, BgDark)))
                .padding(horizontal = 20.dp, vertical = 36.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (newBestScore) "🏆" else "🤿", fontSize = EmojiCelebrate)
                Spacer(Modifier.height(12.dp))
                Text(
                    if (newBestScore) "Neuer Rekord!" else "Level geschafft!",
                    fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (newBestScore) OceanBlue else TextPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Level $level",
                    fontSize = MaterialTheme.typography.labelLarge.fontSize,
                    color = TextMuted,
                )
                Spacer(Modifier.height(16.dp))
                // Star row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(3) { i ->
                        val lit = i < stars
                        Text(
                            if (lit) "★" else "☆",
                            fontSize = EmojiMedium,
                            color = if (lit) SandGold else TextMuted.copy(alpha = 0.35f),
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Score + Züge Cards
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ResultStatCard(emoji = "💎", label = "Score", value = "$score", color = OceanBlue, modifier = Modifier.weight(1f))
                ResultStatCard(emoji = "🎯", label = "Übrige Züge", value = "$movesLeft", color = SandGold, modifier = Modifier.weight(1f))
            }

            // Bestenliste-Karte
            Surface(
                color = if (newBestScore) OceanBlue.copy(alpha = 0.12f) else SurfaceDark,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (newBestScore) 2.dp else 1.dp,
                        color = if (newBestScore) OceanBlue else BorderColor,
                        shape = RoundedCornerShape(14.dp),
                    ),
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text("Bestpunktzahl Level $level", fontSize = ChipLabel, color = TextMuted)
                        Text(
                            "$bestScore",
                            fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (newBestScore) OceanBlue else TextPrimary,
                        )
                    }
                    Text(
                        if (newBestScore) "🏆" else "🎖️",
                        fontSize = MaterialTheme.typography.headlineLarge.fontSize,
                    )
                }
            }

            // Stern-Erklärung
            Surface(
                color = SurfaceDark,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    StarHint(stars = 1, label = "Ziel erreicht", earned = stars >= 1)
                    StarHint(stars = 2, label = "2+ Züge übrig", earned = stars >= 2)
                    StarHint(stars = 3, label = "6+ Züge übrig", earned = stars >= 3)
                }
            }

            Spacer(Modifier.height(4.dp))

            if (level < 150) {
                Button(
                    onClick = onNextLevel,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        "Level ${level + 1} starten",
                        fontSize = MaterialTheme.typography.titleSmall.fontSize,
                        fontWeight = FontWeight.ExtraBold,
                        color = BgDark,
                    )
                }

                OutlinedButton(
                    onClick = onSaveAndQuit,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = OceanBlue),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OceanBlue.copy(alpha = 0.5f)),
                ) {
                    Text(
                        "Level ${level + 1} speichern & beenden",
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            OutlinedButton(
                onClick = onNavigateToLobby,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSub),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
            ) {
                Text(
                    "Zur Lobby",
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun ResultStatCard(emoji: String, label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        color = color.copy(alpha = 0.10f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(emoji, fontSize = MaterialTheme.typography.headlineSmall.fontSize)
            Text(label, fontSize = MaterialTheme.typography.labelSmall.fontSize, color = TextMuted)
            Text(value, fontSize = MaterialTheme.typography.headlineSmall.fontSize, fontWeight = FontWeight.ExtraBold, color = color)
        }
    }
}

@Composable
private fun StarHint(stars: Int, label: String, earned: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            "★".repeat(stars) + "☆".repeat(3 - stars),
            fontSize = ChipLabel,
            color = if (earned) SandGold else TextMuted.copy(alpha = 0.35f),
        )
        Text(label, fontSize = ChipLabelTiny, color = if (earned) TextSub else TextMuted.copy(alpha = 0.5f), textAlign = TextAlign.Center)
    }
}
