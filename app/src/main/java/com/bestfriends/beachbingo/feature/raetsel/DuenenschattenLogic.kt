package com.bestfriends.beachbingo.feature.raetsel

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs

// ── Seeded RNG (mulberry32) ───────────────────────────────────────────────────
class Mulberry32(private var seed: Int) {
    fun next(): Double {
        seed = (seed + 0x6d2b79f5)
        var t = (seed xor (seed ushr 15)).toLong() * (1 or seed).toLong()
        t = t and 0xFFFFFFFFL
        t = (t + ((t xor (t ushr 7)) * (61 or t.toInt()).toLong())) xor t
        t = t and 0xFFFFFFFFL
        return ((t xor (t ushr 14)) and 0xFFFFFFFFL).toDouble() / 4294967296.0
    }
    fun nextInt(n: Int) = (next() * n).toInt()
}

fun <T> List<T>.shuffledWith(rng: Mulberry32): List<T> {
    val a = this.toMutableList()
    for (i in a.size - 1 downTo 1) {
        val j = rng.nextInt(i + 1)
        val tmp = a[i]; a[i] = a[j]; a[j] = tmp
    }
    return a
}

// ── Types ─────────────────────────────────────────────────────────────────────
enum class CellMark { WHITE, BLACK, DOT }

val HITORI_SIZES = mapOf("leicht" to 5, "mittel" to 7, "schwer" to 9, "experte" to 11)

data class HitoriPuzzle(
    val size: Int,
    val grid: Array<IntArray>,       // numbers shown to player
    val solution: Array<BooleanArray>, // true = black cell in solution
)

data class HitoriState(
    val puzzle: HitoriPuzzle,
    val marks: Array<Array<CellMark>>,
    val solved: Boolean = false,
)

// ── Generation ────────────────────────────────────────────────────────────────

private fun hasAdjacentBlack(black: Array<BooleanArray>, r: Int, c: Int, size: Int): Boolean {
    if (r > 0 && black[r-1][c]) return true
    if (r < size-1 && black[r+1][c]) return true
    if (c > 0 && black[r][c-1]) return true
    if (c < size-1 && black[r][c+1]) return true
    return false
}

private fun isWhiteConnected(black: Array<BooleanArray>, size: Int): Boolean {
    val visited = Array(size) { BooleanArray(size) }
    var startR = -1; var startC = -1
    outer@ for (r in 0 until size) for (c in 0 until size) {
        if (!black[r][c]) { startR = r; startC = c; break@outer }
    }
    if (startR == -1) return false
    val queue = ArrayDeque<Pair<Int,Int>>()
    queue.add(startR to startC); visited[startR][startC] = true
    val dirs = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
    while (queue.isNotEmpty()) {
        val (r, c) = queue.removeFirst()
        for ((dr, dc) in dirs) {
            val nr = r+dr; val nc = c+dc
            if (nr in 0 until size && nc in 0 until size && !black[nr][nc] && !visited[nr][nc]) {
                visited[nr][nc] = true; queue.add(nr to nc)
            }
        }
    }
    for (r in 0 until size) for (c in 0 until size) if (!black[r][c] && !visited[r][c]) return false
    return true
}

private fun generateBlackPattern(size: Int, rng: Mulberry32): Array<BooleanArray>? {
    val black = Array(size) { BooleanArray(size) }
    val cells = (0 until size * size).toMutableList().shuffledWith(rng)
    val targetBlack = (size * size * 0.25).toInt()
    var placed = 0
    for (idx in cells) {
        if (placed >= targetBlack) break
        val r = idx / size; val c = idx % size
        if (!hasAdjacentBlack(black, r, c, size)) {
            black[r][c] = true
            if (!isWhiteConnected(black, size)) { black[r][c] = false } else { placed++ }
        }
    }
    return if (placed > 0) black else null
}

private fun assignWhiteNumbers(black: Array<BooleanArray>, size: Int, rng: Mulberry32): Array<IntArray>? {
    val nums = Array(size) { IntArray(size) }
    val whiteCells = mutableListOf<Pair<Int,Int>>()
    for (r in 0 until size) for (c in 0 until size) if (!black[r][c]) whiteCells.add(r to c)

    fun bt(idx: Int): Boolean {
        if (idx == whiteCells.size) return true
        val (r, c) = whiteCells[idx]
        val rowUsed = (0 until size).filter { cc -> !black[r][cc] && nums[r][cc] != 0 }.map { cc -> nums[r][cc] }.toSet()
        val colUsed = (0 until size).filter { rr -> !black[rr][c] && nums[rr][c] != 0 }.map { rr -> nums[rr][c] }.toSet()
        val candidates = (1..size).filter { it !in rowUsed && it !in colUsed }.shuffledWith(rng)
        for (n in candidates) {
            nums[r][c] = n
            if (bt(idx + 1)) return true
            nums[r][c] = 0
        }
        return false
    }
    return if (bt(0)) nums else null
}

