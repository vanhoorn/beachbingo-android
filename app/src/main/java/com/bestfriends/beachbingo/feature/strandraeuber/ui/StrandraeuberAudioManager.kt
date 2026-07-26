package com.bestfriends.beachbingo.feature.strandraeuber.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.*
import kotlin.math.*

internal class StrandraeuberAudioManager {
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

    private fun squareWave(
        freq: Double, durationS: Double,
        gainStart: Double, gainEnd: Double,
    ): ShortArray {
        val n = (sampleRate * durationS).toInt()
        val arr = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / sampleRate
            phase += freq / sampleRate
            val wave = if (phase % 1.0 < 0.5) 1.0 else -1.0
            val gain = gainStart + (gainEnd - gainStart) * (t / durationS)
            arr[i] = (wave * gain * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return arr
    }

    private fun triNoteEnv(freq: Double, durationS: Double, gain: Double): ShortArray {
        val n = (sampleRate * durationS).toInt()
        val fadeIn = (sampleRate * 0.008).toInt()
        val fadeOut = (sampleRate * 0.035).toInt()
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

    private fun sineNoteEnv(freq: Double, durationS: Double, gain: Double): ShortArray {
        val n = (sampleRate * durationS).toInt()
        val fadeIn = (sampleRate * 0.008).toInt()
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
        // CARD_SHUFFLE: kurzes weißes Rauschen, ~0.3s
        soundCache["card_shuffle"] = noise(0.30, 0.40, 0.001)

        // CARD_DRAW: einzelner kurzer Click-Ton (110Hz, kurzes attack-decay)
        soundCache["card_draw"] = sineWave(110.0, 0.12, 0.40, 0.001, 80.0)

        // PAIR_DISCARD: 2-Ton Akkord aufsteigend
        var pairSound = ShortArray(0)
        pairSound = overlayAt(pairSound, sineWave(523.3, 0.20, 0.25, 0.001), 0)
        pairSound = overlayAt(pairSound, sineWave(784.0, 0.20, 0.20, 0.001), (sampleRate * 0.12).toInt())
        soundCache["pair_discard"] = pairSound

        // PLAYER_OUT: kleines Fanfare-Motiv
        var playerOut = ShortArray(0)
        for ((idx, freq) in listOf(523.3, 659.3, 784.0, 1046.5).withIndex())
            playerOut = overlayAt(playerOut, squareWave(freq, 0.14, 0.18, 0.001), (sampleRate * idx * 0.10).toInt())
        soundCache["player_out"] = playerOut

        // GAME_OVER: dramatisches absteigendes Glissando
        var gameOver = ShortArray(0)
        for ((idx, freq) in listOf(440.0, 370.0, 311.1, 261.6, 220.0, 185.0).withIndex())
            gameOver = overlayAt(gameOver, squareWave(freq, 0.18, 0.22, 0.001), (sampleRate * idx * 0.14).toInt())
        soundCache["game_over"] = gameOver

        // TURN_PING: kurzer hoher Ton 880Hz 0.1s
        soundCache["turn_ping"] = sineWave(880.0, 0.10, 0.18, 0.001)
    }

    // ── Music — A-Moll Detective/Mystery style, ~88 BPM ────────────────────

    // A minor notes (Hz)
    // A4=440, B4=494, C5=523, D5=587, Eb5=622, E5=659, F5=698, G5=784
    // A5=880, Bb4=466, Ab4=415

    // Staccato: note~0.20s, gap~0.09s per "beat" at 88bpm ≈ 0.682s/beat
    // Using 8th notes: ~0.34s each, staccato = 0.22 note + 0.12 silence

    private val melodyNotes = listOf(
        // Phrase 1: E5 F5 Eb5 D5 — chromatic sneak
        659.3 to 0.22, 0.0 to 0.12,
        698.5 to 0.22, 0.0 to 0.12,
        622.3 to 0.22, 0.0 to 0.12,
        587.3 to 0.45, 0.0 to 0.25,
        // E5 C5 B4 A4
        659.3 to 0.22, 0.0 to 0.12,
        523.3 to 0.22, 0.0 to 0.12,
        493.9 to 0.22, 0.0 to 0.12,
        440.0 to 0.68, 0.0 to 0.35,
        // Phrase 2: ascending tension A4-C5-E5-G5
        440.0 to 0.22, 0.0 to 0.12,
        466.2 to 0.22, 0.0 to 0.12,  // Bb4 chromatic
        493.9 to 0.22, 0.0 to 0.12,
        523.3 to 0.45, 0.0 to 0.25,
        587.3 to 0.22, 0.0 to 0.12,
        622.3 to 0.22, 0.0 to 0.12,  // Eb5
        659.3 to 0.68, 0.0 to 0.35,
        // Phrase 3: descending resolution
        784.0 to 0.22, 0.0 to 0.12,
        698.5 to 0.22, 0.0 to 0.12,
        659.3 to 0.22, 0.0 to 0.12,
        622.3 to 0.22, 0.0 to 0.12,  // Eb5
        587.3 to 0.22, 0.0 to 0.12,
        523.3 to 0.22, 0.0 to 0.12,
        493.9 to 0.22, 0.0 to 0.12,
        440.0 to 1.00, 0.0 to 0.50,
    )

    private val bassNotes = listOf(
        110.0 to 0.68, 0.0 to 0.35,   // A2
        110.0 to 0.45, 0.0 to 0.25,
        130.8 to 0.68, 0.0 to 0.35,   // C3
        110.0 to 0.68, 0.0 to 0.35,
        146.8 to 0.68, 0.0 to 0.35,   // D3
        164.8 to 0.45, 0.0 to 0.25,   // E3
        110.0 to 1.36, 0.0 to 0.50,   // A2 long
        // Second phrase
        110.0 to 0.68, 0.0 to 0.35,
        130.8 to 0.45, 0.0 to 0.25,
        110.0 to 0.68, 0.0 to 0.35,
        123.5 to 0.68, 0.0 to 0.35,   // B2
        110.0 to 1.50, 0.0 to 0.50,
        // Third phrase
        110.0 to 0.68, 0.0 to 0.35,
        164.8 to 0.45, 0.0 to 0.25,
        130.8 to 0.68, 0.0 to 0.35,
        110.0 to 1.80, 0.0 to 0.50,
    )

    private fun buildMelodyPcm(): ShortArray {
        var melody = ShortArray(0)
        for ((freq, dur) in melodyNotes)
            melody = concat(melody, if (freq > 0.0) triNoteEnv(freq, dur, 0.045) else silence(dur))

        var bass = ShortArray(0)
        for ((freq, dur) in bassNotes)
            bass = concat(bass, if (freq > 0.0) sineNoteEnv(freq, dur, 0.030) else silence(dur))

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
