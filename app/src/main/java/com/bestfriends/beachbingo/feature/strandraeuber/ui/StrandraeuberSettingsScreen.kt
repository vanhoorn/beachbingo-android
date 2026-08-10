package com.bestfriends.beachbingo.feature.strandraeuber.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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

@Composable
fun StrandraeuberSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
) {
    val auth  = FirebaseAuth.getInstance()
    val db    = FirebaseFirestore.getInstance()
    val uid   = auth.currentUser?.uid
    val scope = rememberCoroutineScope()

    var preferredDifficulty by remember { mutableStateOf("SNIPER") }
    var preferredRounds     by remember { mutableStateOf(3) }
    var saving              by remember { mutableStateOf(false) }
    var saved               by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        if (uid == null) return@LaunchedEffect
        try {
            val snap = db.collection("users").document(uid).get().await()
            preferredDifficulty = snap.getString("preferredStrandraeuberDifficulty") ?: "SNIPER"
            preferredRounds     = (snap.getLong("preferredStrandraeuberRounds") ?: 3L).toInt()
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
                        "preferredStrandraeuberRounds"     to preferredRounds.toLong(),
                    )
                ).await()
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
        gameLabel = "STRANDRÄUBER",
        accentColor = Crimson,
        saving = saving,
        saved = saved,
        onBack = onNavigateBack,
        onSave = ::doSave,
        onNavigateToProfile = onNavigateToProfile,
    ) {
        SettingsGroupLabel("Standard-KI-Schwierigkeit")
        listOf(
            Triple("ROOKIE",     "Rookie",     "😅"),
            Triple("SNIPER",     "Sniper",     "🎯"),
            Triple("BOSS_LEVEL", "Boss Level", "💀"),
        ).forEach { (id, label, emoji) ->
            SettingsOptionCard(
                selected = preferredDifficulty == id,
                accentColor = Crimson,
                onClick = { preferredDifficulty = id },
            ) {
                Text(emoji, style = MaterialTheme.typography.titleMedium)
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                if (preferredDifficulty == id) {
                    Text("✓", color = Crimson, fontWeight = FontWeight.Bold)
                }
            }
        }

        SettingsGroupLabel("Standard-Rundenanzahl")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1, 3, 5).forEach { n ->
                val selected = preferredRounds == n
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .border(1.5.dp, if (selected) Crimson else BorderColor, RoundedCornerShape(8.dp))
                        .clickable { preferredRounds = n },
                    color = if (selected) Crimson.copy(alpha = 0.15f) else SurfaceDark,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 14.dp)) {
                        Text(
                            "$n Runde${if (n > 1) "n" else ""}",
                            fontWeight = FontWeight.Bold,
                            color = if (selected) Crimson else TextPrimary,
                        )
                    }
                }
            }
        }
    }
}
