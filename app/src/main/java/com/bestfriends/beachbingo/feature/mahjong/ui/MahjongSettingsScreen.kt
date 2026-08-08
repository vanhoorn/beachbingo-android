package com.bestfriends.beachbingo.feature.mahjong.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bestfriends.beachbingo.feature.mahjong.LAYOUT_ORDER
import com.bestfriends.beachbingo.feature.mahjong.LayoutId
import com.bestfriends.beachbingo.feature.mahjong.MahjongDifficulty
import com.bestfriends.beachbingo.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

private val MjAccent = Color(0xFFD4A820)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MahjongSettingsScreen(onNavigateBack: () -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid

    var prefDifficulty by remember { mutableStateOf(MahjongDifficulty.ROOKIE) }
    var prefLayout by remember { mutableStateOf(LayoutId.SCHILDKROETE) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        if (uid == null) return@LaunchedEffect
        try {
            val snap = db.collection("users").document(uid).get().await()
            snap.getString("preferredMahjongDifficulty")?.let { s ->
                runCatching { MahjongDifficulty.valueOf(s) }.getOrNull()?.let { prefDifficulty = it }
            }
            snap.getString("preferredMahjongLayout")?.let { s ->
                runCatching { LayoutId.valueOf(s) }.getOrNull()?.let { prefLayout = it }
            }
        } catch (_: Exception) {}
    }

    fun save() {
        if (uid == null) { onNavigateBack(); return }
        saving = true
        val updates = mutableMapOf<String, Any>(
            "preferredMahjongDifficulty" to prefDifficulty.name,
            "preferredMahjongLayout" to prefLayout.name,
        )
        db.collection("users").document(uid).update(updates)
            .addOnCompleteListener { saving = false; onNavigateBack() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        // ── Header ───────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(SurfaceDark, Surface2Dark)))
                .padding(horizontal = 20.dp, vertical = 20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Surface2Dark,
                    modifier = Modifier
                        .size(40.dp)
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                        .clickable { onNavigateBack() },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = TextSub, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.width(14.dp))
                Text("⚙️", fontSize = 28.sp)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("GEZEITENSTEINE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.5.sp)
                    Text("Einstellungen", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                }
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // ── Standard-Schwierigkeit ────────────────────────────────────────
            Text("Standard-Schwierigkeit", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
            Text(
                "Diese Schwierigkeit wird beim Öffnen der Lobby vorausgewählt.",
                fontSize = 12.sp, color = TextMuted, lineHeight = 16.sp,
            )

            val difficulties = listOf(
                Triple(MahjongDifficulty.ROOKIE, "🐚", "ROOKIE") to "Freie Steine leuchten · Unbegrenzt Hinweise & Mischungen · Kein Timer",
                Triple(MahjongDifficulty.SNIPER, "🎯", "SNIPER") to "Kein Highlight · 3 Hinweise · 1 Mischung · Timer",
                Triple(MahjongDifficulty.BOSS,   "💀", "BOSS LEVEL") to "Keine Hilfen · Kein Mischen · Timer · Nur Highscore",
            )
            difficulties.forEach { (meta, desc) ->
                val (d, emoji, label) = meta
                val sel = prefDifficulty == d
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (sel) MjAccent.copy(0.15f) else SurfaceDark,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (sel) 2.dp else 1.dp,
                            color = if (sel) MjAccent else BorderColor,
                            shape = RoundedCornerShape(14.dp),
                        )
                        .clickable { prefDifficulty = d },
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(emoji, fontSize = 26.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(label, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = if (sel) MjAccent else TextPrimary)
                            Spacer(Modifier.height(2.dp))
                            Text(desc, fontSize = 11.sp, color = TextMuted, lineHeight = 15.sp)
                        }
                        if (sel) {
                            Spacer(Modifier.width(8.dp))
                            Text("✓", fontSize = 18.sp, color = MjAccent, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ── Standard-Layout (bei SNIPER / BOSS sinnvoll) ─────────────────
            HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))
            Text("Standard-Layout", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
            Text(
                "Bei SNIPER und BOSS Level vorausgewählt. Im ROOKIE-Modus immer Schildkröte.",
                fontSize = 12.sp, color = TextMuted, lineHeight = 16.sp,
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LAYOUT_ORDER.forEach { l ->
                    val sel = prefLayout == l
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (sel) MjAccent.copy(0.15f) else SurfaceDark,
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                width = if (sel) 2.dp else 1.dp,
                                color = if (sel) MjAccent else BorderColor,
                                shape = RoundedCornerShape(10.dp),
                            )
                            .clickable { prefLayout = l },
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(l.emoji, fontSize = 20.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                l.label,
                                fontSize = 9.sp,
                                color = if (sel) MjAccent else TextMuted,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Speichern ─────────────────────────────────────────────────────
            Button(
                onClick = { save() },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MjAccent),
                shape = RoundedCornerShape(14.dp),
            ) {
                if (saving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                } else {
                    Text("Speichern", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                }
            }
            OutlinedButton(
                onClick = { onNavigateBack() },
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, TextSub.copy(0.4f)),
            ) {
                Text("Abbrechen", color = TextSub)
            }
        }
    }
}
