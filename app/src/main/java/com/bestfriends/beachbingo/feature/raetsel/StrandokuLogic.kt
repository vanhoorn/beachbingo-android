package com.bestfriends.beachbingo.feature.raetsel

import org.json.JSONArray
import org.json.JSONObject

// Strandoku: Classic 9×9 Sudoku only for Android Phase 1
// Variants: classic, mega12, mega16 (larger grids)
// (irregular/diagonal/killer/samurai added later if needed)

val STRANDOKU_VARIANT_LABELS = mapOf(
    "classic"  to "Classic 9×9",
    "mega12"   to "Mega 12×12",
    "mega16"   to "Mega 16×16",
)
val STRANDOKU_VARIANT_DESCRIPTIONS = mapOf(
    "classic" to "Standard Sudoku · Ziffern 1–9",
    "mega12"  to "Erweitertes Sudoku · Ziffern 1–12",
    "mega16"  to "Großes Sudoku · Ziffern 1–16",
)

data class StrandokuPuzzle(
    val size: Int,          // 9, 12, or 16
    val given: Array<IntArray>,    // 0 = empty cell
    val solution: Array<IntArray>,
)

data class StrandokuState(
    val puzzle: StrandokuPuzzle,
    val board: Array<IntArray>,
    val notes: Array<Array<MutableSet<Int>>>,
    val errors: Array<BooleanArray>,
    val selected: Pair<Int,Int>? = null,
    val noteMode: Boolean = false,
    val solved: Boolean = false,
)

// ── Generator ─────────────────────────────────────────────────────────────────

private fun boxOf(r: Int, c: Int, size: Int): Int {
    return when (size) {
        9  -> (r / 3) * 3 + (c / 3)
        12 -> (r / 3) * 4 + (c / 4)  // 3×4 boxes
        16 -> (r / 4) * 4 + (c / 4)
        else -> 0
    }
}

private fun isValid(board: Array<IntArray>, r: Int, c: Int, n: Int, size: Int): Boolean {
    for (i in 0 until size) {
        if (board[r][i] == n) return false
        if (board[i][c] == n) return false
    }
    val b = boxOf(r, c, size)
    val (bRows, bCols) = when (size) {
        9  -> 3 to 3
        12 -> 3 to 4
        16 -> 4 to 4
        else -> 3 to 3
    }
    val startR = (b / (size / bCols)) * bRows
    val startC = (b % (size / bCols)) * bCols
    for (dr in 0 until bRows) for (dc in 0 until bCols) {
        if (board[startR+dr][startC+dc] == n) return false
    }
    return true
}

private fun fillSudokuBoard(board: Array<IntArray>, size: Int, rng: Mulberry32): Boolean {
    for (r in 0 until size) for (c in 0 until size) {
        if (board[r][c] == 0) {
            val nums = (1..size).toMutableList().shuffledWith(rng)
            for (n in nums) {
                if (isValid(board, r, c, n, size)) {
                    board[r][c] = n
                    if (fillSudokuBoard(board, size, rng)) return true
                    board[r][c] = 0
                }
            }
            return false
        }
    }
    return true
}

private fun countSolutions(board: Array<IntArray>, size: Int, max: Int = 2): Int {
    // MRV: find cell with fewest candidates
    var bestR = -1; var bestC = -1; var bestCount = Int.MAX_VALUE
    for (r in 0 until size) for (c in 0 until size) {
        if (board[r][c] == 0) {
            val cnt = (1..size).count { n -> isValid(board, r, c, n, size) }
            if (cnt < bestCount) { bestCount = cnt; bestR = r; bestC = c }
        }
    }
    if (bestR == -1) return 1
    var count = 0
    for (n in 1..size) {
        if (isValid(board, bestR, bestC, n, size)) {
            board[bestR][bestC] = n
            count += countSolutions(board, size, max)
            board[bestR][bestC] = 0
            if (count >= max) return count
        }
    }
    return count
}

private val REMOVE_COUNT = mapOf("leicht" to 30, "mittel" to 45, "schwer" to 52, "experte" to 58)
private val REMOVE_COUNT_12 = mapOf("leicht" to 60, "mittel" to 90, "schwer" to 110, "experte" to 128)
private val REMOVE_COUNT_16 = mapOf("leicht" to 100, "mittel" to 160, "schwer" to 200, "experte" to 230)

fun generateStrandoku(variant: String, difficulty: String, seed: Int): StrandokuPuzzle {
    val size = when (variant) { "mega12" -> 12; "mega16" -> 16; else -> 9 }
    val rng = Mulberry32(seed)
    val solution = Array(size) { IntArray(size) }
    fillSudokuBoard(solution, size, rng)

    val removeCounts = when (size) { 12 -> REMOVE_COUNT_12; 16 -> REMOVE_COUNT_16; else -> REMOVE_COUNT }
    val target = removeCounts[difficulty] ?: 45

    val given = solution.map { it.clone() }.toTypedArray()
    val cells = (0 until size * size).toMutableList().shuffledWith(rng)
    var removed = 0
    for (idx in cells) {
        if (removed >= target) break
        val r = idx / size; val c = idx % size
        if (given[r][c] == 0) continue
        val backup = given[r][c]
        given[r][c] = 0
        val test = given.map { it.clone() }.toTypedArray()
        if (countSolutions(test, size) != 1) { given[r][c] = backup } else { removed++ }
    }
    return StrandokuPuzzle(size, given, solution)
}

