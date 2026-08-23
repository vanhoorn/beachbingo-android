package com.bestfriends.beachbingo.feature.sonnenrad.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bestfriends.beachbingo.feature.sonnenrad.SonnenradBoardModel
import com.bestfriends.beachbingo.feature.sonnenrad.SonnenradLadderState
import com.bestfriends.beachbingo.feature.sonnenrad.SonnenradPhase
import com.bestfriends.beachbingo.feature.sonnenrad.SonnenradState
import com.bestfriends.beachbingo.feature.sonnenrad.SonnenradSymbol
import com.bestfriends.beachbingo.ui.components.GameHudBar
import com.bestfriends.beachbingo.ui.components.QuitConfirmDialog
import com.bestfriends.beachbingo.ui.theme.BgDark
import com.bestfriends.beachbingo.ui.theme.BorderColor
import com.bestfriends.beachbingo.ui.theme.EmojiLarge
import com.bestfriends.beachbingo.ui.theme.MahjongGold
import com.bestfriends.beachbingo.ui.theme.OceanBlue
import com.bestfriends.beachbingo.ui.theme.Surface2Dark
import com.bestfriends.beachbingo.ui.theme.SurfaceDark
import com.bestfriends.beachbingo.ui.theme.Success
import com.bestfriends.beachbingo.ui.theme.TextMuted
import com.bestfriends.beachbingo.ui.theme.TextPrimary
import com.bestfriends.beachbingo.ui.theme.TextSub
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

// ── Einstiegspunkt ────────────────────────────────────────────────────────────

@Composable
fun SonnenradBonusScreen(
    onNavigateBack: () -> Unit,
    soundEnabled: Boolean = true,
    musicEnabled: Boolean = true,
    model: SonnenradBoardModel = viewModel(),
) {
    val state by model.state.collectAsState()

    var cardsFaceUp by remember { mutableStateOf(false) }
    var prevPhase by remember { mutableStateOf<SonnenradPhase?>(null) }
    var showQuit by remember { mutableStateOf(false) }
    var showRules by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val audioManager = remember { SonnenradAudioManager(context) }
    DisposableEffect(Unit) {
        audioManager.setSound(soundEnabled)
        onDispose { audioManager.release() }
    }

    val isClimbing = state.phase == SonnenradPhase.CLIMBING
    val isTargetZone = (state.ladderState as? SonnenradLadderState.Active)?.isTargetZone ?: false

    LaunchedEffect(state.phase) {
        when (state.phase) {
            SonnenradPhase.LOADING,
            SonnenradPhase.BONUS_READY -> cardsFaceUp = false

            SonnenradPhase.SHUFFLING -> {
                delay(1300L)
                model.onShuffleComplete()
            }

            SonnenradPhase.REVEALING -> {
                delay(80L)
                cardsFaceUp = true
                delay(750L)
                model.onRevealComplete()
            }

            SonnenradPhase.AWAITING_CHOICE -> {
                if (prevPhase == SonnenradPhase.CLIMBING) audioManager.playSound("step_up")
            }

            SonnenradPhase.FINISHED -> {
                val finished = state.ladderState as? SonnenradLadderState.Finished
                if ((finished?.pointsAwarded ?: 0) > 0) audioManager.playSound("secure")
            }

            else -> Unit
        }
        prevPhase = state.phase
    }

    LaunchedEffect(cardsFaceUp) {
        if (cardsFaceUp) audioManager.playSound("reveal")
    }

    LaunchedEffect(isTargetZone, isClimbing) {
        if (isClimbing) audioManager.playSound("tick")
    }

    BackHandler { showQuit = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .navigationBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            GameHudBar(
                showPause = false,
                paused = false,
                onPauseToggle = {},
                onQuit = { showQuit = true },
                onShowRules = { showRules = true },
            ) {
                Text("☀️", fontSize = 22.sp)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        "TAGESBONUS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.5.sp,
                    )
                    Text(
                        "Sonnenrad",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                    )
                }
                if (state.lifetimePoints > 0) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MahjongGold.copy(alpha = 0.15f),
                        modifier = Modifier.border(1.dp, MahjongGold.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
                    ) {
                        Text(
                            "${state.lifetimePoints} Pkt.",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MahjongGold,
                        )
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (state.phase) {
                    SonnenradPhase.LOADING         -> SonnenradLoadingContent()
                    SonnenradPhase.BONUS_READY     -> SonnenradReadyContent(
                        state = state,
                        onTap = { model.startShuffle() },
                    )
                    SonnenradPhase.SHUFFLING       -> SonnenradShufflingContent()
                    SonnenradPhase.REVEALING       -> SonnenradRevealingContent(
                        symbols = state.symbols,
                        cardsFaceUp = cardsFaceUp,
                    )
                    SonnenradPhase.AWAITING_CHOICE -> SonnenradChoiceContent(
                        state = state,
                        model = model,
                    )
                    SonnenradPhase.CLIMBING        -> SonnenradClimbingContent(
                        state = state,
                        model = model,
                    )
                    SonnenradPhase.FINISHED        -> SonnenradFinishedContent(
                        state = state,
                        onReset = { model.resetToReady() },
                        onBack = onNavigateBack,
                    )
                }
            }
        }
    }

    if (showQuit) {
        QuitConfirmDialog(
            emoji = "☀️",
            message = "Das Sonnenrad-Spiel wird beendet.",
            onConfirm = onNavigateBack,
            onDismiss = { showQuit = false },
        )
    }

    if (showRules) {
        SonnenradRulesDialog(onDismiss = { showRules = false })
    }
}

