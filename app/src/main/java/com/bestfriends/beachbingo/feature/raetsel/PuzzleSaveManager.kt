package com.bestfriends.beachbingo.feature.raetsel

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class PuzzleSave(
    val id: String,
    val gameType: String,   // "strandoku" | "wellensumme" | "kuestenkrieg" | "duenenschatten" | "inselbruecke"
    val variant: String,
    val difficulty: String, // "leicht" | "mittel" | "schwer" | "experte"
    val seed: Long,
    val puzzleState: String, // JSON-stringified game-specific state
    val startedAt: Long,     // ms timestamp
    val elapsedSeconds: Int,
)

object PuzzleSaveManager {

    private const val PREFS_NAME = "beachbande_puzzle_saves"
    private const val KEY_SAVES = "saves"
    private const val PREFS_BEST = "beachbande_puzzle_best"

    fun getSaves(context: Context): List<PuzzleSave> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_SAVES, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                PuzzleSave(
                    id            = obj.getString("id"),
                    gameType      = obj.getString("gameType"),
                    variant       = obj.getString("variant"),
                    difficulty    = obj.getString("difficulty"),
                    seed          = obj.getLong("seed"),
                    puzzleState   = obj.getString("puzzleState"),
                    startedAt     = obj.getLong("startedAt"),
                    elapsedSeconds= obj.getInt("elapsedSeconds"),
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun savePuzzle(context: Context, save: PuzzleSave) {
        val saves = getSaves(context).filter { it.id != save.id }.toMutableList()
        saves.add(0, save) // newest first
        storeSaves(context, saves)
    }

    fun deleteSave(context: Context, id: String) {
        storeSaves(context, getSaves(context).filter { it.id != id })
    }

    fun generateId(): String =
        System.currentTimeMillis().toString(36) + (Math.random() * 1_000_000).toLong().toString(36)

    fun getBestTimeAny(context: Context, gameTypePrefix: String, difficulty: String): Int? {
        val prefs = context.getSharedPreferences(PREFS_BEST, Context.MODE_PRIVATE)
        val suffix = "_$difficulty"
        val matches = prefs.all.entries
            .filter { (k, _) -> k.startsWith(gameTypePrefix) && k.endsWith(suffix) }
            .mapNotNull { (_, v) -> v as? Int }
        return if (matches.isEmpty()) null else matches.min()
    }

    fun getBestTime(context: Context, gameType: String, variant: String, difficulty: String): Int? {
        val prefs = context.getSharedPreferences(PREFS_BEST, Context.MODE_PRIVATE)
        val key = "${gameType}_${variant}_${difficulty}"
        val v = prefs.getInt(key, -1)
        return if (v == -1) null else v
    }

    fun recordBestTime(context: Context, gameType: String, variant: String, difficulty: String, seconds: Int) {
        val prefs = context.getSharedPreferences(PREFS_BEST, Context.MODE_PRIVATE)
        val key = "${gameType}_${variant}_${difficulty}"
        val current = prefs.getInt(key, -1)
        if (current == -1 || seconds < current) {
            prefs.edit().putInt(key, seconds).apply()
        }
    }

    private fun storeSaves(context: Context, saves: List<PuzzleSave>) {
        val arr = JSONArray()
        saves.forEach { s ->
            arr.put(JSONObject().apply {
                put("id",             s.id)
                put("gameType",       s.gameType)
                put("variant",        s.variant)
                put("difficulty",     s.difficulty)
                put("seed",           s.seed)
                put("puzzleState",    s.puzzleState)
                put("startedAt",      s.startedAt)
                put("elapsedSeconds", s.elapsedSeconds)
            })
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_SAVES, arr.toString()).apply()
    }

    fun formatElapsed(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return if (m > 0) "${m}min ${s.toString().padStart(2, '0')}s" else "${s}s"
    }
}

val PUZZLE_GAME_INFO = mapOf(
    "strandoku"      to Triple("Strandoku",     "🔢", 0xFF38BDF8L),
    "wellensumme"    to Triple("WellenSumme",   "➕", 0xFFC084FCL),
    "kuestenkrieg"   to Triple("Küstenkrieg",   "⚓", 0xFFFB7185L),
    "duenenschatten" to Triple("DünenSchatten", "◼",  0xFFFBBF24L),
    "inselbruecke"   to Triple("Inselbrücke",   "🌉", 0xFF4ADE80L),
)

val PUZZLE_DIFFICULTY_LABELS = mapOf(
    "leicht"  to "Leicht",
    "mittel"  to "Mittel",
    "schwer"  to "Schwer",
    "experte" to "Experte",
)
