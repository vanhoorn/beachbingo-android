package com.bestfriends.beachbingo.feature.raetsel

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.min

// ── Typen ─────────────────────────────────────────────────────────────────────

enum class WwLetterStatus { CORRECT, PRESENT, ABSENT, EMPTY, TYPING }

data class WwDifficultyConfig(
    val wordLength: Int,
    val maxGuesses: Int,
    val hardMode: Boolean,
    val label: String,
    val description: String,
)

val WW_CONFIG: Map<String, WwDifficultyConfig> = mapOf(
    "leicht"  to WwDifficultyConfig(4, 7, false, "Leicht",  "4 Buchstaben · 7 Versuche"),
    "mittel"  to WwDifficultyConfig(5, 6, false, "Mittel",  "5 Buchstaben · 6 Versuche"),
    "schwer"  to WwDifficultyConfig(5, 5, false, "Schwer",  "5 Buchstaben · 5 Versuche"),
    "experte" to WwDifficultyConfig(6, 5, true,  "Experte", "6 Buchstaben · 5 Versuche · Hard Mode"),
)

val WW_DIFFICULTIES = listOf("leicht", "mittel", "schwer", "experte")

data class WwStats(
    val played: Int = 0,
    val won: Int = 0,
    val currentStreak: Int = 0,
    val maxStreak: Int = 0,
    val distribution: MutableList<Int> = mutableListOf(),
    val lastPlayedDate: String = "",
    val lastDailyDate: String = "",
    val dailyPlayed: Int = 0,
    val dailyWon: Int = 0,
    val dailyCurrentStreak: Int = 0,
    val dailyMaxStreak: Int = 0,
    val dailyDistribution: MutableList<Int> = mutableListOf(),
)

data class WwInitState(
    val targetWord: String,
    val guesses: List<String>,
    val currentInput: String,
    val gameStatus: String,
    val elapsedSeconds: Int,
)

// ── Hilfsfunktion: Uppercase im Spielformat (ß bleibt ß, nicht SS) ─────────────

internal fun wwToUpper(s: String) = buildString {
    for (c in s) when (c) {
        'ä' -> append('Ä')
        'ö' -> append('Ö')
        'ü' -> append('Ü')
        'ß' -> append('ß')   // ß ist kein Großbuchstabe → bleibt ß
        else -> append(c.uppercaseChar())
    }
}

// ── Wortlisten-Singleton ───────────────────────────────────────────────────────
// Lädt 6 Asset-Dateien einmalig; danach nullkosten-Zugriff.
// Quellen: enz/german-wordlist (CC0) + caco3/wordle-de Targets 5 (MIT)

object WwWordBank {
    @Volatile private var initialized = false

    private var targets4: List<String> = emptyList()
    private var targets5: List<String> = emptyList()
    private var targets6: List<String> = emptyList()
    private var pool4: Set<String> = emptySet()
    private var pool5: Set<String> = emptySet()
    private var pool6: Set<String> = emptySet()

    val isReady: Boolean get() = initialized

    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        targets4 = loadLines(context, "wortwelle/targets_4.txt")
        targets5 = loadLines(context, "wortwelle/targets_5.txt")
        targets6 = loadLines(context, "wortwelle/targets_6.txt")
        // Pool = eigene Pool-Datei + Targets (Targets sind immer gültige Ratewörter)
        pool4 = (loadLines(context, "wortwelle/pool_4.txt") + targets4).toHashSet()
        pool5 = (loadLines(context, "wortwelle/pool_5.txt") + targets5).toHashSet()
        pool6 = (loadLines(context, "wortwelle/pool_6.txt") + targets6).toHashSet()
        initialized = true
    }

    private fun loadLines(context: Context, path: String): List<String> =
        context.assets.open(path).bufferedReader().readLines()
            .map { it.trim() }.filter { it.isNotEmpty() }

    fun getTargets(len: Int): List<String> = when (len) { 4 -> targets4; 5 -> targets5; else -> targets6 }
    fun getPool(len: Int): Set<String>     = when (len) { 4 -> pool4;    5 -> pool5;    else -> pool6    }
}

// ── Wortlisten-Zugriff ─────────────────────────────────────────────────────────

fun getWwTargets(difficulty: String): List<String> {
    val len = WW_CONFIG[difficulty]?.wordLength ?: 5
    return WwWordBank.getTargets(len)
}

// ── Kernlogik ─────────────────────────────────────────────────────────────────

