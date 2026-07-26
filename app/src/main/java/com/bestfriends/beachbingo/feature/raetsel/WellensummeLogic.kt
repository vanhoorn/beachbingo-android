package com.bestfriends.beachbingo.feature.raetsel

import org.json.JSONArray
import org.json.JSONObject

val KAKURO_SIZES = mapOf("leicht" to 7, "mittel" to 9, "schwer" to 11, "experte" to 13)

data class KakuroCell(
    val isBlack: Boolean,
    val downClue: Int? = null,
    val rightClue: Int? = null,
    val solution: Int? = null,
)

data class KakuroPuzzle(val size: Int, val cells: Array<Array<KakuroCell>>)

data class KakuroState(
    val puzzle: KakuroPuzzle,
    val board: Array<IntArray>,   // 0 = empty
    val errors: Array<BooleanArray>,
    val selected: Pair<Int,Int>? = null,
    val solved: Boolean = false,
)

// ── Generation ────────────────────────────────────────────────────────────────

fun generateKakuro(difficulty: String, seed: Int): KakuroPuzzle {
    val size = KAKURO_SIZES[difficulty] ?: 9
    val rng = Mulberry32(seed)
    for (attempt in 0 until 20) {
        val p = tryGenerateKakuro(size, rng)
        if (p != null) return p
    }
    return generateKakuro(difficulty, seed + 111)
}

private fun tryGenerateKakuro(size: Int, rng: Mulberry32): KakuroPuzzle? {
    val isBlack = Array(size) { r -> BooleanArray(size) { c -> r == 0 || c == 0 || (r > 0 && c > 0 && rng.next() < 0.28) } }

    // Fix single-cell runs
    for (pass in 0 until 5) {
        var changed = false
        for (r in 1 until size) for (c in 1 until size) {
            if (!isBlack[r][c]) {
                val runH = getRunLen(isBlack, r, c, 0, 1, size)
                val runV = getRunLen(isBlack, r, c, 1, 0, size)
                if (runH == 1 || runV == 1) { isBlack[r][c] = true; changed = true }
            }
        }
        if (!changed) break
    }

    val whiteCells = (1 until size).flatMap { r -> (1 until size).filter { c -> !isBlack[r][c] }.map { c -> r to c } }
    if (whiteCells.size < 6) return null

    val solution = Array(size) { IntArray(size) }
    if (!fillKakuro(solution, isBlack, size, whiteCells.shuffledWith(rng), 0)) return null

    val cells = Array(size) { r ->
        Array(size) { c ->
            if (isBlack[r][c]) {
                var rightClue: Int? = null; var downClue: Int? = null
                if (c < size - 1 && !isBlack[r][c+1]) {
                    rightClue = (c+1 until size).takeWhile { !isBlack[r][it] }.sumOf { solution[r][it] }
                }
                if (r < size - 1 && !isBlack[r+1][c]) {
                    downClue = (r+1 until size).takeWhile { !isBlack[it][c] }.sumOf { solution[it][c] }
                }
                KakuroCell(true, downClue, rightClue)
            } else {
                KakuroCell(false, solution = solution[r][c])
            }
        }
    }
    return KakuroPuzzle(size, cells)
}

private fun getRunLen(isBlack: Array<BooleanArray>, r: Int, c: Int, dr: Int, dc: Int, size: Int): Int {
    var rr = r; var cc = c
    while (rr > 0 && cc > 0 && !isBlack[rr-dr][cc-dc]) { rr -= dr; cc -= dc }
    var len = 0
    while (rr in 0 until size && cc in 0 until size && !isBlack[rr][cc]) { len++; rr += dr; cc += dc }
    return len
}

private fun fillKakuro(sol: Array<IntArray>, isBlack: Array<BooleanArray>, size: Int, cells: List<Pair<Int,Int>>, idx: Int): Boolean {
    if (idx == cells.size) return cells.all { (r,c) -> !isBlack[r][c] && sol[r][c] != 0 }
    val (r, c) = cells[idx]
    for (n in 1..9) {
        if (isKakuroPlaceable(sol, isBlack, r, c, n, size)) {
            sol[r][c] = n
            if (fillKakuro(sol, isBlack, size, cells, idx+1)) return true
            sol[r][c] = 0
        }
    }
    return false
}

