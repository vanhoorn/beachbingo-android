package com.bestfriends.beachbingo.core.audio

internal object AudioRegistry {
    @Volatile var current: BaseChiptuneAudioManager? = null
}
