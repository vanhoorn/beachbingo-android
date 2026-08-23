package com.bestfriends.beachbingo.feature.sonnenrad.ui

import com.bestfriends.beachbingo.core.audio.BaseChiptuneAudioManager

internal class SonnenradAudioManager(ctx: android.content.Context) : BaseChiptuneAudioManager(ctx) {

    override fun musicAssetName(): String = "sonnenrad.ogg"

    override fun buildSoundCache() {
        soundCache["reveal"]  = squareBurst(660.0, 0.10, 0.5, 0.0)
        soundCache["step_up"] = squareBurst(784.0, 0.12, 0.55, 0.0)
        soundCache["secure"]  = squareBurst(988.0, 0.20, 0.6, 0.0)
        soundCache["tick"]    = squareBurst(440.0, 0.05, 0.3, 0.0)
    }

    // Festliche Tages-Fanfare in C-Dur, ~112 BPM (~0.27s Viertelnote)
    // C5=523  D5=587  E5=659  F5=699  G5=784  A5=880
    // C3=131  D3=147  E3=165  F3=175  G3=196

    private val melodyNotes = listOf(
        // Phrase A — aufsteigendes Arpeggio
        523.3 to 0.14, 659.3 to 0.14, 784.0 to 0.27, 659.3 to 0.14, 0.0 to 0.14,
        784.0 to 0.14, 880.0 to 0.14, 784.0 to 0.27, 0.0 to 0.27,
        // Phrase B — fröhliche Wendung
        659.3 to 0.14, 698.5 to 0.14, 784.0 to 0.27, 659.3 to 0.14, 523.3 to 0.14,
        587.3 to 0.27, 523.3 to 0.54, 0.0 to 0.27,
        // Phrase C — hohe Lage
        880.0 to 0.14, 784.0 to 0.14, 698.5 to 0.14, 659.3 to 0.14,
        784.0 to 0.27, 0.0 to 0.27,
        // Phrase D — Schluss
        659.3 to 0.14, 587.3 to 0.14, 523.3 to 0.14, 440.0 to 0.14,
        523.3 to 0.54, 0.0 to 0.27,
    )

    private val bassNotes = listOf(
        130.8 to 0.27, 0.0 to 0.27, 196.0 to 0.27, 0.0 to 0.27,
        130.8 to 0.27, 0.0 to 0.27, 174.6 to 0.27, 0.0 to 0.27,
        130.8 to 0.27, 0.0 to 0.27, 196.0 to 0.27, 0.0 to 0.27,
        130.8 to 0.54, 0.0 to 0.54,
        130.8 to 0.27, 0.0 to 0.27, 164.8 to 0.27, 0.0 to 0.27,
        146.8 to 0.27, 0.0 to 0.27, 130.8 to 0.27, 0.0 to 0.27,
        130.8 to 0.27, 0.0 to 0.27, 196.0 to 0.27, 0.0 to 0.27,
        130.8 to 0.81, 0.0 to 0.27,
    )

    override fun buildMelodyPcm(): ShortArray {
        var melody = ShortArray(0)
        for ((freq, dur) in melodyNotes)
            melody = concat(melody, if (freq > 0.0) sineNoteEnv(freq, dur, 0.035, 0.008, 0.040) else silence(dur))
        var bass = ShortArray(0)
        for ((freq, dur) in bassNotes)
            bass = concat(bass, if (freq > 0.0) triNoteEnv(freq, dur, 0.022, 0.008, 0.035) else silence(dur))
        return mix(melody, bass)
    }
}
