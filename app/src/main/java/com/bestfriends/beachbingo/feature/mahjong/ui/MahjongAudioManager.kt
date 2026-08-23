package com.bestfriends.beachbingo.feature.mahjong.ui

import com.bestfriends.beachbingo.core.audio.BaseChiptuneAudioManager

internal class MahjongAudioManager(ctx: android.content.Context) : BaseChiptuneAudioManager(ctx) {

    override fun musicAssetName(): String = "mahjong.ogg"

    // ── Sound cache ──────────────────────────────────────────────────────────

    override fun buildSoundCache() {
        // Tile select: soft click
        soundCache["tile_select"] = squareBurst(880.0, 0.06, 0.1, 0.001)

        // Pair matched: ascending 2-tone
        var match = ShortArray(0)
        for ((idx, freq) in listOf(523.0, 659.0).withIndex())
            match = overlayAt(match, squareBurst(freq, 0.15, 0.1, 0.001), (sampleRate * idx * 0.08).toInt())
        soundCache["pair_match"] = match

        // Win: ascending fanfare
        var win = ShortArray(0)
        for ((idx, freq) in listOf(392.0, 523.0, 659.0, 784.0, 1047.0).withIndex())
            win = overlayAt(win, squareBurst(freq, 0.15, 0.18, 0.001), (sampleRate * idx * 0.1).toInt())
        soundCache["win"] = win

        // No moves / game over
        var over = ShortArray(0)
        for ((idx, freq) in listOf(440.0, 370.0, 294.0, 220.0).withIndex())
            over = overlayAt(over, squareBurst(freq, 0.12, 0.18, 0.001), (sampleRate * idx * 0.1).toInt())
        soundCache["game_over"] = over
    }

    // ── Music: Pentatonic zen, C pentatonic, ~80 BPM ────────────────────────

    private val melodyNotes = listOf(
        392.0 to 0.38, 440.0 to 0.38, 523.0 to 0.75, 440.0 to 0.38, 0.0 to 0.38,
        392.0 to 0.38, 329.0 to 0.38, 392.0 to 0.75, 0.0 to 0.75,
        440.0 to 0.38, 523.0 to 0.38, 587.0 to 0.75, 523.0 to 0.38, 440.0 to 0.38,
        392.0 to 1.50, 0.0 to 0.38,
        523.0 to 0.38, 440.0 to 0.38, 392.0 to 0.38, 329.0 to 0.38, 293.0 to 0.75, 0.0 to 0.38,
        329.0 to 0.38, 392.0 to 0.38, 440.0 to 0.38, 392.0 to 0.38, 329.0 to 0.75, 0.0 to 0.38,
        261.0 to 0.38, 293.0 to 0.38, 329.0 to 0.38, 392.0 to 0.38, 440.0 to 0.38, 392.0 to 0.38, 329.0 to 0.75,
        261.0 to 1.50, 0.0 to 0.75,
    )

    private val bassNotes = listOf(
        130.0 to 0.75, 0.0 to 0.38, 196.0 to 0.38, 0.0 to 0.75,
        130.0 to 0.75, 0.0 to 0.38, 110.0 to 0.38, 0.0 to 0.75,
        130.0 to 0.75, 0.0 to 0.38, 146.0 to 0.38, 0.0 to 0.75,
        130.0 to 1.50, 0.0 to 0.75,
        130.0 to 0.75, 0.0 to 0.38, 196.0 to 0.38, 0.0 to 0.75,
        98.0  to 0.75, 0.0 to 0.38, 110.0 to 0.38, 0.0 to 0.75,
        130.0 to 0.75, 0.0 to 0.38, 98.0  to 0.38, 0.0 to 0.75,
        130.0 to 1.50, 0.0 to 0.75,
    )

    override fun buildMelodyPcm(): ShortArray {
        var melody = ShortArray(0)
        for ((freq, dur) in melodyNotes)
            melody = concat(melody, if (freq > 0.0) sineNoteEnv(freq, dur, 0.038, 0.008, 0.050) else silence(dur))
        var bass = ShortArray(0)
        for ((freq, dur) in bassNotes)
            bass = concat(bass, if (freq > 0.0) triNoteEnv(freq, dur, 0.025, 0.008, 0.050) else silence(dur))
        return mix(melody, bass)
    }
}
