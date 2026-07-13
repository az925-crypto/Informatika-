package com.example.data.database

import androidx.room.*
import com.example.data.model.AppConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface AppConfigDao {
    @Query("SELECT * FROM app_config WHERE id = 1 LIMIT 1")
    fun getConfigFlow(): Flow<AppConfig?>

    @Query("SELECT * FROM app_config WHERE id = 1 LIMIT 1")
    suspend fun getConfig(): AppConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(config: AppConfig)
}
