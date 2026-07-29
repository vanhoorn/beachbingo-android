package com.bestfriends.beachbingo.feature.raetsel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bestfriends.beachbingo.feature.raetsel.PuzzleSaveManager
import com.bestfriends.beachbingo.feature.raetsel.STRANDOKU_VARIANT_DESCRIPTIONS
import com.bestfriends.beachbingo.feature.raetsel.STRANDOKU_VARIANT_LABELS
import com.bestfriends.beachbingo.ui.theme.*

private val SdAccent = Color(0xFF38BDF8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrandokuLobbyScreen(
    onNavigateBack: () -> Unit,
    onNavigateToGame: (variant: String, difficulty: String, seed: Long, saveId: String?) -> Unit,
) {
    val context = LocalContext.current
    var variant by remember { mutableStateOf("classic") }
    var difficulty by remember { mutableStateOf("mittel") }
    var saves by remember { mutableStateOf(PuzzleSaveManager.getSaves(context).filter { it.gameType == "strandoku" }) }
    val variants = listOf("classic", "mega12", "mega16", "irregular", "diagonal", "killer", "samurai")
    val difficulties = listOf("leicht", "mittel", "schwer", "experte")
    val diffLabels = mapOf("leicht" to "Leicht", "mittel" to "Mittel", "schwer" to "Schwer", "experte" to "Experte")

    Column(
        modifier = Modifier.fillMaxSize().background(BgDark).statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState())
    ) {
        Box(modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(SurfaceDark, Surface2Dark))).padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(12.dp), color = Surface2Dark,
                    modifier = Modifier.size(40.dp).border(1.dp, BorderColor, RoundedCornerShape(12.dp)).clickable { onNavigateBack() }
                ) { Box(contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = TextSub, modifier = Modifier.size(20.dp)) } }
                Spacer(Modifier.width(14.dp))
                Text("🔢", fontSize = 32.sp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("RÄTSEL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.5.sp)
                    Text("Strandoku", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                }
            }
        }
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("VARIANTE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
            variants.forEach { v ->
                val sel = variant == v
                Surface(shape = RoundedCornerShape(12.dp), color = if (sel) SdAccent.copy(alpha = 0.1f) else SurfaceDark,
                    modifier = Modifier.fillMaxWidth().border(1.5.dp, if (sel) SdAccent else BorderColor, RoundedCornerShape(12.dp)).clickable { variant = v }
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(STRANDOKU_VARIANT_LABELS[v] ?: v, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (sel) SdAccent else TextPrimary)
                            Text(STRANDOKU_VARIANT_DESCRIPTIONS[v] ?: "", fontSize = 12.sp, color = TextMuted, modifier = Modifier.padding(top = 2.dp))
                        }
                        if (sel) Text("✓", fontSize = 18.sp, color = SdAccent)
                    }
                }
            }
            Text("SCHWIERIGKEIT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
            difficulties.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { d ->
                        val sel = difficulty == d
                        Surface(shape = RoundedCornerShape(12.dp), color = if (sel) SdAccent.copy(alpha = 0.1f) else SurfaceDark,
                            modifier = Modifier.weight(1f).border(1.5.dp, if (sel) SdAccent else BorderColor, RoundedCornerShape(12.dp)).clickable { difficulty = d }
                        ) { Text(diffLabels[d] ?: d, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (sel) SdAccent else TextPrimary, modifier = Modifier.padding(14.dp)) }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
            }
            Button(onClick = { onNavigateToGame(variant, difficulty, System.currentTimeMillis(), null) },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SdAccent),
                shape = RoundedCornerShape(14.dp)
            ) { Text("Neues Spiel starten", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = BgDark) }
            if (saves.isNotEmpty()) {
                Text("GESPEICHERTE SPIELE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                saves.forEach { save ->
                    Surface(shape = RoundedCornerShape(12.dp), color = SurfaceDark,
                        modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${STRANDOKU_VARIANT_LABELS[save.variant] ?: save.variant} · ${diffLabels[save.difficulty] ?: save.difficulty}",
                                    fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(PuzzleSaveManager.formatElapsed(save.elapsedSeconds) + " gespielt", fontSize = 12.sp, color = TextMuted, modifier = Modifier.padding(top = 2.dp))
                            }
                            Surface(shape = RoundedCornerShape(8.dp), color = SdAccent.copy(alpha = 0.1f),
                                modifier = Modifier.border(1.dp, SdAccent.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .clickable { onNavigateToGame(save.variant, save.difficulty, save.seed, save.id) }
                            ) { Text("Fortsetzen", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SdAccent, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)) }
                            Spacer(Modifier.width(8.dp))
                            Surface(shape = RoundedCornerShape(8.dp), color = Danger.copy(alpha = 0.1f),
                                modifier = Modifier.border(1.dp, Danger.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .clickable { PuzzleSaveManager.deleteSave(context, save.id); saves = saves.filter { it.id != save.id } }
                            ) { Text("✕", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Danger, modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}
