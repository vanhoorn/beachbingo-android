package com.bestfriends.beachbingo.feature.pong.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bestfriends.beachbingo.core.model.PongDifficulty
import com.bestfriends.beachbingo.feature.auth.viewmodel.AuthViewModel
import com.bestfriends.beachbingo.ui.components.GameSettingsScaffold
import com.bestfriends.beachbingo.ui.components.SettingsGroupLabel
import com.bestfriends.beachbingo.ui.components.SettingsRadioRow
import com.bestfriends.beachbingo.ui.theme.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun PongSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
    authViewModel: AuthViewModel = hiltViewModel(),
) {
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var paddles    by rememberSaveable { mutableIntStateOf(2) }
    var difficulty by rememberSaveable { mutableStateOf(PongDifficulty.ROOKIE) }
    var scoreLimit by rememberSaveable { mutableIntStateOf(7) }
    var isSaving   by remember { mutableStateOf(false) }
    var saved      by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser) {
        val user = currentUser ?: return@LaunchedEffect
        paddles    = user.preferredPongPaddles    ?: 2
        difficulty = user.preferredPongDifficulty ?: PongDifficulty.ROOKIE
        scoreLimit = user.preferredPongScoreLimit ?: 7
    }

    fun doSave() {
        val uid = currentUser?.uid ?: return
        saved    = false
        isSaving = true
        scope.launch {
            try {
                Firebase.firestore.collection("users").document(uid).update(mapOf(
                    "preferredPongPaddles"    to paddles,
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

    GameSettingsScaffold(
        gameLabel = "BEACHVOLLEY",
        accentColor = OceanBlue,
        saving = isSaving,
        saved = saved,
        onBack = onNavigateBack,
        onSave = ::doSave,
        onNavigateToProfile = onNavigateToProfile,
    ) {
        SettingsGroupLabel("Bevorzugte Paddles")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(2, 3, 4).forEach { n ->
                PongPill(
                    label = "$n Paddles",
                    active = paddles == n,
                    color = Coral,
                    modifier = Modifier.weight(1f),
                    onClick = { paddles = n },
                )
            }
        }

        SettingsGroupLabel("KI-Schwierigkeit")
        listOf(
            Triple(PongDifficulty.ROOKIE,     "Rookie",     "Langsam, macht Fehler"),
            Triple(PongDifficulty.SNIPER,     "Sniper",     "Schnell, trifft meistens"),
            Triple(PongDifficulty.BOSS_LEVEL, "Boss Level", "Unerbittlich – viel Spaß 😈"),
        ).forEach { (opt, label, desc) ->
            SettingsRadioRow(
                title = label,
                desc = desc,
                selected = difficulty == opt,
                accentColor = OceanBlue,
                onClick = { difficulty = opt },
            )
        }

        SettingsGroupLabel("Punkte zum Sieg")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceDark)
                .border(1.5.dp, BorderColor, RoundedCornerShape(8.dp))
                .padding(12.dp, 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PongStepButton("-") { scoreLimit = (scoreLimit - 1).coerceAtLeast(1) }
            Text(
                "$scoreLimit Punkte",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = MaterialTheme.typography.titleLarge.fontSize,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
            )
            PongStepButton("+") { scoreLimit = (scoreLimit + 1).coerceAtMost(21) }
        }
    }
}

@Composable
private fun PongPill(label: String, active: Boolean, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) color.copy(alpha = 0.15f) else SurfaceDark)
            .border(if (active) 2.dp else 1.5.dp, if (active) color else BorderColor, RoundedCornerShape(8.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(12.dp, 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = MaterialTheme.typography.labelMedium.fontSize, color = TextPrimary)
    }
}

@Composable
private fun PongStepButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Surface2Dark)
            .border(1.5.dp, BorderColor, RoundedCornerShape(8.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = BingoCallSize, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}