private fun isKakuroPlaceable(sol: Array<IntArray>, isBlack: Array<BooleanArray>, r: Int, c: Int, n: Int, size: Int): Boolean {
    for (cc in c-1 downTo 0) { if (isBlack[r][cc]) break; if (sol[r][cc] == n) return false }
    for (cc in c+1 until size) { if (isBlack[r][cc]) break; if (sol[r][cc] == n) return false }
    for (rr in r-1 downTo 0) { if (isBlack[rr][c]) break; if (sol[rr][c] == n) return false }
    for (rr in r+1 until size) { if (isBlack[rr][c]) break; if (sol[rr][c] == n) return false }
    return true
}

// ── State management ──────────────────────────────────────────────────────────

fun createKakuroState(puzzle: KakuroPuzzle): KakuroState {
    val board = Array(puzzle.size) { IntArray(puzzle.size) }
    val errors = Array(puzzle.size) { BooleanArray(puzzle.size) }
    return KakuroState(puzzle, board, errors)
}

fun selectKakuroCell(state: KakuroState, r: Int, c: Int): KakuroState {
    if (state.puzzle.cells[r][c].isBlack) return state
    return state.copy(selected = r to c)
}

fun enterKakuroNumber(state: KakuroState, n: Int): KakuroState {
    val sel = state.selected ?: return state
    val (r, c) = sel
    val board = state.board.map { it.clone() }.toTypedArray()
    board[r][c] = if (board[r][c] == n) 0 else n
    val errors = computeKakuroErrors(board, state.puzzle)
    val solved = checkKakuroSolved(board, state.puzzle)
    return state.copy(board = board, errors = errors, solved = solved)
}

fun eraseKakuroCell(state: KakuroState): KakuroState {
    val sel = state.selected ?: return state
    val (r, c) = sel
    val board = state.board.map { it.clone() }.toTypedArray()
    board[r][c] = 0
    val errors = computeKakuroErrors(board, state.puzzle)
    return state.copy(board = board, errors = errors, solved = false)
}

private fun computeKakuroErrors(board: Array<IntArray>, puzzle: KakuroPuzzle): Array<BooleanArray> {
    return Array(puzzle.size) { r ->
        BooleanArray(puzzle.size) { c ->
            val cell = puzzle.cells[r][c]
            !cell.isBlack && board[r][c] != 0 && cell.solution != null && board[r][c] != cell.solution
        }
    }
}

private fun checkKakuroSolved(board: Array<IntArray>, puzzle: KakuroPuzzle): Boolean {
    for (r in 0 until puzzle.size) for (c in 0 until puzzle.size) {
        val cell = puzzle.cells[r][c]
        if (!cell.isBlack && board[r][c] != cell.solution) return false
    }
    return true
}

fun getKakuroHint(state: KakuroState): Pair<Int,Int>? {
    val size = state.puzzle.size
    val wrong = mutableListOf<Pair<Int,Int>>()
    for (r in 0 until size) for (c in 0 until size) {
        val cell = state.puzzle.cells[r][c]
        if (!cell.isBlack && state.board[r][c] != cell.solution) wrong.add(r to c)
    }
    return if (wrong.isEmpty()) null else wrong.random()
}

// ── Serialization ─────────────────────────────────────────────────────────────

fun serializeKakuroState(state: KakuroState): String {
    val arr = JSONArray()
    for (row in state.board) { val r = JSONArray(); row.forEach { r.put(it) }; arr.put(r) }
    return JSONObject().put("board", arr).toString()
}

fun deserializeKakuroState(puzzle: KakuroPuzzle, raw: String): KakuroState {
    return try {
        val obj = JSONObject(raw)
        val arr = obj.getJSONArray("board")
        val board = Array(puzzle.size) { r -> IntArray(puzzle.size) { c -> arr.getJSONArray(r).getInt(c) } }
        val errors = computeKakuroErrors(board, puzzle)
        val solved = checkKakuroSolved(board, puzzle)
        KakuroState(puzzle, board, errors, null, solved)
    } catch (_: Exception) {
        createKakuroState(puzzle)
    }
}
