package com.bestfriends.beachbingo.feature.perlentaucher.ui

import com.bestfriends.beachbingo.core.audio.BaseChiptuneAudioManager

internal class PerlentaucherAudioManager : BaseChiptuneAudioManager() {

    override fun buildSoundCache() {
        // 3er-Match: kurzes Aufwärts-Ping
        soundCache["match3"] = squareBurst(523.0, 0.08, 0.12, 0.001)

        // 4er-Match: zwei Töne aufsteigend
        var m4 = ShortArray(0)
        m4 = overlayAt(m4, squareBurst(523.0, 0.07, 0.10, 0.001), 0)
        m4 = overlayAt(m4, squareBurst(659.0, 0.10, 0.10, 0.001), (sampleRate * 0.07).toInt())
        soundCache["match4"] = m4

        // Spezialstein-Aktivierung: lauter, breiter Klang
        var sf = ShortArray(0)
        for ((i, freq) in listOf(523.0, 659.0, 784.0, 1047.0).withIndex())
            sf = overlayAt(sf, squareBurst(freq, 0.12, 0.14, 0.001), (sampleRate * i * 0.06).toInt())
        soundCache["special_fire"] = sf

        // Kaskade (jede Stufe etwas höher)
        soundCache["cascade_1"] = squareBurst(659.0, 0.09, 0.12, 0.001)
        soundCache["cascade_2"] = squareBurst(784.0, 0.09, 0.12, 0.001)
        soundCache["cascade_3"] = squareBurst(988.0, 0.09, 0.12, 0.001)

        // Win-Fanfare
        var win = ShortArray(0)
        for ((i, freq) in listOf(523.0, 659.0, 784.0, 1047.0, 1319.0).withIndex())
            win = overlayAt(win, squareBurst(freq, 0.18, 0.16, 0.001), (sampleRate * i * 0.1).toInt())
        soundCache["win"] = win

        // Loss-Klang
        var loss = ShortArray(0)
        for ((i, freq) in listOf(440.0, 370.0, 294.0).withIndex())
            loss = overlayAt(loss, squareBurst(freq, 0.14, 0.16, 0.001), (sampleRate * i * 0.1).toInt())
        soundCache["loss"] = loss

        // Shuffle (helles Rauschen + kurzer Akkord)
        soundCache["shuffle"] = squareBurst(880.0, 0.18, 0.08, 0.001)
    }

    // ── Musik: Ozeanisches Match-3-Thema, ~100 BPM ──────────────────────────

    private val melodyNotes = listOf(
        // Phrase A
        523.0 to 0.30, 659.0 to 0.15, 784.0 to 0.45, 659.0 to 0.30, 523.0 to 0.30, 0.0 to 0.30,
        392.0 to 0.30, 523.0 to 0.15, 659.0 to 0.45, 523.0 to 0.30, 392.0 to 0.30, 0.0 to 0.30,
        // Phrase B
        440.0 to 0.30, 554.0 to 0.15, 659.0 to 0.45, 784.0 to 0.30, 659.0 to 0.30, 0.0 to 0.30,
        523.0 to 0.60, 392.0 to 0.60, 0.0 to 0.60,
        // Phrase C – aufsteigende Variation
        330.0 to 0.30, 440.0 to 0.30, 523.0 to 0.30, 659.0 to 0.30, 784.0 to 0.60, 0.0 to 0.30,
        659.0 to 0.30, 523.0 to 0.30, 440.0 to 0.30, 392.0 to 0.30, 330.0 to 0.60, 0.0 to 0.30,
        // Phrase D – Rückkehr
        523.0 to 0.30, 659.0 to 0.30, 784.0 to 0.30, 1047.0 to 0.60, 784.0 to 0.30, 0.0 to 0.30,
        659.0 to 0.45, 523.0 to 0.45, 392.0 to 0.90, 0.0 to 0.60,
    )

    private val bassNotes = listOf(
        130.0 to 0.60, 0.0 to 0.30, 196.0 to 0.30, 0.0 to 0.60,
        98.0  to 0.60, 0.0 to 0.30, 130.0 to 0.30, 0.0 to 0.60,
        110.0 to 0.60, 0.0 to 0.30, 146.0 to 0.30, 0.0 to 0.60,
        130.0 to 1.20, 0.0 to 0.60,
        130.0 to 0.60, 0.0 to 0.30, 164.0 to 0.30, 0.0 to 0.60,
        98.0  to 0.60, 0.0 to 0.30, 130.0 to 0.30, 0.0 to 0.60,
        110.0 to 0.60, 0.0 to 0.30, 130.0 to 0.30, 0.0 to 0.60,
        130.0 to 1.50, 0.0 to 0.60,
    )

    override fun buildMelodyPcm(): ShortArray {
        var melody = ShortArray(0)
        for ((freq, dur) in melodyNotes)
            melody = concat(melody, if (freq > 0.0) sineNoteEnv(freq, dur, 0.030, 0.012, 0.040) else silence(dur))
        var bass = ShortArray(0)
        for ((freq, dur) in bassNotes)
            bass = concat(bass, if (freq > 0.0) triNoteEnv(freq, dur, 0.022, 0.010, 0.040) else silence(dur))
        return mix(melody, bass)
    }

    fun playCascade(cascadeLevel: Int) {
        val key = when {
            cascadeLevel >= 3 -> "cascade_3"
            cascadeLevel == 2 -> "cascade_2"
            else              -> "cascade_1"
        }
        playSound(key)
    }
}
