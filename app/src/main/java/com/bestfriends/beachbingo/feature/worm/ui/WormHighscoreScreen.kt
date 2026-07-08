package com.bestfriends.beachbingo.feature.worm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bestfriends.beachbingo.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

private val WormGreen = Color(0xFF22C55E)
private val SandGoldHs = Color(0xFFFBBF24)

private data class HsDiff(val id: String, val emoji: String, val label: String, val sub: String)
private val HS_DIFFICULTIES = listOf(
    HsDiff("ROOKIE",     "🌊", "Rookie",     "Langsam · Wände töten"),
    HsDiff("SNIPER",     "🎯", "Sniper",     "Mittel · Wände töten"),
    HsDiff("BOSS_LEVEL", "💪", "Boss Level", "Schnell · Wände = Teleport"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WormHighscoreScreen(onNavigateBack: () -> Unit) {
    val auth      = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val uid       = auth.currentUser?.uid

    var highScores by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    var loading    by remember { mutableStateOf(true) }

    LaunchedEffect(uid) {
        if (uid == null) { loading = false; return@LaunchedEffect }
        val snap = firestore.collection("users").document(uid).get().await()
        @Suppress("UNCHECKED_CAST")
        highScores = (snap.get("wormHighScores") as? Map<String, Long>) ?: emptyMap()
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("WATTWURM", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("🏆 Rekorde", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.ExtraBold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück", tint = TextPrimary)
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
                    .background(Brush.linearGradient(listOf(WormGreen.copy(alpha = 0.15f), Color(0xFF0a1628))))
                    .padding(horizontal = 20.dp, vertical = 28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🏆", fontSize = 64.sp)
                    Text("Deine Bestleistungen", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = SandGoldHs)
                    Text("Wattwurm – Alle Schwierigkeitsstufen", fontSize = 13.sp, color = TextMuted)
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (loading) {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = WormGreen)
                    }
                } else {
                    HS_DIFFICULTIES.forEach { diff ->
                        val score    = highScores[diff.id]
                        val hasScore = score != null
                        Surface(
                            color = if (hasScore) WormGreen.copy(alpha = 0.08f) else SurfaceDark,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (hasScore) 2.dp else 1.dp,
                                    color = if (hasScore) WormGreen.copy(alpha = 0.5f) else BorderColor,
                                    shape = RoundedCornerShape(16.dp),
                                ),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                ) {
                                    Text(diff.emoji, fontSize = 36.sp)
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(diff.label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (hasScore) TextPrimary else TextMuted)
                                        Text(if (hasScore) "Persönlicher Rekord" else diff.sub, fontSize = 12.sp, color = TextMuted)
                                    }
                                }
                                if (hasScore) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("$score", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = SandGoldHs)
                                        Text("Punkte", fontSize = 11.sp, color = TextMuted)
                                    }
                                } else {
                                    Text("–", fontSize = 28.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
