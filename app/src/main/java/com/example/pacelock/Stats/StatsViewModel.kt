package com.example.pacelock.Stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pacelock.Data.RunResult
import com.example.pacelock.RunRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    val repo = RunRepository(application)

    private val _currentRunResult = MutableStateFlow<RunResult?>(null)
    val currentRunResult = _currentRunResult.asStateFlow()

    private val _isRunSaved = MutableStateFlow<Boolean>(false)
    val isRunSaved = _isRunSaved.asStateFlow()

    fun setRunResult(result: RunResult) {
        _currentRunResult.value = result
    }

    fun saveRun(run: RunResult){
        viewModelScope.launch {
            repo.saveRun(run.distanceMeters,
                run.elapsedSeconds,
                run.pathPoints)
        }
        _isRunSaved.value = true
    }

    fun resetSavedStates(){
        _isRunSaved.value = false
        _currentRunResult.value = null
    }

    val allRuns = repo.getAllRuns()
    val totalDistance = repo.getTotalDistance()
    val totalRuns = repo.getTotalRuns()

}