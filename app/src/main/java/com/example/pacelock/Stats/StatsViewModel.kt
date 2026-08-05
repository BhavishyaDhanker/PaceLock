package com.example.pacelock.Stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.pacelock.RoomDB.RunEntity
import com.example.pacelock.RunRepository
import com.github.mikephil.charting.data.BarEntry
import java.util.Calendar

class StatsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repo = RunRepository(application)

    suspend fun loadRun(runId: Long): RunEntity? {
        return repo.getRunById(runId)
    }

    suspend fun loadLatestRun(): RunEntity? {
        return repo.getLatestRun()
    }

    val allRuns = repo.getAllRuns()
    val totalRuns = repo.getTotalRuns()

    /**
     * Filters and groups the runs by the current Week, Month, or Year.
     */
    fun processChartData(runs: List<RunEntity>, timeframe: Int): Pair<List<BarEntry>, List<String>> {
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()
        val cal = Calendar.getInstance()

        val currentYear = cal.get(Calendar.YEAR)
        val currentMonth = cal.get(Calendar.MONTH)
        val currentWeek = cal.get(Calendar.WEEK_OF_YEAR)

        when (timeframe) {
            0 -> { // THIS WEEK (Mon - Sun)
                labels.addAll(listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"))
                val dailyTotals = FloatArray(7)

                runs.forEach { run ->
                    cal.timeInMillis = run.timestamp
                    if (cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.WEEK_OF_YEAR) == currentWeek) {
                        var dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 2 // Adjust so Monday = 0
                        if (dayOfWeek < 0) dayOfWeek = 6 // Sunday becomes index 6

                        // Convert meters to KM
                        dailyTotals[dayOfWeek] += (run.distance / 1000f)
                    }
                }
                dailyTotals.forEachIndexed { index, total ->
                    entries.add(BarEntry(index.toFloat(), total))
                }
            }
            1 -> { // THIS MONTH (Up to 5 weeks)
                labels.addAll(listOf("Wk 1", "Wk 2", "Wk 3", "Wk 4", "Wk 5"))
                val weeklyTotals = FloatArray(5)

                runs.forEach { run ->
                    cal.timeInMillis = run.timestamp
                    if (cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.MONTH) == currentMonth) {
                        val weekOfMonth = cal.get(Calendar.WEEK_OF_MONTH) - 1
                        if (weekOfMonth in 0..4) {
                            weeklyTotals[weekOfMonth] += (run.distance / 1000f)
                        }
                    }
                }
                weeklyTotals.forEachIndexed { index, total ->
                    entries.add(BarEntry(index.toFloat(), total))
                }
            }
            2 -> { // THIS YEAR (Jan - Dec)
                labels.addAll(listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"))
                val monthlyTotals = FloatArray(12)

                runs.forEach { run ->
                    cal.timeInMillis = run.timestamp
                    if (cal.get(Calendar.YEAR) == currentYear) {
                        val month = cal.get(Calendar.MONTH)
                        monthlyTotals[month] += (run.distance / 1000f)
                    }
                }
                monthlyTotals.forEachIndexed { index, total ->
                    entries.add(BarEntry(index.toFloat(), total))
                }
            }
        }
        return Pair(entries, labels)
    }
}