package com.example.pacelock.Run

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.pacelock.LocationPermissionHelper
import com.example.pacelock.LocationTracker
import com.example.pacelock.R
import com.example.pacelock.databinding.ActivityRunBinding

class RunActivity : AppCompatActivity() {

    lateinit var binding : ActivityRunBinding

    val viewModel : RunViewModel by viewModels()

    val helper = LocationPermissionHelper

    private lateinit var locationTracker: LocationTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRunBinding.inflate(layoutInflater)

        setContentView(binding.root)

        locationTracker = LocationTracker(this)

        if(helper.hasPermissionGranted(this)){
            onPermissionGranted()
        }else{
            helper.requestPermission(this)


        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == helper.REQUEST_CODE){
            val granted = grantResults.isNotEmpty() && grantResults.all {
                it == PackageManager.PERMISSION_GRANTED
            }

            if(granted){
                onPermissionGranted()
            }else{
                onPermissionDenied()
            }
        }
    }

    private fun onPermissionDenied() {
        Toast.makeText(
            this,
            "Location permission is required",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun onPermissionGranted() {

        locationTracker.startTracking { location->   // if last parameter of a function is a lambda function then
                                                     // it can be written outside of parenthesis
                                                     //  this is the "userFunction" in LocationTracker file
            val lat = location.latitude
            val lng = location.longitude
            val accuracy = location.accuracy
            val speed = location.speed

            Log.d("Run Tracker", "lat = $lat , lng = $lng, accuracy = $accuracy")
        }

    }

    override fun onDestroy() {
        super.onDestroy()

        locationTracker.stopTracking()
    }
}