package com.bestfriends.beachbingo.feature.brandung.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.*
import kotlin.math.*

internal class BrandungAudioManager {
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

    private fun squareWave(
        freq: Double, durationS: Double,
        gainStart: Double, gainEnd: Double,
        freqEnd: Double = freq,
    ): ShortArray {
        val n = (sampleRate * durationS).toInt()
        val arr = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / sampleRate
            phase += (freq + (freqEnd - freq) * (t / durationS)) / sampleRate
            val wave = if (phase % 1.0 < 0.5) 1.0 else -1.0
            val gain = gainStart + (gainEnd - gainStart) * (t / durationS)
            arr[i] = (wave * gain * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return arr
    }

    private fun sineWave(
        freq: Double, durationS: Double,
        gainStart: Double, gainEnd: Double,
        freqEnd: Double = freq,
    ): ShortArray {
        val n = (sampleRate * durationS).toInt()
        val arr = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / sampleRate
            phase += (freq + (freqEnd - freq) * (t / durationS)) / sampleRate
            val wave = sin(2 * PI * phase)
            val gain = gainStart + (gainEnd - gainStart) * (t / durationS)
            arr[i] = (wave * gain * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return arr
    }

    private fun sineNoteEnv(freq: Double, durationS: Double, gain: Double): ShortArray {
        val n = (sampleRate * durationS).toInt()
        val fadeIn = (sampleRate * 0.005).toInt()
        val fadeOut = (sampleRate * 0.040).toInt()
        val arr = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            phase += freq / sampleRate
            val wave = sin(2 * PI * phase)
            val env = when {
                i < fadeIn -> i.toDouble() / fadeIn
                i >= n - fadeOut -> (n - i).toDouble() / fadeOut
                else -> 1.0
            }.coerceIn(0.0, 1.0)
            arr[i] = (wave * gain * env * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return arr
    }

    private fun triNoteEnv(freq: Double, durationS: Double, gain: Double): ShortArray {
        val n = (sampleRate * durationS).toInt()
        val fadeIn = (sampleRate * 0.005).toInt()
        val fadeOut = (sampleRate * 0.040).toInt()
        val arr = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            phase += freq / sampleRate
            val p = phase % 1.0
            val wave = if (p < 0.5) (4.0 * p - 1.0) else (3.0 - 4.0 * p)
            val env = when {
                i < fadeIn -> i.toDouble() / fadeIn
                i >= n - fadeOut -> (n - i).toDouble() / fadeOut
                else -> 1.0
            }.coerceIn(0.0, 1.0)
            arr[i] = (wave * gain * env * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return arr
    }

    private fun noise(durationS: Double, gainStart: Double, gainEnd: Double): ShortArray {
        val n = (sampleRate * durationS).toInt()
        val arr = ShortArray(n)
        val rng = java.util.Random()
        for (i in 0 until n) {
            val gain = gainStart + (gainEnd - gainStart) * (i.toDouble() / n)
            arr[i] = ((rng.nextDouble() * 2 - 1) * gain * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return arr
    }

    private fun noiseRiseAndFall(durationS: Double, peakGain: Double): ShortArray {
        val n = (sampleRate * durationS).toInt()
        val arr = ShortArray(n)
        val rng = java.util.Random()
        for (i in 0 until n) {
            val t = i.toDouble() / n
            val env = if (t < 0.5) t * 2.0 * peakGain else (1.0 - t) * 2.0 * peakGain
            arr[i] = ((rng.nextDouble() * 2 - 1) * env * Short.MAX_VALUE).toInt()
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

    private fun buildMelodyPcm(): ShortArray {
        var melody = ShortArray(0)
        for ((freq, dur) in melodyNotes)
            melody = concat(melody, if (freq > 0.0) sineNoteEnv(freq, dur, 0.042) else silence(dur))

        var bass = ShortArray(0)
        for ((freq, dur) in bassNotes)
            bass = concat(bass, if (freq > 0.0) triNoteEnv(freq, dur, 0.028) else silence(dur))

        return mix(melody, bass)
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
