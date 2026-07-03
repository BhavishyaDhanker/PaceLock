package com.example.pacelock.Data

import java.sql.Timestamp

data class RunView(
    val timestamp : Long,
    val distanceMeters : Float,
    val durationSeconds : Int
)
