package com.bestfriends.beachbingo.feature.strandraeuber.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bestfriends.beachbingo.ui.theme.BgDark
import com.bestfriends.beachbingo.ui.theme.BorderColor
import com.bestfriends.beachbingo.ui.theme.Success
import com.bestfriends.beachbingo.ui.theme.SurfaceDark
import com.bestfriends.beachbingo.ui.theme.TextMuted
import com.bestfriends.beachbingo.ui.theme.TextPrimary
import com.bestfriends.beachbingo.ui.theme.TextSub
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private val SpCrimson = androidx.compose.ui.graphics.Color(0xFFE11D48)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrandraeuberSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid
    val scope = rememberCoroutineScope()

    var preferredDifficulty by remember { mutableStateOf("SNIPER") }
    var preferredRounds by remember { mutableStateOf(3) }
    var saving by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        if (uid == null) return@LaunchedEffect
        try {
            val snap = db.collection("users").document(uid).get().await()
            preferredDifficulty = snap.getString("preferredStrandraeuberDifficulty") ?: "SNIPER"
            preferredRounds = (snap.getLong("preferredStrandraeuberRounds") ?: 3L).toInt()
        } catch (_: Exception) {}
    }

    fun doSave() {
        if (uid == null) return
        scope.launch {
            saving = true
            try {
                db.collection("users").document(uid).update(
                    mapOf(
                        "preferredStrandraeuberDifficulty" to preferredDifficulty,
                        "preferredStrandraeuberRounds" to preferredRounds.toLong(),
                    )
                ).await()
                saved = true
                kotlinx.coroutines.delay(2500)
                saved = false
            } catch (_: Exception) {
            } finally {
                saving = false
            }
        }
    }

    Scaffold(
        containerColor = BgDark,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("STRANDRÄUBER", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("Einstellungen", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück", tint = TextPrimary)
                    }
                },
                actions = {
                    TextButton(onClick = { doSave() }, enabled = !saving) {
                        if (saving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = SpCrimson, strokeWidth = 2.dp)
                        } else {
                            Text(if (saved) "✓ Gespeichert" else "Speichern", color = SpCrimson)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Preferred difficulty
            Text(
                "Standard-KI-Schwierigkeit",
                style = MaterialTheme.typography.labelLarge,
                color = TextSub,
                modifier = Modifier.padding(start = 4.dp),
            )
            listOf(
                Triple("ROOKIE", "Rookie", "😅"),
                Triple("SNIPER", "Sniper", "🎯"),
                Triple("BOSS_LEVEL", "Boss Level", "💀"),
            ).forEach { (id, label, emoji) ->
                val selected = preferredDifficulty == id
                Surface(
                    color = if (selected) SpCrimson.copy(alpha = 0.15f) else SurfaceDark,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.5.dp,
                            if (selected) SpCrimson else BorderColor,
                            RoundedCornerShape(10.dp),
                        )
                        .clickable { preferredDifficulty = id },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(emoji, style = MaterialTheme.typography.titleMedium)
                        Text(
                            label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                        )
                        if (selected) Text("✓", color = SpCrimson, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Preferred rounds
            Text(
                "Standard-Rundenanzahl",
                style = MaterialTheme.typography.labelLarge,
                color = TextSub,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 3, 5).forEach { n ->
                    val selected = preferredRounds == n
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                1.5.dp,
                                if (selected) SpCrimson else BorderColor,
                                RoundedCornerShape(8.dp),
                            )
                            .clickable { preferredRounds = n },
                        color = if (selected) SpCrimson.copy(alpha = 0.15f) else SurfaceDark,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        androidx.compose.foundation.layout.Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 14.dp),
                        ) {
                            Text(
                                "$n Runde${if (n > 1) "n" else ""}",
                                fontWeight = FontWeight.Bold,
                                color = if (selected) SpCrimson else TextPrimary,
                            )
                        }
                    }
                }
            }

            // Audio hint
            Surface(
                color = SurfaceDark,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp)),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "💡 Musik & Soundeffekte findest du in Profil & Abmelden.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Öffnen →",
                        style = MaterialTheme.typography.bodySmall,
                        color = SpCrimson,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onNavigateToProfile() },
                    )
                }
            }

            if (saved) {
                Surface(
                    color = Success.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "✓ Einstellungen gespeichert",
                        color = Success,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }

            Spacer(Modifier.size(8.dp))
        }
    }
}
