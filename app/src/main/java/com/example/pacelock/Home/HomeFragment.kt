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

        setupListeners()
        observeStates()
    }

    private fun observeStates() {
        lifecycleScope.launch{
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navigate.collect {
                        val intent = Intent(requireContext(), RunActivity::class.java)
                        startActivity(intent)
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