package com.example.pacelock.Stats

import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.pacelock.Data.RunResult
import com.example.pacelock.RoomDB.GeoPointTypeConverter
import com.example.pacelock.RunStatsCalculator
import com.example.pacelock.databinding.FragmentStatsBinding
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK
import org.osmdroid.util.BoundingBox.fromGeoPoints
import org.osmdroid.views.overlay.Polyline

class StatsFragment : Fragment() {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StatsViewModel by viewModels()

    private val converter = GeoPointTypeConverter()
    private val calculator = RunStatsCalculator

    companion object {

        private const val ARG_RUN_ID = "run_id"

        fun newInstance(runId: Long): StatsFragment {
            return StatsFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_RUN_ID, runId)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentStatsBinding.inflate(
            inflater,
            container,
            false
        )

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        loadRun()
    }

    private fun loadRun() {

        viewLifecycleOwner.lifecycleScope.launch {

            val runId = arguments?.getLong(ARG_RUN_ID, -1L)

            val run = if (runId != null && runId != -1L) {
                viewModel.loadRun(runId)
            } else {
                viewModel.loadLatestRun()
            }

            run?.let {

                val runResult = RunResult(
                    distanceMeters = it.distance,
                    elapsedSeconds = it.elapsed,
                    pathPoints = converter.toGeoPointString(it.pathPointsJson),
                    splits = converter.toSplits(it.splitsJson)
                )

                populateView(runResult)
                setupMap(runResult)

            } ?: showEmptyState()
        }
    }

    private fun showEmptyState() {

        binding.tvDistance.text = "--"
        binding.tvTime.text = "--"
        binding.tvPace.text = "--"
    }

    private fun setupMap(
        result: RunResult
    ) {

        if (result.pathPoints.size < 2) return

        binding.statsMapView.apply {
            setTileSource(MAPNIK)
            setMultiTouchControls(false)
        }

        val routePolyline = Polyline().apply {

            setPoints(result.pathPoints)

            outlinePaint.color =
                Color.parseColor("#2979FF")

            outlinePaint.strokeWidth = 8f
            outlinePaint.strokeCap = Paint.Cap.ROUND
            outlinePaint.isAntiAlias = true
        }

        binding.statsMapView.overlays.clear()
        binding.statsMapView.overlays.add(routePolyline)

        val boundingBox =
            fromGeoPoints(result.pathPoints)

        binding.statsMapView.post {

            binding.statsMapView.zoomToBoundingBox(
                boundingBox.increaseByScale(1.2f),
                false
            )
        }

        binding.statsMapView.invalidate()
    }

    private fun populateView(
        result: RunResult
    ) {

        binding.tvDistance.text =
            calculator.formatDistance(
                result.distanceMeters
            )

        binding.tvTime.text =
            calculator.formatTime(
                result.elapsedSeconds
            )

        binding.tvPace.text =
            calculator.calculatePace(
                result.distanceMeters,
                result.elapsedSeconds
            )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}