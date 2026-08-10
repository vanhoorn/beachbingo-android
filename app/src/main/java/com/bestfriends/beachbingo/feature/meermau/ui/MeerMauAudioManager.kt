package com.bestfriends.beachbingo.feature.meermau.ui

import com.bestfriends.beachbingo.core.audio.BaseChiptuneAudioManager

internal class MeerMauAudioManager : BaseChiptuneAudioManager() {

    // ── Sound cache ──────────────────────────────────────────────────────────

    override fun buildSoundCache() {
        soundCache["card_play"] = sineWave(440.0, 0.10, 0.18, 0.002, 220.0)

        soundCache["card_draw"] = noiseRiseAndFall(0.10, 0.38)

        var mau = squareWave(523.0, 0.12, 0.13, 0.001)
        mau = overlayAt(mau, squareWave(659.0, 0.13, 0.13, 0.001), (sampleRate * 0.13).toInt())
        soundCache["mau"] = mau

        var maumau = ShortArray(0)
        for ((idx, freq) in listOf(523.0, 659.0, 784.0, 1047.0).withIndex())
            maumau = overlayAt(maumau, squareWave(freq, 0.15, 0.20, 0.001), (sampleRate * idx * 0.13).toInt())
        soundCache["maumau"] = maumau

        var roundWin = ShortArray(0)
        for ((idx, freq) in listOf(523.0, 659.0, 784.0, 1047.0, 1319.0).withIndex())
            roundWin = overlayAt(roundWin, squareBurst(freq, 0.18, 0.14, 0.001), (sampleRate * idx * 0.11).toInt())
        soundCache["round_win"] = roundWin

        var gameWin = ShortArray(0)
        for ((idx, freq) in listOf(523.0, 659.0, 784.0, 1047.0, 784.0, 1047.0, 1319.0).withIndex())
            gameWin = overlayAt(gameWin, squareBurst(freq, 0.20, 0.15, 0.001), (sampleRate * idx * 0.12).toInt())
        soundCache["game_win"] = gameWin
    }

    // ── Music ────────────────────────────────────────────────────────────────

    // Playful card-game march in C major
    private val melodyNotes = listOf(
        659.0 to 0.25, 659.0 to 0.25, 587.0 to 0.25, 523.0 to 0.25,
        587.0 to 0.50, 0.0 to 0.25,
        659.0 to 0.25, 784.0 to 0.25, 784.0 to 0.25,
        880.0 to 0.50, 0.0 to 0.25,
        784.0 to 0.25, 698.0 to 0.25, 659.0 to 0.25, 587.0 to 0.25,
        659.0 to 0.75, 0.0 to 0.25,
        523.0 to 0.25, 659.0 to 0.25, 784.0 to 0.25,
        784.0 to 0.75, 0.0 to 0.50,
        880.0 to 0.25, 784.0 to 0.25, 698.0 to 0.25, 659.0 to 0.25,
        587.0 to 0.50, 0.0 to 0.25,
        523.0 to 0.25, 659.0 to 0.25, 784.0 to 0.25,
        698.0 to 0.25, 659.0 to 0.25, 523.0 to 0.75, 0.0 to 0.50,
    )

    private val bassNotes = listOf(
        130.0 to 0.50, 0.0 to 0.25, 196.0 to 0.50, 0.0 to 0.25,
        130.0 to 0.50, 0.0 to 0.25,
        130.0 to 0.50, 0.0 to 0.25, 196.0 to 0.50, 0.0 to 0.25,
        147.0 to 0.50, 0.0 to 0.25, 165.0 to 0.50, 0.0 to 0.25,
        130.0 to 0.75, 0.0 to 0.25,
        130.0 to 0.50, 0.0 to 0.25, 130.0 to 0.75, 0.0 to 0.50,
        165.0 to 0.50, 0.0 to 0.25, 196.0 to 0.50, 0.0 to 0.25,
        130.0 to 0.50, 0.0 to 0.25,
        130.0 to 0.50, 0.0 to 0.25, 196.0 to 0.50, 0.0 to 0.25,
        147.0 to 0.75, 0.0 to 0.25, 130.0 to 1.00, 0.0 to 0.50,
    )

    override fun buildMelodyPcm(): ShortArray {
        var melody = ShortArray(0)
        for ((freq, dur) in melodyNotes)
            melody = concat(melody, if (freq > 0.0) sineNoteEnv(freq, dur, 0.042, 0.005, 0.040) else silence(dur))
        var bass = ShortArray(0)
        for ((freq, dur) in bassNotes)
            bass = concat(bass, if (freq > 0.0) triNoteEnv(freq, dur, 0.026, 0.005, 0.040) else silence(dur))
        return mix(melody, bass)
    }
}