fun getDailyWwWord(difficulty: String): Pair<String, String> {
    val dateStr = LocalDate.now().toString()
    var h = 0
    for (c in dateStr) h = h * 31 + c.code
    val offset = when (difficulty) { "leicht" -> 0; "mittel" -> 1000; "schwer" -> 2000; "experte" -> 3000; else -> 0 }
    val targets = getWwTargets(difficulty)
    val idx = (abs(h.toLong() + offset.toLong()) % targets.size).toInt()
    return Pair(targets[idx], dateStr)
}

fun getWwRandomWord(difficulty: String): String {
    val targets = getWwTargets(difficulty)
    return targets[(Math.random() * targets.size).toInt()]
}

fun isValidWwGuess(word: String, difficulty: String): Boolean {
    val len = WW_CONFIG[difficulty]?.wordLength ?: 5
    // wwToUpper statt .uppercase() damit ß nicht zu SS wird
    return wwToUpper(word) in WwWordBank.getPool(len)
}

fun computeWwStatuses(guess: String, target: String): List<WwLetterStatus> {
    val g = wwToUpper(guess)
    val t = wwToUpper(target)
    val result = MutableList(g.length) { WwLetterStatus.ABSENT }
    val remaining = mutableMapOf<Char, Int>()
    for (i in t.indices) {
        if (g.getOrNull(i) != t[i]) remaining[t[i]] = (remaining[t[i]] ?: 0) + 1
    }
    for (i in g.indices) {
        if (i < t.length && g[i] == t[i]) result[i] = WwLetterStatus.CORRECT
    }
    for (i in g.indices) {
        if (result[i] == WwLetterStatus.CORRECT) continue
        val ch = g[i]
        val rem = remaining[ch] ?: 0
        if (rem > 0) {
            result[i] = WwLetterStatus.PRESENT
            remaining[ch] = rem - 1
        }
    }
    return result
}

fun computeWwKeyStatuses(guesses: List<String>, target: String): Map<Char, WwLetterStatus> {
    val priority = mapOf(
        WwLetterStatus.CORRECT to 3, WwLetterStatus.PRESENT to 2,
        WwLetterStatus.ABSENT to 1, WwLetterStatus.EMPTY to 0, WwLetterStatus.TYPING to 0,
    )
    val result = mutableMapOf<Char, WwLetterStatus>()
    for (guess in guesses) {
        val statuses = computeWwStatuses(guess, target)
        for (i in guess.indices) {
            val ch = guess[i]
            val st = statuses[i]
            val cur = result[ch]
            if (cur == null || (priority[st] ?: 0) > (priority[cur] ?: 0)) result[ch] = st
        }
    }
    return result
}

fun validateWwHardMode(newGuess: String, previousGuesses: List<String>, target: String): String? {
    if (previousGuesses.isEmpty()) return null
    val g = wwToUpper(newGuess)
    for (prev in previousGuesses) {
        val statuses = computeWwStatuses(prev, target)
        for (i in prev.indices) {
            if (statuses[i] == WwLetterStatus.CORRECT && g.getOrNull(i) != prev[i]) {
                return "Position ${i + 1} muss \"${prev[i]}\" sein (gruener Buchstabe)."
            }
        }
        for (i in prev.indices) {
            if (statuses[i] == WwLetterStatus.PRESENT && !g.contains(prev[i])) {
                return "Das Wort muss den Buchstaben \"${prev[i]}\" enthalten."
            }
        }
    }
    return null
}

// ── State-Serialisierung ───────────────────────────────────────────────────────

fun serializeWwState(targetWord: String, guesses: List<String>, currentInput: String, gameStatus: String): String =
    JSONObject().apply {
        put("targetWord", targetWord)
        put("guesses", JSONArray(guesses))
        put("currentInput", currentInput)
        put("gameStatus", gameStatus)
    }.toString()

fun deserializeWwState(raw: String): WwInitState = try {
    val obj = JSONObject(raw)
    val arr = obj.getJSONArray("guesses")
    WwInitState(
        targetWord    = obj.getString("targetWord"),
        guesses       = (0 until arr.length()).map { arr.getString(it) },
        currentInput  = obj.optString("currentInput", ""),
        gameStatus    = obj.optString("gameStatus", "playing"),
        elapsedSeconds = 0,
    )
} catch (_: Exception) {
    WwInitState("", emptyList(), "", "playing", 0)
}

// ── Statistiken ────────────────────────────────────────────────────────────────

private const val WW_STATS_PREFS = "wortwelle_stats"

private fun makeEmptyWwStats(maxGuesses: Int) = WwStats(
    distribution = MutableList(maxGuesses) { 0 },
    dailyDistribution = MutableList(maxGuesses) { 0 },
)

