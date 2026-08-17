package com.bestfriends.beachbingo.feature.perlentaucher

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random

const val BOARD_SIZE = 8

enum class BoardPhase {
    IDLE, SWAPPING, MATCHING, FALLING, FILLING, CHECK_DEADLOCK, SHUFFLE
}

data class Match(
    val cells: List<Pair<Int, Int>>,
    val pieceType: PieceType,
    val isHorizontal: Boolean,
)

// Schritt 2 konsumiert specialGenCells, um neue Spezialsteine auf dem Board zu platzieren
data class MatchResult(
    val matches: List<Match>,
    val clearedCells: Set<Pair<Int, Int>>,
    val pointsGained: Int,
    val specialGenCells: List<Pair<Pair<Int, Int>, SpecialType>>,
)

class PerlentaucherBoardModel(
    val levelSeed: Long,
    private val rng: Random = Random(levelSeed),
) {

    // null = leere Zelle
    val board: Array<Array<PerlentaucherPiece?>> = Array(BOARD_SIZE) { arrayOfNulls(BOARD_SIZE) }

    var phase: BoardPhase = BoardPhase.IDLE
        private set

    var score: Int = 0
        private set

    private var cascadeLevel: Int = 0

    init { initBoard() }

    // ── Init ─────────────────────────────────────────────────────────────────────

    private fun randomPiece(): PerlentaucherPiece =
        PerlentaucherPiece(PieceType.entries[rng.nextInt(PieceType.entries.size)])

    private fun initBoard() {
        for (r in 0 until BOARD_SIZE) {
            for (c in 0 until BOARD_SIZE) {
                var piece = randomPiece()
                while (c >= 2 &&
                    board[r][c - 1]?.type == piece.type &&
                    board[r][c - 2]?.type == piece.type
                ) piece = randomPiece()
                while (r >= 2 &&
                    board[r - 1][c]?.type == piece.type &&
                    board[r - 2][c]?.type == piece.type
                ) piece = randomPiece()
                board[r][c] = piece
            }
        }
    }

    // ── Swap ─────────────────────────────────────────────────────────────────────

    // Prüft zuerst auf Kombo; führt sonst normalen Swap durch.
    // Gibt null zurück wenn der Swap ungültig ist; sonst MatchResult.
    fun trySwap(r1: Int, c1: Int, r2: Int, c2: Int): MatchResult? {
        if (phase != BoardPhase.IDLE) return null
        if (abs(r1 - r2) + abs(c1 - c2) != 1) return null

        // Kombo-Swap hat Vorrang
        tryComboSwap(r1, c1, r2, c2)?.let { return it }

        val tmp = board[r1][c1]
        board[r1][c1] = board[r2][c2]
        board[r2][c2] = tmp

        if (findMatches().isEmpty()) {
            board[r2][c2] = board[r1][c1]
            board[r1][c1] = tmp
            return null
        }
        phase = BoardPhase.MATCHING
        return applyMatches()
    }

    // Aktiviert Kombo wenn beide Felder Spezialsteine haben.
    private fun tryComboSwap(r1: Int, c1: Int, r2: Int, c2: Int): MatchResult? {
        val s1 = board[r1][c1]?.special ?: SpecialType.NONE
        val s2 = board[r2][c2]?.special ?: SpecialType.NONE
        val combo = detectCombo(s1, s2) ?: return null

        val tmp = board[r1][c1]
        board[r1][c1] = board[r2][c2]
        board[r2][c2] = tmp

        val activation = activateCombo(board, r1, c1, r2, c2, combo)

        val comboStones = setOf(Pair(r1, c1), Pair(r2, c2))
        val (allCleared, totalPts) = chainActivateSpecials(
            startCells = activation.clearedCells,
            skipCells = comboStones,
            extraPts = activation.points,
        )
        // Kombo-Steine ebenfalls löschen
        val finalCleared = allCleared + comboStones
        finalCleared.forEach { (r, c) -> board[r][c] = null }

        score += totalPts
        phase = BoardPhase.FALLING
        cascadeLevel = 0

        return MatchResult(emptyList(), finalCleared, totalPts, emptyList())
    }

    // ── Match-Erkennung ───────────────────────────────────────────────────────────

    fun findMatches(): List<Match> {
        val matches = mutableListOf<Match>()

        for (r in 0 until BOARD_SIZE) {
            var c = 0
            while (c < BOARD_SIZE) {
                val type = board[r][c]?.type
                if (type == null) { c++; continue }
                var len = 1
                while (c + len < BOARD_SIZE && board[r][c + len]?.type == type) len++
                if (len >= 3) {
                    matches.add(Match(
                        cells = (0 until len).map { Pair(r, c + it) },
                        pieceType = type,
                        isHorizontal = true,
                    ))
                }
                c += len
            }
        }

        for (c in 0 until BOARD_SIZE) {
            var r = 0
            while (r < BOARD_SIZE) {
                val type = board[r][c]?.type
                if (type == null) { r++; continue }
                var len = 1
                while (r + len < BOARD_SIZE && board[r + len][c]?.type == type) len++
                if (len >= 3) {
                    matches.add(Match(
                        cells = (0 until len).map { Pair(r + it, c) },
                        pieceType = type,
                        isHorizontal = false,
                    ))
                }
                r += len
            }
        }

        return matches
    }

    // ── Matches anwenden ──────────────────────────────────────────────────────────

    // Ruft intern trySwap auf; kann aber auch direkt für automatische Kaskaden aufgerufen werden.
    fun applyMatches(): MatchResult {
        val matches = findMatches()
        if (matches.isEmpty()) {
            cascadeLevel = 0
            phase = BoardPhase.CHECK_DEADLOCK
            return MatchResult(emptyList(), emptySet(), 0, emptyList())
        }

        val multiplier = 1.5.pow(cascadeLevel.toDouble())

        // Punkte für normale Matches (mit Kaskaden-Faktor)
        var matchPts = 0
        val initialCells = mutableSetOf<Pair<Int, Int>>()
        matches.forEach { m ->
            val ppp = when {
                m.cells.size >= 5 -> 120
                m.cells.size == 4 -> 90
                else -> 60
            }
            matchPts += (m.cells.size * ppp * multiplier).roundToInt()
            m.cells.forEach { initialCells.add(it) }
        }

        // Spezialsteine in den gematchten Zellen verketten
        val matchTypeFor: Map<Pair<Int, Int>, PieceType> = buildMap {
            matches.forEach { m -> m.cells.forEach { put(it, m.pieceType) } }
        }
        val initialSpecials: Set<Pair<Pair<Int, Int>, PieceType?>> = initialCells
            .filter { board[it.first][it.second]?.special != SpecialType.NONE }
            .map { Pair(it, matchTypeFor[it]) }
            .toSet()

        val (allCleared, specialPts) = chainActivateSpecials(
            startCells = initialCells,
            initialSpecialQueue = initialSpecials,
        )

        val totalPts = matchPts + specialPts

        // Spezialstein-Generierung nur aus Original-Matches
        val hCells = matches.filter { it.isHorizontal }.flatMap { it.cells }.toSet()
        val vCells = matches.filter { !it.isHorizontal }.flatMap { it.cells }.toSet()
        val intersections = hCells intersect vCells
        val specialGens = buildSpecialGens(matches, intersections)

        allCleared.forEach { (r, c) -> board[r][c] = null }

        score += totalPts
        phase = BoardPhase.FALLING
        cascadeLevel++

        return MatchResult(matches, allCleared, totalPts, specialGens)
    }

    // ── Ketten-Aktivierung ────────────────────────────────────────────────────────

    // Iterativ: aktiviert alle Spezialsteine in startCells und jeden neu freigelegten Spezialstein.
    // skipCells werden als bereits verarbeitet behandelt (z. B. Kombo-Steine).
    // Gibt (alle geleerten Zellen, Punkte aus Spezialaktivierungen) zurück.
    private fun chainActivateSpecials(
        startCells: Set<Pair<Int, Int>>,
        skipCells: Set<Pair<Int, Int>> = emptySet(),
        initialSpecialQueue: Set<Pair<Pair<Int, Int>, PieceType?>> = emptySet(),
        extraPts: Int = 0,
    ): Pair<Set<Pair<Int, Int>>, Int> {
        val allCleared = startCells.toMutableSet()
        var pts = extraPts
        val queue = ArrayDeque<Pair<Pair<Int, Int>, PieceType?>>()
        val activated = skipCells.toMutableSet()

        // Seed queue
        if (initialSpecialQueue.isNotEmpty()) {
            initialSpecialQueue.forEach { queue.add(it) }
        } else {
            startCells.filter { it !in skipCells }.forEach { cell ->
                val piece = board[cell.first][cell.second]
                if (piece != null && piece.special != SpecialType.NONE)
                    queue.add(Pair(cell, piece.type))
            }
        }

        while (queue.isNotEmpty()) {
            val (pos, matchedType) = queue.removeFirst()
            if (!activated.add(pos)) continue
            val piece = board[pos.first][pos.second] ?: continue
            if (piece.special == SpecialType.NONE) continue

            val activation = activateSpecial(board, pos.first, pos.second, matchedType)
            pts += activation.points

            activation.clearedCells.forEach { cell ->
                if (allCleared.add(cell)) {
                    val newPiece = board[cell.first][cell.second]
                    if (newPiece != null && newPiece.special != SpecialType.NONE)
                        queue.add(Pair(cell, newPiece.type))
                }
            }
        }

        return allCleared to pts
    }

    // ── Spezialstein-Generierung ──────────────────────────────────────────────────

    private fun buildSpecialGens(
        matches: List<Match>,
        intersections: Set<Pair<Int, Int>>,
    ): List<Pair<Pair<Int, Int>, SpecialType>> {
        val result = mutableListOf<Pair<Pair<Int, Int>, SpecialType>>()
        matches.forEach { m ->
            val isLT = m.cells.any { it in intersections }
            when {
                m.cells.size >= 5 && !isLT ->
                    result.add(Pair(m.cells[m.cells.size / 2], SpecialType.PERLENKETTE))
                m.cells.size == 4 && !isLT -> {
                    val type = if (m.isHorizontal) SpecialType.GESTREIFT_H else SpecialType.GESTREIFT_V
                    result.add(Pair(m.cells[1], type))
                }
            }
        }
        intersections.forEach { cell ->
            result.add(Pair(cell, SpecialType.EINGEPACKT))
        }
        return result
    }

    // Wird vom ViewModel nach applyMatches() aufgerufen, um Spezialsteine zu setzen.
    fun placeSpecial(r: Int, c: Int, type: PieceType, special: SpecialType) {
        board[r][c] = PerlentaucherPiece(type, special)
    }

    // ── Schwerkraft ───────────────────────────────────────────────────────────────

    fun applyGravity(): Boolean {
        var changed = false
        for (c in 0 until BOARD_SIZE) {
            var writeRow = BOARD_SIZE - 1
            for (r in BOARD_SIZE - 1 downTo 0) {
                val piece = board[r][c]
                if (piece != null) {
                    if (r != writeRow) {
                        board[writeRow][c] = piece
                        board[r][c] = null
                        changed = true
                    }
                    writeRow--
                }
            }
        }
        if (!changed) phase = BoardPhase.FILLING
        return changed
    }

    // ── Auffüllen ─────────────────────────────────────────────────────────────────

    fun fillBoard(): Boolean {
        var filled = false
        for (r in 0 until BOARD_SIZE) {
            for (c in 0 until BOARD_SIZE) {
                if (board[r][c] == null) {
                    board[r][c] = randomPiece()
                    filled = true
                }
            }
        }
        phase = BoardPhase.MATCHING
        return filled
    }

    // ── Deadlock ──────────────────────────────────────────────────────────────────

    fun hasValidMove(): Boolean {
        for (r in 0 until BOARD_SIZE) {
            for (c in 0 until BOARD_SIZE) {
                if (c + 1 < BOARD_SIZE && wouldCreateMatch(r, c, r, c + 1)) return true
                if (r + 1 < BOARD_SIZE && wouldCreateMatch(r, c, r + 1, c)) return true
            }
        }
        return false
    }

    private fun wouldCreateMatch(r1: Int, c1: Int, r2: Int, c2: Int): Boolean {
        val tmp = board[r1][c1]
        board[r1][c1] = board[r2][c2]
        board[r2][c2] = tmp
        val result = findMatches().isNotEmpty()
        board[r2][c2] = board[r1][c1]
        board[r1][c1] = tmp
        return result
    }

    fun checkDeadlock() {
        phase = if (hasValidMove()) BoardPhase.IDLE else BoardPhase.SHUFFLE
    }

    // ── Shuffle ───────────────────────────────────────────────────────────────────

    fun shuffle() {
        val pieces = (0 until BOARD_SIZE).flatMap { r ->
            (0 until BOARD_SIZE).mapNotNull { c -> board[r][c] }
        }.toMutableList()
        pieces.shuffle(rng)
        var idx = 0
        for (r in 0 until BOARD_SIZE) {
            for (c in 0 until BOARD_SIZE) {
                board[r][c] = if (idx < pieces.size) pieces[idx++] else randomPiece()
            }
        }
        phase = BoardPhase.CHECK_DEADLOCK
    }

    // ── Serialisierung ────────────────────────────────────────────────────────────

    fun boardToIntArray(): IntArray = IntArray(BOARD_SIZE * BOARD_SIZE) { i ->
        board[i / BOARD_SIZE][i % BOARD_SIZE]?.toInt() ?: -1
    }

    fun loadFromIntArray(arr: IntArray) {
        arr.forEachIndexed { i, v ->
            val r = i / BOARD_SIZE
            val c = i % BOARD_SIZE
            board[r][c] = if (v == -1) null else PerlentaucherPiece.fromInt(v)
        }
    }
}
