package com.bestfriends.beachbingo.feature.raetsel

import org.json.JSONArray
import org.json.JSONObject

val KRIEG_GRID_SIZES = mapOf("leicht" to 8, "mittel" to 10, "schwer" to 10, "experte" to 12)
val KRIEG_FLEET = mapOf(
    "leicht"  to listOf(3, 2, 2, 1, 1, 1),
    "mittel"  to listOf(4, 3, 3, 2, 2, 2, 1, 1, 1, 1),
    "schwer"  to listOf(4, 3, 3, 2, 2, 2, 1, 1, 1, 1),
    "experte" to listOf(4, 4, 3, 3, 2, 2, 2, 1, 1, 1, 1, 1),
)

enum class ShipMark { UNKNOWN, SHIP, WATER }

data class BattleshipPuzzle(
    val size: Int,
    val solution: Array<BooleanArray>,
    val rowClues: IntArray,
    val colClues: IntArray,
    val givenShip: Array<BooleanArray>,
    val givenWater: Array<BooleanArray>,
)

data class BattleshipState(
    val puzzle: BattleshipPuzzle,
    val marks: Array<Array<ShipMark>>,
    val solved: Boolean = false,
)

// ── Generation ────────────────────────────────────────────────────────────────

private fun canPlaceShip(grid: Array<BooleanArray>, size: Int, r: Int, c: Int, len: Int, horiz: Boolean): Boolean {
    for (i in 0 until len) {
        val nr = r + if (horiz) 0 else i
        val nc = c + if (horiz) i else 0
        if (nr !in 0 until size || nc !in 0 until size) return false
        for (dr in -1..1) for (dc in -1..1) {
            val tr = nr+dr; val tc = nc+dc
            if (tr in 0 until size && tc in 0 until size && grid[tr][tc]) return false
        }
    }
    return true
}

private fun placeShip(grid: Array<BooleanArray>, r: Int, c: Int, len: Int, horiz: Boolean) {
    for (i in 0 until len) {
        grid[r + if (horiz) 0 else i][c + if (horiz) i else 0] = true
    }
}

private fun placeFleet(size: Int, fleet: List<Int>, rng: Mulberry32): Array<BooleanArray>? {
    val grid = Array(size) { BooleanArray(size) }
    for (shipLen in fleet) {
        var placed = false
        val positions = (0 until size * size * 2).map {
            Triple(rng.nextInt(size), rng.nextInt(size), rng.next() < 0.5)
        }.shuffledWith(rng)
        for ((r, c, horiz) in positions) {
            if (canPlaceShip(grid, size, r, c, shipLen, horiz)) {
                placeShip(grid, r, c, shipLen, horiz)
                placed = true; break
            }
        }
        if (!placed) return null
    }
    return grid
}

fun generateBattleship(difficulty: String, seed: Int): BattleshipPuzzle {
    val size = KRIEG_GRID_SIZES[difficulty] ?: 10
    val fleet = (KRIEG_FLEET[difficulty] ?: listOf(4,3,3,2,2,2,1,1,1,1)).sortedDescending()
    val rng = Mulberry32(seed)

    var solution: Array<BooleanArray>? = null
    for (attempt in 0 until 50) {
        solution = placeFleet(size, fleet, rng)
        if (solution != null) break
    }
    if (solution == null) return generateBattleship(difficulty, seed + 9999)

    val sol = solution
    val rowClues = IntArray(size) { r -> sol[r].count { it } }
    val colClues = IntArray(size) { c -> (0 until size).count { r -> sol[r][c] } }

    val givenShip = Array(size) { BooleanArray(size) }
    val givenWater = Array(size) { BooleanArray(size) }

    val hintFraction = when (difficulty) {
        "leicht" -> 0.3; "mittel" -> 0.15; "schwer" -> 0.08; else -> 0.04
    }
    val shipCells = (0 until size).flatMap { r -> (0 until size).filter { c -> sol[r][c] }.map { c -> r to c } }
    val hintCount = (shipCells.size * hintFraction).toInt()
    shipCells.shuffledWith(rng).take(hintCount).forEach { (r, c) -> givenShip[r][c] = true }

    for (r in 0 until size) if (rowClues[r] == 0) for (c in 0 until size) givenWater[r][c] = true
    for (c in 0 until size) if (colClues[c] == 0) for (r in 0 until size) givenWater[r][c] = true

    return BattleshipPuzzle(size, sol, rowClues, colClues, givenShip, givenWater)
}

