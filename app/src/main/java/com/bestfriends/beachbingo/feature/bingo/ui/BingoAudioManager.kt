package com.bestfriends.beachbingo.feature.bingo.ui

import com.bestfriends.beachbingo.core.audio.BaseChiptuneAudioManager

internal class BingoAudioManager : BaseChiptuneAudioManager() {

    // ── Sound cache ──────────────────────────────────────────────────────────

    override fun buildSoundCache() {
        // Number drawn — gentle "ding" (clear sine ping)
        soundCache["number_drawn"] = sineWave(880.0, 0.18, 0.25, 0.001)

        // Drum roll — noise burst (for DRUM draw style)
        soundCache["drum_roll"] = mix(
            noise(0.4, 0.35, 0.001),
            squareWave(120.0, 0.4, 0.15, 0.001),
        )

        // BINGO! — triumphant ascending fanfare
        var bingo = ShortArray(0)
        for ((idx, freq) in listOf(523.0, 659.0, 784.0, 1047.0, 1319.0).withIndex())
            bingo = overlayAt(bingo, squareWave(freq, 0.18, 0.22, 0.001), (sampleRate * idx * 0.14).toInt())
        soundCache["bingo"] = bingo

        // Mark number — subtle tick
        soundCache["mark"] = sineWave(660.0, 0.06, 0.15, 0.001)

        // Elimination — descending whoosh
        var elim = ShortArray(0)
        for ((idx, freq) in listOf(440.0, 370.0, 294.0, 220.0).withIndex())
            elim = overlayAt(elim, squareWave(freq, 0.15, 0.18, 0.001), (sampleRate * idx * 0.12).toInt())
        soundCache["elimination"] = elim
    }

    // ── Melody — Caribbean calypso party (F major, ~96 BPM) ─────────────────
    // Warm sineWave for steel-drum feel: syncopated, sunny, offbeat emphasis

    private val melodyNotes = listOf(
        // ─ SECTION A: Calypso Sunshine ─
        // Phrase 1 – offbeat groove ascending
        0.0 to 0.31, 440.0 to 0.31, 0.0 to 0.31, 523.25 to 0.63, 466.16 to 0.31, 440.0 to 0.63,
        392.0 to 0.31, 440.0 to 0.31, 392.0 to 0.63, 349.23 to 1.25, 0.0 to 0.31,
        // Phrase 2 – climbing variation
        440.0 to 0.31, 466.16 to 0.31, 523.25 to 0.63, 466.16 to 0.31, 523.25 to 0.31, 466.16 to 0.63,
        440.0 to 0.31, 392.0 to 0.31, 349.23 to 1.88, 0.0 to 0.63,
        // ─ SECTION B: Syncopated Energy ─
        // Phrase 3 – higher register punch
        0.0 to 0.31, 698.46 to 0.31, 659.25 to 0.31, 587.33 to 0.31, 698.46 to 0.63, 0.0 to 0.31, 587.33 to 0.31,
        523.25 to 0.31, 0.0 to 0.31, 466.16 to 0.63, 0.0 to 0.31, 523.25 to 0.31, 466.16 to 0.63,
        // Phrase 4 – descending run, resolution to tonic
        523.25 to 0.31, 466.16 to 0.31, 440.0 to 0.31, 392.0 to 0.31, 440.0 to 0.31, 392.0 to 0.31, 349.23 to 0.63,
        440.0 to 0.31, 392.0 to 0.31, 349.23 to 2.50, 0.0 to 0.63,
    )

    private val bassNotes = listOf(
        // Calypso bass – syncopated roots (F major)
        174.61 to 0.63, 0.0 to 0.31, 130.81 to 0.31, 174.61 to 0.63, 0.0 to 0.63,
        196.0 to 0.63, 0.0 to 0.63, 174.61 to 0.63, 0.0 to 0.63,
        174.61 to 0.63, 0.0 to 0.31, 233.08 to 0.31, 174.61 to 0.63, 220.0 to 0.63, 0.0 to 0.63,
        130.81 to 0.63, 0.0 to 0.63, 174.61 to 0.63, 130.81 to 1.88, 0.0 to 0.63,
        174.61 to 0.31, 0.0 to 0.31, 174.61 to 0.31, 0.0 to 0.31, 174.61 to 0.63, 0.0 to 0.63,
        233.08 to 0.63, 220.0 to 0.63, 196.0 to 0.63, 0.0 to 0.63,
        174.61 to 0.63, 0.0 to 0.63, 196.0 to 0.63, 0.0 to 0.63,
        130.81 to 0.63, 174.61 to 0.63, 130.81 to 2.50, 0.0 to 0.63,
    )

    override fun buildMelodyPcm(): ShortArray {
        var melody = ShortArray(0)
        for ((freq, dur) in melodyNotes)
            melody = concat(melody, if (freq > 0.0) sineWave(freq, dur, 0.06, 0.001) else silence(dur))
        var bass = ShortArray(0)
        for ((freq, dur) in bassNotes)
            bass = concat(bass, if (freq > 0.0) triWave(freq, dur, 0.04, 0.001) else silence(dur))
        return mix(melody, bass)
    }
}
