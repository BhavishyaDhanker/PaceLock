package com.example.pacelock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.pacelock.Coaching.CoachingConfig
import com.example.pacelock.LocationTracker
import com.example.pacelock.PermissionHelper
import com.example.pacelock.Data.Split
import com.example.pacelock.R
import com.example.pacelock.Run.RunActivity
import com.example.pacelock.RunStatsCalculator
import com.example.pacelock.Coaching.CoachingEngine
import com.example.pacelock.Coaching.HapticsManager
import com.example.pacelock.Coaching.MetronomeManager
import com.example.pacelock.Coaching.TTSManager
import com.example.pacelock.Data.CoachingSettings
import com.example.pacelock.Data.PaceWindowEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import java.util.ArrayDeque
import kotlin.time.Duration.Companion.milliseconds

class RunTrackingService : Service() {

    //──────────────────────────────────────────────────────────────
    // Binder
    //──────────────────────────────────────────────────────────────

    private val binder = RunServiceBinder()

    inner class RunServiceBinder : Binder() {
        fun getService(): RunTrackingService = this@RunTrackingService
    }

    //──────────────────────────────────────────────────────────────
    // Coroutine Scope
    //──────────────────────────────────────────────────────────────

    private val serviceScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main)

    //──────────────────────────────────────────────────────────────
    // Core Components
    //──────────────────────────────────────────────────────────────

    private lateinit var locationTracker: LocationTracker
    private lateinit var notificationManager: NotificationManager
    private lateinit var helper: PermissionHelper

    // New managers
    private lateinit var ttsManager: TTSManager
    private lateinit var hapticsManager: HapticsManager
    private lateinit var metronomeManager: MetronomeManager

    // Coaching
    private var coachingEngine: CoachingEngine? = null
    private var currentSettings: CoachingSettings? = null

    //──────────────────────────────────────────────────────────────
    // Pace Window
    //──────────────────────────────────────────────────────────────

    private val paceWindow = ArrayDeque<PaceWindowEntry>()

    companion object {
        private const val PACE_WINDOW_DURATION_MS = 5000L

        const val NOTIFICATION_CHANEL_ID = "Run Tracking Service"
        const val NOTIFICATION_ID = 1

        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_FINISH = "ACTION_FINISH"
    }

    //──────────────────────────────────────────────────────────────
    // Run State
    //──────────────────────────────────────────────────────────────

    val pathPoints = mutableListOf<GeoPoint>()

    val splits = mutableListOf<Split>()

    var totalDistanceMeters = 0f

    var elapsedSeconds = 0L

    var isTracking = false

    var isPaused = false

    var lastSplitDistance = 0f

    var lastSplitTime = 0L

    var lastSplitNumber = 0

    //──────────────────────────────────────────────────────────────
    // Callbacks
    //──────────────────────────────────────────────────────────────

    /**
     * GeoPoint
     * Segment Distance
     * Current Pace (sec/km)
     */
    var onLocationUpdate:
            ((GeoPoint, Float, Float) -> Unit)? = null

    var onTimerTick:
            ((Long) -> Unit)? = null

    var onSplitTrack:
            ((List<Split>) -> Unit)? = null

    val onServiceError:
            ((String) -> Unit)? = null

    //──────────────────────────────────────────────────────────────
    // Timer
    //──────────────────────────────────────────────────────────────

    private var timerJob: Job? = null

    //──────────────────────────────────────────────────────────────
