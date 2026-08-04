package com.example.pacelock.PastRuns

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pacelock.RunRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters


enum class RunFilter {
    ALL_TIME, THIS_WEEK, LAST_WEEK
}
class PastRunsViewModel(application: Application) : AndroidViewModel(application) {

    val repo = RunRepository(application)

    private val _currentFilter = MutableStateFlow(RunFilter.ALL_TIME)

    val runs = combine(
        repo.getAllRuns().map { list -> list.map { it.toRunView() } },
        _currentFilter
    ) { runList, filter ->

        when (filter) {
            RunFilter.ALL_TIME -> {
                runList
            }
            RunFilter.THIS_WEEK -> {
                val start = getStartOfThisWeek()

                if (runList.isNotEmpty()) {
                    Log.i("FilterDebug", "Boundary Start: $start")
                    Log.i("FilterDebug", "First Run Timestamp: ${runList[0].timestamp}")
                }

                runList.filter { it.timestamp >= start }
            }
            RunFilter.LAST_WEEK -> {
                val start = getStartOfLastWeek()
                val end = getEndOfLastWeek()
                runList.filter { it.timestamp in start..end }
            }
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        emptyList()
    )

    private val _totalDistance = MutableStateFlow<Float>(0f)
    val totalDistance = _totalDistance.asStateFlow()


    suspend fun getTotalDistance(){
        _totalDistance.value = repo.getTotalDistance()/1000f
    }

    fun setFilter(filter: RunFilter) {
        _currentFilter.value = filter
    }



    private fun getStartOfThisWeek(): Long {
        val today = LocalDate.now()
        val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return startOfWeek.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    // Returns the timestamp for Monday 00:00:00 of the previous week
    private fun getStartOfLastWeek(): Long {
        val today = LocalDate.now()
        val startOfThisWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val startOfLastWeek = startOfThisWeek.minusWeeks(1)
        return startOfLastWeek.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    // Returns the timestamp for Sunday 23:59:59 of the previous week
    private fun getEndOfLastWeek(): Long {
        val today = LocalDate.now()
        val startOfThisWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val endOfLastWeek = startOfThisWeek.minusDays(1)
        return endOfLastWeek.atTime(23, 59, 59, 999999999)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

}