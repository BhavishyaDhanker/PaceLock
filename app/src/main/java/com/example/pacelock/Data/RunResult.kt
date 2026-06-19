package com.example.pacelock.Data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.osmdroid.util.GeoPoint

@Parcelize
data class RunResult(
    val distanceMeters : Float,
    val elapsedSeconds : Long,
    val pathPoints: List<GeoPoint>,
    val splits: List<Split>
) : Parcelable