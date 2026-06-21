package com.example.pacelock.Run

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.pacelock.HomeActivity
import kotlin.let
import com.example.pacelock.PermissionHelper
import com.example.pacelock.databinding.ActivityRunBinding
import com.example.pacelock.service.RunTrackingService
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class RunActivity : AppCompatActivity() {

    lateinit var binding : ActivityRunBinding
    val viewModel : RunViewModel by viewModels()
    val helper = PermissionHelper
    private var trackingService: RunTrackingService? = null
    private var isBound = false


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


    private val serviceConnection = object : ServiceConnection{
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val runBinder = binder as RunTrackingService.RunServiceBinder
            trackingService = runBinder.getService()
            isBound = true
            viewModel.startService(this@RunActivity)

            trackingService?.let { service->
                viewModel.restoreRunStats(
                    service.totalDistanceMeters,
                    service.elapsedSeconds,
                    service.pathPoints,                              // This is just a V1 solution later on we
                    service.splits                                          // can completely shift to service tracking
                )                                                           // by removing the viewModel updates entirely
            }


            trackingService?.onLocationUpdate = {point , segDist ->
                viewModel.addLocation(point, segDist)

                binding.mapView.controller.animateTo(point)

            }

            trackingService?.onTimerTick = { seconds ->
                viewModel.updateElapsedSeconds(seconds)

            }

            trackingService?.onSplitTrack = {splits ->
                viewModel.updateSplits(splits)

            }

        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }
    }


    private fun bindTrackingService() {
        Intent(this, RunTrackingService::class.java).also { intent ->
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
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

        lifecycleScope.launch {
            viewModel.navigateToStats.collect { result->

                result?.let {
                    viewModel.saveRun {runId->

                        runOnUiThread {

                            Intent(this@RunActivity, HomeActivity::class.java).apply {
                                putExtra("run_Id", runId)
                                putExtra("open_fragment", "stats")
                                startActivity(this)
                            }

                            viewModel.navigatedToStats()
                            finish()
                        }

                    }
                }



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
                viewModel.resumeService(this)
            }else{
                viewModel.pauseRun()
                viewModel.pauseService(this)
            }
        }
        binding.finishRun.setOnClickListener {
            viewModel.finishRun()
            viewModel.finishService(this)
        }
    }

    private fun checkPermissionThenStart() {
        when{

            !helper.hasLocationPermission(this) -> helper.requestLocationPermission(this)

            !helper.hasNotificationPermission(this) -> helper.requestNotificationPermission(this)

            else -> startServiceAndCountdown()
        }


    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {

            helper.REQUEST_LOCATION_CODE -> {
                if(grantResults.isNotEmpty() && grantResults.all {
                    it == PackageManager.PERMISSION_GRANTED
                    }){
                    checkPermissionThenStart()
                }else{
                    Toast.makeText(
                        this@RunActivity,
                        "Location Permission is required to track Run",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            helper.REQUEST_NOTIFICATION_CODE -> {
                if(grantResults.firstOrNull() != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(
                        this@RunActivity,
                        "This permission helps to track run in background",
                        Toast.LENGTH_LONG
                    ).show()
                }
                startServiceAndCountdown()
            }

        }
    }



    fun startServiceAndCountdown(){
        bindTrackingService()
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

    override fun onDestroy(){
        super.onDestroy()

        if(isBound){
            unbindService(serviceConnection)
            isBound = false
        }

        binding.mapView.onDetach()
    }

    
}