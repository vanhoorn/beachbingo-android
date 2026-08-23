package com.bestfriends.beachbingo.feature.pirates.ui

import com.bestfriends.beachbingo.core.audio.BaseChiptuneAudioManager

internal class PiratesAudioManager : BaseChiptuneAudioManager() {

    override fun musicAssetName(): String = "pirates.ogg"

    // ── Sound cache ──────────────────────────────────────────────────────────

    override fun buildSoundCache() {
        // Pew! — high-pitched laser shot
        soundCache["shoot"] = squareWave(1200.0, 0.08, 0.18, 0.001, 600.0)

        // Enemy explodes — quick noise burst
        soundCache["enemy_hit"] = mix(
            noise(0.12, 0.5, 0.001),
            squareWave(200.0, 0.1, 0.2, 0.001, 60.0),
        )

        // Player takes hit — heavy crash
        soundCache["player_hit"] = mix(
            noise(0.28, 0.55, 0.001),
            squareWave(350.0, 0.35, 0.22, 0.001, 55.0),
        )

        // Wave cleared — ascending victory arpeggio
        var waveDone = ShortArray(0)
        for ((idx, freq) in listOf(330.0, 415.0, 523.0, 659.0, 784.0).withIndex())
            waveDone = overlayAt(waveDone, squareWave(freq, 0.1, 0.18, 0.001), (sampleRate * idx * 0.08).toInt())
        soundCache["wave_complete"] = waveDone

        // Game over — descending minor sequence
        var go = ShortArray(0)
        for ((idx, freq) in listOf(440.0, 370.0, 330.0, 262.0, 196.0).withIndex())
            go = overlayAt(go, squareWave(freq, 0.2, 0.2, 0.001), (sampleRate * idx * 0.16).toInt())
        soundCache["game_over"] = go
    }

    // ── Melody — Cinematic pirate adventure (A harmonic minor, ~76 BPM) ──────
    // Majestic quarter notes, dramatic leaps, G# for harmonic-minor tension

    private val melodyNotes = listOf(
        // ─ SECTION A: The Main Theme ─
        // Phrase 1 – powerful opening, stepwise with a dramatic edge
        440.0 to 0.79, 523.25 to 0.39, 587.33 to 0.39, 523.25 to 0.79, 0.0 to 0.79,
        440.0 to 0.39, 415.30 to 0.39, 440.0 to 0.79, 493.88 to 0.39, 523.25 to 0.39, 0.0 to 0.79,
        // Phrase 2 – leap to high E, then winding descent
        659.25 to 0.79, 0.0 to 0.39, 587.33 to 0.39, 523.25 to 0.39, 493.88 to 0.39, 440.0 to 0.79,
        415.30 to 0.39, 440.0 to 0.39, 523.25 to 0.39, 659.25 to 0.39, 698.46 to 0.39, 659.25 to 0.39, 587.33 to 0.39, 0.0 to 0.39,
        // ─ SECTION B: March to Battle ─
        // Phrase 3 – march rhythm, rising tension
        523.25 to 0.39, 0.0 to 0.39, 523.25 to 0.39, 0.0 to 0.39, 659.25 to 0.39, 0.0 to 0.39, 784.0 to 0.79,
        // Phrase 4 – climax on high A, triumphant descent
        880.0 to 0.79, 784.0 to 0.39, 698.46 to 0.39, 659.25 to 0.39, 587.33 to 0.39, 523.25 to 0.39, 440.0 to 0.39,
        // Phrase 5 – final resolution to tonic
        440.0 to 0.39, 523.25 to 0.39, 659.25 to 0.79, 587.33 to 0.39, 523.25 to 0.39, 440.0 to 0.79,
        415.30 to 0.39, 440.0 to 0.39, 523.25 to 0.39, 493.88 to 0.39, 440.0 to 1.58, 0.0 to 0.79,
    )

    private val bassNotes = listOf(
        // Cinematic bass – Am/E/D/G heavy downbeats
        110.0 to 0.79, 0.0 to 0.39, 110.0 to 0.39, 0.0 to 0.79, 110.0 to 0.79,
        82.41 to 0.79, 0.0 to 0.79, 82.41 to 0.79, 0.0 to 0.79,
        73.42 to 0.79, 0.0 to 0.79, 73.42 to 0.79, 0.0 to 0.79,
        98.0  to 0.79, 0.0 to 0.79, 82.41 to 0.79, 0.0 to 0.79,
        110.0 to 0.39, 0.0 to 0.39, 110.0 to 0.39, 0.0 to 0.39, 110.0 to 0.79, 0.0 to 0.79,
        87.31 to 0.79, 0.0 to 0.79, 82.41 to 0.79, 0.0 to 0.79,
        110.0 to 0.39, 0.0 to 0.39, 110.0 to 0.79, 110.0 to 0.79, 0.0 to 0.79,
        55.0 to 1.58, 0.0 to 0.79, 55.0 to 0.79, 0.0 to 0.79,
    )

    override fun buildMelodyPcm(): ShortArray {
        var melody = ShortArray(0)
        for ((freq, dur) in melodyNotes)
            melody = concat(melody, if (freq > 0.0) squareWave(freq, dur, 0.042, 0.001) else silence(dur))
        var bass = ShortArray(0)
        for ((freq, dur) in bassNotes)
            bass = concat(bass, if (freq > 0.0) triWave(freq, dur, 0.048, 0.001) else silence(dur))
        return mix(melody, bass)
    }
}
