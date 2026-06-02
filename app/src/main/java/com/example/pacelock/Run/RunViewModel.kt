package com.example.pacelock.Run

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pacelock.RunStatsCalculator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import org.osmdroid.util.GeoPoint
import kotlin.collections.mutableListOf

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
        calculator.elapsedTime(it)
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
            while (_isTracking.value) {

                delay(1000L)

                if (!_isPaused.value) {
                    _elapsedSeconds.value += 1
                }
            }
        }
    }



    fun onPauseBtnClicked() {

        if(!_isPaused.value){
            _isPaused.value = true
        }else{
            _isPaused.value = false
        }
    }

    fun onFinishBtnClicked() {
        _isTracking.value = false
    }





}