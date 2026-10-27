package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.EqPreset
import kotlinx.coroutines.flow.Flow

@Dao
interface EqPresetDao {
    @Query("SELECT * FROM eq_presets ORDER BY isCustom ASC, id ASC")
    fun getAllPresets(): Flow<List<EqPreset>>

    @Query("SELECT * FROM eq_presets WHERE id = :id")
    suspend fun getPresetById(id: Long): EqPreset?

    @Query("SELECT * FROM eq_presets WHERE isCustom = 1 ORDER BY createdAt DESC")
    fun getCustomPresets(): Flow<List<EqPreset>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: EqPreset): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(presets: List<EqPreset>)

    @Update
    suspend fun updatePreset(preset: EqPreset)

    @Delete
    suspend fun deletePreset(preset: EqPreset)

    @Query("DELETE FROM eq_presets WHERE id = :id AND isCustom = 1")
    suspend fun deletePresetById(id: Long)
}
