package com.example.pacelock

import android.content.Context
import android.icu.util.Calendar
import com.example.pacelock.Data.Split
import com.example.pacelock.RoomDB.GeoPointTypeConverter
import com.example.pacelock.RoomDB.RunDatabase
import com.example.pacelock.RoomDB.RunEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint

class RunRepository(context : Context) {

    val dao = RunDatabase.getInstance(context).dao()


    suspend fun saveRun(
        distanceMeters: Float,
        elapsedSeconds: Long,
        pathPoints: List<GeoPoint>,
        splits: List<Split>
    ): Long = withContext(Dispatchers.IO){
        val paceSecPerKm =
        if(distanceMeters > 0f){
            elapsedSeconds /(distanceMeters/1000f)
        }else 0f

        val converter = GeoPointTypeConverter()

        val run = RunEntity(
            distance = distanceMeters,
            elapsed = elapsedSeconds,
            avgPaceSecPerKm = paceSecPerKm,
            timestamp = System.currentTimeMillis(),
            pathPointsJson = converter.fromGeoPointString(pathPoints),
            splitsJson = converter.fromSplits(splits)
        )

        dao.insertRun(run)
    }


    fun getAllRuns(): Flow<List<RunEntity>> {
        return dao.getAllRuns()
    }

    suspend fun getLatestRun(): RunEntity?{
        return dao.getLatestRun()
    }

    suspend fun getRunById(id: Long): RunEntity?{
        return dao.getRunById(id)
    }

    suspend fun getTotalDistance(): Float{
        return dao.getTotalDistance()
    }

    fun getTotalDuration(): Flow<Long?>{
        return dao.getTotalDuration()
    }

    fun getLongestRun(): Flow<RunEntity?>{
        return dao.getLongestRun()
    }

    suspend fun deleteRun(run: RunEntity) = withContext(Dispatchers.IO){
        dao.deleteRun(run)
    }

    fun getTotalRuns() {
        dao.getTotalRuns()
    }

    private fun getWeekStartMillis(): Long {
        val calendar = Calendar.getInstance()

        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        return calendar.timeInMillis
    }

    suspend fun getWeeklyDistance() : Float{
        return dao.getWeeklyDistance(getWeekStartMillis())
    }


}