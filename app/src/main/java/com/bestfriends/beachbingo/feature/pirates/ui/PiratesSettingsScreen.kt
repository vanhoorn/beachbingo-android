package com.bestfriends.beachbingo.feature.pirates.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bestfriends.beachbingo.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private data class DiffOption(val id: String, val emoji: String, val label: String, val desc: String)

private val DIFFICULTIES = listOf(
    DiffOption("ROOKIE",     "🌊", "Rookie",     "Langsam & entspannt – gut zum Starten"),
    DiffOption("SNIPER",     "🎯", "Sniper",     "Mittleres Tempo – fordert Reaktion"),
    DiffOption("BOSS_LEVEL", "💪", "Boss Level", "Schnell & tückisch – alles oder nichts"),
)

private val FIRE_RATE_LABELS = mapOf(
    1 to "Sehr langsam", 2 to "Langsam", 3 to "Gemächlich", 4 to "Moderat",
    5 to "Normal", 6 to "Schnell", 7 to "Zügig", 8 to "Sehr schnell",
    9 to "Rasend", 10 to "Maximum",
)

private val Purple = Color(0xFFA855F7)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PiratesSettingsScreen(onNavigateBack: () -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid

    var selectedDifficulty by remember { mutableStateOf("SNIPER") }
    var fireRate by remember { mutableIntStateOf(5) }
    var controlMode by remember { mutableStateOf("BUTTONS") }
    var saving by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uid) {
        if (uid == null) return@LaunchedEffect
        val snap = firestore.collection("users").document(uid).get().await()
        selectedDifficulty = snap.getString("preferredPiratesDifficulty") ?: "SNIPER"
        fireRate = (snap.getLong("preferredPiratesFireRate") ?: 5L).toInt()
        controlMode = snap.getString("preferredPiratesControlMode") ?: "BUTTONS"
    }

    fun doSave() {
        if (uid == null) return
        scope.launch {
            saving = true
            firestore.collection("users").document(uid).update(mapOf(
                "preferredPiratesDifficulty" to selectedDifficulty,
                "preferredPiratesFireRate" to fireRate,
                "preferredPiratesControlMode" to controlMode,
            )).await()
            saving = false
            saved = true
            kotlinx.coroutines.delay(2500)
            saved = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BeachPirates Einstellungen", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück", tint = TextPrimary)
                    }
                },
                actions = {
                    TextButton(
                        onClick = { doSave() },
                        enabled = !saving,
                    ) {
                        if (saving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Purple, strokeWidth = 2.dp)
                        } else {
                            Text(if (saved) "✓" else "Speichern", color = Purple)
                        }
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
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            // ── Difficulty ──
            Text("Schwierigkeit", style = MaterialTheme.typography.labelLarge, color = TextSub,
                modifier = Modifier.padding(start = 4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DIFFICULTIES.forEach { diff ->
                    val selected = selectedDifficulty == diff.id
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(2.dp, if (selected) Purple else BorderColor, RoundedCornerShape(8.dp))
                            .clickable { selectedDifficulty = diff.id },
                        color = if (selected) Purple.copy(alpha = 0.15f) else SurfaceDark,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Text(diff.emoji, style = MaterialTheme.typography.headlineSmall)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(diff.label, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text(diff.desc, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                            }
                            if (selected) Icon(Icons.Default.Check, null, tint = Purple)
                        }
                    }
                }
            }

            // ── Fire Rate ──
            Text("Schussrate Oktopus", style = MaterialTheme.typography.labelLarge, color = TextSub,
                modifier = Modifier.padding(start = 4.dp))
            Surface(color = SurfaceDark, shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(12.dp))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Rate: $fireRate / 10", color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                        Text(FIRE_RATE_LABELS[fireRate] ?: "", color = Purple, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(8.dp))
                    Slider(
                        value = fireRate.toFloat(),
                        onValueChange = { fireRate = it.toInt() },
                        valueRange = 1f..10f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = Purple,
                            activeTrackColor = Purple,
                            inactiveTrackColor = Surface2Dark,
                        ),
                    )
                }
            }

            // ── Control Mode ──
            Text("Steuerung", style = MaterialTheme.typography.labelLarge, color = TextSub,
                modifier = Modifier.padding(start = 4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("BUTTONS" to "◀▶ Buttons", "TOUCH" to "Touch-Flächen").forEach { (id, label) ->
                    val selected = controlMode == id
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .border(2.dp, if (selected) Purple else BorderColor, RoundedCornerShape(8.dp))
                            .clickable { controlMode = id },
                        color = if (selected) Purple.copy(alpha = 0.15f) else SurfaceDark,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                            Text(label, color = if (selected) Purple else TextSub,
                                style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            if (saved) {
                Surface(color = Success.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()) {
                    Text("✓ Einstellungen gespeichert", color = Success,
                        style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(10.dp))
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
