package com.example.data.local

import androidx.room.*
import com.example.data.model.NotificationLog
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationLogDao {
    @Query("SELECT * FROM notification_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<NotificationLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: NotificationLog): Long

    @Delete
    suspend fun deleteLog(log: NotificationLog)

    @Query("DELETE FROM notification_logs WHERE id = :id")
    suspend fun deleteLogById(id: Int)

    @Query("DELETE FROM notification_logs")
    suspend fun clearAllLogs()
}