fun getWwStats(context: Context, difficulty: String): WwStats {
    val maxGuesses = WW_CONFIG[difficulty]?.maxGuesses ?: 6
    val prefs = context.getSharedPreferences(WW_STATS_PREFS, Context.MODE_PRIVATE)
    val raw = prefs.getString(difficulty, null) ?: return makeEmptyWwStats(maxGuesses)
    return try {
        val obj = JSONObject(raw)
        val dist = obj.getJSONArray("distribution")
        val ddist = obj.getJSONArray("dailyDistribution")
        WwStats(
            played              = obj.optInt("played"),
            won                 = obj.optInt("won"),
            currentStreak       = obj.optInt("currentStreak"),
            maxStreak           = obj.optInt("maxStreak"),
            distribution        = MutableList(maxGuesses) { i -> if (i < dist.length()) dist.getInt(i) else 0 },
            lastPlayedDate      = obj.optString("lastPlayedDate", ""),
            lastDailyDate       = obj.optString("lastDailyDate", ""),
            dailyPlayed         = obj.optInt("dailyPlayed"),
            dailyWon            = obj.optInt("dailyWon"),
            dailyCurrentStreak  = obj.optInt("dailyCurrentStreak"),
            dailyMaxStreak      = obj.optInt("dailyMaxStreak"),
            dailyDistribution   = MutableList(maxGuesses) { i -> if (i < ddist.length()) ddist.getInt(i) else 0 },
        )
    } catch (_: Exception) {
        makeEmptyWwStats(maxGuesses)
    }
}

private fun saveWwStats(context: Context, difficulty: String, stats: WwStats) {
    val prefs = context.getSharedPreferences(WW_STATS_PREFS, Context.MODE_PRIVATE)
    val obj = JSONObject().apply {
        put("played", stats.played)
        put("won", stats.won)
        put("currentStreak", stats.currentStreak)
        put("maxStreak", stats.maxStreak)
        put("distribution", JSONArray(stats.distribution))
        put("lastPlayedDate", stats.lastPlayedDate)
        put("lastDailyDate", stats.lastDailyDate)
        put("dailyPlayed", stats.dailyPlayed)
        put("dailyWon", stats.dailyWon)
        put("dailyCurrentStreak", stats.dailyCurrentStreak)
        put("dailyMaxStreak", stats.dailyMaxStreak)
        put("dailyDistribution", JSONArray(stats.dailyDistribution))
    }
    prefs.edit().putString(difficulty, obj.toString()).apply()
}

fun recordWwResult(context: Context, difficulty: String, won: Boolean, guessCount: Int, isDaily: Boolean, dateStr: String?) {
    var s = getWwStats(context, difficulty)
    val today = dateStr ?: LocalDate.now().toString()
    val yesterday = LocalDate.now().minusDays(1).toString()

    var newStreak = s.currentStreak
    var newMax = s.maxStreak
    val newDist = s.distribution.toMutableList()
    if (won) {
        val idx = min(guessCount - 1, newDist.size - 1)
        if (idx >= 0) newDist[idx]++
        if (s.lastPlayedDate != today) {
            newStreak = if (s.lastPlayedDate == yesterday) s.currentStreak + 1 else 1
            newMax = maxOf(newMax, newStreak)
        }
    } else {
        newStreak = 0
    }
    s = s.copy(
        played = s.played + 1, won = if (won) s.won + 1 else s.won,
        currentStreak = newStreak, maxStreak = newMax,
        distribution = newDist, lastPlayedDate = today,
    )

    if (isDaily && dateStr != null) {
        var dStreak = s.dailyCurrentStreak
        var dMax = s.dailyMaxStreak
        val dDist = s.dailyDistribution.toMutableList()
        if (won) {
            val idx = min(guessCount - 1, dDist.size - 1)
            if (idx >= 0) dDist[idx]++
            if (s.lastDailyDate != dateStr) {
                dStreak = if (s.lastDailyDate == yesterday) s.dailyCurrentStreak + 1 else 1
                dMax = maxOf(dMax, dStreak)
            }
        } else {
            dStreak = 0
        }
        s = s.copy(
            dailyPlayed = s.dailyPlayed + 1, dailyWon = if (won) s.dailyWon + 1 else s.dailyWon,
            dailyCurrentStreak = dStreak, dailyMaxStreak = dMax,
            dailyDistribution = dDist, lastDailyDate = dateStr,
        )
    }
    saveWwStats(context, difficulty, s)
}

fun hasDailyWwBeenPlayed(context: Context, difficulty: String, dateStr: String): Boolean =
    getWwStats(context, difficulty).lastDailyDate == dateStr
