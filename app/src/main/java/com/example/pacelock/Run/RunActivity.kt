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
import com.example.pacelock.PermissionHelper
import com.example.pacelock.databinding.ActivityRunBinding
import com.example.pacelock.Configration.ConfigurationRepository
import com.example.pacelock.RunTrackingService
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class RunActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRunBinding

    private val viewModel: RunViewModel by viewModels()

    private val helper = PermissionHelper

    private lateinit var settingsRepository: ConfigurationRepository

    private var trackingService: RunTrackingService? = null

    private var isBound = false

    private lateinit var runPolyline: Polyline

    private lateinit var myLocationOverlay: MyLocationNewOverlay

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRunBinding.inflate(layoutInflater)
        setContentView(binding.root)

        settingsRepository = ConfigurationRepository(this)

        setupMap()
        setupButtons()
        observeStates()
        checkPermissionThenStart()
    }

    //──────────────────────────────────────────────
    // Service Connection
    //──────────────────────────────────────────────

    private val serviceConnection =
        object : ServiceConnection {

            override fun onServiceConnected(
                name: ComponentName?,
                binder: IBinder?
            ) {

                val runBinder =
                    binder as RunTrackingService.RunServiceBinder

                trackingService = runBinder.getService()

                isBound = true

                trackingService?.let { service ->

                    viewModel.restoreRunStats(
                        service.totalDistanceMeters,
                        service.elapsedSeconds,
                        service.pathPoints,
                        service.splits
                    )

                    //--------------------------------------------------
                    // Apply Coaching Settings
                    //--------------------------------------------------

                    lifecycleScope.launch {

                        settingsRepository.settingsFlow.collect {

                            service.applySettings(it)
                        }
                    }

                    //--------------------------------------------------
                    // Location Callback
                    //--------------------------------------------------

                    service.onLocationUpdate =
                        { point, segmentDistance, currentPace ->

                            viewModel.addLocation(
                                point,
                                segmentDistance,
                                currentPace
                            )

                            binding.mapView.controller
                                .animateTo(point)
                        }

                    //--------------------------------------------------
                    // Timer Callback
                    //--------------------------------------------------

                    service.onTimerTick = { seconds ->

                        viewModel.updateElapsedSeconds(seconds)
                    }

                    //--------------------------------------------------
                    // Split Callback
                    //--------------------------------------------------

                    service.onSplitTrack = {

                        viewModel.updateSplits(it)
                    }
                }
            }

            override fun onServiceDisconnected(
                name: ComponentName?
            ) {

                trackingService = null

                isBound = false
            }
        }

    private fun bindTrackingService() {

        Intent(
            this,
            RunTrackingService::class.java
        ).also {

            bindService(
                it,
                serviceConnection,
                Context.BIND_AUTO_CREATE
            )
        }
    }

    private fun observeStates() {

        lifecycleScope.launch {

            viewModel.isPause.collect {

                binding.pauseRun.text =
                    if (it) "RESUME RUN"
                    else "PAUSE RUN"
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

                it?.let {

                    binding.mapView.controller.animateTo(it)
                }
            }
        }

        /*
         * Average pace
         */
        lifecycleScope.launch {

            viewModel.formattedPace.collect {

                binding.tvPace.text = it
            }
        }

        /*
         * If you later add another TextView
         * (tvCurrentPace), simply replace the
         * collector above with:
         *
         * viewModel.formattedCurrentPace.collect {
         *      binding.tvCurrentPace.text = it
         * }
         */

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

            viewModel.navigateToStats.collect { result ->

                result?.let {

                    viewModel.saveRun { runId ->

                        runOnUiThread {

                            Intent(
                                this@RunActivity,
                                HomeActivity::class.java
                            ).apply {

                                putExtra(
                                    "run_Id",
                                    runId
                                )

                                putExtra(
                                    "open_fragment",
                                    "stats"
                                )

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

            outlinePaint.color =
                Color.parseColor("#2979FF")

            outlinePaint.strokeWidth = 10f

            outlinePaint.strokeCap =
                Paint.Cap.ROUND

            outlinePaint.strokeJoin =
                Paint.Join.ROUND

            outlinePaint.isAntiAlias = true
        }

        myLocationOverlay =
            MyLocationNewOverlay(
                GpsMyLocationProvider(this),
                binding.mapView
            ).apply {

                enableMyLocation()

                enableFollowLocation()
            }

        binding.mapView.overlays.add(runPolyline)

        binding.mapView.overlays.add(myLocationOverlay)
    }

    private fun setupButtons() {

        binding.pauseRun.setOnClickListener {

            if (viewModel.isPause.value) {

                viewModel.resumeRun()

                viewModel.resumeService(this)

            } else {

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

        when {

            !helper.hasLocationPermission(this) ->

                helper.requestLocationPermission(this)

            !helper.hasNotificationPermission(this) ->

                helper.requestNotificationPermission(this)

            else -> startServiceAndCountdown()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        when (requestCode) {

            helper.REQUEST_LOCATION_CODE -> {

                if (
                    grantResults.isNotEmpty() &&
                    grantResults.all {
                        it == PackageManager.PERMISSION_GRANTED
                    }
                ) {

                    checkPermissionThenStart()

                } else {

                    Toast.makeText(
                        this,
                        "Location Permission is required to track Run",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            helper.REQUEST_NOTIFICATION_CODE -> {

                if (
                    grantResults.firstOrNull()
                    != PackageManager.PERMISSION_GRANTED
                ) {

                    Toast.makeText(
                        this,
                        "This permission helps to track run in background",
                        Toast.LENGTH_LONG
                    ).show()
                }

                startServiceAndCountdown()
            }
        }
    }

    private fun startServiceAndCountdown() {

        /*
         * Start the foreground service FIRST,
         * then bind to it.
         */

        viewModel.startService(this)

        bindTrackingService()

        startCountdown()
    }

    private fun startCountdown() {

        // baad m timer bhi lagana h 3 2 1 GO! type

        viewModel.startRun()
    }

    fun onPermissionDenied() {

        Toast.makeText(
            this,
            "Location permission is required to track your run",
            Toast.LENGTH_LONG
        ).show()

        finish()
    }

    override fun onDestroy() {

        super.onDestroy()

        trackingService?.apply {

            onLocationUpdate = null
            onTimerTick = null
            onSplitTrack = null
        }

        if (isBound) {

            unbindService(serviceConnection)

            isBound = false
        }

        binding.mapView.onDetach()
    }
}