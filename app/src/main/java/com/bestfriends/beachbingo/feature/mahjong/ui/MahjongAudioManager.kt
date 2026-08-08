package com.bestfriends.beachbingo.feature.mahjong.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.*
import kotlin.math.*

internal class MahjongAudioManager {
    var soundEnabled = true
    var musicEnabled = true

    private val sampleRate = 44100
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val soundCache = mutableMapOf<String, ShortArray>()
    private var cachedMelody: ShortArray? = null
    private var musicTrack: AudioTrack? = null

    init {
        scope.launch {
            buildSoundCache()
            cachedMelody = buildMelodyPcm()
        }
    }

    // ── Wave generators ──────────────────────────────────────────────────────

    private fun sineNoteEnv(freq: Double, durationS: Double, gain: Double): ShortArray {
        val n = (sampleRate * durationS).toInt()
        val fadeIn  = (sampleRate * 0.008).toInt()
        val fadeOut = (sampleRate * 0.050).toInt()
        val arr = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            phase += freq / sampleRate
            val wave = sin(2 * PI * phase)
            val env = when {
                i < fadeIn        -> i.toDouble() / fadeIn
                i >= n - fadeOut  -> (n - i).toDouble() / fadeOut
                else              -> 1.0
            }.coerceIn(0.0, 1.0)
            arr[i] = (wave * gain * env * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return arr
    }

    private fun triNoteEnv(freq: Double, durationS: Double, gain: Double): ShortArray {
        val n = (sampleRate * durationS).toInt()
        val fadeIn  = (sampleRate * 0.008).toInt()
        val fadeOut = (sampleRate * 0.050).toInt()
        val arr = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            phase += freq / sampleRate
            val p = phase % 1.0
            val wave = if (p < 0.5) (4.0 * p - 1.0) else (3.0 - 4.0 * p)
            val env = when {
                i < fadeIn        -> i.toDouble() / fadeIn
                i >= n - fadeOut  -> (n - i).toDouble() / fadeOut
                else              -> 1.0
            }.coerceIn(0.0, 1.0)
            arr[i] = (wave * gain * env * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return arr
    }

    private fun squareBurst(freq: Double, durationS: Double, gainStart: Double, gainEnd: Double): ShortArray {
        val n = (sampleRate * durationS).toInt()
        val arr = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            phase += freq / sampleRate
            val wave = if (phase % 1.0 < 0.5) 1.0 else -1.0
            val gain = gainStart + (gainEnd - gainStart) * (i.toDouble() / n)
            arr[i] = (wave * gain * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return arr
    }

    private fun silence(durationS: Double) = ShortArray((sampleRate * durationS).toInt())

    private fun concat(vararg parts: ShortArray): ShortArray {
        val out = ShortArray(parts.sumOf { it.size })
        var pos = 0
        for (p in parts) { p.copyInto(out, pos); pos += p.size }
        return out
    }

    private fun overlayAt(base: ShortArray, part: ShortArray, delaySamples: Int): ShortArray {
        val needed = delaySamples + part.size
        val result = if (needed > base.size) base.copyOf(needed) else base.copyOf()
        for (i in part.indices) {
            val pos = delaySamples + i
            result[pos] = (result[pos].toInt() + part[i].toInt())
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return result
    }

    private fun mix(a: ShortArray, b: ShortArray): ShortArray {
        val len = maxOf(a.size, b.size)
        val out = ShortArray(len)
        for (i in 0 until len) {
            val av = if (i < a.size) a[i].toInt() else 0
            val bv = if (i < b.size) b[i].toInt() else 0
            out[i] = (av + bv).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    // ── Sound cache ──────────────────────────────────────────────────────────

    private fun buildSoundCache() {
        // Tile select: soft click
        soundCache["tile_select"] = squareBurst(880.0, 0.06, 0.1, 0.001)

        // Pair matched: ascending 2-tone
        var match = ShortArray(0)
        for ((idx, freq) in listOf(523.0, 659.0).withIndex())
            match = overlayAt(match, squareBurst(freq, 0.15, 0.1, 0.001), (sampleRate * idx * 0.08).toInt())
        soundCache["pair_match"] = match

        // Win: ascending fanfare
        var win = ShortArray(0)
        for ((idx, freq) in listOf(392.0, 523.0, 659.0, 784.0, 1047.0).withIndex())
            win = overlayAt(win, squareBurst(freq, 0.15, 0.18, 0.001), (sampleRate * idx * 0.1).toInt())
        soundCache["win"] = win

        // No moves / game over
        var over = ShortArray(0)
        for ((idx, freq) in listOf(440.0, 370.0, 294.0, 220.0).withIndex())
            over = overlayAt(over, squareBurst(freq, 0.12, 0.18, 0.001), (sampleRate * idx * 0.1).toInt())
        soundCache["game_over"] = over
    }

    // ── Music: Pentatonic zen, C pentatonic, ~80 BPM ────────────────────────

    private val melodyNotes = listOf(
        392.0 to 0.38, 440.0 to 0.38, 523.0 to 0.75, 440.0 to 0.38, 0.0 to 0.38,
        392.0 to 0.38, 329.0 to 0.38, 392.0 to 0.75, 0.0 to 0.75,
        440.0 to 0.38, 523.0 to 0.38, 587.0 to 0.75, 523.0 to 0.38, 440.0 to 0.38,
        392.0 to 1.50, 0.0 to 0.38,
        523.0 to 0.38, 440.0 to 0.38, 392.0 to 0.38, 329.0 to 0.38, 293.0 to 0.75, 0.0 to 0.38,
        329.0 to 0.38, 392.0 to 0.38, 440.0 to 0.38, 392.0 to 0.38, 329.0 to 0.75, 0.0 to 0.38,
        261.0 to 0.38, 293.0 to 0.38, 329.0 to 0.38, 392.0 to 0.38, 440.0 to 0.38, 392.0 to 0.38, 329.0 to 0.75,
        261.0 to 1.50, 0.0 to 0.75,
    )

    private val bassNotes = listOf(
        130.0 to 0.75, 0.0 to 0.38, 196.0 to 0.38, 0.0 to 0.75,
        130.0 to 0.75, 0.0 to 0.38, 110.0 to 0.38, 0.0 to 0.75,
        130.0 to 0.75, 0.0 to 0.38, 146.0 to 0.38, 0.0 to 0.75,
        130.0 to 1.50, 0.0 to 0.75,
        130.0 to 0.75, 0.0 to 0.38, 196.0 to 0.38, 0.0 to 0.75,
        98.0  to 0.75, 0.0 to 0.38, 110.0 to 0.38, 0.0 to 0.75,
        130.0 to 0.75, 0.0 to 0.38, 98.0  to 0.38, 0.0 to 0.75,
        130.0 to 1.50, 0.0 to 0.75,
    )

    private fun buildMelodyPcm(): ShortArray {
        var melody = ShortArray(0)
        for ((freq, dur) in melodyNotes)
            melody = concat(melody, if (freq > 0.0) sineNoteEnv(freq, dur, 0.038) else silence(dur))
        var bass = ShortArray(0)
        for ((freq, dur) in bassNotes)
            bass = concat(bass, if (freq > 0.0) triNoteEnv(freq, dur, 0.025) else silence(dur))
        return mix(melody, bass)
    }

    // ── Playback ─────────────────────────────────────────────────────────────

    private fun playRaw(samples: ShortArray) {
        val byteCount = samples.size * 2
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
        )
        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(byteCount, minBuf))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(samples, 0, samples.size)
            track.play()
            scope.launch {
                delay(samples.size * 1000L / sampleRate + 250)
                try { track.stop(); track.release() } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    fun playSound(name: String) {
        if (!soundEnabled) return
        val samples = soundCache[name] ?: return
        scope.launch(Dispatchers.Default) { playRaw(samples) }
    }

    fun startMusic(soundEnabled: Boolean, musicEnabled: Boolean) {
        this.soundEnabled = soundEnabled
        this.musicEnabled = musicEnabled
        if (!musicEnabled) return
        stopMusic()
        scope.launch(Dispatchers.Default) {
            try {
                val pcm = cachedMelody ?: buildMelodyPcm().also { cachedMelody = it }
                val byteCount = pcm.size * 2
                val minBuf = AudioTrack.getMinBufferSize(
                    sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                )
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(sampleRate)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(maxOf(byteCount, minBuf))
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(pcm, 0, pcm.size)
                track.setLoopPoints(0, pcm.size, -1)
                musicTrack = track
                track.play()
            } catch (_: Exception) {}
        }
    }

    fun stopMusic() {
        val t = musicTrack
        musicTrack = null
        try { t?.pause(); t?.flush(); t?.release() } catch (_: Exception) {}
    }

    fun release() {
        stopMusic()
        scope.cancel()
    }
}
