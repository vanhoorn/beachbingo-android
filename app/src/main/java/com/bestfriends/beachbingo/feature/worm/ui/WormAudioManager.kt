package com.bestfriends.beachbingo.feature.worm.ui

import com.bestfriends.beachbingo.core.audio.BaseChiptuneAudioManager

internal class WormAudioManager : BaseChiptuneAudioManager() {

    override fun musicAssetName(): String = "worm.ogg"

    // ── Sound cache ──────────────────────────────────────────────────────────

    override fun buildSoundCache() {
        // Eat food — satisfying "pop" (quick upward frequency jump)
        soundCache["eat"] = squareWave(440.0, 0.07, 0.22, 0.001, 880.0)

        // Eat rare food — more satisfying higher pop
        soundCache["eat_rare"] = squareWave(600.0, 0.1, 0.25, 0.001, 1200.0)

        // Death — crash + descending notes
        var die = ShortArray(0)
        die = mix(noise(0.25, 0.5, 0.001), die)
        for ((idx, freq) in listOf(330.0, 262.0, 196.0, 147.0).withIndex())
            die = overlayAt(die, squareWave(freq, 0.12, 0.15, 0.001), (sampleRate * (0.05 + idx * 0.1)).toInt())
        soundCache["die"] = die
    }

    // ── Melody — Funky electronic groove (D minor pentatonic, ~120 BPM) ──────
    // Syncopated dotted rhythms, punchy rests, worm dancing in the mud

    private val melodyNotes = listOf(
        // ─ SECTION A: The Groove ─
        // Phrase 1 – punchy riff with breathing room
        0.0 to 0.25, 293.66 to 0.375, 293.66 to 0.125, 0.0 to 0.25, 440.0 to 0.25, 0.0 to 0.25, 523.25 to 0.50,
        440.0 to 0.25, 0.0 to 0.25, 392.0 to 0.375, 349.23 to 0.125, 0.0 to 0.25, 349.23 to 0.25, 293.66 to 0.50,
        // Phrase 2 – variation, higher note appears
        0.0 to 0.25, 293.66 to 0.25, 440.0 to 0.25, 0.0 to 0.25, 523.25 to 0.25, 440.0 to 0.25, 392.0 to 0.50,
        0.0 to 0.25, 587.33 to 0.25, 0.0 to 0.25, 523.25 to 0.25, 440.0 to 0.50, 0.0 to 0.50,
        // ─ SECTION B: Call & Response ─
        // Phrase 3 – high punchy hits, offbeat emphasis
        0.0 to 0.375, 523.25 to 0.125, 0.0 to 0.25, 523.25 to 0.25, 0.0 to 0.25, 587.33 to 0.25, 523.25 to 0.50,
        440.0 to 0.25, 0.0 to 0.25, 440.0 to 0.25, 0.0 to 0.25, 392.0 to 0.25, 349.23 to 0.25, 293.66 to 0.50,
        // Phrase 4 – climax run, big landing
        349.23 to 0.25, 392.0 to 0.25, 440.0 to 0.25, 523.25 to 0.25, 587.33 to 0.25, 523.25 to 0.25, 440.0 to 0.50,
        0.0 to 0.25, 293.66 to 0.375, 440.0 to 0.125, 293.66 to 1.50, 0.0 to 0.50,
    )

    private val bassNotes = listOf(
        // Funky bass – D-groove with syncopation
        73.42 to 0.375, 73.42 to 0.125, 0.0 to 0.25, 73.42 to 0.25, 0.0 to 0.25, 73.42 to 0.50,
        98.0  to 0.375, 98.0  to 0.125, 0.0 to 0.25, 110.0 to 0.25, 0.0 to 0.25, 98.0 to 0.50,
        73.42 to 0.25, 0.0 to 0.25, 73.42 to 0.25, 0.0 to 0.25, 87.31 to 0.25, 73.42 to 0.25, 73.42 to 0.50,
        110.0 to 0.50, 0.0 to 0.50, 110.0 to 0.50, 0.0 to 0.50,
        73.42 to 0.375, 73.42 to 0.125, 0.0 to 0.25, 73.42 to 0.25, 0.0 to 0.25, 73.42 to 0.50,
        98.0  to 0.25, 0.0 to 0.25, 98.0 to 0.25, 0.0 to 0.25, 98.0 to 0.25, 110.0 to 0.25, 98.0 to 0.50,
        73.42 to 0.25, 0.0 to 0.25, 73.42 to 0.50, 0.0 to 0.25, 110.0 to 0.25, 98.0 to 0.25, 73.42 to 0.50,
        73.42 to 2.0, 0.0 to 0.50,
    )

    override fun buildMelodyPcm(): ShortArray {
        var melody = ShortArray(0)
        for ((freq, dur) in melodyNotes)
            melody = concat(melody, if (freq > 0.0) squareWave(freq, dur, 0.06, 0.001) else silence(dur))
        var bass = ShortArray(0)
        for ((freq, dur) in bassNotes)
            bass = concat(bass, if (freq > 0.0) triWave(freq, dur, 0.045, 0.001) else silence(dur))
        return mix(melody, bass)
    }
}
