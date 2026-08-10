package com.bestfriends.beachbingo.feature.raetsel

import org.json.JSONArray
import org.json.JSONObject

// ── Variant metadata ────────────────────────────────────────────────────────────

val STRANDOKU_VARIANT_LABELS = mapOf(
    "classic"   to "Classic 9×9",
    "mega12"    to "Mega 12×12",
    "mega16"    to "Mega 16×16",
    "irregular" to "Irregular 9×9",
    "diagonal"  to "Diagonal 9×9",
    "killer"    to "Killer 9×9",
    "samurai"   to "Samurai 5×9",
)

val STRANDOKU_VARIANT_DESCRIPTIONS = mapOf(
    "classic"   to "Standard Sudoku · Ziffern 1–9",
    "mega12"    to "Erweitertes Sudoku · Ziffern 1–12",
    "mega16"    to "Großes Sudoku · Ziffern 1–16",
    "irregular" to "Unregelmäßige Bereiche",
    "diagonal"  to "Extra: Diagonalen 1–9",
    "killer"    to "Käfige mit Summenbedingungen",
    "samurai"   to "5 überlappende 9×9 Grids",
)

// ── Data classes ────────────────────────────────────────────────────────────────

data class KillerCage(val cells: List<Pair<Int, Int>>, val sum: Int)

/**
 * given[r][c]:  -1 = inactive (Samurai gap)
 *                0 = empty (player fills)
 *               >0 = pre-filled given value
 * solution[r][c]: -1 = inactive, else correct answer
 */
data class StrandokuPuzzle(
    val size: Int,
    val given: Array<IntArray>,
    val solution: Array<IntArray>,
    val variant: String = "classic",
    val regions: Array<IntArray>? = null,
    val cages: List<KillerCage>? = null,
    val isSamurai: Boolean = false,
)

data class StrandokuState(
    val puzzle: StrandokuPuzzle,
    val board: Array<IntArray>,
    val notes: Array<Array<MutableSet<Int>>>,
    val errors: Array<BooleanArray>,
    val selected: Pair<Int, Int>? = null,
    val noteMode: Boolean = false,
    val solved: Boolean = false,
)

// ── Box helpers ─────────────────────────────────────────────────────────────────

private fun boxOf(r: Int, c: Int, size: Int): Int = when (size) {
    9  -> (r / 3) * 3 + (c / 3)
    12 -> (r / 3) * 4 + (c / 4)
    16 -> (r / 4) * 4 + (c / 4)
    else -> 0
}

fun getBoxDimensions(size: Int): Pair<Int, Int> = when (size) {
    9  -> 3 to 3
    12 -> 4 to 3
    16 -> 4 to 4
    else -> 3 to 3
}

// ── Constraint validation ────────────────────────────────────────────────────────

private fun isValid(
    board: Array<IntArray>, r: Int, c: Int, n: Int, size: Int,
    diagonal: Boolean = false, regions: Array<IntArray>? = null,
): Boolean {
    for (i in 0 until size) {
        if (board[r][i] == n) return false
        if (board[i][c] == n) return false
    }
    if (regions != null) {
        val regionId = regions[r][c]
        for (rr in 0 until size) for (cc in 0 until size) {
            if ((rr != r || cc != c) && regions[rr][cc] == regionId && board[rr][cc] == n) return false
        }
    } else {
        val b = boxOf(r, c, size)
        val (bCols, bRows) = getBoxDimensions(size)
        val stride = size / bRows   // must match boxOf's row-group stride
        val startR = (b / stride) * bRows
        val startC = (b % stride) * bCols
        for (dr in 0 until bRows) for (dc in 0 until bCols) {
            if (board[startR + dr][startC + dc] == n) return false
        }
    }
    if (diagonal) {
        if (r == c)
            for (i in 0 until size) { if (i != r && board[i][i] == n) return false }
        if (r + c == size - 1)
            for (i in 0 until size) { if (i != r && board[i][size - 1 - i] == n) return false }
    }
    return true
}

// ── Backtracking fill ───────────────────────────────────────────────────────────

private fun fillBoard(
    board: Array<IntArray>, size: Int, rng: Mulberry32,
    diagonal: Boolean = false, regions: Array<IntArray>? = null,
): Boolean {
    for (r in 0 until size) for (c in 0 until size) {
        if (board[r][c] == 0) {
            val nums = (1..size).toMutableList().shuffledWith(rng)
            for (n in nums) {
                if (isValid(board, r, c, n, size, diagonal, regions)) {
                    board[r][c] = n
                    if (fillBoard(board, size, rng, diagonal, regions)) return true
                    board[r][c] = 0
                }
            }
            return false
        }
    }
    return true
}

