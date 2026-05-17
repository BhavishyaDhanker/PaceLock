package com.example.pacelock

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeActViewModel : ViewModel() {

    private val _selectedTab = MutableStateFlow<Int>(0)
    val selectedTab = _selectedTab.asStateFlow()

    fun changeTab(tabId : Int){
        _selectedTab.value = tabId
    }


}