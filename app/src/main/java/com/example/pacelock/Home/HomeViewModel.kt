package com.example.pacelock.Home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pacelock.Configuration.ConfigurationRepository
import com.example.pacelock.Data.RunResult
import com.example.pacelock.RoomDB.RunEntity
import com.example.pacelock.RunRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = RunRepository(application)
    private val configRepo = ConfigurationRepository(application)

    private val _navigate = MutableSharedFlow<Unit>()
    val navigate = _navigate.asSharedFlow()
    private val _latestRun = MutableStateFlow<RunEntity?>(null)
    val latestRun = _latestRun.asStateFlow()
    private val _lastRunDistance = MutableStateFlow<Float?>(null)
    val lastRunDistance = _lastRunDistance.asStateFlow()
    private val _lastRunPace = MutableStateFlow<String?>(null)
    val lastRunPace = _lastRunPace.asStateFlow()
    private val _weeklyDistance = MutableStateFlow<Float?>(null)
    val weeklyDistance = _weeklyDistance.asStateFlow()
    private var _weeklyTarget = MutableStateFlow<Int?>(null)
    val weeklyTarget = _weeklyTarget.asStateFlow()
    private val _progress = MutableStateFlow(0)
    val progress = _progress.asStateFlow()


    init {
        viewModelScope.launch {
            configRepo.weeklyTarget.collect { target ->
                _weeklyTarget.value = target
                updateProgress()
            }
        }
    }

    
    fun onStartRaceBtnClick(){                  // we used sharedFlow and unit because navigation is a one time
        viewModelScope.launch {                 // activity and this method is much cleaner than stateflow for one
            _navigate.emit(Unit)        //  time use cases
        }
    }

    suspend fun loadLatestRun(){
        _latestRun.value = repo.getLatestRun()

        _lastRunDistance.value = _latestRun.value?.distance

        val paceSecPerKm = _latestRun.value?.avgPaceSecPerKm

        val paceMinutes = paceSecPerKm?.div(60f)?.toInt()
        val paceSeconds = paceSecPerKm?.rem(60f)?.toInt()

        _lastRunPace.value = String.format(
            Locale.getDefault(),
            "%d:%02d",
           paceMinutes, paceSeconds
        )
    }

    suspend fun loadWeeklyDistance(){
        val weeklyDistanceMeters = repo.getWeeklyDistance()

        _weeklyDistance.value = weeklyDistanceMeters/1000f

        updateProgress()
    }


    private fun updateProgress() {

        val distance = _weeklyDistance.value ?: return
        val target = _weeklyTarget.value ?: return

        _progress.value =
            ((distance / target) * 100f)
                .coerceIn(0f, 100f)
                .toInt()
    }


}