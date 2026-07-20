package com.bestfriends.beachbingo.feature.meermau.ui

import androidx.compose.foundation.border
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bestfriends.beachbingo.ui.theme.BgDark
import com.bestfriends.beachbingo.ui.theme.BorderColor
import com.bestfriends.beachbingo.ui.theme.Success
import com.bestfriends.beachbingo.ui.theme.SurfaceDark
import com.bestfriends.beachbingo.ui.theme.TextMuted
import com.bestfriends.beachbingo.ui.theme.TextPrimary
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private val MeermauViolet = Color(0xFF7C3AED)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeermauSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid
    val scope = rememberCoroutineScope()

    var reverseOn9 by remember { mutableStateOf(false) }
    var stopperOn8 by remember { mutableStateOf(false) }
    var wildOn10 by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        if (uid == null) return@LaunchedEffect
        try {
            val snap = db.collection("users").document(uid).get().await()
            reverseOn9 = snap.getBoolean("meermauReverseOn9") ?: false
            stopperOn8 = snap.getBoolean("meermauStopperOn8") ?: false
            wildOn10 = snap.getBoolean("meermauWildOn10") ?: false
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
                        "meermauWildOn10" to wildOn10,
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
                title = { Text("MeerMau Einstellungen", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück", tint = TextPrimary)
                    }
                },
                actions = {
                    TextButton(onClick = { doSave() }, enabled = !saving) {
                        if (saving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MeermauViolet, strokeWidth = 2.dp)
                        } else {
                            Text(if (saved) "✓ Gespeichert" else "Speichern", color = MeermauViolet)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 12.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Optionale Regeln", style = MaterialTheme.typography.labelLarge, color = com.bestfriends.beachbingo.ui.theme.TextSub, modifier = Modifier.padding(start = 4.dp))

            MmToggleRow(
                title = "9 kehrt Richtung um",
                description = "Eine 9 kehrt die Spielrichtung um (nur bei 3+ Spielern sinnvoll).",
                checked = reverseOn9, onCheckedChange = { reverseOn9 = it },
            )
            MmToggleRow(
                title = "8 stoppt 7-Stapel",
                description = "Eine 8 kann einen laufenden Zieh-Stapel stoppen statt mehr Karten zu ziehen.",
                checked = stopperOn8, onCheckedChange = { stopperOn8 = it },
            )
            MmToggleRow(
                title = "10 ist Joker",
                description = "Eine 10 wirkt wie ein Bube – der Spieler darf sich eine Farbe wünschen.",
                checked = wildOn10, onCheckedChange = { wildOn10 = it },
            )

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

@Composable
private fun MmToggleRow(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Surface(
        color = SurfaceDark,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(10.dp)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                Text(description, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MeermauViolet,
                    checkedTrackColor = MeermauViolet.copy(alpha = 0.4f),
                ),
            )
        }
    }
}
