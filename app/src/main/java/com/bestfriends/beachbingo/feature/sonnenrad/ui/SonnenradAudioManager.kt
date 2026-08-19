package com.bestfriends.beachbingo.feature.sonnenrad.ui

import com.bestfriends.beachbingo.core.audio.BaseChiptuneAudioManager

internal class SonnenradAudioManager : BaseChiptuneAudioManager() {

    override fun buildSoundCache() {
        soundCache["reveal"]  = squareBurst(660.0, 0.10, 0.5, 0.0)
        soundCache["step_up"] = squareBurst(784.0, 0.12, 0.55, 0.0)
        soundCache["secure"]  = squareBurst(988.0, 0.20, 0.6, 0.0)
        soundCache["tick"]    = squareBurst(440.0, 0.05, 0.3, 0.0)
    }

    override fun buildMelodyPcm(): ShortArray = silence(1.0)
}
