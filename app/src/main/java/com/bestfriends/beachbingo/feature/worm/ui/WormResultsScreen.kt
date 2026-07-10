package com.bestfriends.beachbingo.feature.worm.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bestfriends.beachbingo.ui.theme.*

private val WormGreen = Color(0xFF22C55E)

private val DIFF_LABEL = mapOf(
    "ROOKIE"     to "🌊 Rookie",
    "SNIPER"     to "🎯 Sniper",
    "BOSS_LEVEL" to "💪 Boss Level",
)

@Composable
fun WormResultsScreen(
    score: Int,
    length: Int,
    difficulty: String,
    highScore: Int,
    newHighScore: Boolean,
    onPlayAgain: () -> Unit,
    onNavigateToHome: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("🪱", fontSize = 64.sp)

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("SPIEL BEENDET", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.5.sp)
            Text("Wattwurm", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
            Text(DIFF_LABEL[difficulty] ?: difficulty, fontSize = 14.sp, color = TextMuted)
        }

        if (newHighScore) {
            Surface(
                color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("🏆", fontSize = 28.sp)
                    Text("Neuer Rekord!", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = SandGold)
                }
            }
        }

        // Score / length tiles
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(color = SurfaceDark, shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("$score", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = WormGreen)
                    Text("Punkte", fontSize = 12.sp, color = TextMuted)
                }
            }
            Surface(color = SurfaceDark, shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("$length", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = OceanBlue)
                    Text("Länge", fontSize = 12.sp, color = TextMuted)
                }
            }
        }

        // Highscore tile
        Surface(color = SurfaceDark, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("🏆 REKORD ${DIFF_LABEL[difficulty]?.uppercase() ?: ""}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                Text(if (highScore > 0) "$highScore" else "—", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = SandGold)
                if (highScore > 0) Text("Punkte", fontSize = 12.sp, color = TextMuted)
            }
        }

        // Buttons
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onPlayAgain,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WormGreen),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("🔄 Nochmal spielen", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onNavigateToHome,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("🏠 Zurück zum Menü", fontSize = 15.sp, color = TextPrimary)
            }
        }
    }
}
