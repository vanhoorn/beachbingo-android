package com.bestfriends.beachbingo.feature.mahjong

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs

// ── Seeded RNG (mulberry32 — mirrors TypeScript exactly) ─────────────────────
class Mulberry32(private var seed: Int) {
    private fun imul(a: Int, b: Int): Int = (a.toLong() * b.toLong()).toInt()
    fun next(): Double {
        seed = (seed + 0x6d2b79f5.toInt())
        var t: Int = imul(seed xor (seed ushr 15), 1 or seed)
        t = (t + imul(t xor (t ushr 7), 61 or t)) xor t
        return ((t xor (t ushr 14)).toLong() and 0xFFFFFFFFL).toDouble() / 4294967296.0
    }
    fun nextInt(n: Int) = (next() * n).toInt()
}

private fun <T> MutableList<T>.shuffleWith(rng: Mulberry32): MutableList<T> {
    for (i in size - 1 downTo 1) {
        val j = rng.nextInt(i + 1)
        val tmp = this[i]; this[i] = this[j]; this[j] = tmp
    }
    return this
}

// ── Types ─────────────────────────────────────────────────────────────────────

enum class MahjongDifficulty { ROOKIE, SNIPER, BOSS }

data class MahjongTile(
    val id: Int,
    val typeId: String,
    val col: Int,
    val row: Int,
    val layer: Int,
    val removed: Boolean = false,
)

data class MahjongState(
    val layoutId: LayoutId,
    val difficulty: MahjongDifficulty,
    val seed: Int,
    val tiles: List<MahjongTile>,
    val selectedId: Int? = null,
    val hintsUsed: Int = 0,
    val shufflesUsed: Int = 0,
    val history: List<Pair<Int, Int>> = emptyList(),
    val won: Boolean = false,
    val gameOver: Boolean = false,
)

// ── Board generation ──────────────────────────────────────────────────────────

fun generateBoard(layoutId: LayoutId, seed: Int): List<MahjongTile> {
    val layout = LAYOUT_DEFS[layoutId]!!
    val rng = Mulberry32(seed)
    val positions = layout.positions.take(layout.tileCount)
    val typeIds = buildTypeIds(positions.size, rng)
    // take(typeIds.size) guards against odd tileCount edge-cases
    return positions.take(typeIds.size).mapIndexed { i, (col, row, layer) ->
        MahjongTile(id = i, typeId = typeIds[i], col = col, row = row, layer = layer)
    }
}

private fun buildTypeIds(count: Int, rng: Mulberry32): List<String> {
    val n = if (count % 2 == 0) count else count - 1
    val deck = buildDeck().toMutableList().shuffleWith(rng)
    val result = mutableListOf<String>()
    var i = 0
    while (result.size < n && i + 1 < deck.size) {
        result.add(deck[i]); result.add(deck[i + 1])
        i += 2
    }
    while (result.size < n && result.size >= 2) {
        result.add(result[result.size - 2]); result.add(result[result.size - 1])
    }
    return result.take(n).toMutableList().shuffleWith(rng)
}

// ── Free tile check ───────────────────────────────────────────────────────────

fun isFree(tile: MahjongTile, tiles: List<MahjongTile>): Boolean {
    if (tile.removed) return false
    val active = tiles.filter { !it.removed && it.id != tile.id }

    val coveredAbove = active.any { t ->
        t.layer == tile.layer + 1 &&
        t.col >= tile.col - 1 && t.col <= tile.col + 1 &&
        t.row >= tile.row - 1 && t.row <= tile.row + 1
    }
    if (coveredAbove) return false

    val blockedLeft  = active.any { t -> t.layer == tile.layer && t.col == tile.col - 2 && abs(t.row - tile.row) < 2 }
    val blockedRight = active.any { t -> t.layer == tile.layer && t.col == tile.col + 2 && abs(t.row - tile.row) < 2 }
    return !(blockedLeft && blockedRight)
}

// ── Find all matching free pairs ──────────────────────────────────────────────

fun findFreePairs(tiles: List<MahjongTile>): List<Pair<MahjongTile, MahjongTile>> {
    val free = tiles.filter { !it.removed && isFree(it, tiles) }
    val pairs = mutableListOf<Pair<MahjongTile, MahjongTile>>()
    for (i in free.indices) for (j in i + 1 until free.size)
        if (tilesMatch(free[i].typeId, free[j].typeId)) pairs += free[i] to free[j]
    return pairs
}

fun hasAnyMoves(tiles: List<MahjongTile>) = findFreePairs(tiles).isNotEmpty()

fun getHint(tiles: List<MahjongTile>): Pair<MahjongTile, MahjongTile>? = findFreePairs(tiles).firstOrNull()

// ── Shuffle ───────────────────────────────────────────────────────────────────

