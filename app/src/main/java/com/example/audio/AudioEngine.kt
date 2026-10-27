package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.net.Uri
import android.util.Log
import com.example.data.model.EqPreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentTrack: AudioTrackItem? = null,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 1L,
    val isLooping: Boolean = true,
    val audioSessionId: Int = 0,
    val isMasterEqEnabled: Boolean = true,
    val isNativeEqualizerSupported: Boolean = false,
    val isNativeBassBoostSupported: Boolean = false,
    val isNativeVirtualizerSupported: Boolean = false,
    val isNativeLoudnessSupported: Boolean = false,
    val activeBandsGain: List<Float> = List(10) { 0f },
    val bassBoostStrength: Int = 40,
    val trebleStrength: Int = 0,
    val virtualizerStrength: Int = 0,
    val subwooferEnabled: Boolean = true,
    val subwooferGain: Int = 60,
    val subwooferCutoffHz: Int = 80,
    val subwooferSlope: Int = 24,
    val subwooferPhaseInverted: Boolean = false,
    val subHarmonicsEnabled: Boolean = true,
    val loudnessEnabled: Boolean = true,
    val preampGainDb: Float = 0f,
    val stereoBalance: Float = 0f
)

class AudioEngine(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private var mediaPlayer: MediaPlayer? = null
    private var nativeEqualizer: Equalizer? = null
    private var nativeBassBoost: BassBoost? = null
    private var nativeVirtualizer: Virtualizer? = null
    private var nativeLoudness: LoudnessEnhancer? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _visualizerBins = MutableStateFlow(FloatArray(32))
    val visualizerBins: StateFlow<FloatArray> = _visualizerBins.asStateFlow()

    private var progressJob: Job? = null
    private var synthVisualizerJob: Job? = null

    val trackList = mutableListOf<AudioTrackItem>()

    init {
        trackList.addAll(AudioSynthesizer.getBuiltInTracks(context))
        _playbackState.value = _playbackState.value.copy(
            currentTrack = trackList.firstOrNull()
        )
        startVisualizerTicker()
    }

    fun prepareTrack(track: AudioTrackItem, autoPlay: Boolean = false) {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            releaseAudioFx()

            val mp = MediaPlayer()
            mediaPlayer = mp

            if (track.isBuiltIn) {
                val file = AudioSynthesizer.getOrGenerateTrackFile(context, track.id)
                mp.setDataSource(file.absolutePath)
            } else if (track.uriString != null) {
                mp.setDataSource(context, Uri.parse(track.uriString))
            }

            mp.isLooping = _playbackState.value.isLooping
            mp.prepare()

            val sessionId = mp.audioSessionId
            initAudioFx(sessionId)

            val duration = if (mp.duration > 0) mp.duration.toLong() else track.durationMs
            _playbackState.value = _playbackState.value.copy(
                currentTrack = track,
                currentPositionMs = 0L,
                durationMs = duration,
                audioSessionId = sessionId
            )

            applyAllAudioFx()

            if (autoPlay) {
                mp.start()
                _playbackState.value = _playbackState.value.copy(isPlaying = true)
                startProgressTicker()
            }
        } catch (e: Exception) {
            Log.e("AudioEngine", "Error preparing track: ${e.message}", e)
        }
    }

    fun togglePlayPause() {
        val mp = mediaPlayer
        if (mp == null) {
            val track = _playbackState.value.currentTrack ?: trackList.firstOrNull()
            if (track != null) {
                prepareTrack(track, autoPlay = true)
            }
            return
        }

        if (mp.isPlaying) {
            mp.pause()
            _playbackState.value = _playbackState.value.copy(isPlaying = false)
            progressJob?.cancel()
        } else {
            mp.start()
            _playbackState.value = _playbackState.value.copy(isPlaying = true)
            startProgressTicker()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.seekTo(positionMs.toInt())
        _playbackState.value = _playbackState.value.copy(currentPositionMs = positionMs)
    }

    fun nextTrack() {
        val current = _playbackState.value.currentTrack
        val idx = trackList.indexOfFirst { it.id == current?.id }
        val nextIdx = if (idx >= 0 && idx < trackList.size - 1) idx + 1 else 0
        val next = trackList.getOrNull(nextIdx)
        if (next != null) {
            prepareTrack(next, autoPlay = _playbackState.value.isPlaying)
        }
    }

    fun previousTrack() {
        val current = _playbackState.value.currentTrack
        val idx = trackList.indexOfFirst { it.id == current?.id }
        val prevIdx = if (idx > 0) idx - 1 else trackList.size - 1
        val prev = trackList.getOrNull(prevIdx)
        if (prev != null) {
            prepareTrack(prev, autoPlay = _playbackState.value.isPlaying)
        }
    }

    fun addCustomTrack(uri: Uri, name: String) {
        val item = AudioTrackItem(
            id = "user_track_${System.currentTimeMillis()}",
            title = name,
            subtitle = "Local Audio File",
            durationMs = 0L,
            uriString = uri.toString(),
            isBuiltIn = false
        )
        trackList.add(item)
        prepareTrack(item, autoPlay = true)
    }

    fun setMasterEqEnabled(enabled: Boolean) {
        _playbackState.value = _playbackState.value.copy(isMasterEqEnabled = enabled)
        applyAllAudioFx()
    }

    fun setBandsGain(bands: List<Float>) {
        _playbackState.value = _playbackState.value.copy(activeBandsGain = bands)
        applyEqualizerBands()
    }

    fun setBandGain(index: Int, gainDb: Float) {
        val current = _playbackState.value.activeBandsGain.toMutableList()
        if (index in current.indices) {
            current[index] = gainDb
            _playbackState.value = _playbackState.value.copy(activeBandsGain = current)
            applyEqualizerBands()
        }
    }

    fun setBassBoost(strength: Int) {
        _playbackState.value = _playbackState.value.copy(bassBoostStrength = strength.coerceIn(0, 100))
        applyBassBoost()
    }

    fun setTreble(strength: Int) {
        _playbackState.value = _playbackState.value.copy(trebleStrength = strength.coerceIn(-50, 50))
        applyTreble()
    }

    fun setVirtualizer(strength: Int) {
        _playbackState.value = _playbackState.value.copy(virtualizerStrength = strength.coerceIn(0, 100))
        applyVirtualizer()
    }

    fun setSubwooferEnabled(enabled: Boolean) {
        _playbackState.value = _playbackState.value.copy(subwooferEnabled = enabled)
        applySubwoofer()
    }

    fun setSubwooferGain(gain: Int) {
        _playbackState.value = _playbackState.value.copy(subwooferGain = gain.coerceIn(0, 100))
        applySubwoofer()
    }

    fun setSubwooferCutoff(cutoffHz: Int) {
        _playbackState.value = _playbackState.value.copy(subwooferCutoffHz = cutoffHz.coerceIn(20, 200))
        applySubwoofer()
    }

    fun setSubwooferSlope(slope: Int) {
        _playbackState.value = _playbackState.value.copy(subwooferSlope = slope)
    }

    fun setSubwooferPhaseInverted(inverted: Boolean) {
        _playbackState.value = _playbackState.value.copy(subwooferPhaseInverted = inverted)
    }

    fun setSubHarmonicsEnabled(enabled: Boolean) {
        _playbackState.value = _playbackState.value.copy(subHarmonicsEnabled = enabled)
    }

    fun setLoudnessEnabled(enabled: Boolean) {
        _playbackState.value = _playbackState.value.copy(loudnessEnabled = enabled)
        applyLoudness()
    }

    fun setPreampGain(gainDb: Float) {
        _playbackState.value = _playbackState.value.copy(preampGainDb = gainDb.coerceIn(-6f, 6f))
        applyVolumeBalance()
    }

    fun setStereoBalance(balance: Float) {
        _playbackState.value = _playbackState.value.copy(stereoBalance = balance.coerceIn(-1f, 1f))
        applyVolumeBalance()
    }

    fun applyPreset(preset: EqPreset) {
        _playbackState.value = _playbackState.value.copy(
            activeBandsGain = preset.getBands(),
            bassBoostStrength = preset.bassBoost,
            trebleStrength = preset.treble,
            virtualizerStrength = preset.virtualizer,
            subwooferGain = preset.subwooferGain,
            subwooferCutoffHz = preset.subwooferCutoff,
            loudnessEnabled = preset.loudness,
            preampGainDb = preset.preampGain
        )
        applyAllAudioFx()
    }

    private fun initAudioFx(sessionId: Int) {
        try {
            nativeEqualizer = Equalizer(0, sessionId).apply {
                enabled = _playbackState.value.isMasterEqEnabled
            }
            _playbackState.value = _playbackState.value.copy(isNativeEqualizerSupported = true)
        } catch (e: Exception) {
            Log.w("AudioEngine", "Native Equalizer not available: ${e.message}")
            _playbackState.value = _playbackState.value.copy(isNativeEqualizerSupported = false)
        }

        try {
            nativeBassBoost = BassBoost(0, sessionId).apply {
                enabled = _playbackState.value.isMasterEqEnabled
            }
            _playbackState.value = _playbackState.value.copy(isNativeBassBoostSupported = true)
        } catch (e: Exception) {
            Log.w("AudioEngine", "Native BassBoost not available: ${e.message}")
            _playbackState.value = _playbackState.value.copy(isNativeBassBoostSupported = false)
        }

        try {
            nativeVirtualizer = Virtualizer(0, sessionId).apply {
                enabled = _playbackState.value.isMasterEqEnabled
            }
            _playbackState.value = _playbackState.value.copy(isNativeVirtualizerSupported = true)
        } catch (e: Exception) {
            Log.w("AudioEngine", "Native Virtualizer not available: ${e.message}")
            _playbackState.value = _playbackState.value.copy(isNativeVirtualizerSupported = false)
        }

        try {
            nativeLoudness = LoudnessEnhancer(sessionId).apply {
                enabled = _playbackState.value.isMasterEqEnabled && _playbackState.value.loudnessEnabled
            }
            _playbackState.value = _playbackState.value.copy(isNativeLoudnessSupported = true)
        } catch (e: Exception) {
            Log.w("AudioEngine", "Native LoudnessEnhancer not available: ${e.message}")
            _playbackState.value = _playbackState.value.copy(isNativeLoudnessSupported = false)
        }
    }

    private fun applyAllAudioFx() {
        val masterEnabled = _playbackState.value.isMasterEqEnabled
        try {
            nativeEqualizer?.enabled = masterEnabled
            nativeBassBoost?.enabled = masterEnabled
            nativeVirtualizer?.enabled = masterEnabled
            nativeLoudness?.enabled = masterEnabled && _playbackState.value.loudnessEnabled
        } catch (e: Exception) {
            Log.w("AudioEngine", "Error setting effect enabled: ${e.message}")
        }

        applyEqualizerBands()
        applyBassBoost()
        applyTreble()
        applyVirtualizer()
        applySubwoofer()
        applyLoudness()
        applyVolumeBalance()
    }

    private fun applyEqualizerBands() {
        val eq = nativeEqualizer ?: return
        if (!_playbackState.value.isMasterEqEnabled) return
        try {
            val numBands = eq.numberOfBands.toInt()
            val minDb = eq.bandLevelRange[0]
            val maxDb = eq.bandLevelRange[1]
            val gains = _playbackState.value.activeBandsGain

            for (i in 0 until numBands) {
                val mappedIndex = (i * gains.size / numBands).coerceIn(0, gains.size - 1)
                val gainDb = gains[mappedIndex]
                val levelMb = (gainDb * 100).toInt().coerceIn(minDb.toInt(), maxDb.toInt()).toShort()
                eq.setBandLevel(i.toShort(), levelMb)
            }
        } catch (e: Exception) {
            Log.w("AudioEngine", "Failed to apply EQ band levels: ${e.message}")
        }
    }

    private fun applyBassBoost() {
        val bb = nativeBassBoost ?: return
        if (!_playbackState.value.isMasterEqEnabled) return
        try {
            val strength = _playbackState.value.bassBoostStrength
            val hwStrength = (strength * 10).toShort().coerceIn(0, 1000)
            bb.setStrength(hwStrength)
        } catch (e: Exception) {
            Log.w("AudioEngine", "Failed to apply BassBoost: ${e.message}")
        }
    }

    private fun applyTreble() {
        val eq = nativeEqualizer ?: return
        if (!_playbackState.value.isMasterEqEnabled) return
        try {
            val numBands = eq.numberOfBands.toInt()
            if (numBands > 0) {
                val lastBand = (numBands - 1).toShort()
                val currentLevel = eq.getBandLevel(lastBand)
                val delta = (_playbackState.value.trebleStrength * 10).toShort()
                val newLevel = (currentLevel + delta).coerceIn(eq.bandLevelRange[0].toInt(), eq.bandLevelRange[1].toInt()).toShort()
                eq.setBandLevel(lastBand, newLevel)
            }
        } catch (e: Exception) {
            Log.w("AudioEngine", "Failed to apply Treble: ${e.message}")
        }
    }

    private fun applyVirtualizer() {
        val virt = nativeVirtualizer ?: return
        if (!_playbackState.value.isMasterEqEnabled) return
        try {
            val strength = (_playbackState.value.virtualizerStrength * 10).toShort().coerceIn(0, 1000)
            virt.setStrength(strength)
        } catch (e: Exception) {
            Log.w("AudioEngine", "Failed to apply Virtualizer: ${e.message}")
        }
    }

    private fun applySubwoofer() {
        val eq = nativeEqualizer ?: return
        if (!_playbackState.value.isMasterEqEnabled) return
        try {
            if (_playbackState.value.subwooferEnabled) {
                val subGain = _playbackState.value.subwooferGain
                val subMb = ((subGain / 100f) * 800f).toInt().toShort()
                if (eq.numberOfBands > 0) {
                    val band0 = 0.toShort()
                    val target = (eq.getBandLevel(band0) + subMb).coerceIn(eq.bandLevelRange[0].toInt(), eq.bandLevelRange[1].toInt()).toShort()
                    eq.setBandLevel(band0, target)
                }
            }
        } catch (e: Exception) {
            Log.w("AudioEngine", "Failed to apply Subwoofer: ${e.message}")
        }
    }

    private fun applyLoudness() {
        val le = nativeLoudness ?: return
        try {
            val enabled = _playbackState.value.isMasterEqEnabled && _playbackState.value.loudnessEnabled
            le.enabled = enabled
            if (enabled) {
                le.setTargetGain(600)
            }
        } catch (e: Exception) {
            Log.w("AudioEngine", "Failed to apply LoudnessEnhancer: ${e.message}")
        }
    }

    private fun applyVolumeBalance() {
        val mp = mediaPlayer ?: return
        try {
            val preamp = _playbackState.value.preampGainDb
            val baseVol = Math.pow(10.0, (preamp / 20.0)).toFloat().coerceIn(0.1f, 1.5f)
            val bal = _playbackState.value.stereoBalance
            val left = (baseVol * (1f - bal.coerceAtLeast(0f))).coerceIn(0f, 1f)
            val right = (baseVol * (1f + bal.coerceAtMost(0f))).coerceIn(0f, 1f)
            mp.setVolume(left, right)
        } catch (e: Exception) {
            Log.w("AudioEngine", "Failed to apply volume balance: ${e.message}")
        }
    }

    private fun releaseAudioFx() {
        try {
            nativeEqualizer?.release()
            nativeBassBoost?.release()
            nativeVirtualizer?.release()
            nativeLoudness?.release()
        } catch (e: Exception) {
            Log.w("AudioEngine", "Error releasing FX: ${e.message}")
        } finally {
            nativeEqualizer = null
            nativeBassBoost = null
            nativeVirtualizer = null
            nativeLoudness = null
        }
    }

    private fun startProgressTicker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val mp = mediaPlayer
                if (mp != null && mp.isPlaying) {
                    _playbackState.value = _playbackState.value.copy(
                        currentPositionMs = mp.currentPosition.toLong(),
                        durationMs = if (mp.duration > 0) mp.duration.toLong() else _playbackState.value.durationMs
                    )
                }
                delay(200)
            }
        }
    }

    private fun startVisualizerTicker() {
        synthVisualizerJob?.cancel()
        synthVisualizerJob = scope.launch(Dispatchers.Default) {
            val bins = FloatArray(32)
            var phase = 0f
            while (isActive) {
                val isPlaying = _playbackState.value.isPlaying
                val gains = _playbackState.value.activeBandsGain
                val subGain = if (_playbackState.value.subwooferEnabled) _playbackState.value.subwooferGain / 100f else 0.1f
                val bassBoost = _playbackState.value.bassBoostStrength / 100f

                phase += 0.15f
                for (i in 0 until 32) {
                    if (isPlaying) {
                        val bandIdx = (i * gains.size / 32).coerceIn(0, gains.size - 1)
                        val eqFactor = (gains[bandIdx] + 12f) / 24f

                        val subBonus = if (i < 8) subGain * 0.4f + bassBoost * 0.3f else 0f
                        val wave = (sin(phase + i * 0.35f) * 0.3f + cos(phase * 0.8f + i * 0.1f) * 0.2f).toFloat()
                        val noise = (Math.random() * 0.15f).toFloat()

                        val target = (0.25f + wave + subBonus + eqFactor * 0.35f + noise).coerceIn(0.08f, 0.98f)
                        bins[i] = bins[i] * 0.7f + target * 0.3f
                    } else {
                        val idleWave = (sin(phase * 0.5f + i * 0.2f) * 0.05f).toFloat()
                        bins[i] = bins[i] * 0.85f + (0.04f + idleWave).coerceAtLeast(0.02f) * 0.15f
                    }
                }
                _visualizerBins.value = bins.copyOf()
                delay(25)
            }
        }
    }

    fun release() {
        progressJob?.cancel()
        synthVisualizerJob?.cancel()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        releaseAudioFx()
    }
}
