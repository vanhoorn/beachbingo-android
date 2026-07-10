package com.bestfriends.beachbingo.feature.strandturm.ui

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

private val StrandturmRed = Color(0xFFDC2626)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrandturmHighscoreScreen(onNavigateBack: () -> Unit) {
    val auth      = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val uid       = auth.currentUser?.uid

    var highScore  by remember { mutableIntStateOf(0) }
    var bestLevel  by remember { mutableIntStateOf(0) }
    var loading    by remember { mutableStateOf(true) }

    LaunchedEffect(uid) {
        if (uid == null) { loading = false; return@LaunchedEffect }
        val snap = firestore.collection("users").document(uid).get().await()
        highScore = (snap.getLong("strandturmHighScore") ?: 0L).toInt()
        bestLevel = (snap.getLong("strandturmBestLevel") ?: 0L).toInt()
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("STRANDTURM", style = MaterialTheme.typography.labelSmall, color = TextMuted)
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(StrandturmRed.copy(alpha = 0.15f), Color(0xFF0a1628))))
                    .padding(horizontal = 20.dp, vertical = 28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🏆", fontSize = 64.sp)
                    Text("Deine Bestleistungen", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = SandGold)
                    Text("Strandturm – Küsten-Klara", fontSize = 13.sp, color = TextMuted)
                }
            }

            if (loading) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = StrandturmRed)
                }
            } else {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // High score card
                    ScoreCard(
                        emoji = "🏆",
                        label = "Highscore",
                        sub = "Höchste erreichte Punktzahl",
                        value = if (highScore > 0) "$highScore Pts" else null,
                        accentColor = SandGold,
                    )
                    // Best level card
                    ScoreCard(
                        emoji = "🎯",
                        label = "Höchstes Level",
                        sub = "Weitester Aufstieg",
                        value = if (bestLevel > 0) "Level $bestLevel" else null,
                        accentColor = OceanBlue,
                    )

                    if (highScore == 0 && bestLevel == 0) {
                        Surface(color = SurfaceDark, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text("🗼", fontSize = 40.sp)
                                Text("Noch kein Spiel gespielt", fontSize = 14.sp, color = TextMuted)
                                Text("Klettere den Pier hoch und lass dir von Mega-Möwe kein Kokosnuss den Weg versperren!", fontSize = 12.sp, color = TextSub)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreCard(emoji: String, label: String, sub: String, value: String?, accentColor: Color) {
    val hasValue = value != null
    Surface(
        color = if (hasValue) accentColor.copy(alpha = 0.08f) else SurfaceDark,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (hasValue) 2.dp else 1.dp,
                color = if (hasValue) accentColor.copy(alpha = 0.5f) else BorderColor,
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
                Text(emoji, fontSize = 36.sp)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (hasValue) TextPrimary else TextMuted)
                    Text(if (hasValue) "Persönlicher Rekord" else sub, fontSize = 12.sp, color = TextMuted)
                }
            }
            if (hasValue) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(value!!, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = accentColor)
                }
            } else {
                Text("–", fontSize = 28.sp, color = TextMuted, fontWeight = FontWeight.Bold)
            }
        }
    }
}
