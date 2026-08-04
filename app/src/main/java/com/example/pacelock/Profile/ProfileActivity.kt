package com.example.pacelock.Profile

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.pacelock.databinding.ActivityProfileBinding
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.tvEditProfile.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)

        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.userProfile.collect { profile ->
                        updateUI(profile)
                    }
                }

                launch {
                    viewModel.isLoading.collect { isLoading ->
                        // Optional: Show or hide a ProgressBar
                        // binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }

    private fun updateUI(profile: UserProfile) {
        binding.apply {
            // Use 'takeIf' to check for empty strings, otherwise show a default placeholder
            name.text = profile.name.takeIf { it.isNotBlank() }?.uppercase() ?: "NEW RUNNER"
            gender.text = profile.gender.takeIf { it.isNotBlank() }?.uppercase() ?: "--"

            // Check if numbers are greater than 0, otherwise show "--"
            tvAge.text = if (profile.age > 0) profile.age.toString() else "--"
            height.text = if (profile.height > 0.0) "${profile.height} CM" else "--"
            weight.text = if (profile.weight > 0.0) "${profile.weight} KG" else "--"

            tvVO2max.text = if (profile.vo2max > 0) profile.vo2max.toString() else "--"
            tvLTHR.text = if (profile.lthr > 0) profile.lthr.toString() else "--"
            tvMaxHR.text = if (profile.maxHr > 0) profile.maxHr.toString() else "--"

            // Handle empty profile picture
            /*
            if (profile.profilePicUrl.isNotBlank()) {
                Glide.with(this@ProfileActivity)
                    .load(profile.profilePicUrl)
                    .circleCrop()
                    .into(pfp)
            } else {
                // Load a default drawable avatar if URL is empty
                pfp.setImageResource(R.drawable.default_avatar)
            }
            */
        }
    }
}