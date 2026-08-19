package com.bestfriends.beachbingo.feature.sonnenrad

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

// ── Symbole ───────────────────────────────────────────────────────────────────

enum class SonnenradSymbol {
    SONNE, WELLE, PALME, MUSCHEL,
    SONNENSCHIRM  // Sonderzeichen, selten (~10 % pro Slot)
}

// ── Phasen ────────────────────────────────────────────────────────────────────

enum class SonnenradPhase {
    LOADING,
    BONUS_READY,
    SHUFFLING,
    REVEALING,
    AWAITING_CHOICE,
    CLIMBING,
    FINISHED,
}

// ── Zustand ───────────────────────────────────────────────────────────────────

data class SonnenradState(
    val phase: SonnenradPhase = SonnenradPhase.LOADING,
    /** true = Tagesbonus verfügbar (volle Punkte), false = normales Spiel (1/3 Punkte) */
    val isBonusRound: Boolean = false,
    /** Millisekunden bis zum nächsten Tagesbonus – zur Info-Anzeige */
    val nextBonusMs: Long = 0L,
    val symbols: List<SonnenradSymbol> = emptyList(),
    val ladderState: SonnenradLadderState = SonnenradLadderState.Idle,
    val lifetimePoints: Int = 0,
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

class SonnenradBoardModel(application: Application) : AndroidViewModel(application) {

    private val prefs get() = getApplication<Application>()
        .getSharedPreferences("sonnenrad", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(SonnenradState())
    val state: StateFlow<SonnenradState> = _state.asStateFlow()

    private var timingJob: Job? = null
    private var countdownJob: Job? = null

    // ── Leiter-Konstanten ─────────────────────────────────────────────────────

    /** Volle Punkte (Tagesbonus): Stufen 0–6 */
    val stepPoints       = intArrayOf(0,  50, 100, 175, 275, 400, 600)
    /** Reduzierte Punkte (normales Spiel, ca. 1/3): Stufen 0–6 */
    val normalStepPoints = intArrayOf(0,  17,  33,  58,  92, 133, 200)
    val maxStep = 6

    private val markerTickMs = 175L
    private val symbolWeights = intArrayOf(9, 9, 9, 9, 4)

    init { loadState() }

    // ── Init ──────────────────────────────────────────────────────────────────

    private fun loadState() {
        val lastClaimed    = prefs.getLong(KEY_LAST_CLAIMED, 0L)
        val lifetimePoints = prefs.getInt(KEY_LIFETIME_POINTS, 0)
        val bonusAvailable = isBonusAvailable(lastClaimed)
        _state.value = SonnenradState(
            phase          = SonnenradPhase.BONUS_READY,
            isBonusRound   = bonusAvailable,
            nextBonusMs    = if (bonusAvailable) 0L else msUntilNextBonus(),
            lifetimePoints = lifetimePoints,
        )
        if (!bonusAvailable) startCountdown()
    }

    // ── Öffentliche Aktionen ──────────────────────────────────────────────────

    fun startShuffle() {
        if (_state.value.phase != SonnenradPhase.BONUS_READY) return
        val symbols = generateSymbols()
        _state.update { it.copy(phase = SonnenradPhase.SHUFFLING, symbols = symbols) }
    }

    fun onShuffleComplete() {
        if (_state.value.phase != SonnenradPhase.SHUFFLING) return
        _state.update { it.copy(phase = SonnenradPhase.REVEALING) }
    }

    fun onRevealComplete() {
        if (_state.value.phase != SonnenradPhase.REVEALING) return
        val entryStep = computeEntryStep(_state.value.symbols)
        if (entryStep == 0) {
            finishRound(0)
        } else {
            _state.update { it.copy(
                phase       = SonnenradPhase.AWAITING_CHOICE,
                ladderState = SonnenradLadderState.Active(securedStep = entryStep),
            )}
        }
    }

    fun collect() {
        val active = _state.value.ladderState as? SonnenradLadderState.Active ?: return
        timingJob?.cancel()
        finishRound(active.securedStep)
    }

    fun startClimbing() {
        val active = _state.value.ladderState as? SonnenradLadderState.Active ?: return
        if (active.securedStep >= maxStep) { collect(); return }
        _state.update { it.copy(phase = SonnenradPhase.CLIMBING) }
        launchTimingMarker()
    }

    fun onMarkerTapped() {
        if (_state.value.phase != SonnenradPhase.CLIMBING) return
        val active = _state.value.ladderState as? SonnenradLadderState.Active ?: return
        timingJob?.cancel()
        if (active.isTargetZone) {
            val newStep = active.securedStep + 1
            if (newStep >= maxStep) {
                finishRound(newStep)
            } else {
                _state.update { it.copy(
                    phase       = SonnenradPhase.AWAITING_CHOICE,
                    ladderState = SonnenradLadderState.Active(securedStep = newStep),
                )}
            }
        } else {
            finishRound(active.securedStep)
        }
    }

    /** Setzt nach FINISHED zurück zu BONUS_READY für eine neue Runde (unbegrenzt). */
    fun resetToReady() {
        if (_state.value.phase != SonnenradPhase.FINISHED) return
        val bonusAvailable = isBonusAvailable(prefs.getLong(KEY_LAST_CLAIMED, 0L))
        countdownJob?.cancel()
        _state.update { it.copy(
            phase       = SonnenradPhase.BONUS_READY,
            isBonusRound = bonusAvailable,
            nextBonusMs  = if (bonusAvailable) 0L else msUntilNextBonus(),
            symbols      = emptyList(),
            ladderState  = SonnenradLadderState.Idle,
        )}
        if (!bonusAvailable) startCountdown()
    }

    fun pointsForStep(step: Int): Int {
        if (step !in 1..maxStep) return 0
        return if (_state.value.isBonusRound) stepPoints[step] else normalStepPoints[step]
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private fun generateSymbols(): List<SonnenradSymbol> {
        val total = symbolWeights.sum()
        return List(3) {
            val roll = (0 until total).random()
            var cumul = 0
            SonnenradSymbol.entries.first { sym ->
                cumul += symbolWeights[sym.ordinal]
                roll < cumul
            }
        }
    }

    private fun launchTimingMarker() {
        timingJob?.cancel()
        timingJob = viewModelScope.launch {
            var inTarget = false
            while (true) {
                inTarget = !inTarget
                _state.update {
                    val active = it.ladderState as? SonnenradLadderState.Active
                        ?: return@update it
                    it.copy(ladderState = active.copy(isTargetZone = inTarget))
                }
                delay(markerTickMs)
            }
        }
    }

    private fun finishRound(step: Int) {
        timingJob?.cancel()
        val wasBonus = _state.value.isBonusRound
        val points   = if (step in 1..maxStep) {
            if (wasBonus) stepPoints[step] else normalStepPoints[step]
        } else 0
        val newLifetime = _state.value.lifetimePoints + points
        prefs.edit().apply {
            if (wasBonus) putLong(KEY_LAST_CLAIMED, System.currentTimeMillis())
            putInt(KEY_LIFETIME_POINTS, newLifetime)
        }.apply()
        _state.update { it.copy(
            phase       = SonnenradPhase.FINISHED,
            ladderState = SonnenradLadderState.Finished(finalStep = step, pointsAwarded = points),
            lifetimePoints = newLifetime,
        )}
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (true) {
                val ms = msUntilNextBonus()
                if (ms <= 0L) {
                    _state.update { it.copy(isBonusRound = true, nextBonusMs = 0L) }
                    break
                }
                _state.update { it.copy(nextBonusMs = ms) }
                delay(1_000L)
            }
        }
    }

    private fun computeEntryStep(symbols: List<SonnenradSymbol>): Int {
        val counts = symbols.groupingBy { it }.eachCount()
        return when {
            counts[SonnenradSymbol.SONNENSCHIRM] == 3 -> 4
            counts.values.any { it == 3 }             -> 2
            counts.values.any { it >= 2 }             -> 1
            else                                      -> 0
        }
    }

    private fun isBonusAvailable(lastClaimedMs: Long): Boolean {
        if (lastClaimedMs == 0L) return true
        val now  = Calendar.getInstance()
        val last = Calendar.getInstance().apply { timeInMillis = lastClaimedMs }
        return now.get(Calendar.YEAR)         != last.get(Calendar.YEAR) ||
               now.get(Calendar.DAY_OF_YEAR) != last.get(Calendar.DAY_OF_YEAR)
    }

    private fun msUntilNextBonus(): Long {
        val nextMidnight = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return (nextMidnight.timeInMillis - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    override fun onCleared() {
        super.onCleared()
        timingJob?.cancel()
        countdownJob?.cancel()
    }

    companion object {
        private const val KEY_LAST_CLAIMED    = "last_claimed"
        private const val KEY_LIFETIME_POINTS = "lifetime_points"
    }
}
