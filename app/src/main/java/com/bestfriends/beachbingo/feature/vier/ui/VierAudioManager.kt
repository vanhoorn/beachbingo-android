package com.bestfriends.beachbingo.feature.vier.ui

import com.bestfriends.beachbingo.core.audio.BaseChiptuneAudioManager

internal class VierAudioManager(ctx: android.content.Context) : BaseChiptuneAudioManager(ctx) {

    override fun musicAssetName(): String = "vier.ogg"

    // ── Sound cache ──────────────────────────────────────────────────────────

    override fun buildSoundCache() {
        // Piece drops and hits the bottom — wooden clack
        // Short noise burst (wood impact) + low sine thud
        soundCache["piece_drop"] = mix(
            noise(0.05, 0.6, 0.001),
            sineWave(180.0, 0.12, 0.35, 0.001),
        )

        // Connect four — triumphant ascending arpeggio
        var connect = ShortArray(0)
        for ((idx, freq) in listOf(261.0, 329.0, 392.0, 523.0, 659.0).withIndex())
            connect = overlayAt(connect, squareWave(freq, 0.14, 0.2, 0.001), (sampleRate * idx * 0.1).toInt())
        soundCache["win"] = connect

        // Draw — neutral two-tone
        soundCache["draw"] = concat(
            squareWave(440.0, 0.15, 0.18, 0.001),
            silence(0.04),
            squareWave(370.0, 0.15, 0.18, 0.001),
        )
    }

    // ── Melody — German folk / Wirtshaus (G major, ~104 BPM) ────────────────
    // Mellow triWave for acoustic feel, singable melody, comfortable pub tempo

    private val melodyNotes = listOf(
        // ─ SECTION A: Opening Folk Theme ─
        // Phrase 1 – stepwise climb, clear and friendly
        392.0 to 0.58, 440.0 to 0.29, 493.88 to 0.29, 523.25 to 0.58, 0.0 to 0.58,
        587.33 to 0.29, 523.25 to 0.29, 493.88 to 0.58, 0.0 to 0.29, 440.0 to 0.29, 440.0 to 0.58,
        // Phrase 2 – ornamented variation, steps back down
        392.0 to 0.29, 440.0 to 0.29, 493.88 to 0.58, 523.25 to 0.29, 493.88 to 0.29, 440.0 to 0.58,
        392.0 to 1.15, 0.0 to 0.58, 392.0 to 0.58,
        // ─ SECTION B: Call & Response ─
        // Phrase 3 – call: scale run upward
        440.0 to 0.29, 493.88 to 0.29, 523.25 to 0.29, 587.33 to 0.29, 659.25 to 0.58, 0.0 to 0.58,
        587.33 to 0.29, 523.25 to 0.29, 493.88 to 0.58, 440.0 to 0.29, 493.88 to 0.29, 392.0 to 0.58,
        // Phrase 4 – response: high flourish, settle to tonic
        659.25 to 0.29, 0.0 to 0.29, 784.0 to 0.29, 0.0 to 0.29, 659.25 to 0.29, 587.33 to 0.29, 523.25 to 0.58,
        523.25 to 0.29, 493.88 to 0.29, 440.0 to 0.29, 392.0 to 0.29, 440.0 to 0.58, 0.0 to 0.29,
        // Phrase 5 – gentle cadence home
        493.88 to 0.58, 440.0 to 0.58, 392.0 to 1.15, 0.0 to 1.15,
    )

    private val bassNotes = listOf(
        // Folk bass – G major with walking motion
        196.0 to 0.58, 0.0 to 0.58, 196.0 to 0.58, 0.0 to 0.58,
        146.83 to 0.58, 0.0 to 0.58, 146.83 to 0.58, 0.0 to 0.58,
        196.0 to 0.58, 0.0 to 0.29, 196.0 to 0.29, 0.0 to 0.29, 220.0 to 0.29, 196.0 to 0.58,
        98.0 to 1.15, 0.0 to 0.58, 98.0 to 0.58,
        220.0 to 0.58, 0.0 to 0.58, 220.0 to 0.58, 0.0 to 0.58,
        146.83 to 0.58, 0.0 to 0.58, 164.81 to 0.58, 0.0 to 0.58,
        196.0 to 0.29, 0.0 to 0.29, 196.0 to 0.29, 0.0 to 0.29, 196.0 to 0.58, 0.0 to 0.58,
        146.83 to 0.29, 0.0 to 0.29, 130.81 to 0.29, 0.0 to 0.29, 146.83 to 0.29, 164.81 to 0.29, 196.0 to 0.58,
        98.0 to 1.15, 0.0 to 1.15,
    )

    override fun buildMelodyPcm(): ShortArray {
        var melody = ShortArray(0)
        for ((freq, dur) in melodyNotes)
            melody = concat(melody, if (freq > 0.0) triWave(freq, dur, 0.065, 0.001) else silence(dur))
        var bass = ShortArray(0)
        for ((freq, dur) in bassNotes)
            bass = concat(bass, if (freq > 0.0) sineWave(freq, dur, 0.045, 0.001) else silence(dur))
        return mix(melody, bass)
    }
}
