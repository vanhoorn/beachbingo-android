package com.bestfriends.beachbingo.feature.meermau.ui

import androidx.compose.runtime.*
import com.bestfriends.beachbingo.ui.components.GameSettingsScaffold
import com.bestfriends.beachbingo.ui.components.SettingsGroupLabel
import com.bestfriends.beachbingo.ui.components.SettingsSwitchRow
import com.bestfriends.beachbingo.ui.theme.PurpleDeep
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun MeermauSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
) {
    val auth  = FirebaseAuth.getInstance()
    val db    = FirebaseFirestore.getInstance()
    val uid   = auth.currentUser?.uid
    val scope = rememberCoroutineScope()

    var reverseOn9  by remember { mutableStateOf(false) }
    var stopperOn8  by remember { mutableStateOf(false) }
    var wildOn10    by remember { mutableStateOf(false) }
    var saving      by remember { mutableStateOf(false) }
    var saved       by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        if (uid == null) return@LaunchedEffect
        try {
            val snap = db.collection("users").document(uid).get().await()
            reverseOn9 = snap.getBoolean("meermauReverseOn9") ?: false
            stopperOn8 = snap.getBoolean("meermauStopperOn8") ?: false
            wildOn10   = snap.getBoolean("meermauWildOn10")   ?: false
        } catch (_: Exception) {}
    }

    fun doSave() {
        if (uid == null) return
        scope.launch {
            saving = true
            try {
                db.collection("users").document(uid).update(
                    mapOf(
                        "meermauReverseOn9" to reverseOn9,
                        "meermauStopperOn8" to stopperOn8,
                        "meermauWildOn10"   to wildOn10,
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
        gameLabel = "MEERMAU",
        accentColor = PurpleDeep,
        saving = saving,
        saved = saved,
        onBack = onNavigateBack,
        onSave = ::doSave,
        onNavigateToProfile = onNavigateToProfile,
    ) {
        SettingsGroupLabel("Optionale Regeln")
        SettingsSwitchRow(
            title = "9 kehrt Richtung um",
            description = "Eine 9 kehrt die Spielrichtung um (nur bei 3+ Spielern sinnvoll).",
            checked = reverseOn9,
            accentColor = PurpleDeep,
            onCheckedChange = { reverseOn9 = it },
        )
        SettingsSwitchRow(
            title = "8 stoppt 7-Stapel",
            description = "Eine 8 kann einen laufenden Zieh-Stapel stoppen statt mehr Karten zu ziehen.",
            checked = stopperOn8,
            accentColor = PurpleDeep,
            onCheckedChange = { stopperOn8 = it },
        )
        SettingsSwitchRow(
            title = "10 ist Joker",
            description = "Eine 10 wirkt wie ein Bube – der Spieler darf sich eine Farbe wünschen.",
            checked = wildOn10,
            accentColor = PurpleDeep,
            onCheckedChange = { wildOn10 = it },
        )
    }
}