@Composable
private fun SonnenradRulesDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = SurfaceDark) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("☀️", fontSize = EmojiLarge)
                Text(
                    "Sonnenrad – Regeln",
                    fontSize = MaterialTheme.typography.titleMedium.fontSize,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Decke 3 Muschelkarten auf. Die Symbole bestimmen, wie hoch du in die Bonusleiter einsteigst:",
                        color = TextSub,
                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                    )
                    Text(
                        "• 3× Basissymbol → Stufe 2\n• 2× gleiches Symbol → Stufe 1\n• 3× Sonnenschirm → Stufe 4\n• Kein Treffer → keine Punkte",
                        color = TextSub,
                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                    )
                    Text(
                        "Danach: Punkte sichern oder per Timing-Herausforderung höher steigen. Tippe im Zielfeld, um eine Stufe aufzusteigen – verfehle es und die Runde endet.",
                        color = TextSub,
                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MahjongGold),
                ) {
                    Text("Verstanden", color = BgDark, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── Loading ───────────────────────────────────────────────────────────────────

@Composable
private fun SonnenradLoadingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MahjongGold)
    }
}

// ── Bereit zum Spielen ────────────────────────────────────────────────────────

@Composable
private fun SonnenradReadyContent(state: SonnenradState, onTap: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "card_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.30f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow_alpha",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onTap() }
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (state.isBonusRound) {
            Text(
                text = "🌟 Tagesbonus verfügbar!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MahjongGold,
                textAlign = TextAlign.Center,
            )
        } else {
            Text(
                text = "Normales Spiel",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Tagesbonus in ${formatMs(state.nextBonusMs)}",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(32.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(3) {
                val borderColor = if (state.isBonusRound)
                    MahjongGold.copy(alpha = glowAlpha) else BorderColor
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .border(2.dp, borderColor, RoundedCornerShape(12.dp)),
                ) {
                    SonnenradSymbolCard(symbol = null, faceUp = false, sizeDp = 100.dp)
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        Text(
            text = if (state.isBonusRound) "✨ Tippe zum Aufdecken" else "Tippe zum Spielen (1/3 Punkte)",
            style = MaterialTheme.typography.labelLarge,
            color = TextMuted,
        )
    }
}

// ── Mischen ───────────────────────────────────────────────────────────────────

@Composable
private fun SonnenradShufflingContent() {
    val infiniteTransition = rememberInfiniteTransition(label = "shuffle")
    val t by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(280, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shuffle_t",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Karten werden gemischt...",
            style = MaterialTheme.typography.titleSmall,
            color = TextMuted,
        )
        Spacer(Modifier.height(32.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            listOf(t * 18f, -t * 14f, t * 9f).forEach { xOff ->
                SonnenradSymbolCard(
                    symbol = null,
                    faceUp = false,
                    sizeDp = 100.dp,
                    modifier = Modifier.offset { IntOffset(xOff.roundToInt(), 0) },
                )
            }
        }
    }
}

