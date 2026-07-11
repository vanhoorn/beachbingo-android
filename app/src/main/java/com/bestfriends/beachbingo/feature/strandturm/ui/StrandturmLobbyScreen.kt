package com.bestfriends.beachbingo.feature.strandturm.ui

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bestfriends.beachbingo.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

private val StrandturmRed = Color(0xFFDC2626)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrandturmLobbyScreen(
    onNavigateToGame: (controlMode: String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHighscore: () -> Unit,
    onNavigateToHome: () -> Unit,
) {
    val auth      = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val uid       = auth.currentUser?.uid

    var controlMode  by remember { mutableStateOf("BUTTONS") }
    var highScore    by remember { mutableIntStateOf(0) }
    var bestLevel    by remember { mutableIntStateOf(0) }
    var isFavorite   by remember { mutableStateOf(false) }
    var loading      by remember { mutableStateOf(true) }

    LaunchedEffect(uid) {
        if (uid == null) { loading = false; return@LaunchedEffect }
        val snap = firestore.collection("users").document(uid).get().await()
        controlMode = snap.getString("preferredStrandturmControlMode") ?: "BUTTONS"
        highScore   = (snap.getLong("strandturmHighScore") ?: 0L).toInt()
        bestLevel   = (snap.getLong("strandturmBestLevel") ?: 0L).toInt()
        @Suppress("UNCHECKED_CAST")
        isFavorite = (snap.get("favoriteGames") as? List<String>)?.contains("strandturm") == true
        loading = false
    }

    fun toggleFavorite() {
        isFavorite = !isFavorite
        val update = if (isFavorite) FieldValue.arrayUnion("strandturm") else FieldValue.arrayRemove("strandturm")
        if (uid != null) firestore.collection("users").document(uid).update("favoriteGames", update)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("STRANDTURM", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("🗼 Lobby", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.ExtraBold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateToHome) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToHighscore) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = "Rekord", tint = SandGold)
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
                Text("🗼", fontSize = 48.sp)
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
                    .background(Brush.linearGradient(listOf(StrandturmRed.copy(alpha = 0.15f), StrandturmRed.copy(alpha = 0.04f))))
                    .border(1.dp, StrandturmRed.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🗼", fontSize = 56.sp)
                    Text("Hol Euch den Rettungsring!", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                    Text(
                        "Böser Seelöwe 🦭 wirft Kokosnüsse 🥥 vom Pier.\nKlettere nach oben — ohne getroffen zu werden!",
                        fontSize = 13.sp, color = TextMuted, lineHeight = 20.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // Stats (only if score > 0)
            if (highScore > 0 || bestLevel > 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(label = "🏆 Rekord", value = "$highScore Pts", modifier = Modifier.weight(1f), color = StrandturmRed)
                    StatCard(label = "🎯 Höchstes Level", value = "Lv. $bestLevel", modifier = Modifier.weight(1f), color = OceanBlue)
                }
            }

            // How to play
            Surface(color = SurfaceDark, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("So geht's", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("🪜 Leitern hochklettern · 🥥 Kokosnüssen ausweichen", fontSize = 13.sp, color = TextMuted)
                    Text("🔨 Hammer finden · Kokosnüsse zerschmettern!", fontSize = 13.sp, color = TextMuted)
                    Text("❤️ 3 Leben · ⏱ Bonuszeit läuft ab", fontSize = 13.sp, color = TextMuted)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Steuerung: ${if (controlMode == "BUTTONS") "🔲 Buttons" else "👆 Touch"}",
                            fontSize = 12.sp, color = TextSub,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Ändern",
                            fontSize = 12.sp, color = StrandturmRed,
                            modifier = Modifier.clickable { onNavigateToSettings() }
                        )
                    }
                }
            }

            // Play button
            Button(
                onClick = { onNavigateToGame(controlMode) },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StrandturmRed),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("🎮 Spielen", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier, color: Color) {
    Surface(color = SurfaceDark, shape = RoundedCornerShape(14.dp), modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(label, fontSize = 12.sp, color = TextMuted)
        }
    }
}
