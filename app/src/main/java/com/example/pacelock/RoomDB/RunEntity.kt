package com.example.pacelock.RoomDB

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "runs")
data class RunEntity(
    @PrimaryKey(autoGenerate = true) val id : Long = 0L,

    val distance: Float,
    val elapsed: Long,
    val avgPaceSecPerKm: Float,
    val timestamp: Long,
    val pathPointsJson: String,      /* We can't store a List<GeoPoint> directly in Room —
                                    so we serialize it to a JSON string. We'll handle
                                    that with a TypeConverter.*/
    val splitsJson: String

)