// ── Aufdecken ─────────────────────────────────────────────────────────────────

@Composable
private fun SonnenradRevealingContent(
    symbols: List<SonnenradSymbol>,
    cardsFaceUp: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = if (cardsFaceUp) "Ergebnis:" else "Aufdecken...",
            style = MaterialTheme.typography.titleSmall,
            color = TextMuted,
        )
        Spacer(Modifier.height(32.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(3) { idx ->
                SonnenradSymbolCard(
                    symbol = symbols.getOrNull(idx),
                    faceUp = cardsFaceUp,
                    sizeDp = 100.dp,
                )
            }
        }
    }
}

// ── Karten-Zeile (fuer Choice + Climbing) ─────────────────────────────────────

@Composable
private fun SmallCardRow(symbols: List<SonnenradSymbol>) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { idx ->
            SonnenradSymbolCard(
                symbol = symbols.getOrNull(idx),
                faceUp = true,
                sizeDp = 80.dp,
            )
        }
    }
}

// ── Wahl treffen ──────────────────────────────────────────────────────────────

@Composable
private fun SonnenradChoiceContent(state: SonnenradState, model: SonnenradBoardModel) {
    val active = state.ladderState as? SonnenradLadderState.Active ?: return
    val securedStep = active.securedStep
    val canClimb = securedStep < model.maxStep

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SmallCardRow(state.symbols)
        Spacer(Modifier.height(12.dp))

        val (hitLabel, hitSub) = matchText(state.symbols)
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MahjongGold.copy(alpha = 0.12f),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MahjongGold.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(hitLabel, fontWeight = FontWeight.Bold, color = MahjongGold, fontSize = 15.sp)
                Text(hitSub, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }
        }

        Spacer(Modifier.height(16.dp))
        SonnenradLadder(model = model, ladderState = state.ladderState, isClimbing = false)
        Spacer(Modifier.height(20.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { model.collect() },
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    text = "Einsammeln\n(${model.pointsForStep(securedStep)} Pkt.)",
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                )
            }

            Button(
                onClick = { model.startClimbing() },
                enabled = canClimb,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
            ) {
                Text(
                    text = "Klettern ↑",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

// ── Timing-Challenge ──────────────────────────────────────────────────────────

@Composable
private fun SonnenradClimbingContent(state: SonnenradState, model: SonnenradBoardModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SmallCardRow(state.symbols)
        Spacer(Modifier.height(8.dp))

        Text(
            text = "Tippe wenn der Pfeil oben ist!",
            style = MaterialTheme.typography.labelLarge,
            color = TextMuted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))

        SonnenradLadder(
            model = model,
            ladderState = state.ladderState,
            isClimbing = true,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { model.onMarkerTapped() },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
        ) {
            Text(
                text = "Jetzt!",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

// ── Leiter-Anzeige ────────────────────────────────────────────────────────────

@Composable
private fun SonnenradLadder(
    model: SonnenradBoardModel,
    ladderState: SonnenradLadderState,
    isClimbing: Boolean,
    modifier: Modifier = Modifier,
) {
    val secured = when (ladderState) {
        is SonnenradLadderState.Active   -> ladderState.securedStep
        is SonnenradLadderState.Finished -> ladderState.finalStep
        SonnenradLadderState.Idle        -> 0
    }
    val isTargetZone = (ladderState as? SonnenradLadderState.Active)?.isTargetZone ?: false

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        for (step in model.maxStep downTo 0) {
            val isSecured = step == secured
            val isMarkerTarget = isClimbing && isTargetZone && step == secured + 1
            val isMarkerBase   = isClimbing && !isTargetZone && step == secured

            val bgColor by animateColorAsState(
                targetValue = when {
                    isMarkerTarget -> Success.copy(alpha = 0.30f)
                    isMarkerBase   -> MahjongGold.copy(alpha = 0.35f)
                    isSecured      -> MahjongGold.copy(alpha = 0.18f)
                    else           -> SurfaceDark
                },
                animationSpec = tween(130),
                label = "row_bg_$step",
            )
            val borderColor by animateColorAsState(
                targetValue = when {
                    isMarkerTarget -> Success.copy(alpha = 0.70f)
                    isMarkerBase   -> MahjongGold.copy(alpha = 0.80f)
                    isSecured      -> MahjongGold.copy(alpha = 0.50f)
                    else           -> BorderColor
                },
                animationSpec = tween(130),
                label = "row_bd_$step",
            )

            val points = model.pointsForStep(step)
            val stepLabel = when (step) {
                0            -> "Start"
                model.maxStep -> "Maximum"
                4            -> "Jackpot ☀️"
                else         -> "Stufe $step"
            }
            val markerIcon = when {
                isMarkerTarget -> " ⬆️"
                isMarkerBase   -> " ◄"
                isSecured && !isClimbing -> " ✓"
                else -> ""
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgColor, RoundedCornerShape(8.dp))
                    .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (step == 0) "—" else "${points} Pkt.",
                    modifier = Modifier.width(80.dp),
                    fontWeight = if (isSecured || isMarkerTarget || isMarkerBase) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSecured || isMarkerTarget || isMarkerBase) TextPrimary else TextMuted,
                    fontSize = 14.sp,
                )
                Text(
                    text = stepLabel,
                    modifier = Modifier.weight(1f),
                    color = TextSub,
                    fontSize = 13.sp,
                )
                Text(
                    text = markerIcon,
                    fontSize = 16.sp,
                )
            }
        }
    }
}

