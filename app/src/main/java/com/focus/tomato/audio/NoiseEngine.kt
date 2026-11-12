package com.focus.tomato.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.Random
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sin

/**
 * Procedurally generated, offline-only ambient noise. No audio assets, no
 * network, no recording permission. A single AudioTrack is fed by a daemon
 * thread so the sound survives activity recreation while the service lives.
 */
object NoiseEngine {
    private const val SAMPLE_RATE = 44_100
    private const val CHANNEL = AudioFormat.CHANNEL_OUT_MONO
    private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT

    private var track: AudioTrack? = null
    private var thread: Thread? = null
    @Volatile private var running = false
    @Volatile private var volume = 0.55f

    fun start(preset: NoisePreset, volume: Float = 0.55f) {
        stop()
        this.volume = volume.coerceIn(0f, 1f)
        running = true

        val minBuffer = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL, ENCODING)
            .coerceAtLeast(4096)
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(ENCODING)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(CHANNEL)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(minBuffer * 2)
            .build()
        track = audioTrack
        audioTrack.play()

        thread = Thread({
            val synth = NoiseSynth(preset)
            val buffer = ShortArray(2048)
            while (running) {
                synth.fill(buffer, this.volume)
                try {
                    audioTrack.write(buffer, 0, buffer.size)
                } catch (_: Exception) {
                    break
                }
            }
        }, "tomato-noise").apply {
            isDaemon = true
            start()
        }
    }

    fun setVolume(value: Float) {
        volume = value.coerceIn(0f, 1f)
    }

    fun stop() {
        running = false
        thread?.let { t ->
            runCatching { t.join(250) }
        }
        thread = null
        track?.let { t ->
            runCatching { t.pause() }
            runCatching { t.flush() }
            runCatching { t.stop() }
            runCatching { t.release() }
        }
        track = null
    }

    fun isRunning(): Boolean = running
}

private class NoiseSynth(private val preset: NoisePreset) {
    private val random = Random(0x5EEDL + preset.ordinal)
    private var brown = 0f
    private var low = 0f
    private var phase = 0.0
    private var droplet = 0f
    private var chirp = 0f
    private var chirpPhase = 0.0
    private var crackle = 0f
    private var sampleIndex = 0L

    fun fill(buffer: ShortArray, volume: Float) {
        for (i in buffer.indices) {
            val white = random.nextFloat() * 2f - 1f
            // Brown-ish integrator and a one-pole low pass, both cheap and stable.
            brown = (brown + white * 0.018f).coerceIn(-1f, 1f) * 0.998f
            low += 0.08f * (white - low)
            phase += 2.0 * PI * 0.07 / 44_100.0

            val sample = when (preset) {
                NoisePreset.RAIN -> rain(white, low)
                NoisePreset.FOREST -> forest(brown)
                NoisePreset.OCEAN -> ocean(brown, low)
                NoisePreset.CAFE -> cafe(brown, low)
                NoisePreset.FIRE -> fire(brown)
                NoisePreset.BROWN -> brown * 0.65f
            }
            val v = (sample * volume).coerceIn(-1f, 1f)
            buffer[i] = (v * Short.MAX_VALUE).toInt().toShort()
            sampleIndex++
        }
    }

    private fun rain(white: Float, low: Float): Float {
        // Occasional droplets: short decaying high-frequency pings.
        if (random.nextFloat() < 0.000018f) droplet = 0.35f
        val drop = if (droplet > 0f) {
            val s = sin(sampleIndex * 2.0 * PI * 1450.0 / 44_100.0).toFloat() * droplet
            droplet *= 0.99982f
            if (droplet < 0.001f) droplet = 0f
            s
        } else 0f
        return white * 0.16f + low * 0.22f + drop
    }

    private fun forest(brown: Float): Float {
        if (random.nextFloat() < 0.000012f) {
            chirp = 0.22f
            chirpPhase = 0.0
        }
        val bird = if (chirp > 0f) {
            chirpPhase += 2.0 * PI * (2100.0 + 500.0 * sin(chirpPhase * 0.7)) / 44_100.0
            val s = sin(chirpPhase).toFloat() * chirp
            chirp *= 0.99965f
            if (chirp < 0.002f) chirp = 0f
            s
        } else 0f
        return brown * 0.30f + low * 0.10f + bird
    }

    private fun ocean(brown: Float, low: Float): Float {
        val swell = (0.55f + 0.45f * sin(phase).toFloat()).coerceIn(0.1f, 1f)
        return brown * 0.38f * swell + low * 0.10f * swell
    }

    private fun cafe(brown: Float, low: Float): Float {
        val murmur = low * (0.55f + 0.15f * sin(phase * 2.7).toFloat())
        return brown * 0.22f + murmur * 0.35f + whiteSoft()
    }

    private fun fire(brown: Float): Float {
        if (random.nextFloat() < 0.00035f) crackle = 0.45f
        val crack = if (crackle > 0f) {
            val s = (random.nextFloat() * 2f - 1f) * crackle
            crackle *= 0.982f
            if (crackle < 0.002f) crackle = 0f
            s
        } else 0f
        return brown * 0.25f + crack * 0.22f
    }

    private fun whiteSoft(): Float = low * 0.06f
}
