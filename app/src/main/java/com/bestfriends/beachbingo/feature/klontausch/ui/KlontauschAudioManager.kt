package com.bestfriends.beachbingo.feature.klontausch.ui

import com.bestfriends.beachbingo.core.audio.BaseChiptuneAudioManager

internal class KlontauschAudioManager : BaseChiptuneAudioManager() {

    override fun musicAssetName(): String = "klontausch.ogg"

    // ── Sound cache ──────────────────────────────────────────────────────────

    override fun buildSoundCache() {
        // card_draw: short click-swipe sound
        soundCache["card_draw"] = sineWave(440.0, 0.08, 0.35, 0.001, 200.0)

        // card_play: slightly longer with envelope
        soundCache["card_play"] = sineWave(520.0, 0.12, 0.40, 0.001)

        // offer: two-tone ascending — signals an offer opening
        var offerSound = ShortArray(0)
        offerSound = overlayAt(offerSound, sineWave(330.0, 0.12, 0.25, 0.001), 0)
        offerSound = overlayAt(offerSound, sineWave(440.0, 0.12, 0.25, 0.001), (sampleRate * 0.10).toInt())
        soundCache["offer"] = offerSound

        // swap: quick ascending then descending — trade confirmed
        var swapSound = ShortArray(0)
        swapSound = overlayAt(swapSound, sineWave(880.0, 0.10, 0.20, 0.001), 0)
        swapSound = overlayAt(swapSound, sineWave(660.0, 0.10, 0.20, 0.001), (sampleRate * 0.08).toInt())
        swapSound = overlayAt(swapSound, sineWave(440.0, 0.10, 0.18, 0.001), (sampleRate * 0.16).toInt())
        soundCache["swap"] = swapSound

        // melden: single bright tone — I have a matching card!
        soundCache["melden"] = sineWave(660.0, 0.10, 0.35, 0.001)

        // win: ascending fanfare
        var winSound = ShortArray(0)
        for ((idx, freq) in listOf(523.3, 659.3, 784.0, 1046.5).withIndex())
            winSound = overlayAt(winSound, sineWave(freq, 0.14, 0.30, 0.001), (sampleRate * idx * 0.11).toInt())
        soundCache["win"] = winSound
    }

    // ── Background music — playful card-game waltz ────────────────────────────

    private val melodyNotes = listOf(
        // Phrase 1: light waltz feel in C major
        523.3 to 0.18, 0.0 to 0.09,
        659.3 to 0.18, 0.0 to 0.09,
        784.0 to 0.18, 0.0 to 0.09,
        880.0 to 0.36, 0.0 to 0.18,
        784.0 to 0.18, 0.0 to 0.09,
        659.3 to 0.18, 0.0 to 0.09,
        // Phrase 2: descend
        587.3 to 0.18, 0.0 to 0.09,
        523.3 to 0.18, 0.0 to 0.09,
        493.9 to 0.18, 0.0 to 0.09,
        440.0 to 0.36, 0.0 to 0.18,
        493.9 to 0.18, 0.0 to 0.09,
        523.3 to 0.18, 0.0 to 0.09,
        // Phrase 3: repeat up
        659.3 to 0.18, 0.0 to 0.09,
        784.0 to 0.18, 0.0 to 0.09,
        880.0 to 0.18, 0.0 to 0.09,
        1046.5 to 0.54, 0.0 to 0.27,
        // Phrase 4: resolution
        880.0 to 0.18, 0.0 to 0.09,
        784.0 to 0.18, 0.0 to 0.09,
        659.3 to 0.18, 0.0 to 0.09,
        523.3 to 0.72, 0.0 to 0.36,
    )

    override fun buildMelodyPcm(): ShortArray {
        var result = ShortArray(0)
        for ((freq, dur) in melodyNotes) {
            val segment = if (freq == 0.0) silence(dur) else triNoteEnv(freq, dur, 0.20)
            result = concat(result, segment)
        }
        return result
    }

    // ── Public play helpers ───────────────────────────────────────────────────

    fun playCardDraw()  { playSound("card_draw") }
    fun playCardPlay()  { playSound("card_play") }
    fun playOffer()     { playSound("offer") }
    fun playSwap()      { playSound("swap") }
    fun playMelden()    { playSound("melden") }
    fun playWin()       { playSound("win") }
}
