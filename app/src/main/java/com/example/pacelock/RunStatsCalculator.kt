package com.example.pacelock

import android.location.Location
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import org.osmdroid.util.GeoPoint
import java.util.Locale

object RunStatsCalculator {

    fun calculateLastSegmentDistance(points : List<GeoPoint>) : Float{
        if(points.size < 2){
            return 0f
        }

        val results = FloatArray(1)
        val last = points[points.size -1]
        val secondLast = points[points.size - 2]

        Location.distanceBetween(
            secondLast.latitude,
            secondLast.longitude,
            last.latitude,
            last.longitude,
            results
            )

        return results[0]
    }

    fun formatDistance( meters: Float) : String{
        return if (meters < 1000f){
            "${meters.toInt()} m"
        }else{
            String.format(Locale.getDefault(),"%.2f km", meters/ 1000f)
        }
    }

    fun calculatePace( distanceMeters: Float, secondsElapsed: Long) : String{
        if(distanceMeters < 10f){
            return "--:-- min/km"
        }

        val distanceKm = distanceMeters /1000f
        val minutesElapsed = secondsElapsed/60f
        val paceMinPerKm = minutesElapsed/distanceKm

        val paceMinutes = paceMinPerKm.toInt()
        val paceSeconds = ((paceMinPerKm - paceMinutes)*60).toInt()

        return String.format(
            Locale.getDefault(),
            "%d:%02d min/km",
            paceMinutes,
            paceSeconds
        )
    }

    fun elapsedTime( secondsElapsed: Long): String{
        val hrsElapsed = secondsElapsed/3600L.toInt()
        val minElapsed = (secondsElapsed - hrsElapsed*3600L)/60L.toInt()
        val secElapsed = (secondsElapsed - minElapsed*60L - hrsElapsed*3600L).toInt()

        return String.format(
            Locale.getDefault(),
            "%02d:%02d:%02d",
            hrsElapsed,
            minElapsed,
            secElapsed
        )
    }
}