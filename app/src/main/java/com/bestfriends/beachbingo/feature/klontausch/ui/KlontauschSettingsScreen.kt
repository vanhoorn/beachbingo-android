package com.bestfriends.beachbingo.feature.klontausch.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bestfriends.beachbingo.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private val KlontauschAccent = Color(0xFF8B5CF6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KlontauschSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToGallery: () -> Unit = {},
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid
    val scope = rememberCoroutineScope()

    var soundEnabled by remember { mutableStateOf(true) }
    var musicEnabled by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        if (uid == null) { loading = false; return@LaunchedEffect }
        try {
            val snap = db.collection("users").document(uid).get().await()
            soundEnabled = snap.getBoolean("klontausch_soundEnabled") ?: true
            musicEnabled = snap.getBoolean("klontausch_musicEnabled") ?: true
        } catch (_: Exception) {}
        loading = false
    }

    fun save() {
        if (uid == null) return
        saving = true
        scope.launch {
            try {
                db.collection("users").document(uid).update(
                    mapOf(
                        "klontausch_soundEnabled" to soundEnabled,
                        "klontausch_musicEnabled" to musicEnabled,
                    )
                ).await()
            } catch (_: Exception) {}
            saving = false
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Klontausch – Einstellungen", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = TextSub)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark),
            )
        },
        containerColor = BgDark,
    ) { padding ->
        if (loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = KlontauschAccent)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Audio", fontWeight = FontWeight.Bold, color = TextPrimary)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text("Soundeffekte", color = TextPrimary)
                            Text("Karten- und Tausch-Sounds", color = TextMuted, fontSize = MaterialTheme.typography.bodySmall.fontSize)
                        }
                        Switch(
                            checked = soundEnabled,
                            onCheckedChange = { soundEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = KlontauschAccent, checkedTrackColor = KlontauschAccent.copy(0.3f)),
                        )
                    }

                    HorizontalDivider(color = BorderColor)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text("Musik", color = TextPrimary)
                            Text("Hintergrundmusik", color = TextMuted, fontSize = MaterialTheme.typography.bodySmall.fontSize)
                        }
                        Switch(
                            checked = musicEnabled,
                            onCheckedChange = { musicEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = KlontauschAccent, checkedTrackColor = KlontauschAccent.copy(0.3f)),
                        )
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Spielfiguren", fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(
                        "Alle 38 Figuren im Überblick — schaut euch an, wer mitspielen kann.",
                        color = TextMuted,
                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                    )
                    OutlinedButton(
                        onClick = onNavigateToGallery,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = KlontauschAccent),
                        border = BorderStroke(1.dp, KlontauschAccent.copy(0.5f)),
                    ) {
                        Text("Figurengalerie anzeigen")
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { save() },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = KlontauschAccent),
            ) {
                if (saving) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = TextPrimary, strokeWidth = 2.dp)
                else Text("Speichern", fontWeight = FontWeight.Bold)
            }
        }
    }
}
