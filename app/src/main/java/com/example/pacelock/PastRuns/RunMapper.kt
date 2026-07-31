package com.example.pacelock.PastRuns

import com.example.pacelock.Data.RunView
import com.example.pacelock.RoomDB.RunEntity


    fun RunEntity.toRunView() : RunView {
        return RunView(
            timestamp = timestamp,
            distanceMeters = distance,
            durationSeconds = elapsed.toInt(),
            id = id
        )
    }

// Extension function: lets us call entity.toRunView() even though RunEntity isn't modified;
// it's actually a top-level function that takes RunEntity as its receiver.