package com.bestfriends.beachbingo.feature.pirates.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bestfriends.beachbingo.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

private val Purple = Color(0xFFA855F7)
private val PurpleDark = Color(0xFF7C3AED)

private data class LobbyDiffItem(val id: String, val emoji: String, val label: String)
private val DIFF_OPTIONS = listOf(
    LobbyDiffItem("ROOKIE",     "🌊", "Rookie"),
    LobbyDiffItem("SNIPER",     "🎯", "Sniper"),
    LobbyDiffItem("BOSS_LEVEL", "💪", "Boss Level"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PiratesLobbyScreen(
    onNavigateToGame: (difficulty: String, fireRate: Int, controlMode: String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToResults: () -> Unit = {},
) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid

    var difficulty by remember { mutableStateOf("SNIPER") }
    var fireRate by remember { mutableIntStateOf(5) }
    var controlMode by remember { mutableStateOf("BUTTONS") }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(uid) {
        if (uid == null) { loading = false; return@LaunchedEffect }
        val snap = firestore.collection("users").document(uid).get().await()
        difficulty = snap.getString("preferredPiratesDifficulty") ?: "SNIPER"
        fireRate = (snap.getLong("preferredPiratesFireRate") ?: 5L).toInt()
        controlMode = snap.getString("preferredPiratesControlMode") ?: "BUTTONS"
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("BEACHPIRATES", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("🐙 Lobby", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.ExtraBold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateToHome) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToResults) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = "Ergebnisse", tint = SandGold)
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Einstellungen", tint = TextMuted)
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
                .verticalScroll(rememberScrollState()),
        ) {
            // Hero banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Color(0xFF1a0a2e), Color(0xFF0a1628))))
                    .padding(horizontal = 20.dp, vertical = 28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🐙", fontSize = 64.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "BeachPirates",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Purple,
                    )
                    Text(
                        "Verteidige den Strand!",
                        fontSize = 14.sp,
                        color = TextMuted,
                    )
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {

                // Difficulty selection
                Text("Schwierigkeit", style = MaterialTheme.typography.labelLarge, color = TextSub,
                    modifier = Modifier.padding(start = 4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DIFF_OPTIONS.forEach { diff ->
                        val selected = difficulty == diff.id
                        Surface(
                            color = if (selected) Purple.copy(alpha = 0.2f) else SurfaceDark,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) Purple else BorderColor,
                                    shape = RoundedCornerShape(10.dp),
                                )
                                .clickable { difficulty = diff.id },
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(diff.emoji, fontSize = 22.sp)
                                Text(
                                    diff.label,
                                    fontSize = 11.sp,
                                    color = if (selected) Purple else TextSub,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                )
                            }
                        }
                    }
                }

                // Info row
                Surface(color = SurfaceDark, shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(12.dp))) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("⚡", fontSize = 20.sp)
                            Text("Schussrate", fontSize = 10.sp, color = TextMuted)
                            Text("$fireRate / 10", fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (controlMode == "BUTTONS") "◀▶" else "👆", fontSize = 20.sp)
                            Text("Steuerung", fontSize = 10.sp, color = TextMuted)
                            Text(if (controlMode == "BUTTONS") "Buttons" else "Touch", fontSize = 14.sp,
                                color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Play button
                Button(
                    onClick = { onNavigateToGame(difficulty, fireRate, controlMode) },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Purple),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("🏴‍☠️  Spiel starten", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
