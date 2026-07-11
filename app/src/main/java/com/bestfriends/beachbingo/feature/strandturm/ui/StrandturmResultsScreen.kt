package com.bestfriends.beachbingo.feature.strandturm.ui

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bestfriends.beachbingo.ui.theme.*

private val StrandturmRed = Color(0xFFDC2626)

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

        Text(if (isKillScreen) "💀" else "🗼", fontSize = 64.sp)

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                if (isKillScreen) "Kill Screen erreicht!" else "SPIEL BEENDET",
                fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.5.sp,
            )
            Text("Strandturm", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
            if (isKillScreen) {
                Text(
                    "Level 22 – der berüchtigte Kill Screen 💀\nTimer-Überlauf macht das Spiel unvollendbar.",
                    fontSize = 12.sp, color = TextMuted, lineHeight = 18.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }

        if (newHighScore || newBestLevel) {
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
                    if (newHighScore && newBestLevel) {
                        Text("Neuer Rekord & neues Höchstlevel!", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = SandGold)
                    } else if (newHighScore) {
                        Text("Neuer Highscore!", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = SandGold)
                    } else {
                        Text("Neues Höchstlevel!", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = SandGold)
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
                    Text("$score", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = StrandturmRed)
                    Text("Punkte", fontSize = 12.sp, color = TextMuted)
                }
            }
            Surface(color = SurfaceDark, shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("Lv. $level", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = OceanBlue)
                    Text("Level", fontSize = 12.sp, color = TextMuted)
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
                    Text("🏆 REKORD", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                    Text(if (highScore > 0) "$highScore" else "—", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = SandGold)
                    if (highScore > 0) Text("Punkte", fontSize = 11.sp, color = TextMuted)
                }
            }
            Surface(color = SurfaceDark, shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("🎯 BEST", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                    Text(if (bestLevel > 0) "Lv. $bestLevel" else "—", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = OceanBlue)
                    if (bestLevel > 0) Text("Level", fontSize = 11.sp, color = TextMuted)
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