fun generateHitori(difficulty: String, seed: Int): HitoriPuzzle {
    val size = HITORI_SIZES[difficulty] ?: 7
    val rng = Mulberry32(seed)

    for (attempt in 0 until 20) {
        val black = generateBlackPattern(size, rng) ?: continue
        val whiteNums = assignWhiteNumbers(black, size, rng) ?: continue

        // Build puzzle grid: white cells get their number, black cells get a number
        // that creates a duplicate (for the puzzle to make sense)
        val grid = Array(size) { IntArray(size) }
        for (r in 0 until size) for (c in 0 until size) {
            if (!black[r][c]) { grid[r][c] = whiteNums[r][c] } else {
                // Pick a number from same row or col white cells (creates duplicate)
                val rowWhites = (0 until size).filter { cc -> !black[r][cc] }.map { cc -> whiteNums[r][cc] }
                val colWhites = (0 until size).filter { rr -> !black[rr][c] }.map { rr -> whiteNums[rr][c] }
                val pool = (rowWhites + colWhites).filter { it != 0 }
                grid[r][c] = if (pool.isNotEmpty()) pool[rng.nextInt(pool.size)] else (rng.nextInt(size) + 1)
            }
        }
        return HitoriPuzzle(size, grid, black)
    }
    return generateHitori(difficulty, seed + 7777)
}

// ── State management ──────────────────────────────────────────────────────────

fun createHitoriState(puzzle: HitoriPuzzle): HitoriState {
    val marks = Array(puzzle.size) { Array(puzzle.size) { CellMark.WHITE } }
    return HitoriState(puzzle, marks)
}

fun toggleMark(state: HitoriState, r: Int, c: Int): HitoriState {
    val marks = state.marks.map { row -> row.clone() }.toTypedArray()
    marks[r][c] = when (marks[r][c]) {
        CellMark.WHITE -> CellMark.BLACK
        CellMark.BLACK -> CellMark.DOT
        CellMark.DOT -> CellMark.WHITE
    }
    val solved = checkHitoriSolved(marks, state.puzzle)
    return state.copy(marks = marks, solved = solved)
}

fun setMark(state: HitoriState, r: Int, c: Int, mark: CellMark): HitoriState {
    val marks = state.marks.map { row -> row.clone() }.toTypedArray()
    marks[r][c] = mark
    val solved = checkHitoriSolved(marks, state.puzzle)
    return state.copy(marks = marks, solved = solved)
}

private fun checkHitoriSolved(marks: Array<Array<CellMark>>, puzzle: HitoriPuzzle): Boolean {
    val size = puzzle.size
    for (r in 0 until size) for (c in 0 until size) {
        val shouldBeBlack = puzzle.solution[r][c]
        val isBlack = marks[r][c] == CellMark.BLACK
        if (shouldBeBlack != isBlack) return false
    }
    return true
}

data class HitoriConflicts(val adjacentBlacks: Set<Pair<Int,Int>>, val duplicateWhites: Set<Pair<Int,Int>>)

fun computeConflicts(state: HitoriState): HitoriConflicts {
    val size = state.puzzle.size
    val adjBlacks = mutableSetOf<Pair<Int,Int>>()
    val dupWhites = mutableSetOf<Pair<Int,Int>>()

    // Adjacent blacks
    for (r in 0 until size) for (c in 0 until size) {
        if (state.marks[r][c] == CellMark.BLACK) {
            if (r > 0 && state.marks[r-1][c] == CellMark.BLACK) { adjBlacks.add(r to c); adjBlacks.add(r-1 to c) }
            if (c > 0 && state.marks[r][c-1] == CellMark.BLACK) { adjBlacks.add(r to c); adjBlacks.add(r to c-1) }
        }
    }
    // Duplicate white numbers per row/col
    for (r in 0 until size) {
        val seen = mutableMapOf<Int,MutableList<Int>>()
        for (c in 0 until size) {
            if (state.marks[r][c] == CellMark.WHITE) {
                val n = state.puzzle.grid[r][c]
                seen.getOrPut(n) { mutableListOf() }.add(c)
            }
        }
        seen.values.filter { it.size > 1 }.flatten().forEach { c -> dupWhites.add(r to c) }
    }
    for (c in 0 until size) {
        val seen = mutableMapOf<Int,MutableList<Int>>()
        for (r in 0 until size) {
            if (state.marks[r][c] == CellMark.WHITE) {
                val n = state.puzzle.grid[r][c]
                seen.getOrPut(n) { mutableListOf() }.add(r)
            }
        }
        seen.values.filter { it.size > 1 }.flatten().forEach { r -> dupWhites.add(r to c) }
    }
    return HitoriConflicts(adjBlacks, dupWhites)
}

fun getHitoriHint(state: HitoriState): Pair<Int,Int>? {
    val size = state.puzzle.size
    val wrong = mutableListOf<Pair<Int,Int>>()
    for (r in 0 until size) for (c in 0 until size) {
        val shouldBeBlack = state.puzzle.solution[r][c]
        val isBlack = state.marks[r][c] == CellMark.BLACK
        if (shouldBeBlack != isBlack) wrong.add(r to c)
    }
    return if (wrong.isEmpty()) null else wrong.random()
}

// ── Serialization ─────────────────────────────────────────────────────────────

fun serializeHitoriState(state: HitoriState): String {
    val arr = JSONArray()
    for (row in state.marks) {
        val rowArr = JSONArray()
        for (mark in row) rowArr.put(mark.ordinal)
        arr.put(rowArr)
    }
    return JSONObject().put("marks", arr).toString()
}

fun deserializeHitoriState(puzzle: HitoriPuzzle, raw: String): HitoriState {
    return try {
        val obj = JSONObject(raw)
        val arr = obj.getJSONArray("marks")
        val marks = Array(puzzle.size) { r ->
            Array(puzzle.size) { c ->
                CellMark.entries[arr.getJSONArray(r).getInt(c)]
            }
        }
        val solved = checkHitoriSolved(marks, puzzle)
        HitoriState(puzzle, marks, solved)
    } catch (_: Exception) {
        createHitoriState(puzzle)
    }
}
