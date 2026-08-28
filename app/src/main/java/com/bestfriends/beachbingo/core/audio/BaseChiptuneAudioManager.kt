package com.bestfriends.beachbingo.core.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.*
import kotlin.math.*

// Pass context to enable file-based music (ExoPlayer + assets/audio/music/).
// Without context, or when the asset file is missing, falls back to PCM synthesis.
internal abstract class BaseChiptuneAudioManager(
    private val context: android.content.Context? = null
) {

    var soundEnabled = true
    var musicEnabled = true

    protected val sampleRate = 44100
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    protected val soundCache = mutableMapOf<String, ShortArray>()
    private var cachedMelody: ShortArray? = null
    private var musicTrack: AudioTrack? = null
    private var musicPlayer: ExoPlayer? = null
    private var musicSetupJob: Job? = null
    @Volatile private var pausedByLifecycle = false

    private class PoolEntry(val track: AudioTrack, val durationMs: Long) {
        @Volatile var freeAfter: Long = 0L
    }
    private val sfxPools = mutableMapOf<String, Array<PoolEntry>>()

    init {
        scope.launch {
            buildSoundCache()
            buildSoundPools()
            cachedMelody = buildMelodyPcm()
        }
    }

    // ── Abstract ─────────────────────────────────────────────────────────────

    protected abstract fun buildSoundCache()
    protected abstract fun buildMelodyPcm(): ShortArray

    // Override to return e.g. "pirates.mp3" — enables ExoPlayer music when context != null.
    protected open fun musicAssetName(): String? = null

    private fun buildSoundPools() {
        val minBuf = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        soundCache.forEach { (id, samples) ->
            val byteCount = samples.size * 2
            val durationMs = samples.size * 1000L / sampleRate + 100L
            val entries = (0 until 3).mapNotNull {
                try {
                    val track = AudioTrack.Builder()
                        .setAudioAttributes(AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                        .setAudioFormat(AudioFormat.Builder()
                            .setSampleRate(sampleRate)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                        .setBufferSizeInBytes(maxOf(byteCount, minBuf))
                        .setTransferMode(AudioTrack.MODE_STATIC).build()
                    track.write(samples, 0, samples.size)
                    PoolEntry(track, durationMs)
                } catch (_: Exception) { null }
            }.toTypedArray()
            if (entries.isNotEmpty()) sfxPools[id] = entries
        }
    }

    // ── Wave generators ──────────────────────────────────────────────────────

    protected fun squareWave(freq: Double, dur: Double, g0: Double, g1: Double, fEnd: Double = freq): ShortArray {
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

    protected fun sineWave(freq: Double, dur: Double, g0: Double, g1: Double, fEnd: Double = freq): ShortArray {
        val n = (sampleRate * dur).toInt()
        val arr = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / sampleRate
            phase += (freq + (fEnd - freq) * (t / dur)) / sampleRate
            val wave = sin(2 * PI * phase)
            val gain = g0 + (g1 - g0) * (t / dur)
            arr[i] = (wave * gain * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return arr
    }

    protected fun triWave(freq: Double, dur: Double, g0: Double, g1: Double): ShortArray {
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

    protected fun noise(dur: Double, g0: Double, g1: Double): ShortArray {
        val n = (sampleRate * dur).toInt()
        val arr = ShortArray(n)
        val rng = java.util.Random()
        for (i in 0 until n) {
            val gain = g0 + (g1 - g0) * (i.toDouble() / n)
            arr[i] = ((rng.nextDouble() * 2 - 1) * gain * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return arr
    }

    protected fun silence(dur: Double) = ShortArray((sampleRate * dur).toInt())

    protected fun concat(vararg parts: ShortArray): ShortArray {
        val out = ShortArray(parts.sumOf { it.size })
        var pos = 0
        for (p in parts) { p.copyInto(out, pos); pos += p.size }
        return out
    }

    protected fun overlayAt(base: ShortArray, part: ShortArray, delaySamples: Int): ShortArray {
        val needed = delaySamples + part.size
        val result = if (needed > base.size) base.copyOf(needed) else base.copyOf()
        for (i in part.indices) {
            val pos = delaySamples + i
            result[pos] = (result[pos].toInt() + part[i].toInt()).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return result
    }

    protected fun mix(a: ShortArray, b: ShortArray): ShortArray {
        val len = maxOf(a.size, b.size)
        val out = ShortArray(len)
        for (i in 0 until len) {
            val av = if (i < a.size) a[i].toInt() else 0
            val bv = if (i < b.size) b[i].toInt() else 0
            out[i] = (av + bv).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    // Envelope generators — fadeInS/fadeOutS let each game tune its attack/release.
    protected fun sineNoteEnv(freq: Double, durationS: Double, gain: Double, fadeInS: Double = 0.008, fadeOutS: Double = 0.040): ShortArray {
        val n = (sampleRate * durationS).toInt()
        val fadeIn = (sampleRate * fadeInS).toInt()
        val fadeOut = (sampleRate * fadeOutS).toInt()
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
            arr[i] = (wave * gain * env * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return arr
    }

    protected fun triNoteEnv(freq: Double, durationS: Double, gain: Double, fadeInS: Double = 0.008, fadeOutS: Double = 0.035): ShortArray {
        val n = (sampleRate * durationS).toInt()
        val fadeIn = (sampleRate * fadeInS).toInt()
        val fadeOut = (sampleRate * fadeOutS).toInt()
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
            arr[i] = (wave * gain * env * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return arr
    }

    // Noise with rise-and-fall envelope (triangle amplitude curve). Used by Brandung.
    protected fun noiseRiseAndFall(durationS: Double, peakGain: Double): ShortArray {
        val n = (sampleRate * durationS).toInt()
        val arr = ShortArray(n)
        val rng = java.util.Random()
        for (i in 0 until n) {
            val t = i.toDouble() / n
            val env = if (t < 0.5) t * 2.0 * peakGain else (1.0 - t) * 2.0 * peakGain
            arr[i] = ((rng.nextDouble() * 2 - 1) * env * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return arr
    }

    // Square wave with linear gain ramp (no frequency sweep). Used by Mahjong.
    protected fun squareBurst(freq: Double, durationS: Double, gainStart: Double, gainEnd: Double): ShortArray {
        val n = (sampleRate * durationS).toInt()
        val arr = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            phase += freq / sampleRate
            val wave = if (phase % 1.0 < 0.5) 1.0 else -1.0
            val gain = gainStart + (gainEnd - gainStart) * (i.toDouble() / n)
            arr[i] = (wave * gain * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return arr
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
        val pool = sfxPools[id]
        if (pool != null) {
            val now = System.currentTimeMillis()
            val entry = pool.firstOrNull { it.freeAfter <= now }
            if (entry != null) {
                entry.freeAfter = now + entry.durationMs
                scope.launch(Dispatchers.Default) {
                    try {
                        entry.track.stop()
                        entry.track.setPlaybackHeadPosition(0)
                        entry.track.play()
                    } catch (_: Exception) {}
                }
                return
            }
        }
        val samples = soundCache[id] ?: return
        scope.launch(Dispatchers.Default) { playRaw(samples) }
    }

    fun startMusic(soundEnabled: Boolean = this.soundEnabled, musicEnabled: Boolean = this.musicEnabled) {
        this.soundEnabled = soundEnabled
        this.musicEnabled = musicEnabled
        if (!musicEnabled) return
        stopMusic()
        AudioRegistry.current = this

        val ctx = context
        val assetName = musicAssetName()
        if (ctx != null && assetName != null) {
            // Try ExoPlayer with asset file; falls back to synthesis on error.
            musicSetupJob = scope.launch(Dispatchers.Main) {
                try {
                    val player = ExoPlayer.Builder(ctx).build()
                    musicPlayer = player
                    player.addListener(object : Player.Listener {
                        override fun onPlayerError(error: PlaybackException) {
                            if (musicPlayer === player) {
                                musicPlayer = null
                                scope.launch(Dispatchers.Main) {
                                    try { player.release() } catch (_: Exception) {}
                                }
                                if (musicEnabled) startMusicSynthesis()
                            }
                        }
                    })
                    player.setMediaItem(MediaItem.fromUri(android.net.Uri.parse("asset:///audio/music/$assetName")))
                    player.repeatMode = Player.REPEAT_MODE_ONE
                    player.prepare()
                    player.play()
                } catch (_: Exception) {
                    if (musicEnabled) startMusicSynthesis()
                }
            }
        } else {
            startMusicSynthesis()
        }
    }

    private fun startMusicSynthesis() {
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

    fun pauseMusic() {
        val playerActive = musicPlayer?.isPlaying == true
        val trackActive = musicTrack?.playState == AudioTrack.PLAYSTATE_PLAYING
        if (playerActive || trackActive) {
            pausedByLifecycle = true
            musicPlayer?.pause()
            try { musicTrack?.pause() } catch (_: Exception) {}
        }
    }

    fun resumeMusic() {
        if (!pausedByLifecycle || !musicEnabled) return
        pausedByLifecycle = false
        musicPlayer?.play()
        try { musicTrack?.play() } catch (_: Exception) {}
    }

    fun stopMusic() {
        pausedByLifecycle = false
        if (AudioRegistry.current === this) AudioRegistry.current = null
        musicSetupJob?.cancel(); musicSetupJob = null
        val p = musicPlayer; musicPlayer = null
        if (p != null) {
            // Use a fresh scope — the main scope may be cancelled immediately after (in release()),
            // which would prevent p.stop()/p.release() from executing and leave the player running.
            CoroutineScope(Dispatchers.Main).launch {
                try { p.stop(); p.release() } catch (_: Exception) {}
            }
        }
        val t = musicTrack; musicTrack = null
        try { t?.pause(); t?.flush(); t?.release() } catch (_: Exception) {}
    }

    fun setSound(enabled: Boolean) { soundEnabled = enabled }

    fun setMusic(enabled: Boolean) {
        musicEnabled = enabled
        if (!enabled) stopMusic() else startMusic()
    }

    fun release() {
        if (AudioRegistry.current === this) AudioRegistry.current = null
        stopMusic()
        sfxPools.values.forEach { entries ->
            entries.forEach { e ->
                try { e.track.stop(); e.track.release() } catch (_: Exception) {}
            }
        }
        sfxPools.clear()
        scope.cancel()
    }
}
