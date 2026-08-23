package com.bestfriends.beachbingo.feature.raetsel.ui

import com.bestfriends.beachbingo.core.audio.BaseChiptuneAudioManager

internal class KuestenkriegAudioManager : BaseChiptuneAudioManager() {

    override fun musicAssetName(): String = "kuestenkrieg.ogg"

    override fun buildSoundCache() {
        // Treffer: Explosion (Rauschen + Burst)
        var hit = noise(0.20, 0.50, 0.001)
        hit = mix(hit, squareBurst(110.0, 0.18, 0.22, 0.001))
        soundCache["hit"] = hit

        // Daneben: Wassertropfen (fallende Sinuswelle)
        soundCache["miss"] = sineWave(440.0, 0.18, 0.14, 0.001, 196.0)

        // Schiff versenkt: dramatischer Akkord
        var sunk = ShortArray(0)
        for ((i, freq) in listOf(147.0, 185.0, 220.0, 294.0).withIndex())
            sunk = overlayAt(sunk, squareBurst(freq, 0.22, 0.18, 0.001), (sampleRate * i * 0.09).toInt())
        sunk = mix(sunk, noise(0.40, 0.30, 0.001))
        soundCache["sunk"] = sunk

        // Sieg: aufsteigende Fanfare
        var win = ShortArray(0)
        for ((i, freq) in listOf(294.0, 370.0, 440.0, 587.3, 740.0, 880.0).withIndex())
            win = overlayAt(win, squareBurst(freq, 0.20, 0.18, 0.001), (sampleRate * i * 0.10).toInt())
        soundCache["win"] = win

        // Niederlage: absteigendes Lamento
        var lose = ShortArray(0)
        for ((i, freq) in listOf(440.0, 370.0, 294.0, 233.0, 185.0).withIndex())
            lose = overlayAt(lose, squareWave(freq, 0.20, 0.18, 0.001), (sampleRate * i * 0.14).toInt())
        soundCache["lose"] = lose

        // Eigenes Schiff getroffen (im Battle-Modus)
        soundCache["own_hit"] = squareBurst(196.0, 0.22, 0.28, 0.001)
    }

    // Seemanns-Marsch in D-Moll, ~80 BPM (~0.375s Viertelnote)
    // D4=294  E4=330  F4=349  G4=392  A4=440  Bb4=466  C5=523  D5=587

    private val melodyNotes = listOf(
        // Phrase A — marschierendes Thema
        294.0 to 0.19, 0.0 to 0.09, 330.0 to 0.19, 0.0 to 0.09,
        349.0 to 0.38, 0.0 to 0.19,
        392.0 to 0.38, 440.0 to 0.38, 0.0 to 0.19,
        // Phrase B — aufsteigende Spannung
        466.0 to 0.19, 0.0 to 0.09, 440.0 to 0.19, 0.0 to 0.09,
        392.0 to 0.38, 0.0 to 0.19,
        349.0 to 0.38, 330.0 to 0.19, 294.0 to 0.57, 0.0 to 0.28,
        // Phrase C — dramatische Höhe
        523.3 to 0.19, 0.0 to 0.09, 494.0 to 0.19, 0.0 to 0.09,
        466.0 to 0.38, 0.0 to 0.19,
        440.0 to 0.38, 392.0 to 0.38, 0.0 to 0.19,
        // Phrase D — Auflösung
        349.0 to 0.19, 0.0 to 0.09, 392.0 to 0.19, 0.0 to 0.09,
        440.0 to 0.38, 0.0 to 0.19,
        392.0 to 0.19, 349.0 to 0.19, 294.0 to 0.75, 0.0 to 0.38,
    )

    private val bassNotes = listOf(
        146.8 to 0.38, 0.0 to 0.19, 220.0 to 0.38, 0.0 to 0.19,
        146.8 to 0.38, 0.0 to 0.19, 175.0 to 0.38, 0.0 to 0.19,
        110.0 to 0.57, 0.0 to 0.28, 130.8 to 0.57, 0.0 to 0.28,
        146.8 to 1.13, 0.0 to 0.38,
        175.0 to 0.38, 0.0 to 0.19, 220.0 to 0.38, 0.0 to 0.19,
        196.0 to 0.38, 0.0 to 0.19, 175.0 to 0.38, 0.0 to 0.19,
        146.8 to 0.57, 0.0 to 0.28, 110.0 to 0.57, 0.0 to 0.28,
        146.8 to 1.13, 0.0 to 0.38,
    )

    override fun buildMelodyPcm(): ShortArray {
        var melody = ShortArray(0)
        for ((freq, dur) in melodyNotes)
            melody = concat(melody, if (freq > 0.0) triNoteEnv(freq, dur, 0.040, 0.005, 0.035) else silence(dur))
        var bass = ShortArray(0)
        for ((freq, dur) in bassNotes)
            bass = concat(bass, if (freq > 0.0) sineNoteEnv(freq, dur, 0.028, 0.005, 0.040) else silence(dur))
        return mix(melody, bass)
    }
}
