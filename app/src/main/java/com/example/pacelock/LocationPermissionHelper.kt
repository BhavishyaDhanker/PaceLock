package com.example.pacelock

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object LocationPermissionHelper {
    val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    const val REQUEST_CODE = 1001

    fun hasPermissionGranted(context : Context): Boolean {
        return REQUIRED_PERMISSIONS.all{ permission ->  // all to iterate on both permissions if we had more
            // particular permissions like camera and location then we
            // would have checked them separately

            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED  // if permission is
            // granted true is returned else false is returned
        }
    }


    fun requestPermission(activity: Activity){
        ActivityCompat.requestPermissions(
            activity,
            REQUIRED_PERMISSIONS,
            REQUEST_CODE
        )
    }
}