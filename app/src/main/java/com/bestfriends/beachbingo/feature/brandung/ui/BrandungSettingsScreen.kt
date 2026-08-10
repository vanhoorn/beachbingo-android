package com.bestfriends.beachbingo.feature.brandung.ui

import androidx.compose.runtime.*
import com.bestfriends.beachbingo.ui.components.GameSettingsScaffold
import com.bestfriends.beachbingo.ui.components.SettingsGroupLabel
import com.bestfriends.beachbingo.ui.components.SettingsSwitchRow
import com.bestfriends.beachbingo.ui.theme.Teal
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun BrandungSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
) {
    val auth  = FirebaseAuth.getInstance()
    val db    = FirebaseFirestore.getInstance()
    val uid   = auth.currentUser?.uid
    val scope = rememberCoroutineScope()

    var newCardsOnAllPass by remember { mutableStateOf(false) }
    var passingForbidden  by remember { mutableStateOf(false) }
    var saving            by remember { mutableStateOf(false) }
    var saved             by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        if (uid == null) return@LaunchedEffect
        try {
            val snap = db.collection("users").document(uid).get().await()
            newCardsOnAllPass = snap.getBoolean("brandungNewCardsOnAllPass") ?: false
            passingForbidden  = snap.getBoolean("brandungPassingForbidden")  ?: false
        } catch (_: Exception) {}
    }

    fun doSave() {
        if (uid == null) return
        scope.launch {
            saving = true
            try {
                db.collection("users").document(uid).update(
                    mapOf(
                        "brandungNewCardsOnAllPass" to newCardsOnAllPass,
                        "brandungPassingForbidden"  to passingForbidden,
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
        gameLabel = "BRANDUNG",
        accentColor = Teal,
        saving = saving,
        saved = saved,
        onBack = onNavigateBack,
        onSave = ::doSave,
        onNavigateToProfile = onNavigateToProfile,
    ) {
        SettingsGroupLabel("Spielregeln")
        SettingsSwitchRow(
            title = "Neue Karten wenn alle schieben",
            description = "Wenn alle Spieler schieben, werden neue Tischkarten ausgeteilt.",
            checked = newCardsOnAllPass,
            accentColor = Teal,
            onCheckedChange = { newCardsOnAllPass = it },
        )
        SettingsSwitchRow(
            title = "Schieben verboten",
            description = "Spieler müssen immer mindestens eine Karte tauschen.",
            checked = passingForbidden,
            accentColor = Teal,
            onCheckedChange = { passingForbidden = it },
        )
    }
}
