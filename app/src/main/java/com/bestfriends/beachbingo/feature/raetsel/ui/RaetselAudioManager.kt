package com.bestfriends.beachbingo.feature.raetsel.ui

import com.bestfriends.beachbingo.core.audio.BaseChiptuneAudioManager

internal class RaetselAudioManager(ctx: android.content.Context) : BaseChiptuneAudioManager(ctx) {

    override fun musicAssetName(): String = "raetsel.ogg"

    override fun buildSoundCache() {
        // Richtige Eingabe: kurzes aufsteigendes Ping
        soundCache["correct"] = sineWave(659.3, 0.09, 0.14, 0.001, 784.0)

        // Falsche Eingabe: kurzes absteigendes Buzz
        soundCache["wrong"] = squareWave(330.0, 0.12, 0.12, 0.001, 220.0)

        // Hinweis/Tipp: sanfter Chime
        soundCache["hint"] = sineWave(880.0, 0.18, 0.10, 0.001)

        // Gewinn: jubilierendes Arpeggio G-H-D-G
        var win = ShortArray(0)
        for ((i, freq) in listOf(392.0, 494.0, 587.3, 784.0, 988.0).withIndex())
            win = overlayAt(win, squareBurst(freq, 0.18, 0.14, 0.001), (sampleRate * i * 0.11).toInt())
        soundCache["win"] = win
    }

    // Ruhige Fokus-Melodie in G-Dur, ~72 BPM (~0.42s Viertelnote)
    // G4=392  A4=440  B4=494  C5=523  D5=587  E5=659  G5=784

    private val melodyNotes = listOf(
        // Phrase A — sanfte Eröffnung
        392.0 to 0.42, 440.0 to 0.42, 523.3 to 0.84, 0.0 to 0.42,
        494.0 to 0.42, 440.0 to 0.42, 392.0 to 0.84, 0.0 to 0.42,
        // Phrase B — etwas höher
        523.3 to 0.42, 587.3 to 0.42, 659.3 to 0.84, 0.0 to 0.42,
        587.3 to 0.42, 523.3 to 0.42, 440.0 to 0.84, 0.0 to 0.42,
        // Phrase C — ruhige Auflösung
        392.0 to 0.42, 440.0 to 0.42, 494.0 to 0.42, 523.3 to 0.42,
        587.3 to 0.84, 0.0 to 0.42,
        392.0 to 0.42, 440.0 to 0.42, 392.0 to 1.26, 0.0 to 0.84,
    )

    private val bassNotes = listOf(
        196.0 to 0.84, 0.0 to 0.42, 246.9 to 0.42, 0.0 to 0.42,
        196.0 to 0.84, 0.0 to 0.42,
        146.8 to 0.84, 0.0 to 0.42, 196.0 to 0.42, 0.0 to 0.42,
        196.0 to 0.84, 0.0 to 0.42,
        130.8 to 0.84, 0.0 to 0.42, 196.0 to 0.42, 0.0 to 0.42,
        146.8 to 0.84, 0.0 to 0.42,
        196.0 to 1.68, 0.0 to 0.84,
    )

    override fun buildMelodyPcm(): ShortArray {
        var melody = ShortArray(0)
        for ((freq, dur) in melodyNotes)
            melody = concat(melody, if (freq > 0.0) sineNoteEnv(freq, dur, 0.028, 0.012, 0.050) else silence(dur))
        var bass = ShortArray(0)
        for ((freq, dur) in bassNotes)
            bass = concat(bass, if (freq > 0.0) triNoteEnv(freq, dur, 0.018, 0.010, 0.050) else silence(dur))
        return mix(melody, bass)
    }
}
