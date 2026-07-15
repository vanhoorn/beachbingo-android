package com.bestfriends.beachbingo.feature.worm.ui

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bestfriends.beachbingo.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private val WormGreen = Color(0xFF22C55E)

private data class OptionItem(val id: String, val title: String, val desc: String)

private val DIFF_OPTIONS = listOf(
    OptionItem("ROOKIE",     "🌊 Rookie",     "Gemütliches Tempo · Wände töten · Ideal zum Starten"),
    OptionItem("SNIPER",     "🎯 Sniper",     "Flottes Tempo · Wände töten · Echte Herausforderung"),
    OptionItem("BOSS_LEVEL", "💪 Boss Level", "Volles Tempo · Wände töten · Viel Spaß 😈"),
)

private val CONTROL_OPTIONS = listOf(
    OptionItem("BUTTONS", "🔲 Buttons", "Vier Pfeil-Buttons unter dem Spielfeld"),
    OptionItem("SWIPE",   "👆 Swipe",   "Auf dem Spielfeld wischen zum Lenken"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WormSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
) {
    val auth      = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val uid       = auth.currentUser?.uid
    val scope     = rememberCoroutineScope()

    var difficulty  by remember { mutableStateOf("ROOKIE") }
    var controlMode by remember { mutableStateOf("BUTTONS") }
    var saving      by remember { mutableStateOf(false) }
    var saved       by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        if (uid == null) return@LaunchedEffect
        val snap = firestore.collection("users").document(uid).get().await()
        difficulty  = snap.getString("preferredWormDifficulty")  ?: "ROOKIE"
        controlMode = snap.getString("preferredWormControlMode") ?: "BUTTONS"
    }

    fun doSave() {
        if (uid == null) return
        saving = true
        scope.launch {
            try {
                firestore.collection("users").document(uid).update(
                    mapOf(
                        "preferredWormDifficulty"  to difficulty,
                        "preferredWormControlMode" to controlMode,
                    )
                ).await()
            } catch (_: Exception) {}
            saving = false
            saved  = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("WATTWURM", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("⚙️ Einstellungen", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.ExtraBold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück", tint = TextPrimary)
                    }
                },
                actions = {
                    TextButton(onClick = { doSave() }, enabled = !saving) {
                        Text(if (saving) "…" else "Speichern", color = WormGreen, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark),
            )
        },
        containerColor = BgDark,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Diese Einstellungen werden beim Start als Standardwerte übernommen.",
                fontSize = 14.sp, color = TextMuted,
            )

            // Difficulty
            Text("Schwierigkeit", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextMuted, modifier = Modifier.padding(start = 4.dp))
            DIFF_OPTIONS.forEach { opt ->
                SettingsRadioRow(opt = opt, selected = difficulty == opt.id, accentColor = WormGreen) { difficulty = opt.id }
            }

            // Control mode
            Text("🕹️ Steuerung", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextMuted, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
            CONTROL_OPTIONS.forEach { opt ->
                SettingsRadioRow(opt = opt, selected = controlMode == opt.id, accentColor = WormGreen) { controlMode = opt.id }
            }

            // Musik & Soundeffekte
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceDark)
                    .border(1.5.dp, BorderColor, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "💡 Musik & Soundeffekte findest du in Profil & Abmelden.",
                    fontSize = 13.sp, color = TextMuted,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "Öffnen →",
                    fontSize = 13.sp, color = WormGreen, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(onClick = onNavigateToProfile),
                )
            }

            if (saved) {
                Surface(
                    color = Color(0xFF22C55E).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "✓ Einstellungen gespeichert",
                        color = Color(0xFF22C55E),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(12.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsRadioRow(opt: OptionItem, selected: Boolean, accentColor: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) accentColor.copy(alpha = 0.12f) else SurfaceDark)
            .border(1.5.dp, if (selected) accentColor else BorderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(18.dp).clip(RoundedCornerShape(50))
                .background(if (selected) accentColor else Color.Transparent)
                .border(2.dp, if (selected) accentColor else TextMuted, RoundedCornerShape(50)),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(Color.White))
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(opt.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(opt.desc, fontSize = 12.sp, color = TextMuted)
        }
    }
}
