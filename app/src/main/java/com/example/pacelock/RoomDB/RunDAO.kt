package com.example.pacelock.RoomDB

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RunDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRun(run: RunEntity): Long

    @Delete
    suspend fun deleteRun(run: RunEntity)

    @Query("SELECT * FROM runs WHERE id = :id")
    suspend fun getRunById(id: Long): RunEntity?

    @Query("SELECT * FROM runs ORDER BY timestamp DESC")
    fun getAllRuns(): Flow<List<RunEntity>>

    @Query("SELECT * FROM runs ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestRun(): RunEntity?

    @Query("SELECT * FROM runs ORDER BY distance DESC LIMIT 1")
    fun getLongestRun(): Flow<RunEntity?>

    @Query("SELECT MAX(distance) FROM runs")
    fun getLongestDistance(): Flow<Float?>

    @Query("SELECT SUM(distance) FROM runs")
    suspend fun getTotalDistance(): Float

    @Query("SELECT COUNT(*) FROM runs")
    fun getTotalRuns(): Flow<Int>

    @Query("SELECT SUM(elapsed) FROM runs")
    fun getTotalDuration(): Flow<Long?>

    @Query("SELECT AVG(distance) FROM runs")
    fun getAvgDistance(): Flow<Double?>

    @Query("SELECT AVG(elapsed) FROM runs")
    fun getAvgDuration(): Flow<Double?>

    @Query("SELECT * FROM runs ORDER BY avgPaceSecPerKm DESC LIMIT 1")
    fun getSlowestRun(): Flow<RunEntity?>

    @Query("SELECT MAX(avgPaceSecPerKm) FROM runs")
    fun getSlowestPace(): Flow<Float?>

    @Query("SELECT * FROM runs ORDER BY avgPaceSecPerKm ASC LIMIT 1")
    fun getFastestRun(): Flow<RunEntity?>

    @Query("SELECT MIN(avgPaceSecPerKm) FROM runs")
    fun getFastestPace(): Flow<Float?>

    @Query("""
        SELECT * FROM runs
        WHERE timestamp BETWEEN :startTime AND :endTime
        ORDER BY timestamp ASC
    """)
    fun getRunsBetween(
        startTime: Long,
        endTime: Long
    ): Flow<List<RunEntity>>
}