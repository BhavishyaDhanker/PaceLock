package com.example.pacelock.Data

import org.osmdroid.util.GeoPoint
import java.sql.Timestamp

data class PaceWindowEntry(
    val point: GeoPoint,
    val timestamp: Long
)
