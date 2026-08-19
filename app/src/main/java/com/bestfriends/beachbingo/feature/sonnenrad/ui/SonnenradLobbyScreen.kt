package com.bestfriends.beachbingo.feature.sonnenrad.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bestfriends.beachbingo.core.model.ALL_GAME_RULES
import com.bestfriends.beachbingo.feature.home.ui.GameRulesBottomSheet
import com.bestfriends.beachbingo.ui.theme.BgDark
import com.bestfriends.beachbingo.ui.theme.BingoCallSize
import com.bestfriends.beachbingo.ui.theme.BorderColor
import com.bestfriends.beachbingo.ui.theme.ChipLabelTiny
import com.bestfriends.beachbingo.ui.theme.MahjongGold
import com.bestfriends.beachbingo.ui.theme.OceanBlue
import com.bestfriends.beachbingo.ui.theme.ScoreLarge
import com.bestfriends.beachbingo.ui.theme.Surface2Dark
import com.bestfriends.beachbingo.ui.theme.SurfaceDark
import com.bestfriends.beachbingo.ui.theme.TextMuted
import com.bestfriends.beachbingo.ui.theme.TextPrimary
import com.bestfriends.beachbingo.ui.theme.TextSub
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import java.util.Calendar

@Composable
fun SonnenradLobbyScreen(
    onNavigateBack: () -> Unit,
    onNavigateToGame: () -> Unit,
) {
    val context = LocalContext.current
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val db = FirebaseFirestore.getInstance()

    val prefs = remember { context.getSharedPreferences("sonnenrad", Context.MODE_PRIVATE) }

    // Bonus-Verfügbarkeit: live aktualisiert via Countdown
    var bonusAvailable by remember { mutableStateOf(isSonnenradBonusAvailable(prefs.getLong("last_claimed", 0L))) }
    var nextBonusMs by remember { mutableLongStateOf(msUntilSonnenradBonus()) }
    val lifetimePoints = remember { prefs.getInt("lifetime_points", 0) }

    // Countdown läuft, solange kein Bonus verfügbar
    LaunchedEffect(bonusAvailable) {
        if (!bonusAvailable) {
            while (true) {
                val ms = msUntilSonnenradBonus()
                nextBonusMs = ms
                if (ms <= 0L) { bonusAvailable = true; break }
                delay(1_000L)
            }
        }
    }

    var isFavorite by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }
    var showRules by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        if (uid == null) return@LaunchedEffect
        try {
            val snap = db.collection("users").document(uid).get().await()
            @Suppress("UNCHECKED_CAST")
            isFavorite = (snap.get("favoriteGames") as? List<String>)?.contains("sonnenrad") == true
        } catch (_: Exception) {}
    }

    fun toggleFavorite() {
        isFavorite = !isFavorite
        val update = if (isFavorite) FieldValue.arrayUnion("sonnenrad") else FieldValue.arrayRemove("sonnenrad")
        if (uid != null) db.collection("users").document(uid).update("favoriteGames", update)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        // ── Header ───────────────────────────────────────────────────────────────
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
                Text("☀️", fontSize = ScoreLarge)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("TAGESBONUS", fontSize = ChipLabelTiny, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.5.sp)
                    Text("Sonnenrad", fontSize = BingoCallSize, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                }
                // Stats
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MahjongGold.copy(alpha = 0.12f),
                    modifier = Modifier
                        .size(36.dp)
                        .border(1.dp, MahjongGold.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .clickable { showStats = true },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🏆", fontSize = MaterialTheme.typography.titleSmall.fontSize)
                    }
                }
                Spacer(Modifier.width(8.dp))
                // Regeln
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MahjongGold.copy(alpha = 0.12f),
                    modifier = Modifier
                        .size(36.dp)
                        .border(1.dp, MahjongGold.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .clickable { showRules = true },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Info, null, tint = MahjongGold, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.width(8.dp))
                // Favorit
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isFavorite) MahjongGold.copy(0.2f) else Surface2Dark,
                    modifier = Modifier
                        .size(36.dp)
                        .border(1.dp, if (isFavorite) MahjongGold else BorderColor, RoundedCornerShape(10.dp))
                        .clickable { toggleFavorite() },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            if (isFavorite) "★" else "☆",
                            fontSize = MaterialTheme.typography.titleMedium.fontSize,
                            color = if (isFavorite) MahjongGold else TextMuted,
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Bonus-Status ─────────────────────────────────────────────────────
            val bonusBorderColor = if (bonusAvailable) MahjongGold.copy(alpha = 0.6f) else BorderColor
            val bonusBgColor = if (bonusAvailable) MahjongGold.copy(alpha = 0.10f) else SurfaceDark

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = bonusBgColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, bonusBorderColor, RoundedCornerShape(16.dp)),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (bonusAvailable) {
                        Text("🌟 Tagesbonus verfügbar!", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = MahjongGold)
                        Spacer(Modifier.height(4.dp))
                        Text("Volle Punkte — bis zu 600 pro Runde", style = MaterialTheme.typography.bodySmall, color = TextMuted, textAlign = TextAlign.Center)
                    } else {
                        Text("Nächster Tagesbonus in", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        Spacer(Modifier.height(4.dp))
                        Text(formatSonnenradMs(nextBonusMs), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text("Normales Spiel: 1/3 Punkte (bis zu 200)", style = MaterialTheme.typography.bodySmall, color = TextMuted, textAlign = TextAlign.Center)
                    }
                }
            }

            // ── Punkte-Gesamt ────────────────────────────────────────────────────
            if (lifetimePoints > 0) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Surface2Dark,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Gesammelte Punkte", style = MaterialTheme.typography.bodyMedium, color = TextSub)
                        Text("$lifetimePoints Pkt.", fontWeight = FontWeight.Bold, color = MahjongGold, fontSize = 15.sp)
                    }
                }
            }

            // ── Punkte-Tabelle ───────────────────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceDark,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text("Bonusleiter", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                    Spacer(Modifier.height(10.dp))
                    val rows = listOf(
                        "Stufe 6 — Maximum" to ("600" to "200"),
                        "Stufe 5"           to ("400" to "133"),
                        "Stufe 4 — Jackpot ☀️" to ("275" to "92"),
                        "Stufe 3"           to ("175" to "58"),
                        "Stufe 2"           to ("100" to "33"),
                        "Stufe 1"           to ("50"  to "17"),
                    )
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                        Text("", modifier = Modifier.weight(1f))
                        Text("Tagesbonus", style = MaterialTheme.typography.labelSmall, color = MahjongGold, modifier = Modifier.width(72.dp), textAlign = TextAlign.End)
                        Text("Normal", style = MaterialTheme.typography.labelSmall, color = TextMuted, modifier = Modifier.width(60.dp), textAlign = TextAlign.End)
                    }
                    HorizontalDivider(color = BorderColor)
                    rows.forEach { (label, pts) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(label, style = MaterialTheme.typography.bodySmall, color = TextSub, modifier = Modifier.weight(1f))
                            Text("${pts.first} Pkt.", style = MaterialTheme.typography.bodySmall, color = MahjongGold, modifier = Modifier.width(72.dp), textAlign = TextAlign.End)
                            Text("${pts.second} Pkt.", style = MaterialTheme.typography.bodySmall, color = TextMuted, modifier = Modifier.width(60.dp), textAlign = TextAlign.End)
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Spielen-Button ───────────────────────────────────────────────────
            Button(
                onClick = onNavigateToGame,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (bonusAvailable) MahjongGold else OceanBlue,
                ),
            ) {
                Text(
                    text = if (bonusAvailable) "🌟 Tagesbonus spielen" else "Spielen",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                )
            }
        }
    }

    // ── Stats-Dialog ─────────────────────────────────────────────────────────────
    if (showStats) {
        AlertDialog(
            onDismissRequest = { showStats = false },
            title = { Text("☀️ Sonnenrad — Statistik") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Gesammelte Punkte", style = MaterialTheme.typography.bodyMedium)
                        Text("$lifetimePoints", fontWeight = FontWeight.Bold, color = MahjongGold)
                    }
                    HorizontalDivider()
                    Text(
                        "Einmal täglich gibt es den vollen Tagesbonus. Normale Runden laufen jederzeit mit 1/3 der Punkte.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showStats = false }) { Text("OK") }
            },
        )
    }

    // ── Regeln ────────────────────────────────────────────────────────────────────
    if (showRules) {
        ALL_GAME_RULES["sonnenrad"]?.let { rule ->
            GameRulesBottomSheet(rule = rule, onDismiss = { showRules = false })
        }
    }
}

// ── Helpers (lokal, kein import von BoardModel nötig) ────────────────────────

private fun isSonnenradBonusAvailable(lastClaimedMs: Long): Boolean {
    if (lastClaimedMs == 0L) return true
    val now  = Calendar.getInstance()
    val last = Calendar.getInstance().apply { timeInMillis = lastClaimedMs }
    return now.get(Calendar.YEAR)         != last.get(Calendar.YEAR) ||
           now.get(Calendar.DAY_OF_YEAR) != last.get(Calendar.DAY_OF_YEAR)
}

private fun msUntilSonnenradBonus(): Long {
    val nextMidnight = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    return (nextMidnight.timeInMillis - System.currentTimeMillis()).coerceAtLeast(0L)
}

private fun formatSonnenradMs(ms: Long): String {
    val total = (ms / 1000L).coerceAtLeast(0L)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return "%02d:%02d:%02d".format(h, m, s)
}
