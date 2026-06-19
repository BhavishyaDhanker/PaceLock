package com.example.pacelock.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.pacelock.LocationTracker
import com.example.pacelock.PermissionHelper
import com.example.pacelock.Data.Split
import com.example.pacelock.R
import com.example.pacelock.Run.RunActivity
import com.example.pacelock.RunStatsCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

class RunTrackingService : Service() {

    private val binder = RunServiceBinder()

    inner class RunServiceBinder : Binder() {
        fun getService(): RunTrackingService = this@RunTrackingService
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)


                // serviceScope is a coroutineScope like viewModelScope or lifeCycleScope,but it
                // is declared by us rather than being given by the android itself
                // SupervisorJob controls the lifecycles of all coroutines inside this scope
                // Dispatchers.Main means that the coroutine runs on main thread

    private lateinit var locationTracker: LocationTracker
    private lateinit var helper: PermissionHelper
    private lateinit var notificationManager: NotificationManager

    val pathPoints = mutableListOf<GeoPoint>()
    val splits = mutableListOf<Split>()
    var totalDistanceMeters = 0f
    var elapsedSeconds = 0L
    var isTracking = false
    var isPaused = false
    var lastSplitDistance = 0f
    var lastSplitTime = 0L
    var lastSplitNumber = 0


    var onLocationUpdate: ((GeoPoint, Float) -> Unit)? = null
    var onTimerTick: ((Long) -> Unit)? = null
    var onSplitTrack: ((List<Split>) -> Unit)? = null
    val onServiceError: ((String) -> Unit)? = null

    private var timerJob: Job? = null


    companion object {
        const val NOTIFICATION_CHANEL_ID = "Run Tracking Service"
        const val NOTIFICATION_ID = 1

        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_FINISH = "ACTION_FINISH"
    }

    override fun onCreate() {
        super.onCreate()
        locationTracker = LocationTracker(this)
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        helper = PermissionHelper

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        Log.d(
            "PACETEST",
            "onStartCommand action=${intent?.action}"
        )
        when(intent?.action){
            ACTION_START -> {
                Log.d("PACETEST", "ACTION_START received")
                startTracking()
            }
            ACTION_PAUSE -> pauseTracking()
            ACTION_RESUME -> resumeTracking()
            ACTION_FINISH -> finishTracking()
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        locationTracker.stopTracking()
    }

    private fun startTracking() {

        Log.d("RUN_SERVICE", "startTracking called")
        if(!helper.hasLocationPermission(this)){
            // check for permission first
            onServiceError?.invoke("Location Permission is required to Track Run")
            stopSelf()
            return
        }

        isTracking = true
        isPaused = false

        if(helper.hasLocationPermission(this)){
            startForeground(NOTIFICATION_ID, buildNotification("0 m", "00:00:00"))
            onServiceError?.invoke("No Notification permission - Tracking may stop is screen is turned off")

            Log.d("RUN_SERVICE", "startForeground called")
        }

        locationTracker.startTracking { location->
            val point =  GeoPoint(location.latitude, location.longitude)

            if(isTracking && !isPaused){

                pathPoints.add(point)
                val segmentDistance = RunStatsCalculator.calculateLastSegmentDistance(pathPoints)
                totalDistanceMeters += segmentDistance

                while(totalDistanceMeters > lastSplitDistance + 1000f){
                    lastSplitNumber++
                    val split = Split(
                        lastSplitNumber,
                        elapsedSeconds - lastSplitTime,
                        elapsedSeconds
                    )

                    splits.add(split)
                    onSplitTrack?.invoke(splits.toList())
                    lastSplitTime = elapsedSeconds
                    lastSplitDistance += 1000f
                }

                onLocationUpdate?.invoke(point, segmentDistance)

                updateNotification(
                    RunStatsCalculator.formatTime(elapsedSeconds),
                    RunStatsCalculator.formatDistance(totalDistanceMeters)
                )

            }


        }
        startTimer()

    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while(isTracking){
                delay(1000L)

                if(!isPaused){

                    elapsedSeconds++

                    onTimerTick?.invoke(elapsedSeconds)

                    updateNotification(
                        RunStatsCalculator.formatTime(elapsedSeconds),
                        RunStatsCalculator.formatDistance(totalDistanceMeters)
                    )

                }
            }

        }
    }

    private fun buildNotification(
        totalDistanceMeters: String,
        elapsedTime: String,
        isPaused: Boolean = false
    ): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, RunActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        Log.d("RUN_SERVICE", "buildNotification called")

        return NotificationCompat.Builder(this, NOTIFICATION_CHANEL_ID)
            .setContentTitle(if(!isPaused){"Run Tracking in Process" } else { "Run Tracking Paused"})
            .setContentText("$totalDistanceMeters  •  $elapsedTime" )
            .setSmallIcon(R.drawable.logo)
            .setContentIntent(pendingIntent)
            .setSilent(true)    // no sound when notification updates (which is going to happen quite a lot)
            .setOngoing(true)   // can't be swiped away
            .build()

    }

    private fun pauseTracking() {
        isPaused = true
        timerJob?.cancel()

        updateNotification(
            RunStatsCalculator.formatTime(elapsedSeconds),
            RunStatsCalculator.formatDistance(totalDistanceMeters),
            isPaused
        )

    }

    private fun resumeTracking() {
        isPaused = false
        timerJob?.cancel()
        startTimer()
        updateNotification(
            RunStatsCalculator.formatTime(elapsedSeconds),
            RunStatsCalculator.formatDistance(totalDistanceMeters)
        )

    }

    private fun finishTracking() {
        isTracking = false
        isPaused = true
        startTimer()
        locationTracker.stopTracking()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

    }

    private fun createNotificationChannel() {

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val channel = NotificationChannel(
                NOTIFICATION_CHANEL_ID,
                "Run Tranking",
                NotificationManager.IMPORTANCE_LOW
            ).apply{
                description="Shows live run stats while running"
            }

            notificationManager.createNotificationChannel(channel)
        }

    }

    private fun updateNotification(
        distance: String,
        time: String,
        isPaused: Boolean = false
    ) {
        notificationManager.notify(
            NOTIFICATION_ID,
            buildNotification(distance, time, isPaused)
        )
    }
}