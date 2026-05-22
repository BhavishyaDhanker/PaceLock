package com.example.pacelock

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class LocationTracker(context: Context) {

    private val fusedLocationClient : FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,  // Use GPS for best accuracy
        2000L                              // Ask for update every 2 seconds
    ).apply {
        setMinUpdateIntervalMillis(1000L)  // But no faster than 1 second
        setMinUpdateDistanceMeters(5f)     // Only update if moved 5+ meters
    }.build()


    private var locationCallback : LocationCallback? = null

    private var savedFunction: ((Location) -> Unit)? = null  // This function is defined and saved in this file

    @SuppressLint("MissingPermission")
    fun startTracking(userFunction: ((Location) -> Unit)){              // secondary function - When input of a function is
        this.savedFunction = userFunction                               // another function. here userFunction is a function
                                                                        // that will be passed from the activity. It can be any function
                                                                        // that is a lambda with location as a parameter.
                                                                        // This type of architecture improves reusability as we can use
                                                                        // different lambdas for different purposes

        locationCallback = object : LocationCallback(){
            override fun onLocationResult(result: LocationResult) {

                result.lastLocation?.let{ location->
                    savedFunction?.invoke(location)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback!!,
            Looper.getMainLooper()   // Because Location stimulates UI changes and UI changes happen on main thread
        )
    }

    fun stopTracking(){
        locationCallback?.let{
            fusedLocationClient.removeLocationUpdates(it)
        }
        locationCallback = null
        savedFunction = null
    }
}