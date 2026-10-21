package com.tvfile.app.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackProgressDao {
    @Query("SELECT * FROM playback_progress WHERE fileId = :fileId")
    suspend fun getProgress(fileId: String): PlaybackProgressEntity?

    @Query("SELECT * FROM playback_progress")
    fun getAllProgressFlow(): Flow<List<PlaybackProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: PlaybackProgressEntity)

    @Query("DELETE FROM playback_progress WHERE fileId = :fileId")
    suspend fun deleteProgress(fileId: String)
}
