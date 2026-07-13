package com.bestfriends.beachbingo.feature.strandturm.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.*
import kotlin.math.*

// Procedural audio for Strandturm — mirrors the Web Audio API synthesis in AudioManager.ts.
// All sounds are PCM-generated; no audio files are used.
internal class StrandturmAudioManager {
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

    private fun triWave(
        freq: Double, durationS: Double,
        gainStart: Double, gainEnd: Double,
    ): ShortArray {
        val n = (sampleRate * durationS).toInt()
        val arr = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / sampleRate
            phase += freq / sampleRate
            val p = phase % 1.0
            val wave = if (p < 0.5) (4.0 * p - 1.0) else (3.0 - 4.0 * p)
            val gain = gainStart + (gainEnd - gainStart) * (t / durationS)
            arr[i] = (wave * gain * Short.MAX_VALUE).toInt()
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

    private fun silence(durationS: Double) = ShortArray((sampleRate * durationS).toInt())

    private fun concat(vararg parts: ShortArray): ShortArray {
        val out = ShortArray(parts.sumOf { it.size })
        var pos = 0
        for (p in parts) { p.copyInto(out, pos); pos += p.size }
        return out
    }

    // Overlay 'part' into 'base' starting at 'delaySamples', growing base if needed.
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

    // Mix two arrays (longer wins length), summing amplitudes.
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
        soundCache["jump"] = squareWave(280.0, 0.2, 0.22, 0.001, 560.0)

        soundCache["land"] = noise(0.06, 0.5, 0.001)

        soundCache["climb"] = squareWave(900.0, 0.04, 0.07, 0.001)

        soundCache["coconut_bounce"] = sineWave(160.0, 0.22, 0.28, 0.001, 70.0)

        soundCache["hit"] = mix(
            noise(0.35, 0.45, 0.001),
            squareWave(440.0, 0.5, 0.25, 0.001, 55.0),
        )

        // Overlapping note sequences — match Web Audio API scheduled playback.
        var lifeLost = ShortArray(0)
        for ((idx, freq) in listOf(400.0, 350.0, 300.0, 220.0).withIndex())
            lifeLost = overlayAt(lifeLost, squareWave(freq, 0.12, 0.22, 0.001), (sampleRate * idx * 0.1).toInt())
        soundCache["life_lost"] = lifeLost

        var lvlComplete = ShortArray(0)
        for ((idx, freq) in listOf(261.0, 329.0, 392.0, 523.0, 659.0, 784.0).withIndex())
            lvlComplete = overlayAt(lvlComplete, squareWave(freq, 0.15, 0.2, 0.001), (sampleRate * idx * 0.1).toInt())
        soundCache["level_complete"] = lvlComplete

        var gameOver = ShortArray(0)
        for ((idx, freq) in listOf(440.0, 392.0, 349.0, 294.0, 261.0, 196.0).withIndex())
            gameOver = overlayAt(gameOver, squareWave(freq, 0.22, 0.2, 0.001), (sampleRate * idx * 0.18).toInt())
        soundCache["game_over"] = gameOver

        soundCache["timer_tick"] = squareWave(1400.0, 0.05, 0.1, 0.001)

        var bonus = ShortArray(0)
        for ((idx, freq) in listOf(523.0, 659.0, 784.0).withIndex())
            bonus = overlayAt(bonus, squareWave(freq, 0.1, 0.18, 0.001), (sampleRate * idx * 0.07).toInt())
        soundCache["bonus"] = bonus
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

    fun playSound(id: String) {
        if (!soundEnabled) return
        val samples = soundCache[id] ?: return
        scope.launch(Dispatchers.Default) { playRaw(samples) }
    }

    // ── Music ────────────────────────────────────────────────────────────────

    // Note sequences: frequency_hz to duration_s, 0.0 = rest. Mirrors AudioManager.ts MELODIES.
    private val melodyNotes = listOf(
        659.0 to 0.125, 784.0 to 0.125, 659.0 to 0.125, 784.0 to 0.125,
        659.0 to 0.125, 587.0 to 0.125, 659.0 to 0.25,
        0.0   to 0.125,
        523.0 to 0.125, 659.0 to 0.125, 784.0 to 0.125, 523.0 to 0.25,
        0.0   to 0.375,
        440.0 to 0.125, 523.0 to 0.125, 659.0 to 0.125, 440.0 to 0.125,
        392.0 to 0.125, 440.0 to 0.125, 494.0 to 0.125, 0.0   to 0.125,
        523.0 to 0.375, 0.0   to 0.125,
        392.0 to 0.125, 440.0 to 0.125, 494.0 to 0.125, 523.0 to 0.125,
        587.0 to 0.125, 659.0 to 0.25,  0.0   to 0.125,
        784.0 to 0.125, 659.0 to 0.125, 587.0 to 0.125, 523.0 to 0.125,
        494.0 to 0.125, 440.0 to 0.25,  0.0   to 0.25,
    )

    private val bassNotes = listOf(
        130.0 to 0.5, 0.0 to 0.5, 146.0 to 0.5, 0.0 to 0.5,
        130.0 to 0.5, 0.0 to 0.5, 130.0 to 1.0,
    )

    private fun buildMelodyPcm(): ShortArray {
        var melody = ShortArray(0)
        for ((freq, dur) in melodyNotes)
            melody = concat(melody, if (freq > 0.0) squareWave(freq, dur, 0.07, 0.001) else silence(dur))

        var bass = ShortArray(0)
        for ((freq, dur) in bassNotes)
            bass = concat(bass, if (freq > 0.0) triWave(freq, dur, 0.04, 0.001) else silence(dur))

        return mix(melody, bass)
    }

    fun startMusic() {
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
                track.setLoopPoints(0, pcm.size, -1) // -1 = loop infinitely
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

    fun setSound(enabled: Boolean) { soundEnabled = enabled }

    fun setMusic(enabled: Boolean) {
        musicEnabled = enabled
        if (!enabled) stopMusic() else startMusic()
    }

    fun release() {
        stopMusic()
        scope.cancel()
    }
}
