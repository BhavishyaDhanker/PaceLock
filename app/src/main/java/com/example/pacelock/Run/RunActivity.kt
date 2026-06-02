package com.example.pacelock.Run

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
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


    private lateinit var locationTracker: LocationTracker
    private lateinit var mapView: MapView
    private lateinit var myLocationOverlay: MyLocationNewOverlay
    private lateinit var runPolyline: Polyline


    private val pathPoints = mutableListOf<GeoPoint>()
    private var isTracking = true  //abhi true h jb tk m countdown nhi lga rha
    private var isPause = false



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRunBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mapView = findViewById(R.id.mapView)
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
        finish()
    }

    private fun onPermissionGranted() {
        setupMap()
        setupLocationUpdates()
        setupListeners()
        observeStates()
    }

    private fun observeStates() {
        lifecycleScope.launch {
            viewModel.isPause.collect { flag->
                if(flag){
                    binding.pauseRun.text = "PAUSE"
                }else{
                    binding.pauseRun.text = "RESUME"
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isTracking.collect {
                if(!it){
                    val intent = Intent(this@RunActivity, HomeActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }
    }


    private fun setupListeners() {

        binding.pauseRun.setOnClickListener {
            viewModel.onPauseBtnClicked()
        }

        binding.finishRun.setOnClickListener {
            viewModel.onFinishBtnClicked()
        }
    }



    private fun setupLocationUpdates() {
        locationTracker.startTracking { location->   // if last parameter of a function is a lambda function then
                                                     // it can be written outside of parenthesis
                                                     //  this is the "userFunction" in LocationTracker file

            val lat = location.latitude
            val lng = location.longitude
            val accuracy = location.accuracy
            val speed = location.speed

            val newPoint = GeoPoint(lat, lng)
            mapView.controller.animateTo(newPoint)

            if (isTracking && !isPause){
                pathPoints.add(newPoint)
                runPolyline.setPoints(pathPoints)
                mapView.invalidate()        // recreates map (so that in can be correctly updated)
            }


            Log.d("Run Tracker", "lat = $lat , lng = $lng, accuracy = $accuracy")
        }
    }

    private fun setupMap() {
        mapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)  // OpenStreetMap(OSM) tiles
            setMultiTouchControls(true)              // Pinch to zoom
            controller.setZoom(18.00)                // 18 = Street level zoom (use 5 for country level)
        }

        myLocationOverlay = MyLocationNewOverlay(
            GpsMyLocationProvider(this), mapView
        ).apply {
            enableMyLocation()
            enableFollowLocation()
        }

        runPolyline = Polyline().apply {
            outlinePaint.color = Color.parseColor("#00F0FF")
            outlinePaint.strokeWidth = 10f
            outlinePaint.strokeCap = Paint.Cap.ROUND                // setting up the polyline
            outlinePaint.strokeJoin = Paint.Join.ROUND
            outlinePaint.isAntiAlias = true
        }



        mapView.overlays.add(myLocationOverlay)
        mapView.overlays.add(runPolyline)
    }

    override fun onRestart() {
        super.onRestart()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDetach()

        locationTracker.stopTracking()
    }
}