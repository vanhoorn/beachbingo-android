package com.bestfriends.beachbingo.feature.pirates.ui

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bestfriends.beachbingo.ui.theme.*

private val Purple = Color(0xFFA855F7)

private fun diffLabel(diff: String) = when (diff) {
    "ROOKIE"     -> "🌊 Rookie"
    "SNIPER"     -> "🎯 Sniper"
    "BOSS_LEVEL" -> "💪 Boss Level"
    else         -> diff
}

@Composable
fun PiratesResultsScreen(
    score: Int,
    wave: Int,
    difficulty: String,
    highScore: Int,
    newHighScore: Boolean,
    onPlayAgain: () -> Unit,
    onNavigateToHome: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color(0xFF1a0a2e), Color(0xFF0a1628))))
                .padding(horizontal = 20.dp, vertical = 36.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (newHighScore) "🏆" else "💀", fontSize = 72.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    if (newHighScore) "Neuer Rekord!" else "Game Over",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (newHighScore) Purple else TextPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text(diffLabel(difficulty), fontSize = 15.sp, color = TextMuted)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Score cards row
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(emoji = "⭐", label = "Score", value = "$score", color = Purple, modifier = Modifier.weight(1f))
                StatCard(emoji = "🌊", label = "Welle", value = "$wave", color = OceanBlue, modifier = Modifier.weight(1f))
            }

            // High score card
            Surface(
                color = if (newHighScore) Purple.copy(alpha = 0.15f) else SurfaceDark,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (newHighScore) 2.dp else 1.dp,
                        color = if (newHighScore) Purple else BorderColor,
                        shape = RoundedCornerShape(14.dp),
                    ),
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text("Rekord (${diffLabel(difficulty)})", fontSize = 12.sp, color = TextMuted)
                        Text("$highScore", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold,
                            color = if (newHighScore) Purple else TextPrimary)
                    }
                    Text(if (newHighScore) "🏆" else "🎖️", fontSize = 36.sp)
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onPlayAgain,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Purple),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("🔄  Nochmal spielen", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onNavigateToHome,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSub),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
            ) {
                Text("🏠  Zurück zum Menü", fontSize = 17.sp)
            }
        }
    }
}

@Composable
private fun StatCard(emoji: String, label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(emoji, fontSize = 24.sp)
            Text(label, fontSize = 11.sp, color = TextMuted)
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = color)
        }
    }
}
