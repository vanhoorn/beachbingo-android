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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bestfriends.beachbingo.ui.theme.*
import com.bestfriends.beachbingo.feature.raetsel.SoloGameSaveManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import com.bestfriends.beachbingo.core.model.ALL_GAME_RULES
import com.bestfriends.beachbingo.feature.home.ui.GameRulesBottomSheet
import com.bestfriends.beachbingo.feature.home.ui.SavedGameRow

private data class LobbyDiffItem(val id: String, val emoji: String, val label: String)
private val DIFF_OPTIONS = listOf(
    LobbyDiffItem("ROOKIE",     "🌊", "Rookie"),
    LobbyDiffItem("SNIPER",     "🎯", "Sniper"),
    LobbyDiffItem("BOSS_LEVEL", "💪", "Boss Level"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PiratesLobbyScreen(
    onNavigateToGame: (difficulty: String, fireRate: Int, controlMode: String, saveId: String?) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToResults: () -> Unit = {},
) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid
    val context = androidx.compose.ui.platform.LocalContext.current

    var difficulty by remember { mutableStateOf("ROOKIE") }
    var fireRate by remember { mutableIntStateOf(5) }
    var controlMode by remember { mutableStateOf("BUTTONS") }
    var loading by remember { mutableStateOf(true) }
    var isFavorite by remember { mutableStateOf(false) }
    var showRules by remember { mutableStateOf(false) }
    var savedGame by remember { mutableStateOf(SoloGameSaveManager.getGameSave(context, "pirates")) }

    LaunchedEffect(uid) {
        if (uid == null) { loading = false; return@LaunchedEffect }
        val snap = firestore.collection("users").document(uid).get().await()
        difficulty = snap.getString("preferredPiratesDifficulty") ?: "ROOKIE"
        fireRate = (snap.getLong("preferredPiratesFireRate") ?: 5L).toInt()
        controlMode = snap.getString("preferredPiratesControlMode") ?: "BUTTONS"
        @Suppress("UNCHECKED_CAST")
        isFavorite = (snap.get("favoriteGames") as? List<String>)?.contains("pirates") == true
        loading = false
    }

    fun toggleFavorite() {
        isFavorite = !isFavorite
        val update = if (isFavorite) FieldValue.arrayUnion("pirates") else FieldValue.arrayRemove("pirates")
        if (uid != null) firestore.collection("users").document(uid).update("favoriteGames", update)
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
                    IconButton(onClick = { toggleFavorite() }) {
                        Text(
                            if (isFavorite) "★" else "☆",
                            fontSize = MaterialTheme.typography.titleLarge.fontSize,
                            color = if (isFavorite) SandGold else TextMuted,
                        )
                    }
                    IconButton(onClick = { showRules = true }) {
                        Icon(Icons.AutoMirrored.Outlined.HelpOutline, contentDescription = "Spielanleitung", tint = TextSub)
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
                    .background(Brush.linearGradient(listOf(BgPirateDark, BgDark)))
                    .padding(horizontal = 20.dp, vertical = 28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🐙", fontSize = DrawNumberPhone)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "BeachPirates",
                        fontSize = TitleHero,
                        fontWeight = FontWeight.ExtraBold,
                        color = PiratesPurple,
                    )
                    Text(
                        "Verteidige den Strand!",
                        fontSize = CellNumber,
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
                            color = if (selected) PiratesPurple.copy(alpha = 0.2f) else SurfaceDark,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) PiratesPurple else BorderColor,
                                    shape = RoundedCornerShape(10.dp),
                                )
                                .clickable { difficulty = diff.id },
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(diff.emoji, fontSize = MaterialTheme.typography.titleLarge.fontSize)
                                Text(
                                    diff.label,
                                    fontSize = MaterialTheme.typography.labelSmall.fontSize,
                                    color = if (selected) PiratesPurple else TextSub,
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
                            Text("⚡", fontSize = BingoCallSize)
                            Text("Schussrate", fontSize = ChipLabelTiny, color = TextMuted)
                            Text("$fireRate / 10", fontSize = CellNumber, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (controlMode == "BUTTONS") "◀▶" else "👆", fontSize = BingoCallSize)
                            Text("Steuerung", fontSize = ChipLabelTiny, color = TextMuted)
                            Text(if (controlMode == "BUTTONS") "Buttons" else "Touch", fontSize = CellNumber,
                                color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Saved game card
                savedGame?.let { sg ->
                    SavedGameRow(
                        title = "BeachPirates",
                        subtitle = sg.displayLabel,
                        color = PiratesPurple,
                        onResume = { onNavigateToGame(sg.difficulty, fireRate, controlMode, sg.id) },
                        onDelete = { SoloGameSaveManager.deleteGameSave(context, "pirates"); savedGame = null },
                    )
                }

                // Play button
                Button(
                    onClick = { onNavigateToGame(difficulty, fireRate, controlMode, null) },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PiratesPurple),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("🎮 Spielen", fontSize = MaterialTheme.typography.titleMedium.fontSize, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (showRules) {
        ALL_GAME_RULES["pirates"]?.let { GameRulesBottomSheet(rule = it, onDismiss = { showRules = false }) }
    }
}
