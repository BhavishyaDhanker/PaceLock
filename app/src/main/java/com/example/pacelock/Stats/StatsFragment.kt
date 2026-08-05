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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pacelock.Data.RunResult
import com.example.pacelock.R
import com.example.pacelock.RoomDB.GeoPointTypeConverter
import com.example.pacelock.RoomDB.RunEntity
import com.example.pacelock.RunStatsCalculator
import com.example.pacelock.databinding.FragmentStatsBinding
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK
import org.osmdroid.util.BoundingBox.fromGeoPoints
import org.osmdroid.views.overlay.Polyline
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.tabs.TabLayout

class StatsFragment : Fragment() {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StatsViewModel by viewModels()
    private lateinit var runResult: RunResult
    private lateinit var mySplitsAdapter: SplitsAdapter
    private lateinit var barChart: BarChart
    private lateinit var tabLayout: TabLayout

    private val converter = GeoPointTypeConverter()
    private val calculator = RunStatsCalculator
    private var currentRuns: List<RunEntity> = emptyList()

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

        barChart = view.findViewById(R.id.statsBarChart)
        tabLayout = view.findViewById(R.id.timeframeTabLayout)

        setupChart()

        // Observe Room Database for all runs
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allRuns.collect { runs ->
                currentRuns = runs
                // Refresh chart data immediately when database updates
                loadDataForTimeframe(tabLayout.selectedTabPosition)
            }
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.position?.let { loadDataForTimeframe(it) }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        setupRecyclerView()
        loadRun()
    }

    private fun setupChart() {
        barChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setDrawGridBackground(false)
            setDrawBorders(false)

            xAxis.apply {
                textColor = Color.parseColor("#888888")
                setDrawGridLines(false)
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f // Ensure one label per bar
            }

            axisLeft.apply {
                textColor = Color.parseColor("#888888")
                setDrawGridLines(true)
                gridColor = Color.parseColor("#333333")
                axisMinimum = 0f // Start Y axis at 0
            }
            axisRight.isEnabled = false
        }
    }

    private fun loadDataForTimeframe(tabPosition: Int) {
        if (currentRuns.isEmpty()) {
            barChart.clear()
            return
        }

        // Fetch grouped data and labels from ViewModel
        val (entries, labels) = viewModel.processChartData(currentRuns, tabPosition)

        // Set the custom X-Axis formatter based on the current labels
        barChart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return if (index >= 0 && index < labels.size) labels[index] else ""
            }
        }

        val dataSet = BarDataSet(entries, "Distance").apply {
            color = Color.parseColor("#C6FF00")
            valueTextColor = Color.WHITE
            valueTextSize = 10f
        }

        barChart.data = BarData(dataSet)
        // Add animation for smooth transitions between tabs
        barChart.animateY(500)
        barChart.invalidate()
    }

    private fun setupRecyclerView() {
        mySplitsAdapter = SplitsAdapter(emptyList())

        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = mySplitsAdapter
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

                runResult = RunResult(
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

        if (result.distanceMeters < 1000f) {

            binding.tvDistance.text =
                String.format("%.0f", result.distanceMeters)

            binding.tvDistanceUnit.text = "m"

        } else {

            binding.tvDistance.text =
                String.format("%.2f", result.distanceMeters / 1000f)

            binding.tvDistanceUnit.text = "km"
        }




        binding.tvTime.text =
            calculator.formatTime(
                result.elapsedSeconds
            )

        binding.tvPace.text =
            calculator.calculatePace(
                result.distanceMeters,
                result.elapsedSeconds
            )

        mySplitsAdapter.updateRecyclerView(runResult.splits )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}