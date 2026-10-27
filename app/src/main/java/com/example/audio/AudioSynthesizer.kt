package com.example.audio

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import kotlin.math.PI
import kotlin.math.sin

data class AudioTrackItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val durationMs: Long,
    val uriString: String? = null,
    val isBuiltIn: Boolean = true
)

object AudioSynthesizer {

    fun getBuiltInTracks(context: Context): List<AudioTrackItem> {
        return listOf(
            AudioTrackItem(
                id = "track_sub_808",
                title = "808 Sub Kick & Groove",
                subtitle = "Deep Sub 45-60Hz • Punchy 808 & Crisp Hats",
                durationMs = 24000
            ),
            AudioTrackItem(
                id = "track_synthwave",
                title = "Cyber Synthwave Pulse",
                subtitle = "Warm Mid Chords • 75Hz Rolling Bassline",
                durationMs = 24000
            ),
            AudioTrackItem(
                id = "track_sub_sweep",
                title = "Subwoofer Crossover Sweep",
                subtitle = "Logarithmic 20Hz → 200Hz Sub Frequency Test",
                durationMs = 20000
            ),
            AudioTrackItem(
                id = "track_vocal_acoustic",
                title = "Acoustic Harmonics & Vocal",
                subtitle = "Rich Vocal Midrange (1-4kHz) & Open Air",
                durationMs = 24000
            )
        )
    }

    /**
     * Generates a 44100Hz 16-bit stereo WAV audio file in cacheDir if not already generated.
     */
    fun getOrGenerateTrackFile(context: Context, trackId: String): File {
        val cacheFile = File(context.cacheDir, "$trackId.wav")
        if (cacheFile.exists() && cacheFile.length() > 44) {
            return cacheFile
        }

        val sampleRate = 44100
        val durationSeconds = when (trackId) {
            "track_sub_sweep" -> 20
            else -> 24
        }
        val numSamples = sampleRate * durationSeconds
        val buffer = ShortArray(numSamples * 2) // Stereo (L, R)

        val twoPi = 2.0 * PI

        when (trackId) {
            "track_sub_808" -> {
                // BPM 120 (0.5s per beat).
                // Beat 1 & 3: Deep 808 kick (drops from 110Hz to 48Hz with sub decay)
                // Beat 2 & 4: Snare snap (200Hz + white noise)
                // 1/8th notes: Hi-hat ticks (10kHz - 15kHz)
                // Underlying chord pad (A2 110Hz + E3 165Hz)
                val samplesPerBeat = sampleRate / 2
                for (i in 0 until numSamples) {
                    val beatIndex = i % samplesPerBeat
                    val beatNumber = (i / samplesPerBeat) % 4
                    val timeSec = i.toDouble() / sampleRate
                    var sampleVal = 0.0

                    // 1. 808 Sub Kick on beat 0 and 2
                    if (beatNumber == 0 || beatNumber == 2) {
                        val tBeat = beatIndex.toDouble() / sampleRate
                        if (tBeat < 0.4) {
                            val decay = (1.0 - tBeat / 0.4) * (1.0 - tBeat / 0.4)
                            val instantFreq = 50.0 + 70.0 * Math.exp(-tBeat * 14.0)
                            sampleVal += sin(twoPi * instantFreq * tBeat) * 0.75 * decay
                        }
                    }

                    // 2. Snare / Clack on beat 1 and 3
                    if (beatNumber == 1 || beatNumber == 3) {
                        val tBeat = beatIndex.toDouble() / sampleRate
                        if (tBeat < 0.2) {
                            val decay = (1.0 - tBeat / 0.2) * (1.0 - tBeat / 0.2)
                            val noise = (Math.random() * 2.0 - 1.0) * 0.35 * decay
                            val tone = sin(twoPi * 180.0 * tBeat) * 0.3 * decay
                            sampleVal += (noise + tone)
                        }
                    }

                    // 3. Crisp Hi-hats every 1/8 note
                    val eighthIndex = i % (sampleRate / 4)
                    val tEighth = eighthIndex.toDouble() / sampleRate
                    if (tEighth < 0.05) {
                        val hatDecay = (1.0 - tEighth / 0.05)
                        val hatNoise = (Math.random() * 2.0 - 1.0) * 0.2 * hatDecay
                        sampleVal += hatNoise
                    }

                    // 4. Warm harmonic synth bass & chord pad
                    val pad = (sin(twoPi * 110.0 * timeSec) * 0.15 +
                            sin(twoPi * 164.81 * timeSec) * 0.12 +
                            sin(twoPi * 220.0 * timeSec) * 0.08)
                    sampleVal += pad

                    val clamped = (sampleVal * 28000.0).coerceIn(-32767.0, 32767.0).toInt().toShort()
                    buffer[i * 2] = clamped
                    buffer[i * 2 + 1] = clamped
                }
            }
            "track_sub_sweep" -> {
                // Logarithmic sweep 20Hz -> 200Hz repeating every 10 seconds
                for (i in 0 until numSamples) {
                    val cycleTime = (i % (sampleRate * 10)).toDouble() / (sampleRate * 10)
                    // Freq from 20 to 200
                    val freq = 20.0 * Math.pow(10.0, cycleTime)
                    val phase = twoPi * 20.0 * (Math.pow(10.0, cycleTime) - 1.0) / Math.log(10.0) * 10.0
                    val sample = sin(phase) * 0.75
                    val sVal = (sample * 28000.0).coerceIn(-32767.0, 32767.0).toInt().toShort()
                    buffer[i * 2] = sVal
                    buffer[i * 2 + 1] = sVal
                }
            }
            "track_synthwave" -> {
                // Synthwave groove with rolling 16th bass
                val samplesPer16th = sampleRate / 8
                for (i in 0 until numSamples) {
                    val timeSec = i.toDouble() / sampleRate
                    val idx16 = i % samplesPer16th
                    val t16 = idx16.toDouble() / sampleRate

                    val bassFreq = when ((i / (sampleRate * 2)) % 4) {
                        0 -> 65.41 // C2
                        1 -> 58.27 // Bb1
                        2 -> 51.91 // Ab1
                        else -> 43.65 // F1
                    }
                    val bassEnv = (1.0 - t16 / (samplesPer16th.toDouble() / sampleRate)).coerceIn(0.0, 1.0)
                    // Rich sawtooth bass
                    val saw = (sin(twoPi * bassFreq * timeSec) +
                            0.5 * sin(twoPi * bassFreq * 2.0 * timeSec) +
                            0.25 * sin(twoPi * bassFreq * 3.0 * timeSec)) * 0.45 * bassEnv

                    // Shimmer lead
                    val lead = sin(twoPi * (bassFreq * 8.0) * timeSec) * 0.15

                    val combined = (saw + lead)
                    val sVal = (combined * 26000.0).coerceIn(-32767.0, 32767.0).toInt().toShort()
                    buffer[i * 2] = sVal
                    buffer[i * 2 + 1] = sVal
                }
            }
            else -> {
                // Vocal / Acoustic Harmonics
                for (i in 0 until numSamples) {
                    val timeSec = i.toDouble() / sampleRate
                    val baseFreq = 196.0 // G3
                    val vib = sin(twoPi * 5.0 * timeSec) * 2.5
                    val vocal = (sin(twoPi * (baseFreq + vib) * timeSec) * 0.35 +
                            sin(twoPi * (baseFreq * 2.0) * timeSec) * 0.25 +
                            sin(twoPi * 1200.0 * timeSec) * 0.12 +
                            sin(twoPi * 2800.0 * timeSec) * 0.08)
                    val sVal = (vocal * 26000.0).coerceIn(-32767.0, 32767.0).toInt().toShort()
                    buffer[i * 2] = sVal
                    buffer[i * 2 + 1] = sVal
                }
            }
        }

        writeWavFile(cacheFile, buffer, sampleRate, 2)
        return cacheFile
    }