// ── Uniqueness check (MRV heuristic) ──────────────────────────────────────────

private fun countSolutions(
    board: Array<IntArray>, size: Int, max: Int = 2,
    diagonal: Boolean = false, regions: Array<IntArray>? = null,
): Int {
    var count = 0
    var steps = 0
    fun solve(): Boolean {
        if (count >= max) return true
        if (++steps > 60_000) return true
        var bestR = -1; var bestC = -1; var bestN = Int.MAX_VALUE
        for (r in 0 until size) for (c in 0 until size) {
            if (board[r][c] == 0) {
                val cnt = (1..size).count { n -> isValid(board, r, c, n, size, diagonal, regions) }
                if (cnt == 0) return false
                if (cnt < bestN) { bestN = cnt; bestR = r; bestC = c }
            }
        }
        if (bestR == -1) { count++; return count >= max }
        for (n in 1..size) {
            if (isValid(board, bestR, bestC, n, size, diagonal, regions)) {
                board[bestR][bestC] = n
                if (solve()) return true
                board[bestR][bestC] = 0
            }
        }
        return false
    }
    solve()
    return count
}

// ── Clue removal ────────────────────────────────────────────────────────────────

private val REMOVE_9  = mapOf("leicht" to 30, "mittel" to 45, "schwer" to 52, "experte" to 58)
private val REMOVE_12 = mapOf("leicht" to 60, "mittel" to 90, "schwer" to 110, "experte" to 128)
private val REMOVE_16 = mapOf("leicht" to 100, "mittel" to 130, "schwer" to 160, "experte" to 180)

private fun removeClues(
    solution: Array<IntArray>, size: Int, difficulty: String, rng: Mulberry32,
    diagonal: Boolean = false, regions: Array<IntArray>? = null,
): Array<IntArray> {
    val target = (when (size) { 12 -> REMOVE_12; 16 -> REMOVE_16; else -> REMOVE_9 })[difficulty] ?: 45
    val given = solution.map { it.clone() }.toTypedArray()
    val cells = (0 until size * size).toMutableList().shuffledWith(rng)
    // Large boards (12×12, 16×16) and irregular hard/expert time out in countSolutions,
    // returning 0 → every cell treated as non-unique → zero blanks. Skip uniqueness here.
    val skipUniqueness =
        (size > 9 || regions != null) &&
        (difficulty == "schwer" || difficulty == "experte")

    var removed = 0
    for (idx in cells) {
        if (removed >= target) break
        val r = idx / size; val c = idx % size
        if (given[r][c] == 0) continue
        if (skipUniqueness) {
            given[r][c] = 0
            removed++
        } else {
            val backup = given[r][c]
            given[r][c] = 0
            val test = given.map { it.clone() }.toTypedArray()
            if (countSolutions(test, size, 2, diagonal, regions) != 1) given[r][c] = backup
            else removed++
        }
    }
    return given
}

// ── Irregular region generator ──────────────────────────────────────────────────

private fun generateIrregularRegions(size: Int, rng: Mulberry32): Array<IntArray> {
    repeat(50) {
        val result = tryGenerateIrregularRegions(size, rng)
        if (result != null) return result
        rng.nextInt(size) // advance RNG so next attempt differs
    }
    // Fallback: rows as regions (guaranteed equal size)
    return Array(size) { r -> IntArray(size) { r } }
}

private fun tryGenerateIrregularRegions(size: Int, rng: Mulberry32): Array<IntArray>? {
    val regions = Array(size) { IntArray(size) { -1 } }
    for (regionId in 0 until size) {
        var startR = -1; var startC = -1
        outer@ for (r in 0 until size) for (c in 0 until size) {
            if (regions[r][c] == -1) { startR = r; startC = c; break@outer }
        }
        if (startR == -1) break
        regions[startR][startC] = regionId
        var placed = 1
        val frontier = mutableListOf(startR to startC)
        while (placed < size && frontier.isNotEmpty()) {
            val fi = rng.nextInt(frontier.size)
            val (cr, cc) = frontier[fi]
            val dirs = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1).shuffledWith(rng)
            var expanded = false
            for ((dr, dc) in dirs) {
                val nr = cr + dr; val nc = cc + dc
                if (nr in 0 until size && nc in 0 until size && regions[nr][nc] == -1) {
                    regions[nr][nc] = regionId
                    frontier.add(nr to nc)
                    placed++; expanded = true; break
                }
            }
            if (!expanded) frontier.removeAt(fi)
        }
        if (placed < size) return null  // region too small — retry entire generation
    }
    return regions
}

