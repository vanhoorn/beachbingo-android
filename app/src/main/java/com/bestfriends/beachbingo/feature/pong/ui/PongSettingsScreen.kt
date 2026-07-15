package com.bestfriends.beachbingo.feature.pong.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bestfriends.beachbingo.core.model.PongDifficulty
import com.bestfriends.beachbingo.feature.auth.viewmodel.AuthViewModel
import com.bestfriends.beachbingo.ui.theme.BgDark
import com.bestfriends.beachbingo.ui.theme.BorderColor
import com.bestfriends.beachbingo.ui.theme.Coral
import com.bestfriends.beachbingo.ui.theme.OceanBlue
import com.bestfriends.beachbingo.ui.theme.SurfaceDark
import com.bestfriends.beachbingo.ui.theme.Surface2Dark
import com.bestfriends.beachbingo.ui.theme.TextMuted
import com.bestfriends.beachbingo.ui.theme.TextPrimary
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PongSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var paddles by rememberSaveable { mutableIntStateOf(2) }
    var difficulty by rememberSaveable { mutableStateOf(PongDifficulty.ROOKIE) }
    var scoreLimit by rememberSaveable { mutableIntStateOf(7) }
    var isSaving by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

    // Load existing preferences
    LaunchedEffect(currentUser) {
        val user = currentUser ?: return@LaunchedEffect
        paddles = user.preferredPongPaddles ?: 2
        difficulty = user.preferredPongDifficulty ?: PongDifficulty.ROOKIE
        scoreLimit = user.preferredPongScoreLimit ?: 7
    }

    fun doSave() {
        val uid = currentUser?.uid ?: return
        saved = false
        isSaving = true
        scope.launch {
            try {
                Firebase.firestore.collection("users").document(uid).update(mapOf(
                    "preferredPongPaddles" to paddles,
                    "preferredPongDifficulty" to difficulty.name,
                    "preferredPongScoreLimit" to scoreLimit,
                )).await()
                saved = true
            } catch (_: Exception) {
            } finally {
                isSaving = false
            }
        }
    }

    Scaffold(
        containerColor = BgDark,
        topBar = {
            TopAppBar(
                title = { Text("BeachVolley Einstellungen", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück", tint = OceanBlue)
                    }
                },
                actions = {
                    androidx.compose.material3.TextButton(
                        onClick = { doSave() },
                        enabled = !isSaving,
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = OceanBlue, strokeWidth = 2.dp)
                        } else {
                            Text(if (saved) "✓" else "Speichern", color = OceanBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Preferred paddles
            SettingsSection("Bevorzugte Paddles") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(2, 3, 4).forEach { n ->
                        SettingsPill(
                            label = "$n Paddles",
                            active = paddles == n,
                            color = Coral,
                            modifier = Modifier.weight(1f)
                        ) { paddles = n }
                    }
                }
            }

            // Preferred difficulty
            SettingsSection("KI-Schwierigkeit") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Triple(PongDifficulty.ROOKIE, "Rookie", "Langsam, macht Fehler"),
                        Triple(PongDifficulty.SNIPER, "Sniper", "Schnell, trifft meistens"),
                        Triple(PongDifficulty.BOSS_LEVEL, "Boss Level", "Unerbittlich — viel Spaß 😈"),
                    ).forEach { (opt, label, desc) ->
                        val active = difficulty == opt
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (active) OceanBlue.copy(alpha = 0.12f) else SurfaceDark)
                                .border(if (active) 2.dp else 1.5.dp, if (active) OceanBlue else BorderColor, RoundedCornerShape(8.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { difficulty = opt }
                                .padding(13.dp, 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text(desc, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(RoundedCornerShape(50))
                                        .border(if (active) 5.dp else 2.dp, if (active) OceanBlue else BorderColor, RoundedCornerShape(50))
                                )
                            }
                        }
                    }
                }
            }

            // Score limit
            SettingsSection("Punkte zum Sieg") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceDark)
                        .border(1.5.dp, BorderColor, RoundedCornerShape(8.dp))
                        .padding(12.dp, 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingsStepButton("-") { scoreLimit = (scoreLimit - 1).coerceAtLeast(1) }
                    Text(
                        "$scoreLimit Punkte",
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    SettingsStepButton("+") { scoreLimit = (scoreLimit + 1).coerceAtMost(21) }
                }
            }

            // Musik & Soundeffekte
            Column {
                Text(
                    "AUDIO",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp
                )
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceDark)
                        .border(1.5.dp, BorderColor, RoundedCornerShape(8.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "💡 Musik & Soundeffekte findest du in Profil & Abmelden.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Öffnen →",
                            style = MaterialTheme.typography.bodySmall,
                            color = OceanBlue,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onNavigateToProfile() }
                        )
                    }
                }
            }

            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp
        )
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun SettingsPill(label: String, active: Boolean, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) color.copy(alpha = 0.15f) else SurfaceDark)
            .border(if (active) 2.dp else 1.5.dp, if (active) color else BorderColor, RoundedCornerShape(8.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(12.dp, 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
    }
}

@Composable
private fun SettingsStepButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Surface2Dark)
            .border(1.5.dp, BorderColor, RoundedCornerShape(8.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}
