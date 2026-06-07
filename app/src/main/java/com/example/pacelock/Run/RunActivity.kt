package com.example.pacelock.Run

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.pacelock.HomeActivity
import com.example.pacelock.LocationPermissionHelper
import com.example.pacelock.LocationTracker
import com.example.pacelock.R
import com.example.pacelock.databinding.ActivityRunBinding
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class RunActivity : AppCompatActivity() {

    lateinit var binding : ActivityRunBinding
    val viewModel : RunViewModel by viewModels()
    val helper = LocationPermissionHelper


    private lateinit var runPolyline: Polyline
    private lateinit var myLocationOverlay: MyLocationNewOverlay




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRunBinding.inflate(layoutInflater)
        setContentView(binding.root)



        setupMap()
        setupButtons()
        observeStates()
        checkPermissionThenStart()

    }

    private fun observeStates() {
        lifecycleScope.launch {
            viewModel.isPause.collect {
                binding.pauseRun.text = if(it){
                    "RESUME RUN"
                }else{
                    "PAUSE RUN"
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isTracking.collect { tracking->
                if (!tracking && viewModel.pathPoints.value.isNotEmpty()) {
                    // navigateToSummary()

                }
            }
        }

        lifecycleScope.launch {
            viewModel.pathPoints.collect {

                runPolyline.setPoints(it)
                binding.mapView.invalidate()
            }
        }

        lifecycleScope.launch {
            viewModel.currentLocation.collect {
                binding.mapView.controller.animateTo(it)
            }
        }

        lifecycleScope.launch {
            viewModel.formattedPace.collect {
                binding.tvPace.text = it
            }
        }

        lifecycleScope.launch {
            viewModel.formattedTime.collect {
                binding.tvTime.text = it
            }
        }

        lifecycleScope.launch {
            viewModel.formattedDistance.collect {
                binding.tvDistance.text = it
            }
        }
    }

    private fun setupMap() {

        binding.mapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(18.0)
        }

        runPolyline = Polyline().apply {
            outlinePaint.color = Color.parseColor("#2979FF")
            outlinePaint.strokeWidth = 10f
            outlinePaint.strokeCap = Paint.Cap.ROUND
            outlinePaint.strokeJoin = Paint.Join.ROUND
            outlinePaint.isAntiAlias = true
        }

        myLocationOverlay = MyLocationNewOverlay(
            GpsMyLocationProvider(this), binding.mapView
        ).apply {
            enableMyLocation()
            enableFollowLocation()
        }

        binding.mapView.overlays.add(runPolyline)
        binding.mapView.overlays.add(myLocationOverlay)
    }

    private fun setupButtons() {

        binding.pauseRun.setOnClickListener {
            if(viewModel.isPause.value){
                viewModel.resumeRun()
            }else{
                viewModel.pauseRun()
            }
        }

        binding.finishRun.setOnClickListener {
            viewModel.finishRun()

        }
    }

    private fun checkPermissionThenStart() {

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

    fun onPermissionGranted(){
        viewModel.startLocationUpdates()
        startCountdown()
    }

    private fun startCountdown() {

        // baad m timer bhi lagana h 3 2 1 GO! type
        viewModel.startRun()
    }

    fun onPermissionDenied(){
        Toast.makeText(
            this,
            "Location permission is required to track your run",
            Toast.LENGTH_LONG
        ).show()

        finish()
    }

    
}