fun shuffleTiles(tiles: List<MahjongTile>, extraSeed: Int): List<MahjongTile> {
    val rng = Mulberry32(extraSeed)
    val active = tiles.filter { !it.removed }
    val typeIds = active.map { it.typeId }.toMutableList().shuffleWith(rng)
    var idx = 0
    return tiles.map { t -> if (t.removed) t else t.copy(typeId = typeIds[idx++]) }
}

// ── Remove pair ───────────────────────────────────────────────────────────────

fun removePair(state: MahjongState, idA: Int, idB: Int): MahjongState {
    val tiles = state.tiles.map { if (it.id == idA || it.id == idB) it.copy(removed = true) else it }
    val remaining = tiles.count { !it.removed }
    return state.copy(
        tiles = tiles,
        selectedId = null,
        history = state.history + (idA to idB),
        won = remaining == 0,
        gameOver = remaining > 0 && !hasAnyMoves(tiles),
    )
}

// ── Undo ──────────────────────────────────────────────────────────────────────

fun undoLast(state: MahjongState): MahjongState {
    if (state.history.isEmpty()) return state
    val history = state.history.dropLast(1)
    val last = state.history.last()
    val tiles = state.tiles.map { if (it.id == last.first || it.id == last.second) it.copy(removed = false) else it }
    return state.copy(tiles = tiles, history = history, selectedId = null, won = false, gameOver = false)
}

// ── Click ─────────────────────────────────────────────────────────────────────

fun handleTileClick(state: MahjongState, tileId: Int): MahjongState {
    val tile = state.tiles.find { it.id == tileId } ?: return state
    if (tile.removed || !isFree(tile, state.tiles)) return state
    if (state.selectedId == null) return state.copy(selectedId = tileId)
    if (state.selectedId == tileId) return state.copy(selectedId = null)
    val selected = state.tiles.find { it.id == state.selectedId } ?: return state.copy(selectedId = tileId)
    return if (tilesMatch(selected.typeId, tile.typeId))
        removePair(state, state.selectedId!!, tileId)
    else
        state.copy(selectedId = tileId)
}

// ── Create ────────────────────────────────────────────────────────────────────

fun createMahjongState(layoutId: LayoutId, difficulty: MahjongDifficulty, seed: Int) = MahjongState(
    layoutId = layoutId,
    difficulty = difficulty,
    seed = seed,
    tiles = generateBoard(layoutId, seed),
)

// ── Serialize / Deserialize ───────────────────────────────────────────────────

fun serializeMahjong(state: MahjongState): String = JSONObject().apply {
    put("layoutId", state.layoutId.name)
    put("difficulty", state.difficulty.name)
    put("seed", state.seed)
    put("removedIds", JSONArray(state.tiles.filter { it.removed }.map { it.id }))
    put("typeIds", JSONArray(state.tiles.map { it.typeId }))
    put("hintsUsed", state.hintsUsed)
    put("shufflesUsed", state.shufflesUsed)
}.toString()

fun deserializeMahjong(raw: String): MahjongState {
    val obj = JSONObject(raw)
    val layoutId   = LayoutId.valueOf(obj.getString("layoutId"))
    val difficulty = MahjongDifficulty.valueOf(obj.getString("difficulty"))
    val seed       = obj.getInt("seed")
    val removedArr = obj.getJSONArray("removedIds")
    val typeArr    = obj.getJSONArray("typeIds")
    val removedIds = (0 until removedArr.length()).map { removedArr.getInt(it) }.toSet()
    val typeIds    = (0 until typeArr.length()).map { typeArr.getString(it) }

    val baseTiles = generateBoard(layoutId, seed)
    val tiles = baseTiles.mapIndexed { i, t ->
        t.copy(typeId = typeIds.getOrElse(i) { t.typeId }, removed = t.id in removedIds)
    }
    return MahjongState(
        layoutId = layoutId, difficulty = difficulty, seed = seed,
        tiles = tiles,
        hintsUsed    = obj.optInt("hintsUsed", 0),
        shufflesUsed = obj.optInt("shufflesUsed", 0),
        gameOver = !hasAnyMoves(tiles),
    )
}

// ── Difficulty limits ─────────────────────────────────────────────────────────

val HINT_LIMIT = mapOf(MahjongDifficulty.ROOKIE to Int.MAX_VALUE, MahjongDifficulty.SNIPER to 3, MahjongDifficulty.BOSS to 0)
val SHUFFLE_LIMIT = mapOf(MahjongDifficulty.ROOKIE to Int.MAX_VALUE, MahjongDifficulty.SNIPER to 1, MahjongDifficulty.BOSS to 0)
val SHOW_FREE_HIGHLIGHT = mapOf(MahjongDifficulty.ROOKIE to true, MahjongDifficulty.SNIPER to false, MahjongDifficulty.BOSS to false)