// ── Ergebnis ──────────────────────────────────────────────────────────────────

@Composable
private fun SonnenradFinishedContent(state: SonnenradState, onReset: () -> Unit, onBack: () -> Unit) {
    val finished = state.ladderState as? SonnenradLadderState.Finished
    val points = finished?.pointsAwarded ?: 0
    val won = points > 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = if (won) "🎉" else "😔", fontSize = 72.sp)
        Spacer(Modifier.height(20.dp))

        if (won) {
            Text(
                text = "+$points Punkte!",
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MahjongGold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Herzlichen Glueckwunsch!",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                textAlign = TextAlign.Center,
            )
        } else {
            Text(
                text = "Kein Treffer",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Morgen hast du wieder eine Chance!",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(24.dp))
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Surface2Dark,
        ) {
            Text(
                text = "Gesamt: ${state.lifetimePoints} Pkt.",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                style = MaterialTheme.typography.titleSmall,
                color = TextSub,
            )
        }

        Spacer(Modifier.height(40.dp))
        Button(
            onClick = onReset,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
        ) {
            Text("Nochmal spielen", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text("Zurück", fontSize = 16.sp)
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatMs(ms: Long): String {
    val total = (ms / 1000L).coerceAtLeast(0L)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return "%02d:%02d:%02d".format(h, m, s)
}

private fun matchText(symbols: List<SonnenradSymbol>): Pair<String, String> {
    if (symbols.isEmpty()) return ("" to "")
    val counts = symbols.groupingBy { it }.eachCount()
    return when {
        counts[SonnenradSymbol.SONNENSCHIRM] == 3 ->
            "3× Sonnenschirm" to "Jackpot! ☀️"
        counts.values.any { it == 3 } -> {
            val sym = counts.entries.first { it.value == 3 }.key
            "3× ${symbolName(sym)}" to "Grosser Treffer!"
        }
        counts.values.any { it >= 2 } -> {
            val sym = counts.entries.first { it.value >= 2 }.key
            "2× ${symbolName(sym)}" to "Kleiner Treffer"
        }
        else -> "Kein Treffer" to "Diesmal leider nichts"
    }
}

private fun symbolName(sym: SonnenradSymbol): String = when (sym) {
    SonnenradSymbol.SONNE        -> "Sonne"
    SonnenradSymbol.WELLE        -> "Welle"
    SonnenradSymbol.PALME        -> "Palme"
    SonnenradSymbol.MUSCHEL      -> "Muschel"
    SonnenradSymbol.SONNENSCHIRM -> "Sonnenschirm"
}