// Lifecycle
//──────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()

        locationTracker = LocationTracker(this)

        notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager

        helper = PermissionHelper

        ttsManager = TTSManager(this)

        hapticsManager = HapticsManager(this)

        metronomeManager =
            MetronomeManager(this, hapticsManager)

        createNotificationChannel()
    }

    /**
     * Called by RunActivity after binding to the service.
     *
     * This supplies the latest coaching settings to the service.
     */
    fun applySettings(settings: CoachingSettings) {

        currentSettings = settings

        if (settings.targetPacePerSecPerKm > 0f) {

            coachingEngine = CoachingEngine(
                CoachingConfig(
                    targetPacePerSecPerKm =
                        settings.targetPacePerSecPerKm
                )
            )

        } else {

            coachingEngine = null

        }

        if (settings.metronomeEnabled) {

            metronomeManager.start(
                settings.metronomeBpm,
                settings.metronomeUseSound
            )

        } else {

            metronomeManager.stop()

        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        Log.d(
            "PACETEST",
            "onStartCommand action=${intent?.action}"
        )

        when (intent?.action) {

            ACTION_START -> {

                Log.d(
                    "PACETEST",
                    "ACTION_START received"
                )

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

        timerJob?.cancel()

        locationTracker.stopTracking()

        metronomeManager.shutdown()

        ttsManager.shutdown()

        coachingEngine?.reset()

        serviceScope.cancel()
    }

    private fun startTracking() {

        Log.d("RUN_SERVICE", "startTracking called")

        if (isTracking) {
            Log.d("RUN_SERVICE", "Ignoring duplicate ACTION_START")
            return
        }




        if (!helper.hasLocationPermission(this)) {
            onServiceError?.invoke("Location permission is required to track run.")
            stopSelf()
            return
        }

        isTracking = true
        isPaused = false

        pathPoints.clear()
        splits.clear()
        paceWindow.clear()

        totalDistanceMeters = 0f
        elapsedSeconds = 0L

        lastSplitDistance = 0f
        lastSplitTime = 0L
        lastSplitNumber = 0

        startForeground(
            NOTIFICATION_ID,
            buildNotification("0 m", "00:00:00")
        )

        locationTracker.startTracking { location ->

            if (!isTracking || isPaused) return@startTracking

            val point = GeoPoint(
                location.latitude,
                location.longitude
            )

            pathPoints.add(point)

            //----------------------------------------------------------
            // Sliding Pace Window
            //----------------------------------------------------------

            val now = location.time

            paceWindow.addLast(
                PaceWindowEntry(
                    point,
                    now
                )
            )

            while (
                paceWindow.isNotEmpty() &&
                now - paceWindow.first().timestamp >
                PACE_WINDOW_DURATION_MS
            ) {
                paceWindow.removeFirst()
            }

            //----------------------------------------------------------
            // Current Pace
            //----------------------------------------------------------

            val currentPaceSecPerKm =
                RunStatsCalculator.calculateCurrentPace(
                    paceWindow.toList()
                )

            //----------------------------------------------------------
            // Distance
            //----------------------------------------------------------

            val segmentDistance =
                RunStatsCalculator.calculateLastSegmentDistance(
                    pathPoints
                )

            totalDistanceMeters += segmentDistance

            //----------------------------------------------------------
            // Splits
            //----------------------------------------------------------

            while (
                totalDistanceMeters >=
                lastSplitDistance + 1000f
            ) {

                lastSplitNumber++

                val split = Split(
                     lastSplitNumber,
                    elapsedSeconds - lastSplitTime,
                    elapsedSeconds
                )

                splits.add(split)

                onSplitTrack?.invoke(
                    splits.toList()
                )

                lastSplitTime = elapsedSeconds
                lastSplitDistance += 1000f
            }

            //----------------------------------------------------------
            // Notify Activity / ViewModel
            //----------------------------------------------------------

            onLocationUpdate?.invoke(
                point,
                segmentDistance,
                currentPaceSecPerKm
            )

            //----------------------------------------------------------
            // Coaching
            //----------------------------------------------------------

            Log.d(
                "COACHING",
                "evaluate() called | pace=$currentPaceSecPerKm target=${currentSettings?.targetPacePerSecPerKm} distance=$totalDistanceMeters time=$elapsedSeconds"
            )
            coachingEngine
                ?.evaluate(
                    distanceMeters = totalDistanceMeters,
                    currentPacePerSecPerKm = currentPaceSecPerKm,
                    elapsedSeconds = elapsedSeconds
                )
                ?.let { cue ->

                    val settings = currentSettings

                    if (settings?.ttsEnabled == true) {
                        ttsManager.speak(cue.message)
                    }

                    if (settings?.hapticsEnabled == true) {
                        hapticsManager.vibrate(
                            cue.hapticPattern
                        )
                    }
                }

            //----------------------------------------------------------
            // Notification
            //----------------------------------------------------------

            updateNotification(
                RunStatsCalculator.formatDistance(
                    totalDistanceMeters
                ),
                RunStatsCalculator.formatTime(
                    elapsedSeconds
                )
            )
        }

        startTimer()
    }

    private fun startTimer() {

        timerJob?.cancel()

        timerJob = serviceScope.launch {

            while (isTracking) {

                delay(1000L.milliseconds)

                if (isPaused) continue

                elapsedSeconds++
                Log.d("RunService", "Time=$elapsedSeconds")

                onTimerTick?.invoke(elapsedSeconds)

                updateNotification(
                    RunStatsCalculator.formatDistance(totalDistanceMeters),
                    RunStatsCalculator.formatTime(elapsedSeconds)
                )
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
            Intent(this, RunActivity::class.java).apply {
                putExtra("NEW_RUN", false)
            }

            ,
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

        if (!isTracking || isPaused)
            return

        isPaused = true

        timerJob?.cancel()

        metronomeManager.stop()

        updateNotification(
            RunStatsCalculator.formatDistance(totalDistanceMeters),
            RunStatsCalculator.formatTime(elapsedSeconds),
            true
        )
    }

    private fun resumeTracking() {

        if (!isTracking || !isPaused)
            return

        isPaused = false

        currentSettings?.let {

            if (it.metronomeEnabled) {

                metronomeManager.start(
                    it.metronomeBpm,
                    it.metronomeUseSound
                )
            }
        }

        startTimer()

        updateNotification(
            RunStatsCalculator.formatDistance(totalDistanceMeters),
            RunStatsCalculator.formatTime(elapsedSeconds)
        )
    }

    private fun finishTracking() {

        isTracking = false
        isPaused = false

        timerJob?.cancel()

        locationTracker.stopTracking()

        metronomeManager.shutdown()

        ttsManager.shutdown()

        coachingEngine?.reset()

        paceWindow.clear()

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