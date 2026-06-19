package com.example.pacelock

import android.content.Context
import android.location.Location
import com.example.pacelock.RoomDB.GeoPointTypeConverter
import com.example.pacelock.RoomDB.RunDatabase
import com.example.pacelock.RoomDB.RunEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint

class RunRepository(context : Context) {

    val dao = RunDatabase.getInstance(context).dao()

    val locationTracker = LocationTracker(context)

    private val _locationFlow = MutableSharedFlow<GeoPoint>(replay = 0)
    val locationFlow = _locationFlow.asSharedFlow()

    fun startTracking(onLocation: (GeoPoint) -> Unit) {
        locationTracker.startTracking { location: Location ->
            val point = GeoPoint(location.latitude, location.longitude)
            onLocation(point)
        }
    }


    fun stopTracking() {
        locationTracker.stopTracking()
    }

    suspend fun saveRun(
        distanceMeters: Float,
        elapsedSeconds: Long,
        pathPoints: List<GeoPoint>
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
        )

        dao.insertRun(run)
    }


    fun getAllRuns(): Flow<List<RunEntity>> {
        return dao.getAllRuns()
    }

    fun getLatestRun(): Flow<RunEntity?>{
        return dao.getLatestRun()
    }

    fun getTotalDistance(): Flow<Float?>{
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
}