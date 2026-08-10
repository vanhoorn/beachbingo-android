package com.bestfriends.beachbingo.feature.mahjong.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bestfriends.beachbingo.feature.mahjong.LAYOUT_ORDER
import com.bestfriends.beachbingo.feature.mahjong.LayoutId
import com.bestfriends.beachbingo.feature.mahjong.MahjongDifficulty
import com.bestfriends.beachbingo.ui.components.GameSettingsScaffold
import com.bestfriends.beachbingo.ui.components.SettingsGroupLabel
import com.bestfriends.beachbingo.ui.components.SettingsRadioRow
import com.bestfriends.beachbingo.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@Composable
fun MahjongSettingsScreen(onNavigateBack: () -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid

    var prefDifficulty by remember { mutableStateOf(MahjongDifficulty.ROOKIE) }
    var prefLayout by remember { mutableStateOf(LayoutId.SCHILDKROETE) }
    var saving by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

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

    fun doSave() {
        if (uid == null) { onNavigateBack(); return }
        saving = true
        saved = false
        val updates = mutableMapOf<String, Any>(
            "preferredMahjongDifficulty" to prefDifficulty.name,
            "preferredMahjongLayout" to prefLayout.name,
        )
        db.collection("users").document(uid).update(updates)
            .addOnCompleteListener { saving = false; saved = true }
    }

    GameSettingsScaffold(
        gameLabel = "GEZEITENSTEINE",
        accentColor = MahjongGold,
        saving = saving,
        saved = saved,
        onBack = onNavigateBack,
        onSave = ::doSave,
    ) {
        SettingsGroupLabel("Standard-Schwierigkeit")
        Text(
            "Diese Schwierigkeit wird beim Oeffnen der Lobby vorausgewaehlt.",
            fontSize = ChipLabel,
            color = TextMuted,
            lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
        )

        listOf(
            Triple(MahjongDifficulty.ROOKIE, "🐚 ROOKIE",     "Freie Steine leuchten · Unbegrenzt Hinweise & Mischungen · Kein Timer"),
            Triple(MahjongDifficulty.SNIPER, "🎯 SNIPER",     "Kein Highlight · 3 Hinweise · 1 Mischung · Timer"),
            Triple(MahjongDifficulty.BOSS,   "💀 BOSS LEVEL", "Keine Hilfen · Kein Mischen · Timer · Nur Highscore"),
        ).forEach { (d, label, desc) ->
            SettingsRadioRow(
                title = label,
                desc = desc,
                selected = prefDifficulty == d,
                accentColor = MahjongGold,
                onClick = { prefDifficulty = d },
            )
        }

        HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))
        SettingsGroupLabel("Standard-Layout")
        Text(
            "Bei SNIPER und BOSS Level vorausgewaehlt. Im ROOKIE-Modus immer Schildkroete.",
            fontSize = ChipLabel,
            color = TextMuted,
            lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LAYOUT_ORDER.forEach { l ->
                val sel = prefLayout == l
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (sel) MahjongGold.copy(0.15f) else SurfaceDark,
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            width = if (sel) 2.dp else 1.dp,
                            color = if (sel) MahjongGold else BorderColor,
                            shape = RoundedCornerShape(10.dp),
                        )
                        .clickable { prefLayout = l },
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(l.emoji, fontSize = BingoCallSize)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            l.label,
                            fontSize = StatusTiny,
                            color = if (sel) MahjongGold else TextMuted,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}
