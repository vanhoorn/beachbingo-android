package com.bestfriends.beachbingo.core.model

data class BingoCard(
    val grid: List<List<Int>> = emptyList(),
    val markedNumbers: Set<Int> = emptySet()
) {
    fun hasCompletedRow(): Boolean {
        return grid.any { row -> row.all { it in markedNumbers } }
    }

    fun hasCompletedColumn(): Boolean {
        return (0 until 5).any { colIndex ->
            grid.all { row -> row[colIndex] in markedNumbers }
        }
    }

    fun hasCompletedDiagonal(): Boolean {
        val mainDiagonal = (0 until 5).all { i -> grid[i][i] in markedNumbers }
        val antiDiagonal = (0 until 5).all { i -> grid[i][4 - i] in markedNumbers }
        return mainDiagonal || antiDiagonal
    }

    fun hasBingo(): Boolean {
        return hasCompletedRow() || hasCompletedColumn() || hasCompletedDiagonal()
    }
}
