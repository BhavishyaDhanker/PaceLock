package com.example.pacelock.Configration

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.slider.Slider
import com.example.pacelock.R
import com.example.pacelock.databinding.FragmentConfigurationBinding
import com.example.pacelock.Configration.ConfigurationViewModel
import kotlinx.coroutines.launch
import java.util.Locale

class ConfigurationFragment : Fragment(R.layout.fragment_configuration) {

    private var _binding: FragmentConfigurationBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ConfigurationViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentConfigurationBinding.bind(view)

        observeViewModel()
        setupListeners()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.settings.collect {settings ->
                binding.switchVoice.isChecked = settings.ttsEnabled
                binding.switchVibration.isChecked = settings.hapticsEnabled

                binding.sliderCadence.value = settings.metronomeBpm.toFloat()
                binding.tvCadence.text = settings.metronomeBpm.toString()

                updatePaceText(binding.sliderPace.value.toInt())


            }
        }

    }

    private fun setupListeners() {

        binding.switchVoice.setOnCheckedChangeListener { _, checked ->
            viewModel.updateTTS(checked)
        }

        binding.switchVibration.setOnCheckedChangeListener { _, checked ->
            viewModel.updateHaptics(checked)
        }

        binding.sliderCadence.addOnChangeListener(
            Slider.OnChangeListener { _, value, fromUser ->

                if (!fromUser) return@OnChangeListener

                val bpm = value.toInt()

                binding.tvCadence.text = bpm.toString()

                viewModel.updateBPM(bpm)
            }
        )

        binding.sliderPace.addOnChangeListener(
            Slider.OnChangeListener { _, value, fromUser ->

                if (!fromUser) return@OnChangeListener

                val seconds = value.toInt()

                updatePaceText(seconds)

                viewModel.updateTargetPacePerSecPerKm(seconds.toFloat())
            }
        )
    }

    private fun updatePaceText(totalSeconds: Int) {

        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60

        binding.tvPaceValue.text =
            String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}