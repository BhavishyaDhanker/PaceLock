package com.example.pacelock.PastRuns

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pacelock.RunRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class PastRunsViewModel(application: Application) : AndroidViewModel(application) {

    val repo = RunRepository(application)


    val runs = repo.getAllRuns().map { list->
        list.map { it.toRunView() }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        emptyList()
    )

    private val _totalDistance = MutableStateFlow<Float>(0f)
    val totalDistance = _totalDistance.asStateFlow()


    suspend fun getTotalDistance(){
        _totalDistance.value = repo.getTotalDistance()/1000f
    }

}