    private fun writeWavFile(file: File, buffer: ShortArray, sampleRate: Int, channels: Int) {
        val fos = FileOutputStream(file)
        val byteRate = sampleRate * channels * 2
        val dataSize = buffer.size * 2
        val totalSize = 36 + dataSize

        // 44-byte WAV Header
        val header = ByteArray(44)
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalSize and 0xff).toByte()
        header[5] = ((totalSize shr 8) and 0xff).toByte()
        header[6] = ((totalSize shr 16) and 0xff).toByte()
        header[7] = ((totalSize shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // Subchunk1Size (16 for PCM)
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // AudioFormat 1 = PCM
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (channels * 2).toByte() // Block align
        header[33] = 0
        header[34] = 16 // Bits per sample
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (dataSize and 0xff).toByte()
        header[41] = ((dataSize shr 8) and 0xff).toByte()
        header[42] = ((dataSize shr 16) and 0xff).toByte()
        header[43] = ((dataSize shr 24) and 0xff).toByte()

        fos.write(header)

        // Write PCM ShortArray
        val byteBuffer = ByteArray(buffer.size * 2)
        var bIdx = 0
        for (sample in buffer) {
            byteBuffer[bIdx++] = (sample.toInt() and 0xff).toByte()
            byteBuffer[bIdx++] = ((sample.toInt() shr 8) and 0xff).toByte()
        }
        fos.write(byteBuffer)
        fos.flush()
        fos.close()
    }
}
