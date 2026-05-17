package com.example.pacelock.Home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val _navigate = MutableSharedFlow<Unit>()

    val navigate = _navigate.asSharedFlow()

    fun onStartRaceBtnClick(){                  // we used sharedFlow and unit because navigation is a one time
        viewModelScope.launch {                 // activity and this method is much cleaner than stateflow for one
            _navigate.emit(Unit)        //  time use cases
        }
    }
}