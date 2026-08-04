package com.example.pacelock.Profile

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.pacelock.databinding.ActivityEditProfileBinding
import kotlinx.coroutines.launch

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private val viewModel: EditProfileViewModel by viewModels()

    private val genderOptions = arrayOf("Male", "Female", "Other", "Prefer not to say")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupListeners()
        observeViewModel()
    }

    private fun setupUI() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, genderOptions)
        binding.spinnerGender.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnClose.setOnClickListener {
            finish()
        }

        binding.btnSave.setOnClickListener {
            saveData()
        }

        binding.tvChangePhoto.setOnClickListener {
            // Implementation for photo picker goes here
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.userProfile.collect { profile ->
                        populateFields(profile)
                    }
                }

                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                        binding.btnSave.isEnabled = !isLoading
                    }
                }

                launch {
                    viewModel.uiEvent.collect { event ->
                        when (event) {
                            is EditProfileViewModel.UiEvent.ShowToast -> {
                                Toast.makeText(this@EditProfileActivity, event.message, Toast.LENGTH_SHORT).show()
                            }
                            is EditProfileViewModel.UiEvent.NavigateBack -> {
                                finish()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun populateFields(profile: UserProfile) {
        binding.apply {
            etName.setText(profile.name)
            etAge.setText(if (profile.age > 0) profile.age.toString() else "")
            etHeight.setText(if (profile.height > 0.0) profile.height.toString() else "")
            etWeight.setText(if (profile.weight > 0.0) profile.weight.toString() else "")
            etVo2Max.setText(if (profile.vo2max > 0) profile.vo2max.toString() else "")
            etLthr.setText(if (profile.lthr > 0) profile.lthr.toString() else "")
            etMaxHr.setText(if (profile.maxHr > 0) profile.maxHr.toString() else "")

            val genderIndex = genderOptions.indexOfFirst { it.equals(profile.gender, ignoreCase = true) }
            if (genderIndex >= 0) {
                spinnerGender.setSelection(genderIndex)
            }
        }
    }

    private fun saveData() {
        binding.apply {
            val name = etName.text.toString().trim()
            val age = etAge.text.toString().toIntOrNull() ?: 0
            val height = etHeight.text.toString().toDoubleOrNull() ?: 0.0
            val weight = etWeight.text.toString().toDoubleOrNull() ?: 0.0
            val vo2Max = etVo2Max.text.toString().toIntOrNull() ?: 0
            val lthr = etLthr.text.toString().toIntOrNull() ?: 0
            val maxHr = etMaxHr.text.toString().toIntOrNull() ?: 0
            val gender = spinnerGender.selectedItem.toString()

            if (name.isEmpty()) {
                Toast.makeText(this@EditProfileActivity, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                return
            }

            // Keep the existing profile picture URL if we aren't changing it yet
            val currentPicUrl = viewModel.userProfile.value.profilePicUrl

            val updatedProfile = UserProfile(
                name = name,
                age = age,
                gender = gender,
                height = height,
                weight = weight,
                vo2max = vo2Max,
                lthr = lthr,
                maxHr = maxHr,
                profilePicUrl = currentPicUrl
            )

            viewModel.saveProfile(updatedProfile)
        }
    }
}