package com.bestfriends.beachbingo.feature.strandturm.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bestfriends.beachbingo.ui.theme.*

@Composable
fun StrandturmResultsScreen(
    score: Int,
    level: Int,
    highScore: Int,
    bestLevel: Int,
    newHighScore: Boolean,
    newBestLevel: Boolean,
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
        val isKillScreen = level >= 22

        Text(if (isKillScreen) "💀" else "🗼", fontSize = DrawNumberPhone)

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                if (isKillScreen) "Kill Screen erreicht!" else "SPIEL BEENDET",
                fontSize = ChipLabel, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.5.sp,
            )
            Text("Strandturm", fontSize = MaterialTheme.typography.headlineMedium.fontSize, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
            if (isKillScreen) {
                Text(
                    "Level 22 – der berüchtigte Kill Screen 💀\nTimer-Überlauf macht das Spiel unvollendbar.",
                    fontSize = ChipLabel, color = TextMuted, lineHeight = 18.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }

        if (newHighScore || newBestLevel) {
            Surface(
                color = SandGold.copy(alpha = 0.15f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("🏆", fontSize = MaterialTheme.typography.headlineMedium.fontSize)
                    if (newHighScore && newBestLevel) {
                        Text("Neuer Rekord & neues Höchstlevel!", fontSize = MaterialTheme.typography.labelLarge.fontSize, fontWeight = FontWeight.ExtraBold, color = SandGold)
                    } else if (newHighScore) {
                        Text("Neuer Highscore!", fontSize = MaterialTheme.typography.titleSmall.fontSize, fontWeight = FontWeight.ExtraBold, color = SandGold)
                    } else {
                        Text("Neues Höchstlevel!", fontSize = MaterialTheme.typography.titleSmall.fontSize, fontWeight = FontWeight.ExtraBold, color = SandGold)
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(color = SurfaceDark, shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("$score", fontSize = MaterialTheme.typography.headlineMedium.fontSize, fontWeight = FontWeight.ExtraBold, color = StrandturmRed)
                    Text("Punkte", fontSize = ChipLabel, color = TextMuted)
                }
            }
            Surface(color = SurfaceDark, shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("Lv. $level", fontSize = MaterialTheme.typography.headlineSmall.fontSize, fontWeight = FontWeight.ExtraBold, color = OceanBlue)
                    Text("Level", fontSize = ChipLabel, color = TextMuted)
                }
            }
        }

        // Records
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(color = SurfaceDark, shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("🏆 REKORD", fontSize = ChipLabelTiny, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                    Text(if (highScore > 0) "$highScore" else "—", fontSize = TitleHero, fontWeight = FontWeight.ExtraBold, color = SandGold)
                    if (highScore > 0) Text("Punkte", fontSize = MaterialTheme.typography.labelSmall.fontSize, color = TextMuted)
                }
            }
            Surface(color = SurfaceDark, shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("🎯 BEST", fontSize = ChipLabelTiny, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                    Text(if (bestLevel > 0) "Lv. $bestLevel" else "—", fontSize = MaterialTheme.typography.headlineSmall.fontSize, fontWeight = FontWeight.ExtraBold, color = OceanBlue)
                    if (bestLevel > 0) Text("Level", fontSize = MaterialTheme.typography.labelSmall.fontSize, color = TextMuted)
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onPlayAgain,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StrandturmRed),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("🔄 Nochmal spielen", fontSize = MaterialTheme.typography.labelLarge.fontSize, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onNavigateToHome,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("🏠 Zurück zum Menü", fontSize = MaterialTheme.typography.labelLarge.fontSize, color = TextPrimary)
            }
        }
    }
}
