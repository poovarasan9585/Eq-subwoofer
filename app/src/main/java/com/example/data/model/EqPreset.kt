package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "eq_presets")
data class EqPreset(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val bandsGainJson: String, // comma-separated or JSON list of 10 float values
    val bassBoost: Int = 40,
    val treble: Int = 0,
    val virtualizer: Int = 0,
    val subwooferGain: Int = 50,
    val subwooferCutoff: Int = 80,
    val loudness: Boolean = true,
    val preampGain: Float = 0.0f,
    val isCustom: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getBands(): List<Float> {
        return try {
            bandsGainJson.split(",").map { it.trim().toFloat() }
        } catch (e: Exception) {
            List(10) { 0.0f }
        }
    }

    companion object {
        val BAND_FREQUENCIES = listOf(
            31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000
        )
        val BAND_LABELS = listOf(
            "31Hz", "62Hz", "125Hz", "250Hz", "500Hz", "1kHz", "2kHz", "4kHz", "8kHz", "16kHz"
        )

        fun create(
            name: String,
            bands: List<Float>,
            bassBoost: Int = 40,
            treble: Int = 0,
            virtualizer: Int = 0,
            subwooferGain: Int = 50,
            subwooferCutoff: Int = 80,
            loudness: Boolean = true,
            preampGain: Float = 0.0f,
            isCustom: Boolean = true
        ): EqPreset {
            return EqPreset(
                name = name,
                bandsGainJson = bands.joinToString(",") { String.format(java.util.Locale.US, "%.1f", it) },
                bassBoost = bassBoost,
                treble = treble,
                virtualizer = virtualizer,
                subwooferGain = subwooferGain,
                subwooferCutoff = subwooferCutoff,
                loudness = loudness,
                preampGain = preampGain,
                isCustom = isCustom
            )
        }

        val DEFAULT_PRESETS = listOf(
            create("Flat", listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f), bassBoost = 0, subwooferGain = 40, subwooferCutoff = 80, isCustom = false),
            create("Bass Boost", listOf(8f, 7f, 5f, 2f, 0f, 0f, 0f, 0f, 1f, 2f), bassBoost = 75, subwooferGain = 75, subwooferCutoff = 80, isCustom = false),
            create("Deep Bass", listOf(10f, 8f, 5f, 1f, -1f, -1f, 0f, 1f, 2f, 2f), bassBoost = 90, subwooferGain = 85, subwooferCutoff = 60, isCustom = false),
            create("Rock", listOf(5f, 4f, 2f, -1f, -2f, 0f, 3f, 5f, 6f, 6f), bassBoost = 50, treble = 30, subwooferGain = 55, subwooferCutoff = 90, isCustom = false),
            create("Pop", listOf(-1f, 1f, 3f, 4f, 3f, 0f, -1f, 1f, 3f, 4f), bassBoost = 40, treble = 20, subwooferGain = 50, subwooferCutoff = 100, isCustom = false),
            create("Classical", listOf(5f, 4f, 3f, 2f, -1f, -1f, 0f, 2f, 4f, 5f), bassBoost = 30, treble = 10, subwooferGain = 40, subwooferCutoff = 80, isCustom = false),
            create("Vocal", listOf(-3f, -2f, 0f, 3f, 5f, 5f, 3f, 1f, 0f, -2f), bassBoost = 20, treble = 10, virtualizer = 30, subwooferGain = 30, subwooferCutoff = 80, isCustom = false),
            create("Lo-Fi", listOf(-2f, 3f, 5f, 4f, 2f, 0f, -3f, -6f, -8f, -10f), bassBoost = 55, treble = -20, subwooferGain = 60, subwooferCutoff = 70, isCustom = false),
            create("Ambient", listOf(4f, 3f, 1f, 0f, 1f, 2f, 2f, 3f, 4f, 5f), bassBoost = 45, virtualizer = 60, subwooferGain = 50, subwooferCutoff = 80, isCustom = false),
            create("Electronic", listOf(8f, 7f, 3f, 0f, 1f, 2f, 4f, 6f, 7f, 8f), bassBoost = 80, treble = 40, subwooferGain = 80, subwooferCutoff = 90, isCustom = false)
        )
    }
}
