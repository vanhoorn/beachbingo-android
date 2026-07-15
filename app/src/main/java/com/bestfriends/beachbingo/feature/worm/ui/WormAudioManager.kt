package com.bestfriends.beachbingo.feature.worm.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.*
import kotlin.math.*

internal class WormAudioManager {
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

    private fun squareWave(freq: Double, dur: Double, g0: Double, g1: Double, fEnd: Double = freq): ShortArray {
        val n = (sampleRate * dur).toInt()
        val arr = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / sampleRate
            phase += (freq + (fEnd - freq) * (t / dur)) / sampleRate
            val wave = if (phase % 1.0 < 0.5) 1.0 else -1.0
            arr[i] = (wave * (g0 + (g1 - g0) * (t / dur)) * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return arr
    }

    private fun sineWave(freq: Double, dur: Double, g0: Double, g1: Double): ShortArray {
        val n = (sampleRate * dur).toInt()
        val arr = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / sampleRate
            phase += freq / sampleRate
            arr[i] = (sin(2 * PI * phase) * (g0 + (g1 - g0) * (t / dur)) * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return arr
    }

    private fun triWave(freq: Double, dur: Double, g0: Double, g1: Double): ShortArray {
        val n = (sampleRate * dur).toInt()
        val arr = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / sampleRate
            phase += freq / sampleRate
            val p = phase % 1.0
            val wave = if (p < 0.5) (4.0 * p - 1.0) else (3.0 - 4.0 * p)
            arr[i] = (wave * (g0 + (g1 - g0) * (t / dur)) * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return arr
    }

    private fun noise(dur: Double, g0: Double, g1: Double): ShortArray {
        val n = (sampleRate * dur).toInt()
        val arr = ShortArray(n)
        val rng = java.util.Random()
        for (i in 0 until n) {
            arr[i] = ((rng.nextDouble() * 2 - 1) * (g0 + (g1 - g0) * (i.toDouble() / n)) * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return arr
    }

    private fun silence(dur: Double) = ShortArray((sampleRate * dur).toInt())

    private fun concat(vararg parts: ShortArray): ShortArray {
        val out = ShortArray(parts.sumOf { it.size })
        var pos = 0
        for (p in parts) { p.copyInto(out, pos); pos += p.size }
        return out
    }

    private fun mix(a: ShortArray, b: ShortArray): ShortArray {
        val len = maxOf(a.size, b.size)
        val out = ShortArray(len)
        for (i in 0 until len) {
            out[i] = ((if (i < a.size) a[i].toInt() else 0) + (if (i < b.size) b[i].toInt() else 0)).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    private fun overlayAt(base: ShortArray, part: ShortArray, delaySamples: Int): ShortArray {
        val needed = delaySamples + part.size
        val result = if (needed > base.size) base.copyOf(needed) else base.copyOf()
        for (i in part.indices) {
            val pos = delaySamples + i
            result[pos] = (result[pos].toInt() + part[i].toInt()).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return result
    }

    // ── Sound cache ──────────────────────────────────────────────────────────

    private fun buildSoundCache() {
        // Eat food — satisfying "pop" (quick upward frequency jump)
        soundCache["eat"] = squareWave(440.0, 0.07, 0.22, 0.001, 880.0)

        // Eat rare food — more satisfying higher pop
        soundCache["eat_rare"] = squareWave(600.0, 0.1, 0.25, 0.001, 1200.0)

        // Death — crash + descending notes
        var die = ShortArray(0)
        die = mix(noise(0.25, 0.5, 0.001), die)
        for ((idx, freq) in listOf(330.0, 262.0, 196.0, 147.0).withIndex())
            die = overlayAt(die, squareWave(freq, 0.12, 0.15, 0.001), (sampleRate * (0.05 + idx * 0.1)).toInt())
        soundCache["die"] = die
    }

    // ── Melody — Funky electronic groove (D minor pentatonic, ~120 BPM) ──────
    // Syncopated dotted rhythms, punchy rests, worm dancing in the mud

    private val melodyNotes = listOf(
        // ─ SECTION A: The Groove ─
        // Phrase 1 – punchy riff with breathing room
        0.0 to 0.25, 293.66 to 0.375, 293.66 to 0.125, 0.0 to 0.25, 440.0 to 0.25, 0.0 to 0.25, 523.25 to 0.50,
        440.0 to 0.25, 0.0 to 0.25, 392.0 to 0.375, 349.23 to 0.125, 0.0 to 0.25, 349.23 to 0.25, 293.66 to 0.50,
        // Phrase 2 – variation, higher note appears
        0.0 to 0.25, 293.66 to 0.25, 440.0 to 0.25, 0.0 to 0.25, 523.25 to 0.25, 440.0 to 0.25, 392.0 to 0.50,
        0.0 to 0.25, 587.33 to 0.25, 0.0 to 0.25, 523.25 to 0.25, 440.0 to 0.50, 0.0 to 0.50,
        // ─ SECTION B: Call & Response ─
        // Phrase 3 – high punchy hits, offbeat emphasis
        0.0 to 0.375, 523.25 to 0.125, 0.0 to 0.25, 523.25 to 0.25, 0.0 to 0.25, 587.33 to 0.25, 523.25 to 0.50,
        440.0 to 0.25, 0.0 to 0.25, 440.0 to 0.25, 0.0 to 0.25, 392.0 to 0.25, 349.23 to 0.25, 293.66 to 0.50,
        // Phrase 4 – climax run, big landing
        349.23 to 0.25, 392.0 to 0.25, 440.0 to 0.25, 523.25 to 0.25, 587.33 to 0.25, 523.25 to 0.25, 440.0 to 0.50,
        0.0 to 0.25, 293.66 to 0.375, 440.0 to 0.125, 293.66 to 1.50, 0.0 to 0.50,
    )

    private val bassNotes = listOf(
        // Funky bass – D-groove with syncopation
        73.42 to 0.375, 73.42 to 0.125, 0.0 to 0.25, 73.42 to 0.25, 0.0 to 0.25, 73.42 to 0.50,
        98.0  to 0.375, 98.0  to 0.125, 0.0 to 0.25, 110.0 to 0.25, 0.0 to 0.25, 98.0 to 0.50,
        73.42 to 0.25, 0.0 to 0.25, 73.42 to 0.25, 0.0 to 0.25, 87.31 to 0.25, 73.42 to 0.25, 73.42 to 0.50,
        110.0 to 0.50, 0.0 to 0.50, 110.0 to 0.50, 0.0 to 0.50,
        73.42 to 0.375, 73.42 to 0.125, 0.0 to 0.25, 73.42 to 0.25, 0.0 to 0.25, 73.42 to 0.50,
        98.0  to 0.25, 0.0 to 0.25, 98.0 to 0.25, 0.0 to 0.25, 98.0 to 0.25, 110.0 to 0.25, 98.0 to 0.50,
        73.42 to 0.25, 0.0 to 0.25, 73.42 to 0.50, 0.0 to 0.25, 110.0 to 0.25, 98.0 to 0.25, 73.42 to 0.50,
        73.42 to 2.0, 0.0 to 0.50,
    )

    private fun buildMelodyPcm(): ShortArray {
        var melody = ShortArray(0)
        for ((freq, dur) in melodyNotes)
            melody = concat(melody, if (freq > 0.0) squareWave(freq, dur, 0.06, 0.001) else silence(dur))
        var bass = ShortArray(0)
        for ((freq, dur) in bassNotes)
            bass = concat(bass, if (freq > 0.0) triWave(freq, dur, 0.045, 0.001) else silence(dur))
        return mix(melody, bass)
    }

    // ── Playback ─────────────────────────────────────────────────────────────

    private fun playRaw(samples: ShortArray) {
        val byteCount = samples.size * 2
        val minBuf = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                .setAudioFormat(AudioFormat.Builder().setSampleRate(sampleRate).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(maxOf(byteCount, minBuf))
                .setTransferMode(AudioTrack.MODE_STATIC).build()
            track.write(samples, 0, samples.size)
            track.play()
            scope.launch {
                delay(samples.size * 1000L / sampleRate + 250)
                try { track.stop(); track.release() } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    fun playSound(id: String) {
        if (!soundEnabled) return
        val samples = soundCache[id] ?: return
        scope.launch(Dispatchers.Default) { playRaw(samples) }
    }

    fun startMusic() {
        if (!musicEnabled) return
        stopMusic()
        scope.launch(Dispatchers.Default) {
            try {
                val pcm = cachedMelody ?: buildMelodyPcm().also { cachedMelody = it }
                val byteCount = pcm.size * 2
                val minBuf = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
                val track = AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                    .setAudioFormat(AudioFormat.Builder().setSampleRate(sampleRate).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(maxOf(byteCount, minBuf))
                    .setTransferMode(AudioTrack.MODE_STATIC).build()
                track.write(pcm, 0, pcm.size)
                track.setLoopPoints(0, pcm.size, -1)
                musicTrack = track
                track.play()
            } catch (_: Exception) {}
        }
    }

    fun stopMusic() {
        val t = musicTrack; musicTrack = null
        try { t?.pause(); t?.flush(); t?.release() } catch (_: Exception) {}
    }

    fun setSound(enabled: Boolean) { soundEnabled = enabled }

    fun setMusic(enabled: Boolean) {
        musicEnabled = enabled
        if (!enabled) stopMusic() else startMusic()
    }

    fun release() { stopMusic(); scope.cancel() }
}
