package com.bestfriends.beachbingo.feature.pong.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.*
import kotlin.math.*

internal class PongAudioManager {
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
            arr[i] = (if (phase % 1.0 < 0.5) 1.0 else -1.0).times(g0 + (g1 - g0) * (t / dur)).times(Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return arr
    }

    private fun sineWave(freq: Double, dur: Double, g0: Double, g1: Double, fEnd: Double = freq): ShortArray {
        val n = (sampleRate * dur).toInt()
        val arr = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / sampleRate
            phase += (freq + (fEnd - freq) * (t / dur)) / sampleRate
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
            arr[i] = ((if (p < 0.5) (4.0 * p - 1.0) else (3.0 - 4.0 * p)) * (g0 + (g1 - g0) * (t / dur)) * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
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

    private fun overlayAt(base: ShortArray, part: ShortArray, delaySamples: Int): ShortArray {
        val needed = delaySamples + part.size
        val result = if (needed > base.size) base.copyOf(needed) else base.copyOf()
        for (i in part.indices) {
            val pos = delaySamples + i
            result[pos] = (result[pos].toInt() + part[i].toInt()).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return result
    }

    private fun mix(a: ShortArray, b: ShortArray): ShortArray {
        val len = maxOf(a.size, b.size)
        val out = ShortArray(len)
        for (i in 0 until len) {
            out[i] = ((if (i < a.size) a[i].toInt() else 0) + (if (i < b.size) b[i].toInt() else 0)).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    // ── Sound cache ──────────────────────────────────────────────────────────

    private fun buildSoundCache() {
        // Ball hits paddle — sharp "thwack" (tennis ball impact)
        // Mix of short noise burst + mid-frequency sine sweep
        soundCache["ball_hit"] = mix(
            noise(0.035, 0.45, 0.001),
            sineWave(800.0, 0.05, 0.3, 0.001, 400.0),
        )

        // Ball hits wall — slightly different thump
        soundCache["wall_hit"] = mix(
            noise(0.025, 0.35, 0.001),
            sineWave(550.0, 0.04, 0.25, 0.001, 300.0),
        )

        // Score point — quick upward ding
        soundCache["score"] = concat(
            squareWave(660.0, 0.08, 0.2, 0.001),
            silence(0.03),
            squareWave(880.0, 0.1, 0.2, 0.001),
        )

        // Game over / match won
        var win = ShortArray(0)
        for ((idx, freq) in listOf(523.0, 659.0, 784.0, 1047.0).withIndex())
            win = overlayAt(win, squareWave(freq, 0.15, 0.18, 0.001), (sampleRate * idx * 0.12).toInt())
        soundCache["win"] = win
    }

    // ── Melody — Electronic synthwave drive (D minor, ~128 BPM) ─────────────
    // Fast 8th-note pulse, driving arpeggios, urgent competitive energy

    private val melodyNotes = listOf(
        // ─ SECTION A: Driving Arpeggio ─
        // Phrase 1 – arpeggio launch
        293.66 to 0.23, 440.0 to 0.23, 523.25 to 0.23, 587.33 to 0.23, 523.25 to 0.47, 0.0 to 0.23, 587.33 to 0.23,
        523.25 to 0.23, 0.0 to 0.23, 440.0 to 0.47, 0.0 to 0.23, 392.0 to 0.23, 349.23 to 0.47, 0.0 to 0.23,
        // Phrase 2 – stepping descent
        587.33 to 0.23, 0.0 to 0.23, 523.25 to 0.47, 0.0 to 0.23, 440.0 to 0.47, 0.0 to 0.23, 392.0 to 0.23,
        349.23 to 0.23, 0.0 to 0.23, 440.0 to 0.23, 349.23 to 0.47, 293.66 to 0.47, 0.0 to 0.47,
        // ─ SECTION B: Melodic Hook ─
        // Phrase 3 – punchy synth hook
        0.0 to 0.23, 523.25 to 0.23, 0.0 to 0.23, 587.33 to 0.47, 0.0 to 0.23, 523.25 to 0.23, 440.0 to 0.23,
        0.0 to 0.23, 440.0 to 0.23, 0.0 to 0.23, 392.0 to 0.47, 349.23 to 0.47, 0.0 to 0.47,
        // Phrase 4 – climbing peak then resolve to tonic
        349.23 to 0.23, 392.0 to 0.23, 440.0 to 0.23, 523.25 to 0.23, 587.33 to 0.23, 659.25 to 0.23, 587.33 to 0.47,
        523.25 to 0.23, 440.0 to 0.23, 392.0 to 0.23, 349.23 to 0.23, 293.66 to 0.94, 0.0 to 0.47,
    )

    private val bassNotes = listOf(
        // Electronic bass – driving Dm vamp
        293.66 to 0.23, 0.0 to 0.23, 293.66 to 0.47, 0.0 to 0.23, 220.0 to 0.47, 0.0 to 0.23,
        174.61 to 0.23, 0.0 to 0.23, 174.61 to 0.47, 0.0 to 0.23, 261.63 to 0.47, 0.0 to 0.23,
        196.0 to 0.23,  0.0 to 0.23, 196.0 to 0.47,  0.0 to 0.23, 196.0 to 0.47,  0.0 to 0.23,
        130.81 to 0.47, 0.0 to 0.23, 220.0 to 0.47,  0.0 to 0.23, 146.83 to 0.47, 0.0 to 0.23,
        293.66 to 0.23, 0.0 to 0.23, 293.66 to 0.47, 0.0 to 0.23, 220.0 to 0.47,  0.0 to 0.23,
        174.61 to 0.23, 0.0 to 0.23, 174.61 to 0.47, 0.0 to 0.23, 261.63 to 0.47, 0.0 to 0.23,
        196.0 to 0.23,  0.0 to 0.23, 196.0 to 0.23,  146.83 to 0.23, 196.0 to 0.23, 220.0 to 0.23, 196.0 to 0.47,
        73.42 to 0.94,  0.0 to 0.47, 73.42 to 0.47,  0.0 to 0.47,
    )

    private fun buildMelodyPcm(): ShortArray {
        var melody = ShortArray(0)
        for ((freq, dur) in melodyNotes)
            melody = concat(melody, if (freq > 0.0) squareWave(freq, dur, 0.055, 0.001) else silence(dur))
        var bass = ShortArray(0)
        for ((freq, dur) in bassNotes)
            bass = concat(bass, if (freq > 0.0) squareWave(freq, dur, 0.04, 0.001) else silence(dur))
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
