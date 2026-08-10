package com.bestfriends.beachbingo.feature.pirates.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bestfriends.beachbingo.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

private data class DiffItem(val id: String, val emoji: String, val label: String)
private val DIFFICULTIES = listOf(
    DiffItem("ROOKIE",     "🌊", "Rookie"),
    DiffItem("SNIPER",     "🎯", "Sniper"),
    DiffItem("BOSS_LEVEL", "💪", "Boss Level"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PiratesHighscoreScreen(onNavigateBack: () -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid

    var highScores by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(uid) {
        if (uid == null) { loading = false; return@LaunchedEffect }
        val snap = firestore.collection("users").document(uid).get().await()
        @Suppress("UNCHECKED_CAST")
        highScores = (snap.get("piratesHighScores") as? Map<String, Long>) ?: emptyMap()
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("BEACHPIRATES", style = MaterialTheme.typography.labelSmall, color = TextMuted)
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
                    .background(Brush.linearGradient(listOf(BgPirateDark, BgDark)))
                    .padding(horizontal = 20.dp, vertical = 28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🏆", fontSize = DrawNumberPhone)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Deine Bestleistungen",
                        fontSize = BingoCallSize,
                        fontWeight = FontWeight.ExtraBold,
                        color = SandGoldLight,
                    )
                    Text(
                        "BeachPirates – Alle Schwierigkeitsstufen",
                        fontSize = MaterialTheme.typography.labelMedium.fontSize,
                        color = TextMuted,
                    )
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (loading) {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PiratesPurple)
                    }
                } else {
                    DIFFICULTIES.forEach { diff ->
                        val score = highScores[diff.id]
                        val hasScore = score != null
                        Surface(
                            color = if (hasScore) PiratesPurple.copy(alpha = 0.08f) else SurfaceDark,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (hasScore) 2.dp else 1.dp,
                                    color = if (hasScore) PiratesPurple.copy(alpha = 0.5f) else BorderColor,
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
                                    Text(diff.emoji, fontSize = MaterialTheme.typography.headlineLarge.fontSize)
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            diff.label,
                                            fontSize = MaterialTheme.typography.titleSmall.fontSize,
                                            fontWeight = FontWeight.Bold,
                                            color = if (hasScore) TextPrimary else TextMuted,
                                        )
                                        Text(
                                            if (hasScore) "Persönlicher Rekord" else "Noch kein Spiel",
                                            fontSize = ChipLabel,
                                            color = TextMuted,
                                        )
                                    }
                                }
                                if (hasScore) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            "$score",
                                            fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = SandGoldLight,
                                        )
                                        Text(
                                            "Punkte",
                                            fontSize = MaterialTheme.typography.labelSmall.fontSize,
                                            color = TextMuted,
                                        )
                                    }
                                } else {
                                    Text("–", fontSize = MaterialTheme.typography.headlineMedium.fontSize, color = TextMuted, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
