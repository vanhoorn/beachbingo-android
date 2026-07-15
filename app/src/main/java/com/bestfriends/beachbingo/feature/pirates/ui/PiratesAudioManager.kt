package com.bestfriends.beachbingo.feature.pirates.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.*
import kotlin.math.*

internal class PiratesAudioManager {
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
            val gain = g0 + (g1 - g0) * (t / dur)
            arr[i] = (wave * gain * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
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
            val gain = g0 + (g1 - g0) * (t / dur)
            arr[i] = (sin(2 * PI * phase) * gain * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
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
            val gain = g0 + (g1 - g0) * (t / dur)
            arr[i] = (wave * gain * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return arr
    }

    private fun noise(dur: Double, g0: Double, g1: Double): ShortArray {
        val n = (sampleRate * dur).toInt()
        val arr = ShortArray(n)
        val rng = java.util.Random()
        for (i in 0 until n) {
            val gain = g0 + (g1 - g0) * (i.toDouble() / n)
            arr[i] = ((rng.nextDouble() * 2 - 1) * gain * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
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
            val av = if (i < a.size) a[i].toInt() else 0
            val bv = if (i < b.size) b[i].toInt() else 0
            out[i] = (av + bv).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    // ── Sound cache ──────────────────────────────────────────────────────────

    private fun buildSoundCache() {
        // Pew! — high-pitched laser shot
        soundCache["shoot"] = squareWave(1200.0, 0.08, 0.18, 0.001, 600.0)

        // Enemy explodes — quick noise burst
        soundCache["enemy_hit"] = mix(
            noise(0.12, 0.5, 0.001),
            squareWave(200.0, 0.1, 0.2, 0.001, 60.0),
        )

        // Player takes hit — heavy crash
        soundCache["player_hit"] = mix(
            noise(0.28, 0.55, 0.001),
            squareWave(350.0, 0.35, 0.22, 0.001, 55.0),
        )

        // Wave cleared — ascending victory arpeggio
        var waveDone = ShortArray(0)
        for ((idx, freq) in listOf(330.0, 415.0, 523.0, 659.0, 784.0).withIndex())
            waveDone = overlayAt(waveDone, squareWave(freq, 0.1, 0.18, 0.001), (sampleRate * idx * 0.08).toInt())
        soundCache["wave_complete"] = waveDone

        // Game over — descending minor sequence
        var go = ShortArray(0)
        for ((idx, freq) in listOf(440.0, 370.0, 330.0, 262.0, 196.0).withIndex())
            go = overlayAt(go, squareWave(freq, 0.2, 0.2, 0.001), (sampleRate * idx * 0.16).toInt())
        soundCache["game_over"] = go
    }

    // ── Melody — Cinematic pirate adventure (A harmonic minor, ~76 BPM) ──────
    // Majestic quarter notes, dramatic leaps, G# for harmonic-minor tension

    private val melodyNotes = listOf(
        // ─ SECTION A: The Main Theme ─
        // Phrase 1 – powerful opening, stepwise with a dramatic edge
        440.0 to 0.79, 523.25 to 0.39, 587.33 to 0.39, 523.25 to 0.79, 0.0 to 0.79,
        440.0 to 0.39, 415.30 to 0.39, 440.0 to 0.79, 493.88 to 0.39, 523.25 to 0.39, 0.0 to 0.79,
        // Phrase 2 – leap to high E, then winding descent
        659.25 to 0.79, 0.0 to 0.39, 587.33 to 0.39, 523.25 to 0.39, 493.88 to 0.39, 440.0 to 0.79,
        415.30 to 0.39, 440.0 to 0.39, 523.25 to 0.39, 659.25 to 0.39, 698.46 to 0.39, 659.25 to 0.39, 587.33 to 0.39, 0.0 to 0.39,
        // ─ SECTION B: March to Battle ─
        // Phrase 3 – march rhythm, rising tension
        523.25 to 0.39, 0.0 to 0.39, 523.25 to 0.39, 0.0 to 0.39, 659.25 to 0.39, 0.0 to 0.39, 784.0 to 0.79,
        // Phrase 4 – climax on high A, triumphant descent
        880.0 to 0.79, 784.0 to 0.39, 698.46 to 0.39, 659.25 to 0.39, 587.33 to 0.39, 523.25 to 0.39, 440.0 to 0.39,
        // Phrase 5 – final resolution to tonic
        440.0 to 0.39, 523.25 to 0.39, 659.25 to 0.79, 587.33 to 0.39, 523.25 to 0.39, 440.0 to 0.79,
        415.30 to 0.39, 440.0 to 0.39, 523.25 to 0.39, 493.88 to 0.39, 440.0 to 1.58, 0.0 to 0.79,
    )

    private val bassNotes = listOf(
        // Cinematic bass – Am/E/D/G heavy downbeats
        110.0 to 0.79, 0.0 to 0.39, 110.0 to 0.39, 0.0 to 0.79, 110.0 to 0.79,
        82.41 to 0.79, 0.0 to 0.79, 82.41 to 0.79, 0.0 to 0.79,
        73.42 to 0.79, 0.0 to 0.79, 73.42 to 0.79, 0.0 to 0.79,
        98.0  to 0.79, 0.0 to 0.79, 82.41 to 0.79, 0.0 to 0.79,
        110.0 to 0.39, 0.0 to 0.39, 110.0 to 0.39, 0.0 to 0.39, 110.0 to 0.79, 0.0 to 0.79,
        87.31 to 0.79, 0.0 to 0.79, 82.41 to 0.79, 0.0 to 0.79,
        110.0 to 0.39, 0.0 to 0.39, 110.0 to 0.79, 110.0 to 0.79, 0.0 to 0.79,
        55.0 to 1.58, 0.0 to 0.79, 55.0 to 0.79, 0.0 to 0.79,
    )

    private fun buildMelodyPcm(): ShortArray {
        var melody = ShortArray(0)
        for ((freq, dur) in melodyNotes)
            melody = concat(melody, if (freq > 0.0) squareWave(freq, dur, 0.042, 0.001) else silence(dur))
        var bass = ShortArray(0)
        for ((freq, dur) in bassNotes)
            bass = concat(bass, if (freq > 0.0) triWave(freq, dur, 0.048, 0.001) else silence(dur))
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
