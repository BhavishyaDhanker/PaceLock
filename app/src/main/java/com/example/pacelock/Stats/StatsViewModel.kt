package com.example.pacelock.Stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.pacelock.RoomDB.RunEntity
import com.example.pacelock.RunRepository

class StatsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repo = RunRepository(application)

    suspend fun loadRun(
        runId: Long
    ): RunEntity? {
        return repo.getRunById(runId)
    }

    suspend fun loadLatestRun(): RunEntity? {
        return repo.getLatestRun()
    }

    val allRuns = repo.getAllRuns()
    val totalRuns = repo.getTotalRuns()
}