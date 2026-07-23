package com.example.pacelock.Configuration

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pacelock.Data.CoachingSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ConfigurationViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = ConfigurationRepository(application)
    val settings = repo.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = CoachingSettings()
    )

    fun updateTTS(flag: Boolean){
        viewModelScope.launch {
            repo.updateTTS(flag)
        }
    }

    fun updateHaptics(flag: Boolean){
        viewModelScope.launch {
            repo.updateHaptics(flag)
        }
    }

    fun updateMetronome(flag: Boolean){
        viewModelScope.launch {
            repo.updateMetronome(flag)
        }
    }

    fun updateBPM(bpm : Int){
        viewModelScope.launch {
            repo.updateBPM(bpm)
        }
    }

    fun updateMetronomeSound(flag: Boolean){
        viewModelScope.launch {
            repo.updateMetronomeSound(flag)
        }
    }

    fun updateTargetPacePerSecPerKm(flag : Float){
        viewModelScope.launch {
            repo.updateTargetPacePerSecPerKm(flag)
        }
    }

    fun updateTargetPaceTolerance(flag: Float){
        viewModelScope.launch {
            repo.updateTargetPaceTolerance(flag)
        }
    }

    fun updateWeeklyTarget(target: Int) {
        viewModelScope.launch {
            repo.updateWeeklyTarget(target)
        }
    }

}