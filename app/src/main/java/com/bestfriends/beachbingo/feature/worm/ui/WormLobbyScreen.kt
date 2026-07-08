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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

private val WormGreen     = Color(0xFF22C55E)
private val WormGreenDark = Color(0xFF15803D)

private data class DiffItem(val id: String, val emoji: String, val label: String, val desc: String)
private val DIFF_OPTIONS = listOf(
    DiffItem("ROOKIE",     "🌊", "Rookie",     "Langsam · Wände töten · Ideal zum Starten"),
    DiffItem("SNIPER",     "🎯", "Sniper",     "Schneller · Wände töten · Echte Herausforderung"),
    DiffItem("BOSS_LEVEL", "💪", "Boss Level", "Sehr schnell · Wände = Teleport · Viel Spaß 😈"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WormLobbyScreen(
    onNavigateToGame: (difficulty: String, controlMode: String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHighscore: () -> Unit,
    onNavigateToHome: () -> Unit,
) {
    val auth      = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val uid       = auth.currentUser?.uid

    var difficulty   by remember { mutableStateOf("ROOKIE") }
    var controlMode  by remember { mutableStateOf("BUTTONS") }
    var loading      by remember { mutableStateOf(true) }
    var isFavorite   by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        if (uid == null) { loading = false; return@LaunchedEffect }
        val snap = firestore.collection("users").document(uid).get().await()
        difficulty  = snap.getString("preferredWormDifficulty") ?: "ROOKIE"
        controlMode = snap.getString("preferredWormControlMode") ?: "BUTTONS"
        @Suppress("UNCHECKED_CAST")
        isFavorite = (snap.get("favoriteGames") as? List<String>)?.contains("worm") == true
        loading = false
    }

    fun toggleFavorite() {
        isFavorite = !isFavorite
        val update = if (isFavorite) FieldValue.arrayUnion("worm") else FieldValue.arrayRemove("worm")
        if (uid != null) firestore.collection("users").document(uid).update("favoriteGames", update)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("WATTWURM", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("🪱 Lobby", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.ExtraBold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateToHome) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToHighscore) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = "Rekorde", tint = SandGold)
                    }
                    IconButton(onClick = { toggleFavorite() }) {
                        Text(
                            if (isFavorite) "★" else "☆",
                            fontSize = 22.sp,
                            color = if (isFavorite) SandGold else TextMuted,
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Einstellungen", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark),
            )
        },
        containerColor = BgDark,
    ) { padding ->
        if (loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("🪱", fontSize = 48.sp)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Hero
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(WormGreen.copy(alpha = 0.15f), WormGreen.copy(alpha = 0.05f))))
                    .border(1.dp, WormGreen.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🪱", fontSize = 56.sp)
                    Text("Wattenfresser unterwegs!", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                    Text(
                        "Frisst Krabben 🦀, Muscheln 🐚 und Fische 🐟.\nWerde länger — verliere dich nicht!",
                        fontSize = 13.sp, color = TextMuted, lineHeight = 20.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }

            // Difficulty selection
            Text("Schwierigkeit", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextMuted, modifier = Modifier.padding(start = 4.dp))
            DIFF_OPTIONS.forEach { diff ->
                val selected = difficulty == diff.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) WormGreen.copy(alpha = 0.12f) else SurfaceDark)
                        .border(1.5.dp, if (selected) WormGreen else BorderColor, RoundedCornerShape(14.dp))
                        .clickable { difficulty = diff.id }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier.size(18.dp).clip(RoundedCornerShape(50))
                            .background(if (selected) WormGreen else Color.Transparent)
                            .border(2.dp, if (selected) WormGreen else TextMuted, RoundedCornerShape(50)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(Color.White))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("${diff.emoji} ${diff.label}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text(diff.desc, fontSize = 12.sp, color = TextMuted)
                    }
                }
            }

            // Control mode hint
            Text(
                "Steuerung: ${if (controlMode == "BUTTONS") "🔲 Buttons" else "👆 Swipe"} · In Einstellungen ändern",
                fontSize = 12.sp, color = TextMuted,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            // Play button
            Button(
                onClick = { onNavigateToGame(difficulty, controlMode) },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WormGreen),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("🎮 Spielen", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
