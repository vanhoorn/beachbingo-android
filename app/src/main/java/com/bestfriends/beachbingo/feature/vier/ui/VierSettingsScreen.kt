package com.bestfriends.beachbingo.feature.vier.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bestfriends.beachbingo.ui.components.GameSettingsScaffold
import com.bestfriends.beachbingo.ui.components.SettingsGroupLabel
import com.bestfriends.beachbingo.ui.components.SettingsOptionCard
import com.bestfriends.beachbingo.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private data class DifficultyOption(val id: String, val label: String, val description: String, val emoji: String)

private val DIFFICULTIES = listOf(
    DifficultyOption("ROOKIE",     "Rookie",     "Macht häufig Fehler – gut zum Üben",   "😅"),
    DifficultyOption("SNIPER",     "Sniper",     "85% richtige Züge – fordert aber fair", "🎯"),
    DifficultyOption("BOSS_LEVEL", "Boss Level", "Fast unbesiegbar – alles oder nichts",  "💀"),
)

@Composable
fun VierSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
) {
    val auth      = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val uid       = auth.currentUser?.uid
    val scope     = rememberCoroutineScope()

    var selectedDrinkId    by remember { mutableStateOf("lager") }
    var selectedDifficulty by remember { mutableStateOf("SNIPER") }
    var saving             by remember { mutableStateOf(false) }
    var saved              by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        if (uid == null) return@LaunchedEffect
        val snap = firestore.collection("users").document(uid).get().await()
        selectedDrinkId    = snap.getString("preferredVierDrinkId")    ?: "lager"
        selectedDifficulty = snap.getString("preferredVierDifficulty") ?: "SNIPER"
    }

    fun doSave() {
        if (uid == null) return
        scope.launch {
            saving = true
            try {
                firestore.collection("users").document(uid).update(mapOf(
                    "preferredVierDrinkId"    to selectedDrinkId,
                    "preferredVierDifficulty" to selectedDifficulty,
                )).await()
                saved = true
                delay(2500)
                saved = false
            } catch (_: Exception) {
            } finally {
                saving = false
            }
        }
    }

    GameSettingsScaffold(
        gameLabel = "VIER4BIER",
        accentColor = BeerOrange,
        saving = saving,
        saved = saved,
        onBack = onNavigateBack,
        onSave = ::doSave,
        onNavigateToProfile = onNavigateToProfile,
    ) {
        SettingsGroupLabel("KI-Schwierigkeit")
        DIFFICULTIES.forEach { diff ->
            SettingsOptionCard(
                selected = selectedDifficulty == diff.id,
                accentColor = BeerOrange,
                onClick = { selectedDifficulty = diff.id },
            ) {
                Text(diff.emoji, style = MaterialTheme.typography.headlineSmall)
                Column(modifier = Modifier.weight(1f)) {
                    Text(diff.label,       style = MaterialTheme.typography.titleSmall,  color = TextPrimary)
                    Text(diff.description, style = MaterialTheme.typography.bodySmall,   color = TextMuted)
                }
                if (selectedDifficulty == diff.id) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = BeerOrange)
                }
            }
        }

        SettingsGroupLabel("Lieblingsgetränk")
        Text(
            "Wird in der Lobby vorausgewählt.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            DRINKS.chunked(3).forEach { rowDrinks ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowDrinks.forEach { drink ->
                        val selected = selectedDrinkId == drink.id
                        Surface(
                            onClick = { selectedDrinkId = drink.id },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) drink.color.copy(alpha = 0.2f) else SurfaceDark,
                            modifier = Modifier.weight(1f),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) drink.color else BorderColor,
                            ),
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                DrinkPiece(drinkId = drink.id, size = 44.dp)
                                Text(drink.name, style = MaterialTheme.typography.labelSmall, color = TextSub)
                            }
                        }
                    }
                    repeat(3 - rowDrinks.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}
