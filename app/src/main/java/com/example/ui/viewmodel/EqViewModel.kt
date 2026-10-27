package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioEngine
import com.example.audio.AudioTrackItem
import com.example.audio.PlaybackState
import com.example.data.model.EqPreset
import com.example.data.repository.EqRepository
import com.example.ui.theme.AccentTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EqViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = EqRepository(application)
    val audioEngine = AudioEngine(application)

    val playbackState: StateFlow<PlaybackState> = audioEngine.playbackState
    val visualizerBins: StateFlow<FloatArray> = audioEngine.visualizerBins

    val presets: StateFlow<List<EqPreset>> = repository.getAllPresets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EqPreset.DEFAULT_PRESETS)

    private val _activePresetName = MutableStateFlow("Bass Boost")
    val activePresetName: StateFlow<String> = _activePresetName.asStateFlow()

    private val _accentTheme = MutableStateFlow(repository.getSavedAccentTheme())
    val accentTheme: StateFlow<AccentTheme> = _accentTheme.asStateFlow()

    private val _currentScreen = MutableStateFlow(0) // 0: EQ, 1: Subwoofer, 2: Player, 3: Settings
    val currentScreen: StateFlow<Int> = _currentScreen.asStateFlow()

    private val _showSavePresetDialog = MutableStateFlow(false)
    val showSavePresetDialog: StateFlow<Boolean> = _showSavePresetDialog.asStateFlow()

    init {
        // Load initial state
        val masterEnabled = repository.isMasterEqEnabled()
        audioEngine.setMasterEqEnabled(masterEnabled)

        val subEnabled = repository.isSubwooferEnabled()
        audioEngine.setSubwooferEnabled(subEnabled)

        val subSlope = repository.getSubwooferSlope()
        audioEngine.setSubwooferSlope(subSlope)

        val subPhase = repository.isSubwooferPhaseInverted()
        audioEngine.setSubwooferPhaseInverted(subPhase)

        val subHarmonics = repository.isSubHarmonicsEnabled()
        audioEngine.setSubHarmonicsEnabled(subHarmonics)

        // Select Bass Boost default preset
        val defaultPreset = EqPreset.DEFAULT_PRESETS.firstOrNull { it.name == "Bass Boost" }
            ?: EqPreset.DEFAULT_PRESETS.first()
        applyPreset(defaultPreset)
    }

    fun setScreen(index: Int) {
        _currentScreen.value = index
    }

    fun setAccentTheme(theme: AccentTheme) {
        _accentTheme.value = theme
        repository.setSavedAccentTheme(theme)
    }

    fun toggleMasterEq() {
        val next = !playbackState.value.isMasterEqEnabled
        audioEngine.setMasterEqEnabled(next)
        repository.setMasterEqEnabled(next)
    }

    fun setBandGain(index: Int, gainDb: Float) {
        audioEngine.setBandGain(index, gainDb)
        _activePresetName.value = "Custom"
    }

    fun resetEq() {
        val flat = EqPreset.DEFAULT_PRESETS.firstOrNull { it.name == "Flat" } ?: return
        applyPreset(flat)
    }

    fun setBassBoost(strength: Int) {
        audioEngine.setBassBoost(strength)
        _activePresetName.value = "Custom"
    }

    fun setTreble(strength: Int) {
        audioEngine.setTreble(strength)
        _activePresetName.value = "Custom"
    }

    fun setVirtualizer(strength: Int) {
        audioEngine.setVirtualizer(strength)
        _activePresetName.value = "Custom"
    }

    fun toggleSubwoofer() {
        val next = !playbackState.value.subwooferEnabled
        audioEngine.setSubwooferEnabled(next)
        repository.setSubwooferEnabled(next)
    }

    fun setSubwooferGain(gain: Int) {
        audioEngine.setSubwooferGain(gain)
    }

    fun setSubwooferCutoff(cutoffHz: Int) {
        audioEngine.setSubwooferCutoff(cutoffHz)
    }

    fun setSubwooferSlope(slope: Int) {
        audioEngine.setSubwooferSlope(slope)
        repository.setSubwooferSlope(slope)
    }

    fun toggleSubwooferPhase() {
        val next = !playbackState.value.subwooferPhaseInverted
        audioEngine.setSubwooferPhaseInverted(next)
        repository.setSubwooferPhaseInverted(next)
    }

    fun toggleSubHarmonics() {
        val next = !playbackState.value.subHarmonicsEnabled
        audioEngine.setSubHarmonicsEnabled(next)
        repository.setSubHarmonicsEnabled(next)
    }

    fun toggleLoudness() {
        val next = !playbackState.value.loudnessEnabled
        audioEngine.setLoudnessEnabled(next)
    }

    fun setPreampGain(gainDb: Float) {
        audioEngine.setPreampGain(gainDb)
    }

    fun setStereoBalance(balance: Float) {
        audioEngine.setStereoBalance(balance)
    }

    fun applyPreset(preset: EqPreset) {
        _activePresetName.value = preset.name
        audioEngine.applyPreset(preset)
        repository.setSavedActivePresetId(preset.id)
    }

    fun openSavePresetDialog() {
        _showSavePresetDialog.value = true
    }

    fun closeSavePresetDialog() {
        _showSavePresetDialog.value = false
    }

    fun saveCurrentAsPreset(name: String) {
        if (name.isBlank()) return
        val current = playbackState.value
        val preset = EqPreset.create(
            name = name.trim(),
            bands = current.activeBandsGain,
            bassBoost = current.bassBoostStrength,
            treble = current.trebleStrength,
            virtualizer = current.virtualizerStrength,
            subwooferGain = current.subwooferGain,
            subwooferCutoff = current.subwooferCutoffHz,
            loudness = current.loudnessEnabled,
            preampGain = current.preampGainDb,
            isCustom = true
        )
        viewModelScope.launch {
            repository.saveCustomPreset(preset)
            _activePresetName.value = preset.name
            closeSavePresetDialog()
        }
    }

    fun deleteCustomPreset(id: Long) {
        viewModelScope.launch {
            repository.deletePreset(id)
            if (_activePresetName.value != "Bass Boost") {
                _activePresetName.value = "Custom"
            }
        }
    }

    // Playback functions
    fun togglePlayPause() = audioEngine.togglePlayPause()
    fun nextTrack() = audioEngine.nextTrack()
    fun previousTrack() = audioEngine.previousTrack()
    fun seekTo(positionMs: Long) = audioEngine.seekTo(positionMs)
    fun playTrack(track: AudioTrackItem) = audioEngine.prepareTrack(track, autoPlay = true)
    fun addLocalTrack(uri: Uri, name: String) = audioEngine.addCustomTrack(uri, name)

    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
    }
}
