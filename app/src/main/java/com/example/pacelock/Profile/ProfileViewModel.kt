package com.example.pacelock.Profile

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow


data class UserProfile(
    val name: String = "",
    val age: Int = 0,
    val gender: String = "",
    val height: Double = 0.0,
    val weight: Double = 0.0,
    val vo2max: Int = 0,
    val lthr: Int = 0,
    val maxHr: Int = 0,
    val profilePicUrl: String = ""
)

class ProfileViewModel : ViewModel() {

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile = _userProfile.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // Keep track of the listener so we can detach it if needed
    private var snapshotListener: ListenerRegistration? = null

    init {
        fetchUserData()
    }

    private fun fetchUserData() {
        val userId = auth.currentUser?.uid ?: return


        snapshotListener = firestore.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ProfileViewModel", "Firestore listen failed.", error)
                    _isLoading.value = false
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    // Convert the Firestore document directly into our data class
                    val profile = snapshot.toObject(UserProfile::class.java)
                    if (profile != null) {
                        _userProfile.value = profile
                    }
                } else {
                    Log.d("ProfileViewModel", "No such document exists")
                }
                _isLoading.value = false
            }
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up the listener when the ViewModel is destroyed to prevent memory leaks
        snapshotListener?.remove()
    }
}