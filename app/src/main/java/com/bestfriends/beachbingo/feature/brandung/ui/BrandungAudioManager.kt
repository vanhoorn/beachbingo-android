package com.bestfriends.beachbingo.feature.brandung.ui

import com.bestfriends.beachbingo.core.audio.BaseChiptuneAudioManager

internal class BrandungAudioManager(ctx: android.content.Context) : BaseChiptuneAudioManager(ctx) {

    override fun musicAssetName(): String = "brandung.ogg"

    // ── Sound cache ──────────────────────────────────────────────────────────

    override fun buildSoundCache() {
        soundCache["card_deal"] = noise(0.08, 0.5, 0.001)

        soundCache["card_draw"] = noiseRiseAndFall(0.12, 0.4)

        soundCache["card_place"] = sineWave(200.0, 0.11, 0.35, 0.001, 80.0)

        var knock = sineWave(160.0, 0.12, 0.45, 0.001, 60.0)
        knock = overlayAt(knock, sineWave(160.0, 0.12, 0.45, 0.001, 60.0), (sampleRate * 0.18).toInt())
        soundCache["card_knock"] = knock

        soundCache["card_select"] = sineWave(880.0, 0.06, 0.12, 0.001)

        var feuer = ShortArray(0)
        for ((idx, freq) in listOf(523.0, 659.0, 784.0, 1047.0, 1319.0).withIndex())
            feuer = overlayAt(feuer, squareWave(freq, 0.18, 0.2, 0.001), (sampleRate * idx * 0.09).toInt())
        soundCache["card_feuer"] = feuer

        var lvlComplete = ShortArray(0)
        for ((idx, freq) in listOf(523.0, 659.0, 784.0, 1047.0).withIndex())
            lvlComplete = overlayAt(lvlComplete, squareWave(freq, 0.15, 0.2, 0.001), (sampleRate * idx * 0.12).toInt())
        soundCache["level_complete"] = lvlComplete

        var lifeLost = ShortArray(0)
        for ((idx, freq) in listOf(440.0, 370.0, 294.0, 220.0).withIndex())
            lifeLost = overlayAt(lifeLost, squareWave(freq, 0.12, 0.2, 0.001), (sampleRate * idx * 0.1).toInt())
        soundCache["life_lost"] = lifeLost
    }

    // ── Music ────────────────────────────────────────────────────────────────

    private val melodyNotes = listOf(
        659.0 to 0.31, 587.0 to 0.31, 523.0 to 0.63, 440.0 to 0.63, 0.0 to 0.31,
        440.0 to 0.31, 494.0 to 0.31, 523.0 to 0.31, 587.0 to 0.63, 0.0 to 0.31,
        659.0 to 0.63, 587.0 to 0.31, 523.0 to 0.31, 494.0 to 0.63, 440.0 to 0.31, 0.0 to 0.31,
        440.0 to 1.25, 0.0 to 0.63,
        698.0 to 0.31, 659.0 to 0.31, 587.0 to 0.31, 523.0 to 0.31, 494.0 to 0.63, 0.0 to 0.31,
        523.0 to 0.31, 587.0 to 0.31, 659.0 to 0.63, 587.0 to 0.31, 0.0 to 0.31,
        440.0 to 0.31, 494.0 to 0.31, 523.0 to 0.31, 659.0 to 0.31, 587.0 to 0.63, 523.0 to 0.31, 0.0 to 0.31,
        440.0 to 1.88, 0.0 to 0.63,
    )

    private val bassNotes = listOf(
        110.0 to 0.63, 0.0 to 0.31, 110.0 to 0.31, 0.0 to 0.31, 130.0 to 0.63, 0.0 to 0.63,
        110.0 to 0.63, 0.0 to 0.31,  98.0 to 0.31, 0.0 to 0.31,  82.0 to 0.63, 0.0 to 0.63,
        110.0 to 1.25, 0.0 to 0.63,
        147.0 to 0.63, 0.0 to 0.31, 130.0 to 0.31, 0.0 to 0.31, 147.0 to 0.63, 0.0 to 0.31,
        165.0 to 0.31, 0.0 to 0.31, 110.0 to 0.63, 0.0 to 0.31, 110.0 to 0.31, 0.0 to 0.31,
        110.0 to 1.88, 0.0 to 0.63,
    )

    override fun buildMelodyPcm(): ShortArray {
        var melody = ShortArray(0)
        for ((freq, dur) in melodyNotes)
            melody = concat(melody, if (freq > 0.0) sineNoteEnv(freq, dur, 0.042, 0.005, 0.040) else silence(dur))
        var bass = ShortArray(0)
        for ((freq, dur) in bassNotes)
            bass = concat(bass, if (freq > 0.0) triNoteEnv(freq, dur, 0.028, 0.005, 0.040) else silence(dur))
        return mix(melody, bass)
    }
}
