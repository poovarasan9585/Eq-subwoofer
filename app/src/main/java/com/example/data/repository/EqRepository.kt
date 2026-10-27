package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.EqDatabase
import com.example.data.model.EqPreset
import com.example.ui.theme.AccentTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class EqRepository(context: Context) {
    private val db = EqDatabase.getInstance(context)
    private val dao = db.eqPresetDao()
    private val prefs: SharedPreferences =
        context.getSharedPreferences("aura_eq_prefs", Context.MODE_PRIVATE)

    fun getAllPresets(): Flow<List<EqPreset>> = dao.getAllPresets()

    fun getCustomPresets(): Flow<List<EqPreset>> = dao.getCustomPresets()

    suspend fun saveCustomPreset(preset: EqPreset): Long = withContext(Dispatchers.IO) {
        dao.insertPreset(preset)
    }

    suspend fun deletePreset(id: Long): Unit = withContext(Dispatchers.IO) {
        dao.deletePresetById(id)
    }

    // Persisted preferences
    fun getSavedActivePresetId(): Long = prefs.getLong("active_preset_id", -1L)
    fun setSavedActivePresetId(id: Long) = prefs.edit().putLong("active_preset_id", id).apply()

    fun getSavedAccentTheme(): AccentTheme {
        val name = prefs.getString("accent_theme", AccentTheme.NEON_CYAN.name)
        return try {
            AccentTheme.valueOf(name ?: AccentTheme.NEON_CYAN.name)
        } catch (e: Exception) {
            AccentTheme.NEON_CYAN
        }
    }
    fun setSavedAccentTheme(theme: AccentTheme) {
        prefs.edit().putString("accent_theme", theme.name).apply()
    }

    fun isMasterEqEnabled(): Boolean = prefs.getBoolean("master_eq_enabled", true)
    fun setMasterEqEnabled(enabled: Boolean) = prefs.edit().putBoolean("master_eq_enabled", enabled).apply()

    fun isSubwooferEnabled(): Boolean = prefs.getBoolean("subwoofer_enabled", true)
    fun setSubwooferEnabled(enabled: Boolean) = prefs.edit().putBoolean("subwoofer_enabled", enabled).apply()

    fun getSubwooferSlope(): Int = prefs.getInt("subwoofer_slope", 24) // 12, 24, 48 dB/oct
    fun setSubwooferSlope(slope: Int) = prefs.edit().putInt("subwoofer_slope", slope).apply()

    fun isSubwooferPhaseInverted(): Boolean = prefs.getBoolean("subwoofer_phase_inverted", false)
    fun setSubwooferPhaseInverted(inverted: Boolean) = prefs.edit().putBoolean("subwoofer_phase_inverted", inverted).apply()

    fun isSubHarmonicsEnabled(): Boolean = prefs.getBoolean("sub_harmonics_enabled", true)
    fun setSubHarmonicsEnabled(enabled: Boolean) = prefs.edit().putBoolean("sub_harmonics_enabled", enabled).apply()
}
