package com.example.pacelock.Run

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pacelock.RunStatsCalculator
import com.example.pacelock.service.RunTrackingService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import org.osmdroid.util.GeoPoint
import kotlin.collections.mutableListOf
import kotlin.concurrent.timer

class RunViewModel(application : Application) : AndroidViewModel(application) {

    private val repo = RunRepository(application)
    private val calculator = RunStatsCalculator

    private val _isTracking = MutableStateFlow<Boolean>(false)
    private val _isPaused = MutableStateFlow<Boolean>(false)
    private val _pathPoints = MutableStateFlow<MutableList<GeoPoint>>(mutableListOf())
    private val _totalDistanceMeters = MutableStateFlow<Float>(0f)
    private val _elapsedSeconds = MutableStateFlow<Long>(0L)



    val isTracking = _isTracking.asStateFlow()
    val isPause = _isPaused.asStateFlow()
    val pathPoints = _pathPoints.asStateFlow()
    val totalDistanceMeters = _totalDistanceMeters.asStateFlow()
    val elapsedSeconds = _elapsedSeconds.asStateFlow()

    val formattedDistance = _totalDistanceMeters.map{
        calculator.formatDistance(it)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "0m"
    )

    val formattedTime = _elapsedSeconds.map{
        calculator.formatTime(it)
    }.stateIn(
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

    fun updateElapsedSeconds(seconds: Long) {
        _elapsedSeconds.value = seconds
    }

    fun addLocation(point: GeoPoint, segmentDistance: Float) {
        val currentPoints = _pathPoints.value ?: mutableListOf()
        currentPoints.add(point)
        _pathPoints.value = currentPoints
        _currentLocation.value = (point)
        _totalDistanceMeters.value = (
            (_totalDistanceMeters.value ?: 0f) + segmentDistance
        )
    }



    val formattedPace : StateFlow<String> =
        combine(
            _totalDistanceMeters,
            _elapsedSeconds
        ) { distance, time ->

            RunStatsCalculator.calculatePace(
                distance,
                time
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            "--:--"
        )


    private val _currentLocation = MutableStateFlow<GeoPoint?>(null)
    val currentLocation = _currentLocation.asStateFlow()



// ----------------- Timer -----------------------

    private var timerJob: Job? = null

    private fun startTimer() {
        timerJob?.cancel()

        timerJob = viewModelScope.launch {
            while (isActive && _isTracking.value) {

                delay(1000L)

                if (!_isPaused.value) {
                    _elapsedSeconds.value += 1
                }
            }
        }
    }

    /*
    Job is like a pointer to the coroutine (viewModelScope) example here
    the timerJob is the pointer to that particular coroutine.
    isActive checks if the coroutine is active or not (if Job.cancel() have been
    called or not)
     */

    fun startLocationUpdates(){
        repo.startTracking { point ->

            _currentLocation.value = point

            if (_isTracking.value && !_isPaused.value){

                val updatedList = _pathPoints.value.apply {
                    add(point)
                }

                _pathPoints.value = updatedList

                val segmentedDistance = calculator.calculateLastSegmentDistance(updatedList)

                _totalDistanceMeters.value += segmentedDistance
            }
        }
    }

    fun startService(context: Context) {

        val intent = Intent(
            context,
            RunTrackingService::class.java
        )

        intent.action = RunTrackingService.ACTION_START

        context.startService(intent)
    }

    fun pauseService(context: Context){
        Intent(context, RunTrackingService::class.java).apply{
            action = RunTrackingService.ACTION_PAUSE
            context.startService(this)
        }
    }

    fun resumeService(context: Context){
        Intent(context, RunTrackingService::class.java).apply{
            action = RunTrackingService.ACTION_RESUME
            context.startService(this)
        }
    }

    fun finishService(context: Context){
        Intent(context, RunTrackingService::class.java).apply{
            action = RunTrackingService.ACTION_FINISH
            context.startService(this)
        }
    }


    fun startRun(){
        _isTracking.value = true
        _isPaused.value = false
        startTimer()
    }

    fun pauseRun(){
        _isPaused.value = true
    }

    fun resumeRun(){
        _isPaused.value = false
    }

    fun finishRun(){
        _isTracking.value = false

        timerJob?.cancel()

        repo.stopTracking()

        Log.d("Finish Run", "Finish run clicked ")
    }


    override fun onCleared() {
        super.onCleared()

        repo.stopTracking()

        timerJob?.cancel()
    }


}