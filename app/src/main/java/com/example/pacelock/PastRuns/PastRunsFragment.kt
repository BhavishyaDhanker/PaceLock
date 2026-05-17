package com.example.pacelock.PastRuns

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.pacelock.PastRuns.PastRunsViewModel
import com.example.pacelock.R

class PastRunsFragment : Fragment() {

    companion object {
        fun newInstance() = PastRunsFragment()
    }

    private val viewModel: PastRunsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: Use the ViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_past_runs, container, false)
    }
}