package com.bestfriends.beachbingo.feature.pong.ui

import com.bestfriends.beachbingo.core.audio.BaseChiptuneAudioManager

internal class PongAudioManager : BaseChiptuneAudioManager() {

    override fun musicAssetName(): String = "pong.ogg"

    // ── Sound cache ──────────────────────────────────────────────────────────

    override fun buildSoundCache() {
        // Ball hits paddle — sharp "thwack" (tennis ball impact)
        // Mix of short noise burst + mid-frequency sine sweep
        soundCache["ball_hit"] = mix(
            noise(0.035, 0.45, 0.001),
            sineWave(800.0, 0.05, 0.3, 0.001, 400.0),
        )

        // Ball hits wall — slightly different thump
        soundCache["wall_hit"] = mix(
            noise(0.025, 0.35, 0.001),
            sineWave(550.0, 0.04, 0.25, 0.001, 300.0),
        )

        // Score point — quick upward ding
        soundCache["score"] = concat(
            squareWave(660.0, 0.08, 0.2, 0.001),
            silence(0.03),
            squareWave(880.0, 0.1, 0.2, 0.001),
        )

        // Game over / match won
        var win = ShortArray(0)
        for ((idx, freq) in listOf(523.0, 659.0, 784.0, 1047.0).withIndex())
            win = overlayAt(win, squareWave(freq, 0.15, 0.18, 0.001), (sampleRate * idx * 0.12).toInt())
        soundCache["win"] = win
    }

    // ── Melody — Electronic synthwave drive (D minor, ~128 BPM) ─────────────
    // Fast 8th-note pulse, driving arpeggios, urgent competitive energy

    private val melodyNotes = listOf(
        // ─ SECTION A: Driving Arpeggio ─
        // Phrase 1 – arpeggio launch
        293.66 to 0.23, 440.0 to 0.23, 523.25 to 0.23, 587.33 to 0.23, 523.25 to 0.47, 0.0 to 0.23, 587.33 to 0.23,
        523.25 to 0.23, 0.0 to 0.23, 440.0 to 0.47, 0.0 to 0.23, 392.0 to 0.23, 349.23 to 0.47, 0.0 to 0.23,
        // Phrase 2 – stepping descent
        587.33 to 0.23, 0.0 to 0.23, 523.25 to 0.47, 0.0 to 0.23, 440.0 to 0.47, 0.0 to 0.23, 392.0 to 0.23,
        349.23 to 0.23, 0.0 to 0.23, 440.0 to 0.23, 349.23 to 0.47, 293.66 to 0.47, 0.0 to 0.47,
        // ─ SECTION B: Melodic Hook ─
        // Phrase 3 – punchy synth hook
        0.0 to 0.23, 523.25 to 0.23, 0.0 to 0.23, 587.33 to 0.47, 0.0 to 0.23, 523.25 to 0.23, 440.0 to 0.23,
        0.0 to 0.23, 440.0 to 0.23, 0.0 to 0.23, 392.0 to 0.47, 349.23 to 0.47, 0.0 to 0.47,
        // Phrase 4 – climbing peak then resolve to tonic
        349.23 to 0.23, 392.0 to 0.23, 440.0 to 0.23, 523.25 to 0.23, 587.33 to 0.23, 659.25 to 0.23, 587.33 to 0.47,
        523.25 to 0.23, 440.0 to 0.23, 392.0 to 0.23, 349.23 to 0.23, 293.66 to 0.94, 0.0 to 0.47,
    )

    private val bassNotes = listOf(
        // Electronic bass – driving Dm vamp
        293.66 to 0.23, 0.0 to 0.23, 293.66 to 0.47, 0.0 to 0.23, 220.0 to 0.47, 0.0 to 0.23,
        174.61 to 0.23, 0.0 to 0.23, 174.61 to 0.47, 0.0 to 0.23, 261.63 to 0.47, 0.0 to 0.23,
        196.0 to 0.23,  0.0 to 0.23, 196.0 to 0.47,  0.0 to 0.23, 196.0 to 0.47,  0.0 to 0.23,
        130.81 to 0.47, 0.0 to 0.23, 220.0 to 0.47,  0.0 to 0.23, 146.83 to 0.47, 0.0 to 0.23,
        293.66 to 0.23, 0.0 to 0.23, 293.66 to 0.47, 0.0 to 0.23, 220.0 to 0.47,  0.0 to 0.23,
        174.61 to 0.23, 0.0 to 0.23, 174.61 to 0.47, 0.0 to 0.23, 261.63 to 0.47, 0.0 to 0.23,
        196.0 to 0.23,  0.0 to 0.23, 196.0 to 0.23,  146.83 to 0.23, 196.0 to 0.23, 220.0 to 0.23, 196.0 to 0.47,
        73.42 to 0.94,  0.0 to 0.47, 73.42 to 0.47,  0.0 to 0.47,
    )

    override fun buildMelodyPcm(): ShortArray {
        var melody = ShortArray(0)
        for ((freq, dur) in melodyNotes)
            melody = concat(melody, if (freq > 0.0) squareWave(freq, dur, 0.055, 0.001) else silence(dur))
        var bass = ShortArray(0)
        for ((freq, dur) in bassNotes)
            bass = concat(bass, if (freq > 0.0) squareWave(freq, dur, 0.04, 0.001) else silence(dur))
        return mix(melody, bass)
    }
}
