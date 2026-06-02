package com.example.pacelock.Run

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.osmdroid.util.GeoPoint
import kotlin.collections.mutableListOf

class RunViewModel : ViewModel() {


    private val _isTracking = MutableStateFlow<Boolean>(false)
    private val _isPause = MutableStateFlow<Boolean>(false)
    private val _pathPoints = MutableStateFlow<MutableList<GeoPoint>>(mutableListOf())

    val isTracking = _isTracking.asStateFlow()
    val isPause = _isPause.asStateFlow()
    val pathPoints = _pathPoints.asStateFlow()


    fun onPauseBtnClicked() {

        if(!_isPause.value){
            _isPause.value = true
        }else{
            _isPause.value = false
        }
    }

    fun onFinishBtnClicked() {
        _isTracking.value = false
    }





}