// ── Killer cage generator ───────────────────────────────────────────────────────

private fun generateKillerCages(solution: Array<IntArray>, size: Int, rng: Mulberry32): List<KillerCage> {
    val assigned = Array(size) { BooleanArray(size) }
    val cages = mutableListOf<KillerCage>()
    val cells = (0 until size * size).toMutableList().shuffledWith(rng)
    for (idx in cells) {
        val r = idx / size; val c = idx % size
        if (assigned[r][c]) continue
        val targetSize = 2 + rng.nextInt(3)
        val cage = mutableListOf(r to c)
        assigned[r][c] = true
        while (cage.size < targetSize) {
            val (cr, cc) = cage[rng.nextInt(cage.size)]
            val dirs = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1).shuffledWith(rng)
            var found = false
            for ((dr, dc) in dirs) {
                val nr = cr + dr; val nc = cc + dc
                if (nr in 0 until size && nc in 0 until size && !assigned[nr][nc]) {
                    cage.add(nr to nc); assigned[nr][nc] = true; found = true; break
                }
            }
            if (!found) break
        }
        val sum = cage.sumOf { (row, col) -> solution[row][col] }
        cages.add(KillerCage(cage, sum))
    }
    return cages
}

// ── Samurai generator ───────────────────────────────────────────────────────────

private fun generateSamurai(difficulty: String, seed: Int): StrandokuPuzzle {
    val rng = Mulberry32(seed)
    val FULL = 21
    val offsets = listOf(0 to 0, 0 to 12, 6 to 6, 12 to 0, 12 to 12)

    val fullSolution = Array(FULL) { IntArray(FULL) { -1 } }
    for ((or, oc) in offsets) {
        val sol = Array(9) { IntArray(9) }
        fillBoard(sol, 9, rng)
        for (r in 0 until 9) for (c in 0 until 9) fullSolution[or + r][oc + c] = sol[r][c]
    }

    // Valid cells (not -1) start as 0 (player fills)
    val fullGiven = Array(FULL) { r -> IntArray(FULL) { c -> if (fullSolution[r][c] != -1) 0 else -1 } }

    // Show some cells as given
    val removeCount = (REMOVE_9[difficulty] ?: 45) * 5
    val validCells = mutableListOf<Pair<Int, Int>>()
    for (r in 0 until FULL) for (c in 0 until FULL) if (fullSolution[r][c] != -1) validCells.add(r to c)
    val toShow = validCells.shuffledWith(rng).take(maxOf(0, validCells.size - removeCount))
    toShow.forEach { (r, c) -> fullGiven[r][c] = fullSolution[r][c] }

    return StrandokuPuzzle(FULL, fullGiven, fullSolution, "samurai", isSamurai = true)
}

// ── Main generator ──────────────────────────────────────────────────────────────

fun generateStrandoku(variant: String, difficulty: String, seed: Int): StrandokuPuzzle {
    if (variant == "samurai") return generateSamurai(difficulty, seed)

    val size = when (variant) { "mega12" -> 12; "mega16" -> 16; else -> 9 }
    val diagonal = variant == "diagonal"
    val rng = Mulberry32(seed)

    val regions: Array<IntArray>? = if (variant == "irregular") generateIrregularRegions(size, rng) else null

    val solution = Array(size) { IntArray(size) }
    fillBoard(solution, size, rng, diagonal, regions)

    val given = if (variant == "killer") {
        Array(size) { IntArray(size) }  // no given clues in killer
    } else {
        removeClues(solution, size, difficulty, rng, diagonal, regions)
    }

    val cages = if (variant == "killer") generateKillerCages(solution, size, rng) else null

    return StrandokuPuzzle(size, given, solution, variant, regions, cages)
}

// ── State management ────────────────────────────────────────────────────────────

fun createStrandokuState(puzzle: StrandokuPuzzle): StrandokuState {
    val board = puzzle.given.map { it.clone() }.toTypedArray()
    val notes = Array(puzzle.size) { Array(puzzle.size) { mutableSetOf<Int>() } }
    val errors = Array(puzzle.size) { BooleanArray(puzzle.size) }
    return StrandokuState(puzzle, board, notes, errors)
}

