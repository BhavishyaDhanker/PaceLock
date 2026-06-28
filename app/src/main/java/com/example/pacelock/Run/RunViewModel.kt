package com.example.pacelock.Run

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pacelock.Data.RunResult
import com.example.pacelock.Data.Split
import com.example.pacelock.RunRepository
import com.example.pacelock.RunStatsCalculator
import com.example.pacelock.RunTrackingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

class RunViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repo = RunRepository(application)
    private val calculator = RunStatsCalculator

    //──────────────────────────────────────────────
    // State
    //──────────────────────────────────────────────

    private val _isTracking =
        MutableStateFlow(false)

    private val _isPaused =
        MutableStateFlow(false)

    private val _pathPoints =
        MutableStateFlow<MutableList<GeoPoint>>(mutableListOf())

    private val _currentLocation =
        MutableStateFlow<GeoPoint?>(null)

    private val _totalDistanceMeters =
        MutableStateFlow(0f)

    private val _elapsedSeconds =
        MutableStateFlow(0L)

    private val _currentPaceSecPerKm =
        MutableStateFlow(0f)

    private val _navigateToStats =
        MutableStateFlow<RunResult?>(null)

    private val _splits =
        MutableStateFlow<List<Split>>(emptyList())

    //──────────────────────────────────────────────
    // Public State
    //──────────────────────────────────────────────

    val isTracking = _isTracking.asStateFlow()

    val isPause = _isPaused.asStateFlow()

    val pathPoints = _pathPoints.asStateFlow()

    val currentLocation = _currentLocation.asStateFlow()

    val totalDistanceMeters =
        _totalDistanceMeters.asStateFlow()

    val elapsedSeconds =
        _elapsedSeconds.asStateFlow()

    val currentPaceSecPerKm =
        _currentPaceSecPerKm.asStateFlow()

    val navigateToStats =
        _navigateToStats.asStateFlow()

    val splits =
        _splits.asStateFlow()

    //──────────────────────────────────────────────
    // Formatted State
    //──────────────────────────────────────────────

    val formattedDistance =
        _totalDistanceMeters
            .map {
                calculator.formatDistance(it)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                "0 m"
            )

    val formattedTime =
        _elapsedSeconds
            .map {
                calculator.formatTime(it)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                "00:00:00"
            )

    /*
    if we used code like

    val formattedTime = calculator.formateTime(elapsedSeconds)

    then it will not be a flow and would just calculate the value once, but we
    need a continuous flow of values to be shown. that is done by .map{}

    after we get a flow we need the latest value (as done by the stateFlow
    .stateIn does that for us

    parameters -
        viewModelScope = thread for the stateflow to run on
        SharingStarted.WhileSubscribed(5000) = it tell the stateflow to wait for 5 sec before
                                               destroying itself in absence of observer
        Initial Value = initial value of the state flow

     */

    val formattedCurrentPace =
        currentPaceSecPerKm
            .map {
                calculator.formatCurrentPace(it)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                "--:-- /km"
            )

    /**
     * Average pace of the run.
     *
     * This is different from formattedCurrentPace,
     * which represents the live pace over the last
     * few seconds.
     */
    val formattedPace: StateFlow<String> =
        combine(
            _totalDistanceMeters,
            _elapsedSeconds
        ) { distance, time ->

            calculator.calculatePace(
                distance,
                time
            )

        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            "--:--"
        )

    //──────────────────────────────────────────────
    // Updates from RunTrackingService
    //──────────────────────────────────────────────

    fun updateElapsedSeconds(
        seconds: Long
    ) {
        _elapsedSeconds.value = seconds
    }

    fun addLocation(
        point: GeoPoint,
        segmentDistance: Float,
        currentPaceSecPerKm: Float
    ) {

        val updatedPoints =
            _pathPoints.value.toMutableList()

        updatedPoints.add(point)

        _pathPoints.value = updatedPoints

        _currentLocation.value = point

        _totalDistanceMeters.value +=
            segmentDistance

        _currentPaceSecPerKm.value =
            currentPaceSecPerKm
    }

    fun updateSplits(
        splits: List<Split>
    ) {
        _splits.value = splits
    }

    fun restoreRunStats(
        distance: Float,
        elapsed: Long,
        path: List<GeoPoint>,
        splits: List<Split>
    ) {

        _totalDistanceMeters.value =
            distance

        _elapsedSeconds.value =
            elapsed

        _pathPoints.value =
            path.toMutableList()

        _splits.value =
            splits
    }



// ----------------- Timer -----------------------

    /*
    Timer is now mainly used to keep the UI state in sync.
    The authoritative elapsed time comes from RunTrackingService.
    */

    fun startRun() {
        _isTracking.value = true
        _isPaused.value = false
    }

    fun pauseRun() {
        _isPaused.value = true
    }

    fun resumeRun() {
        _isPaused.value = false
    }

    fun finishRun() {

        _isTracking.value = false
        _isPaused.value = false

        val result = RunResult(
            distanceMeters = _totalDistanceMeters.value,
            elapsedSeconds = _elapsedSeconds.value,
            pathPoints = _pathPoints.value,
            splits = _splits.value
        )

        _navigateToStats.value = result

        Log.d(
            "Finish Run",
            "Finish run clicked"
        )
    }

    /*
    Job is like a pointer to the coroutine (viewModelScope) example here
    the timerJob is the pointer to that particular coroutine.
    isActive checks if the coroutine is active or not (if Job.cancel() have been
    called or not)
     */

    //──────────────────────────────────────────────
    // Service Controls
    //──────────────────────────────────────────────

    fun startService(context: Context) {

        Log.d(
            "PACETEST",
            "ViewModel startService called"
        )

        Intent(
            context,
            RunTrackingService::class.java
        ).apply {

            action = RunTrackingService.ACTION_START

            ContextCompat.startForegroundService(
                context,
                this
            )
        }
    }

    fun pauseService(
        context: Context
    ) {

        Intent(
            context,
            RunTrackingService::class.java
        ).apply {

            action =
                RunTrackingService.ACTION_PAUSE

            context.startService(this)
        }
    }

    fun resumeService(
        context: Context
    ) {

        Intent(
            context,
            RunTrackingService::class.java
        ).apply {

            action =
                RunTrackingService.ACTION_RESUME

            context.startService(this)
        }
    }

    fun finishService(
        context: Context
    ) {

        Intent(
            context,
            RunTrackingService::class.java
        ).apply {

            action =
                RunTrackingService.ACTION_FINISH

            context.startService(this)
        }
    }

    //──────────────────────────────────────────────
    // Navigation
    //──────────────────────────────────────────────

    fun navigatedToStats() {
        _navigateToStats.value = null
    }

    //──────────────────────────────────────────────
    // Save Run
    //──────────────────────────────────────────────

    fun saveRun(
        onSaved: (Long) -> Unit
    ) {

        val result =
            _navigateToStats.value ?: return

        viewModelScope.launch {

            val runId =
                repo.saveRun(
                    result.distanceMeters,
                    result.elapsedSeconds,
                    result.pathPoints,
                    result.splits
                )

            onSaved(runId)
        }
    }

    override fun onCleared() {

        super.onCleared()

        /*
        GPS tracking is now handled entirely
        by RunTrackingService.

        Therefore, there is nothing left to stop
        inside the ViewModel.
        */

    }
}