// ── State management ──────────────────────────────────────────────────────────

fun createStrandokuState(puzzle: StrandokuPuzzle): StrandokuState {
    val board = puzzle.given.map { it.clone() }.toTypedArray()
    val notes = Array(puzzle.size) { Array(puzzle.size) { mutableSetOf<Int>() } }
    val errors = Array(puzzle.size) { BooleanArray(puzzle.size) }
    return StrandokuState(puzzle, board, notes, errors)
}

fun selectStrandokuCell(state: StrandokuState, r: Int, c: Int): StrandokuState {
    if (state.puzzle.given[r][c] != 0) return state
    return state.copy(selected = r to c)
}

fun enterStrandokuNumber(state: StrandokuState, n: Int): StrandokuState {
    val sel = state.selected ?: return state
    val (r, c) = sel
    if (state.puzzle.given[r][c] != 0) return state

    val board = state.board.map { it.clone() }.toTypedArray()
    val notes = state.notes.map { row -> row.map { it.toMutableSet() }.toTypedArray() }.toTypedArray()

    if (state.noteMode) {
        if (n in notes[r][c]) notes[r][c].remove(n) else notes[r][c].add(n)
    } else {
        board[r][c] = if (board[r][c] == n) 0 else n
        notes[r][c].clear()
    }
    val errors = computeStrandokuErrors(board, state.puzzle)
    val solved = checkStrandokuSolved(board, state.puzzle)
    return state.copy(board = board, notes = notes, errors = errors, solved = solved)
}

fun eraseStrandokuCell(state: StrandokuState): StrandokuState {
    val sel = state.selected ?: return state
    val (r, c) = sel
    if (state.puzzle.given[r][c] != 0) return state
    val board = state.board.map { it.clone() }.toTypedArray()
    val notes = state.notes.map { row -> row.map { it.toMutableSet() }.toTypedArray() }.toTypedArray()
    board[r][c] = 0; notes[r][c].clear()
    val errors = computeStrandokuErrors(board, state.puzzle)
    return state.copy(board = board, notes = notes, errors = errors, solved = false)
}

private fun computeStrandokuErrors(board: Array<IntArray>, puzzle: StrandokuPuzzle): Array<BooleanArray> {
    return Array(puzzle.size) { r ->
        BooleanArray(puzzle.size) { c ->
            board[r][c] != 0 && board[r][c] != puzzle.solution[r][c]
        }
    }
}

private fun checkStrandokuSolved(board: Array<IntArray>, puzzle: StrandokuPuzzle): Boolean {
    for (r in 0 until puzzle.size) for (c in 0 until puzzle.size) {
        if (board[r][c] != puzzle.solution[r][c]) return false
    }
    return true
}

fun getStrandokuHint(state: StrandokuState): Pair<Int,Int>? {
    val size = state.puzzle.size
    val wrong = mutableListOf<Pair<Int,Int>>()
    for (r in 0 until size) for (c in 0 until size) {
        if (state.puzzle.given[r][c] != 0) continue
        if (state.board[r][c] != state.puzzle.solution[r][c]) wrong.add(r to c)
    }
    return if (wrong.isEmpty()) null else wrong.random()
}

// ── Serialization ─────────────────────────────────────────────────────────────

fun serializeStrandokuState(state: StrandokuState): String {
    val boardArr = JSONArray(); for (row in state.board) { val r = JSONArray(); row.forEach { r.put(it) }; boardArr.put(r) }
    val notesArr = JSONArray(); for (row in state.notes) { val r = JSONArray(); for (s in row) { val sa = JSONArray(); s.forEach { sa.put(it) }; r.put(sa) }; notesArr.put(r) }
    return JSONObject().put("board", boardArr).put("notes", notesArr).toString()
}

fun deserializeStrandokuState(puzzle: StrandokuPuzzle, raw: String): StrandokuState {
    return try {
        val obj = JSONObject(raw)
        val boardArr = obj.getJSONArray("board")
        val board = Array(puzzle.size) { r -> IntArray(puzzle.size) { c -> boardArr.getJSONArray(r).getInt(c) } }
        val notesArr = obj.optJSONArray("notes")
        val notes = Array(puzzle.size) { r ->
            Array(puzzle.size) { c ->
                val s = mutableSetOf<Int>()
                notesArr?.optJSONArray(r)?.optJSONArray(c)?.let { sa -> (0 until sa.length()).forEach { i -> s.add(sa.getInt(i)) } }
                s
            }
        }
        val errors = computeStrandokuErrors(board, puzzle)
        val solved = checkStrandokuSolved(board, puzzle)
        StrandokuState(puzzle, board, notes, errors, null, false, solved)
    } catch (_: Exception) {
        createStrandokuState(puzzle)
    }
}
