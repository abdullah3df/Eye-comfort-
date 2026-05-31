package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "eye_break_logs")
data class EyeBreakLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val exerciseTitle: String,
    val steps: String,
    val benefit: String,
    val durationSeconds: Int,
    val languageCode: String,
    val timestamp: Long = System.currentTimeMillis(),
    val preStrainLevel: String,   // "None", "Light", "Medium", "Severe"
    val postStrainLevel: String   // "None", "Light", "Medium", "Severe"
)

@Dao
interface EyeBreakLogDao {
    @Query("SELECT * FROM eye_break_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<EyeBreakLog>>

    @Insert
    suspend fun insertLog(log: EyeBreakLog)

    @Query("DELETE FROM eye_break_logs")
    suspend fun clearAllLogs()

    @Query("SELECT COUNT(*) FROM eye_break_logs")
    suspend fun getLogCount(): Int
}

@Database(entities = [EyeBreakLog::class], version = 1, exportSchema = false)
abstract class EyeDatabase : RoomDatabase() {
    abstract fun eyeBreakLogDao(): EyeBreakLogDao
}
