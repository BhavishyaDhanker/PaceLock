package com.example.pacelock

import android.location.Location
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import com.example.pacelock.Data.PaceWindowEntry
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.config.Lookup
import org.osmdroid.util.GeoPoint
import java.util.Locale

object RunStatsCalculator {

    private const val MIN_MOVING_SPEED_MS = 0.5f


    fun calculateCurrentPace(window: List<PaceWindowEntry>): Float{

        val newest = window.last()
        val oldest = window.first()

        val results = FloatArray(1)

        Location.distanceBetween(
            oldest.point.latitude,
            oldest.point.longitude,
            newest.point.latitude,
            newest.point.longitude,
            results
        )

        val distanceMeters = results[0]
        val elapsedSeconds = (newest.timestamp - oldest.timestamp)/1000L

        if(elapsedSeconds <= 0f || distanceMeters <= 0f){
            return 0f
        }

        val currentPace = elapsedSeconds/(distanceMeters/1000f)
        if(currentPace < MIN_MOVING_SPEED_MS){
            return 0f
        }

        return currentPace
    }

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
            "%d:%02d",
            paceMinutes,
            paceSeconds
        )
    }

    fun formatCurrentPace( currentPaceSeconds: Float): String{
        if(currentPaceSeconds <= 0f){
            return "--:--"
        }

        val paceMinutes = (currentPaceSeconds / 60).toInt()
        val paceSeconds = (currentPaceSeconds % 60).toInt()

        return String.format(
            Locale.getDefault(),
            "%d:%02d",
            paceMinutes,
            paceSeconds
        )
    }

    fun formatTime( secondsElapsed: Long): String{
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