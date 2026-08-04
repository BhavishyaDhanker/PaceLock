package com.example.pacelock.Profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class EditProfileViewModel : ViewModel() {

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile = _userProfile.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    init {
        loadCurrentUserProfile()
    }

    private fun loadCurrentUserProfile() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val document = firestore.collection("users").document(userId).get().await()
                if (document.exists()) {
                    val profile = document.toObject(UserProfile::class.java)
                    if (profile != null) {
                        _userProfile.value = profile
                    }
                }
            } catch (e: Exception) {
                Log.e("EditProfileVM", "Error loading profile", e)
                _uiEvent.emit(UiEvent.ShowToast("Failed to load profile."))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveProfile(updatedProfile: UserProfile) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            viewModelScope.launch { _uiEvent.emit(UiEvent.ShowToast("User not authenticated.")) }
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                firestore.collection("users").document(userId).set(updatedProfile).await()
                _uiEvent.emit(UiEvent.ShowToast("Profile updated successfully"))
                _uiEvent.emit(UiEvent.NavigateBack)
            } catch (e: Exception) {
                Log.e("EditProfileVM", "Error saving profile", e)
                _uiEvent.emit(UiEvent.ShowToast("Failed to save changes."))
            } finally {
                _isLoading.value = false
            }
        }
    }

    sealed class UiEvent {
        data class ShowToast(val message: String) : UiEvent()
        object NavigateBack : UiEvent()
    }
}