// ── State management ──────────────────────────────────────────────────────────

fun createBattleshipState(puzzle: BattleshipPuzzle): BattleshipState {
    val marks = Array(puzzle.size) { r ->
        Array(puzzle.size) { c ->
            when {
                puzzle.givenShip[r][c] -> ShipMark.SHIP
                puzzle.givenWater[r][c] -> ShipMark.WATER
                else -> ShipMark.UNKNOWN
            }
        }
    }
    return BattleshipState(puzzle, marks)
}

fun setKriegMark(state: BattleshipState, r: Int, c: Int, mark: ShipMark): BattleshipState {
    if (state.puzzle.givenShip[r][c] || state.puzzle.givenWater[r][c]) return state
    val marks = state.marks.map { row -> row.clone() }.toTypedArray()
    marks[r][c] = if (marks[r][c] == mark) ShipMark.UNKNOWN else mark
    val solved = checkBattleshipSolved(marks, state.puzzle)
    return state.copy(marks = marks, solved = solved)
}

private fun checkBattleshipSolved(marks: Array<Array<ShipMark>>, puzzle: BattleshipPuzzle): Boolean {
    for (r in 0 until puzzle.size) for (c in 0 until puzzle.size) {
        if ((marks[r][c] == ShipMark.SHIP) != puzzle.solution[r][c]) return false
    }
    return true
}

data class KriegErrors(val rows: BooleanArray, val cols: BooleanArray)

fun computeKriegErrors(state: BattleshipState): KriegErrors {
    val size = state.puzzle.size
    val rowShip = IntArray(size) { r -> state.marks[r].count { it == ShipMark.SHIP } }
    val colShip = IntArray(size) { c -> (0 until size).count { r -> state.marks[r][c] == ShipMark.SHIP } }
    return KriegErrors(
        BooleanArray(size) { r -> rowShip[r] > state.puzzle.rowClues[r] },
        BooleanArray(size) { c -> colShip[c] > state.puzzle.colClues[c] },
    )
}

fun getKriegHint(state: BattleshipState): Pair<Int,Int>? {
    val size = state.puzzle.size
    val wrong = mutableListOf<Pair<Int,Int>>()
    for (r in 0 until size) for (c in 0 until size) {
        if (state.puzzle.givenShip[r][c] || state.puzzle.givenWater[r][c]) continue
        if ((state.marks[r][c] == ShipMark.SHIP) != state.puzzle.solution[r][c]) wrong.add(r to c)
    }
    return if (wrong.isEmpty()) null else wrong.random()
}

// ── Serialization ─────────────────────────────────────────────────────────────

fun serializeBattleshipState(state: BattleshipState): String {
    val arr = JSONArray()
    for (row in state.marks) {
        val r = JSONArray(); for (m in row) r.put(m.ordinal); arr.put(r)
    }
    return JSONObject().put("marks", arr).toString()
}

fun deserializeBattleshipState(puzzle: BattleshipPuzzle, raw: String): BattleshipState {
    return try {
        val obj = JSONObject(raw)
        val arr = obj.getJSONArray("marks")
        val marks = Array(puzzle.size) { r ->
            Array(puzzle.size) { c -> ShipMark.entries[arr.getJSONArray(r).getInt(c)] }
        }
        val solved = checkBattleshipSolved(marks, puzzle)
        BattleshipState(puzzle, marks, solved)
    } catch (_: Exception) {
        createBattleshipState(puzzle)
    }
}
