package com.bestfriends.beachbingo.feature.vier.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bestfriends.beachbingo.ui.theme.BgDark
import com.bestfriends.beachbingo.ui.theme.Coral
import com.bestfriends.beachbingo.ui.theme.Success
import com.bestfriends.beachbingo.ui.theme.SurfaceDark
import com.bestfriends.beachbingo.ui.theme.TextMuted
import com.bestfriends.beachbingo.ui.theme.TextPrimary
import com.bestfriends.beachbingo.ui.theme.TextSub
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private data class DifficultyOption(val id: String, val label: String, val description: String, val emoji: String)

private val DIFFICULTIES = listOf(
    DifficultyOption("ROOKIE",     "Rookie",     "Macht häufig Fehler – gut zum Üben",   "😅"),
    DifficultyOption("SNIPER",     "Sniper",     "85% richtige Züge – fordert aber fair", "🎯"),
    DifficultyOption("BOSS_LEVEL", "Boss Level", "Fast unbesiegbar – alles oder nichts",  "💀"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VierSettingsScreen(
    onNavigateBack: () -> Unit,
) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid

    var selectedDrinkId by remember { mutableStateOf("lager") }
    var selectedDifficulty by remember { mutableStateOf("SNIPER") }
    var saving by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uid) {
        if (uid == null) return@LaunchedEffect
        val snap = firestore.collection("users").document(uid).get().await()
        selectedDrinkId = snap.getString("preferredVierDrinkId") ?: "lager"
        selectedDifficulty = snap.getString("preferredVierDifficulty") ?: "SNIPER"
    }

    fun doSave() {
        if (uid == null) return
        scope.launch {
            saving = true
            firestore.collection("users").document(uid)
                .update(mapOf(
                    "preferredVierDrinkId" to selectedDrinkId,
                    "preferredVierDifficulty" to selectedDifficulty,
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
                title = { Text("Vier4Bier Einstellungen", color = TextPrimary) },
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
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Coral, strokeWidth = 2.dp)
                        } else {
                            Text(if (saved) "✓" else "Speichern", color = Coral)
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
            Text(
                text = "KI-Schwierigkeit",
                style = MaterialTheme.typography.labelLarge,
                color = TextSub,
                modifier = Modifier.padding(start = 4.dp),
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DIFFICULTIES.forEach { diff ->
                    val selected = selectedDifficulty == diff.id
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = 2.dp,
                                color = if (selected) Color(0xFFC2410C) else Color(0xFF1E3050),
                                shape = RoundedCornerShape(8.dp),
                            )
                            .clickable { selectedDifficulty = diff.id },
                        color = if (selected) Color(0xFFC2410C).copy(alpha = 0.15f) else SurfaceDark,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Text(text = diff.emoji, style = MaterialTheme.typography.headlineSmall)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = diff.label, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text(text = diff.description, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                            }
                            if (selected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFFC2410C))
                            }
                        }
                    }
                }
            }

            // ── Drink ──
            Text(
                text = "Lieblingsgetränk",
                style = MaterialTheme.typography.labelLarge,
                color = TextSub,
                modifier = Modifier.padding(start = 4.dp),
            )
            Text(
                text = "Wird in der Lobby vorausgewählt.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DRINKS.chunked(3).forEach { rowDrinks ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        rowDrinks.forEach { drink ->
                            val selected = selectedDrinkId == drink.id
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = 2.dp,
                                        color = if (selected) drink.color else Color(0xFF1E3050),
                                        shape = RoundedCornerShape(8.dp),
                                    )
                                    .clickable { selectedDrinkId = drink.id },
                                color = if (selected) drink.color.copy(alpha = 0.2f) else SurfaceDark,
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    DrinkPiece(drinkId = drink.id, size = 44.dp)
                                    Text(
                                        text = drink.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSub,
                                    )
                                }
                            }
                        }
                        // fill empty cells if last row has < 3 items
                        repeat(3 - rowDrinks.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            if (saved) {
                Surface(
                    color = Success.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "✓ Einstellungen gespeichert",
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
