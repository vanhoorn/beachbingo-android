package com.bestfriends.beachbingo.feature.strandturm.ui

import com.bestfriends.beachbingo.core.audio.BaseChiptuneAudioManager

internal class StrandturmAudioManager : BaseChiptuneAudioManager() {

    override fun musicAssetName(): String = "strandturm.ogg"

    // ── Sound cache ──────────────────────────────────────────────────────────

    override fun buildSoundCache() {
        soundCache["jump"] = squareWave(280.0, 0.2, 0.22, 0.001, 560.0)

        soundCache["land"] = noise(0.06, 0.5, 0.001)

        soundCache["climb"] = squareWave(900.0, 0.04, 0.07, 0.001)

        soundCache["coconut_bounce"] = sineWave(160.0, 0.22, 0.28, 0.001, 70.0)

        soundCache["hit"] = mix(
            noise(0.35, 0.45, 0.001),
            squareWave(440.0, 0.5, 0.25, 0.001, 55.0),
        )

        // Overlapping note sequences — match Web Audio API scheduled playback.
        var lifeLost = ShortArray(0)
        for ((idx, freq) in listOf(400.0, 350.0, 300.0, 220.0).withIndex())
            lifeLost = overlayAt(lifeLost, squareWave(freq, 0.12, 0.22, 0.001), (sampleRate * idx * 0.1).toInt())
        soundCache["life_lost"] = lifeLost

        var lvlComplete = ShortArray(0)
        for ((idx, freq) in listOf(261.0, 329.0, 392.0, 523.0, 659.0, 784.0).withIndex())
            lvlComplete = overlayAt(lvlComplete, squareWave(freq, 0.15, 0.2, 0.001), (sampleRate * idx * 0.1).toInt())
        soundCache["level_complete"] = lvlComplete

        var gameOver = ShortArray(0)
        for ((idx, freq) in listOf(440.0, 392.0, 349.0, 294.0, 261.0, 196.0).withIndex())
            gameOver = overlayAt(gameOver, squareWave(freq, 0.22, 0.2, 0.001), (sampleRate * idx * 0.18).toInt())
        soundCache["game_over"] = gameOver

        soundCache["timer_tick"] = squareWave(1400.0, 0.05, 0.1, 0.001)

        var bonus = ShortArray(0)
        for ((idx, freq) in listOf(523.0, 659.0, 784.0).withIndex())
            bonus = overlayAt(bonus, squareWave(freq, 0.1, 0.18, 0.001), (sampleRate * idx * 0.07).toInt())
        soundCache["bonus"] = bonus
    }

    // ── Music ────────────────────────────────────────────────────────────────

    // Note sequences: frequency_hz to duration_s, 0.0 = rest. Mirrors AudioManager.ts MELODIES.
    private val melodyNotes = listOf(
        659.0 to 0.125, 784.0 to 0.125, 659.0 to 0.125, 784.0 to 0.125,
        659.0 to 0.125, 587.0 to 0.125, 659.0 to 0.25,
        0.0   to 0.125,
        523.0 to 0.125, 659.0 to 0.125, 784.0 to 0.125, 523.0 to 0.25,
        0.0   to 0.375,
        440.0 to 0.125, 523.0 to 0.125, 659.0 to 0.125, 440.0 to 0.125,
        392.0 to 0.125, 440.0 to 0.125, 494.0 to 0.125, 0.0   to 0.125,
        523.0 to 0.375, 0.0   to 0.125,
        392.0 to 0.125, 440.0 to 0.125, 494.0 to 0.125, 523.0 to 0.125,
        587.0 to 0.125, 659.0 to 0.25,  0.0   to 0.125,
        784.0 to 0.125, 659.0 to 0.125, 587.0 to 0.125, 523.0 to 0.125,
        494.0 to 0.125, 440.0 to 0.25,  0.0   to 0.25,
    )

    private val bassNotes = listOf(
        130.0 to 0.5, 0.0 to 0.5, 146.0 to 0.5, 0.0 to 0.5,
        130.0 to 0.5, 0.0 to 0.5, 130.0 to 1.0,
    )

    override fun buildMelodyPcm(): ShortArray {
        var melody = ShortArray(0)
        for ((freq, dur) in melodyNotes)
            melody = concat(melody, if (freq > 0.0) squareWave(freq, dur, 0.07, 0.001) else silence(dur))
        var bass = ShortArray(0)
        for ((freq, dur) in bassNotes)
            bass = concat(bass, if (freq > 0.0) triWave(freq, dur, 0.04, 0.001) else silence(dur))
        return mix(melody, bass)
    }
}
