package com.bestfriends.beachbingo.feature.strandraeuber.ui

import com.bestfriends.beachbingo.core.audio.BaseChiptuneAudioManager

internal class StrandraeuberAudioManager : BaseChiptuneAudioManager() {

    // ── Sound cache ──────────────────────────────────────────────────────────

    override fun buildSoundCache() {
        // CARD_SHUFFLE: kurzes weißes Rauschen, ~0.3s
        soundCache["card_shuffle"] = noise(0.30, 0.40, 0.001)

        // CARD_DRAW: einzelner kurzer Click-Ton (110Hz, kurzes attack-decay)
        soundCache["card_draw"] = sineWave(110.0, 0.12, 0.40, 0.001, 80.0)

        // PAIR_DISCARD: 2-Ton Akkord aufsteigend
        var pairSound = ShortArray(0)
        pairSound = overlayAt(pairSound, sineWave(523.3, 0.20, 0.25, 0.001), 0)
        pairSound = overlayAt(pairSound, sineWave(784.0, 0.20, 0.20, 0.001), (sampleRate * 0.12).toInt())
        soundCache["pair_discard"] = pairSound

        // PLAYER_OUT: kleines Fanfare-Motiv
        var playerOut = ShortArray(0)
        for ((idx, freq) in listOf(523.3, 659.3, 784.0, 1046.5).withIndex())
            playerOut = overlayAt(playerOut, squareWave(freq, 0.14, 0.18, 0.001), (sampleRate * idx * 0.10).toInt())
        soundCache["player_out"] = playerOut

        // GAME_OVER: dramatisches absteigendes Glissando
        var gameOver = ShortArray(0)
        for ((idx, freq) in listOf(440.0, 370.0, 311.1, 261.6, 220.0, 185.0).withIndex())
            gameOver = overlayAt(gameOver, squareWave(freq, 0.18, 0.22, 0.001), (sampleRate * idx * 0.14).toInt())
        soundCache["game_over"] = gameOver

        // TURN_PING: kurzer hoher Ton 880Hz 0.1s
        soundCache["turn_ping"] = sineWave(880.0, 0.10, 0.18, 0.001)
    }

    // ── Music — A-Moll Detective/Mystery style, ~88 BPM ────────────────────

    // A minor notes (Hz)
    // A4=440, B4=494, C5=523, D5=587, Eb5=622, E5=659, F5=698, G5=784
    // A5=880, Bb4=466, Ab4=415

    // Staccato: note~0.20s, gap~0.09s per "beat" at 88bpm ≈ 0.682s/beat
    // Using 8th notes: ~0.34s each, staccato = 0.22 note + 0.12 silence

    private val melodyNotes = listOf(
        // Phrase 1: E5 F5 Eb5 D5 — chromatic sneak
        659.3 to 0.22, 0.0 to 0.12,
        698.5 to 0.22, 0.0 to 0.12,
        622.3 to 0.22, 0.0 to 0.12,
        587.3 to 0.45, 0.0 to 0.25,
        // E5 C5 B4 A4
        659.3 to 0.22, 0.0 to 0.12,
        523.3 to 0.22, 0.0 to 0.12,
        493.9 to 0.22, 0.0 to 0.12,
        440.0 to 0.68, 0.0 to 0.35,
        // Phrase 2: ascending tension A4-C5-E5-G5
        440.0 to 0.22, 0.0 to 0.12,
        466.2 to 0.22, 0.0 to 0.12,  // Bb4 chromatic
        493.9 to 0.22, 0.0 to 0.12,
        523.3 to 0.45, 0.0 to 0.25,
        587.3 to 0.22, 0.0 to 0.12,
        622.3 to 0.22, 0.0 to 0.12,  // Eb5
        659.3 to 0.68, 0.0 to 0.35,
        // Phrase 3: descending resolution
        784.0 to 0.22, 0.0 to 0.12,
        698.5 to 0.22, 0.0 to 0.12,
        659.3 to 0.22, 0.0 to 0.12,
        622.3 to 0.22, 0.0 to 0.12,  // Eb5
        587.3 to 0.22, 0.0 to 0.12,
        523.3 to 0.22, 0.0 to 0.12,
        493.9 to 0.22, 0.0 to 0.12,
        440.0 to 1.00, 0.0 to 0.50,
    )

    private val bassNotes = listOf(
        110.0 to 0.68, 0.0 to 0.35,   // A2
        110.0 to 0.45, 0.0 to 0.25,
        130.8 to 0.68, 0.0 to 0.35,   // C3
        110.0 to 0.68, 0.0 to 0.35,
        146.8 to 0.68, 0.0 to 0.35,   // D3
        164.8 to 0.45, 0.0 to 0.25,   // E3
        110.0 to 1.36, 0.0 to 0.50,   // A2 long
        // Second phrase
        110.0 to 0.68, 0.0 to 0.35,
        130.8 to 0.45, 0.0 to 0.25,
        110.0 to 0.68, 0.0 to 0.35,
        123.5 to 0.68, 0.0 to 0.35,   // B2
        110.0 to 1.50, 0.0 to 0.50,
        // Third phrase
        110.0 to 0.68, 0.0 to 0.35,
        164.8 to 0.45, 0.0 to 0.25,
        130.8 to 0.68, 0.0 to 0.35,
        110.0 to 1.80, 0.0 to 0.50,
    )

    override fun buildMelodyPcm(): ShortArray {
        var melody = ShortArray(0)
        for ((freq, dur) in melodyNotes)
            melody = concat(melody, if (freq > 0.0) triNoteEnv(freq, dur, 0.045) else silence(dur))
        var bass = ShortArray(0)
        for ((freq, dur) in bassNotes)
            bass = concat(bass, if (freq > 0.0) sineNoteEnv(freq, dur, 0.030) else silence(dur))
        return mix(melody, bass)
    }
}
