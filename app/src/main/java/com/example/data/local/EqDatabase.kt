package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.EqPreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [EqPreset::class], version = 1, exportSchema = false)
abstract class EqDatabase : RoomDatabase() {
    abstract fun eqPresetDao(): EqPresetDao

    companion object {
        @Volatile
        private var INSTANCE: EqDatabase? = null

        fun getInstance(context: Context): EqDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EqDatabase::class.java,
                    "aura_eq.db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            getInstance(context).eqPresetDao().insertAll(EqPreset.DEFAULT_PRESETS)
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
