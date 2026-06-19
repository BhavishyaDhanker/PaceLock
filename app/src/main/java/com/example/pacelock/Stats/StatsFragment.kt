package com.example.pacelock.Stats

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.Cap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.pacelock.Data.RunResult
import com.example.pacelock.R
import com.example.pacelock.RunStatsCalculator
import com.example.pacelock.databinding.FragmentHomeBinding
import com.example.pacelock.databinding.FragmentStatsBinding
import org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.BoundingBox.fromGeoPoints
import org.osmdroid.views.overlay.Polyline

class StatsFragment : Fragment() {

    private var runResult : RunResult? = null
    private var _binding : FragmentStatsBinding? = null
    private val binding get() =  _binding!!
    private lateinit var calculator : RunStatsCalculator



    companion object {

        const val ARG_RUN_RESULT = "run_result"
        fun newInstance(runResult: RunResult): StatsFragment {
            return StatsFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_RUN_RESULT, runResult)
                }
            }
        }
    }

    private val viewModel: StatsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        runResult = arguments?.getParcelable(ARG_RUN_RESULT)
        calculator = RunStatsCalculator
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatsBinding.inflate(inflater, container , false)
        val view = binding.root
        return view
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        runResult?.let {
            populateView(it)
            setupMap(it)
        } ?: run {
            showEmptyStates()
        }
    }

    fun showEmptyStates() {

    }

    fun setupMap(result: RunResult) {
        if(result.pathPoints.size >= 2){
            binding.statsMapView.apply{
                setTileSource(MAPNIK)
                setMultiTouchControls(false)
            }


            val routePolyline = Polyline().apply {
                setPoints(result.pathPoints)
                outlinePaint.color = Color.parseColor("#2979FF")
                outlinePaint.strokeWidth = 8f
                outlinePaint.strokeCap = Paint.Cap.ROUND
                outlinePaint.isAntiAlias = true
            }

            binding.statsMapView.overlays.add(routePolyline)

            val boundingBox =
                fromGeoPoints(result.pathPoints)
            binding.statsMapView.post {
                binding.statsMapView.zoomToBoundingBox(boundingBox.increaseByScale(1.2f), false)
            }

            binding.statsMapView.invalidate()
        }
    }

    fun populateView(result: RunResult) {
        binding.tvDistance.text =  calculator.formatDistance(result.distanceMeters)
        binding.tvTime.text = calculator.formatTime(result.elapsedSeconds)
        binding.tvPace.text = calculator.calculatePace(result.distanceMeters, result.elapsedSeconds)
    }
}