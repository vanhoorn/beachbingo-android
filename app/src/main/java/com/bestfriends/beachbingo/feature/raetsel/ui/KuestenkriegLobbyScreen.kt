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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bestfriends.beachbingo.feature.raetsel.KRIEG_FLEET
import com.bestfriends.beachbingo.feature.raetsel.KRIEG_GRID_SIZES
import com.bestfriends.beachbingo.feature.raetsel.PuzzleSaveManager
import com.bestfriends.beachbingo.ui.theme.*

private val KkAccent = Color(0xFFFB7185)

private fun fleetLabel(fleet: List<Int>): String {
    val counts = fleet.groupBy { it }
    return counts.entries.sortedByDescending { it.key }.joinToString(", ") { "${it.value.size}×${it.key}er" }
}

private enum class KkMode { PUZZLE, KI, ONLINE }

private data class AiOption(val id: String, val label: String, val desc: String, val emoji: String)

private val AI_OPTIONS = listOf(
    AiOption("matrose",  "Matrose",  "Schießt zufällig",             "🌊"),
    AiOption("kapitaen", "Kapitän",  "Wahrscheinlichkeitsbasiert",   "⚓"),
    AiOption("admiral",  "Admiral",  "Sucht & zielt — stärkste KI", "🏴‍☠️"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KuestenkriegLobbyScreen(
    onNavigateBack: () -> Unit,
    onNavigateToGame: (difficulty: String, seed: Long, saveId: String?) -> Unit,
    onNavigateToPlacement: (aiMode: String) -> Unit,
) {
    val context = LocalContext.current
    var mode by remember { mutableStateOf(KkMode.PUZZLE) }
    var selectedDiff by remember { mutableStateOf("mittel") }
    var selectedAi by remember { mutableStateOf("kapitaen") }
    val saves = remember { PuzzleSaveManager.getSaves(context).filter { it.gameType == "kuestenkrieg" } }
    val difficulties = listOf("leicht", "mittel", "schwer", "experte")
    val diffLabels = mapOf("leicht" to "Leicht", "mittel" to "Mittel", "schwer" to "Schwer", "experte" to "Experte")

    Column(modifier = Modifier.fillMaxSize().background(BgDark).statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState())) {
        // Header
        Box(modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(SurfaceDark, Surface2Dark))).padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(12.dp), color = Surface2Dark,
                    modifier = Modifier.size(40.dp).border(1.dp, BorderColor, RoundedCornerShape(12.dp)).clickable { onNavigateBack() }
                ) { Box(contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = TextSub, modifier = Modifier.size(20.dp)) } }
                Spacer(Modifier.width(14.dp))
                Text("⚓", fontSize = 32.sp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("RÄTSEL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.5.sp)
                    Text("Küstenkrieg", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                }
            }
        }

        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {

            // Mode selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("SPIELMODUS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                listOf(
                    Triple(KkMode.PUZZLE, "🧩", "Solo Rätsel" to "Zahlen am Rand verraten die Schiffe"),
                    Triple(KkMode.KI,     "🤖", "Gegen KI"    to "Klassisches Schiffe versenken"),
                    Triple(KkMode.ONLINE, "🌐", "Online"      to "Gegen echten Spieler (demnächst)"),
                ).forEach { (m, emoji, texts) ->
                    val sel = mode == m
                    val disabled = m == KkMode.ONLINE
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (sel) KkAccent.copy(alpha = 0.08f) else SurfaceDark,
                        modifier = Modifier.fillMaxWidth()
                            .border(1.5.dp, if (sel) KkAccent else BorderColor, RoundedCornerShape(12.dp))
                            .alpha(if (disabled) 0.45f else 1f)
                            .let { if (!disabled) it.clickable { mode = m } else it }
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(emoji, fontSize = 22.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(texts.first, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (sel) KkAccent else if (disabled) TextMuted else TextPrimary)
                                    if (disabled) {
                                        Spacer(Modifier.width(6.dp))
                                        Text("demnächst", fontSize = 11.sp, color = TextMuted)
                                    }
                                }
                                Text(texts.second, fontSize = 12.sp, color = TextMuted, modifier = Modifier.padding(top = 2.dp))
                            }
                            if (sel) Text("✓", fontSize = 18.sp, color = KkAccent)
                        }
                    }
                }
            }

            // Puzzle options
            if (mode == KkMode.PUZZLE) {
                Surface(shape = RoundedCornerShape(12.dp), color = SurfaceDark, modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(12.dp))) {
                    Text("Schlachtschiff-Rätsel: Zahlen am Rand zeigen Schiffsfelder pro Zeile/Spalte. Schiffe berühren sich nie diagonal. Tippen = Schiff, Lang drücken = Wasser.",
                        fontSize = 13.sp, color = TextMuted, lineHeight = 20.sp, modifier = Modifier.padding(14.dp))
                }
                Text("SCHWIERIGKEIT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                difficulties.forEach { d ->
                    val sel = selectedDiff == d
                    Surface(shape = RoundedCornerShape(12.dp), color = if (sel) KkAccent.copy(alpha = 0.1f) else SurfaceDark,
                        modifier = Modifier.fillMaxWidth().border(1.5.dp, if (sel) KkAccent else BorderColor, RoundedCornerShape(12.dp)).clickable { selectedDiff = d }
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(diffLabels[d] ?: d, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (sel) KkAccent else TextPrimary)
                                val size = KRIEG_GRID_SIZES[d] ?: 10
                                val fleet = KRIEG_FLEET[d] ?: emptyList()
                                Text("${size}×${size} · ${fleetLabel(fleet)}", fontSize = 11.sp, color = TextMuted, modifier = Modifier.padding(top = 3.dp))
                            }
                            if (sel) Text("✓", fontSize = 18.sp, color = KkAccent)
                        }
                    }
                }
                Button(onClick = { onNavigateToGame(selectedDiff, System.currentTimeMillis(), null) },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KkAccent),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Neues Rätsel", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = BgDark) }

                // Saved puzzle games
                if (saves.isNotEmpty()) {
                    Text("GESPEICHERTE RÄTSEL", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                    saves.forEach { save ->
                        Surface(shape = RoundedCornerShape(12.dp), color = SurfaceDark, modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(12.dp))) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${diffLabels[save.difficulty] ?: save.difficulty} · ${KRIEG_GRID_SIZES[save.difficulty] ?: 10}×${KRIEG_GRID_SIZES[save.difficulty] ?: 10}",
                                        fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text(PuzzleSaveManager.formatElapsed(save.elapsedSeconds) + " gespielt", fontSize = 12.sp, color = TextMuted, modifier = Modifier.padding(top = 2.dp))
                                }
                                Surface(shape = RoundedCornerShape(8.dp), color = KkAccent.copy(alpha = 0.1f),
                                    modifier = Modifier.border(1.dp, KkAccent.copy(alpha = 0.4f), RoundedCornerShape(8.dp)).clickable { onNavigateToGame(save.difficulty, save.seed, save.id) }
                                ) { Text("Fortsetzen", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = KkAccent, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)) }
                            }
                        }
                    }
                }
            }

            // KI options
            if (mode == KkMode.KI) {
                Text("KI-SCHWIERIGKEIT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                AI_OPTIONS.forEach { opt ->
                    val sel = selectedAi == opt.id
                    Surface(shape = RoundedCornerShape(12.dp), color = if (sel) KkAccent.copy(alpha = 0.08f) else SurfaceDark,
                        modifier = Modifier.fillMaxWidth().border(1.5.dp, if (sel) KkAccent else BorderColor, RoundedCornerShape(12.dp)).clickable { selectedAi = opt.id }
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(opt.emoji, fontSize = 22.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(opt.label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (sel) KkAccent else TextPrimary)
                                Text(opt.desc, fontSize = 12.sp, color = TextMuted, modifier = Modifier.padding(top = 2.dp))
                            }
                            if (sel) Text("✓", fontSize = 18.sp, color = KkAccent)
                        }
                    }
                }
                Button(onClick = { onNavigateToPlacement(selectedAi) },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KkAccent),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Schiffe setzen →", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = BgDark) }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}
