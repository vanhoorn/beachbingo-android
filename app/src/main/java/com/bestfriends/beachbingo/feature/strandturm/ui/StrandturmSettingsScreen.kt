package com.bestfriends.beachbingo.feature.strandturm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bestfriends.beachbingo.ui.components.GameSettingsScaffold
import com.bestfriends.beachbingo.ui.components.SettingsGroupLabel
import com.bestfriends.beachbingo.ui.components.SettingsRadioRow
import com.bestfriends.beachbingo.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val ADMIN_UID = "oliWTLaCLydkhHl9qF9XZWvSi322"

private data class OptionItem(val id: String, val title: String, val desc: String)

private val CONTROL_OPTIONS = listOf(
    OptionItem("BUTTONS", "🔲 Klassisch",    "D-Pad mittig unter dem Spielfeld – ◄ ▲ ► und ▼"),
    OptionItem("SPLIT",   "✌️ Zwei-Händig", "◄ ► links · ▲ ▼ rechts – ideal für zwei Daumen"),
    OptionItem("TOUCH",   "👆 Touch",        "Bildschirm antippen zum Lenken und Springen"),
)

@Composable
fun StrandturmSettingsScreen(onNavigateBack: () -> Unit, onNavigateToProfile: () -> Unit) {
    val auth      = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val uid       = auth.currentUser?.uid
    val scope     = rememberCoroutineScope()
    val isAdmin   = uid == ADMIN_UID

    var controlMode by remember { mutableStateOf("BUTTONS") }
    var startLevel  by remember { mutableIntStateOf(1) }
    var saving      by remember { mutableStateOf(false) }
    var saved       by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        if (uid == null) return@LaunchedEffect
        val snap = firestore.collection("users").document(uid).get().await()
        controlMode = snap.getString("preferredStrandturmControlMode") ?: "BUTTONS"
        startLevel  = (snap.getLong("strandturmStartLevel") ?: 1L).toInt()
    }

    fun doSave() {
        if (uid == null) return
        saving = true
        scope.launch {
            try {
                val updates = mutableMapOf<String, Any>("preferredStrandturmControlMode" to controlMode)
                if (isAdmin) updates["strandturmStartLevel"] = startLevel
                firestore.collection("users").document(uid).update(updates).await()
            } catch (_: Exception) {}
            saving = false
            saved  = true
        }
    }

    GameSettingsScaffold(
        gameLabel = "STRANDTURM",
        accentColor = StrandturmRed,
        saving = saving,
        saved = saved,
        onBack = onNavigateBack,
        onSave = ::doSave,
        onNavigateToProfile = onNavigateToProfile,
    ) {
        Text(
            "Diese Einstellungen werden beim Start als Standardwerte übernommen.",
            fontSize = CellNumber,
            color = TextMuted,
        )

        SettingsGroupLabel("🕹️ Steuerung")
        CONTROL_OPTIONS.forEach { opt ->
            SettingsRadioRow(
                title = opt.title,
                desc = opt.desc,
                selected = controlMode == opt.id,
                accentColor = StrandturmRed,
                onClick = { controlMode = opt.id },
            )
        }

        // Steuerung im Detail
        Surface(
            color = SurfaceDark,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(14.dp)),
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Steuerung im Detail", fontSize = CellNumber, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(bottom = 4.dp))
                listOf(
                    "Laufen" to "◄ / ► drücken",
                    "Springen" to "▲ drücken (auf dem Boden)",
                    "Leiter hoch" to "▲ an der Leiter halten",
                    "Leiter runter" to "▼ auf der Plattform über Leiter",
                ).forEach { (label, detail) ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("$label:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text(detail, style = MaterialTheme.typography.labelMedium, color = TextMuted)
                    }
                }
            }
        }

        if (isAdmin) {
            Text("🔧 Admin", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = SandGold, modifier = Modifier.padding(start = 4.dp))
            Surface(
                color  = SandGold.copy(alpha = 0.08f),
                shape  = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().border(1.5.dp, SandGold.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Startlevel (nur für Tests)", fontSize = CellNumber, fontWeight = FontWeight.SemiBold, color = SandGold)
                    val levelLabels = listOf("🏗️ Level 1", "🏭 Level 2", "🛗 Level 3", "🔩 Level 4")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        levelLabels.forEachIndexed { idx, label ->
                            val lvl = idx + 1
                            val sel = startLevel == lvl
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (sel) SandGold.copy(alpha = 0.18f) else SurfaceDark)
                                    .border(1.5.dp, if (sel) SandGold else BorderColor, RoundedCornerShape(8.dp))
                                    .clickable { startLevel = lvl }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    label, style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (sel) SandGold else TextMuted, textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
