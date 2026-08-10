package com.bestfriends.beachbingo.feature.worm.ui

import androidx.compose.runtime.*
import com.bestfriends.beachbingo.ui.components.GameSettingsScaffold
import com.bestfriends.beachbingo.ui.components.SettingsGroupLabel
import com.bestfriends.beachbingo.ui.components.SettingsRadioRow
import com.bestfriends.beachbingo.ui.theme.Success
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private data class OptionItem(val id: String, val title: String, val desc: String)

private val DIFF_OPTIONS = listOf(
    OptionItem("ROOKIE",     "🌊 Rookie",     "Gemütliches Tempo · Wände töten · Ideal zum Starten"),
    OptionItem("SNIPER",     "🎯 Sniper",     "Flottes Tempo · Wände töten · Echte Herausforderung"),
    OptionItem("BOSS_LEVEL", "💪 Boss Level", "Volles Tempo · Wände töten · Viel Spaß 😈"),
)

private val CONTROL_OPTIONS = listOf(
    OptionItem("BUTTONS", "🔲 Buttons", "Vier Pfeil-Buttons unter dem Spielfeld"),
    OptionItem("SWIPE",   "👆 Swipe",   "Auf dem Spielfeld wischen zum Lenken"),
)

@Composable
fun WormSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
) {
    val auth      = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val uid       = auth.currentUser?.uid
    val scope     = rememberCoroutineScope()

    var difficulty  by remember { mutableStateOf("ROOKIE") }
    var controlMode by remember { mutableStateOf("BUTTONS") }
    var saving      by remember { mutableStateOf(false) }
    var saved       by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        if (uid == null) return@LaunchedEffect
        val snap = firestore.collection("users").document(uid).get().await()
        difficulty  = snap.getString("preferredWormDifficulty")  ?: "ROOKIE"
        controlMode = snap.getString("preferredWormControlMode") ?: "BUTTONS"
    }

    fun doSave() {
        if (uid == null) return
        saving = true
        scope.launch {
            try {
                firestore.collection("users").document(uid).update(
                    mapOf(
                        "preferredWormDifficulty"  to difficulty,
                        "preferredWormControlMode" to controlMode,
                    )
                ).await()
            } catch (_: Exception) {}
            saving = false
            saved  = true
        }
    }

    GameSettingsScaffold(
        gameLabel = "WATTWURM",
        accentColor = Success,
        saving = saving,
        saved = saved,
        onBack = onNavigateBack,
        onSave = ::doSave,
        onNavigateToProfile = onNavigateToProfile,
    ) {
        SettingsGroupLabel("Schwierigkeit")
        DIFF_OPTIONS.forEach { opt ->
            SettingsRadioRow(
                title = opt.title,
                desc = opt.desc,
                selected = difficulty == opt.id,
                accentColor = Success,
                onClick = { difficulty = opt.id },
            )
        }
        SettingsGroupLabel("🕹️ Steuerung")
        CONTROL_OPTIONS.forEach { opt ->
            SettingsRadioRow(
                title = opt.title,
                desc = opt.desc,
                selected = controlMode == opt.id,
                accentColor = Success,
                onClick = { controlMode = opt.id },
            )
        }
    }
}
