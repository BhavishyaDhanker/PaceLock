package com.example.pacelock.Data

import java.sql.Timestamp

data class RunView(
    val id: Long,
    val timestamp : Long,
    val distanceMeters : Float,
    val durationSeconds : Int
)
