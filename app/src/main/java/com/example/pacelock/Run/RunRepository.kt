package com.example.pacelock.Run

import android.content.Context
import android.location.Location
import com.example.pacelock.LocationTracker
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.osmdroid.util.GeoPoint

class RunRepository(context : Context) {

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
}