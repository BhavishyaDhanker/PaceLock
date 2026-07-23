package com.example.pacelock.Home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.pacelock.Home.HomeViewModel
import com.example.pacelock.R
import com.example.pacelock.Run.RunActivity
import com.example.pacelock.databinding.FragmentHomeBinding
import kotlinx.coroutines.launch
import java.util.Locale

class HomeFragment : Fragment() {

    companion object {
        fun newInstance() = HomeFragment()
    }

    private var _binding: FragmentHomeBinding? = null

    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container , false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
        setupListeners()
        observeStates()

    }

    private fun setupViews() {

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.loadLatestRun()
            viewModel.loadWeeklyDistance()
        }
    }

    private fun observeStates() {
        viewLifecycleOwner.lifecycleScope.launch{
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navigate.collect {
                        Intent(requireContext(), RunActivity::class.java).apply{
                            putExtra("NEW_RUN", true)
                            startActivity(this)
                        }

                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch{
            repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.lastRunDistance.collect{distance->
                    if (distance != null) {
                        if (distance < 1000f){
                            binding.tvLastRunDistance.text = String.format(
                                Locale.getDefault(),
                                "%.1f",
                                distance
                            )

                            binding.tvRunUnit.text = "M"
                        }
                        else {
                            binding.tvLastRunDistance.text = String.format(
                                Locale.getDefault(),
                                "%.2f",
                                distance/1000f
                            )

                            binding.tvRunUnit.text = "KM"
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch{
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.lastRunPace.collect {
                    binding.tvLastRunPace.text = it
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.weeklyDistance.collect {
                    binding.tvWeeklyDistance.text =
                        String.format(Locale.getDefault(), "%.1f", it)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.progress.collect { progress ->
                    binding.weeklyDistanceProgress.progress = progress
                }
            }
        }


    }

    private fun setupListeners() {
        binding.startRun.setOnClickListener {
            viewModel.onStartRaceBtnClick()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}