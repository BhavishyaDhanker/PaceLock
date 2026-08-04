package com.example.pacelock

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeActViewModel : ViewModel() {

    private val _selectedTab = MutableStateFlow<Int>(0)
    val selectedTab = _selectedTab.asStateFlow()
    private val _profileBtnClicked = MutableSharedFlow<Boolean>()
    val profileBtnClicked = _profileBtnClicked.asSharedFlow()

    fun changeTab(tabId : Int){
        _selectedTab.value = tabId
    }


    fun profileIntent() {
        viewModelScope.launch {
            _profileBtnClicked.emit(true)
        }
    }
}