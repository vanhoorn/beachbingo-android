package com.bestfriends.beachbingo.feature.perlentaucher

data class SpecialActivation(
    val clearedCells: Set<Pair<Int, Int>>,
    val points: Int,
)

enum class ComboType {
    CROSS,         // GESTREIFT_H + GESTREIFT_V → gesamte Zeile + Spalte
    TRIPLE_SWEEP,  // GESTREIFT + EINGEPACKT   → 3 parallele Zeilen oder Spalten
    AREA_BLAST,    // EINGEPACKT + EINGEPACKT  → 5×5 Bereich
}

// Gibt zurück, welche Zellen freizuräumen sind + flache Punktzahl (kein Kaskaden-Faktor).
// Modifiziert das Board NICHT.
fun activateSpecial(
    board: Array<Array<PerlentaucherPiece?>>,
    r: Int,
    c: Int,
    matchedType: PieceType?,
): SpecialActivation {
    val piece = board[r][c] ?: return SpecialActivation(emptySet(), 0)
    val cleared = mutableSetOf<Pair<Int, Int>>()

    return when (piece.special) {
        SpecialType.GESTREIFT_H -> {
            (0 until BOARD_SIZE).forEach { col ->
                if (board[r][col] != null) cleared.add(Pair(r, col))
            }
            SpecialActivation(cleared, 200)
        }
        SpecialType.GESTREIFT_V -> {
            (0 until BOARD_SIZE).forEach { row ->
                if (board[row][c] != null) cleared.add(Pair(row, c))
            }
            SpecialActivation(cleared, 200)
        }
        SpecialType.EINGEPACKT -> {
            for (dr in -1..1) {
                for (dc in -1..1) {
                    val nr = r + dr; val nc = c + dc
                    if (nr in 0 until BOARD_SIZE && nc in 0 until BOARD_SIZE && board[nr][nc] != null)
                        cleared.add(Pair(nr, nc))
                }
            }
            SpecialActivation(cleared, 300)
        }
        SpecialType.PERLENKETTE -> {
            // Bei Ketten-Aktivierung: eigenen Typ räumen; bei direktem Match: matchedType
            val targetType = matchedType ?: piece.type
            for (row in 0 until BOARD_SIZE) {
                for (col in 0 until BOARD_SIZE) {
                    if (board[row][col]?.type == targetType) cleared.add(Pair(row, col))
                }
            }
            SpecialActivation(cleared, 500)
        }
        SpecialType.NONE -> SpecialActivation(emptySet(), 0)
    }
}

// Erkennt die Kombination zweier Spezialsteine (NONE → kein Combo).
fun detectCombo(s1: SpecialType, s2: SpecialType): ComboType? {
    if (s1 == SpecialType.NONE || s2 == SpecialType.NONE) return null
    val both = setOf(s1, s2)
    return when {
        both == setOf(SpecialType.GESTREIFT_H, SpecialType.GESTREIFT_V) -> ComboType.CROSS
        both.contains(SpecialType.EINGEPACKT) &&
            (both.contains(SpecialType.GESTREIFT_H) || both.contains(SpecialType.GESTREIFT_V)) ->
            ComboType.TRIPLE_SWEEP
        s1 == SpecialType.EINGEPACKT && s2 == SpecialType.EINGEPACKT -> ComboType.AREA_BLAST
        else -> null
    }
}

// Führt eine Kombo-Aktivierung durch. Modifiziert das Board NICHT.
fun activateCombo(
    board: Array<Array<PerlentaucherPiece?>>,
    r1: Int, c1: Int,
    r2: Int, c2: Int,
    combo: ComboType,
): SpecialActivation {
    val cleared = mutableSetOf<Pair<Int, Int>>()
    val pts: Int

    when (combo) {
        ComboType.CROSS -> {
            // Komplette Zeile r1 + komplette Spalte c1
            (0 until BOARD_SIZE).forEach { i ->
                if (board[r1][i] != null) cleared.add(Pair(r1, i))
                if (board[i][c1] != null) cleared.add(Pair(i, c1))
            }
            pts = 400
        }
        ComboType.TRIPLE_SWEEP -> {
            val p1Special = board[r1][c1]?.special ?: SpecialType.NONE
            val isP1Gestreift = p1Special == SpecialType.GESTREIFT_H || p1Special == SpecialType.GESTREIFT_V
            val gr = if (isP1Gestreift) r1 else r2
            val gc = if (isP1Gestreift) c1 else c2
            val gSpecial = if (isP1Gestreift) p1Special else board[r2][c2]?.special ?: SpecialType.NONE

            if (gSpecial == SpecialType.GESTREIFT_H) {
                for (dr in -1..1) {
                    val nr = gr + dr
                    if (nr in 0 until BOARD_SIZE)
                        (0 until BOARD_SIZE).forEach { col -> if (board[nr][col] != null) cleared.add(Pair(nr, col)) }
                }
            } else {
                for (dc in -1..1) {
                    val nc = gc + dc
                    if (nc in 0 until BOARD_SIZE)
                        (0 until BOARD_SIZE).forEach { row -> if (board[row][nc] != null) cleared.add(Pair(row, nc)) }
                }
            }
            pts = 600
        }
        ComboType.AREA_BLAST -> {
            // 5×5 um r1/c1
            for (dr in -2..2) {
                for (dc in -2..2) {
                    val nr = r1 + dr; val nc = c1 + dc
                    if (nr in 0 until BOARD_SIZE && nc in 0 until BOARD_SIZE && board[nr][nc] != null)
                        cleared.add(Pair(nr, nc))
                }
            }
            pts = 800
        }
    }

    return SpecialActivation(cleared, pts)
}
