package com.example.pacelock

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionHelper {
    val REQUIRED_LOCATION_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    const val REQUEST_LOCATION_CODE = 1001
    const val REQUEST_NOTIFICATION_CODE = 1002

    fun hasLocationPermission(context : Context): Boolean {
        return REQUIRED_LOCATION_PERMISSIONS.all{ permission ->  // all to iterate on both permissions if we had more
            // particular permissions like camera and location then we
            // would have checked them separately

            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED  // if permission is
            // granted true is returned else false is returned
        }
    }


    fun requestLocationPermission(activity: Activity){
        ActivityCompat.requestPermissions(
            activity,
            REQUIRED_LOCATION_PERMISSIONS,
            REQUEST_LOCATION_CODE
        )
    }


    fun hasNotificationPermission(context: Context) : Boolean {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }else{
            return true
        }
    }


    fun requestNotificationPermission(activity: Activity){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_NOTIFICATION_CODE
            )
        }
    }

}