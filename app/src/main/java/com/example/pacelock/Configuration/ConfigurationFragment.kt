package com.example.pacelock.Configuration

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.slider.Slider
import com.example.pacelock.R
import com.example.pacelock.databinding.FragmentConfigurationBinding
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

            viewModel.settings.collect { settings ->

                // Voice & Haptics

                binding.switchVoice.isChecked =
                    settings.ttsEnabled

                binding.switchVibration.isChecked =
                    settings.hapticsEnabled

                // Metronome

                binding.switchMetronome.isChecked =
                    settings.metronomeEnabled

                if (settings.metronomeUseSound) {

                    binding.radioSound.isChecked = true

                } else {

                    binding.radioVibration.isChecked = true
                }

                binding.radioSound.isEnabled =
                    settings.metronomeEnabled

                binding.radioVibration.isEnabled =
                    settings.metronomeEnabled


                // Cadence

                binding.sliderCadence.value =
                    settings.metronomeBpm.toFloat()

                binding.tvCadence.text =
                    settings.metronomeBpm.toString()


                binding.sliderCadence.isEnabled =
                    settings.metronomeEnabled

                binding.tvCadence.alpha =
                    if (settings.metronomeEnabled) 1f else 0.5f

                // Pace

                binding.sliderPace.value =
                    settings.targetPacePerSecPerKm

                updatePaceText(
                    settings.targetPacePerSecPerKm.toInt()
                )

                // Pace Tolerance

                binding.sliderTolerance.value =
                    settings.targetPaceTolerance

                binding.tvTolerance.text =
                    "±${settings.targetPaceTolerance.toInt()} seconds"


                // Weekly Target
                binding.sliderWeeklyTarget.addOnChangeListener { _, value, _ ->

                    binding.tvWeeklyTarget.text = value.toInt().toString()

                    viewModel.updateWeeklyTarget(value.toInt())
                }
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

        binding.switchMetronome.setOnCheckedChangeListener {

                _,
                checked ->

            viewModel.updateMetronome(checked)
        }

        binding.radioMetronomeType.setOnCheckedChangeListener {

                    _,
                    checkedId ->

                viewModel.updateMetronomeSound(

                    checkedId ==
                            binding.radioSound.id

                )
            }

        binding.sliderTolerance.addOnChangeListener {

                _,
                value,
                fromUser ->

            if (!fromUser) return@addOnChangeListener

            binding.tvTolerance.text =
                "±${value.toInt()} s"

            viewModel.updateTargetPaceTolerance(value)
        }


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