fun selectStrandokuCell(state: StrandokuState, r: Int, c: Int): StrandokuState {
    if (state.puzzle.solution[r][c] == -1) return state
    return state.copy(selected = r to c)
}

fun enterStrandokuNumber(state: StrandokuState, n: Int): StrandokuState {
    val sel = state.selected ?: return state
    val (r, c) = sel
    if (state.puzzle.given[r][c] > 0) return state
    if (state.puzzle.solution[r][c] == -1) return state

    val board = state.board.map { it.clone() }.toTypedArray()
    val notes = state.notes.map { row -> row.map { it.toMutableSet() }.toTypedArray() }.toTypedArray()

    if (state.noteMode) {
        if (board[r][c] != 0) return state
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
    if (state.puzzle.given[r][c] > 0) return state
    if (state.puzzle.solution[r][c] == -1) return state
    val board = state.board.map { it.clone() }.toTypedArray()
    val notes = state.notes.map { row -> row.map { it.toMutableSet() }.toTypedArray() }.toTypedArray()
    board[r][c] = 0; notes[r][c].clear()
    val errors = computeStrandokuErrors(board, state.puzzle)
    return state.copy(board = board, notes = notes, errors = errors, solved = false)
}

private fun computeStrandokuErrors(board: Array<IntArray>, puzzle: StrandokuPuzzle): Array<BooleanArray> {
    return Array(puzzle.size) { r ->
        BooleanArray(puzzle.size) { c ->
            val sol = puzzle.solution[r][c]
            sol != -1 && board[r][c] != 0 && board[r][c] != sol
        }
    }
}

private fun checkStrandokuSolved(board: Array<IntArray>, puzzle: StrandokuPuzzle): Boolean {
    for (r in 0 until puzzle.size) for (c in 0 until puzzle.size) {
        val sol = puzzle.solution[r][c]
        if (sol == -1) continue
        if (board[r][c] != sol) return false
    }
    return true
}

fun getStrandokuHint(state: StrandokuState): Pair<Int, Int>? {
    val wrong = mutableListOf<Pair<Int, Int>>()
    for (r in 0 until state.puzzle.size) for (c in 0 until state.puzzle.size) {
        val sol = state.puzzle.solution[r][c]
        if (sol == -1 || state.puzzle.given[r][c] > 0) continue
        if (state.board[r][c] != sol) wrong.add(r to c)
    }
    return wrong.randomOrNull()
}

// ── Display helpers ──────────────────────────────────────────────────────────────

val REGION_COLORS_ARGB = listOf(
    0xFF0ea5e9L, 0xFFf59e0bL, 0xFF22c55eL, 0xFFec4899L,
    0xFF8b5cf6L, 0xFF06b6d4L, 0xFFf97316L, 0xFF6366f1L,
    0xFF14b8a6L, 0xFFf43224L,
)

val CAGE_COLORS_ARGB = listOf(
    0xFF0ea5e9L, 0xFFf59e0bL, 0xFF22c55eL, 0xFFec4899L,
    0xFF8b5cf6L, 0xFF06b6d4L, 0xFFf97316L, 0xFF6366f1L,
)

fun getCageIndex(puzzle: StrandokuPuzzle, r: Int, c: Int): Int {
    val cages = puzzle.cages ?: return -1
    return cages.indexOfFirst { cage -> cage.cells.any { (cr, cc) -> cr == r && cc == c } }
}

fun isCageTopLeft(puzzle: StrandokuPuzzle, r: Int, c: Int): KillerCage? {
    val cages = puzzle.cages ?: return null
    return cages.find { cage -> cage.cells.firstOrNull() == (r to c) }
}

// ── Serialization ────────────────────────────────────────────────────────────────

fun serializeStrandokuState(state: StrandokuState): String {
    val boardArr = JSONArray()
    for (row in state.board) { val r = JSONArray(); row.forEach { r.put(it) }; boardArr.put(r) }
    val notesArr = JSONArray()
    for (row in state.notes) {
        val r = JSONArray()
        for (s in row) { val sa = JSONArray(); s.forEach { sa.put(it) }; r.put(sa) }
        notesArr.put(r)
    }
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
                notesArr?.optJSONArray(r)?.optJSONArray(c)?.let { sa ->
                    (0 until sa.length()).forEach { i -> s.add(sa.getInt(i)) }
                }
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
