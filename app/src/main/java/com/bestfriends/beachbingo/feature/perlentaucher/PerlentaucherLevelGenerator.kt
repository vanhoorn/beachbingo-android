package com.bestfriends.beachbingo.feature.perlentaucher

object PerlentaucherLevelGenerator {

    data class LevelConfig(
        val level: Int,
        val seed: Long,
        val movesLeft: Int,
        val targetScore: Int,
    )

    fun generate(level: Int): LevelConfig {
        val movesLeft = movesForLevel(level)
        val targetScore = scoreTargetForLevel(level)

        var seed = baseSeedForLevel(level)
        for (attempt in 0 until 10) {
            val model = PerlentaucherBoardModel(seed)
            if (model.hasValidMove()) {
                return LevelConfig(level, seed, movesLeft, targetScore)
            }
            seed = baseSeedForLevel(level) + (attempt + 1) * 31337L
        }

        // Fallback: last seed (statistically, all 10 attempts having no valid move is impossible)
        return LevelConfig(level, seed, movesLeft, targetScore)
    }

    private fun baseSeedForLevel(level: Int): Long = level.toLong() * 7919L + 12345L

    private fun movesForLevel(level: Int): Int = when {
        level <= 20  -> 35
        level <= 40  -> 30
        level <= 60  -> 25
        level <= 100 -> 20
        else         -> 18
    }

    // Plateau at level 60: target stays at 12 500 from there on
    private fun scoreTargetForLevel(level: Int): Int =
        if (level >= 60) 12_500
        else 600 + (level - 1) * 200
}
