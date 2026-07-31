package com.example.pacelock.PastRuns

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pacelock.PastRuns.PastRunsViewModel
import com.example.pacelock.R
import com.example.pacelock.Stats.StatsFragment
import com.example.pacelock.databinding.FragmentPastRunsBinding
import kotlinx.coroutines.launch
import java.util.Locale

class PastRunsFragment : Fragment() {

    companion object {
        fun newInstance() = PastRunsFragment()
    }

    private var _binding: FragmentPastRunsBinding? = null

    private val binding get() = _binding!!

    private val viewModel: PastRunsViewModel by viewModels()

    private lateinit var myRunsAdapter: PastRunsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPastRunsBinding.inflate(inflater, container , false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        setupViews()
        setupListeners()
        observeViewModel()
    }

    private fun setupViews() {

        // setting up recyclerView
        myRunsAdapter = PastRunsAdapter(emptyList()){ runId ->

            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.frame,
                    StatsFragment.newInstance(runId)
                )
                .addToBackStack(null)
                .commit()
        }

        binding.historyRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.historyRecycler.adapter = myRunsAdapter


        // getting total distance
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getTotalDistance()
        }
    }

    private fun observeViewModel() {

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.runs.collect { runs->
                myRunsAdapter.updateRuns(runs)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.totalDistance.collect { totalDistance->
                binding.totalDistance.text = String.format(
                    Locale.getDefault(),
                    "%.2f",
                    totalDistance
                )
            }
        }


    }

    private fun setupListeners() {
    }
}